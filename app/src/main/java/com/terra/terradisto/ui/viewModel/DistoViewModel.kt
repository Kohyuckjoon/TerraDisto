package com.terra.terradisto.ui.viewModel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.terra.terradisto.distosdkapp.device.YetiDeviceController

data class SurveyProject(
    val id: String,
    val name: String,
    val location: String,
    val count: Int
)

class DistoViewModel : ViewModel(), YetiDeviceController.YetiDataListener {
    private val _projects = MutableStateFlow(listOf(
        SurveyProject("1", "MH-Master", "서울시 강남구", 12),
        SurveyProject("2", "도봉구 조사", "서울시 도봉구", 8),
        SurveyProject("3", "성북구 현장", "서울시 성북구", 12)
    ))

    // 시스템 블루투스 활성화 상태 추가
    private val _isBluetoothEnabled = mutableStateOf(false) // 초기값
    val isBluetoothEnabled: State<Boolean> = _isBluetoothEnabled

    // 블루투스 상태 업데이트 함수
    fun updateBluetoothEnabled(isEnabled: Boolean) {
        _isBluetoothEnabled.value = isEnabled
    }
    val projects: StateFlow<List<SurveyProject>> = _projects

    // 입력 필드 상태
    var manholName = mutableStateOf("")
    var depthValue = mutableStateOf("")
    var diameterValue = mutableStateOf("")

    // 초기값을 false로 설정하고 연결 시점에 업데이트 되도록 변경
    private val _isDistoConnected = mutableStateOf(false)
    val isDistoConnected: State<Boolean> = _isDistoConnected

    // 외부(MainActivity)에서 연결 상태를 강제로 업데이트하기 위한 함수
    fun updateConnectionState(isConected: Boolean) {
        _isDistoConnected.value = isConected
    }

    fun onSaveData() {
        // 저장 로직 구현
    }

    override fun onBasicMeasurements_Received(basicData: YetiDeviceController.BasicData?) {
        basicData?.let {
            // Disto에서 측정된 거리값을 depthValue 상태에 즉시 반영
            depthValue.value = it.distance
        }
    }

    // 아래는 인터페이스 유지를 위한 빈 구현
    override fun onP2PMeasurements_Received(p2pData: YetiDeviceController.P2PData?) {}
    override fun onQuaternionMeasurement_Received(quaternionData: YetiDeviceController.QuaternionData?) {}
    override fun onAccRotationMeasurement_Received(accRotatonMeasurement: YetiDeviceController.AccRotData?) {}
    override fun onMagnetometerMeasurement_Received(magnetometerData: YetiDeviceController.MagnetometerData?) {}
    override fun onDistocomTransmit_Received(data: String?) {}
    override fun onDistocomEvent_Received(data: String?) {}
    override fun onBrand_Received(data: String?) {}
    override fun onAPPSoftwareVersion_Received(data: String?) {}
    override fun onId_Received(data: String?) {}
    override fun onEDMSoftwareVersion_Received(data: String?) {}
    override fun onFTASoftwareVersion_Received(data: String?) {}
    override fun onAPPSerial_Received(data: String?) {}
    override fun onEDMSerial_Received(data: String?) {}
    override fun onFTASerial_Received(data: String?) {}
    override fun onModel_Received(data: String?) {}
}