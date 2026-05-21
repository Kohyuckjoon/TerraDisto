package com.terra.terradisto.ui.distoconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.terra.terradisto.R;

import java.util.List;

import ch.leica.sdk.Devices.Device;
import ch.leica.sdk.Types;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.VH> {

    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);
    }

    private List<Device> items;
    private final OnDeviceClickListener listener;

    public DeviceListAdapter(List<Device> items, OnDeviceClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<Device> newItems) {
        this.items = newItems;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Device d = items.get(position);
        h.name.setText(d.getDeviceName());
        h.sub.setText(d.getDeviceID() + " • " + d.getConnectionType().name());

        // 간단한 아이콘 매핑
        int iconRes = R.drawable.ic_bluetooth; // 기본
        if (d.getConnectionType() == Types.ConnectionType.wifiAP
                || d.getConnectionType() == Types.ConnectionType.wifiHotspot) {
            iconRes = R.drawable.ic_wifi; // 프로젝트에 맞는 아이콘 사용
        }
        h.icon.setImageResource(iconRes);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDeviceClick(d);
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        TextView sub;
        VH(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.ivIcon);
            name = itemView.findViewById(R.id.tvName);
            sub  = itemView.findViewById(R.id.tvSub);
        }
    }
}
