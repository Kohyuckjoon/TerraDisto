package com.terra.terradisto.ui

import android.R
import android.widget.Space
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
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

    // 정렬 상태 관리를 위한 변수 선언 (true: 생성일자순, false: 가나다순)
    var sortByCreated by remember { mutableStateOf(true) }

    // 리스트 스크롤 위치를 제어하기 위한 컴포즈 상태 생성
    val listState = rememberLazyListState()

    // 정렬 버튼(sortByCreated)이 클릭시 리스트 상단 위치
    LaunchedEffect(sortByCreated) {
        if (projects.isNotEmpty()) {
            listState.scrollToItem(0) // 부드러운 스크롤을 원하시면 animateScrollToItem(0) 사용 가능
        }
    }

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 프로젝트 리스트
                Text(
                    "최근 프로젝트",
                    modifier = Modifier.padding(horizontal = 24.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4E5968)
                )

                // 애니메이션
                SortSegmentControl(
                    sortByCreated = sortByCreated,
                    onToggle = { sortByCreated = it }
                )
            }

            // 선택된 프로젝트 상단 고정 조건식 유지 + 선택한 정렬 탭 기준 정렬 분기 추가
            val sortedProjects = remember (projects, selectedProject, sortByCreated){
                val baseList = if (sortByCreated) {
                    // 생성일자순: ID 큰 순서 정렬 (최신순)
                    projects.sortedByDescending { it.id }
                } else {
                    // 가나다순: 프로젝트 이름 가나다 오름차순 정렬
                    projects.sortedBy { it.projectName }
                }
                // 정렬 완료 후, 현재 선택된 활성 프로젝트를 무조건 최상단(Top)으로 배치
                baseList.sortedByDescending { it.id == selectedProject?.id }
            }

//            val sortedProjects = remember (projects, selectedProject){
//                projects.sortedByDescending { it.id == selectedProject?.id }
//            }

            LazyColumn(
                state = listState, // 리스트 위치 상태 값
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

// 기획서 디자인 구현을 위한 전용 슬라이딩 컴포넌트 선언
@Composable
fun SortSegmentControl(
    sortByCreated: Boolean,
    onToggle: (Boolean) -> Unit
) {
    // 스르륵- 움직이는 하이라이트 인디케이터 패딩 위치 계산 애니메이션
    val indicatorOffset by animateDpAsState(
        targetValue = if (sortByCreated) 0.dp else 64.dp,
        animationSpec = spring(stiffness = 380f)
    )

    Box(
        modifier = Modifier
            .width(136.dp)
            .height(34.dp)
            .background(Color(0xFFE5E8EB), CircleShape)
            .padding(4.dp)
    ) {
        // 배경 위에서 스르륵 미끄러지는 흰색 활성 상태 표시 바 블록
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset) // 좌우 무빙 제어
                .width(64.dp)
                .fillMaxHeight()
                .background(Color.White, CircleShape)
        )

        // 상단 텍스트 레이어 영역
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 생성일자 선택 영역
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { onToggle(true) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "↑↓ 생성일자",
                    fontSize = 11.sp,
                    fontWeight = if (sortByCreated) FontWeight.Bold else FontWeight.Medium,
                    color = if (sortByCreated) Color(0xFF333D4B) else Color(0xFF6B7684)
                )
            }

            // 가나다 선택 영역
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { onToggle(false) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "가나다",
                    fontSize = 11.sp,
                    fontWeight = if (!sortByCreated) FontWeight.Bold else FontWeight.Medium,
                    color = if (!sortByCreated) Color(0xFF333D4B) else Color(0xFF6B7684)
                )
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
