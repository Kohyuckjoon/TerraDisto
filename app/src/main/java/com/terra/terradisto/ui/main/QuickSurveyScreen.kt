package com.terra.terradisto.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSurveyScreen(
    isDistoConnected: Boolean,
    distoMeasuredDistance: String,
    onMeasureClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    var currentDistance by remember { mutableStateOf("0.000") }
    val measurementLog = remember { mutableStateListOf<String>() }

    // 연속 측정 및 피크 감지를 위한 상태들
    // 연속 측정 및 피크 감지를 위한 상태들
    var isMeasuring by remember { mutableStateOf(false) }
    var maxDistance by remember { mutableDoubleStateOf(0.0) }
    var decreaseCount by remember { mutableIntStateOf(0) }
    var trendingUp by remember { mutableStateOf(false) }
    val EPS = 0.002 // 2mm 오차 허용 범위

    // 화면 진입 시 이전 측정값이 남아있지 않도록 제어하는 플래그
    var isFirstComposition by remember { mutableStateOf(true) }

    // 1. [반복 측정 명령] 1초 간격으로 하드웨어 측정 명령 전송
    LaunchedEffect(isMeasuring) {
        if (isMeasuring) {
            while (isMeasuring) {
                onMeasureClick()
                delay(1000) // 1초 대기
            }
        }
    }

    LaunchedEffect(distoMeasuredDistance) {
//        if (distoMeasuredDistance.isNotEmpty() && distoMeasuredDistance != "0.000") {
//            measurementLog.add(0, "$distoMeasuredDistance m")
//        }

        // 화면 진입 시 최초 값
        if (isFirstComposition) {
            isFirstComposition = false
            currentDistance = "0.000"
            return@LaunchedEffect
        }

        if (!isMeasuring) {
            // 측정 중이 아닐 때 값이 들어온 경우 (예: 수동 1회 측정 등)
            if (distoMeasuredDistance.isNotEmpty() && distoMeasuredDistance != "0.000") {
                currentDistance = distoMeasuredDistance
            }
            return@LaunchedEffect
        }

        val dist = distoMeasuredDistance.toDoubleOrNull() ?: return@LaunchedEffect
        if (dist <= 0.0) return@LaunchedEffect

        // 현재 값이 기존 최대값보다 큰 경우 (고점 갱신)
        if (dist > maxDistance + EPS) {
            maxDistance = dist
            decreaseCount = 0
            trendingUp = true
            currentDistance = distoMeasuredDistance // 화면에는 최대값을 표시
        }
        // 고점을 찍은 후 값이 확실히 줄어드는지 감시 (피크를 지났는지 판단)
        else if (trendingUp && dist < maxDistance - EPS) {
            decreaseCount++
            // 2회 연속 감소 시 측정이 끝난 것으로 판단하여 자동 정지
            if (decreaseCount >= 2) {
                isMeasuring = false
                measurementLog.add(0, String.format("%.3f m", maxDistance))
                currentDistance = String.format("%.3f", maxDistance) // 최종 고점 확정 표시
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF2F4F6),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "간편 측정",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191F28)
                        )
                        Text(
                            text = "빠르게 거리만 측정하기",
                            fontSize = 12.sp,
                            color = Color(0xFF8B95A1)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color(0xFF191F28)
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(
                                color = if (isDistoConnected) Color(0xFFE8F3FF) else Color(0xFFFEEBEE),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDistoConnected) Icons.Rounded.BluetoothConnected else Icons.Rounded.BluetoothDisabled,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = if (isDistoConnected) Color(0xFF3182F6) else Color(0xFFF04452)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isDistoConnected) "연결됨" else "연결안됨",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDistoConnected) Color(0xFF3182F6) else Color(0xFFF04452)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF2F4F6))
            )
        },
        bottomBar = {
            // 하단 컨트롤러 영역
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF2F4F6)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. 초기화 버튼
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.White, CircleShape)
                                .clickable {
                                    // //  모든 상태 초기화
                                    measurementLog.clear()
                                    currentDistance = "0.000"
                                    maxDistance = 0.0
                                    decreaseCount = 0
                                    trendingUp = false
                                    isMeasuring = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "초기화",
                                tint = Color(0xFF4E5968),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "초기화",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7684),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 2. 거리 측정 메인 버튼
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val buttonColor = if (isDistoConnected) Color(0xFF3182F6) else Color(0xFFADB5BD)
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(buttonColor, CircleShape)
                                .clickable(enabled = isDistoConnected) {
                                    // 기존에 사용하시던 하드웨어 전송 명령 함수(`startMeasurementFor(...)` 등)를 호출하도록 연동합니다.
//                                    onMeasureClick()
                                    if (!isMeasuring) {
                                        // 측정 시작 시 상태 초기화
                                        maxDistance = 0.0
                                        decreaseCount = 0
                                        trendingUp = false
                                        currentDistance = "0.000"
                                        isMeasuring = true
                                    } else {
                                        // 측정 수동 정지
                                        isMeasuring = false
                                    }
                                },

                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
