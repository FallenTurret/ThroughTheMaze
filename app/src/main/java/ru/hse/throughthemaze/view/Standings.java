package ru.hse.throughthemaze.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import ru.hse.throughthemaze.R;

import java.util.List;
import java.util.Objects;

public class Standings extends BaseAdapter {

    private Context mContext;
    private LayoutInflater inflater;
    private List<Item> items;

    public Standings(Context context, List<Item> items) {
        this.mContext = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (inflater == null) {
            inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        if (convertView == null) {
            convertView = Objects.requireNonNull(inflater).inflate(R.layout.standings, parent, false);
            holder = new ViewHolder();
            holder.color = convertView.findViewById(R.id.color);
            holder.wins = convertView.findViewById(R.id.wins);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Item m = items.get(position);
        holder.color.setText(m.color);
        holder.wins.setText(m.wins);

        return convertView;
    }

    static class ViewHolder {
        public TextView color;
        public TextView wins;
    }
}