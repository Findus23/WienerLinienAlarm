package aenadon.wienerlinienalarm.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aenadon.wienerlinienalarm.R;
import aenadon.wienerlinienalarm.enums.Weekday;
import aenadon.wienerlinienalarm.models.alarm.LegacyAlarm;
import aenadon.wienerlinienalarm.utils.Const;
import aenadon.wienerlinienalarm.utils.RealmService;
import aenadon.wienerlinienalarm.utils.StringDisplay;
import io.realm.RealmResults;

public class AlarmListAdapter extends BaseAdapter {

    private final Context ctx;
    private final int alarmModePage;

    private final RealmResults<LegacyAlarm> alarms;

    public AlarmListAdapter(Context c, int alarmModePage) {
        ctx = c;
        this.alarmModePage = alarmModePage;

        alarms = RealmService.getAlarms(c, alarmModePage);
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

        if (convertView == null) {
            LayoutInflater infl = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infl.inflate(R.layout.list_item_alarm, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.date = (TextView) convertView.findViewById(R.id.alarm_list_date);
            viewHolder.time = (TextView) convertView.findViewById(R.id.alarm_list_time);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        LegacyAlarm alarmElement = alarms.get(position);

        String date;
        String time = StringDisplay.getTime(alarmElement.getAlarmHour(), alarmElement.getAlarmMinute());

        switch (alarmModePage) {
            case Const.ALARM_ONETIME:
                date = StringDisplay.getOnetimeDate(alarmElement.getOneTimeAlarmYear(), alarmElement.getOneTimeAlarmMonth(), alarmElement.getOneTimeAlarmDay());
                break;
            case Const.ALARM_RECURRING:
                date = StringDisplay.getRecurringDays(ctx, setFromArray(alarmElement.getRecurringChosenDays()));
                break;
            default:
                throw new Error("Page index out of range");
        }

        viewHolder.date.setText(date);
        viewHolder.time.setText(time);

        return convertView;
    }

    // TODO remove ASAP
    @Deprecated
    private Set<Weekday> setFromArray(boolean[] weekdayArray) {
        List<Weekday> allWeekdaysList = Arrays.asList(Weekday.values());
        Set<Weekday> weekdaySet = new HashSet<>();
        for (int i = 0; i < allWeekdaysList.size(); i++) {
            if (weekdayArray[i]) {
                weekdaySet.add(allWeekdaysList.get(i));
            }
        }
        return weekdaySet;
    }

    private class ViewHolder {
        TextView date;      // @+id/alarm_list_date
        TextView time;      // @+id/alarm_list_time
    }
}
