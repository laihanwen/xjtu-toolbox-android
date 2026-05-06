package com.xjtu.toolbox.jiaocai

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import java.io.File
import java.net.CookiePolicy
import java.net.HttpCookie
import java.net.URI

private const val TAG = "JiaocaiScreen"

// ── 主入口 ────────────────────────────────────────────────────────────

@Composable
fun JiaocaiScreen(login: JiaocaiLogin, onBack: () -> Unit) {
    var selectedBook by remember { mutableStateOf<JiaocaiBook?>(null) }

    if (selectedBook != null) {
        JiaocaiDetailScreen(
            login = login,
            book = selectedBook!!,
            onBack = { selectedBook = null }
        )
    } else {
        JiaocaiSearchScreen(
            login = login,
            onBookClick = { selectedBook = it },
            onBack = onBack
        )
    }
}

// ── 搜索页 ────────────────────────────────────────────────────────────

@Composable
private fun JiaocaiSearchScreen(
    login: JiaocaiLogin,
    onBookClick: (JiaocaiBook) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var keyword by remember { mutableStateOf("") }
    var books by remember { mutableStateOf<List<JiaocaiBook>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    fun doSearch() {
        if (keyword.isBlank()) return
        scope.launch {
            isLoading = true; errorMsg = null
            try {
                books = withContext(Dispatchers.IO) { JiaocaiApi(login).search(keyword) }
                hasSearched = true
            } catch (e: Exception) {
                errorMsg = "搜索失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "教材中心",
                color = MiuixTheme.colorScheme.surfaceVariant,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // 搜索框
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.xjtu.toolbox.ui.components.AppSearchBar(
                    query = keyword,
                    onQueryChange = { keyword = it },
                    label = "搜索教材（书名/作者/课程）",
                    onSearch = { doSearch() },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { doSearch() },
                    enabled = !isLoading && keyword.isNotBlank()
                ) {
                    Text(if (isLoading) "…" else "搜索")
                }
            }

            Spacer(Modifier.height(12.dp))

            when {
                isLoading -> Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                errorMsg != null -> Text(errorMsg!!, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 24.dp))
                hasSearched && books.isEmpty() -> {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MenuBook, null, Modifier.size(40.dp),
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            Spacer(Modifier.height(8.dp))
                            Text("未找到相关教材", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                    }
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(books) { book ->
                        BookCard(book = book, onClick = { onBookClick(book) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun BookCard(book: JiaocaiBook, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        cornerRadius = 12.dp,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(44.dp).background(
                    MiuixTheme.colorScheme.primary.copy(alpha = 0.12f),
                    RoundedCornerShape(8.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MenuBook, null, Modifier.size(24.dp),
                    tint = MiuixTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, style = MiuixTheme.textStyles.body2, fontWeight = FontWeight.Medium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (book.author.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(book.author, style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (book.summary.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(book.summary, style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                if (book.hasFullText) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.background(
                            MiuixTheme.colorScheme.primary.copy(alpha = 0.15f),
                            RoundedCornerShape(4.dp)
                        ).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("本地全文", style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// ── 详情页 ────────────────────────────────────────────────────────────

@Composable
private fun JiaocaiDetailScreen(
    login: JiaocaiLogin,
    book: JiaocaiBook,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var ssno by remember { mutableStateOf(book.ssno) }
    var isLoadingSsno by remember { mutableStateOf(ssno.isEmpty()) }
    var showWebView by remember { mutableStateOf(false) }

    // 下载状态
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }

    LaunchedEffect(book.id) {
        if (ssno.isEmpty()) {
            isLoadingSsno = true
            ssno = withContext(Dispatchers.IO) { JiaocaiApi(login).fetchSsno(book) }
            isLoadingSsno = false
        }
    }

    if (showWebView && ssno.isNotBlank()) {
        JiaocaiWebViewScreen(
            login = login,
            ssno = ssno,
            onBack = { showWebView = false }
        )
        return
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = book.title,
                color = MiuixTheme.colorScheme.surfaceVariant,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .overScrollVertical()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 封面占位
            Box(
                Modifier.fillMaxWidth().height(180.dp).background(
                    MiuixTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(12.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MenuBook, null, Modifier.size(64.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }

            Spacer(Modifier.height(16.dp))

            Text(book.title, style = MiuixTheme.textStyles.title4, fontWeight = FontWeight.Bold)
            if (book.author.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(book.author, style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            if (book.summary.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(book.summary, style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }

            Spacer(Modifier.height(24.dp))

            // 操作按钮
            if (isLoadingSsno) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("获取图书信息…", style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            } else if (ssno.isBlank()) {
                Text("暂无全文访问", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            } else {
                // 在线阅览按钮
                Button(
                    onClick = { showWebView = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Visibility, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("在线阅览")
                }

                Spacer(Modifier.height(10.dp))

                // 下载 PDF 按钮
                when (val state = downloadState) {
                    is DownloadState.Idle -> Button(
                        onClick = {
                            downloadState = DownloadState.Running(0, 0)
                            scope.launch {
                                downloadPdf(context, login, ssno) { cur, total ->
                                    downloadState = DownloadState.Running(cur, total)
                                }.let { result ->
                                    downloadState = if (result != null)
                                        DownloadState.Done(result)
                                    else
                                        DownloadState.Error("下载失败，请稍后重试")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("下载 PDF")
                    }

                    is DownloadState.Running -> {
                        Column(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (state.total > 0) "下载中 ${state.current}/${state.total} 页"
                                    else "准备下载…",
                                    style = MiuixTheme.textStyles.footnote1
                                )
                            }
                            if (state.total > 0) {
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = state.current.toFloat() / state.total,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    is DownloadState.Done -> {
                        Card(
                            cornerRadius = 10.dp,
                            colors = CardDefaults.defaultColors(
                                color = MiuixTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp),
                                    tint = MiuixTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("PDF 已保存", style = MiuixTheme.textStyles.footnote1,
                                        fontWeight = FontWeight.Medium)
                                    Text(state.path, style = MiuixTheme.textStyles.footnote2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { downloadState = DownloadState.Idle },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                color = MiuixTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text("重新下载")
                        }
                    }

                    is DownloadState.Error -> {
                        Text(state.msg, color = Color.Red, style = MiuixTheme.textStyles.footnote1)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { downloadState = DownloadState.Idle },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("重试") }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

// ── WebView 阅览 ──────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun JiaocaiWebViewScreen(
    login: JiaocaiLogin,
    ssno: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val readerUrl = JiaocaiApi(login).buildReaderUrl(ssno)

    // 将 OkHttp cookie 同步到 WebView CookieManager
    LaunchedEffect(Unit) {
        syncCookiesToWebView(login.client, listOf(
            JiaocaiLogin.BASE_URL,
            JiaocaiLogin.READER_HOST
        ))
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "在线阅览",
                color = MiuixTheme.colorScheme.surfaceVariant,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    webViewClient = WebViewClient()
                    loadUrl(readerUrl)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

// ── Cookie 同步工具 ───────────────────────────────────────────────────

private fun syncCookiesToWebView(client: OkHttpClient, urlStrings: List<String>) {
    val webCookieManager = CookieManager.getInstance()
    webCookieManager.setAcceptCookie(true)
    // 通过反射或内部字段获取 CookieJar 中的 cookie
    // OkHttp 使用 JavaNetCookieJar，底层是 java.net.CookieManager
    try {
        val field = client.javaClass.getDeclaredField("cookieJar")
        field.isAccessible = true
        val jar = field.get(client)
        val innerField = jar.javaClass.getDeclaredField("cookieHandler")
        innerField.isAccessible = true
        val handler = innerField.get(jar) as? java.net.CookieManager
        handler?.let { cm ->
            for (urlStr in urlStrings) {
                val uri = URI.create(urlStr)
                val cookies = cm.cookieStore.get(uri)
                cookies.forEach { cookie ->
                    val cookieStr = "${cookie.name}=${cookie.value}"
                    webCookieManager.setCookie(urlStr, cookieStr)
                }
            }
        }
    } catch (e: Exception) {
        Log.w("JiaocaiScreen", "syncCookiesToWebView failed via reflection: ${e.message}")
    }
    webCookieManager.flush()
}

// ── PDF 下载逻辑 ──────────────────────────────────────────────────────

private sealed class DownloadState {
    object Idle : DownloadState()
    data class Running(val current: Int, val total: Int) : DownloadState()
    data class Done(val path: String) : DownloadState()
    data class Error(val msg: String) : DownloadState()
}

private suspend fun downloadPdf(
    context: Context,
    login: JiaocaiLogin,
    ssno: String,
    onProgress: (current: Int, total: Int) -> Unit
): String? = withContext(Dispatchers.IO) {
    try {
        val api = JiaocaiApi(login)
        onProgress(0, 0)

        // 1. 获取 reader 参数（加密路径和 session hash）
        val params = api.fetchReaderParams(ssno) ?: run {
            Log.e(TAG, "downloadPdf: fetchReaderParams returned null")
            return@withContext null
        }
        Log.d(TAG, "downloadPdf: params=$params")

        // 2. 下载前置页
        val specialPages = listOf("cov001.jpg", "bok001.jpg", "!00001.jpg",
            "fow001.jpg", "fow002.jpg", "fow003.jpg",
            "fow004.jpg", "fow005.jpg", "fow006.jpg", "fow007.jpg", "fow008.jpg")

        val tmpDir = File(context.cacheDir, "jiaocai_$ssno").also { it.mkdirs() }
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Linux; Android 14; XJTUToolbox)",
            "Referer" to "${JiaocaiLogin.READER_HOST}/jpath/reader/reader.shtml"
        )

        val downloadedFiles = mutableListOf<File>()

        fun downloadPage(url: String, name: String): File? {
            return try {
                val req = Request.Builder().url(url).apply {
                    headers.forEach { (k, v) -> header(k, v) }
                }.get().build()
                val resp = login.client.newCall(req).execute()
                val bytes = resp.body?.use { it.bytes() } ?: return null
                if (bytes.size < 500 || bytes.take(2) != listOf(0xFF.toByte(), 0xD8.toByte())) return null
                val file = File(tmpDir, name)
                file.writeBytes(bytes)
                file
            } catch (e: Exception) { null }
        }

        // 下载特殊页
        for (page in specialPages) {
            val url = api.buildSpecialPageUrl(params, page)
            downloadPage(url, page)?.let { downloadedFiles.add(it) }
        }

        // 3. 遍历正文页直到 404
        var pageNum = 1
        var failStreak = 0
        while (failStreak < 3 && isActive) {
            onProgress(pageNum - 1, -1)
            val url = api.buildPageImageUrl(params, pageNum)
            val file = downloadPage(url, "%06d.jpg".format(pageNum))
            if (file != null) {
                downloadedFiles.add(file)
                failStreak = 0
            } else {
                failStreak++
            }
            pageNum++
        }

        val mainCount = pageNum - 1 - failStreak
        onProgress(mainCount, mainCount)

        if (downloadedFiles.isEmpty()) return@withContext null

        // 4. 合并为 PDF（使用 Android 原生 Canvas，无需第三方库）
        val pdfPath = File(context.getExternalFilesDir(null), "${ssno}_${System.currentTimeMillis()}.pdf")
        mergeToPdf(downloadedFiles, pdfPath)

        // 清理临时文件
        tmpDir.deleteRecursively()

        pdfPath.absolutePath
    } catch (e: Exception) {
        Log.e(TAG, "downloadPdf failed", e)
        null
    }
}

private fun mergeToPdf(imageFiles: List<File>, outputFile: File) {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    for ((index, imgFile) in imageFiles.withIndex()) {
        try {
            val bmp = android.graphics.BitmapFactory.decodeFile(imgFile.absolutePath) ?: continue
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                bmp.width, bmp.height, index + 1
            ).create()
            val page = pdfDocument.startPage(pageInfo)
            page.canvas.drawBitmap(bmp, 0f, 0f, null)
            pdfDocument.finishPage(page)
            bmp.recycle()
        } catch (e: Exception) {
            Log.w(TAG, "mergeToPdf: skip ${imgFile.name}: ${e.message}")
        }
    }
    outputFile.outputStream().use { pdfDocument.writeTo(it) }
    pdfDocument.close()
}
