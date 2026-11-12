package com.example.learn_kotlin.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Brand = Color(0xFF5E3BA8)
private val BrandDeep = Color(0xFF402174)
private val BrandSoft = Color(0xFFF0EAFB)
private val Ink = Color(0xFF241F2B)
private val Muted = Color(0xFF706B79)
private val Canvas = Color(0xFFF8F7FB)

@Composable
fun AuthTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = Brand,
        onPrimary = Color.White,
        primaryContainer = BrandSoft,
        onPrimaryContainer = BrandDeep,
        background = Canvas,
        surface = Color.White,
        onSurface = Ink,
        onSurfaceVariant = Muted,
        error = Color(0xFFB3261E)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun LoginScreen(
    loading: Boolean,
    serverError: String?,
    initialEmail: String,
    onLogin: (String, String) -> Unit,
    onSignup: () -> Unit,
    onForgotPassword: (String) -> Unit
) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var password by rememberSaveable { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    AuthPage {
        AuthCard {
            Spacer(Modifier.height(24.dp))
            Text("Chào mừng trở lại", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text("Đăng nhập để tiếp tục mua sắm cùng Lumi.", style = MaterialTheme.typography.bodyMedium, color = Muted)
            Spacer(Modifier.height(24.dp))
            AuthField(
                value = email,
                onValueChange = { email = it; emailError = null },
                label = "Email",
                placeholder = "name@example.com",
                leading = { Icon(Icons.Default.Email, contentDescription = null) },
                error = emailError,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
            Spacer(Modifier.height(14.dp))
            AuthPasswordField(
                value = password,
                onValueChange = { password = it; passwordError = null },
                label = "Mật khẩu",
                visible = showPassword,
                onVisibilityChange = { showPassword = !showPassword },
                error = passwordError,
                imeAction = ImeAction.Done,
                onImeAction = { submitLogin(email, password, { emailError = it }, { passwordError = it }, onLogin) }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onForgotPassword(email.trim()) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)) {
                    Text("Quên mật khẩu?", fontWeight = FontWeight.SemiBold)
                }
            }
            ServerError(serverError)
            PrimaryAction(text = "Đăng nhập", loading = loading, onClick = {
                submitLogin(email, password, { emailError = it }, { passwordError = it }, onLogin)
            })
            Spacer(Modifier.height(20.dp))
            AccountPrompt(text = "Chưa có tài khoản?", action = "Tạo tài khoản", onClick = onSignup)
        }
        AuthFooter()
    }

}

@Composable
fun SignupScreen(
    loading: Boolean,
    serverError: String?,
    onSignup: (String, String, String) -> Unit,
    onLogin: () -> Unit
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var fullNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirmPassword by rememberSaveable { mutableStateOf(false) }

    AuthPage {
        AuthCard {
            Spacer(Modifier.height(22.dp))
            Text("Tạo tài khoản", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text("Đăng ký để lưu sản phẩm và theo dõi đơn hàng.", style = MaterialTheme.typography.bodyMedium, color = Muted)
            Spacer(Modifier.height(22.dp))
            AuthField(
                value = fullName,
                onValueChange = { fullName = it; fullNameError = null },
                label = "Họ và tên",
                placeholder = "Nguyễn Minh Anh",
                leading = { Icon(Icons.Default.Person, contentDescription = null) },
                error = fullNameError,
                imeAction = ImeAction.Next
            )
            Spacer(Modifier.height(12.dp))
            AuthField(
                value = email,
                onValueChange = { email = it; emailError = null },
                label = "Email",
                placeholder = "name@example.com",
                leading = { Icon(Icons.Default.Email, contentDescription = null) },
                error = emailError,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
            Spacer(Modifier.height(12.dp))
            AuthPasswordField(
                value = password,
                onValueChange = { password = it; passwordError = null },
                label = "Mật khẩu",
                visible = showPassword,
                onVisibilityChange = { showPassword = !showPassword },
                error = passwordError,
                imeAction = ImeAction.Next
            )
            Spacer(Modifier.height(4.dp))
            Text("8–64 ký tự", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelMedium, color = Muted)
            Spacer(Modifier.height(12.dp))
            AuthPasswordField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; confirmError = null },
                label = "Nhập lại mật khẩu",
                visible = showConfirmPassword,
                onVisibilityChange = { showConfirmPassword = !showConfirmPassword },
                error = confirmError,
                imeAction = ImeAction.Done,
                onImeAction = {
                    submitSignup(fullName, email, password, confirmPassword,
                        { fullNameError = it }, { emailError = it }, { passwordError = it }, { confirmError = it }, onSignup)
                }
            )
            ServerError(serverError)
            Spacer(Modifier.height(18.dp))
            PrimaryAction(text = "Tạo tài khoản", loading = loading, onClick = {
                submitSignup(fullName, email, password, confirmPassword,
                    { fullNameError = it }, { emailError = it }, { passwordError = it }, { confirmError = it }, onSignup)
            })
            Spacer(Modifier.height(18.dp))
            AccountPrompt(text = "Đã có tài khoản?", action = "Đăng nhập", onClick = onLogin)
        }
        AuthFooter()
    }
}

