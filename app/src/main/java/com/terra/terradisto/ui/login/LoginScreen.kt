package com.terra.terradisto.ui.login

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terra.terradisto.viewmodel.LoginViewModel

// 디자인 가이드 컬러
private val BrandBlue = Color(0xFF3182F6)
private val Background = Color(0xFFFFFFFF)
private val FieldBackground = Color(0xFFF2F4F6)
private val TextDark = Color(0xFF191F28)
private val TextGray = Color(0xFF8B95A1)
private val ErrorRed = Color(0xFFF04452)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onContactAdmin: () -> Unit,
    viewModel: LoginViewModel = viewModel() // ViewModel 주입
) {
    var idInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // ViewModel 에서 errorMessage 가져오기
    val errorMessageFromVm = viewModel.errorMessage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
    ) {
        // 상단 여백 확보 (중앙 정렬 유도)
        Spacer(modifier = Modifier.height(80.dp))

        // 로고 및 타이틀
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = BrandBlue,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_edit),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Disto Survey", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Text("스마트 맨홀 측량 시스템에 오신 것을 환영합니다", fontSize = 14.sp, color = TextGray)

        Spacer(modifier = Modifier.height(48.dp))

        // 아이디 입력
        Text("아이디", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = idInput,
            onValueChange = { idInput = it },
            placeholder = { Text("아이디를 입력하세요", color = TextGray) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = FieldBackground,
                unfocusedContainerColor = FieldBackground,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 비밀번호 입력
        Text("비밀번호", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            placeholder = { Text("비밀번호를 입력하세요", color = TextGray) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(mask = '*'),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(painterResource(android.R.drawable.ic_menu_view), contentDescription = null)
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = FieldBackground,
                unfocusedContainerColor = FieldBackground,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Password),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true
        )

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 로그인 버튼
        Button(
            onClick = {
                if (idInput.isEmpty()) {
                    errorMessage = "아이디를 입력해주세요"
                } else if (passwordInput.isEmpty()) {
                    errorMessage = "비밀번호를 입력해주세요"
                } else {
                    viewModel.login(idInput, passwordInput) { success ->
                        if (success) {
                            onLoginSuccess() // 로그인 성공 시, 메인 화면으로 이동
                        } else {
                            errorMessage = viewModel.errorMessage ?: "로그인 실패"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("로그인", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 회원가입 및 하단 고정 정보
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(
            onClick = { uriHandler.openUri("https://terra-survey.com/auth/login?mode=signup") },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("계정이 없으신가요? 회원가입", color = TextGray, textDecoration = TextDecoration.Underline)
        }

        Spacer(modifier = Modifier.height(40.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("계정 문의: 관리자에게 문의하세요", fontSize = 12.sp, color = TextGray, modifier = Modifier.clickable { onContactAdmin() })
            Text("v1.0.0", fontSize = 12.sp, color = TextGray.copy(alpha = 0.5f))
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    name = "Login Screen Preview (Dark Mode)",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun LoginScreenDarkPreview() {
    MaterialTheme {
        LoginScreen(
            onLoginSuccess = {},
            onContactAdmin = {},
//            onSignUpClick = {}
        )
    }
}