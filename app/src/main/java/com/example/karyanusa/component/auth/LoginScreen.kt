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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.karyanusa.network.RetrofitClient
import com.example.karyanusa.network.LoginRequest
import com.example.karyanusa.network.LoginResponse
import androidx.compose.ui.platform.LocalContext


@Composable
fun LoginScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background
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
                text = "Login",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E342E)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
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
                    val loginRequest = LoginRequest(username = username, password = password)

                    RetrofitClient.instance.loginUser(loginRequest).enqueue(object : Callback<LoginResponse> {
                        override fun onResponse(
                            call: Call<LoginResponse>,
                            response: Response<LoginResponse>
                        ) {
                            if (response.isSuccessful && response.body()?.status == true) {
                                val body = response.body()!!
                                val tokenManager = LoginTokenManager(context)

                                tokenManager.saveToken(
                                    token = body.token,
                                    userId = body.user_id,
                                    userName = body.nama
                                )

                                Log.d("LOGIN_DEBUG", "Saved user_id = ${body.user_id}")
                                Log.d("LOGIN_DEBUG", "Saved token = ${body.token}")
                                Log.d("LOGIN_DEBUG", "Saved username = ${body.nama}")


                                Toast.makeText(context, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                                navController.navigate("beranda")
                            } else {
                                Toast.makeText(context, "Login gagal: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                            Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037))
            ) {
                Text("Login", fontSize = 20.sp, color = Color.White)
            }


            Spacer(modifier = Modifier.height(16.dp))

            val annotatedText = buildAnnotatedString {
                append("Don’t have an account? ")
                withStyle(style = SpanStyle(color = Color(0xFFFF0057), fontWeight = FontWeight.Bold)) {
                    append("Register")
                }
            }

            TextButton(
                onClick = { navController.navigate("register") },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = annotatedText, color = Color.Black)
            }
        }
    }
}

