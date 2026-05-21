package com.terra.terradisto.ui.distoconnect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import ch.leica.sdk.Devices.Device

class DistoConnectViewModel : ViewModel() {
    // 검색된 기기 리스트 상태
    val availableDevices = mutableStateListOf<Device>()

    // 연결 상태 추가 (Compose에서 관찰 가능한 state)
    var isDistoConnected by mutableStateOf(false)
        private set

    // 기기 리스트 업데이트
    fun updateDeviceList(newDevices: List<Device>) {
        availableDevices.clear()
        availableDevices.addAll(newDevices)
    }

    // 연결 상태 업데이트 함수 추가
    fun updateConnectionStatus(connected: Boolean) {
        isDistoConnected = connected
    }
}