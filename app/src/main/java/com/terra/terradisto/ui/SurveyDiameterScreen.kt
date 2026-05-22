package com.terra.terradisto.ui

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terra.terradisto.distosdkapp.clipboard.Clipboard
import com.terra.terradisto.distosdkapp.device.YetiDeviceController
import ch.leica.sdk.Devices.Device
import com.terra.terradisto.data.MeasurementDao
import com.terra.terradisto.ui.components.PipeDirectionDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ActiveTarget {
    NONE, LID_SIZE, TOPI, CHAMBER_SIZE, PIPE_SIZE, PIPE_HEIGHT_SIZE
}

data class PipeUiItem(
    val id: Int,
    var direction: String = "",
    var size: String = "",
    var height: String = "",
    var material: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyMeasurementScreen(
    currentProjectId: Long = 1L,
    measurementDao: MeasurementDao, // DB 저장을 처리할 Dao
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    // 외부에서 전달받는 상태값들을 그대로 사용
    distanceState: MutableState<String> = remember { mutableStateOf("") },
    angleState: MutableState<String> = remember { mutableStateOf("") },
    buttonTextState: MutableState<String> = remember { mutableStateOf("측정 시작") }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // 팝업 상태 추가
    var showSaveToast by remember { mutableStateOf(false) }

    // 팝업 제어 및 관경 방향 상태
    var showDirectionDialog by remember { mutableStateOf(false) }
    var pipeDirection by remember { mutableStateOf("") }

    // 개별 필드 상태 관리
    var manholeType by remember { mutableStateOf("") }
    var lidSize by remember { mutableStateOf("") }      // 1. 맨홀 뚜껑 규격
    var topieValue by remember { mutableStateOf("") }   // 2. 토피
    var chamberSize by remember { mutableStateOf("") }  // 3. 변실 규격
    var pipeSize by remember { mutableStateOf("") }     // 4. 관경
    var pipeHeight by remember { mutableStateOf("") }   // 5. 관 높이
    var lidMaterial by remember { mutableStateOf("") }
    var pipeMaterial by remember { mutableStateOf("") }

    var chamberMaterial by remember { mutableStateOf("") }
    var isMaterialExpanded by remember { mutableStateOf(false) }

    // 변실 모양
    var selectedChamberShape by remember { mutableStateOf("사각형") }

    var hasLadder by remember { mutableStateOf(false) }
    var hasInverter by remember { mutableStateOf(false) }

    var pipeList by remember { mutableStateOf(listOf(PipeUiItem(id = 1))) }
    var activePipeIndexForDialog by remember { mutableStateOf(0) }

    // Disto 제어 상태
    var activeTarget by remember { mutableStateOf(ActiveTarget.NONE) }
    var isMeasuring by remember { mutableStateOf(false) }
    var lastDistance by remember { mutableStateOf(Double.NaN) }
    var maxDistance by remember { mutableStateOf(Double.NEGATIVE_INFINITY) }
    var trendingUp by remember { mutableStateOf(false) }
    var decreaseCount by remember { mutableStateOf(0) } // 연속 감소 카운트
    val EPS = 0.002

    // 이상 상태 유무(메모) 상태 정의
    var anomalyMemo by remember { mutableStateOf("") }

    // 재질 드롭다운 메뉴 확장을 위한 상태 정의
    var isLidMaterialExpanded by remember { mutableStateOf(false) }
    val materialOptions = listOf("주철", "SG(스틸그레이팅)", "칼라콘크리트", "직접 입력")
    var isCustomMaterialInput by remember { mutableStateOf(false) }

    // YetiDeviceController를 Compose 안에서 초기화 및 리스너 맵핑
    val yetiController = remember {
        YetiDeviceController(
            context.applicationContext,
            object : YetiDeviceController.YetiDataListener {
                override fun onBasicMeasurements_Received(basicData: YetiDeviceController.BasicData?) {
                    if (!isMeasuring) return

                    basicData?.let { data ->
                        val dist = parseDoubleSafe(data.distance)

                        // 최대값 비교 로직: 이전 측정값보다 큰 경우에만 UI에 반영
//                        if (!dist.isNaN() && dist > maxDistance) {
                        // 최대값 비교 로직 : 이전 최고점보다 큰 경우에만 UI 반영
                        if (dist > maxDistance + EPS) {
                            maxDistance = dist
                            decreaseCount = 0
                            trendingUp = true

                            val formattedValue = "${data.distance} ${data.distanceUnit ?: ""}"
                            val formattedAngle = "${data.inclination ?: ""} ${data.inclinationUnit ?: ""}"

                            when (activeTarget) {
                                ActiveTarget.LID_SIZE -> lidSize = formattedValue
                                ActiveTarget.TOPI -> topieValue = formattedValue
                                ActiveTarget.CHAMBER_SIZE -> chamberSize = formattedValue
                                ActiveTarget.PIPE_SIZE -> {
                                    pipeSize = formattedValue
                                    if (pipeList.isNotEmpty()) {
                                        pipeList[0].size = formattedValue
                                    }
                                }
                                ActiveTarget.PIPE_HEIGHT_SIZE -> {
                                    pipeHeight = formattedValue
                                    if (pipeList.isNotEmpty()) {
                                        pipeList[0].height = formattedValue
                                    }
                                }
                                else -> {}
                            }
                        }

                        else if (trendingUp && dist < maxDistance - EPS) {
                            decreaseCount++
                            // 2~3회 연속으로 최대값보다 작은 값이 들어오면 측정이 끝난 것으로 판단
                            if (decreaseCount >= 2) {
                                isMeasuring = false
                                buttonTextState.value = "측정 시작"
                                activeTarget = ActiveTarget.NONE
                            }
                        }

                        lastDistance = dist

                        // 기존 Java의 '감소 감지 시 정지' 로직 그대로 이식
//                        if (!dist.isNaN()) {
//                            if (lastDistance.isNaN()) {
//                                lastDistance = dist
//                            } else {
//                                if (dist > lastDistance + EPS) {
//                                    trendingUp = true
//                                } else if (trendingUp && dist < lastDistance - EPS) {
//                                    isMeasuring = false
//                                    buttonTextState.value = "측정 시작"
//                                }
//                                lastDistance = dist
//                            }
//                        }
                    }
                }

                // 나머지 리스너는 빈 값으로 유지
                override fun onP2PMeasurements_Received(p2pData: YetiDeviceController.P2PData?) {}
                override fun onQuaternionMeasurement_Received(d: YetiDeviceController.QuaternionData?) {}
                override fun onAccRotationMeasurement_Received(d: YetiDeviceController.AccRotData?) {}
                override fun onMagnetometerMeasurement_Received(d: YetiDeviceController.MagnetometerData?) {}
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
            })
    }

    //  화면 시작 시 디바이스 주입 및 연결 체크
    LaunchedEffect(Unit) {
        val info = Clipboard.INSTANCE.informationActivityData
        if (info?.device != null && info.device.deviceType == ch.leica.sdk.Types.DeviceType.Yeti) {
            yetiController.setCurrentDevice(info.device)
            yetiController.setListeners()
            yetiController.checkForReconnection(context)
        }
    }

    //  측정 시작 시 1초 간격으로 명령을 보내는 루프 로직
    LaunchedEffect(isMeasuring) {
        if (isMeasuring) {
            while (isMeasuring) {
                Thread { yetiController.sendDistanceCommand() }.start()
                delay(1000) // 기존 Handler의 postDelayed와 동일한 역할
            }
        }
    }

    //  측정 중지 및 상태 초기화 함수
    val startMeasurementFor = { target: ActiveTarget ->
        val dev = yetiController.currentDevice
        if (dev == null || dev.connectionState != Device.ConnectionState.connected) {
            Toast.makeText(context, "기기를 연결하세요.", Toast.LENGTH_SHORT).show()
        } else {
            if (!isMeasuring) {
                activeTarget = target // 어떤 칸을 채울지 지정
                isMeasuring = true
                buttonTextState.value = "측정 정지"
                lastDistance = Double.NaN
                maxDistance = Double.NEGATIVE_INFINITY
                trendingUp = false
                decreaseCount = 0 // 시작시 초기화
            } else {
                isMeasuring = false
                buttonTextState.value = "측정 시작"
                activeTarget = ActiveTarget.NONE
            }
        }
    }

    val isAtBottom = scrollState.value >= (scrollState.maxValue - 10)
//    val isFormComplete = topieValue.isNotEmpty()

    // 전체 입력 완료 체크 로직
    val isFormComplete = manholeType.isNotEmpty() && lidMaterial.isNotEmpty() &&
            topieValue.isNotEmpty() && pipeList.all { it.direction.isNotEmpty() && it.size.isNotEmpty() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "현장 측정",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF191F28)
                        )
                        Text("MH-Master 프로젝트", fontSize = 12.sp, color = Color(0xFF8B95A1))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "뒤로가기",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { /* 기록 보기 */ }) {
                        Text("기록", color = Color(0xFF3182F6), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(
                        0xFFF2F4F6
                    )
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 20.dp
            ) {
//                AnimatedContent(targetState = isAtBottom && isFormComplete) { isReadyToSave ->
                AnimatedContent(targetState = isFormComplete) { isReadyToSave ->
                    if (isReadyToSave) {
                        Button(
                            onClick = {
                                onSaveClick()
                                Toast.makeText(context, "저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                                .height(58.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3182F6))
                        ) {
                            Icon(Icons.Rounded.CloudUpload, null)
                            Spacer(Modifier.width(8.dp))
                            Text("측정 데이터 저장하기", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                                .height(58.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF2F4F6),
                                contentColor = Color(0xFF3182F6)
                            )
                        ) {
                            Text("아래로 스크롤하기", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Rounded.ArrowDownward, null)
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF2F4F6)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F4F6))
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 맨홀 기본 정보
            FormSection(title = "맨홀명", icon = Icons.Rounded.Info) {
                InputField(
                    label = "맨홀 타입",
                    placeholder = "예: MH-001",
                    value = manholeType,
                    onValueChange = { manholeType = it })
            }

            // 2. 맨홀 뚜껑 상세
            FormSection(title = "맨홀", icon = Icons.Rounded.RadioButtonChecked) {
                val materialOptions = listOf("주철", "SG(스틸그레이팅)", "칼라콘크리트", "직접 입력")
                var isDropDownExpanded by remember { mutableStateOf(false) }

                Text(
                    text = "맨홀 뚜껑의 재질",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B95A1),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = isDropDownExpanded,
                    onExpandedChange = { isDropDownExpanded = !isDropDownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = lidMaterial.ifEmpty { "선택하세요" },
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (lidMaterial.isEmpty()) Color(0xFF3182F6) else Color(0xFF191F28),
                            unfocusedTextColor = if (lidMaterial.isEmpty()) Color(0xFF3182F6) else Color(0xFF191F28),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF3182F6),
                            unfocusedBorderColor = if (lidMaterial.isEmpty()) Color(0xFF3182F6) else Color(0xFFE5E8EB)
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropDownExpanded) }
                    )

                    // 기획안 이미지의 드롭다운 팝업 스타일 완벽 재현
                    ExposedDropdownMenu(
                        expanded = isDropDownExpanded,
                        onDismissRequest = { isDropDownExpanded = false },
                        modifier = Modifier
                            .background(Color(0xFF4E5968), shape = RoundedCornerShape(14.dp))
                            .fillMaxWidth()
                    ) {
                        // 기본 선택 항목 구성
                        DropdownMenuItem(
                            text = { Text("✓ 선택하세요", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                            onClick = {
                                lidMaterial = ""
                                isDropDownExpanded = false
                            }
                        )
                        materialOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = Color.White, fontSize = 15.sp) },
                                onClick = {
                                    lidMaterial = option
                                    isDropDownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 맨홀 뚜껑의 규격 입력 및 측정 필드 레이아웃 개선
                Text(
                    text = "맨홀 뚜껑의 규격",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B95A1),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = lidSize,
                        onValueChange = { lidSize = it },
                        placeholder = { Text("예: 600mm", color = Color(0xFFB0B8C1)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF191F28),
                            unfocusedTextColor = Color(0xFF191F28),
                            focusedContainerColor = Color(0xFFF2F4F6),
                            unfocusedContainerColor = Color(0xFFF2F4F6),
                            focusedBorderColor = Color(0xFF3182F6),
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent
                        )
                    )

                    // 기획안 우측의 파란색 '측정' 버튼 매핑 완료 (기존 SDK 측정 로직 완벽 유지)
                    Button(
                        onClick = { startMeasurementFor(ActiveTarget.LID_SIZE) },
                        modifier = Modifier.height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3182F6),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        Icon(Icons.Rounded.Construction, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (activeTarget == ActiveTarget.LID_SIZE) buttonTextState.value else "측정",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 기획안 이미지 하단의 '이상상태유무 (메모)' 입력 필드 영역 구현
                Text(
                    text = "이상상태유무 (메모)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B95A1),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = anomalyMemo,
                    onValueChange = { anomalyMemo = it },
                    placeholder = { Text("이상 상태를 메모하세요", color = Color(0xFFB0B8C1)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF191F28),
                        unfocusedTextColor = Color(0xFF191F28),
                        focusedContainerColor = Color(0xFFF2F4F6),
                        unfocusedContainerColor = Color(0xFFF2F4F6),
                        focusedBorderColor = Color(0xFF3182F6),
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent
                    )
                )
            }

            // 3. 토피 (강조 섹션)
            FormSection(title = "토피", icon = Icons.Rounded.Height, isRequired = true) {
                InputField(
                    label = "토피 (수기 입력) *",
                    placeholder = "예: 1.5m",
                    value = topieValue,
                    onValueChange = { topieValue = it },
                    isHighlight = true,
//                    hasMeasure = true,
//                    onMeasureClick = { startMeasurementFor(ActiveTarget.TOPI) },
//                    buttonText = if (activeTarget == ActiveTarget.TOPI) buttonTextState.value else "측정 시작"
                )
            }

            // 변실 재질
            FormSection(title = "변실", icon = Icons.Rounded.HomeWork) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    //1. 재질 레이블 및 드롭다운 선택 필드
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "재질",
                            fontSize = 13.sp,
                            fontWeight =  FontWeight.Bold,
                            color = Color(0xFF8B95A1)
                        )

                        Box {
                            Surface (
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clickable { isMaterialExpanded = true },
                                color = Color(0xFFF2F4F6),
                                shape = RoundedCornerShape(16.dp)
                            ){
                                Row (
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ){
                                    Text(
                                        text = chamberMaterial.ifEmpty { "선택하세요" },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF191F28)
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = "재질 선택",
                                        tint = Color(0xFF4E5968),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isMaterialExpanded,
                                onDismissRequest = { isMaterialExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f).background(Color.White)
                            ) {
                                val materials = listOf("콘크리트", "벽돌", "기타")
                                // 💡 [해결] 중복 할당 제거 및 람다 변수명 충돌 버그 수정
                                materials.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item, fontSize = 15.sp, color = Color(0xFF191F28)) },
                                        onClick = {
                                            chamberMaterial = item
                                            isMaterialExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column (
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ){
                        Text(
                            text = "규격",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B95A1)
                        )

                        Row (
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                color = Color(0xFFF2F4F6),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (chamberSize.isEmpty()) {
                                        Text(
                                            text = "예: 1200 x 1200",
                                            color = Color(0xFFB0B8C1),
                                            fontSize = 16.sp
                                        )
                                    }
                                    // 수기 입력을 위한 BasicTextField
                                    BasicTextField(
                                        value = chamberSize,
                                        onValueChange = { chamberSize = it },
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF191F28)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Button(
                                onClick = { startMeasurementFor(ActiveTarget.CHAMBER_SIZE) },
                                modifier = Modifier.height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeTarget == ActiveTarget.CHAMBER_SIZE) Color(0xFFFF4D4F) else Color(0xFF3182F6),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Straighten, // 자(Measurement) 아이콘 매핑
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (activeTarget == ActiveTarget.CHAMBER_SIZE) buttonTextState.value else "측정",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 5. 변실
            FormSection(title = "변실 모양", icon = Icons.Rounded.HomeWork) {
                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ){
                    SelectableBox(
                        text = "사각형 (ㅁ)",
                        isSelected = selectedChamberShape == "사각형",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedChamberShape = "사각형" }
                    )

                    SelectableBox(
                        text = "원형 (ㅇ)",
                        isSelected = selectedChamberShape == "원형",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedChamberShape = "원형" }
                    )
                }
            }

            // 5. 사다리 인버터 유무
            FormSection(title = "사다리 인버터 유무", icon = Icons.Rounded.HomeWork) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToggleRowBox(
                        label = "사다리",
                        isActive = hasLadder,
                        onToggle = { hasLadder = it }
                    )
                    ToggleRowBox(
                        label = "인버터",
                        isActive = hasInverter,
                        onToggle = { hasInverter = it }
                    )
                }
            }

            // 6. 관경 정보
            FormSection(
                title = "관경",
                icon = Icons.Rounded.SettingsInputComponent,
                hasAddButton = true,
                onAddClick = {
                    val newId = pipeList.size + 1
                    pipeList = pipeList + PipeUiItem(id = newId)
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    pipeList.forEachIndexed { index, pipeItem ->
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFF2F4F6))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row (
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ){
                                    Text(
                                        "관 ${index + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF191F28)
                                    )

                                    if ( pipeList.size >= 2 ){
                                        IconButton(
                                            onClick = {
                                                pipeList = pipeList.filterIndexed { i, _ -> i != index }
                                            },
                                            modifier = Modifier
                                                .background(Color(0xFFFEEBEE), CircleShape).size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Remove,
                                                contentDescription = "관 삭제",
                                                tint = Color(0xFFF04438),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                InputField(
                                    label = "관 방향",
                                    placeholder = "선택 버튼을 눌러주세요",
                                    value = pipeItem.direction,
                                    readOnly = true,
                                    onValueChange = { },
                                    hasSelect = true,
                                    onSelectClick = {
                                        activePipeIndexForDialog = index
                                        showDirectionDialog = true
                                    },

                                    // 지우기 버튼 동작 시 기존 리스트 내부 개체 상태 초기화
                                    onClearClick = {
                                        pipeList = pipeList.mapIndexed { i, p ->
                                            if (i == index) p.copy(direction = "") else p
                                        }
                                    },

                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            activePipeIndexForDialog = index
                                            showDirectionDialog = true
                                        }
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                InputField(
                                    label = "관경",
                                    placeholder = "예: 200mm",
                                    value = pipeSize,
                                    onValueChange = { pipeSize = it },
                                    hasMeasure = true,
                                    onMeasureClick = { startMeasurementFor(ActiveTarget.PIPE_SIZE) },
                                    buttonText = if (activeTarget == ActiveTarget.PIPE_SIZE) buttonTextState.value else "측정 시작"
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                InputField(
                                    label = "높이",
                                    placeholder = "예: 1.5m",
                                    value = pipeHeight,
                                    onValueChange = { pipeHeight = it },
                                    hasMeasure = true,
                                    onMeasureClick = { startMeasurementFor(ActiveTarget.PIPE_HEIGHT_SIZE) },
                                    buttonText = if (activeTarget == ActiveTarget.PIPE_HEIGHT_SIZE) buttonTextState.value else "측정 시작"
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                InputField(
                                    label = "재질",
                                    placeholder = "예: PVC",
                                    value = pipeItem.material,
                                    onValueChange = { newValue ->
                                        pipeList = pipeList.mapIndexed { i, item ->
                                            if (i == index) item.copy(material = newValue) else item
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (showDirectionDialog) {
            PipeDirectionDialog(
                onDismiss = { showDirectionDialog = false },
                onSelect = { selectedDir ->
                    val currentItem = pipeList.getOrNull(activePipeIndexForDialog)
                    currentItem?.let { item ->
                        val updatedDirection = if (item.direction.isEmpty()) selectedDir else "${item.direction} -> $selectedDir"
                        pipeList = pipeList.mapIndexed { i, p ->
                            if (i == activePipeIndexForDialog) p.copy(direction = updatedDirection) else p
                        }
                    }
                }
            )
        }
    }
}

//  유틸 함수를 파일 레벨 혹은 클래스 내부에 정의
private fun parseDoubleSafe(s: String?): Double {
    if (s == null) return Double.NaN
    return try {
        val normalized = s.trim().replace(",", ".").replace(Regex("[^0-9+\\-Ee.]"), "")
        if (normalized.isEmpty()) Double.NaN else normalized.toDouble()
    } catch (e: Exception) {
        Double.NaN
    }
}

// --- 공통 컴포넌트 ---
@Composable
fun FormSection(
    title: String,
    icon: ImageVector,
    isRequired: Boolean = false,
    hasAddButton: Boolean = false,
    onAddClick: () -> Unit = {}, // 파라미터 추가 (기본값 지정)
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE5E8EB).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF191F28)
                    )
                    if (isRequired) {
                        Surface(
                            color = Color(0xFFE8F3FF),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                "필수",
                                color = Color(0xFF3182F6),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (hasAddButton) {
                    IconButton(
                        onClick = onAddClick,
                        modifier = Modifier
                            .background(Color(0xFF3182F6), CircleShape)
                            .size(28.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
fun InputField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit = {},
    hasMeasure: Boolean = false,
    hasSelect: Boolean = false,
    isHighlight: Boolean = false,
    readOnly: Boolean = false, // 추가
    modifier: Modifier = Modifier, // 추가
    buttonText: String = "측정",
    onMeasureClick: () -> Unit = {},
    onSelectClick: () -> Unit = {},
    onClearClick: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF8B95A1),
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = modifier
                    .weight(1f)
                    .onFocusChanged {
                        isFocused = it.isFocused
                    }, // 상위에서 보낸 Modifier.clickable이 여기서 작동!
                color = if (isHighlight) Color(0xFFF8F9FA) else Color(0xFFF2F4F6),
                shape = RoundedCornerShape(14.dp),
                border = when {
                    isHighlight -> BorderStroke(1.dp, Color(0xFF3182F6))
                    isFocused -> BorderStroke(1.2.dp, Color(0xFF3182F6).copy(alpha = 0.8f))
                    else -> null
                }
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    readOnly = readOnly,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) Text(
                            placeholder,
                            color = Color(0xFFADB5BD),
                            fontSize = 15.sp
                        )
                        innerTextField()
                    }
                )
            }

            if (hasMeasure) {
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onMeasureClick,
                    modifier = Modifier.height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3182F6))
                ) {
                    Icon(Icons.Rounded.Straighten, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(buttonText, fontWeight = FontWeight.Bold)
                }
            }

            if (hasSelect) {
                Spacer(Modifier.width(8.dp))
                AnimatedContent(targetState = value.isNotEmpty()) { hasData ->
                    if (hasData) {
                        // 데이터가 들어온 상태: [지우기 아이콘] + [변경] 버튼을 유기적으로 배치
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 토스 고유의 터치하기 편한 원형 회색 X 아이콘 지우기 버턴
                            IconButton(
                                onClick = onClearClick,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Cancel,
                                    contentDescription = "초기화",
                                    tint = Color(0xFFB0B8C1),
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    onSelectClick()
                                },
                                modifier = Modifier.height(54.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE8F3FF),
                                    contentColor = Color(0xFF3182F6)
                                )
                            ) {
                                Text("변경", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    } else {
                        // 데이터가 비어있는 상태: 기본 버튼 스위칭
                        Button(
                            onClick = onSelectClick,
                            modifier = Modifier.height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE8F3FF),
                                contentColor = Color(0xFF3182F6)
                            )
                        ) {
                            Icon(Icons.Rounded.Explore, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("선택", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DropdownField(label: String, selectedValue: String) {
    Column {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF8B95A1),
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { },
            color = Color(0xFFF2F4F6),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedValue, fontSize = 15.sp, color = Color(0xFF191F28))
                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color(0xFF8B95A1))
            }
        }
    }
}

@Composable
fun SelectableBox(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable{ onClick() },
        color = if (isSelected) Color(0xFFF2F8FF) else Color(0xFFF2F4F6),
        shape = RoundedCornerShape(14.dp),
        border = if (isSelected) BorderStroke(1.2.dp, Color(0xFF3182F6)) else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                null,
                tint = if (isSelected) Color(0xFF3182F6) else Color(0xFFADB5BD),
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(8.dp))
            Text(
                text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF3182F6) else Color(0xFF4E5968)
            )
        }
    }
}

