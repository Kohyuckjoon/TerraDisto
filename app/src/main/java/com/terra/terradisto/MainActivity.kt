package com.terra.terradisto

import SurveyDiameterScreen
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.terra.terradisto.ui.distoconnect.DistoConnectViewModel
import com.terra.terradisto.ui.main.DistoMainScreen
import com.terra.terradisto.ui.project.CreateProjectScreen
import com.terra.terradisto.ui.navigationHostWrapper.NavigationHostWrapper
import androidx.compose.animation.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terra.terradisto.data.AppDatabase

import com.terra.terradisto.ui.ProjectListScreen

import com.terra.terradisto.ui.history.MeasureHistoryScreen
import com.terra.terradisto.ui.main.QuickSurveyScreen
import com.terra.terradisto.ui.screens.SurveyMeasurementScreen
import com.terra.terradisto.viewmodel.ProjectViewModel
import kotlinx.coroutines.delay

interface DistoStatusListener {
    fun onStatusChanged(isConnected: Boolean)
}

class MainActivity : FragmentActivity(), DistoStatusListener {

    private val viewModel: DistoConnectViewModel by viewModels()
    private val distoViewModel: com.terra.terradisto.ui.viewModel.DistoViewModel by viewModels()
    private val projectViewModel: ProjectViewModel by viewModels() // 프로젝트 상태를 앱 전체 유지
    private var findDevices: com.terra.terradisto.distosdkapp.device.FindDevices? = null

