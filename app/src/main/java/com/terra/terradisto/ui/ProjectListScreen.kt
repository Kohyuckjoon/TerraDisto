package com.terra.terradisto.ui

import android.R
import android.widget.Space
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.terra.terradisto.ui.viewModel.DistoViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.terra.terradisto.ui.components.StatusBadge
import com.terra.terradisto.viewmodel.ProjectViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.flowOf

@Composable
fun ProjectListScreen(
    onNavigateToSurvey: () -> Unit, // 새 프로젝트 화면 이동 (SurveyMeasurementScreen 이동용)
    onCreateClick: () -> Unit, // 프로젝트 생성
    onConnectClick: () -> Unit = {}, // 블루투스 연결 화면으로 이동
    projectViewModel: ProjectViewModel = viewModel<ProjectViewModel>(),
    distoViewModel: DistoViewModel = viewModel<DistoViewModel>() // 하드웨어 상태(Disto BLE)
) {
    // 데이터베이스 프로젝트 리스트 상태 관찰
    val projects by projectViewModel.allProjects.collectAsState(initial = emptyList())
    val selectedProject by projectViewModel.selectedProject.collectAsState()

    // 연동
    val isConnected by distoViewModel.isDistoConnected
//    val isConnected = false // Disto 연결 상태 등은 필요시 DistoViewModel에서 따로 가져오거나 통합

    var projectToDelete by remember { mutableStateOf<com.terra.terradisto.data.Project?>(null) }
    if (projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "프로젝트 삭제",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF191F28)
                )
            },

            text = {
                Text(
                    text = "'${projectToDelete?.projectName}' 현장의 모든 측정 데이터가 영구적으로 삭제됩니다. 정말 삭제하시겠습니까?",
                    fontSize = 15.sp,
                    color = Color(0xFF4E5968),
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        projectToDelete?.let { projectViewModel.deleteProject(it) }
                        projectToDelete = null // 팝업 닫기
                    }
                ) {
                    Text("삭제", color = Color(0xFFFF4D4F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("취소", color = Color(0xFF8B95A1), fontWeight = FontWeight.Medium)
                }
            }
        )
    }

//    val projects by viewModel.projects.collectAsState()
//    val isConnected by viewModel.isDistoConnected

    Scaffold(
        containerColor = Color(0xFFF2F4F6),
        bottomBar = {

            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ){
                // 프로젝트 생성
                Button(
                    onClick = onCreateClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,       // 맑은 날에도 완벽히 보장되는 흰색 배경
                        contentColor = Color(0xFF3182F6)    // 글자와 아이콘은 브랜드 컬러로 통일감 부여
                    ),
                    border = BorderStroke(1.5.dp, Color(0xFF3182F6)) //
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("프로젝트 생성 시작", fontSize = 16.sp , fontWeight = FontWeight.Bold)
                }

                // 측정 시작
                Button(
                    onClick = onNavigateToSurvey,
//                    onClick = onNavigateToCreate, // 클릭 시 이동 로직 연결
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3182F6))
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("새 현장 측정 시작", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

            }

        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Disto Survey",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF191F28)
                )
                Text(
                    "스마트 맨홀 측량 시스템",
                    fontSize = 15.sp,
                    color = Color(0xFF8B95A1),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                StatusBadge(
                    icon = if (isConnected) Icons.Rounded.BluetoothConnected else Icons.Rounded.BluetoothDisabled,
                    text = if (isConnected) "Disto 연결됨" else "Disto 연결안됨",
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onConnectClick() }, // 클릭 시 블루투스 연결 화면 유도
                    isActive = isConnected,
                    activeColor = if (isConnected) Color(0xFF3182F6) else Color(0xFFF04452)
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

            Spacer(modifier = Modifier.height(24.dp))

            // 프로젝트 리스트
            Text(
                "최근 프로젝트",
                modifier = Modifier.padding(horizontal = 24.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E5968)
            )

            val sortedProjects = remember (projects, selectedProject){
                projects.sortedByDescending { it.id == selectedProject?.id }
            }

            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sortedProjects, key = { it.id }) { project ->
                    ProjectItemCard(
                        project = project,
                        isSelected = project.id == selectedProject?.id,
                        onSelect = { projectViewModel.selectProject(project) },
                        onDelete = { projectToDelete = project }
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectItemCard(
    project: com.terra.terradisto.data.Project,
    isSelected: Boolean, // 선택 여부 추가
    onSelect: () -> Unit, // 선택 함수 추가
    onDelete: () -> Unit // 삭제 함수 추가
) {
    Surface (
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(22.dp),
        border = if (isSelected) BorderStroke(2.dp, Color(0xFF3182F6)) else null,
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ){
        Row (
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Column(modifier = Modifier.weight(1f)){
                Text(project.projectName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF191F28))
                Spacer(Modifier.height(8.dp))
                Text(project.location, fontSize = 14.sp, color = Color(0xFF8B95A1))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF4D4F))
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("삭제", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = onSelect,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFFF2F4F6) else Color(0xFF3182F6),
                        contentColor = if (isSelected) Color(0xFF8B95A1) else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(if (isSelected) "선택됨" else "선택", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

//            Column (horizontalAlignment = Alignment.End){
//                // 측정 개수는 추후 포인트 테이블과 Join해서 가져와야 하므로 일단 고정값 혹은 날짜 표시
//                Text(project.createdAt, fontSize = 12.sp, color = Color(0xFF3182F6))
//                Spacer(Modifier.height(12.dp))
//                Button(
//                    onClick = onSelect,
//                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
//
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = if (isSelected) Color(0xFFF2F4F6) else Color(0xFF3182F6),
//                        contentColor = if (isSelected) Color(0xFF8B95A1) else Color.White
//                    ),
//                    shape = RoundedCornerShape(12.dp),
//                    modifier = Modifier.height(36.dp)
//                ) {
//                    Text(if (isSelected) "선택됨" else "선택", fontSize = 13.sp, fontWeight = FontWeight.Bold)
//                }
//            }
        }
    }
}

//@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
//@Composable
//fun ProjectListScreenPreview() {
//    MaterialTheme {
//        ProjectListScreen(
//            onNavigateToSurvey = {},
//            onNavigateToCreate = {},
//            onCreateClick = {},
//            onConnectClick = {},
//        )
//    }
//}


@Preview(showBackground = true, device = "spec:width=1080px, height=2340px, dpi=440")
@Composable
fun ProjectListScreenPreview() {
    MaterialTheme {
        // 실제 앱의 배경색과 맞춤
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF2F4F6)
        ) {
            // Preview에서는 ViewModel의 로직이 동작하지 않으므로
            // 실제 프로젝트 리스트가 보이지 않을 수 있습니다.
            // (ViewModel 내부의 Flow가 초기값 emptyList를 반환하기 때문)
            ProjectListScreen(
                onNavigateToSurvey = {},
                onCreateClick = {},
                onConnectClick = {}
                // ViewModel은 기본 파라미터가 있으므로 생략 가능
            )
        }
    }
}