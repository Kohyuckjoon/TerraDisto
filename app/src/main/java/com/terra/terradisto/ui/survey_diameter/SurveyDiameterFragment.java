package com.terra.terradisto.ui.survey_diameter;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.terra.terradisto.databinding.FragmentSurveyDiameterBinding;

// Leica SDK / 앱 내부 클래스들
import ch.leica.sdk.Devices.BleDevice;
import ch.leica.sdk.Devices.Device;

import com.terra.terradisto.distosdkapp.clipboard.Clipboard;
import com.terra.terradisto.distosdkapp.clipboard.InformationActivityData;
import com.terra.terradisto.distosdkapp.device.YetiDeviceController;
import ch.leica.sdk.ErrorHandling.ErrorObject;

public class SurveyDiameterFragment extends Fragment
        implements YetiDeviceController.YetiDataListener {

    private static final String TAG = "SurveyDiameterFragment";

    private FragmentSurveyDiameterBinding binding;

    // 컨트롤러
    private YetiDeviceController yetiController;

    // 측정 스케줄링
    private final Handler measureHandler = new Handler(Looper.getMainLooper());
    private Runnable measureTask;
    private boolean isMeasuring = false;

    // 실시간/최대값 추적
    private double lastDistance = Double.NaN;
    private boolean trendingUp = false;           // 최근에 증가 흐름을 보였는지
    private static final double EPS = 0.002;      // 감소 판단을 위한 임계값(단위: m 기준 2mm 정도)

    private double maxDistance = Double.NEGATIVE_INFINITY;
    private String maxDistanceUnit = "";
    private double maxAngle = Double.NEGATIVE_INFINITY;
    private String maxAngleUnit = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        yetiController = new YetiDeviceController(
                requireContext().getApplicationContext(),
                this // YetiDataListener
        );

        // 🔗 연결된 디바이스 주입
        InformationActivityData info = Clipboard.INSTANCE.getInformationActivityData();
        if (info != null && info.device != null) {
            if (info.device.getDeviceType() == ch.leica.sdk.Types.DeviceType.Yeti) {
                yetiController.setCurrentDevice(info.device);
                yetiController.setListeners(); // 리스너 재바인딩
            } else {
                Log.w(TAG, "Connected device is not Yeti. Current type=" + info.device.getDeviceType());
            }
        } else {
            Log.w(TAG, "No device in Clipboard; connect first in ConnectDistoFragment.");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSurveyDiameterBinding.inflate(inflater, container, false);

        binding.btnSurvey.setOnClickListener(v -> onClickSurveyToggle());

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 이미 페어링/선택된 디바이스가 있다면 자동 재연결 시도
        if (getContext() != null) {
            yetiController.checkForReconnection(requireContext());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // 화면 떠날 땐 측정 중지 + 노티 멈춤
        stopMeasuring(false);
        try {
            yetiController.pauseBTConnection(new BleDevice.BTConnectionCallback() {
                @Override
                public void onFinished() {
                    Log.d(TAG, "Notifications deactivated.");
                }
            });
        } catch (Exception ignore) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 핸들러 콜백 제거
        measureHandler.removeCallbacksAndMessages(null);
        binding = null;
    }

    /* =========================
       버튼: 측정 토글
       ========================= */
    private void onClickSurveyToggle() {
        Device dev = yetiController.getCurrentDevice();
        if (dev == null) {
            showToast("먼저 Connect 화면에서 기기를 연결하세요.");
            return;
        }
        if (dev.getConnectionState() != Device.ConnectionState.connected) {
            showToast("기기 재연결 시도 중...");
            yetiController.checkForReconnection(requireContext());
            return;
        }

        if (!isMeasuring) {
            startMeasuring();
        } else {
            stopMeasuring(true);
        }
    }

    /* =========================
       측정 시작/정지
       ========================= */
    private void startMeasuring() {
        // 상태 초기화
        isMeasuring = true;
        lastDistance = Double.NaN;
        trendingUp = false;

        maxDistance = Double.NEGATIVE_INFINITY;
        maxDistanceUnit = "";
        maxAngle = Double.NEGATIVE_INFINITY;
        maxAngleUnit = "";

        // UI 초기화
        if (binding != null) {
            binding.tvRealtimeDistance.setText("");
            binding.tvRealtimeAngle.setText("");
            binding.tvMaxDistance.setText("");
            binding.tvMaxAngle.setText("");
            binding.btnSurvey.setText("측정 정지");
        }

        // 1초 간격 측정 태스크
        measureTask = new Runnable() {
            @Override
            public void run() {
                if (!isMeasuring) return;
                sendDistanceCommandOnWorker();
                // 다음 예약
                measureHandler.postDelayed(this, 1000);
            }
        };

        // 즉시 1회 + 주기 시작
        measureHandler.post(measureTask);
    }

    private void stopMeasuring(boolean showToast) {
        if (!isMeasuring) return;
        isMeasuring = false;
        measureHandler.removeCallbacksAndMessages(null);

        if (binding != null) {
            binding.btnSurvey.setText(getString(com.terra.terradisto.R.string.survey_diameter));
        }
        if (showToast) showToast("측정을 중지했습니다.");
    }

    /* =========================
       명령 전송(백그라운드)
       ========================= */
    private void sendDistanceCommandOnWorker() {
        new Thread(() -> {
            ErrorObject error = yetiController.sendDistanceCommand();
            if (error != null && isAdded()) {
                requireActivity().runOnUiThread(() -> showToast(formatErrorMessage(error)));
            }
        }).start();
    }

    private void showToast(String msg) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private String formatErrorMessage(ErrorObject error) {
        return error.getErrorMessage();
    }

    /* =========================
       YetiDataListener 콜백
       ========================= */
    @Override
    public void onBasicMeasurements_Received(YetiDeviceController.BasicData basicData) {
        // distance / inclination 값은 문자열일 수 있으니 안전 파싱
        final double distance = parseDoubleSafe(basicData.distance);
        final String distanceUnit = basicData.distanceUnit == null ? "" : basicData.distanceUnit;
        final double angle = parseDoubleSafe(basicData.inclination);
        final String angleUnit = basicData.inclinationUnit == null ? "" : basicData.inclinationUnit;

        if (!isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            // 실시간 표시
            if (binding != null) {
                if (!Double.isNaN(distance)) {
                    binding.tvRealtimeDistance.setText(basicData.distance + " " + distanceUnit);
                }
                if (!Double.isNaN(angle)) {
                    binding.tvRealtimeAngle.setText(basicData.inclination + " " + angleUnit);
                }
            }

            // 최대값 갱신 (독립적으로 추적)
            if (!Double.isNaN(distance) && distance > maxDistance) {
                maxDistance = distance;
                maxDistanceUnit = distanceUnit;
                if (binding != null) {
                    binding.tvMaxDistance.setText(basicData.distance + " " + distanceUnit);
                }
            }
            if (!Double.isNaN(angle) && angle > maxAngle) {
                maxAngle = angle;
                maxAngleUnit = angleUnit;
                if (binding != null) {
                    binding.tvMaxAngle.setText(basicData.inclination + " " + angleUnit);
                }
            }

            // 감소 감지 → 자동 정지
            // (최근에 증가 흐름을 보였고, 현재 값이 이전 값보다 EPS 이상 작아졌다면 정지)
            if (!Double.isNaN(distance)) {
                if (Double.isNaN(lastDistance)) {
                    lastDistance = distance; // 첫 샘플 세팅
                } else {
                    if (distance > lastDistance + EPS) {
                        trendingUp = true;      // 증가 흐름 진입
                    } else if (trendingUp && distance < lastDistance - EPS) {
                        // 피크 이후 하강 시작 → 측정 종료
                        stopMeasuring(true);
                    }
                    lastDistance = distance;
                }
            }
        });
    }

    @Override
    public void onP2PMeasurements_Received(YetiDeviceController.P2PData p2pData) {
        Log.d(TAG, "[P2P] hz=" + p2pData.hzValue + ", ve=" + p2pData.veValue
                + ", inclStatus=" + p2pData.inclinationStatus
                + ", ts=" + p2pData.timestamp);
    }

    @Override
    public void onQuaternionMeasurement_Received(YetiDeviceController.QuaternionData quaternionData) {
        Log.d(TAG, "[QUAT] x=" + quaternionData.quaternionX
                + ", y=" + quaternionData.quaternionY
                + ", z=" + quaternionData.quaternionZ
                + ", w=" + quaternionData.quaternionW
                + ", ts=" + quaternionData.timestamp);
    }

    @Override
    public void onAccRotationMeasurement_Received(YetiDeviceController.AccRotData accRotatonMeasurement) {
        Log.d(TAG, "[ACC/ROT] ax=" + accRotatonMeasurement.accelerationX
                + ", ay=" + accRotatonMeasurement.accelerationY
                + ", az=" + accRotatonMeasurement.accelerationZ
                + ", rx=" + accRotatonMeasurement.rotationX
                + ", ry=" + accRotatonMeasurement.rotationY
                + ", rz=" + accRotatonMeasurement.rotationZ
                + ", ts=" + accRotatonMeasurement.timestamp);
    }

    @Override
    public void onMagnetometerMeasurement_Received(YetiDeviceController.MagnetometerData magnetometerData) {
        Log.d(TAG, "[MAG] mx=" + magnetometerData.magnetometerX
                + ", my=" + magnetometerData.magnetometerY
                + ", mz=" + magnetometerData.magnetometerZ
                + ", ts=" + magnetometerData.timestamp);
    }

    @Override
    public void onDistocomTransmit_Received(String data) {
        Log.d(TAG, "[DISTOCOM RESP] " + data);
    }

    @Override
    public void onDistocomEvent_Received(String data) {
        Log.d(TAG, "[DISTOCOM EVENT] " + data);
    }

    @Override
    public void onBrand_Received(String data) { Log.d(TAG, "[INFO] brand=" + data); }

    @Override
    public void onAPPSoftwareVersion_Received(String data) { Log.d(TAG, "[INFO] appSW=" + data); }

    @Override
    public void onId_Received(String data) { Log.d(TAG, "[INFO] id=" + data); }

    @Override
    public void onEDMSoftwareVersion_Received(String data) { Log.d(TAG, "[INFO] edmSW=" + data); }

    @Override
    public void onFTASoftwareVersion_Received(String data) { Log.d(TAG, "[INFO] ftaSW=" + data); }

    @Override
    public void onAPPSerial_Received(String data) { Log.d(TAG, "[INFO] appSerial=" + data); }

    @Override
    public void onEDMSerial_Received(String data) { Log.d(TAG, "[INFO] edmSerial=" + data); }

    @Override
    public void onFTASerial_Received(String data) { Log.d(TAG, "[INFO] ftaSerial=" + data); }

    @Override
    public void onModel_Received(String data) { Log.d(TAG, "[INFO] model=" + data); }

    /* =========================
       유틸
       ========================= */
    private static double parseDoubleSafe(String s) {
        if (s == null) return Double.NaN;
        try {
            // "12,34" 같은 포맷/공백/문자 제거
            String normalized = s.trim()
                    .replace(",", ".")
                    .replaceAll("[^0-9+\\-Ee.]", "");
            if (normalized.isEmpty()) return Double.NaN;
            return Double.parseDouble(normalized);
        } catch (Exception ignore) {
            return Double.NaN;
        }
    }
}
