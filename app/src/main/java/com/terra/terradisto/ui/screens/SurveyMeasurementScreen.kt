package com.terra.terradisto.ui.screens

import android.R.attr.label
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terra.terradisto.ui.components.InputCard
import com.terra.terradisto.ui.components.InputField
import com.terra.terradisto.ui.distoconnect.DistoConnectViewModel
import com.terra.terradisto.ui.viewModel.DistoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyMeasurementScreen (viewModel: DistoViewModel) {
    Scaffold (
        containerColor = Color(0xFFF2F4F6),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("현장 측정", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ){ padding ->
        Column (
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ){
            InputCard(label = "맨홀명", icon = Icons.Rounded.Edit) {
                InputField(
                    value = viewModel.manholName.value,
                    onValueChange = { viewModel.manholName.value = it },
                    placeholder = "예: MH-001"
                )
            }

            // 측정 값 섹션 (가로 병렬 배치)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    InputCard(label = "심도 (m)", icon = Icons.Rounded.Height) {
                        InputField(
                            value = viewModel.depthValue.value,
                            onValueChange = { viewModel.depthValue.value = it },
                            placeholder = "0.000"
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    InputCard(label = "직경 (mm)", icon = Icons.Rounded.Architecture) {
                        InputField(
                            value = viewModel.diameterValue.value,
                            onValueChange = { viewModel.diameterValue.value = it },
                            placeholder = "600"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 저장 버튼
            Button(
                onClick = { viewModel.onSaveData() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3182F6))
            ) {
                Text("측정 데이터 저장하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}