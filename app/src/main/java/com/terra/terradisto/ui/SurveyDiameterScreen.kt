package com.terra.terradisto.ui

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terra.terradisto.data.MeasurementDao
import com.terra.terradisto.data.MeasurementEntity
import com.terra.terradisto.data.PipeUiItem
import com.terra.terradisto.ui.components.PipeDirectionDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

//data class PipeUiItem(
//    val id: Int = 0, // 기본값 추가
//    var direction: String = "",
//    var diameter: String = "", // size 대신 diameter로 수정
//    var height: String = "",
//    var material: String = ""
//)
enum class ActiveTarget {
    NONE, LID_SIZE, TOPI, CHAMBER_SIZE, CHAMBER_WIDTH, CHAMBER_HEIGHT, PIPE_SIZE, PIPE_HEIGHT_SIZE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyDiameterScreen(
    currentProjectId: Long,
    isDistoConnected: Boolean,
    distoMeasuredDistance: String,
    measurementDao: MeasurementDao, // 저장 기능 추가
    onMeasureClick: () -> Unit,
    onBackClick: () -> Unit,
    onHistoryClick: () -> Unit // 기록 화면
) {
    // 측정 상태 관리
    var currentDistance by remember { mutableStateOf("0.000") }
    var isMeasuring by remember { mutableStateOf(false) }

    // 최대값 감지 로직 상태
    var maxDistance by remember { mutableDoubleStateOf(0.0) }
    var decreaseCount by remember { mutableIntStateOf(0) }
    var trendingUp by remember { mutableStateOf(false) }
    val EPS = 0.002 // 2mm 오차 허용

    // 입력 상태
    var manholeName by remember { mutableStateOf("") }
    var selectedMaterial by remember { mutableStateOf("선택하세요") }
    var expanded by remember { mutableStateOf(false) }
    var isCustomInput by remember { mutableStateOf(false) } // 직접 입력 상태 관리
    var customMaterial by remember { mutableStateOf("") } // 직접 입력값
    var memo by remember { mutableStateOf("") }
    val materials = listOf("선택하세요", "주철", "SG(스틸그레이팅)", "칼라콘크리트", "직접 입력")

    // 토피 상태
    var topiValue by remember { mutableStateOf("") }

    /**
     * 변실 상태
     * 사각형 / 원형 선택값에 따른 UI 변동
     */
    var chamberWidth by remember { mutableStateOf("") }
    var chamberHeight by remember { mutableStateOf("") }
    var chamberDiameter by remember { mutableStateOf("") }
    var chamberShape by remember { mutableStateOf<String?>("원형") } // 사각형, 원형
    var chamberSize by remember { mutableStateOf("") }

    // 사다리 인버트 상태
    var hasLadder by remember { mutableStateOf(false) }
    var hasInvert by remember { mutableStateOf(false) }

    // 관경 데이터 리스트 관리
    val pipeList = remember { mutableStateListOf(PipeUiItem(id = 0)) }
    var activePipeTarget by remember { mutableStateOf<Pair<Int, String>?>(null) } // 특정 관의 관경/높이 타겟팅을 위해 인덱스 활용
    var activeTarget by remember { mutableStateOf(ActiveTarget.NONE) } // 측정 관련 상태

    val buttonTextState = remember { mutableStateOf("측정") }
    var lidSize by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() } // 팝업 메세지용 상태 값
    var showSaveSuccessDialog by remember { mutableStateOf(false) } // 저장 성공 알림 다이얼로그 상태 값

    // 관 방향 다이얼로그 관련 상태
    var showDirectionDialog by remember { mutableStateOf(false) }
    var activePipeIndexForDialog by remember { mutableIntStateOf(0) }

    // 뒤로 가기 확인 다이얼로그 상태
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    // 기록 확인 다이얼로그 상태
    var showHistoryConfirmDialog by remember { mutableStateOf(false) }

