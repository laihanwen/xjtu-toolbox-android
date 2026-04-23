package com.xjtu.toolbox.neo

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun NeoScreen(
    session: NeoSession,
    onBack: () -> Unit
) {
    var currentPage by remember { mutableStateOf(if (session.isLoggedIn) NeoPage.COURSE_LIST else NeoPage.LOGIN) }
    var selectedCourse by remember { mutableStateOf<NeoCourse?>(null) }

    when (currentPage) {
        NeoPage.LOGIN -> NeoLoginPage(
            session = session,
            onLoginSuccess = { currentPage = NeoPage.COURSE_LIST },
            onBack = onBack
        )
        NeoPage.COURSE_LIST -> NeoCourseListPage(
            session = session,
            onCourseClick = { course ->
                selectedCourse = course
                currentPage = NeoPage.COURSE_DETAIL
            },
            onLogout = { currentPage = NeoPage.LOGIN },
            onBack = onBack
        )
        NeoPage.COURSE_DETAIL -> {
            val course = selectedCourse
            if (course != null) {
                NeoCourseDetailPage(
                    session = session,
                    course = course,
                    onBack = { currentPage = NeoPage.COURSE_LIST }
                )
            } else {
                currentPage = NeoPage.COURSE_LIST
            }
        }
    }
}

private enum class NeoPage { LOGIN, COURSE_LIST, COURSE_DETAIL }

// ════════════════════════════════════════
//  登录页
// ════════════════════════════════════════

