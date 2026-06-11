package com.terra.terradisto.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(
    currentLicenseKey: String = "06A9-B7AB-D490",
    hasServerLicense: Boolean = false, // 서버 라이선스 상태 추가
    onLicenseSaveClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 회원정보 상태 (조회전용)
    val userId by remember { mutableStateOf("admin") }
    val userPassword by remember { mutableStateOf("••••••••••••") }
    var passwordVisible by remember { mutableStateOf(false) }

    // 라이선스 섹션 상태 관리
    var isEditMode by remember { mutableStateOf(false) }
    var licenseInput by remember { mutableStateOf(currentLicenseKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("마이페이지", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF191F28))
                        Text("계정 및 라이선스 관리", fontSize = 12.sp, color = Color(0xFF8B95A1))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "뒤로가기", tint = Color(0xFF191F28))
                    }
                },
                actions = {
                    // 상단 우측 프로필 아바타 아이콘
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(36.dp)
                            .background(Color(0xFFE8F3FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = "프로필", tint = Color(0xFF3182F6))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF2F4F6) // 토스 배경 회색 단색
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // -------------------------------------------------------------
            // [섹션 1] 회원정보 (조회 전용 및 웹 이동)
            // -------------------------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // 섹션 헤더
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF3182F6), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("회원정보", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF191F28))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 아이디 표시 영역
                    Text("아이디", fontSize = 13.sp, color = Color(0xFF4E5968), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(Color(0xFFF2F4F6), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(text = userId, fontSize = 15.sp, color = Color(0xFF8B95A1))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 비밀번호 표시 영역
                    Text("비밀번호", fontSize = 13.sp, color = Color(0xFF4E5968), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(Color(0xFFF2F4F6), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (passwordVisible) "admin1234!" else userPassword,
                            fontSize = 15.sp,
                            color = Color(0xFF8B95A1)
                        )
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "비밀번호 보기 토글",
                            tint = Color(0xFF8B95A1),
                            modifier = Modifier.clickable { passwordVisible = !passwordVisible }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("회원정보를 변경하려면 수정 버튼을 누르세요.", fontSize = 12.sp, color = Color(0xFF8B95A1))
                    Spacer(modifier = Modifier.height(16.dp))

                    // 웹사이트 이동 수정 버튼
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://terra-survey.com/auth/login"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F3FF)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color(0xFF3182F6), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("수정", color = Color(0xFF3182F6), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // -------------------------------------------------------------
            // [섹션 2] 라이선스 (앱 내부 동적 수정 및 저장 기능)
            // -------------------------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // 섹션 헤더
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF3182F6), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("라이선스", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF191F28))
                        }

                        val badgeBgColor = if (hasServerLicense) Color(0xFFE8F3FF) else Color(0xFFFEE4E2)
                        val badgeTextColor = if (hasServerLicense) Color(0xFF3182F6) else Color(0xFFF04452)
                        val badgeText = if (hasServerLicense) "● 라이선스 등록됨" else "● 라이선스 미등록"

                        // 상단 현재 상태 뱃지
                        Box(
                            modifier = Modifier
                                .background(badgeBgColor, RoundedCornerShape(30.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
//                            Text("● 라이선스 등록됨", fontSize = 11.sp, color = Color(0xFF3182F6), fontWeight = FontWeight.Bold)
                            Text(
                                text = badgeText,
                                fontSize = 11.sp,
                                color = badgeTextColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("라이선스 키", fontSize = 13.sp, color = Color(0xFF4E5968), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))

                    // 라이선스 입력 필드 (상태에 따라 스타일 전환)
                    BasicTextField(
                        value = licenseInput,
                        onValueChange = { if (isEditMode) licenseInput = it },
                        enabled = isEditMode,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = if (isEditMode) Color(0xFF191F28) else Color(0xFF6B7684),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .background(
                                        color = if (isEditMode) Color.White else Color(0xFFF2F4F6),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = if (isEditMode) 1.5.dp else 0.dp,
                                        color = if (isEditMode) Color(0xFF3182F6) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                innerTextField()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isEditMode) "변경할 라이선스 코드를 입력한 후 저장해주세요." else "라이선스 키를 변경하려면 수정 버튼을 누르세요.",
                        fontSize = 12.sp,
                        color = Color(0xFF8B95A1)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // 상태별 동적 하단 버튼 영역 제어 (수정 vs 취소/저장)
                    AnimatedVisibility(visible = !isEditMode) {
                        Button(
                            onClick = { isEditMode = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F3FF)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color(0xFF3182F6), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("수정", color = Color(0xFF3182F6), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    AnimatedVisibility(visible = isEditMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 취소 버튼
                            OutlinedButton(
                                onClick = {
                                    isEditMode = false
                                    licenseInput = currentLicenseKey // 기존 키로 복원
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6B7684)),
                                border = BorderStroke(1.dp, Color(0xFFE5E8EB))
                            ) {
                                Text("✕ 취소", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            // 저장 버튼
                            Button(
                                onClick = {
                                    isEditMode = false
                                    onLicenseSaveClick(licenseInput)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3182F6))
                            ) {
                                Text("💾 저장", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMyPageScreen() {
    MaterialTheme {
        MyPageScreen()
    }
}