package com.astin.moneymaster.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.astin.moneymaster.R;

import java.util.List;

public class AppListAdapter extends BaseAdapter {
    private Context context;
    private List<ApplicationInfo> apps;
    private PackageManager packageManager;

    public AppListAdapter(Context context, List<ApplicationInfo> apps, PackageManager packageManager) {
        this.context = context;
        this.apps = apps;
        this.packageManager = packageManager;
    }

    @Override
    public int getCount() {
        return apps.size();
    }

    @Override
    public Object getItem(int position) {
        return apps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        ImageView icon;
        TextView name;
        TextView packageName;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.app_icon);
            holder.name = convertView.findViewById(R.id.app_name);
            holder.packageName = convertView.findViewById(R.id.app_package);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ApplicationInfo app = apps.get(position);
        holder.icon.setImageDrawable(app.loadIcon(packageManager));
        holder.name.setText(packageManager.getApplicationLabel(app));
        holder.packageName.setText(app.packageName.equals("none") ? "No app selected" : app.packageName);

        return convertView;
    }
}

