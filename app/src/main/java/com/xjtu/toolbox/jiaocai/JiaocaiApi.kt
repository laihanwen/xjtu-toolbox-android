package com.xjtu.toolbox.jiaocai

import android.util.Log
import com.xjtu.toolbox.util.safeParseJsonObject
import com.xjtu.toolbox.util.safeString
import com.xjtu.toolbox.util.safeInt
import okhttp3.Request

private const val TAG = "JiaocaiApi"

// ── 数据模型 ─────────────────────────────────────────────────────────

data class JiaocaiBook(
    val id: String = "",          // general_55258665
    val appId: Int = 0,
    val engineInstanceId: Int = 0,
    val title: String = "",
    val author: String = "",
    val summary: String = "",     // 包含课程名/获取方式等描述（含 HTML tag）
    val ssno: String = "",        // 阅览器 ssno（从详情 HTML 提取）
    val hasFullText: Boolean = false  // 是否有"本地全文"
)

// ── API 类 ───────────────────────────────────────────────────────────

class JiaocaiApi(private val login: JiaocaiLogin) {

    private val client get() = login.client
    private val BASE get() = JiaocaiLogin.BASE_URL
    private val FID get() = JiaocaiLogin.FID
    private val PAGE_ID get() = JiaocaiLogin.PAGE_ID
    private val SEARCH_ID get() = JiaocaiLogin.SEARCH_ID

    private fun get(url: String): String {
        val req = Request.Builder().url(url)
            .header("Referer", "$BASE/")
            .header("X-Requested-With", "XMLHttpRequest")
            .get().build()
        return client.newCall(req).execute().use { it.body?.string() ?: "" }
    }

