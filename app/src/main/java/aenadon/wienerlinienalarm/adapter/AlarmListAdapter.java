package aenadon.wienerlinienalarm.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import aenadon.wienerlinienalarm.R;
import aenadon.wienerlinienalarm.models.Alarm;
import aenadon.wienerlinienalarm.utils.Const;
import aenadon.wienerlinienalarm.utils.RealmUtils;
import aenadon.wienerlinienalarm.utils.StringDisplay;
import io.realm.RealmResults;

public class AlarmListAdapter extends BaseAdapter {

    private String LOG_TAG = AlarmListAdapter.class.getSimpleName();

    private Context ctx;
    private int alarmModePage;

    private RealmResults<Alarm> alarms;

    // Getter for alarm list
    public RealmResults<Alarm> getAlarms() {
        return alarms;
    }

    public AlarmListAdapter(Context c, int alarmModePage) {
        ctx = c;
        this.alarmModePage = alarmModePage;

        alarms = RealmUtils.getAlarms(c, alarmModePage);
    }

    @Override public Object getItem(int position) {
        return null;
    }
    @Override public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getCount() {
        return alarms.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        LayoutInflater infl = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = infl.inflate(R.layout.alarm_list_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.date = (TextView) convertView.findViewById(R.id.alarm_list_date);
            viewHolder.time = (TextView) convertView.findViewById(R.id.alarm_list_time);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        Alarm alarmElement = alarms.get(position);

        String date;
        String time = StringDisplay.getTime(alarmElement.getAlarmHour(), alarmElement.getAlarmMinute());

        switch (alarmModePage) {
            case Const.ALARM_ONETIME:
                date = StringDisplay.getOnetimeDate(alarmElement.getOneTimeAlarmYear(), alarmElement.getOneTimeAlarmMonth(), alarmElement.getOneTimeAlarmDay());
                break;
            case Const.ALARM_RECURRING:
                date = StringDisplay.getRecurringDays(ctx, alarmElement.getRecurringChosenDays());
                break;
            default:
                Log.e(LOG_TAG, "Page index out of range");
                return null;
        }

        viewHolder.date.setText(date);
        viewHolder.time.setText(time);

        return convertView;
    }

    private class ViewHolder {
        TextView date;      // @+id/alarm_list_date
        TextView time;      // @+id/alarm_list_time
    }
}