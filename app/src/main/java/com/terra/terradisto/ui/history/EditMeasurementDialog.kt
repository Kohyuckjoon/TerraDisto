package com.terra.terradisto.ui.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.terra.terradisto.data.MeasurementEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMeasurementDialog(
    item: MeasurementEntity,
    onDismiss: () -> Unit,
    onConfirm: (MeasurementEntity) -> Unit
) {
    // 파이어베이스 연동 변수
    var manholeType by remember { mutableStateOf(item.manholeType) }
    var depthValue by remember { mutableStateOf(item.topieValue) } // 심도

    val dims = item.chamberSize.split(" x ")
    var widthValue by remember { mutableStateOf(dims.getOrNull(0) ?: item.chamberSize) }
    var heightValue by remember { mutableStateOf(dims.getOrNull(1) ?: "") }

    val formattedDate = remember(item.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.timestamp))
    }


//    var chamberSize by remember { mutableStateOf(item.chamberSize) }
//    var lidSize by remember { mutableStateOf(item.lidSize) }
//    var lidMaterial by remember { mutableStateOf(item.lidMaterial) }
//    val formattedDate = remember(item.timestamp) {
//        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.timestamp))
//    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),  // [수정] 토스 스타일의 컴팩트한 다이얼로그 너비 확보를 위한 패딩 조정
        shape = RoundedCornerShape(24.dp),  // [수정] 머티리얼3 & 토스 스타일의 부드럽고 큰 라운딩 적용
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            // --- 1. 상단 헤더 영역 ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                     // [수정] 이미지와 동일한 블루 원형 번호 배지 구현
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFE8F3FF), shape = RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%03d", item.id), // ID를 001, 002 형식으로 포맷팅
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3182F6)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                         // [수정] 텍스트 가독성을 위한 토스풍 타이포그래피 스타일링
                        Text(
                            text = "${item.id}번 측정 데이터 수정",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191F28)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formattedDate,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF8B95A1)
                        )
                    }
                }

                 // [수정] 이미지 상의 X 닫기 버튼 추가 (직관적인 UX 제공)
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = Color(0xFFB0B8C1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. 입력 폼 영역 (2열 배치 구조) ---
             // [수정] 입력 폼 전체를 감싸는 미니멀한 라이트 그레이 배경 카드 박스
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F4F6), shape = RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 공통 텍스트 필드 스타일 정의 (가독성 및 머티리얼 컴포넌트 기준 충족)
                val textFieldColors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
                val textFieldShape = RoundedCornerShape(12.dp)

                // 1열: 맨홀 타입 / 심도
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 맨홀 타입 (기존 데이터 완벽 보존)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("맨홀 타입", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF8B95A1))
                        Spacer(modifier = Modifier.height(6.dp))
                        TextField(
                            value = manholeType,
                            onValueChange = { manholeType = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = textFieldShape,
                            colors = textFieldColors,
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        )
                    }

                    // 심도 (기존 변수명 chamberSize 유지 + 이미지와 동일하게 접미사 m 추가)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("심도", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF8B95A1))
                        Spacer(modifier = Modifier.height(6.dp))
                        TextField(
                            value = depthValue,
                            onValueChange = { depthValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = textFieldShape,
                            colors = textFieldColors,
                            singleLine = true,
                            suffix = { Text("m", color = Color(0xFFB0B8C1), fontSize = 14.sp) },  // [수정] 우측에 단위(m) 표시로 직관성 향상
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                // 2열: 직경 / 세로 (사각형일 때만 세로 노출)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 직경 (기존 변수명 lidSize 유지)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("직경", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF8B95A1))
                        Spacer(modifier = Modifier.height(6.dp))
                        TextField(
                            value = widthValue,
                            onValueChange = { widthValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = textFieldShape,
                            colors = textFieldColors,
                            singleLine = true,
                            suffix = { Text("m", color = Color(0xFFB0B8C1), fontSize = 14.sp) },  // [수정] 우측에 단위(m) 표시
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        )
                    }

                    // 세로 (기존 변수명 lidMaterial 유지 및 사각형일 때만 필드 노출 조건문 보존)
                    Column(modifier = Modifier.weight(1f)) {
                        if (item.selectedChamberShape == "사각형") {
                            Text("세로", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF8B95A1))
                            Spacer(modifier = Modifier.height(6.dp))
                            TextField(
                                value = heightValue,
                                onValueChange = { heightValue = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = textFieldShape,
                                colors = textFieldColors,
                                singleLine = true,
                                suffix = { Text("m", color = Color(0xFFB0B8C1), fontSize = 14.sp) },  // [수정] 우측에 단위(m) 표시
                            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            )
                        } else {
                            // 사각형이 아닐 경우 2열 그리드 밸런스를 맞추기 위한 빈 공간 유지
                            Spacer(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- 3. 하단 액션 버튼 영역 ---
             // [수정] 이미지와 완벽히 일치하는 둥글고 시인성이 높은 고대비 버튼 레이아웃 구조로 전면 수정
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 취소 버튼
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF4E5968)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E8EB))  // [수정] 토스풍의 연한 테두리선 추가
                ) {
                Text(
                    text = "취소",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

                // 수정 완료 버튼 (파이어베이스 데이터 전송 로직 완벽 보존)
                Button(
                    onClick = {
                        val updatedChamberSize = if (item.selectedChamberShape == "사각형") {
                            "$widthValue x $heightValue"
                        } else {
                            widthValue
                        }
                        onConfirm(
                            item.copy(
                                manholeType = manholeType,
                                topieValue = depthValue, // 심도 저장
                                chamberSize = updatedChamberSize // 직경/가로세로 저장
                            )
                        )
                    },
                    modifier = Modifier
                        .weight(1.3f) // 수정 완료 버튼을 조금 더 강조하는 비율 배정
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3182F6), // 토스 시그니처 블루
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "수정 완료",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    }
}