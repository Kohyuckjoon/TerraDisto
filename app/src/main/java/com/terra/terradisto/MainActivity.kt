package com.terra.terradisto

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
import com.terra.terradisto.ui.ActiveTarget
import com.terra.terradisto.ui.ProjectListScreen
import com.terra.terradisto.ui.SurveyMeasurementScreen
import com.terra.terradisto.ui.history.MeasureHistoryScreen
import com.terra.terradisto.ui.main.QuickSurveyScreen
import com.terra.terradisto.viewmodel.ProjectViewModel

interface DistoStatusListener {
    fun onStatusChanged(isConnected: Boolean)
}

class MainActivity : FragmentActivity(), DistoStatusListener {

    private val viewModel: DistoConnectViewModel by viewModels()
    private val distoViewModel: com.terra.terradisto.ui.viewModel.DistoViewModel by viewModels()

    // 프로젝트 상태를 앱 전체 유지
    private val projectViewModel: ProjectViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                val isDistoConnected = viewModel.isDistoConnected

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
                        }
                    )
                }

// [여기 추가] 화면 진입 시 블루투스 장비 인스턴스 자동 주입 및 바인딩 시점 확보
                LaunchedEffect(currentScreen) {
                    if (currentScreen == "quick_survey") {
                        val info = com.terra.terradisto.distosdkapp.clipboard.Clipboard.INSTANCE.informationActivityData
                        if (info?.device != null && info.device.deviceType == ch.leica.sdk.Types.DeviceType.Yeti) {
                            mainYetiController.setCurrentDevice(info.device)
                            mainYetiController.setListeners()
                            mainYetiController.checkForReconnection(applicationContext)
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
                        shape = RoundedCornerShape(24.dp), // 토스 특유의 둥글고 세련된 느낌 적용
                        containerColor = Color.White,
                        title = {
                            Text(
                                text = "선택된 프로젝트가 없어요",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF191F28) // 토스 주요 다크 그레이
                            )
                        },
                        text = {
                            Text(
                                text = "측정 내역을 확인하려면\n프로젝트를 먼저 선택해야 해요.",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4E5968), // 토스 서브 본문 컬러
                                lineHeight = 22.sp
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showProjectErrorDialog = false
                                    currentScreen = "project_list" // 🔴 확인 누르면 프로젝트 목록으로 다이렉트 이동
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF3182F6), // 토스 시그니처 블루
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

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        val isBackscaling = targetState == "main" || (targetState == "project_list" && initialState == "create_project")
//                        val isBackscaling = targetState == "main"

                        if (isBackscaling) {
                            (slideInHorizontally { width -> -width } + fadeIn())
                                .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> width } + fadeIn())
                                .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                        }
                    }
                ) { target ->
                    // 화면 로직
                    when (target) {
                        "main" -> {
                            DistoMainScreen(
                                isDistoConnected = isDistoConnected,
                                selectedProjectName = selectedProject?.projectName,
                                onConnectClick = { currentScreen = "connect" },
                                onCreateProjectClick = {
                                    previousScreen = "main"
                                    currentScreen = "create_project"
                                },
                                onQuickSurveyClick = { currentScreen = "quick_survey" },
                                onSurveyClick = { currentScreen = "survey" },
                                onProjectListClick = { currentScreen = "project_list" },
                                onHistoryClick = {
                                    if (selectedProject == null) {
                                        showProjectErrorDialog = true // 프로젝트가 없으면 토스풍 팝업 개방
                                    } else {
                                        currentScreen = "history" // 프로젝트가 있으면 히스토리로 정상 진입
                                    }
                                }
                            )
                        }

                        // 간편 측정
                        "quick_survey" -> {
                            QuickSurveyScreen(
                                isDistoConnected = isDistoConnected,
                                // 메인 스코어보드 및 내부 일러스트 텍스트에 실시간 물리 거리 데이터 매핑
                                distoMeasuredDistance = quickDistanceState.value,
                                onMeasureClick = {
                                    // 하드웨어로 레이저 측정 명령 전송 백그라운드 스레드 실행
                                    Thread { mainYetiController.sendDistanceCommand() }.start()
                                },
                                onBackClick = { currentScreen = "main" }
                            )
                        }

                        // Disto 연결
                        "connect" -> {
                            NavigationHostWrapper(onBack = { currentScreen = "main" })
                        }

                        // 프로젝트 생성
                        "create_project" -> {
                            // 완료 시 고정된 main이 아니라, 이전 previousScreen 화면으로 이동
                            CreateProjectScreen(onBack = { currentScreen = previousScreen })
//                            CreateProjectScreen(onBack = { currentScreen = "main" })
                        }

                        // 정밀 측정
                        "survey" -> {
//                            SurveyMeasurementScreen(onBackClick = { currentScreen = "main" })
                            val context = androidx.compose.ui.platform.LocalContext.current

                            // 가져온 context를 사용
                            val db = AppDatabase.getDatabase(context)

                            SurveyMeasurementScreen(
                                measurementDao = db.measurementDao(),
                                onBackClick = { currentScreen = "main" }
                            )
                        }

                        // 프로젝트 리스트
                        "project_list" -> {
                            ProjectListScreen(
                                projectViewModel = projectViewModel,
//                                projectViewModel = viewModel<com.terra.terradisto.viewmodel.ProjectViewModel>(),
                                // 인스턴스를 새로 파던 부분을 기존 멤버 변수 주입 구조로 올바르게 교체
                                distoViewModel = distoViewModel,
//                                onNavigateToCreate = {
//                                    previousScreen = "project_list"
//                                    currentScreen = "create_project"
//                                 },

                                onNavigateToSurvey = {
                                    currentScreen = "survey"
                                },

                                onCreateClick = {
                                    // 프로젝트 리스트 화면에서 진입했음을 기록
                                    previousScreen = "project_list"
                                    currentScreen = "create_project"
                                },

                                onConnectClick = { currentScreen = "connect" }
                            )
                        }

                        "history" -> {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val db = AppDatabase.getDatabase(context)

                            val selectedProjectId = selectedProject?.id ?: -1L
                            val historyItems by db.measurementDao()
                                .getMesurementByProject(selectedProjectId)
                                .collectAsState(initial = emptyList())

                            MeasureHistoryScreen(
                                items = historyItems,
                                onBackClick = { currentScreen = "main" }
                            )
                        }
                    }
                }
            }
        }
    }

    // 상태값 변경
    override fun onStatusChanged(isConnected: Boolean) {
        runOnUiThread {
            viewModel.updateConnectionStatus(isConnected) // SDK 연동용 상태 업데이트
            distoViewModel.updateConnectionState(isConnected) // DistoMainScreen, ProjectList 뷰모델 동기화
        }
    }
}


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