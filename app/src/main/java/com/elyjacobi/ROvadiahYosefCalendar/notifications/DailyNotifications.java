package com.elyjacobi.ROvadiahYosefCalendar.notifications;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static com.elyjacobi.ROvadiahYosefCalendar.activities.MainActivity.SHARED_PREF;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.elyjacobi.ROvadiahYosefCalendar.classes.JewishDateInfo;
import com.elyjacobi.ROvadiahYosefCalendar.R;
import com.elyjacobi.ROvadiahYosefCalendar.activities.MainActivity;
import com.kosherjava.zmanim.ComplexZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DailyNotifications extends BroadcastReceiver {

    private static int MID = 50;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        JewishDateInfo jewishDateInfo = new JewishDateInfo(
                sp.getBoolean("inIsrael",false), true);
        if (sp.getBoolean("isSetup",false)) {
            ComplexZmanimCalendar c = new ComplexZmanimCalendar(new GeoLocation(
                    sp.getString("name", ""),
                    Double.longBitsToDouble(sp.getLong("lat", 0)),
                    Double.longBitsToDouble(sp.getLong("long", 0)),
                    TimeZone.getTimeZone(sp.getString("timezoneID", ""))));

            if (!jewishDateInfo.getSpecialDay().isEmpty()) {
                long when = c.getSunrise().getTime();
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel("Jewish Special Day",
                            "Daily Notifications",
                            NotificationManager.IMPORTANCE_DEFAULT);
                    channel.setDescription("This notification will check daily if there is a " +
                            "special jewish day and display it at sunrise.");
                    channel.enableLights(true);
                    channel.enableVibration(true);
                    notificationManager.createNotificationChannel(channel);
                }

                Intent notificationIntent = new Intent(context, MainActivity.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                        notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                if (sp.getString("lastKnownDay","").equals(
                        jewishDateInfo.getJewishDate())) {//We only want 1 notification a day.
                    NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(context,
                            "Jewish Special Day").setSmallIcon(R.drawable.calendar_foreground)
                            .setContentTitle("Jewish Special Day")
                            .setContentText("Today is " + jewishDateInfo.getSpecialDay())
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .setBigContentTitle("Jewish Special Day")
                                    .setSummaryText("Great Neck, NY"))
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setLights(Color.BLUE, 500, 500)
                            .setSound(alarmSound)
                            .setAutoCancel(true)
                            .setWhen(when)
                            .setContentIntent(pendingIntent);
                    notificationManager.notify(MID, mNotifyBuilder.build());
                    MID++;
                }
                sp.edit().putString("lastKnownDay", jewishDateInfo.getJewishDate()).apply();
            }
            updateAlarm(context, c);
        }
    }

    private void updateAlarm(Context context, ComplexZmanimCalendar c) {
        Calendar calendar = Calendar.getInstance();
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        calendar.setTimeInMillis(c.getSunrise().getTime());
        if (calendar.getTime().compareTo(new Date()) < 0) {
            calendar.add(Calendar.DATE, 1);
        }
        PendingIntent dailyPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                0, new Intent(context.getApplicationContext(), DailyNotifications.class), 0);
        am.cancel(dailyPendingIntent);
        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), dailyPendingIntent);
    }
}