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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
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
    onSurveyClick: () -> Unit,
    onProjectListClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF2F4F6)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp)
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
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MainActionButton(
                    title = "측정 시작",
                    subtitle = "현장 거리 측량 시작",
                    icon = Icons.Rounded.Add,
                    containerColor = if (isDistoConnected) Color(0xFF3182F6) else Color(0xFFADB5BD), // 비 연결시에도 버튼은 유지 하되 클릭시 연결 안내를 하거나 색상을 조절함
                    modifier = Modifier.weight(0.8f), // 측정 시작 강조
                    onClick = {
                        /**
                         * 1. 블루투스가 연결된 경우라면 측정 화면
                         * 2. 블루투스가 연결되지 않은 경우라면 블루투스 연결 화면 이동
                         */
                        if (isDistoConnected) onSurveyClick() else onConnectClick()
                    }
                )

                MainActionButton(
                    title = "데이터 보기",
                    subtitle = "기록된 측량 내역 확인",
                    icon = Icons.Rounded.BarChart,
                    containerColor = Color(0xFF4E5968),
                    modifier = Modifier.weight(0.8f),
                    // 데이터 보기도 프로젝트 리스트 화면으로 연결 (구조적 통일)
                    onClick = onProjectListClick
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
            onSurveyClick = {},
            onProjectListClick = {}
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
            onSurveyClick = {},
            onProjectListClick = {}
        )
    }
}