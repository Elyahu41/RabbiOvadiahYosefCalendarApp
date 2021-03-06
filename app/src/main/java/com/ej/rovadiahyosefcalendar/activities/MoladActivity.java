package com.ej.rovadiahyosefcalendar.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.CustomDatePickerDialog;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MoladActivity extends AppCompatActivity {

    private TextView mCurrentMonths;
    private TextView mMoladAnnouncementTime;
    private TextView mMoladDate;
    private TextView mMoladDate7Days;
    private TextView mMoladDate15Days;
    private final Calendar mUserChosenDate = Calendar.getInstance();
    private final JewishCalendar mJewishCalendar = new JewishCalendar();
    private final SimpleDateFormat mSDF = new SimpleDateFormat("EEE MMM d h:mm:ss aa", Locale.getDefault());
    private final String[] mHebrewMonths = { "Nissan", "Iyar", "Sivan", "Tammuz", "Av",
            "Elul", "Tishrei", "Cheshvan", "Kislev", "Tevet", "Shevat", "Adar", "Adar II", "Adar I"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_molad);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mCurrentMonths = findViewById(R.id.currentMonths);
        mMoladAnnouncementTime = findViewById(R.id.moladAnnouncementTime);
        mMoladDate = findViewById(R.id.moladDate);
        mMoladDate7Days = findViewById(R.id.moladDate7Days);
        mMoladDate15Days = findViewById(R.id.moladDate15Days);

        updateMoladDates();

        Button moladButton = findViewById(R.id.molad_button);
        DatePickerDialog dialog = createDialog();

        moladButton.setOnClickListener(v -> {
            dialog.updateDate(mUserChosenDate.get(Calendar.YEAR),
                    mUserChosenDate.get(Calendar.MONTH),
                    mUserChosenDate.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });


    }

    private DatePickerDialog createDialog() {
        DatePickerDialog.OnDateSetListener onDateSetListener = (view, year, month, day) -> {
            mUserChosenDate.set(year, month, day);
            mJewishCalendar.setDate(mUserChosenDate);
            updateMoladDates();
        };

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return new CustomDatePickerDialog(this, onDateSetListener,
                    mUserChosenDate.get(Calendar.YEAR),
                    mUserChosenDate.get(Calendar.MONTH),
                    mUserChosenDate.get(Calendar.DAY_OF_MONTH),
                    mJewishCalendar);
        } else {
            return new DatePickerDialog(this, onDateSetListener,
                    mUserChosenDate.get(Calendar.YEAR),
                    mUserChosenDate.get(Calendar.MONTH),
                    mUserChosenDate.get(Calendar.DAY_OF_MONTH));
        }
    }

    private void updateMoladDates() {
        String currentHebrewMonth;
        if (mJewishCalendar.isJewishLeapYear() && mJewishCalendar.getJewishMonth() == JewishDate.ADAR) {
            currentHebrewMonth = mHebrewMonths[13]; // return Adar I, not Adar in a leap year
        } else {
            currentHebrewMonth = mHebrewMonths[mJewishCalendar.getJewishMonth() - 1];
        }
        String currentMonths = mJewishCalendar.getGregorianCalendar().getDisplayName(
                Calendar.MONTH, Calendar.LONG, Locale.getDefault()) + " "
                + mJewishCalendar.getGregorianYear()
                + " / "
                + currentHebrewMonth + " " + mJewishCalendar.getJewishYear();
        mCurrentMonths.setText(currentMonths);

        JewishDate molad = mJewishCalendar.getMolad();
        int moladHours = molad.getMoladHours();
        int moladMinutes = molad.getMoladMinutes();
        int moladChalakim = molad.getMoladChalakim();

        String moladTime = moladHours + "h:" + moladMinutes + "m and " + moladChalakim + " Chalakim";

        mMoladAnnouncementTime.setText(moladTime);
        mMoladDate.setText(mSDF.format(mJewishCalendar.getMoladAsDate()));
        mMoladDate7Days.setText(mSDF.format(mJewishCalendar.getTchilasZmanKidushLevana7Days()));
        mMoladDate15Days.setText(mSDF.format(mJewishCalendar.getSofZmanKidushLevana15Days()));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}