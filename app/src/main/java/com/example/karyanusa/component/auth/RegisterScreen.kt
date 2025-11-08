package com.example.karyanusa.component.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.karyanusa.R
import com.example.karyanusa.network.RegisterRequest
import com.example.karyanusa.network.RegisterResponse
import com.example.karyanusa.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun RegisterScreen(navController: NavController) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.karyanusabg),
            contentDescription = "Background",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 380.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "REGISTER",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E342E)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Nama Lengkap") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFD89B9B),
                    unfocusedBorderColor = Color(0xFFD89B9B),
                    focusedLabelColor = Color(0xFF4E342E)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFD89B9B),
                    unfocusedBorderColor = Color(0xFFD89B9B),
                    focusedLabelColor = Color(0xFF4E342E)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFD89B9B),
                    unfocusedBorderColor = Color(0xFFD89B9B),
                    focusedLabelColor = Color(0xFF4E342E)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            val context = LocalContext.current

            Button(
                onClick = {
                    if (fullName.isBlank() || username.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val request = RegisterRequest(
                        nama = fullName,
                        email = username,
                        password = password
                    )

                    Log.d("RegisterScreen", "Mengirim request: $request")

                    RetrofitClient.instance.registerUser(request)
                        .enqueue(object : Callback<RegisterResponse> {
                            override fun onResponse(
                                call: Call<RegisterResponse>,
                                response: Response<RegisterResponse>
                            ) {
                                if (response.isSuccessful) {
                                    val body = response.body()
                                    Log.d("RegisterScreen", "Response sukses: $body")

                                    if (body?.status == true) {
                                        Toast.makeText(context, "Registrasi berhasil!", Toast.LENGTH_SHORT).show()
                                        navController.navigate("login")
                                    } else {
                                        Toast.makeText(context, "Gagal: ${body?.message ?: "Tidak diketahui"}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Log.e("RegisterScreen", "Response gagal: ${response.errorBody()?.string()}")
                                    Toast.makeText(context, "Gagal konek ke server", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                                Log.e("RegisterScreen", "Error: ${t.message}", t)
                                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037))
            ) {
                Text("Register", fontSize = 20.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            val annotatedText = buildAnnotatedString {
                append("Sudah punya akun? ")
                withStyle(style = SpanStyle(color = Color(0xFFFF0057), fontWeight = FontWeight.Bold)) {
                    append("Login")
                }
            }

            TextButton(
                onClick = { navController.navigate("login") },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = annotatedText, color = Color.Black)
            }
        }
    }
}