    // 시스템 백 버튼 클랙 시 바로 나가지 않고 다이얼로그 표시
    BackHandler(enabled = true) {
        showExitConfirmDialog = true
    }

    // 연속 측정 로직
    LaunchedEffect(isMeasuring) {
        if (isMeasuring) {
            maxDistance = 0.0
            decreaseCount = 0
            trendingUp = false

            while (isMeasuring) {
                onMeasureClick()
                delay(1000) // 1초마다 명령 전송
            }
        }
    }

    /**
     * 1. 실제 측정 값 연동 로직
     * 2. 최대값 감지 및 데이터 갱신 로직
     */
    LaunchedEffect(distoMeasuredDistance) {
        if (!isMeasuring) return@LaunchedEffect
        Log.d("SurveyDebug", "Received distance: $distoMeasuredDistance")

        val dist = distoMeasuredDistance.toDoubleOrNull() ?: return@LaunchedEffect
        if (dist <= 0.0) return@LaunchedEffect
        if (!isMeasuring) return@LaunchedEffect

        if (dist > maxDistance + EPS) {
            maxDistance = dist
            decreaseCount = 0
            trendingUp = true

            val targetIndex = activePipeTarget?.first ?: -1
            val targetType = activePipeTarget?.second ?: ""

            when (activeTarget) {
                ActiveTarget.LID_SIZE -> lidSize = String.format("%.3f", maxDistance)
                ActiveTarget.TOPI -> topiValue = String.format("%.3f", maxDistance)
                ActiveTarget.CHAMBER_WIDTH -> chamberWidth = String.format("%.3f", maxDistance)
                ActiveTarget.CHAMBER_HEIGHT -> chamberHeight = String.format("%.3f", maxDistance)
                ActiveTarget.CHAMBER_SIZE -> chamberDiameter = String.format("%.3f", maxDistance)
                ActiveTarget.PIPE_SIZE -> {
                    activePipeTarget?.let { (index, type) ->
                        if (type == "diameter") {
                            pipeList[index] =
                                pipeList[index].copy(diameter = String.format("%.3f", maxDistance))
                        }
                    }
                }

                ActiveTarget.PIPE_HEIGHT_SIZE -> {
                    if (targetIndex != -1 && targetIndex < pipeList.size && targetType == "height") {
                        pipeList[targetIndex] =
                            pipeList[targetIndex].copy(height = String.format("%.3f", maxDistance))
                    }
                }

                else -> {}
            }
        } else if (trendingUp && dist < maxDistance - EPS) {
            decreaseCount++
            if (decreaseCount >= 2) {
                isMeasuring = false
                currentDistance = String.format("%.3f", maxDistance)
                lidSize = currentDistance
                buttonTextState.value = "측정"
                activeTarget = ActiveTarget.NONE // 측정 종료 시 타겟 초기화
            }
        }

//        if (isMeasuring && distoMeasuredDistance.isNotEmpty()) {
//            currentDistance = distoMeasuredDistance
//        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Snackbar 추가
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
                        Text("TERRA DISTO", fontSize = 12.sp, color = Color(0xFF8B95A1))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showExitConfirmDialog = true }) {
                        Icon(
                            Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "뒤로가기",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { showHistoryConfirmDialog = true }) {
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
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    SaveButton(onClick = {
                        if (manholeName.trim().isEmpty()) {
                            // 1. 맨홀명 필수 체크
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("맨홀명을 입력해주세요.")
                            }
                        } else {
                            coroutineScope.launch {
                                try {
                                    val measurement = MeasurementEntity(
                                        projectId = currentProjectId,
                                        manholeType = manholeName,
                                        lidMaterial = selectedMaterial,
                                        lidSize = lidSize,
                                        topieValue = topiValue,
                                        chamberMaterial = chamberShape ?: "",
                                        chamberSize = if (chamberShape == "사각형") "${chamberWidth} x ${chamberHeight}" else chamberDiameter,
                                        selectedChamberShape = chamberShape ?: "원형",
                                        hasLadder = hasLadder,
                                        hasInverter = hasInvert,
                                        anomalyMemo = memo,
                                        pipeList = pipeList.toList(),
                                        timestamp = System.currentTimeMillis()
                                    )

                                    measurementDao.insertMeasurement(measurement)

                                    // 저장이 완료되면 모든 필드 초기화
                                    manholeName = ""
                                    selectedMaterial = "선택하세요"
                                    isCustomInput = false
                                    customMaterial = ""
                                    lidSize = ""
                                    topiValue = ""
                                    chamberWidth = ""
                                    chamberHeight = ""
                                    chamberDiameter = ""
                                    memo = ""
                                    hasLadder = false
                                    hasInvert = false
                                    pipeList.clear()
                                    pipeList.add(PipeUiItem(id = 0))
                                    isMeasuring = false
                                    activeTarget = ActiveTarget.NONE
                                    activePipeTarget = null
                                    maxDistance = 0.0
                                    decreaseCount = 0
                                    trendingUp = false

                                    showSaveSuccessDialog = true // 상태값 변경
                                } catch (e: Exception) {
                                    if (e is CancellationException) throw e

                                    Log.e("SurveyDebug", "Error saving survey data : ", e)
                                    Toast.makeText(
                                        context,
                                        "저장 중 오류가 발생했습니다.\n${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    })
                }
            }
        }
    ) { padding ->
        if (showSaveSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSaveSuccessDialog = false },
                shape = RoundedCornerShape(24.dp), //  특유의 둥글고 세련된 느낌 적용
                containerColor = Color.White,
                title = {
                    Text(
                        text = "저장이 완료되었어요",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191F28) //  주요 다크 그레이
                    )
                },
                text = {
                    Text(
                        text = "데이터가 안전하게 저장되었습니다.\n" +
                                "측정 내역 화면에서 확인하실 수 있어요.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4E5968),
                        lineHeight = 22.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showSaveSuccessDialog = false },
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
                            text = "확인",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = null
            )
        }

        if (showHistoryConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showHistoryConfirmDialog = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color.White,
                title = {
                    Text(
                        text = "기록 화면으로 이동할까요?",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191F28)
                    )
                },
                text = {
                    Text(
                        text = "현재 측정 진행 중이에요. 측정 내역 화면으로 이동하시겠어요?\n저장되지 않은 데이터는 지워집니다.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4E5968),
                        lineHeight = 22.sp
                    )
                },
                confirmButton = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 이동하기 버튼
                        Button(
                            onClick = {
                                showHistoryConfirmDialog = false
                                onHistoryClick() // 기록 화면으로 이동
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3182F6))
                        ) {
                            Text("이동하기", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        // 닫기 버튼
                        TextButton(
                            onClick = { showHistoryConfirmDialog = false },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                "닫기",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B95A1)
                            )
                        }
                    }
                }
            )
        }

        // 뒤로가기 확인 다이얼로그
        if (showExitConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showExitConfirmDialog = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color.White,
                title = {
                    Text(
                        text = "측정 화면에서 나갈까요?", // [수정] 타이틀
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191F28)
                    )
                },
                text = {
                    Text(
                        text = "측정 화면에서 정말 나가시겠어요?\n저장되지 않은 데이터는 지워집니다.", // [수정] 본문
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4E5968),
                        lineHeight = 22.sp
                    )
                },
                confirmButton = {
                    // 버튼 레이아웃: 나가기와 취소를 세로로 배치
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                showExitConfirmDialog = false
                                onBackClick() // 실제 화면 나가기 실행
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
                            Text("나가기", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = { showExitConfirmDialog = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                "취소",
                                color = Color(0xFF4E5968),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }

        // 관 방향 선택 다이얼로그
        if (showDirectionDialog) {
            PipeDirectionDialog(
                onDismiss = { showDirectionDialog = false },
                onSelect = { direction ->
                    // 선택한 번호를 해당 관의 direction에 반영
                    pipeList[activePipeIndexForDialog] =
                        pipeList[activePipeIndexForDialog].copy(direction = direction)
                    showDirectionDialog = false
                }
            )
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 입력 폼 섹션
            FormSection(title = "맨홀명", icon = Icons.Rounded.Info, isRequired = true) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "맨홀 타입",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4E5968)
                    )

                    // 기획서 디자인을 맞춘 입력 필드 (InputField 대신 사용할 경우)
                    OutlinedTextField(
                        value = manholeName, // state 연동 필요
                        onValueChange = { manholeName = it },
                        placeholder = { Text("예: OO번 측량", color = Color(0xFF8B95A1)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF2F4F6),
                            unfocusedContainerColor = Color(0xFFF2F4F6),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }
            }


            FormSection(title = "맨홀", icon = Icons.Rounded.Info) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 재질 선택
                    Column {
                        Text(
                            "맨홀 뚜껑의 재질",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4E5968)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // 상단에 정의된 expanded, selectedMaterial 활용
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedMaterial,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(), // Material3 필수 요소
                                shape = RoundedCornerShape(14.dp),
                                // 선택된 값에 따라 색상 분기
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = if (selectedMaterial == "선택하세요") Color(
                                        0xFF3182F6
                                    ) else Color(0xFF191F28),
                                    unfocusedTextColor = if (selectedMaterial == "선택하세요") Color(
                                        0xFF3182F6
                                    ) else Color(0xFF191F28),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Color(0xFF3182F6),
                                    unfocusedBorderColor = if (selectedMaterial == "선택하세요") Color(
                                        0xFF3182F6
                                    ) else Color(0xFFE5E8EB)
                                ),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                            )

                            // 기획안 스타일을 반영한 어두운 배경의 드롭다운
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier
                                    .background(
                                        Color(0xFF4E5968),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .fillMaxWidth()
                            ) {
                                materials.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                option,
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = if (option == selectedMaterial) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            selectedMaterial = option
                                            isCustomInput = (option == "직접 입력")
                                            expanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }

                        // AnimatedVisibility 추가 (직접 입력 시에만 나타남)
                        AnimatedVisibility(
                            visible = isCustomInput,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Text(
                                    "재질 직접 입력",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF8B95A1)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = customMaterial,
                                    onValueChange = { customMaterial = it },
                                    placeholder = {
                                        Text(
                                            "직접 입력할 재질을 기재하세요",
                                            color = Color(0xFFB0B8C1)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF2F4F6),
                                        unfocusedContainerColor = Color(0xFFF2F4F6),
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }

                    // 규격 및 측정
                    Column {
                        Text(
                            "맨홀 뚜껑의 규격",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4E5968)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = lidSize,
                                onValueChange = {},
                                placeholder = { Text("예: 6m", color = Color(0xFF8B95A1)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF2F4F6),
                                    unfocusedContainerColor = Color(0xFFF2F4F6),
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                singleLine = true
                            )
                            MeasureButton(
                                onClick = {
                                    activeTarget = ActiveTarget.LID_SIZE
                                    isMeasuring = !isMeasuring
                                    if (isMeasuring) onMeasureClick()
                                },
                                isMeasuring = (isMeasuring && activeTarget == ActiveTarget.LID_SIZE)
                            )
                        }
                    }

                    // 메모
                    Column {
                        Text(
                            "이상상태 유무 (메모)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4E5968)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = memo, onValueChange = { memo = it },
                            placeholder = { Text("이상 상태를 메모하세요", color = Color(0xFF8B95A1)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF2F4F6),
                                unfocusedContainerColor = Color(0xFFF2F4F6),
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            // 3. 토피 섹션 (추가)
            FormSection(title = "토피", icon = Icons.Rounded.Info, isRequired = true) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "토피 (수기 입력) *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4E5968)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = topiValue,
                            onValueChange = { topiValue = it },
                            placeholder = { Text("예: 1.5m", color = Color(0xFF8B95A1)) },
                            // weight(1f)를 주되, 버튼이 밀리지 않도록 Row 범위 내에서 처리
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF2F4F6),
                                unfocusedContainerColor = Color(0xFFF2F4F6),
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                        )

                        MeasureButton(
                            onClick = {
                                activeTarget = ActiveTarget.TOPI;
                                isMeasuring = !isMeasuring
                                if (isMeasuring) onMeasureClick()
                            },
                            isMeasuring = (isMeasuring && activeTarget == ActiveTarget.TOPI)
                        )
                    }
                }
            }


            // 4. 변실 모양 섹션 (추가)
            FormSection(title = "변실 모양", icon = Icons.Rounded.Info) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShapeButton("사각형", isSelected = chamberShape == "사각형") {
                            chamberShape = "사각형"
                            chamberDiameter = "" // 원형 값 초기화
                        }
                        ShapeButton("원형", isSelected = chamberShape == "원형") {
                            chamberShape = "원형"

                            //// 사각형 값 초기화
                            chamberWidth = "";
                            chamberHeight = ""
                        }
                    }

                    when (chamberShape) {
                        "사각형" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // 가로
                                Text(
                                    "가로 길이",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4E5968)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = chamberWidth,
                                        onValueChange = { chamberWidth = it },
                                        placeholder = { Text("가로 (m)", color = Color(0xFF8B95A1)) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFFB0B8C1),
                                            unfocusedContainerColor = Color(0xFFF2F4F6),
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        )
                                    )
                                    MeasureButton(
                                        onClick = {
                                            activeTarget = ActiveTarget.CHAMBER_WIDTH; isMeasuring =
                                            !isMeasuring
                                        },
                                        isMeasuring = (isMeasuring && activeTarget == ActiveTarget.CHAMBER_WIDTH)
                                    )
                                }

                                // 세로
                                Text(
                                    "세로 길이",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4E5968)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = chamberHeight,
                                        onValueChange = { chamberHeight = it },
                                        placeholder = { Text("세로 (m)", color = Color(0xFF8B95A1)) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFFF2F4F6),
                                            unfocusedContainerColor = Color(0xFFF2F4F6),
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        )
                                    )
                                    MeasureButton(
                                        onClick = {
                                            activeTarget =
                                                ActiveTarget.CHAMBER_HEIGHT; isMeasuring =
                                            !isMeasuring
                                        },
                                        isMeasuring = (isMeasuring && activeTarget == ActiveTarget.CHAMBER_HEIGHT)
                                    )
                                }
                            }
                        }

                        "원형" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "지름 규격",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4E5968)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = chamberDiameter,
                                        onValueChange = { chamberDiameter = it },
                                        placeholder = { Text("지름 (m)", color = Color(0xFF8B95A1)) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFFF2F4F6),
                                            unfocusedContainerColor = Color(0xFFF2F4F6),
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        ),
                                    )
                                    MeasureButton(
                                        onClick = {
                                            activeTarget = ActiveTarget.CHAMBER_SIZE; isMeasuring =
                                            !isMeasuring
                                        },
                                        isMeasuring = (isMeasuring && activeTarget == ActiveTarget.CHAMBER_SIZE)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            FormSection(title = "사다리 인버트 유무", icon = Icons.Rounded.Info, isRequired = true) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 사다리 선택
                    SelectionRow(
                        label = "사다리",
                        isSelected = hasLadder,
                        onSelect = { hasLadder = it }
                    )
                    // 인버트 선택
                    SelectionRow(
                        label = "인버트",
                        isSelected = hasInvert,
                        onSelect = { hasInvert = it }
                    )
                }
            }

            FormSection(
                title = "관경",
                icon = Icons.Rounded.Info,
                hasAddButton = true,
                onAddClick = {
                    val newId = (pipeList.maxOfOrNull { it.id } ?: 0) + 1
                    pipeList.add(PipeUiItem(id = newId))
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    pipeList.forEachIndexed { index, pipe ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), // 미세한 그림자로 떠 있는 느낌
                            border = BorderStroke(1.dp, Color(0xFFD7E0E8).copy(alpha = 0.5f))
                        ) {
                            PipeItemForm(
                                index = index,
                                pipe = pipe,
                                onUpdate = { updatedPipe -> pipeList[index] = updatedPipe },
                                onRemove = { pipeList.removeAt(index) },
                                showRemove = pipeList.size > 1,
                                onMeasure = { type ->
                                    if (type == "diameter") {
                                        Log.d("SurveyDebug_01", "타입: $type")
                                        activeTarget = ActiveTarget.PIPE_SIZE
                                    } else {
                                        Log.d("SurveyDebug_02", "타입: $type")
                                        activeTarget = ActiveTarget.PIPE_HEIGHT_SIZE
                                    }
                                    activePipeTarget = Pair(index, type)
                                    isMeasuring = !isMeasuring
                                    if (isMeasuring) onMeasureClick()
                                },
                                // 측정 상태를 각 필드에 맞게 명확히 전달
                                isMeasuringDiameter = (isMeasuring && activeTarget == ActiveTarget.PIPE_SIZE && activePipeTarget == Pair(
                                    index,
                                    "diameter"
                                )),
                                isMeasuringHeight = (isMeasuring && activeTarget == ActiveTarget.PIPE_HEIGHT_SIZE && activePipeTarget == Pair(
                                    index,
                                    "height"
                                )),
                                onDirectionClick = {
                                    activePipeIndexForDialog = index
                                    showDirectionDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
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
fun RowScope.ShapeButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .height(56.dp)
            .weight(1f),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) Color(0xFFE8F3FF) else Color(0xFFF2F4F6)
        ),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF3182F6) else Color.Transparent)
    ) {
        Text(
            text,
            color = if (isSelected) Color(0xFF3182F6) else Color.Black,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MeasureButton(onClick: () -> Unit, isMeasuring: Boolean) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .height(56.dp)
            .width(105.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isMeasuring) Color(0xFFF33131) else Color(0xFF3182F6)
        )
    ) {
        Icon(Icons.Rounded.Construction, null, Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
//        Text("측정", fontWeight = FontWeight.Bold)
        Text(if (isMeasuring) "중지" else "측정", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SelectionRow(label: String, isSelected: Boolean, onSelect: (Boolean) -> Unit) {
    Column {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4E5968))
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 유 버튼
            SelectButton(text = "유", isSelected = isSelected, onClick = { onSelect(true) })
            // 무 버튼
            SelectButton(text = "무", isSelected = !isSelected, onClick = { onSelect(false) })
        }
    }
}

