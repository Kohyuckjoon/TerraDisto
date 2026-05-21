package com.terra.terradisto.ui.distoconnect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.leica.sdk.Devices.Device

// ✅ 자바 Fragment에서 호출할 브릿지 함수
fun setDistoContent(
    composeView: ComposeView,
    viewModel: DistoConnectViewModel,
    onDeviceClick: (Device) -> Unit
) {
    composeView.setContent {
        MaterialTheme {
            DistoConnectScreen(viewModel, onDeviceClick)
        }
    }
}

@Composable
fun DistoConnectScreen(
    viewModel: DistoConnectViewModel,
    onDeviceClick: (Device) -> Unit
) {
    val devices = viewModel.availableDevices

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F6))
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(72.dp))

        Text(
            text = "검색된 장치",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF191F28),
            modifier = Modifier.padding(vertical = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(devices) { device ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDeviceClick(device) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = Color(0xFF3182F6),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = device.deviceName ?: "Unknown Device",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF191F28)
                            )
                            Text(
                                text = device.deviceID,
                                fontSize = 13.sp,
                                color = Color(0xFF8B95A1)
                            )
                        }
                    }
                }
            }
        }
    }
}