package com.xjtu.toolbox.neo

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

private const val TAG = "NeoSession"

class NeoSession(context: Context) {

    companion object {
        const val BASE_URL = "https://edu4-api.neoschool.com"
        const val APP_ID = "0D287AA766884C89A5F6569B1BA61F79"
        const val PLATFORM = "S"
        private const val PREFS_NAME = "neo_session"
        private const val KEY_TOKEN = "token"
        private const val KEY_ACCOUNT = "account"
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private var rawToken: String = prefs.getString(KEY_TOKEN, "") ?: ""

    val token: String get() = rawToken

    var savedAccount: String = prefs.getString(KEY_ACCOUNT, "") ?: ""
        private set

    val isLoggedIn: Boolean get() = rawToken.isNotEmpty()

    fun saveSession(newRawToken: String, account: String) {
        rawToken = newRawToken
        savedAccount = account
        prefs.edit()
            .putString(KEY_TOKEN, newRawToken)
            .putString(KEY_ACCOUNT, account)
            .apply()
    }

    fun clearSession() {
        rawToken = ""
        savedAccount = ""
        prefs.edit().remove(KEY_TOKEN).remove(KEY_ACCOUNT).apply()
    }

    fun authenticatedRequest(url: String): Request.Builder =
        Request.Builder()
            .url(url)
            .header("appid", APP_ID)
            .header("p", PLATFORM)
            .header("Authorization", "Bearer $rawToken")
            .header("Cookie", "token=$rawToken")
            .header("Origin", "https://www.neoschool.com")
            .header("Referer", "https://www.neoschool.com/")

    fun postJson(url: String, bodyJson: String = "{}"): String {
        val request = authenticatedRequest(url)
            .post(bodyJson.toByteArray(Charsets.UTF_8).toRequestBody(JSON_TYPE))
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        response.close()
        return body
    }

    fun login(account: String, password: String): Pair<Boolean, String> {
        Log.d(TAG, "login: account=$account")
        val request = Request.Builder()
            .url("$BASE_URL/sdm/bic/user/login")
            .header("appid", APP_ID)
            .header("p", PLATFORM)
            .post(
                FormBody.Builder()
                    .add("username", account)
                    .add("password", password)
                    .build()
            )
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            response.close()
            Log.d(TAG, "login response: $responseBody")

            val gson = Gson()
            val json = gson.fromJson(responseBody, com.google.gson.JsonObject::class.java)
            val success = json.get("success")?.asBoolean ?: false
            if (success) {
                val data = json.getAsJsonObject("data")
                val tokenStr = data?.get("token")?.asString ?: ""
                if (tokenStr.isNotEmpty()) {
                    saveSession(tokenStr, account)
                    Pair(true, "登录成功")
                } else {
                    Pair(false, "未获取到授权令牌")
                }
            } else {
                val message = json.get("msg")?.asString ?: "登录失败"
                Pair(false, message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "login error", e)
            Pair(false, "网络错误：${e.message}")
        }
    }
}