//                                imageVector = Icons.Rounded.PlayArrow,
                                imageVector = if (isMeasuring) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = "측정",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
//                            text = "거리 측정",
                            text = if (isMeasuring) "측정 정지" else "거리 측정",
                            fontSize = 13.sp,
                            color = Color(0xFF191F28),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F3FF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LEICA DISTO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3182F6),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "버튼 한 번으로\n거리를 측정하세요",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF191F28),
                                lineHeight = 24.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "블루투스로 기기와 자동 연동",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7684)
                            )
                        }

                        Box(
                            modifier = Modifier.size(64.dp, 140.dp), // 하단 레이저 공간 확보를 위해 높이를 140.dp로 확장
                            contentAlignment = Alignment.TopCenter
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "LaserTransition")
                            val laserOffset by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 20f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "LaserOffset"
                            )

                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(135.dp) // 장비 아래쪽까지 길게 쭉 뻗음
                            ) {
                                // 장비의 정중앙 X좌표 계산
                                val centerX = size.width / 2f
                                // 장비 본체(100.dp)의 아래쪽 적당한 지점부터 시작
                                val startY = 85.dp.toPx()
                                val endY = size.height

                                // 빨간색 점선 레이저 광선
                                drawLine(
                                    color = Color(0xFFF04452), //  레드 컬러
                                    start = androidx.compose.ui.geometry.Offset(centerX, startY),
                                    end = androidx.compose.ui.geometry.Offset(centerX, endY),
                                    strokeWidth = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(
                                        intervals = floatArrayOf(8f, 6f),
                                        phase = laserOffset
                                    )
                                )
                                // 레이저 종착지 또는 시작점의 찌릿한 네온 포인트 점 효과
                                drawCircle(
                                    color = Color(0xFFF04452),
                                    radius = 3.5.dp.toPx(),
                                    center = androidx.compose.ui.geometry.Offset(centerX, endY - 2.dp.toPx())
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(64.dp, 100.dp)
                                    .background(Color(0xFF1C222B), RoundedCornerShape(12.dp))
                                    .padding(6.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // 1) 장비 액정 화면
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(32.dp)
                                            .background(Color(0xFF2C3542), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Column {
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(
                                                // 가짜 리터럴 대신 실제 들어온 기기 측정값(`distoMeasuredDistance`)을 바인딩해 통일감을 높입니다.
//                                                text = "$distoMeasuredDistance m",
                                                text = "$currentDistance m",
                                                color = Color(0xFF64B5F6),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.align(Alignment.End)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // 2) 메인 MEASURE 버튼
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(18.dp)
                                            .background(Color(0xFF3182F6), RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "MEASURE",
                                            color = Color.White,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Black,
                                            style = TextStyle(
                                                platformStyle = PlatformTextStyle(
                                                    includeFontPadding = false
                                                )
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 3) 하단 보조 버튼 그리드
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        repeat(3) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp, 6.dp)
                                                    .background(Color(0xFF4E5968), RoundedCornerShape(1.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF9E6), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFFFE0B2), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(Color(0xFFFFAD1F), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("i", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "간편 측정 모드에서는 데이터 저장이 되지 않습니다.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF9E6900)
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
//                            text = "실시간 측정 거리",
                            text = if (isMeasuring) "피크 감지 측정 중..." else "최종 측정 거리",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B95A1)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
//                                text = distoMeasuredDistance,
                                text = currentDistance,
                                fontSize = 54.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF191F28),
                                letterSpacing = (-1).sp
                            )
                            Text(
                                text = "m",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
//                                color = Color(0xFF8B95A1),
                                color = if (isMeasuring) Color(0xFF3182F6) else Color(0xFF8B95A1),
                                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "측정 내역 히스토리",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4E5968),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (measurementLog.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "측정된 기록이 없습니다.\n하단의 버튼을 눌러 측정을 시작하세요.",
                            fontSize = 13.sp,
                            color = Color(0xFFB0B8C1),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                itemsIndexed(measurementLog) { index, item ->
                    val currentTimestamp = remember {
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(0xFFF2F4F6), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${measurementLog.size - index}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4E5968)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = item,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF191F28)
                                    )
                                    Text(
                                        text = "로그 타임시각: $currentTimestamp",
                                        fontSize = 11.sp,
                                        color = Color(0xFF8B95A1)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "삭제",
                                tint = Color(0xFFB0B8C1),
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable { measurementLog.removeAt(index) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
fun PreviewQuickSurveyScreen() {
    MaterialTheme {
        // 프리뷰 렌더링에 필요한 더미 값들과 빈 콜백 블록을 명시적으로 매핑해 줍니다.
        QuickSurveyScreen(
            isDistoConnected = true,
            distoMeasuredDistance = "1.523", // 프리뷰용 기본 가상 데이터
            onMeasureClick = {},            // 프리뷰용 빈 액션 콜백
            onBackClick = {}
        )
    }
}