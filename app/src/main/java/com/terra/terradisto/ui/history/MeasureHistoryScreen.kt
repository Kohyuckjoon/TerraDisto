package com.terra.terradisto.ui.history

import android.R.attr.onClick
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.launch
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.room.util.getColumnIndex
import com.terra.terradisto.data.AppDatabase
import com.terra.terradisto.ui.history.components.EditMeasurementDialog
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import kotlin.concurrent.write

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasureHistoryScreen(
    items: List<MeasurementEntity>,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    // 팝업의 표시 여부와 어떤 아이템을 수정할지 선택된 데이터를 저장하는 상태 선언
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedItemForEdit by remember { mutableStateOf<MeasurementEntity?>(null) }

    var showDetailDialog by remember { mutableStateOf(false) }
    var selectedItemForDetail by remember { mutableStateOf<MeasurementEntity?>(null) }

    var showExcelSuccessDialog by remember { mutableStateOf(false) } // 엑셀 내보내기 성공 팝업 제어 상태 변수

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("측정 내역", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { // 콜백 연결
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.shadow(3.dp)
            )
        },
        containerColor = Color(0xFFF2F4F6)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(), // 중복 padding 제거
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
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
                        onEditClick = { targetItem ->
                            selectedItemForEdit = targetItem
                            showEditDialog = true
                        },
                        onDetailClick = { targetItem ->
                            selectedItemForDetail = targetItem
                            showDetailDialog = true
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFFF2F4F6))
//                    .background(Color.White) // 배경을 화이트 패널로 주어 리스트 컴포넌트 시인성 가림 방지
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFE5E8EB), RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (items.isNotEmpty()) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    exportToExcel(context, items) { success ->
                                        if (success) {
                                            showExcelSuccessDialog = true
                                        }
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "이 기능은 Android 10 이상에서 지원됩니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                Toast.makeText(context, "내보낼 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
                            }
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Excel Export",
                        modifier = Modifier.size(22.dp),
                        tint = Color(0xFF3182F6)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "엑셀 내보내기 (Excel)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3182F6) // 다크 그레이 문자 매칭
                    )
                }
            }

            if (showEditDialog && selectedItemForEdit != null) {
                EditMeasurementDialog(
                    item = selectedItemForEdit!!,
                    onDismiss = {
                        showEditDialog = false
                        selectedItemForEdit = null
                    },
                    onConfirm = { updatedItem ->
                        scope.launch {
                            db.measurementDao().updateMeasurement(updatedItem)
                            Toast.makeText(context, "수정되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                        showEditDialog = false
                        selectedItemForEdit = null
                    }
                )
            }

            if (showExcelSuccessDialog) {
                AlertDialog(
                    onDismissRequest = { showExcelSuccessDialog = false },
                    shape = RoundedCornerShape(24.dp), //  특유의 둥글고 세련된 느낌 적용
                    containerColor = Color.White,
                    title = {
                        Text(
                            text = "엑셀 파일이 준비되었어요",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191F28) //  주요 다크 그레이
                        )
                    },
                    text = {
                        Text(
                            text = "현장 측정 데이터 내역이 엑셀 형식으로 \n" +
                                    "기기의 다운로드(Download) 폴더에 저장되었어요",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4E5968),
                            lineHeight = 22.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { showExcelSuccessDialog = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3182F6),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "확인",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = null
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun exportToExcel(
    context: android.content.Context,
    items: List<MeasurementEntity>,
    onResult: (Boolean) -> Unit
) {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("측정 내역 데이터")

    val headerRow = sheet.createRow(0)
    headerRow.createCell(0).setCellValue("목록")
    headerRow.createCell(1).setCellValue("상세 정보")

    items.forEachIndexed { index, item ->
        val row = sheet.createRow(index + 1)
        val title = "${item.id}번 측정 데이터"

        val detail = if (item.selectedChamberShape == "사각형") {
            val dims = item.chamberSize.split("x")
            val w = dims.getOrNull(0) ?: "-"
            val h = dims.getOrNull(0) ?: "-"
            "${item.manholeType} / ${item.topieValue}m / ${w}m / ${h}m"
        } else {
            "${item.manholeType} / ${item.topieValue}m / ${item.chamberSize}"
        }

        row.createCell(0).setCellValue(title)
        row.createCell(1).setCellValue(detail)
    }

    // 셀 너비 강제 확장 조절 셋팅
    sheet.setColumnWidth(0, 25 * 256)
    // 상세 정보 컬럼은 텍스트 길이 조건에 맞게
//    sheet.autoSizeColumn(1)
    sheet.setColumnWidth(1, 45 * 256)
//    sheet.setColumnWidth(2, 20 * 256)

    // 저장할 파일명 설정(.xlsx 확장자)
    val fileName = "TerraDIsto_Export_${System.currentTimeMillis()}.xlsx"

    val resolver = context.contentResolver
    val contentValues = android.content.ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
    }

    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

    if (uri != null) {
        try {
            resolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                // POI 통합문서 바이너리 스트림을 파일 출력지에 직접 플러시 연동
                workbook.write(outputStream)
            }
            workbook.close()
            onResult(true)
        } catch (e: Exception) {
            workbook.close()
            onResult(false)
            android.widget.Toast.makeText(
                context,
                "엑셀 생성 실패: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    } else {
        workbook.close()
        onResult(false)
    }



//    // 엑셀 내용 구성
//    val csvHeader = "목록, 상세정보\n"
//    val csvBody = items.joinToString("\n") { item ->
//        val title = "${item.id}번 측정 데이터"
//
//        // 데이터 조합(사각형 원형 구분)
//        val detail = if (item.selectedChamberShape == "사각형") {
//            val dims = item.chamberSize.split(" x ")
//            val w = dims.getOrNull(0) ?: "-"
//            val h = dims.getOrNull(1) ?: "-"
//            "${item.manholeType} / ${item.topieValue}m / ${w}m / ${h}m"
//        } else {
//            "${item.manholeType} / ${item.topieValue}m / ${item.chamberSize}"
//        }
//        "\"$title\",\"$detail\"" // 쉼표가 데이터에 포함될 경우 큰 따옴표로 감싸주기
//    }
//
//    val fullContent = csvHeader + csvBody
//    val fileName = "TerraDIsto_Export_${System.currentTimeMillis()}.csv"
//
//    // MediaStore 이용해서 DownLoad 폴더 저장
//    val resolver = context.contentResolver
//    val contentValues = android.content.ContentValues().apply {
//        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
//        put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
//        }
//    }
//
//    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
//
//    if (uri != null) {
//        try {
//            resolver.openOutputStream(uri)?.use { outputStream ->
//                OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
//                    // Excel 한글 깨짐 방지를 위한 BOM(Byte Order Mark) 추가
//                    writer.write('\uFEFF'.toInt())
//                    writer.write(fullContent)
//                }
//            }
//            onResult(true)
//        } catch (e: Exception) {
//            onResult(false)
//            android.widget.Toast.makeText(
//                context,
//                "저장 실패: ${e.message}",
//                android.widget.Toast.LENGTH_SHORT
//            ).show()
//        }
//    } else {
//        onResult(false)
//    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHistory() {
    // 프리뷰용 더미 데이터 완성
    val dummy = listOf(
        MeasurementEntity(
            id = 1,
            projectId = 1,
            manholeType = "오수 (원형)",
            lidMaterial = "주철",
            lidSize = "0.900",
            topieValue = "320",
            chamberMaterial = "콘크리트",
            chamberSize = "1200",
            selectedChamberShape = "원형",
            hasLadder = true,
            hasInverter = false,
            anomalyMemo = "특이사항 없음",
            timestamp = System.currentTimeMillis(),
            pipeList = emptyList()
        ),
        MeasurementEntity(
            id = 2,
            projectId = 1,
            manholeType = "우수 (사각)",
            lidMaterial = "콘크리트",
            lidSize = "0.600",
            topieValue = "150",
            chamberMaterial = "벽돌",
            chamberSize = "900 x 900",
            selectedChamberShape = "사각형",
            hasLadder = false,
            hasInverter = true,
            anomalyMemo = "내부 파손 확인됨",
            timestamp = System.currentTimeMillis() - 86400000,
            pipeList = emptyList()
        )
    )

    // 실제 화면 호출
    MeasureHistoryScreen(
        items = dummy,
        onBackClick = {}
    )
}