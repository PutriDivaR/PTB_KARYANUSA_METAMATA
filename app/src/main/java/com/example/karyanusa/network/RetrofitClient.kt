package com.example.karyanusa.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://algometrically-squabby-kinsley.ngrok-free.dev"
    // ini kalau pakai ngrok ngrok wkwkw:
    // BASE_URL = "https://algometrically-squabby-kinsley.ngrok-free.dev"
    // BASE_URL = "https://ornamented-ken-semisentimentally.ngrok-free.dev" punya vans
    // ini kalau pakai emulator:
    // BASE_URL = "http://10.0.2.2:8000/"
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // Tambah ini
        .readTimeout(30, TimeUnit.SECONDS)     // Tambah ini
        .writeTimeout(30, TimeUnit.SECONDS)    // Tambah ini
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    // .addInterceptor(logging)
        //.build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
