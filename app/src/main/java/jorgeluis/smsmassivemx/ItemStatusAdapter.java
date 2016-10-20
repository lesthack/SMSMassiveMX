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
 * Created by lesthack on 19/10/16.
 */

public class ItemStatusAdapter extends ArrayAdapter<ItemStatus>{
    public ItemStatusAdapter(Context context, List<ItemStatus> objects){
        super(context, 0, objects);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // ¿Existe el view actual?
        if (null == convertView) {
            convertView = inflater.inflate(
                    R.layout.item_status,
                    parent,
                    false);
        }

        TextView TextItemName = (TextView) convertView.findViewById(R.id.item_status_name);
        TextView TextItemValue = (TextView) convertView.findViewById(R.id.item_status_value);

        ItemStatus item = getItem(position);

        TextItemName.setText(item.getItemName());
        TextItemValue.setText(item.getItemValueStr());
        if(item.getItemValue()){
            TextItemValue.setTextColor(getContext().getColor(R.color.colorOK));
        }
        else{
            TextItemValue.setTextColor(getContext().getColor(R.color.colorNoOK));
        }

        return convertView;
    }
}
