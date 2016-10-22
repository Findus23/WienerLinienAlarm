package aenadon.wienerlinienalarm;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import aenadon.wienerlinienalarm.models.Alarm;
import io.realm.Realm;
import io.realm.RealmResults;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class AlarmSetterActivity extends AppCompatActivity {

    private int ALARM_MODE = C.ALARM_ONETIME;

    Vibrator v;

    private static int[] chosenDate = null; // Chosen Date
    private static int[] chosenTime = null; // arr[0] = hours; arr[1] = minutes;
    private boolean[] chosenDays = new boolean[7]; // true,true,false,false,false,false,true ==> Monday, Tuesday, Sunday

    private String chosenRingtone = null;               // standard: no sound
    private int chosenVibratorMode = C.VIBRATION_NONE;  // standard: no vibration

    private String[] pickedStationData = null; // {stationName, stationDir, stationId, h.getArrayIndex()}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_setter);

        // if device has no vibrator, hide the vibration choice
        v = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        if (!v.hasVibrator()) {
            findViewById(R.id.choose_vibration_container).setVisibility(View.GONE);
        }

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Realm.init(this);

        new GetApiFiles(this).execute(); // get CSV files/check for updates on them
    }

    // handling all the click events from the view
    public void onClickHandler(View view) {
        switch (view.getId()) {
            case R.id.radio_frequency_one_time:
                pickAlarmFrequency(C.ALARM_ONETIME);
                break;
            case R.id.radio_frequency_recurring:
                pickAlarmFrequency(C.ALARM_RECURRING);
                break;
            case R.id.choose_date_button:
            case R.id.choose_date_text:
                pickDate();
                break;
            case R.id.choose_time_button:
            case R.id.choose_time_text:
                pickTime();
                break;
            case R.id.choose_days_button:
            case R.id.choose_days_text:
                pickDays();
                break;
            case R.id.choose_station_button:
            case R.id.choose_station_text:
                pickStation();
                break;
            case R.id.choose_ringtone_button:
            case R.id.choose_ringtone_text:
                pickRingtone();
                break;
            case R.id.choose_vibration_button:
            case R.id.choose_vibration_text:
                pickVibration();
                break;
            case R.id.fab_alarm:
                done();
                break;
        }
    }

    private void pickAlarmFrequency(int setTo) {
        if (ALARM_MODE == setTo) return; // if nothing changed, do nothing

        LinearLayout chooseDateContainer = (LinearLayout) findViewById(R.id.choose_date_container);
        LinearLayout chooseDaysContainer = (LinearLayout) findViewById(R.id.choose_days_container);
        // LinearLayout chooseTimeContainer; --> always on screen!

        switch (setTo) {
            case C.ALARM_ONETIME:   // hide the days+time chooser and show the date chooser
                chooseDaysContainer.setVisibility(View.GONE);
                // -- //
                chooseDateContainer.setVisibility(View.VISIBLE);
                break;
            case C.ALARM_RECURRING: // hide the date chooser and show the days+time chooser
                chooseDateContainer.setVisibility(View.GONE);
                // -- //
                chooseDaysContainer.setVisibility(View.VISIBLE);
                break;
        }
        ALARM_MODE = setTo; // in the end, set the mode as current mode
    }

    private void pickDate() {
        new DatePickerFragment().show(getFragmentManager(), "DatePickerDialog");
    }

    private void pickTime() {
        new TimePickerFragment().show(getFragmentManager(), "TimePickerDialog");
    }

    private void pickDays() {
        final String[] weekDayStrings = new String[]{
                getString(R.string.monday),
                getString(R.string.tuesday),
                getString(R.string.wednesday),
                getString(R.string.thursday),
                getString(R.string.friday),
                getString(R.string.saturday),
                getString(R.string.sunday),
        };

        final boolean[] tempChoices = chosenDays.clone();

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.alarm_recurring_dialog_expl))
                .setMultiChoiceItems(weekDayStrings, tempChoices, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        tempChoices[which] = isChecked;
                    }
                })
                .setPositiveButton(R.string.ok, dayDialogListener(tempChoices))
                .setNegativeButton(R.string.cancel, dayDialogListener(tempChoices))
                .show();
    }

    private DialogInterface.OnClickListener dayDialogListener(final boolean[] tempChoices) {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        chosenDays = tempChoices.clone(); // assign our tempChoices to our "persistent" choices

                        String selection;
                        if (!(chosenDays[0] || chosenDays[1] || chosenDays[2] || chosenDays[3] || chosenDays[4] || chosenDays[5] || chosenDays[6])) {
                            selection = getString(R.string.alarm_no_days_set);  // then say "no days selected"
                        } else if (!(chosenDays[0] || chosenDays[1] || chosenDays[2] || chosenDays[3] || chosenDays[4]) && (chosenDays[5] && chosenDays[6])) {
                            selection = getString(R.string.weekends);
                        } else if ((chosenDays[0] && chosenDays[1] && chosenDays[2] && chosenDays[3] && chosenDays[4]) && !(chosenDays[5] || chosenDays[6])) {
                            selection = getString(R.string.weekdays);
                        } else if (chosenDays[0] && chosenDays[1] && chosenDays[2] && chosenDays[3] && chosenDays[4] && chosenDays[5] && chosenDays[6]) {
                            selection = getString(R.string.everyday);
                        } else {
                            int selectedDays = 0;
                            for (int i = 0; i < 7; i++) {
                                if (chosenDays[i]) selectedDays++;
                            }
                            selection = getResources().getQuantityString(R.plurals.days_chosen, selectedDays, selectedDays); // else show the count of days chosen
                        }

                        TextView t = (TextView) findViewById(R.id.choose_days_text);
                        t.setText(selection);
                        dialog.dismiss();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.dismiss();
                        break;
                }
            }
        };
    }

    private void pickStation() {
        startActivityForResult(new Intent(this, StationPicker.class), C.REQUEST_STATION);
    }

    private void pickRingtone() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        this.startActivityForResult(intent, C.REQUEST_RINGTONE);
    }

    private void pickVibration() {
        final String[] vibrationModes = new String[]{
                getString(R.string.alarm_vibration_none),
                getString(R.string.alarm_vibration_short),
                getString(R.string.alarm_vibration_medium),
                getString(R.string.alarm_vibration_long),
        };

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.alarm_recurring_dialog_expl))
                .setItems(vibrationModes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chosenVibratorMode = which;
                        if (v.hasVibrator()) {
                            v.vibrate(C.VIBRATION_DURATION[which]);
                        }
                        vibrationModes[0] = getString(R.string.alarm_no_vibration_chosen); // if "none" is selected, display "(No vibration)"
                        ((TextView)findViewById(R.id.choose_vibration_text)).setText(vibrationModes[which]);
                    }
                })
                .show();
    }

    private void done() {
        // Errorcheck
        boolean isError = false;
        String errors = "";

        // Mode-specific checks
        if (ALARM_MODE == C.ALARM_ONETIME) {
            if (chosenDate == null) {
                isError = true;
                errors += getString(R.string.missing_info_date);
            }
            if (chosenDate != null && chosenTime != null) {
                Calendar now = Calendar.getInstance();
                Calendar date = Calendar.getInstance();
                date.set(chosenDate[0], chosenDate[1], chosenDate[2], chosenTime[0], chosenTime[1], 0); // 0 seconds
                if (date.compareTo(now) < 0) {
                    errors += getString(R.string.missing_info_past);
                }
            }
        } else if (ALARM_MODE == C.ALARM_RECURRING) {
            boolean noDays = true;
            for (boolean daySelected : chosenDays) {
                if (daySelected) { // if any day was set to true, no error
                    noDays = false;
                    break;
                }
            }
            if (noDays) {
                isError = true;
                errors += getString(R.string.missing_info_days);
            }
        }
        // General checks
        if (chosenTime == null) {
            isError = true;
            errors += getString(R.string.missing_info_time);
        }
        if (pickedStationData == null) {
            isError = true;
            errors += getString(R.string.missing_info_station);
        }
        // If error:
        if (isError) {
            AlertDialogs.missingInfo(this, errors);
            return;
            // if error, we're done here
        }

        Alarm newAlarm = new Alarm();
        newAlarm.setAlarmMode(ALARM_MODE);
        switch (ALARM_MODE) {
            case C.ALARM_ONETIME:
                newAlarm.setOneTimeAlarmYear(chosenDate[0]);
                newAlarm.setOneTimeAlarmMonth(chosenDate[1]);
                newAlarm.setOneTimeAlarmDay(chosenDate[2]);
                break;
            case C.ALARM_RECURRING:
                newAlarm.setRecurringChosenDays(chosenDays);
                break;
        }
        newAlarm.setAlarmHour(chosenTime[0]);
        newAlarm.setAlarmMinute(chosenTime[1]);

        newAlarm.setChosenRingtone(chosenRingtone);
        newAlarm.setChosenVibrationDuration(C.VIBRATION_DURATION[chosenVibratorMode]);

        // {stationName, stationDir, stationId, h.getArrayIndex()}
        newAlarm.setStationName(pickedStationData[0]);
        newAlarm.setStationDirection(pickedStationData[1]);
        newAlarm.setStationId(pickedStationData[2]);
        newAlarm.setStationArrayIndex(Integer.parseInt(pickedStationData[3]));

        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.copyToRealm(newAlarm);
        realm.commitTransaction();

        // TODO REMOVE THIS
        RealmResults<Alarm> x = realm.where(Alarm.class).findAll();
        for (Alarm a : x) {
            Log.d("test", a.getStationName());
        }


        setResult(Activity.RESULT_OK, new Intent().putExtra("mode", ALARM_MODE));
        finish(); // we're done here.
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case C.REQUEST_STATION:
                    pickedStationData = data.getStringArrayExtra("stationInfo");
                    ((TextView) findViewById(R.id.choose_station_text)).setText(pickedStationData[0] + "\n" + pickedStationData[1]);
                    break;
                case C.REQUEST_RINGTONE:
                    Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

                    chosenRingtone = (uri != null) ? uri.toString() : null;

                    Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
                    String title = (chosenRingtone == null) ?
                            getString(R.string.alarm_no_ringtone_chosen) :
                            ringtone.getTitle(this);

                    ((TextView)findViewById(R.id.choose_ringtone_text)).setText(title);
                    break;
            }
        }
    }

    class GetApiFiles extends AsyncTask<Void, Void, Boolean> {

        ProgressDialog warten;
        Context mContext;

        GetApiFiles(Context c) {
            mContext = c;
        }

        @Override
        protected void onPreExecute() {
            warten = new ProgressDialog(mContext);
            warten.setCancelable(false);
            warten.setIndeterminate(true);
            warten.setMessage(getString(R.string.updating_stations));
            warten.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Response<ResponseBody> versionResponse = RetrofitInfo.getCSVInfo().create(RetrofitInfo.CSVCalls.class).getVersionCSV().execute(); // check file version
                String versionResponseString = versionResponse.body().string();

                if (!versionResponse.isSuccessful()) return false;
                File csv = new File(mContext.getFilesDir(), C.CSV_FILENAME);
                String csvString = C.getCSVfromFile(mContext);
                if (csv.exists() && csvString != null) {
                    String x = csvString.split(C.CSV_FILE_SEPARATOR)[C.CSV_PART_VERSION];
                    if (x.equals(versionResponseString))
                        return true; // if we already have the latest version, skip the redownload
                }

                Response<ResponseBody> haltestellenResponse = RetrofitInfo.getCSVInfo().create(RetrofitInfo.CSVCalls.class).getHaltestellenCSV().execute();
                Response<ResponseBody> steigResponse = RetrofitInfo.getCSVInfo().create(RetrofitInfo.CSVCalls.class).getSteigeCSV().execute();

                if (!haltestellenResponse.isSuccessful() || !steigResponse.isSuccessful()) {
                    throw new IOException("At least one server response not successful " +
                            "(" + haltestellenResponse.code() + "/" + steigResponse.code() + ")"); // [...] (403/403)
                } else {
                    if (csv.exists()) csv.delete();
                    String combined =
                            versionResponseString      // last update date
                                    + C.CSV_FILE_SEPARATOR +             // separator
                                    haltestellenResponse.body().string() // haltestellen CSV
                                    + C.CSV_FILE_SEPARATOR +             // separator
                                    steigResponse.body().string();       // steige CSV

                    FileOutputStream fos = new FileOutputStream(csv);
                    fos.write(combined.getBytes());
                    fos.close();
                    return true;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            warten.dismiss();

            if (!success) {
                AlertDialogs.serverNotAvailable(mContext);
                findViewById(R.id.choose_station_button).setEnabled(false); // disable station picker
            }
        }
    }

    public static class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute, true);
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            chosenTime = new int[]{hourOfDay, minute};
            TextView t = (TextView) getActivity().findViewById(R.id.choose_time_text);
            t.setText(String.format(Locale.ENGLISH, "%02d:%02d", hourOfDay, minute));
        }
    }

    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            DatePickerDialog d = new DatePickerDialog(getActivity(), this, year, month, day);
            d.getDatePicker().setMinDate(c.getTimeInMillis());
            return d;
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int day) {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day);
            Date chosenDateAtMidnight = cal.getTime();
            String formattedDate = DateFormat.getDateInstance().format(chosenDateAtMidnight);

            chosenDate = new int[]{year, month, day};

            TextView t = (TextView) getActivity().findViewById(R.id.choose_date_text);
            t.setText(formattedDate);
        }
    }
}
