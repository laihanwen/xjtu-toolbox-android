package com.xjtu.toolbox.ui.settings

import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.Tab
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Carrier
import top.yukonga.miuix.kmp.icon.extended.CloudFill
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Forward
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xjtu.toolbox.BuildConfig
import com.xjtu.toolbox.util.CredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import java.io.File

@Composable
fun SettingsScreen(
    credentialStore: CredentialStore,
    onBack: () -> Unit,
    onNavBarStyleChanged: (String) -> Unit = {},
    onDarkModeChanged: (String) -> Unit = {},
    onDefaultTabChanged: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var navBarStyle by remember { mutableStateOf(credentialStore.navBarStyle) }
    var darkMode by remember { mutableStateOf(credentialStore.darkMode) }
    var defaultTab by remember { mutableStateOf(credentialStore.defaultTab) }
    var networkMode by remember { mutableStateOf(credentialStore.networkMode) }
    var autoCheckUpdate by remember { mutableStateOf(credentialStore.autoCheckUpdate) }
    var updateChannel by remember { mutableStateOf(credentialStore.updateChannel) }
    var cacheSizeText by remember { mutableStateOf("计算中...") }
    var showChangelog by remember { mutableStateOf(false) }
    var showEula by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            cacheSizeText = runCatching {
                context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }.map(::formatFileSize).getOrDefault("无法获取")
        }
    }

    val versionText = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    val lmsDownloadDir = remember {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
    }
    val replayDownloadDir = remember {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "ClassReplay"
        ).absolutePath
    }

    // ── 选项数据 ──
    val darkModeOptions = listOf("跟随系统", "始终浅色", "始终深色")
    val darkModeValues = listOf(
        CredentialStore.DARK_MODE_SYSTEM,
        CredentialStore.DARK_MODE_LIGHT,
        CredentialStore.DARK_MODE_DARK
    )
    val navStyleOptions = listOf("悬浮胶囊", "经典底栏")
    val navStyleValues = listOf(
        CredentialStore.NAV_STYLE_FLOATING,
        CredentialStore.NAV_STYLE_CLASSIC
    )
    val tabOptions = listOf("首页", "日程", "工具", "我的")
    val tabValues = listOf(
        CredentialStore.TAB_HOME,
        CredentialStore.TAB_COURSES,
        CredentialStore.TAB_TOOLS,
        CredentialStore.TAB_PROFILE
    )
    val networkOptions = listOf("自动检测", "强制直连", "强制 WebVPN")
    val networkValues = listOf(
        CredentialStore.NETWORK_AUTO,
        CredentialStore.NETWORK_DIRECT,
        CredentialStore.NETWORK_VPN
    )
    val channelOptions = listOf("稳定版", "测试版")
    val channelValues = listOf(
        CredentialStore.CHANNEL_STABLE,
        CredentialStore.CHANNEL_BETA
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = "设置",
                largeTitle = "设置",
                color = MiuixTheme.colorScheme.background,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .overScrollVertical()
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            // ── 外观 ──
            SmallTitle("外观")
            SettingsCard {
                OverlayDropdownPreference(
                    title = "深色模式",
                    items = darkModeOptions,
                    selectedIndex = darkModeValues.indexOf(darkMode).coerceAtLeast(0),
                    startAction = { SettingsIcon(MiuixIcons.Theme, MiuixTheme.colorScheme.primary) },
                    onSelectedIndexChange = { idx ->
                        val v = darkModeValues[idx]
                        darkMode = v
                        credentialStore.darkMode = v
                        onDarkModeChanged(v)
                    }
                )
                OverlayDropdownPreference(
                    title = "底栏风格",
                    items = navStyleOptions,
                    selectedIndex = navStyleValues.indexOf(navBarStyle).coerceAtLeast(0),
                    startAction = { SettingsIcon(MiuixIcons.Carrier, MiuixTheme.colorScheme.primaryVariant) },
                    onSelectedIndexChange = { idx ->
                        val v = navStyleValues[idx]
                        navBarStyle = v
                        credentialStore.navBarStyle = v
                        onNavBarStyleChanged(v)
                    }
                )
                OverlayDropdownPreference(
                    title = "默认启动 Tab",
                    items = tabOptions,
                    selectedIndex = tabValues.indexOf(defaultTab).coerceAtLeast(0),
                    startAction = { SettingsIcon(Icons.Default.Tab, MiuixTheme.colorScheme.secondary) },
                    onSelectedIndexChange = { idx ->
                        val v = tabValues[idx]
                        defaultTab = v
                        credentialStore.defaultTab = v
                        onDefaultTabChanged(v)
                    }
                )
            }

            // ── 网络 ──
            SmallTitle("网络")
            SettingsCard {
                OverlayDropdownPreference(
                    title = "连接模式",
                    items = networkOptions,
                    selectedIndex = networkValues.indexOf(networkMode).coerceAtLeast(0),
                    startAction = { SettingsIcon(MiuixIcons.Carrier, MiuixTheme.colorScheme.primary) },
                    onSelectedIndexChange = { idx ->
                        val v = networkValues[idx]
                        networkMode = v
                        credentialStore.networkMode = v
                    }
                )
            }

            // ── 数据 ──
            SmallTitle("数据")
            SettingsCard {
                ArrowPreference(
                    title = "缓存大小",
                    summary = cacheSizeText,
                    startAction = { SettingsIcon(MiuixIcons.CloudFill, MiuixTheme.colorScheme.primaryVariant) }
                )
                ArrowPreference(
                    title = "清除缓存",
                    summary = "清除临时文件和图片缓存，不影响登录与下载文件",
                    startAction = { SettingsIcon(MiuixIcons.Delete, MiuixTheme.colorScheme.error) },
                    onClick = { showClearCacheDialog = true }
                )
                ArrowPreference(
                    title = "LMS 下载位置",
                    summary = lmsDownloadDir,
                    startAction = { SettingsIcon(MiuixIcons.Folder, MiuixTheme.colorScheme.primaryVariant) }
                )
                ArrowPreference(
                    title = "课堂回放下载位置",
                    summary = replayDownloadDir,
                    startAction = { SettingsIcon(MiuixIcons.Folder, MiuixTheme.colorScheme.secondary) }
                )
            }

            // ── 更新 ──
            SmallTitle("更新")
            SettingsCard {
                SwitchPreference(
                    title = "启动时检查更新",
                    summary = "打开 App 时自动检查新版本",
                    checked = autoCheckUpdate,
                    onCheckedChange = {
                        autoCheckUpdate = it
                        credentialStore.autoCheckUpdate = it
                    },
                    startAction = { SettingsIcon(MiuixIcons.Update, MiuixTheme.colorScheme.primaryVariant) }
                )
                OverlayDropdownPreference(
                    title = "更新渠道",
                    items = channelOptions,
                    selectedIndex = channelValues.indexOf(updateChannel).coerceAtLeast(0),
                    startAction = { SettingsIcon(MiuixIcons.Settings, MiuixTheme.colorScheme.secondary) },
                    onSelectedIndexChange = { idx ->
                        val v = channelValues[idx]
                        updateChannel = v
                        credentialStore.updateChannel = v
                    }
                )
            }

            // ── 关于 ──
            SmallTitle("关于")
            SettingsCard {
                ArrowPreference(
                    title = "版本号",
                    summary = versionText,
                    startAction = { SettingsIcon(MiuixIcons.Info, MiuixTheme.colorScheme.primary) }
                )
                ArrowPreference(
                    title = "更新日志",
                    summary = "查看历史版本变化",
                    startAction = { SettingsIcon(MiuixIcons.Recent, MiuixTheme.colorScheme.primaryVariant) },
                    onClick = { showChangelog = true }
                )
                ArrowPreference(
                    title = "项目主页",
                    summary = "GitHub · yeliqin666/xjtu-toolbox-android",
                    startAction = { SettingsIcon(MiuixIcons.Forward, MiuixTheme.colorScheme.secondary) },
                    onClick = { uriHandler.openUri("https://github.com/yeliqin666/xjtu-toolbox-android") }
                )
                ArrowPreference(
                    title = "反馈建议",
                    summary = "提交 GitHub Issue",
                    startAction = { SettingsIcon(MiuixIcons.Report, MiuixTheme.colorScheme.primaryVariant) },
                    onClick = { uriHandler.openUri("https://github.com/yeliqin666/xjtu-toolbox-android/issues") }
                )
                ArrowPreference(
                    title = "用户协议与隐私政策",
                    startAction = { SettingsIcon(MiuixIcons.File, MiuixTheme.colorScheme.onSurfaceVariantSummary) },
                    onClick = { showEula = true }
                )
            }

            // ── 致谢 ──
            SmallTitle("致谢")
            SettingsCard {
                ArrowPreference(
                    title = "XJTUToolBox by yan-xiaoo",
                    summary = "初代工具箱项目",
                    startAction = { SettingsIcon(MiuixIcons.Info, MiuixTheme.colorScheme.primary) },
                    onClick = { uriHandler.openUri("https://github.com/yan-xiaoo/XJTUToolBox") }
                )
            }

            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }

    if (showClearCacheDialog) {
        OverlayDialog(
            show = showClearCacheDialog,
            title = "清除缓存",
            summary = "将清除约 $cacheSizeText 的临时缓存，不会影响登录状态和下载文件。",
            onDismissRequest = { showClearCacheDialog = false }
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    text = "取消",
                    onClick = { showClearCacheDialog = false },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        showClearCacheDialog = false
                        scope.launch(Dispatchers.IO) {
                            val cleared = runCatching {
                                context.cacheDir.deleteRecursively()
                                context.cacheDir.mkdirs()
                            }.isSuccess
                            withContext(Dispatchers.Main) {
                                if (cleared) {
                                    cacheSizeText = "0 B"
                                    Toast.makeText(context, "缓存已清除", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "清除失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("确认清除")
                }
            }
        }
    }

    ChangelogSheet(
        show = showChangelog,
        onDismiss = { showChangelog = false }
    )

    EulaSheet(
        show = showEula,
        onDismiss = { showEula = false }
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        cornerRadius = 16.dp,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = color)
        }
    }
    Spacer(Modifier.width(12.dp))
}

