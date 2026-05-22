package com.terra.terradisto.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terra.terradisto.data.MeasurementEntity

@Composable
fun MeasureHistoryItem(
    item: MeasurementEntity,
    onDeleteClick: (MeasurementEntity) -> Unit,
    onEditClick: (MeasurementEntity) -> Unit,
//    onMenuClick: () -> Unit
){
    var expanded by remember { mutableStateOf(false) }

    Card (
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ){
        Column (modifier = Modifier.padding(24.dp)){
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                Text(text = "MH-${item.id.toString().padStart(3, '0')}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)

                //  토스 스타일의 직관적인 수정/삭제 팝업 메뉴 컴포넌트 마운트
                Box {
                    IconButton (onClick = { expanded = true }){
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color(0xFF8B95A1))
                    }

                    //  둥글고 깔끔한 토스풍 드롭다운 패널 디자인 구현
                    MaterialTheme(
                        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
                    ) {
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .background(Color.White)
                                .border(1.dp, Color(0xFFF2F4F6), RoundedCornerShape(16.dp))
                                .padding(vertical = 4.dp),
                            scrollState = androidx.compose.foundation.rememberScrollState()
                        ) {
                            //  '수정' 메뉴 아이템 - 가독성을 극대화한 아이콘 + 텍스트 조합
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "수정",
                                            modifier = Modifier.size(18.dp),
                                            tint = Color(0xFF4E5968) // 토스 다크 그레이 컬러
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "수정",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF333D4B)
                                        )
                                    }
                                },
                                onClick = {
                                    expanded = false
                                    onEditClick(item) // 기존 바인딩된 파이어베이스/액션 함수 호출 유지
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = Color(0xFF333D4B)
                                )
                            )

                            //  '삭제' 메뉴 아이템 - 직관적인 경고 레드 컬러 포인트 적용
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "삭제",
                                            modifier = Modifier.size(18.dp),
                                            tint = Color(0xFFF04452) // 토스 경고 레드 컬러
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "삭제",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFF04452)
                                        )
                                    }
                                },
                                onClick = {
                                    expanded = false
                                    // TODO: 삭제 콜백이 분리되어 있다면 이곳에 기존 삭제 함수명을 명시하세요.
                                    // 현재 구조에서는 기존 기능 유지를 위해 onMenuClick() 스텁을 그대로 둡니다.
                                    onDeleteClick(item)
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = Color(0xFFF04452)
                                )
                            )
                        }
                    }
                }
            }

            Text(text = "2026-04-22 14:30", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF8B95A1)))

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoBlock("맨홀 타입", item.manholeType, Modifier.weight(1f))
                InfoBlock("심도", "${item.chamberSize} m", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)){
                InfoBlock("직경", "${item.lidSize} m", Modifier.weight(1f))
                if (item.selectedChamberShape == "사각형") {
                    InfoBlock("세로", "${item.lidMaterial} m", Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun InfoBlock(label: String, value: String, modifier: Modifier){
    Column (modifier = modifier
        .background(Color(0xFFF7F8FA), RoundedCornerShape(16.dp))
        .padding(16.dp))
    {
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF8B95A1), fontWeight = FontWeight.Medium))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp))
    }
}


@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun PreviewMeasureHistoryItem() {
    // 필수 파라미터를 모두 채워 넣어야 프리뷰가 정상 작동합니다.
    val sampleItem = MeasurementEntity(
        id = 1,
        projectId = 1,
        manholeType = "오수",
        lidMaterial = "0.850",
        lidSize = "0.900",
        topieValue = "3",
        chamberMaterial = "벽돌",
        chamberSize = "1.98",
        selectedChamberShape = "사각형", // 이 값을 "원형"으로 바꾸면 원형 UI로 프리뷰 가능
        hasLadder = true,
        hasInverter = true,
        anomalyMemo = "None",
        pipeList = emptyList()
    )

    // 토스 디자인 확인을 위해 배경색을 회색으로 설정
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .background(Color(0xFFF2F4F6))
            .padding(16.dp)
    ) {
        MeasureHistoryItem(
            item = sampleItem,
//            onMenuClick = {}
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}