    override fun onStatusChanged(isConnected: Boolean) {
        runOnUiThread {
            viewModel.updateConnectionStatus(isConnected)
            distoViewModel.updateConnectionState(isConnected)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findDevices = com.terra.terradisto.distosdkapp.device.FindDevices(applicationContext, object : com.terra.terradisto.distosdkapp.device.AvailableDevicesListener {
            override fun onAvailableDeviceFound() {}
            override fun onAvailableDevicesChanged(devices: MutableList<ch.leica.sdk.Devices.Device>?) {
                devices?.forEach { device ->
                    // [핵심] 수동 연결 없이 기기 전원만 켰을 때, SDK가 'Connected' 상태라면 Clipboard에 즉시 주입
                    if (device.connectionState == ch.leica.sdk.Devices.Device.ConnectionState.connected) {
                        val info = com.terra.terradisto.distosdkapp.clipboard.InformationActivityData(
                            device, null, ch.leica.sdk.Devices.DeviceManager.getInstance(applicationContext)
                        )
                        com.terra.terradisto.distosdkapp.clipboard.Clipboard.INSTANCE.informationActivityData = info
                        onStatusChanged(true)
                    }
                }
            }
        })

        findDevices?.registerReceivers()


        setContent {
            /**
             * Android Java / XML / Activity / Fragment 사용시 -> XML 교체 방식
             * Compose / Kotlin 사용시 -> State에 따라 UI를 갈아 끼우는 방식
             */
            MaterialTheme {
                // 구조 수정 - 현재 화면 상태 관리
                var currentScreen by remember { mutableStateOf("main") }

                // 프로젝트 생성 홤면 진입 전 "이전 화면" 기억하기 상태 변수
                var previousScreen by remember { mutableStateOf("main") }

                // SDK 연동용 상태 값(Disto SDK)
//                val isDistoConnected = viewModel.isDistoConnected

                // 데이터 베이스로부터 실시간 선택된 프로젝트 상태를 확인(Compose state)
                val selectedProject by projectViewModel.selectedProject.collectAsState()

                // 프로젝트 미 선택 안내 팝업을 제어하기 위한 상태 변수 추가
                var showProjectErrorDialog by remember { mutableStateOf(false) }

                val quickDistanceState = remember { mutableStateOf("0.000") }
                val mainYetiController = remember {
                    com.terra.terradisto.distosdkapp.device.YetiDeviceController(
                        applicationContext,
                        object : com.terra.terradisto.distosdkapp.device.YetiDeviceController.YetiDataListener {
                            override fun onBasicMeasurements_Received(basicData: com.terra.terradisto.distosdkapp.device.YetiDeviceController.BasicData?) {
                                basicData?.let { data ->
                                    // 측정된 물리 거리를 상태값에 갱신하여 UI로 전달
                                    quickDistanceState.value = data.distance ?: "0.000"
                                }
                            }
                            // 나머지 필수 리스너 오버라이드는 공백 유지
                            override fun onP2PMeasurements_Received(p2pData: com.terra.terradisto.distosdkapp.device.YetiDeviceController.P2PData?) {}
                            override fun onQuaternionMeasurement_Received(d: com.terra.terradisto.distosdkapp.device.YetiDeviceController.QuaternionData?) {}
                            override fun onAccRotationMeasurement_Received(d: com.terra.terradisto.distosdkapp.device.YetiDeviceController.AccRotData?) {}
                            override fun onMagnetometerMeasurement_Received(d: com.terra.terradisto.distosdkapp.device.YetiDeviceController.MagnetometerData?) {}
                            override fun onDistocomTransmit_Received(d: String?) {}
                            override fun onDistocomEvent_Received(d: String?) {}
                            override fun onBrand_Received(d: String?) {}
                            override fun onAPPSoftwareVersion_Received(d: String?) {}
                            override fun onId_Received(d: String?) {}
                            override fun onEDMSoftwareVersion_Received(d: String?) {}
                            override fun onFTASoftwareVersion_Received(d: String?) {}
                            override fun onAPPSerial_Received(d: String?) {}
                            override fun onEDMSerial_Received(d: String?) {}
                            override fun onFTASerial_Received(d: String?) {}
                            override fun onModel_Received(d: String?) {}
                        },
                        null,
                        object : com.terra.terradisto.distosdkapp.device.DeviceStatusListener {
                            override fun onConnectionStateChanged(deviceId: String?, state: ch.leica.sdk.Devices.Device.ConnectionState?) {
                                val isConnected = state == ch.leica.sdk.Devices.Device.ConnectionState.connected

                                onStatusChanged(isConnected)

                                // 뷰모델에 연결 상태 전달
                                distoViewModel.updateConnectionState(isConnected)
                            }

                            // //  인터페이스 명세에 맞춰 오버라이드 (경로 수정 시 자동으로 매칭됨)
                            override fun onStatusChange(status: String?) {}
                            override fun onReconnect() {}
                            override fun onError(code: Int, message: String?) {}
                        }
                    )
                }

                // 앱 실행 시 또는 메인 화면에 있을때 연결 상태를 주기적으로 확인 및 초기화
                LaunchedEffect(Unit) {
                    while(true) {
                        // 1. 이미 시스템에 블루투스로 연결된 장치가 있는지 SDK에 강제로 물어봄
                        findDevices?.requestConnectedDevices()

                        val info = com.terra.terradisto.distosdkapp.clipboard.Clipboard.INSTANCE.informationActivityData
                        val device = info?.device
                        val actualConnected = device?.connectionState == ch.leica.sdk.Devices.Device.ConnectionState.connected

                        // 2. [중요] 실제 하드웨어 상태와 우리 앱의 ViewModel 상태를 강제로 일치시킴
                        if (viewModel.isDistoConnected != actualConnected) {
                            onStatusChanged(actualConnected)
                        }

                        // 3. 기기가 연결되어 있다면 컨트롤러에 주입하고 리스너(측정 데이터 수신) 활성화
                        if (device != null && actualConnected) {
                            mainYetiController.setCurrentDevice(device)
                            mainYetiController.setListeners()
                        }
                        delay(2000) // 2초마다 상태 체크하여 즉각 반응 보장
                    }
                }

                // 화면 전환 시에도 리스너를 다시 붙여 끊김 방지
                LaunchedEffect(currentScreen) {
                    val info = com.terra.terradisto.distosdkapp.clipboard.Clipboard.INSTANCE.informationActivityData
                    info?.device?.let { device ->
                        if (device.connectionState == ch.leica.sdk.Devices.Device.ConnectionState.connected) {
                            mainYetiController.setCurrentDevice(device)
                            mainYetiController.setListeners()
                        }
                    }
                }



                // 시스템 뒤로가기 버튼
                BackHandler(enabled = currentScreen != "main") {
                    if (currentScreen == "create_project") {
                        currentScreen = previousScreen
                    } else {
                        currentScreen = "main"
                    }
                }

                // 프로젝트 미선택 팝업창
                if (showProjectErrorDialog) {
                    AlertDialog(
                        onDismissRequest = { showProjectErrorDialog = false },
                        shape = RoundedCornerShape(24.dp), //  특유의 둥글고 세련된 느낌 적용
                        containerColor = Color.White,
                        title = {
                            Text(
                                text = "선택된 프로젝트가 없어요",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF191F28) //  주요 다크 그레이
                            )
                        },
                        text = {
                            Text(
                                text = "측정 내역을 확인하려면\n프로젝트를 먼저 선택해야 해요.",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4E5968), //  서브 본문 컬러
                                lineHeight = 22.sp
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showProjectErrorDialog = false
                                    currentScreen = "project_list" // 확인 누르면 프로젝트 목록으로 다이렉트 이동
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF3182F6),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "기존 프로젝트 불러오기",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        dismissButton = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "닫기",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF8B95A1), // 가볍게 인지할 취소 텍스트 색상
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showProjectErrorDialog = false }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    )
                }

                AnimatedContent(targetState = currentScreen) { target ->
                    when (target) {
                        "main" -> DistoMainScreen(
                            isDistoConnected = viewModel.isDistoConnected,
                            selectedProjectName = selectedProject?.projectName,
                            onConnectClick = { currentScreen = "connect" },
                            onCreateProjectClick = { previousScreen = "main"; currentScreen = "create_project" },
                            onQuickSurveyClick = { currentScreen = "quick_survey" },
                            onSurveyClick = {
                                if (selectedProject == null) showProjectErrorDialog = true
                                else currentScreen = "survey"
                            },
                            onProjectListClick = { currentScreen = "project_list" },
                            onHistoryClick = {
                                if (selectedProject == null) showProjectErrorDialog = true
                                else currentScreen = "history"
                            }
                        )
                        "quick_survey" -> QuickSurveyScreen(
                            isDistoConnected = viewModel.isDistoConnected,
                            distoMeasuredDistance = quickDistanceState.value,
                            onMeasureClick = { Thread { mainYetiController.sendDistanceCommand() }.start() },
                            onBackClick = { currentScreen = "main" }
                        )
                        "connect" -> NavigationHostWrapper(onBack = { currentScreen = "main" })
                        "create_project" -> CreateProjectScreen(onBack = { currentScreen = previousScreen })
                        "survey" -> {
                            // 1. 필요한 상태값들을 MainActivity 내에서 가져옵니다
                            SurveyDiameterScreen(
                                isDistoConnected = viewModel.isDistoConnected, // 기존 연결 상태
                                distoMeasuredDistance = quickDistanceState.value, // 실시간 측정값
                                onMeasureClick = {
                                    // 측정 명령 전송 로직
                                    Thread { mainYetiController.sendDistanceCommand() }.start()
                                },
                                onBackClick = { currentScreen = "main" }
                            )
                        }
                        "project_list" -> ProjectListScreen(projectViewModel = projectViewModel, distoViewModel = distoViewModel, onNavigateToSurvey = { currentScreen = "survey" }, onCreateClick = { previousScreen = "project_list"; currentScreen = "create_project" }, onConnectClick = { currentScreen = "connect" })
                        "history" -> {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val db = AppDatabase.getDatabase(context)
                            val items by db.measurementDao().getMesurementByProject(selectedProject?.id ?: -1L).collectAsState(initial = emptyList())
                            MeasureHistoryScreen(items = items, onBackClick = { currentScreen = "main" })
                        }
                    }
                }
            }
        }
    }
}


