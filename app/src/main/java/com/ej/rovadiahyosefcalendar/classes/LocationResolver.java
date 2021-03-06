package com.ej.rovadiahyosefcalendar.classes;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sCurrentLocationName;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sGPSLocationServiceIsDisabled;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sLongitude;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sNetworkLocationServiceIsDisabled;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.ej.rovadiahyosefcalendar.activities.MainActivity;

import org.geonames.WebService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import us.dustinj.timezonemap.TimeZoneMap;

public class LocationResolver extends Thread {

    private final Context mContext;
    private final Activity mActivity;
    private final Geocoder mGeocoder;
    private final SharedPreferences mSharedPreferences;

    public LocationResolver(Context context, Activity activity) {
        mContext = context;
        mActivity = activity;
        mGeocoder = new Geocoder(mContext);
        mSharedPreferences = mContext.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
    }

    /**
     * This method gets the devices last known latitude and longitude. It will ask for permission
     * if we do not have it, and it will alert the user if location services is disabled.
     * <p>
     * As of Android 11 (API 30) there is a more accurate way of getting the current location of the
     * device, however, the process is slower as it needs to actually make a call to the GPS service
     * if the location has not been updated recently.
     * <p>
     * This method will now first check if the user wants to use a zip code. If the user entered a
     * zip code before, the app will use that zip code for as the current location.
     */
    @SuppressWarnings("BusyWait")
    public void acquireLatitudeAndLongitude() {
        if (mSharedPreferences.getBoolean("useZipcode", false)) {
            getLatitudeAndLongitudeFromZipcode();
        } else {
            if (ActivityCompat.checkSelfPermission(mContext, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mActivity, new String[]{ACCESS_FINE_LOCATION}, 1);
            } else {
                try {
                    LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
                    if (locationManager != null) {
                        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                            sNetworkLocationServiceIsDisabled = true;
                        }
                        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            sGPSLocationServiceIsDisabled = true;
                        }
                        LocationListener locationListener = new LocationListener() {
                            @Override
                            public void onLocationChanged(@NonNull Location location) { }
                            @Override
                            public void onProviderEnabled(@NonNull String provider) { }
                            @Override
                            public void onProviderDisabled(@NonNull String provider) { }
                            @Override
                            public void onStatusChanged(String provider, int status, Bundle extras) { }
                        };
                        if (!sNetworkLocationServiceIsDisabled) {
                            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null);
                        }
                        if (!sGPSLocationServiceIsDisabled) {
                            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
                        }
                        if (!sNetworkLocationServiceIsDisabled || !sGPSLocationServiceIsDisabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {//newer implementation
                                locationManager.getCurrentLocation(LocationManager.NETWORK_PROVIDER,
                                        null, Runnable::run,
                                        location -> {
                                            if (location != null) {
                                                sLatitude = location.getLatitude();
                                                sLongitude = location.getLongitude();
                                            }
                                        });
                                locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER,
                                        null, Runnable::run,
                                        location -> {
                                            if (location != null) {
                                                sLatitude = location.getLatitude();
                                                sLongitude = location.getLongitude();
                                            }
                                        });
                                long tenSeconds = System.currentTimeMillis() + 10000;
                                while ((sLatitude == 0 && sLongitude == 0) && System.currentTimeMillis() < tenSeconds) {
                                    Thread.sleep(0);//we MUST wait for the location data to be set or else the app will crash
                                }
                                if (sLatitude == 0 && sLongitude == 0) {//if 10 seconds passed and we still don't have the location, use the older implementation
                                    Location location;//location might be old
                                    if (!sNetworkLocationServiceIsDisabled) {
                                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                    } else {
                                        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                    }
                                    if (location != null) {
                                        sLatitude = location.getLatitude();
                                        sLongitude = location.getLongitude();
                                    }
                                }
                            } else {//older implementation
                                Location location = null;//location might be old
                                if (!sNetworkLocationServiceIsDisabled) {
                                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                }
                                if (location != null) {
                                    sLatitude = location.getLatitude();
                                    sLongitude = location.getLongitude();
                                }
                                if (!sGPSLocationServiceIsDisabled) {
                                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                }
                                if (location != null && (sLatitude == 0 && sLongitude == 0)) {
                                    sLatitude = location.getLatitude();
                                    sLongitude = location.getLongitude();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        resolveCurrentLocationName();
    }

    /**
     * Resolves the current location name to be a latitude and longitude if mCurrentLocationName is empty
     * @see com.ej.rovadiahyosefcalendar.activities.MainActivity#sCurrentLocationName
     */
    public void resolveCurrentLocationName() {
        sCurrentLocationName = getLocationAsName();
        if (sCurrentLocationName.isEmpty()) {
            if (sLatitude != 0 && sLongitude != 0) {
                String lat = String.valueOf(sLatitude);
                if (lat.contains("-")) {
                    lat = lat.substring(0, 5);
                } else {
                    lat = lat.substring(0, 4);
                }
                String longitude = String.valueOf(sLongitude);
                if (longitude.contains("-")) {
                    longitude = longitude.substring(0, 5);
                } else {
                    longitude = longitude.substring(0, 4);
                }
                sCurrentLocationName = "Lat: " + lat + " Long: " + longitude;
            }
        }
    }

    /**
     * This method uses the Geocoder class to try and get the current location's name. I have
     * tried to make my results similar to the zmanim app by JGindin on the Play Store. In america,
     * it will get the current location by state and city. Whereas, in other areas of the world, it
     * will get the country and the city.
     *
     * @return a string containing the name of the current city and state/country that the user is located in.
     * @see Geocoder
     */
    public String getLocationAsName() {
        StringBuilder result = new StringBuilder();
        List<Address> addresses = null;
        try {
            addresses = mGeocoder.getFromLocation(sLatitude, sLongitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses != null && addresses.size() > 0) {

            String city = addresses.get(0).getLocality();
            if (city != null) {
                result.append(city).append(", ");
            }

            String state = addresses.get(0).getAdminArea();
            if (state != null) {
                result.append(state);
            }

            if (result.toString().endsWith(", ")) {
                result.deleteCharAt(result.length() - 2);
            }

            if (city == null && state == null) {
                String country = addresses.get(0).getCountryName();
                result.append(country);
            }
        }
        return result.toString().trim();
    }

    /**
     * This method uses the Geocoder class to get a latitude and longitude coordinate from the user
     * specified zip code. If it can not find am address it will make a toast saying that an error
     * occurred.
     *
     * @see Geocoder
     */
    public void getLatitudeAndLongitudeFromZipcode() {
        String zipcode = mSharedPreferences.getString("Zipcode", "");
        List<Address> address = null;
        try {
            address = mGeocoder.getFromLocationName(zipcode, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if ((address != null ? address.size() : 0) > 0) {
            Address first = address.get(0);
            sLatitude = first.getLatitude();
            sLongitude = first.getLongitude();
            sCurrentLocationName = getLocationAsName();
            mSharedPreferences.edit().putLong("oldLat", Double.doubleToRawLongBits(sLatitude)).apply();
            mSharedPreferences.edit().putLong("oldLong", Double.doubleToRawLongBits(sLongitude)).apply();
        } else {
            getOldZipcodeLocation();
        }
    }

    /**
     * This method retrieves the old location data from the devices storage if it has already been
     * setup beforehand.
     *
     * @see #getLatitudeAndLongitudeFromZipcode()
     */
    public void getOldZipcodeLocation() {
        double oldLat = Double.longBitsToDouble(mSharedPreferences.getLong("oldLat", 0));
        double oldLong = Double.longBitsToDouble(mSharedPreferences.getLong("oldLong", 0));

        if (oldLat == sLatitude && oldLong == sLongitude) {
            Toast.makeText(mContext, "Unable to change location, using old location.", Toast.LENGTH_LONG).show();
        }

        if (oldLat != 0 && oldLong != 0) {
            sLatitude = oldLat;
            sLongitude = oldLong;
        } else {
            Toast.makeText(mContext, "An error occurred getting zipcode coordinates", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * This method uses the TimeZoneMap to get the current timezone ID based on the latitude and longitude of the device.
     * If the latitude and longitude are not known, it will use the default timezone ID.
     */
    public void setTimeZoneID() {
        if (sLatitude != 0 && sLongitude != 0) {
            TimeZoneMap timeZoneMap = TimeZoneMap.forRegion(
                    Math.floor(sLatitude), Math.floor(sLongitude),
                    Math.ceil(sLatitude), Math.ceil(sLongitude));//trying to avoid using the forEverywhere() method
            MainActivity.sCurrentTimeZoneID = Objects.requireNonNull(timeZoneMap.getOverlappingTimeZone(sLatitude, sLongitude)).getZoneId();
        } else {
            MainActivity.sCurrentTimeZoneID = TimeZone.getDefault().getID();
        }
    }

    public void getElevationFromWebService() throws IOException {
        WebService.setUserName("Elyahu41");
        ArrayList<Integer> elevations = new ArrayList<>();
        int sum = 0;
        int size;
        try {
            int e1 = WebService.srtm3(sLatitude, sLongitude);
            if (e1 > 0) {
                elevations.add(e1);
            }
            int e2 = WebService.astergdem(sLatitude, sLongitude);
            if (e2 > 0) {
                elevations.add(e2);
            }
            int e3 = WebService.gtopo30(sLatitude, sLongitude);
            if (e3 > 0) {
                elevations.add(e3);
            }

            for (int e : elevations) {
                sum += e;
            }
            size = elevations.size();
            if (size == 0) {
                size = 1;//edge case if no elevation data is available
            }
        } catch (NumberFormatException ex) {//and error occurred getting the elevation, probably because too many requests were made
            try {
                WebService.setUserName("graviton57");//another user api key that I found online
                int e1 = WebService.srtm3(sLatitude, sLongitude);
                if (e1 > 0) {
                    elevations.add(e1);
                }
                int e2 = WebService.astergdem(sLatitude, sLongitude);
                if (e2 > 0) {
                    elevations.add(e2);
                }
                int e3 = WebService.gtopo30(sLatitude, sLongitude);
                if (e3 > 0) {
                    elevations.add(e3);
                }

                for (int e : elevations) {
                    sum += e;
                }
                size = elevations.size();
                if (size == 0) {
                    size = 1;//edge case if no elevation data is available
                }
            } catch (NumberFormatException ex1) {
                Toast.makeText(mContext, "Elevation not updated. Too many requests. Try again later.", Toast.LENGTH_LONG).show();
                ex.printStackTrace();
                ex1.printStackTrace();
                return;
            }
        }
        mSharedPreferences.edit().putString("elevation" + sCurrentLocationName, String.valueOf(sum / size)).apply();
    }

    @Override
    public void run() {
        try {
            getElevationFromWebService();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
