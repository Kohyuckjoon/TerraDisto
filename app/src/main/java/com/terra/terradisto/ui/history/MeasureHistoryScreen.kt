package com.terra.terradisto.ui.history

import android.R.attr.onClick
import android.widget.Toast
import androidx.activity.result.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.terra.terradisto.data.MeasurementEntity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.terra.terradisto.data.AppDatabase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasureHistoryScreen(
    items: List<MeasurementEntity>,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("측정 내역", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { // 콜백 연결
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFFF2F4F6)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(top = 20.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { item ->
                MeasureHistoryItem(
                    item = item,
                    onDeleteClick = { targetItem ->
                        scope.launch {
                            db.measurementDao().deleteMeasurement(targetItem)
                            Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEditClick = { /* 수정 로직은 다음에 */ }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHistory() {
    val dummy = listOf(
        MeasurementEntity(
            id = 1,
            projectId = 1, // 필수 추가
            manholeType = "오수",
            lidMaterial = "주철", // 정의된 필드에 맞게 채움
            lidSize = "0.900",
            topieValue = "3", // 필수 추가
            chamberMaterial = "벽돌", // 필수 추가
            chamberSize = "1.98",
            selectedChamberShape = "사각형",
            hasLadder = true,
            hasInverter = true,
            anomalyMemo = "None",
            pipeList = emptyList() // 필수 추가
        ),
        MeasurementEntity(
            id = 2,
            projectId = 1,
            manholeType = "우수",
            lidMaterial = "주철",
            lidSize = "0.800",
            topieValue = "55",
            chamberMaterial = "콘크리트",
            chamberSize = "1.65",
            selectedChamberShape = "원형",
            hasLadder = false,
            hasInverter = false,
            anomalyMemo = "None",
            pipeList = emptyList()
        )
    )
}