// 상태값 변경
//    override fun onStatusChanged(isConnected: Boolean) {
//        runOnUiThread {
//            viewModel.updateConnectionStatus(isConnected) // SDK 연동용 상태 업데이트
//            distoViewModel.updateConnectionState(isConnected) // DistoMainScreen, ProjectList 뷰모델 동기화
//        }
//    }
//}


@Composable
fun onCreateProjectClick(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp).background(Color(0xFFE8F3FF), CircleShape))
        }
    }
}

@Composable
fun HeaderSection(
    // 현재 선택된 프로젝트 이름을 받아와서 띄워주도록 매개변수 구조 수정 및 onClick 핸들러 연결 가능케 변경
    selectedProjectName: String?,
    onProjectListClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "Disto Survey",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF191F28),
                letterSpacing = (-0.5).sp
            )
            Text(
                "스마트 맨홀 측량 시스템",
                fontSize = 15.sp,
                color = Color(0xFF4E5968),
                fontWeight = FontWeight.Medium
            )
        }
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onProjectListClick() }, //우상단 프로젝트 영역 클릭하면 프로젝트 리스트 화면으로 이동되게 변경
//            color = Color.White,
            color = if (selectedProjectName != null) Color(0xFFE8F3FF) else Color(0xFFF2F4F6),
            border = if (selectedProjectName != null) null else BorderStroke(1.dp, Color(0xFFE5E8EB))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        "현재 프로젝트",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedProjectName != null) Color(0xFF1B64D1) else Color(0xFF8B95A1), /* 상단 라벨 톤 매칭 */
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = selectedProjectName ?: "목록 선택",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selectedProjectName != null) Color(0xFF3182F6) else Color(0xFF4E5968),
                        maxLines = 1,
                        letterSpacing = (-0.3).sp
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    null,
                    tint = Color(0xFFB0B8C1),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ColumnScope.MainActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(24.dp), tint = Color.White)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun BottomActionArea(
    onStartClick: () -> Unit,
    onCreateClick: () -> Unit,
    onProjectListClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onCreateClick, // 프로젝트 생성화면으로 연결
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3182F6),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("프로젝트 생성 시작", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onProjectListClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFE5E8EB)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4E5968))
        ) {
            Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("기존 프로젝트 불러오기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// 프리뷰는 그대로 유지 (정상 동작)
@Preview(showBackground = true, showSystemUi = true, name = "메인 화면 - 연결됨")
@Composable
fun DistoMainScreenConnectedPreview() {
    MaterialTheme {
        // 실제 ViewModel 대신 상태값만 전달하여 디자인 확인
        DistoMainScreen(
            isDistoConnected = true,
            selectedProjectName = "서울 역삼맨홀 현장",
            onConnectClick = { /* 프리뷰에서는 동작하지 않음 */ },
            onCreateProjectClick = { },
            onSurveyClick = { },
            onProjectListClick = { }, // 누락된 인자 추가로 에러 방지
            onQuickSurveyClick = {},
            onHistoryClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "메인 화면 - 연결안됨")
@Composable
fun DistoMainScreenDisconnectedPreview() {
    MaterialTheme {
        DistoMainScreen(
            isDistoConnected = false, // [체크] 연결 안 된 상태의 디자인 확인용
            selectedProjectName = null,
            onConnectClick = { },
            onCreateProjectClick = { },
            onSurveyClick = { },
            onProjectListClick = { },
            onQuickSurveyClick = {},
            onHistoryClick = {}
        )
    }
}