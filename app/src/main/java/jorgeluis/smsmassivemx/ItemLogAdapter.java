package jorgeluis.smsmassivemx;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
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

        return convertView;
    }

}
