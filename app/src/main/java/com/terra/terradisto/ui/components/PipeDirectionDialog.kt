package com.terra.terradisto.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.terra.terradisto.R
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PipeDirectionDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var selectedDirection by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(32.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(vertical = 32.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("관 방향 선택", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF191F28))
                Spacer(Modifier.height(8.dp))
                Text("맨홀 유입/유출 방향을 터치하세요", fontSize = 15.sp, color = Color(0xFF8B95A1))

                Spacer(Modifier.height(40.dp))

                // 휠 영역
                Box(modifier = Modifier.size(340.dp), contentAlignment = Alignment.Center) {

                    // 1. 연한 원형 가이드라인 (두 번째 이미지 스타일)
                    Canvas(modifier = Modifier.size(240.dp)) {
                        drawCircle(
                            color = Color(0xFFF2F4F6),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // 2. 중앙 맨홀 이미지 (긴 상하 여백 무시하고 크게 확대)
                    Box(
                        modifier = Modifier
                            .size(160.dp) // 맨홀이 들어갈 원형 틀
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_manhole_cover),
                            contentDescription = "Manhole Cover",
                            contentScale = ContentScale.Crop, // 여백을 잘라내고 꽉 채움
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(1.5f) // 첫 번째 이미지의 상하 여백을 날리기 위해 확대
                        )
                    }

                    // 3. 8방향 버튼
                    val directions = listOf(
                        DirectionInfo("1", 270f),
                        DirectionInfo("2", 315f),
                        DirectionInfo("3", 0f),
                        DirectionInfo("4", 45f),
                        DirectionInfo("5", 90f),
                        DirectionInfo("6", 135f),
                        DirectionInfo("7", 180f),
                        DirectionInfo("8", 225f)
                    )

                    directions.forEach { info ->
                        DirectionButton(
                            info = info,
                            isSelected = selectedDirection == info.name,
                            onClick = { selectedDirection = info.name }
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))

                // 안내 칩
                Surface(
                    color = Color(0xFFF9FAFB),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.Info, null, tint = Color(0xFFADB5BD), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("선택한 방향이 유입/유출 방향으로 설정됩니다.", fontSize = 13.sp, color = Color(0xFF6B7684))
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { if(selectedDirection.isNotEmpty()) { onSelect(selectedDirection); onDismiss() } },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3182F6)),
                    enabled = selectedDirection.isNotEmpty()
                ) {
                    Text("확인", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                    Text("취소", color = Color(0xFF8B95A1), fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun BoxScope.DirectionButton(
    info: DirectionInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val radius = 128.dp
    val angleRad = Math.toRadians(info.angleDegrees.toDouble())
    val xOffset = (radius.value * cos(angleRad)).dp
    val yOffset = (radius.value * sin(angleRad)).dp

    val bgColor by animateColorAsState(if (isSelected) Color(0xFF3182F6) else Color.White)
    val iconColor by animateColorAsState(if (isSelected) Color.White else Color(0xFF3182F6))
    val borderColor by animateColorAsState(if (isSelected) Color(0xFF3182F6) else Color(0xFFE5E8EB))

    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(x = xOffset, y = yOffset),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .clickable { onClick() },
            color = bgColor,
            shape = CircleShape,
            border = BorderStroke(2.dp, borderColor),
            shadowElevation = if (isSelected) 8.dp else 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer(rotationZ = info.angleDegrees + 90f),
                    tint = iconColor
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            info.name,
            fontSize = 17.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF191F28) else Color(0xFF8B95A1)
        )
    }
}

data class DirectionInfo(val name: String, val angleDegrees: Float)