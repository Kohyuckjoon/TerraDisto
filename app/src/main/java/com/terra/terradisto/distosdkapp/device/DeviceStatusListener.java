package com.terra.terradisto.distosdkapp.device;

import ch.leica.sdk.Devices.Device;

/**
 * 컨트롤러가 UI 쪽에 연결/상태/에러를 전달하기 위한 콜백.
 * 액티비티/프래그먼트가 필요 시 구현하거나, 필요 없으면 NO-OP로 대체할 수 있습니다.
 */
public interface DeviceStatusListener {
    void onConnectionStateChanged(String deviceId, Device.ConnectionState state);
    void onStatusChange(String status);
    void onReconnect();
    void onError(int code, String message);
}

