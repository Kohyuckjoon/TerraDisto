package com.terra.terradisto.ui.distoconnect;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultCaller;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.terra.terradisto.DistoStatusListener;
import com.terra.terradisto.databinding.FragmentConnectDistoBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import ch.leica.sdk.Devices.Device;
import ch.leica.sdk.Devices.DeviceManager;
import ch.leica.sdk.ErrorHandling.ErrorDefinitions;
import ch.leica.sdk.ErrorHandling.ErrorObject;
import ch.leica.sdk.ErrorHandling.IllegalArgumentCheckedException;
import ch.leica.sdk.LeicaSdk;
import ch.leica.sdk.Listeners.ErrorListener;
import ch.leica.sdk.Types;
import ch.leica.sdk.Utilities.WifiHelper;

import com.terra.terradisto.R;
import com.terra.terradisto.distosdkapp.AppLicenses;
import com.terra.terradisto.distosdkapp.clipboard.Clipboard;
import com.terra.terradisto.distosdkapp.clipboard.InformationActivityData;
import com.terra.terradisto.distosdkapp.device.AvailableDevicesListener;
import com.terra.terradisto.distosdkapp.device.FindDevices;
import com.terra.terradisto.distosdkapp.permissions.PermissionsHelper;
import com.terra.terradisto.distosdkapp.utilities.dialog.DialogHandler;
import com.terra.terradisto.ui.viewModel.DistoViewModel;

import org.json.JSONException;