@Composable
private fun ChangelogSheet(show: Boolean, onDismiss: () -> Unit) {
    BackHandler(enabled = show) { onDismiss() }
    OverlayBottomSheet(
        show = show,
        title = "更新日志",
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            changelogItems().forEach { entry ->
                Text(
                    text = entry.version,
                    style = MiuixTheme.textStyles.subtitle,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                entry.items.forEach { item ->
                    Text(
                        text = "· $item",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("知道了")
            }
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun EulaSheet(show: Boolean, onDismiss: () -> Unit) {
    BackHandler(enabled = show) { onDismiss() }
    OverlayBottomSheet(
        show = show,
        title = "用户协议与隐私政策",
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text("用户协议", style = MiuixTheme.textStyles.subtitle, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "本应用仅用于个人学习和校园信息查询。请遵守西安交通大学信息系统使用规定，不要使用本应用进行任何违规操作。",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Spacer(Modifier.height(16.dp))
            Text("隐私政策", style = MiuixTheme.textStyles.subtitle, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "登录凭据仅在本地加密存储。应用直接访问学校官方系统，不会向第三方服务器上传密码或个人数据。",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("知道了")
            }
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
    else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
}

private data class ChangelogEntry(
    val version: String,
    val items: List<String>
)

private fun changelogItems(): List<ChangelogEntry> = listOf(
    ChangelogEntry(
        version = "v${BuildConfig.VERSION_NAME}",
        items = listOf(
            "新增电子加餐券查询与自动登录",
            "首页和工具页新增加餐券入口",
            "修复设置页文案显示和长路径布局问题",
            "改进课表缓存读取、节假日过滤与小组件稳定性"
        )
    ),
    ChangelogEntry(
        version = "v3.2.0",
        items = listOf(
            "新增假期日历与日程体验优化",
            "完善登录和 WebVPN 相关流程"
        )
    ),
    ChangelogEntry(
        version = "v3.1.0",
        items = listOf(
            "新增电子教材中心、NeoSchool 和校园卡新平台支持",
            "优化课程、资源与下载相关体验"
        )
    )
)