@Composable
private fun NeoLoginPage(
    session: NeoSession,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var account by remember { mutableStateOf(session.savedAccount) }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            TopAppBar(
                title = "NeoSchool 拔尖课程",
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .overScrollVertical()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Icon(
                Icons.Default.School,
                null,
                Modifier.size(64.dp),
                tint = MiuixTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "拔尖计划课程平台",
                style = MiuixTheme.textStyles.title1,
                fontWeight = FontWeight.Bold
            )
            Text(
                "使用 NeoSchool 账号登录",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Spacer(Modifier.height(32.dp))

            TextField(
                value = account,
                onValueChange = { account = it; error = null },
                label = "账号（手机号）",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            TextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = "密码",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    error!!,
                    color = MiuixTheme.colorScheme.error,
                    style = MiuixTheme.textStyles.body2
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (account.isBlank() || password.isBlank()) {
                        error = "请填写账号和密码"
                        return@Button
                    }
                    isLoading = true
                    error = null
                    scope.launch {
                        val (ok, msg) = withContext(Dispatchers.IO) { session.login(account, password) }
                        isLoading = false
                        if (ok) onLoginSuccess() else error = msg
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(size = 18.dp, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isLoading) "登录中..." else "登录")
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

// ════════════════════════════════════════
//  课程列表页
// ════════════════════════════════════════

@Composable
private fun NeoCourseListPage(
    session: NeoSession,
    onCourseClick: (NeoCourse) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var courses by remember { mutableStateOf<List<NeoCourse>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        withContext(Dispatchers.IO) {
            try {
                val api = NeoApi(session)
                courses = api.getMyCourseList()
            } catch (e: Exception) {
                error = "加载失败：${e.message}"
            }
        }
        isLoading = false
    }

    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            TopAppBar(
                title = "拔尖课程",
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    try { courses = NeoApi(session).getMyCourseList() }
                                    catch (e: Exception) { error = e.message }
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .overScrollVertical()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            when {
                isLoading -> {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Spacer(Modifier.height(32.dp))
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, null, Modifier.size(48.dp), tint = MiuixTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Text(error!!, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            isLoading = true; error = null
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    try { courses = NeoApi(session).getMyCourseList() }
                                    catch (e: Exception) { error = e.message }
                                }
                                isLoading = false
                            }
                        }) { Text("重试") }
                    }
                }
                courses != null && courses!!.isEmpty() -> {
                    Spacer(Modifier.height(32.dp))
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.School, null, Modifier.size(40.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        Spacer(Modifier.height(12.dp))
                        Text("暂无课程", style = MiuixTheme.textStyles.body1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
                courses != null -> {
                    Text(
                        "我的课程（${courses!!.size}）",
                        style = MiuixTheme.textStyles.headline1,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                    courses!!.forEach { course ->
                        NeoCourseCard(course = course, onClick = { onCourseClick(course) })
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(
                text = "退出 NeoSchool 登录",
                onClick = { session.clearSession(); onLogout() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun NeoCourseCard(course: NeoCourse, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.School, null,
                    Modifier.size(18.dp),
                    tint = MiuixTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    course.courseName,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                    Modifier.size(18.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.5f)
                )
            }
            if (course.professorName.isNotBlank() || course.majorsName.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                val subtitle = listOfNotNull(
                    course.professorName.takeIf { it.isNotBlank() },
                    course.majorsName.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                Text(subtitle, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            if (course.courseBeginTime.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "开始：${course.courseBeginTime}",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    }
}

// ════════════════════════════════════════
//  课程详情页（公告 + 资源）
// ════════════════════════════════════════

@Composable
private fun NeoCourseDetailPage(
    session: NeoSession,
    course: NeoCourse,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("日程", "公告", "资源")

    var chapters by remember { mutableStateOf<List<NeoCourseChapter>?>(null) }
    var announcements by remember { mutableStateOf<List<NeoAnnouncement>?>(null) }
    var resources by remember { mutableStateOf<List<NeoResource>?>(null) }
    var isLoadingChap by remember { mutableStateOf(false) }
    var isLoadingAnn by remember { mutableStateOf(false) }
    var isLoadingRes by remember { mutableStateOf(false) }

    LaunchedEffect(course.courseId) {
        isLoadingChap = true
        withContext(Dispatchers.IO) {
            try { chapters = NeoApi(session).getCourseChapters(course.courseId) }
            catch (_: Exception) { chapters = emptyList() }
        }
        isLoadingChap = false
    }

    LaunchedEffect(course.courseId) {
        isLoadingAnn = true
        withContext(Dispatchers.IO) {
            try { announcements = NeoApi(session).getCourseAnnouncements(course.courseId) }
            catch (_: Exception) { announcements = emptyList() }
        }
        isLoadingAnn = false
    }

    LaunchedEffect(course.courseId) {
        isLoadingRes = true
        withContext(Dispatchers.IO) {
            try { resources = NeoApi(session).getCourseResources(course.courseId) }
            catch (_: Exception) { resources = emptyList() }
        }
        isLoadingRes = false
    }

    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            TopAppBar(
                title = course.courseName,
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Surface(modifier = Modifier.fillMaxWidth(), color = MiuixTheme.colorScheme.surfaceVariant) {
                TabRowWithContour(
                    tabs = tabs,
                    selectedTabIndex = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .overScrollVertical()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                when (selectedTab) {
                    0 -> ChaptersContent(chapters, isLoadingChap, session, course.courseId)
                    1 -> AnnouncementsContent(announcements, isLoadingAnn)
                    2 -> ResourcesContent(resources, isLoadingRes)
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun ChaptersContent(
    chapters: List<NeoCourseChapter>?,
    isLoading: Boolean,
    session: NeoSession,
    courseId: String
) {
    var expandedId by remember { mutableStateOf<String?>(null) }
    val contentsCache = remember { mutableStateMapOf<String, List<NeoChapterContent>>() }
    val loadingSet = remember { mutableStateMapOf<String, Boolean>() }
    val context = LocalContext.current

    when {
        isLoading -> Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        chapters.isNullOrEmpty() -> {
            Spacer(Modifier.height(32.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.DateRange, null, Modifier.size(40.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Spacer(Modifier.height(8.dp))
                Text("暂无日程", style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
        else -> chapters!!.forEach { chapter ->
            val isExpanded = expandedId == chapter.chapterId
            val contents = contentsCache[chapter.chapterId]
            val isLoadingContents = loadingSet[chapter.chapterId] == true

            if (isExpanded && contents == null && !isLoadingContents) {
                LaunchedEffect(chapter.chapterId) {
                    loadingSet[chapter.chapterId] = true
                    withContext(Dispatchers.IO) {
                        try {
                            contentsCache[chapter.chapterId] =
                                NeoApi(session).getChapterContents(courseId, chapter.chapterId)
                        } catch (_: Exception) {
                            contentsCache[chapter.chapterId] = emptyList()
                        }
                    }
                    loadingSet[chapter.chapterId] = false
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 12.dp,
                colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { expandedId = if (isExpanded) null else chapter.chapterId }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    MiuixTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "W${chapter.week}",
                                style = MiuixTheme.textStyles.footnote2,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(chapter.chapterName, style = MiuixTheme.textStyles.body2, fontWeight = FontWeight.Medium)
                            val dateLine = buildString {
                                if (chapter.planStartTime.isNotBlank()) {
                                    append(chapter.planStartTime.take(16))
                                    if (chapter.planEndTime.isNotBlank()) {
                                        append(" – ${chapter.planEndTime.takeLast(5)}")
                                    }
                                }
                                if (chapter.contentNum > 0) {
                                    if (isNotEmpty()) append("  ")
                                    append("${chapter.contentNum} 个内容")
                                }
                            }
                            if (dateLine.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(dateLine, style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            }
                        }
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            null, Modifier.size(20.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }

                    if (isExpanded) {
                        Column(Modifier.padding(start = 62.dp, end = 14.dp, bottom = 12.dp)) {
                                when {
                                    isLoadingContents -> CircularProgressIndicator(Modifier.size(20.dp).padding(bottom = 8.dp))
                                    contents.isNullOrEmpty() -> Text("暂无资源文件", style = MiuixTheme.textStyles.footnote1,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        modifier = Modifier.padding(bottom = 4.dp))
                                    else -> contents.forEach { pdf ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val intent = Intent(Intent.ACTION_VIEW,
                                                        android.net.Uri.parse("https://www.neoschool.com"))
                                                    context.startActivity(intent)
                                                }
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Description, null, Modifier.size(16.dp),
                                                tint = MiuixTheme.colorScheme.primary)
                                            Spacer(Modifier.width(8.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(pdf.resourceName, style = MiuixTheme.textStyles.footnote1,
                                                    fontWeight = FontWeight.Medium)
                                                val meta = buildString {
                                                    if (pdf.fileType.isNotBlank()) append(pdf.fileType.uppercase())
                                                    if (pdf.sizeBytes > 0) {
                                                        if (isNotEmpty()) append("  ")
                                                        val mb = pdf.sizeBytes / 1024f / 1024f
                                                        append("%.1f MB".format(mb))
                                                    }
                                                    if (pdf.pageNum > 0) {
                                                        if (isNotEmpty()) append("  ")
                                                        append("${pdf.pageNum}页")
                                                    }
                                                }
                                                if (meta.isNotBlank()) Text(meta,
                                                    style = MiuixTheme.textStyles.footnote2,
                                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                            }
                                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(14.dp),
                                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.6f))
                                        }
                                    }
                                }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AnnouncementsContent(announcements: List<NeoAnnouncement>?, isLoading: Boolean) {
    when {
        isLoading -> Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        announcements.isNullOrEmpty() -> {
            Spacer(Modifier.height(32.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Notifications, null, Modifier.size(40.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Spacer(Modifier.height(8.dp))
                Text("暂无公告", style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
        else -> announcements!!.forEach { ann ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 12.dp,
                colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(ann.title, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold)
                    if (ann.publishTime.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(ann.publishTime, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    if (ann.content.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            ann.content.stripHtml(),
                            style = MiuixTheme.textStyles.body2,
                            maxLines = 5,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ResourcesContent(resources: List<NeoResource>?, isLoading: Boolean) {
    val context = LocalContext.current
    when {
        isLoading -> Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        resources.isNullOrEmpty() -> {
            Spacer(Modifier.height(32.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Folder, null, Modifier.size(40.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Spacer(Modifier.height(8.dp))
                Text("暂无资源", style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
        else -> resources!!.forEach { res ->
            Card(
                onClick = { openResourceInBrowser(context, res.downloadUrl) },
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 12.dp,
                colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.InsertDriveFile, null,
                        Modifier.size(22.dp),
                        tint = MiuixTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            res.resourceName,
                            style = MiuixTheme.textStyles.body2,
                            fontWeight = FontWeight.Medium
                        )
                        val meta = buildString {
                            if (res.fileType.isNotBlank()) append(res.fileType.uppercase())
                            if (res.fileSize > 0L) { if (isNotEmpty()) append("  "); append(formatFileSize(res.fileSize)) }
                        }
                        if (meta.isNotBlank()) {
                            Text(meta, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                    }
                    Icon(
                        Icons.Default.Download, null,
                        Modifier.size(18.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Suppress("DEPRECATION")
private fun String.stripHtml(): String {
    val spanned = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
        android.text.Html.fromHtml(this, android.text.Html.FROM_HTML_MODE_COMPACT)
    else android.text.Html.fromHtml(this)
    return spanned.toString().trim().replace(Regex("\\n{3,}"), "\n\n")
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

private fun openResourceInBrowser(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}
