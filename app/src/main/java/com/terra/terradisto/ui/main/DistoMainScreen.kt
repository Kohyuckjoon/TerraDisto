package com.terra.terradisto.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.terra.terradisto.BottomActionArea
import com.terra.terradisto.HeaderSection
import com.terra.terradisto.MainActionButton
import com.terra.terradisto.ui.components.StatusBadge

@Composable
fun DistoMainScreen(
    isDistoConnected: Boolean,
    selectedProjectName: String?,
    onConnectClick: () -> Unit,
    onCreateProjectClick: () -> Unit,
    onQuickSurveyClick: () -> Unit,
    onSurveyClick: () -> Unit,
    onProjectListClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    // 스크롤 상태 정의
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF2F4F6)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState) // 글자가 찌그러지지 않고 자연스럽게 스크롤되도록 변경
                .padding(horizontal = 24.dp, vertical = 3.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            HeaderSection(
                selectedProjectName = selectedProjectName,
                onProjectListClick = onProjectListClick
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 상단 상태 영역
            Row(modifier = Modifier.fillMaxWidth()) {
                StatusBadge(
                    icon = if (isDistoConnected) Icons.Rounded.BluetoothConnected else Icons.Rounded.BluetoothDisabled,
                    text = if (isDistoConnected) "Disto 연결됨" else "Disto 연결안됨",
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onConnectClick() },
                    isActive = isDistoConnected,
                    activeColor = if (isDistoConnected) Color(0xFF3182F6) else Color(0xFFF04452)
                )

                Spacer(modifier = Modifier.width(12.dp))

                StatusBadge(
                    icon = Icons.Rounded.Lock,
                    text = "서비스 준비중",
                    modifier = Modifier.weight(1f),
                    isActive = false,
                    activeColor = Color(0xFF3182F6)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MainActionButton(
                    title = "간편 측정",
                    subtitle = "항목 없이 바로 거리만 측정",
                    icon = Icons.Rounded.Construction,
                    containerColor = if (isDistoConnected) Color(0xFF00B67A) else Color(0xFFADB5BD),
                    modifier = Modifier.height(170.dp), //  글자 가독성이 가장 완벽한 높이 확보
                    onClick = {
                        /* 블루투스 연결된 상태면 간편 측정 화면, 미 연결 상태면 Disto 화면으로 이동 */
                        if (isDistoConnected) onQuickSurveyClick() else onConnectClick()
                    }
                )

                MainActionButton(
                    title = "정밀 측정",
                    subtitle = "모든 항목 상세 기록하기",
                    icon = Icons.Rounded.Add,
                    containerColor = if (isDistoConnected) Color(0xFF3182F6) else Color(0xFFADB5BD),
                    modifier = Modifier.height(170.dp), //  간편 측정과 대칭을 이루도록 고정 높이 적용
                    onClick = {
                        if (isDistoConnected) onSurveyClick() else onConnectClick()
                    }
                )

                MainActionButton(
                    title = "데이터 보기",
                    subtitle = "기록된 측량 내역 확인",
                    icon = Icons.Rounded.BarChart,
                    containerColor = Color(0xFF4E5968),
                    modifier = Modifier.height(170.dp), //  일체감을 주는 높이 매핑으로 시각적 안정감 극대화
                    onClick = onHistoryClick
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            // 하단 버튼 영역 연결
            BottomActionArea(
                onStartClick = onConnectClick,
                onCreateClick = onCreateProjectClick,
                onProjectListClick = onProjectListClick // [맵핑] 리스트 화면 이동 연결
            )
        }
    }
}


@Preview(name = "연결 완료 상태", showBackground = true)
@Composable
fun PreviewDistoMainScreenConnected() {
    MaterialTheme {
        DistoMainScreen(
            isDistoConnected = true,
            selectedProjectName = "강남구 대치동 맨홀",
            onConnectClick = {},
            onCreateProjectClick = {},
            onQuickSurveyClick = {},
            onSurveyClick = {},
            onProjectListClick = {},
            onHistoryClick = {}
        )
    }
}

@Preview(name = "연결 끊김 상태", showBackground = true)
@Composable
fun PreviewDistoMainScreenDisconnected() {
    MaterialTheme {
        DistoMainScreen(
            isDistoConnected = false,
            selectedProjectName = null,
            onConnectClick = {},
            onCreateProjectClick = {},
            onQuickSurveyClick = {},
            onSurveyClick = {},
            onProjectListClick = {},
            onHistoryClick = {}
        )
    }
}