@Composable
fun RowScope.SelectButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFFE8F3FF) else Color(0xFFF2F4F6),
            contentColor = if (isSelected) Color(0xFF3182F6) else Color(0xFF4E5968)
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

// 관경 개별 폼 컴포넌트
@Composable
fun PipeItemForm(
    index: Int,
    pipe: PipeUiItem,
    onUpdate: (PipeUiItem) -> Unit,
    onRemove: () -> Unit,
    showRemove: Boolean,
    onMeasure: (String) -> Unit,
    isMeasuringDiameter: Boolean,
    isMeasuringHeight: Boolean,
    onDirectionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        // 타이틀 및 삭제 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "관 ${index + 1}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Color(0xFF191F28)
            )
//            if (showRemove) {
//                TextButton(onClick = onRemove) {
//                    Text("삭제", color = Color(0xFFF33131), fontSize = 14.sp)
//                }
//            }

            if (showRemove) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .background(Color(0xFFFFE8E8), CircleShape)
                        .size(28.dp)
                ) {
                    Icon(
                        Icons.Rounded.Remove, // 마이너스 아이콘 사용
                        contentDescription = "삭제",
                        tint = Color(0xFFF33131), // 강렬한 빨간색
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 방향 입력
//        PipeInputField("관 방향", "선택 버튼을 눌러주세요", pipe.direction) { onUpdate(pipe.copy(direction = it)) }
        // 관 방향
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                "관 방향",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8B95A1)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 텍스트 필드 (읽기 전용 혹은 표시용)
                OutlinedTextField(
                    value = pipe.direction,
                    onValueChange = { onUpdate(pipe.copy(direction = it)) },
                    placeholder = { Text("선택 버튼을 눌러주세요", color = Color(0xFFB0B8C1)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF2F4F6),
                        unfocusedContainerColor = Color(0xFFF2F4F6),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )

                Button(
                    onClick = onDirectionClick,
                    modifier = Modifier
                        .height(56.dp)
                        .width(105.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE8F3FF), // 옅은 파란색 배경
                        contentColor = Color(0xFF3182F6)    // 강한 파란색 글자
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(Icons.Rounded.Explore, null, Modifier.size(18.dp)) // 나침반 아이콘 사용
                    Spacer(Modifier.width(4.dp))
                    Text("선택", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 관경 입력
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top // 1. 상단 라벨 기준으로 시작
        ) {
            PipeInputField(
                "관경 (m)",
                "예: 2.0 m",
                pipe.diameter,
                Modifier.weight(1f)
            ) { onUpdate(pipe.copy(diameter = it)) }

            // 2. 버튼이 입력창의 TextField 부분과 정렬되도록 top 패딩(라벨+간격)을 강제로 줌
            Box(modifier = Modifier.padding(top = 34.dp)) {
                MeasureButton(
                    onClick = { onMeasure("diameter") },
                    isMeasuring = isMeasuringDiameter
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            PipeInputField(
                "높이 (m)",
                "예: 1.5 m",
                pipe.height,
                Modifier.weight(1f)
            ) { onUpdate(pipe.copy(height = it)) }

            Box(modifier = Modifier.padding(top = 34.dp)) {
                MeasureButton(onClick = { onMeasure("height") }, isMeasuring = isMeasuringHeight)
            }
        }
        // 재질 입력
        PipeInputField("재질", "예: PVC", pipe.material) { onUpdate(pipe.copy(material = it)) }
    }
}

@Composable
fun PipeInputField(
    label: String,
    placeholder: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: ((String) -> Unit)? = null
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8B95A1))
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange?.invoke(it) },
            placeholder = { Text(placeholder, color = Color(0xFFB0B8C1)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF2F4F6),
                unfocusedContainerColor = Color(0xFFF2F4F6),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SaveButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp), // 부드러운 둥근 모서리
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF3182F6),
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        // [수정] 아이콘과 텍스트로 직관성 부여
        Icon(
            imageVector = Icons.Rounded.Save, // (참고: Icons.Rounded.Save 사용)
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "측정 데이터 저장",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1080px, height=2340px, dpi=440")
@Composable
fun PreviewSurveyDiameterScreen() {
    MaterialTheme {
        // 프리뷰를 위한 가짜(Mock) DAO 생성
        val dummyDao = object : MeasurementDao {
            override suspend fun insertMeasurement(measurement: MeasurementEntity) {}
            override fun getMesurementByProject(projectId: Long) =
                kotlinx.coroutines.flow.flowOf(emptyList<MeasurementEntity>())

            override suspend fun updateMeasurement(measurement: MeasurementEntity) {}
            override suspend fun deleteMeasurement(measurement: MeasurementEntity) {}
        }

        SurveyDiameterScreen(
            currentProjectId = 1L,
            isDistoConnected = true,
            distoMeasuredDistance = "1.234",
            measurementDao = dummyDao,
            onMeasureClick = {},
            onBackClick = {},
            onHistoryClick = {}
        )
    }
}