@Composable
fun ToggleRowBox(
    label: String,
    isActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row (
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(Color(0xFFF2F4F6), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ){
        Row (verticalAlignment = Alignment.CenterVertically){
            Icon(
                if (isActive) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = if (isActive) Color(0xFF3182F6) else Color(0xFF8B95A1),
                modifier = Modifier
                    .size(22.dp)
                    .clickable { onToggle(!isActive) }
            )

            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF191F28)
            )
        }

        // 우측 토글 뱃지 (무 / 유) 형태 구현
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onToggle(!isActive) },
            color = if (isActive) Color(0xFF3182F6) else Color(0xFFE5E8EB)
        ) {
            Text(
                text = if (isActive) "유" else "무",
                color = if (isActive) Color.White else Color(0xFF6B7684),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=1080px, height=2340px, dpi=440")
@Composable
fun PreviewSurveyMeasurementScreen() {
    MaterialTheme {
        val distState = remember { mutableStateOf("1.524 m") }
        val angleState = remember { mutableStateOf("12.5°") }
        val buttonState = remember { mutableStateOf("측정 시작") }

        // Preview를 위한 가짜 Dao 구현
        val dummyDao = object : com.terra.terradisto.data.MeasurementDao {
            override suspend fun insertMeasurement(measurement: com.terra.terradisto.data.MeasurementEntity) {}
            override fun getMesurementByProject(projectId: Long): kotlinx.coroutines.flow.Flow<List<com.terra.terradisto.data.MeasurementEntity>> =
                kotlinx.coroutines.flow.flowOf(emptyList())
            override suspend fun deleteMeasurement(measurement: com.terra.terradisto.data.MeasurementEntity) {}
        }

        SurveyMeasurementScreen(
            measurementDao = dummyDao,
            distanceState = distState,
            angleState = angleState,
            buttonTextState = buttonState
        )
    }
}