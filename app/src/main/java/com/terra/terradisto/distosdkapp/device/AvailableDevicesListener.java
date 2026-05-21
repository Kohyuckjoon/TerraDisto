package com.terra.terradisto.distosdkapp.device;


import java.util.List;

import ch.leica.sdk.Devices.Device;


public interface AvailableDevicesListener {
    void onAvailableDeviceFound();
    void onAvailableDevicesChanged(List<Device> availableDevices);
}
