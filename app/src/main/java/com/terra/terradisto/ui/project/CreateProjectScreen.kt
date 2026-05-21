package com.terra.terradisto.ui.project

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.terra.terradisto.ui.components.InputCard
import com.terra.terradisto.ui.components.InputField
import com.terra.terradisto.viewmodel.ProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectScreen(
    onBack: () -> Unit,
    projectViewModel: ProjectViewModel = viewModel<ProjectViewModel>()
) {
    var projectName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var currentDate = remember { SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA).format(Date()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("새 프로젝트 생성", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF191F28))
                        Text("필수 정보를 입력하면 시작할 수 있어요", fontSize = 13.sp, color = Color(0xFF4E5968))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "뒤로가기", modifier = Modifier.size(22.dp), tint = Color(0xFF191F28))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp)
            ) {
                Button(
                    onClick = { /* 생성 로직 */
                        projectViewModel.insertProject(projectName, location, description, currentDate)
                        onBack() // 저장 후 이전 화면으로 이동
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp), //  버튼 높이를 키워 터치 편의성 증대
                    shape = RoundedCornerShape(18.dp), //  더 둥근 모서리(토스 스타일)
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3182F6),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE5E8EB), //  비활성 색상 명확화
                        disabledContentColor = Color(0xFFB0B8C1)
                    ),
                    enabled = projectName.isNotBlank() && location.isNotBlank()
                ) {
                    Text("프로젝트 생성하기", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.textButtonColors(containerColor = Color(0xFFF2F4F6))
                ) {
                    Text("나중에 할게요", color = Color(0xFF4E5968), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        containerColor = Color(0xFFF2F4F6)
    ) { paddingValues ->
        Column (
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp) //좌우 여백 확대
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ){
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFE8F3FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Description, null, tint = Color(0xFF3182F6), modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("현장 상세 정보", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF191F28))
                        Text("정확한 측량을 위해 입력해주세요", fontSize = 14.sp, color = Color(0xFF8B95A1))
                    }
                }
            }

            // 입력 카드 1 : 프로젝트명
            InputCard(label = "프로젝트 이름 *", icon = Icons.Rounded.Edit) {
                InputField(value = projectName, onValueChange = { projectName = it }, placeholder = "예: OO동 맨홀 측정")
            }

            InputCard(label = "측량 위치 *", icon = Icons.Rounded.LocationOn) {
                InputField(value = location, onValueChange = { location = it }, placeholder = "예: 서울시 송파구 문정동..")
            }

            InputCard(label = "메모 (선택)", icon = Icons.Rounded.ChatBubbleOutline) {
                InputField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "특이사항을 적어주세요",
                    singleLine = false
                )
            }

            // 날짜 표시 (읽기 전용)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF9FAFB), //  입력칸과 차별화된 연한 배경
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E8EB))
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CalendarMonth, null, tint = Color(0xFF8B95A1), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("오늘 날짜", fontSize = 12.sp, color = Color(0xFF8B95A1), fontWeight = FontWeight.Medium)
                        Text(currentDate, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4E5968))
                    }
                }
            }

            Text(
                "• 필수 항목(*)을 입력해야 프로젝트 생성이 가능합니다.",
                fontSize = 13.sp,
                color = Color(0xFF8B95A1),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
fun CreateProjectScreenPreview() {
    // 테마가 있다면 테마로 감싸주시고, 없으면 Surface로 감싸서 확인합니다.
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF2F4F6)) {
        CreateProjectScreen(onBack = { /* 프리뷰이므로 동작 정의 없음 */ })
    }
}