public class ConnectDistoFragment extends Fragment
        implements Device.ConnectionListener,
        ErrorListener,
        AvailableDevicesListener,
        DeviceListAdapter.OnDeviceClickListener {

    //리스너를 담을 변수 추가
    private DistoStatusListener statusListener;

    //MainActivity에서 리스너를 꽂아줄 수 있도록 Setter 메서드 추가
    public void setDistoStatusListener(DistoStatusListener listener) {
        this.statusListener = listener;
    }
    private static final String TAG = "ConnectDistoFragment";

    private FragmentConnectDistoBinding b;

    // UI
    private DeviceListAdapter adapter;
    private DialogHandler connectingDialog;
    private DialogHandler alertsDialog;

    // SDK / 디바이스
    private DeviceManager deviceManager;
    private FindDevices findDevices;
    private Device currentDevice;

    // 권한
    private PermissionsHelper permissionsHelper;

    // 연결 시도 취소/타임아웃 관리
    private final Map<Device, Boolean> connectionAttempts = new HashMap<>();
    private Device currentConnectionAttemptToDevice = null;
    private Timer connectionTimeoutTimer;
    private TimerTask connectionTimeoutTask;

    // 최초 1회 안내 다이얼로그
    private static boolean searchInfoShown = false;

    private DistoViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentConnectDistoBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                boolean isEnabled = (state == BluetoothAdapter.STATE_ON);

                updateBtStatusUI(isEnabled);

                // 2. ViewModel 상태 업데이트 (Compose와 연동)
                if (viewModel != null) {
                    viewModel.updateBluetoothEnabled(isEnabled);
                }
            }
        }
    };

    private void updateBtStatusUI(boolean isEnabled) {
        if (b == null) return;
        requireActivity().runOnUiThread(() -> {
            if (isEnabled) {
                b.tvBtStatus.setText("켜짐");
                b.tvBtStatus.setTextColor(android.graphics.Color.parseColor("#3182F6")); //  블루
            } else {
                b.tvBtStatus.setText("꺼짐");
                b.tvBtStatus.setTextColor(android.graphics.Color.parseColor("#8B95A1")); // 회색
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(DistoViewModel.class);

        // Dialogs
        connectingDialog = new DialogHandler();
        alertsDialog = new DialogHandler();

        // RecyclerView
        adapter = new DeviceListAdapter(new ArrayList<>(), this);
        b.rvDevices.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvDevices.setAdapter(adapter);

        // 권한 헬퍼 (Activity 컨텍스트 필요)
        permissionsHelper = new PermissionsHelper(requireActivity());

        // SDK / 스캐닝 구성
        initSDK();
        deviceManager = DeviceManager.getInstance(requireContext().getApplicationContext());
        deviceManager.setErrorListener(this);

        findDevices = new FindDevices(requireContext().getApplicationContext(), this);
        findDevices.registerReceivers();

        // 클립보드에 이전 디바이스 남아있으면 리스너 붙이기
        InformationActivityData info = Clipboard.INSTANCE.getInformationActivityData();
        if (info != null && info.device != null) {
            currentDevice = info.device;
            currentDevice.setConnectionListener(this);
            currentDevice.setErrorListener(this);
        }

        // 타이머
        connectionTimeoutTimer = new Timer();

        // SDK 버전 표시는 필요 시 b.tvSectionTitle 등에 append 가능
        // String version = LeicaSdk.getVersion();

        // 진입 시 목록 갱신
        updateList();

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        updateBtStatusUI(btAdapter != null && btAdapter.isEnabled());

        // 2. 리시버 등록 (상태 변화 감지)
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        requireActivity().registerReceiver(bluetoothStateReceiver, filter);

        // 3. 클릭 리스너 설정 (꺼짐 텍스트와 화살표 아이콘 클릭 시 설정 이동)
        View.OnClickListener goToBtSettings = v -> {
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        };

        b.tvBtStatus.setOnClickListener(goToBtSettings);
        b.ivArrow.setOnClickListener(goToBtSettings);

        // 새로고침 버튼 이벤트 구현
        b.tvRefresh.setOnClickListener(v -> {
            // 1. 연타 방지: 버튼 비활성화
            v.setEnabled(false);

            // 2. 리스트 초기화 (사용자 피드백)
            if (this.adapter != null) {
                this.adapter.setItems(new ArrayList<>());
                this.adapter.notifyDataSetChanged();
            }

            // 3. 기존 스캔 중지 후 재시작 (FindDevices 클래스의 기존 로직 활용)
            if (findDevices != null) {
                findDevices.stopFindingDevices();
                // 잠시 후 다시 시작 (안정성)
                v.postDelayed(() -> {
                    findAvailableDevices();
                    Log.d(TAG, "새로고침: 장치 스캔 재시작");
                }, 200);
            }

            // 4. 1초 후 버튼 다시 활성화
            v.postDelayed(() -> v.setEnabled(true), 1000);
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        updateList();

        if (!searchInfoShown) {
            searchInfoShown = true;
            alertsDialog.setAlert(requireActivity(), "장치를 찾기 위해 블루투스를 켜주세요");
            alertsDialog.show();
        }

        // 저장 권한 등
        permissionsHelper.requestStoragePermission();

        // 이미 연결된 디바이스만 우선 보여주고
        findDevices.requestConnectedDevices();
        // 즉시 스캔 시작
        findAvailableDevices();
    }

    @Override
    public void onPause() {
        super.onPause();
        // 필요 시 중단 로직 추가 가능
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().unregisterReceiver(bluetoothStateReceiver);

        if (findDevices != null) {
            findDevices.stopFindingDevices();
            findDevices.unregisterReceivers();
            findDevices.onDestroy();
        }

        stopConnectionTimeOutTimer();

        if (connectingDialog != null) connectingDialog.dismiss();

        b = null;
    }

    /* =========================
       SDK 초기화
       ========================= */
    private void initSDK() {
        if (!LeicaSdk.isInit) {
            LeicaSdk.InitObject initObject = new LeicaSdk.InitObject("commands.json");
            try {
                LeicaSdk.init(requireContext().getApplicationContext(), initObject);
                LeicaSdk.setLogLevel(android.util.Log.VERBOSE);
                LeicaSdk.setMethodCalledLog(false);
                LeicaSdk.setScanConfig(true, true, true, true);

                AppLicenses appLicenses = new AppLicenses();
                LeicaSdk.setLicenses(appLicenses.keys);

                // 기본값: 시작 시 어댑터 off (원 코드 유지)
                LeicaSdk.scanConfig.setWifiAdapterOn(false);
                LeicaSdk.scanConfig.setBleAdapterOn(false);
            } catch (JSONException e) {
                Log.e(TAG, "SDK init JSON 구조 오류", e);
            } catch (IllegalArgumentCheckedException e) {
                Log.e(TAG, "SDK init 데이터 오류", e);
            } catch (IOException e) {
                Log.e(TAG, "SDK init 파일 읽기 오류", e);
            }
        }
    }

    /* =========================
       권한 콜백
       ========================= */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionsHelper != null) {
            permissionsHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /* =========================
       스캔 제어
       ========================= */
    public void findAvailableDevices() {
        updateList();

        InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();
        if (data != null && data.isSearchingEnabled) {
            permissionsHelper.requestNetworkPermissions();
            if (deviceManager != null) deviceManager.setErrorListener(this);
            if (findDevices != null) findDevices.findAvailableDevices(requireContext());
        } else {
            Log.i(TAG, "findAvailableDevices: 재생성 이슈 회피로 스킵");
        }
    }

    /* =========================
       AvailableDevicesListener
       ========================= */
    @Override
    public void onAvailableDeviceFound() {
        updateList();
    }

    @Override
    public void onAvailableDevicesChanged(java.util.List<Device> availableDevices) {
        updateList();
    }

    private void updateList() {
        if (b == null || adapter == null || findDevices == null) return;
        requireActivity().runOnUiThread(() -> {
            adapter.setItems(new ArrayList<>(findDevices.getAvailableDevices()));
            adapter.notifyDataSetChanged();
        });
    }

    /* =========================
       클릭 콜백 (Recycler item)
       ========================= */
    @Override
    public void onDeviceClick(Device device) {
        // SearchDevicesActivity.OnItemClickListener 내용을 그대로 포팅
        if (findDevices != null) findDevices.stopFindingDevices();

        if (device == null) {
            Log.i(TAG, "device not found");
            return;
        }

        currentDevice = device;

        if (device.getConnectionState() == Device.ConnectionState.connected) {
            goToInfoScreen(device);
            return;
        }

        // 연결 다이얼로그
        String title = "Connecting";
        String message = "Connecting... This may take up to 30 seconds... ";
        String negativeText = "Cancel";
        Runnable negativeAction = () -> {
            stopConnectionAttempt();
            findAvailableDevices();
        };
        connectingDialog.setDialog(requireActivity(),
                title, message, false,
                null, null,
                negativeText, negativeAction);
        connectingDialog.show();

        if (currentDevice.getConnectionType().equals(Types.ConnectionType.wifiHotspot)) {
            String wifiName = WifiHelper.getWifiName(requireContext().getApplicationContext());
            if (wifiName == null || !wifiName.equalsIgnoreCase(currentDevice.getDeviceName())) {
                gotoWifiPanel();
                return;
            } else {
                connectToDevice(currentDevice);
                return;
            }
        }

        // BLE 타임아웃 타이머
        startConnectionTimeOutTimer();

        connectToDevice(currentDevice);
    }

    /* =========================
       연결 제어
       ========================= */
    private void connectToDevice(final Device device) {
        Log.d(TAG, "connectToDevice: connecting...");

        currentConnectionAttemptToDevice = device;
        connectionAttempts.put(device, Boolean.FALSE);

        device.setConnectionListener(this);
        device.setErrorListener(this);
        if (deviceManager != null) deviceManager.stopFindingDevices();

        InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();
        if (data != null) data.isSearchingEnabled = false;

        device.connect();
    }

    private synchronized void stopConnectionAttempt() {
        InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();
        if (data != null) data.isSearchingEnabled = true;

        if (currentConnectionAttemptToDevice != null) {
            connectionAttempts.put(currentConnectionAttemptToDevice, Boolean.TRUE);
        }

        stopConnectionTimeOutTimer();

        if (connectingDialog != null) connectingDialog.dismiss();

        if (currentDevice != null) currentDevice.disconnect();
    }

    private void startConnectionTimeOutTimer() {
        if (currentDevice != null && Types.ConnectionType.ble.equals(currentDevice.getConnectionType())) {
            final long timeoutMs = 90 * 1000;
            connectionTimeoutTask = new TimerTask() {
                @Override
                public void run() {
                    stopConnectionAttempt();
                    showConnectionTimedOutDialog();

                    if (currentDevice != null) {
                        findDevices.requestConnectedDevices();
                        updateList();
                        currentDevice.disconnect();
                    }
                    findAvailableDevices();
                }
            };
            connectionTimeoutTimer.schedule(connectionTimeoutTask, timeoutMs);
        }
    }

    private void stopConnectionTimeOutTimer() {
        if (connectionTimeoutTask != null) {
            connectionTimeoutTask.cancel();
            connectionTimeoutTask = null;
        }
        if (connectionTimeoutTimer != null) {
            connectionTimeoutTimer.purge();
        }
    }

    // 연결 상태에 따른 헤더 UI 업데이트 로직
    private void updateConnectionUI(boolean isConnected) {
        if (b == null) return;

        requireActivity().runOnUiThread(() -> {
            if (isConnected) {
                // 연결됨: 파란색 꽉 찬 동그라미 + 흰색 아이콘
                b.ivHeaderBluetooth.setBackgroundResource(R.drawable.bg_circle_blue_fill); // 새로 만들어야 함
                b.ivHeaderBluetooth.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
            } else {
                // 연결 안 됨: 흰색 배경 + 파란색 아이콘 (현재 상태 유지)
                b.ivHeaderBluetooth.setBackgroundResource(R.drawable.bg_circle_white);
                b.ivHeaderBluetooth.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3182F6")));
            }
        });
    }

    /* =========================
       연결 상태/에러 콜백
       ========================= */
    @Override
    public void onConnectionStateChanged(Device device, Device.ConnectionState state) {
        Log.i(TAG, "onConnectionStateChanged: " + device.getDeviceID() + ", " + state);

        switch (state) {
            case connected:
                if (connectingDialog != null) connectingDialog.dismiss();

                // 연결 성공을 알림
                if (statusListener != null) statusListener.onStatusChanged(true);

                // 헤더 UI 변경
                updateConnectionUI(true);

                Boolean canceled = connectionAttempts.get(device);
                if (canceled != null && canceled) {
                    device.disconnect();
                    connectionAttempts.remove(device);
                    updateList();
                    return;
                }
                goToInfoScreen(device);
                break;

            case disconnected:
                stopConnectionTimeOutTimer();
                if (statusListener != null) statusListener.onStatusChanged(false);

                updateConnectionUI(false);
                break;
        }
    }

    @Override
    public void onError(final ErrorObject errorObject, final Device device) {
        Log.i(TAG, "onError: " + errorObject.getErrorMessage() + ", code: " + errorObject.getErrorCode());
        if (statusListener != null) statusListener.onStatusChanged(false);

        // 에러 발생 시 UI 원복
        updateConnectionUI(false);

        String message = "";

        if (connectingDialog != null) connectingDialog.dismiss();

        int code = errorObject.getErrorCode();
        if (code == ErrorDefinitions.BLUETOOTH_DEVICE_133_ERROR_CODE
                || code == ErrorDefinitions.BLUETOOTH_DEVICE_62_ERROR_CODE) {

            if (device != null && currentConnectionAttemptToDevice != null
                    && device.getDeviceID().equalsIgnoreCase(currentConnectionAttemptToDevice.getDeviceID())) {
                String title = "Device not found";
                message = "The Device can not be found, please verify the device is turned ON and in range";
                connectingDialog.setDialog(requireActivity(), title, message, true);
                connectingDialog.show();
                return;
            }

            stopConnectionAttempt();
            showError(errorObject, message);
            return;
        }

        if (code == ErrorDefinitions.HOTSPOT_DEVICE_IP_NOT_REACHABLE_CODE
                || code == ErrorDefinitions.AP_DEVICE_IP_NOT_REACHABLE_CODE) {
            showError(errorObject, message);
            return;
        }

        if (code == ErrorDefinitions.BLUETOOTH_DEVICE_UNABLE_TO_PAIR_CODE) {
            message = String.format(Locale.getDefault(),
                    "%s \n %s",
                    "Please Reset Device and remove pairing Settings manually in Android settings.",
                    "and try again."
            );
            showError(errorObject, message);
            stopConnectionTimeOutTimer();
            return;
        }

        showError(errorObject, message);
    }

    /* =========================
       화면 전환 / 보조 UI
       ========================= */
    // ConnectDistoFragment.java

    private void goToInfoScreen(Device device) {
        stopConnectionTimeOutTimer();

        // 1) 전역 저장 (이미 하고 있지만 확실히 유지)
        Clipboard.INSTANCE.setInformationActivityData(
                new InformationActivityData(device, null, deviceManager)
        );

        // 2) 액티비티 전환 제거 (주석 처리)
        // Class<?> nextActivity = ... (전부 제거)
        // startActivity(intent);

        // 3) 원하는 화면으로 네비게이션 (Navigation Component 쓰면)
        // NavHostFragment.findNavController(this)
        //       .navigate(R.id.action_connectDisto_to_surveyDiameter);

        // 또는 탭 전환/콜백 등으로 SurveyDiameter로 이동
        Log.i(TAG, "Device connected. Ready to use in other fragments.");
        currentDevice = null;
    }


    private void gotoWifiPanel() {
        String title = "Wifi Settings";
        String message = "Please connect to the WIFI HOTSPOT from the device.";
        String positive = "OK";
        Runnable action = () -> {
            connectingDialog.dismiss();
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        };
        DialogHandler wifiDialog = new DialogHandler();
        wifiDialog.setDialog(requireActivity(), title, message, false, positive, action, null, null);
        wifiDialog.show();
    }

    private void showConnectionTimedOutDialog() {
        if (currentDevice != null) {
            String title = "Connection Timeout";
            String message = String.format(
                    "Could not connect to \n%s\nPlease check your device and adapters and try again.",
                    currentDevice.getDeviceID()
            );
            connectingDialog.setDialog(requireActivity(), title, message, true);
            connectingDialog.show();
        }
    }

    private void showError(ErrorObject error, String message) {
        alertsDialog.setAlert(
                requireActivity(),
                String.format(Locale.getDefault(),
                        "errorCode: %d, %s \n %s",
                        error.getErrorCode(),
                        error.getErrorMessage(),
                        message)
        );
        alertsDialog.show();
    }
}

