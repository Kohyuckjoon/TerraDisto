package com.terra.terradisto.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun InputCard(label: String, icon: ImageVector, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 0.5.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color(0xFF8B95A1), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4E5968)
                )
            }
            content()
        }
    }
}

@Composable
fun StatusBadge(
    icon: ImageVector,
    text: String,
    modifier: Modifier,
    isActive: Boolean,
    activeColor: Color,
    isLocked: Boolean = false // 자물쇠 스타일 여부 추가
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )
    val opacity by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "opacity"
    )

    Surface(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp)),
        color = Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = if (isActive) activeColor.copy(alpha = 0.1f) else Color(0xFFE5E8EB)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(20.dp)) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .scale(scale)
                            .background(activeColor.copy(alpha = opacity), CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isActive) activeColor else Color(0xFFD1D6DB), CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                icon,
                null,
                modifier = Modifier.size(20.dp),
                tint = if (isActive) activeColor else Color(0xFF8B95A1)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color(0xFF333D4B) else Color(0xFF8B95A1),
                letterSpacing = (-0.3).sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputField(value: String, onValueChange: (String) -> Unit, placeholder: String, singleLine: Boolean = true){
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color(0xFFB0B8C1)) },
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.textFieldColors(
            containerColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent, //  하단 선 제거로 깔끔함 극대화
            focusedIndicatorColor = Color(0xFF3182F6), //  포커스 시에만 파란 선 표시
        ),
        singleLine = singleLine,
        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color(0xFF191F28))
    )
}

@Preview(showBackground = true, name = "공통 컴포넌트 모음")
@Composable
fun PreviewCommonComponents() {
    val textState = remember { mutableStateOf("") }

    MaterialTheme{
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF2F4F6))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ){
            // 1. StatusBadge 테스트 (연결됨 / 연결안됨)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusBadge(
                    icon = Icons.Rounded.Bluetooth,
                    text = "Disto 연결됨",
                    modifier = Modifier.weight(1f),
                    isActive = true,
                    activeColor = Color(0xFF3182F6)
                )
                StatusBadge(
                    icon = Icons.Rounded.Bluetooth,
                    text = "연결 안 됨",
                    modifier = Modifier.weight(1f),
                    isActive = false,
                    activeColor = Color(0xFFF04452)
                )
            }

            // 2. InputCard + InputField 조합
            InputCard(
                label = "현장 위치",
                icon = Icons.Rounded.LocationOn
            ) {
                InputField(
                    value = textState.value,
                    onValueChange = { textState.value = it },
                    placeholder = "주소를 입력해주세요"
                )
            }
        }
    }
}