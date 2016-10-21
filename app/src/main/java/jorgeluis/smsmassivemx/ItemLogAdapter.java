package jorgeluis.smsmassivemx;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by lesthack on 20/10/16.
 */

public class ItemLogAdapter extends ArrayAdapter<ItemLog> {
    public ItemLogAdapter(Context context, List<ItemLog> objects){
        super(context, 0, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // ¿Existe el view actual?
        if (null == convertView) {
            convertView = inflater.inflate(
                    R.layout.item_log,
                    parent,
                    false);
        }

        TextView TextItemDate = (TextView) convertView.findViewById(R.id.item_log_date);
        TextView TextItemText = (TextView) convertView.findViewById(R.id.item_log_text);

        ItemLog item = getItem(position);

        TextItemDate.setText(item.getItemDate());
        TextItemText.setText(item.getItemText());

        switch(item.getItemType()){
            case 1: TextItemText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorLogSent)); break;
            case 2: TextItemText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorLogError)); break;
            case 3: TextItemText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorLogWarn)); break;
            default: TextItemText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorLog)); break;
        }

        return convertView;
    }

}