@Composable
fun ForgotPasswordScreen(
    initialIdentity: String,
    otpRequested: Boolean,
    otpVerified: Boolean,
    requestLoading: Boolean,
    verifyLoading: Boolean,
    resetLoading: Boolean,
    serverError: String?,
    onRequestOtp: (String) -> Unit,
    onVerifyOtp: (String, String) -> Unit,
    onResetPassword: (String, String) -> Unit,
    onBackToLogin: () -> Unit
) {
    var identity by rememberSaveable { mutableStateOf(initialIdentity) }
    var otp by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var identityError by remember { mutableStateOf<String?>(null) }
    var otpError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirmPassword by rememberSaveable { mutableStateOf(false) }

    AuthPage {
        AuthCard {
            TextButton(onClick = onBackToLogin, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)) {
                Text("←  Quay lại đăng nhập")
            }
            Spacer(Modifier.height(10.dp))
            val stepTitle = when { !otpRequested -> "Bước 1/3 · Nhận mã OTP"; !otpVerified -> "Bước 2/3 · Xác thực OTP"; else -> "Bước 3/3 · Tạo mật khẩu mới" }
            Text(stepTitle, style = MaterialTheme.typography.titleMedium, color = Brand, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Quên mật khẩu?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text(
                when { !otpRequested -> "Nhập email để nhận mã xác thực khôi phục tài khoản."; !otpVerified -> "Nhập mã OTP được gửi đến email của bạn để xác minh."; else -> "OTP đã được xác minh. Hãy tạo mật khẩu mới cho tài khoản." },
                style = MaterialTheme.typography.bodyMedium,
                color = Muted
            )
            Spacer(Modifier.height(22.dp))

            if (!otpRequested) {
                AuthField(
                    value = identity,
                    onValueChange = { identity = it; identityError = null },
                    label = "Email",
                    placeholder = "name@example.com",
                    leading = { Icon(Icons.Default.Email, contentDescription = null) },
                    error = identityError,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    enabled = !requestLoading
                )
                ServerError(serverError)
                Spacer(Modifier.height(16.dp))
                PrimaryAction(text = "Gửi mã xác thực", loading = requestLoading, onClick = {
                    val clean = identity.trim()
                    val valid = android.util.Patterns.EMAIL_ADDRESS.matcher(clean).matches()
                    identityError = if (valid) null else "Vui lòng nhập email hợp lệ"
                    if (valid) onRequestOtp(clean)
                })
            } else if (!otpVerified) {
                Text(
                    "Mã xác thực đã được gửi đến ${maskIdentity(identity)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
                Spacer(Modifier.height(16.dp))
                AuthField(
                    value = otp,
                    onValueChange = { value -> otp = value.filter(Char::isDigit).take(6); otpError = null },
                    label = "Mã OTP gồm 6 chữ số",
                    placeholder = "123456",
                    leading = { Icon(Icons.Default.Lock, contentDescription = null) },
                    error = otpError,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
                ServerError(serverError)
                TextButton(onClick = { onRequestOtp(identity.trim()) }, enabled = !requestLoading && !verifyLoading) {
                    Text(if (requestLoading) "Đang gửi mã…" else "Gửi lại mã OTP")
                }
                Spacer(Modifier.height(4.dp))
                PrimaryAction(text = "Xác thực OTP", loading = verifyLoading, onClick = {
                    otpError = if (otp.length == 6) null else "Nhập đủ 6 chữ số trong tin nhắn"
                    if (otp.length == 6) onVerifyOtp(identity.trim(), otp)
                })
            } else {
                Text("OTP đã xác minh cho ${maskIdentity(identity)}.", style = MaterialTheme.typography.bodySmall, color = Muted)
                Spacer(Modifier.height(16.dp))
                AuthPasswordField(value = newPassword, onValueChange = { newPassword = it; passwordError = null }, label = "Mật khẩu mới", visible = showPassword, onVisibilityChange = { showPassword = !showPassword }, error = passwordError, imeAction = ImeAction.Next)
                Spacer(Modifier.height(4.dp))
                Text("8–64 ký tự", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelMedium, color = Muted)
                Spacer(Modifier.height(12.dp))
                AuthPasswordField(value = confirmPassword, onValueChange = { confirmPassword = it; confirmError = null }, label = "Nhập lại mật khẩu mới", visible = showConfirmPassword, onVisibilityChange = { showConfirmPassword = !showConfirmPassword }, error = confirmError, imeAction = ImeAction.Done)
                ServerError(serverError)
                Spacer(Modifier.height(16.dp))
                PrimaryAction(text = "Đặt lại mật khẩu", loading = resetLoading, onClick = {
                    passwordError = if (newPassword.length in 8..64) null else "Mật khẩu cần từ 8 đến 64 ký tự"
                    confirmError = when { confirmPassword.isBlank() -> "Vui lòng nhập lại mật khẩu"; confirmPassword != newPassword -> "Mật khẩu nhập lại không trùng khớp"; else -> null }
                    if (newPassword.length in 8..64 && confirmPassword == newPassword) onResetPassword(identity.trim(), newPassword)
                })
            }
            Spacer(Modifier.height(18.dp))
            AccountPrompt(text = "Nhớ mật khẩu rồi?", action = "Đăng nhập", onClick = onBackToLogin)
        }
        AuthFooter()
    }
}

private fun maskIdentity(identity: String): String = if (identity.contains('@')) {
    val parts = identity.split('@', limit = 2)
    val name = parts[0]
    "${name.take(1)}${"•".repeat((name.length - 1).coerceAtLeast(2))}@${parts[1]}"
} else {
    val digits = identity.filter(Char::isDigit)
    "••••••${digits.takeLast(4)}"
}

private fun submitLogin(
    email: String,
    password: String,
    setEmailError: (String?) -> Unit,
    setPasswordError: (String?) -> Unit,
    onSubmit: (String, String) -> Unit
) {
    val cleanEmail = email.trim()
    val emailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()
    setEmailError(when { cleanEmail.isBlank() -> "Vui lòng nhập email"; !emailValid -> "Email chưa đúng định dạng"; else -> null })
    setPasswordError(if (password.isBlank()) "Vui lòng nhập mật khẩu" else null)
    if (emailValid && password.isNotBlank()) onSubmit(cleanEmail, password)
}

private fun submitSignup(
    fullName: String,
    email: String,
    password: String,
    confirmation: String,
    setNameError: (String?) -> Unit,
    setEmailError: (String?) -> Unit,
    setPasswordError: (String?) -> Unit,
    setConfirmationError: (String?) -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    val cleanName = fullName.trim()
    val cleanEmail = email.trim()
    val emailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()
    setNameError(if (cleanName.isBlank()) "Vui lòng nhập họ và tên" else null)
    setEmailError(when { cleanEmail.isBlank() -> "Vui lòng nhập email"; !emailValid -> "Email chưa đúng định dạng"; else -> null })
    setPasswordError(if (password.length in 8..64) null else "Mật khẩu cần từ 8 đến 64 ký tự")
    setConfirmationError(when { confirmation.isBlank() -> "Vui lòng nhập lại mật khẩu"; password != confirmation -> "Mật khẩu nhập lại không trùng khớp"; else -> null })
    if (cleanName.length >= 2 && emailValid && password.length in 8..64 && password == confirmation) onSubmit(cleanName, cleanEmail, password)
}

@Composable
private fun AuthPage(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Canvas)) {
        Column(
            modifier = Modifier.fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth().height(230.dp)) {
                AuthHeroBackdrop(Modifier.fillMaxSize())
                Column(
                    Modifier.align(Alignment.TopCenter).padding(top = 50.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BrandMark()
                    Spacer(Modifier.height(13.dp))
                    Text("LUMI", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
                    Text("Mỗi ngày, một điều bạn yêu thích", color = Color.White.copy(alpha = .86f), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 0.dp, bottom = 10.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun AuthHeroBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.background(Brush.linearGradient(listOf(BrandDeep, Brand, Color(0xFF8A69C4))))) {
        Box(Modifier.align(Alignment.TopStart).padding(start = 22.dp, top = 24.dp).size(116.dp).clip(CircleShape).background(Color.White.copy(alpha = .07f)))
        Box(Modifier.align(Alignment.TopEnd).padding(end = 18.dp, top = 36.dp).size(174.dp).clip(CircleShape).background(Color.White.copy(alpha = .08f)))
        Box(Modifier.align(Alignment.BottomStart).padding(start = 82.dp, bottom = 3.dp).size(74.dp).clip(CircleShape).background(Color(0xFFB59AE5).copy(alpha = .22f)))
    }
}

@Composable
private fun BrandMark() {
    Box(
        Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.ShoppingBag, contentDescription = "Lumi", tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun AuthCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 22.dp), verticalArrangement = Arrangement.Top, content = { content() })
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leading: @Composable () -> Unit,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder, color = Muted.copy(alpha = .7f)) },
        leadingIcon = leading,
        singleLine = true,
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Brand,
            unfocusedBorderColor = Color(0xFFE5E0EA),
            focusedLabelColor = Brand,
            focusedLeadingIconColor = Brand,
            unfocusedLeadingIconColor = Muted,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

@Composable
private fun AuthPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onVisibilityChange: () -> Unit,
    error: String?,
    imeAction: ImeAction,
    onImeAction: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onVisibilityChange, modifier = Modifier.size(48.dp)) {
                Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = if (visible) "Ẩn mật khẩu" else "Hiện mật khẩu")
            }
        },
        singleLine = true,
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onImeAction?.invoke() }),
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Brand,
            unfocusedBorderColor = Color(0xFFE5E0EA),
            focusedLabelColor = Brand,
            focusedLeadingIconColor = Brand,
            unfocusedLeadingIconColor = Muted,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

@Composable
private fun PrimaryAction(text: String, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Brand, contentColor = Color.White)
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(21.dp), color = Color.White, strokeWidth = 2.dp)
        else {
            Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(9.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AccountPrompt(text: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Muted, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 6.dp, end = 6.dp)) {
            Text(action, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ServerError(message: String?) {
    if (message.isNullOrBlank()) return
    Surface(Modifier.fillMaxWidth().padding(bottom = 12.dp), color = Color(0xFFFFEFEE), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, color = Color(0xFF9D2520), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AuthFooter() {
    Row(
        Modifier.fillMaxWidth().padding(top = 17.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF39805B), modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text("Hỗ trợ xác thực email qua OTP", color = Muted, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}
