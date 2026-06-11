package com.terra.terradisto.ui.main

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terra.terradisto.BottomActionArea
import com.terra.terradisto.HeaderSection
import com.terra.terradisto.MainActionButton
import com.terra.terradisto.ui.components.StatusBadge

@Composable
fun DistoMainScreen(
    isDistoConnected: Boolean,
    selectedProjectName: String?,
    hasServerLicense: Boolean = false,
    userEmail: String = "user@example.com",
    onConnectClick: () -> Unit,
    onCreateProjectClick: () -> Unit,
    onQuickSurveyClick: () -> Unit,
    onSurveyClick: () -> Unit,
    onProjectListClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onMyPageClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState() // 스크롤 상태 정의
    // 다이얼로그 상태 관리 변수
    var showDistoConnectDialog by remember { mutableStateOf(false) }
    var showProjectSelectDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFF2F4F6),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 13.dp)
            ) {
                BottomActionArea(
                    onStartClick = onConnectClick,
                    onCreateClick = onCreateProjectClick,
                    onProjectListClick = onProjectListClick
                )
            }
        }
    ){ paddingValues ->
        // 메인 컨텐츠 배치 (상단 고정 헤더 + 중앙 스크롤 리스트)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            // 상단 고정 영역 (스크롤 안 됨)
            Spacer(modifier = Modifier.height(20.dp))

            val licenseBadgeText = if (hasServerLicense) {
                "✓ 라이선스 등록 완료됨"
            } else {
                "⚠️ 라이선스 등록이 필요해요"
            }
            HeaderSection(
                selectedProjectName = selectedProjectName,
                userEmail = licenseBadgeText,
                onProjectListClick = onProjectListClick,
                onBadgeClick = onMyPageClick // 뱃지 클릭은 내부 컴포저블 안에서 완벽하게 처리됩니다.
            )

            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(17.dp)
            ) {
                item {
                    MainActionButton(
                        title = "간편 측정",
                        subtitle = "항목 없이 바로 거리만 측정",
                        icon = Icons.Rounded.Construction,
                        containerColor = if (isDistoConnected) Color(0xFF00B67A) else Color(0xFFADB5BD),
                        modifier = Modifier.height(170.dp),
                        onClick = {
                            /* 블루투스 연결된 상태면 간편 측정 화면, 미 연결 상태면 Disto 화면으로 이동 */
                            if (!isDistoConnected) {
                                showDistoConnectDialog = true // 기기 연결 팝업
                            } else {
                                onQuickSurveyClick() // 간편 측정 화면으로 연결
                            }
                        }
                    )
                }

                item {
                    MainActionButton(
                        title = "정밀 측정",
                        subtitle = "모든 항목 상세 기록하기",
                        icon = Icons.Rounded.Add,
                        containerColor = if (isDistoConnected && selectedProjectName != null) Color(0xFF3182F6) else Color(0xFFADB5BD),
                        modifier = Modifier.height(170.dp), //  간편 측정과 대칭을 이루도록 고정 높이 적용

                        onClick = {
                            if (!isDistoConnected) {
                                showDistoConnectDialog = true // 기기 연결 팝업
                            } else if (selectedProjectName == null) {
                                showProjectSelectDialog = true // 프로젝트 선택 팝업
                            } else {
                                onSurveyClick()
                            }
                        }
                    )
                }

                item {
                    MainActionButton(
                        title = "데이터 보기",
                        subtitle = "기록된 측량 내역 확인",
                        icon = Icons.Rounded.BarChart,
                        containerColor = Color(0xFF4E5968),
                        modifier = Modifier.height(170.dp),
                        onClick = onHistoryClick
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 하단 버튼 영역 연결
//            BottomActionArea(
//                onStartClick = onConnectClick,
//                onCreateClick = onCreateProjectClick,
//                onProjectListClick = onProjectListClick // 리스트 화면 이동 연결
//            )

            if (showDistoConnectDialog) {
                AlertDialog(
                    onDismissRequest = { showDistoConnectDialog = false },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = Color.White,
                    title = {
                        Text(
                            text = "Disto 연결이 필요해요",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191F28)
                        )
                    },
                    text = {
                        Text(
                            text = "정밀 측량을 시작하려면\nDisto 기기를 먼저 연결해야 해요.",
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
                            verticalArrangement = Arrangement.spacedBy(8.dp) // 버튼 사이 간격
                        ) {
                            // 1. 메인 버튼 (연결하러 가기)
                            Button(
                                onClick = {
                                    showDistoConnectDialog = false
                                    onConnectClick()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3182F6))
                            ) {
                                Text("연결하러 가기", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }

                            //  닫기 버튼
                            TextButton(
                                onClick = { showDistoConnectDialog = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
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

            // 프로젝트 선택 안내 다이얼로그
            if (showProjectSelectDialog) {
                AlertDialog(
                    onDismissRequest = { showProjectSelectDialog = false },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    containerColor = Color.White,
                    title = {
                        Text(
                            text = "프로젝트를 선택해 주세요",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191F28)
                        )
                    },
                    text = {
                        Text(
                            text = "정밀 측량을 시작하려면\n측정할 프로젝트를 먼저 선택해 주세요.",
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
                            // 1. 메인 버튼 (프로젝트 선택하기)
                            Button(
                                onClick = {
                                    showProjectSelectDialog = false
                                    onProjectListClick()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3182F6))
                            ) {
                                Text("프로젝트 선택하기", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }

                            // 2. 닫기 버튼 (보조 버튼)
                            TextButton(
                                onClick = { showProjectSelectDialog = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
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