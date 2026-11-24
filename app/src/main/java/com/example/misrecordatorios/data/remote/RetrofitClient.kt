
package com.example.misrecordatorios.data.remote

import android.util.Log
import com.example.misrecordatorios.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // ============================================
    // 1. CONFIGURACION GENERAL
    // ============================================

    private const val TAG = "MisRecordatoriosHTTP"

    private const val CONNECT_TIMEOUT = 5L
    private const val READ_TIMEOUT = 5L
    private const val WRITE_TIMEOUT = 5L
    private const val CALL_TIMEOUT = 10L

    val baseUrl: String
        get() = BuildConfig.RECORDATORIOS_BASE_URL


    // ============================================
    // 2. CONFIGURACION GSON
    // ============================================

    private val gson: Gson by lazy {

        GsonBuilder()
            .serializeNulls()
            .create()
    }


    // ============================================
    // 3. LOGGING HTTP
    // ============================================

    private val loggingInterceptor:
            HttpLoggingInterceptor by lazy {

        HttpLoggingInterceptor { mensaje ->

            Log.d(TAG, mensaje)

        }.apply {

            level = if (BuildConfig.DEBUG) {

                HttpLoggingInterceptor.Level.BASIC

            } else {

                HttpLoggingInterceptor.Level.NONE
            }

            redactHeader("Authorization")
            redactHeader("Cookie")
        }
    }


    // ============================================
    // 4. CLIENTE OKHTTP
    // ============================================

    private val okHttpClient: OkHttpClient by lazy {

        OkHttpClient.Builder()

            .connectTimeout(
                CONNECT_TIMEOUT,
                TimeUnit.SECONDS
            )

            .readTimeout(
                READ_TIMEOUT,
                TimeUnit.SECONDS
            )

            .writeTimeout(
                WRITE_TIMEOUT,
                TimeUnit.SECONDS
            )

            .callTimeout(
                CALL_TIMEOUT,
                TimeUnit.SECONDS
            )

            .connectionPool(
                ConnectionPool(
                    0,
                    1,
                    TimeUnit.SECONDS
                )
            )

            .retryOnConnectionFailure(false)

            .addInterceptor(loggingInterceptor)

            .build()
    }


    // ============================================
    // 5. INSTANCIA RETROFIT
    // ============================================

    private val retrofit: Retrofit by lazy {

        Retrofit.Builder()

            .baseUrl(baseUrl)

            .client(okHttpClient)

            .addConverterFactory(
                GsonConverterFactory.create(gson)
            )

            .build()
    }


    // ============================================
    // 6. INTERFAZ DEL SERVICIO
    // ============================================

    val api: RecordatoriosApi by lazy {

        retrofit.create(
            RecordatoriosApi::class.java
        )
    }
}
 