    /** 搜索教材，返回书目列表 */
    fun search(keyword: String, page: Int = 1, pageSize: Int = 20): List<JiaocaiBook> {
        return try {
            val url = "$BASE/engine2/search/search-list" +
                    "?wfwfid=$FID" +
                    "&keyWord=${java.net.URLEncoder.encode(keyword, "UTF-8")}" +
                    "&pageIndex=$page" +
                    "&pageSize=$pageSize" +
                    "&pageId=$PAGE_ID" +
                    "&searchStrategy=0" +
                    "&searchId=$SEARCH_ID"
            val body = get(url)
            Log.d(TAG, "search[$keyword]: ${body.take(200)}")
            val json = body.safeParseJsonObject()
            val list = json.getAsJsonObject("data")?.getAsJsonArray("dataList") ?: return emptyList()
            list.mapNotNull { elem ->
                try {
                    val obj = elem.asJsonObject
                    val raw = obj.get("content")?.safeString() ?: ""
                    val cleanSummary = raw.replace(Regex("<[^>]+>"), "")
                    val hasFullText = raw.contains("本地全文") || raw.contains("全文获取")
                    JiaocaiBook(
                        id = obj.get("id")?.safeString() ?: return@mapNotNull null,
                        appId = obj.get("appId")?.safeInt() ?: 0,
                        engineInstanceId = obj.get("engineInstanceId")?.safeInt() ?: 0,
                        title = (obj.get("title")?.safeString() ?: "").replace(Regex("<[^>]+>"), ""),
                        author = obj.get("author")?.safeString() ?: "",
                        summary = cleanSummary,
                        hasFullText = hasFullText
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            Log.e(TAG, "search failed", e)
            emptyList()
        }
    }

    /**
     * 获取图书的 ssno（从详情页 HTML 中提取）。
     * 页面 HTML 中包含 `ssno=XXXXXXX` 字符串。
     */
    fun fetchSsno(book: JiaocaiBook): String {
        return try {
            val idNum = book.id.removePrefix("general_")
            val url = "$BASE/engine2/general/$idNum/detail" +
                    "?pageId=$PAGE_ID&engineInstanceId=${book.engineInstanceId}"
            val html = get(url)
            // 从 HTML 中提取 ssno（格式: ssno=15207199 或 ssno\\u003d15207199）
            val m = Regex("""ssno[=\\u0026]+(\d{5,12})""").find(html)
            val ssno = m?.groupValues?.get(1) ?: ""
            Log.d(TAG, "fetchSsno: ${book.id} -> ssno=$ssno")
            ssno
        } catch (e: Exception) {
            Log.e(TAG, "fetchSsno failed for ${book.id}", e)
            ""
        }
    }

    /**
     * 构建阅览器 URL（在 WebView 中打开）。
     * 需要 WebView 携带和 OkHttp client 相同的 cookie。
     */
    fun buildReaderUrl(ssno: String): String =
        "${JiaocaiLogin.READER_HOST}/guajie/common?ssno=$ssno&cpage=1&channel=100"

    /**
     * 获取图书加密路径和 session hash（用于下载页面图片）。
     * 需要先通过 guajie/common 获取 reader.shtml URL，再从 URL 中提取参数。
     */
    fun fetchReaderParams(ssno: String): ReaderParams? {
        return try {
            // 1. 访问 guajie/common，跟随重定向，获取最终 reader.shtml URL
            val guajieUrl = buildReaderUrl(ssno)
            val req = Request.Builder().url(guajieUrl)
                .header("Referer", "$BASE/")
                .get().build()
            val resp = client.newCall(req).execute()
            val finalUrl = resp.request.url.toString()
            resp.close()
            Log.d(TAG, "fetchReaderParams: finalUrl=$finalUrl")

            // 2. 从 reader.shtml URL 提取 code 参数
            val codeMatch = Regex("""[?&]code=([a-f0-9]{32})""").find(finalUrl)
            val code = codeMatch?.groupValues?.get(1) ?: ""

            // 3. 访问 reader.shtml HTML，提取加密图书路径
            val readerReq = Request.Builder().url(finalUrl)
                .header("Referer", guajieUrl)
                .get().build()
            val readerHtml = client.newCall(readerReq).execute().use { it.body?.string() ?: "" }

            // 从 reader.js 配置或页面 HTML 中提取加密路径
            // 格式: /jpath/img/a174/{ENCRYPTED}/0/0/{SESSION}/
            val pathMatch = Regex("""/jpath/img/a174/([A-F0-9]{100,})/0/0/([a-f0-9]{32})/""").find(readerHtml)
            if (pathMatch != null) {
                ReaderParams(
                    ssno = ssno,
                    code = code,
                    encryptedPath = pathMatch.groupValues[1],
                    sessionHash = pathMatch.groupValues[2]
                )
            } else {
                // 路径可能在 reader.js 脚本中，尝试从 reader-config.js 获取
                fetchReaderParamsFromConfig(ssno, code, readerHtml)
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchReaderParams failed for ssno=$ssno", e)
            null
        }
    }

    private fun fetchReaderParamsFromConfig(ssno: String, code: String, readerHtml: String): ReaderParams? {
        return try {
            // reader.js 文件中通常包含图书路径配置
            val configUrl = "${JiaocaiLogin.READER_HOST}/jpath/js/reader/reader-config.js"
            val configBody = Request.Builder().url(configUrl).get().build()
                .let { client.newCall(it).execute().use { r -> r.body?.string() ?: "" } }

            val pathMatch = Regex("""[A-F0-9]{100,}""").find(configBody)
            val sessionMatch = Regex("""[a-f0-9]{32}""").find(configBody)

            if (pathMatch != null && sessionMatch != null) {
                ReaderParams(ssno, code, pathMatch.value, sessionMatch.value)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "fetchReaderParamsFromConfig failed", e)
            null
        }
    }

    /**
     * 构建指定页的图片 URL（无需 pixy 重定向，直接 zoom=0 返回图片）
     */
    fun buildPageImageUrl(params: ReaderParams, pageNum: Int): String {
        val page = "%06d.jpg".format(pageNum)
        return "${JiaocaiLogin.READER_HOST}/jpath/img/a174/${params.encryptedPath}/0/0/${params.sessionHash}/$page?zoom=0"
    }

    fun buildSpecialPageUrl(params: ReaderParams, pageName: String): String =
        "${JiaocaiLogin.READER_HOST}/jpath/img/a174/${params.encryptedPath}/0/0/${params.sessionHash}/$pageName?zoom=0"
}

data class ReaderParams(
    val ssno: String,
    val code: String,
    val encryptedPath: String,
    val sessionHash: String
)
