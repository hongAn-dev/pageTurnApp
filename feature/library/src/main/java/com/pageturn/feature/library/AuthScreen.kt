package com.pageturn.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pageturn.core.designsystem.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val authUiState by viewModel.authUiState.collectAsState()
    
    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authUiState) {
        when (authUiState) {
            is LibraryViewModel.AuthUiState.Success -> {
                viewModel.resetAuthState()
                onAuthSuccess()
            }
            is LibraryViewModel.AuthUiState.Error -> {
                errorMessage = (authUiState as LibraryViewModel.AuthUiState.Error).message
            }
            else -> {
                errorMessage = null
            }
        }
    }

    // Dynamic background colors according to the theme
    val isDark = MaterialTheme.colorScheme.background == PtDarkBg
    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F131C),
                PtDarkBg
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                PtGoldLight,
                MaterialTheme.colorScheme.background
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        // Decorative soft blur canvas elements
        if (!isDark) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-80).dp, y = (-80).dp)
                    .size(240.dp)
                    .clip(RoundedCornerShape(120.dp))
                    .background(PtNavyLight.copy(alpha = 0.5f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 100.dp, y = 100.dp)
                    .size(300.dp)
                    .clip(RoundedCornerShape(150.dp))
                    .background(PtSurfaceWarm.copy(alpha = 0.4f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Logo / App Name Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Libra",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Normal,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 1.5.sp
                    )
                )
                
                // Elegant diamond divider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.width(40.dp).height(1.dp).background(PtGoldenAccent.copy(alpha = 0.5f)))
                    Text(
                        text = " ✦ ✦ ",
                        color = PtGoldenAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(modifier = Modifier.width(40.dp).height(1.dp).background(PtGoldenAccent.copy(alpha = 0.5f)))
                }

                Text(
                    text = if (isSignUpMode) "Tạo tài khoản để đồng bộ thư viện cá nhân" else "Chào mừng bạn quay trở lại hành trình đọc",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Elegant Card containing the forms
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(24.dp),
                        clip = false,
                        ambientColor = if (isDark) Color.Black else PtNavyPrimary.copy(alpha = 0.15f),
                        spotColor = if (isDark) Color.Black else PtNavyPrimary.copy(alpha = 0.25f)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header text inside Card
                    Text(
                        text = if (isSignUpMode) "ĐĂNG KÝ THÀNH VIÊN" else "THÔNG TIN ĐĂNG NHẬP",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = PtGoldenAccent
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Name Field (only sign up)
                    AnimatedVisibility(
                        visible = isSignUpMode,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Họ và Tên") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PtGoldenAccent,
                                focusedLabelColor = PtGoldenAccent,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Địa chỉ Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PtGoldenAccent,
                            focusedLabelColor = PtGoldenAccent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mật khẩu") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PtGoldenAccent,
                            focusedLabelColor = PtGoldenAccent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Error Text Box
                    errorMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = msg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Main Action Button (Gradient Style)
                    val buttonBrush = Brush.horizontalGradient(
                        colors = if (isDark) {
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                        } else {
                            listOf(PtNavyPrimary, PtNavyDark)
                        }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(brush = buttonBrush)
                            .clickable(
                                enabled = authUiState !is LibraryViewModel.AuthUiState.Loading,
                                onClick = {
                                    if (email.isBlank() || password.length < 6) {
                                        errorMessage = "Vui lòng nhập email hợp lệ & mật khẩu tối thiểu 6 ký tự"
                                        return@clickable
                                    }
                                    if (isSignUpMode) {
                                        viewModel.signUpWithEmail(email, password, name.ifBlank { "Độc giả Libra" })
                                    } else {
                                        viewModel.signInWithEmail(email, password)
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (authUiState is LibraryViewModel.AuthUiState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = if (isSignUpMode) "Đăng ký tài khoản" else "Đăng nhập",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Switch sign-in / sign-up
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSignUpMode) "Đã có tài khoản? " else "Chưa có tài khoản? ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isSignUpMode) "Đăng nhập" else "Đăng ký ngay",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.clickable {
                                isSignUpMode = !isSignUpMode
                                errorMessage = null
                            }
                        )
                    }
                }
            }

            // Divider Line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                Text(
                    text = " HOẶC ",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                )
                Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
            }

            // Anonymous Sign In / Skip Option Button
            OutlinedButton(
                onClick = { viewModel.signInAnonymously() },
                enabled = authUiState !is LibraryViewModel.AuthUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(listOf(PtGoldenAccent, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)))
                )
            ) {
                Text(
                    text = "Đọc thử không cần tài khoản",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

