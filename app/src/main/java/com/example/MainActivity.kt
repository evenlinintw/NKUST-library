package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.SslErrorHandler
import android.net.http.SslError
import android.widget.Toast
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NkustAmber
import com.example.ui.theme.NkustNavy
import com.example.ui.theme.NkustTeal

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                LibraryAppMainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryAppMainScreen() {
    val context = LocalContext.current
    val viewModel: LibraryViewModel = viewModel()
    
    // Bottom navigation tabs matching screenshot: 首頁, 預約, 個人
    var selectedTab by remember { mutableStateOf(0) }
    var profileSubTab by remember { mutableStateOf("card") } // "card" or "service"
    
    // Global user login state
    val studentProfile by viewModel.studentProfile.collectAsStateWithLifecycle()
    
    // Separate persistent loader state variables for each WebView (tabs 0, 1, 2, and 3)
    var webViewRef0 by remember { mutableStateOf<WebView?>(null) }
    var webViewRef1 by remember { mutableStateOf<WebView?>(null) }
    var webViewRef2 by remember { mutableStateOf<WebView?>(null) }
    var webViewRef3 by remember { mutableStateOf<WebView?>(null) }

    var web0CanGoBack by remember { mutableStateOf(false) }
    var web1CanGoBack by remember { mutableStateOf(false) }
    var web2CanGoBack by remember { mutableStateOf(false) }
    var web3CanGoBack by remember { mutableStateOf(false) }

    var web0CanGoForward by remember { mutableStateOf(false) }
    var web1CanGoForward by remember { mutableStateOf(false) }
    var web2CanGoForward by remember { mutableStateOf(false) }
    var web3CanGoForward by remember { mutableStateOf(false) }

    var web0IsLoading by remember { mutableStateOf(false) }
    var web1IsLoading by remember { mutableStateOf(false) }
    var web2IsLoading by remember { mutableStateOf(false) }
    var web3IsLoading by remember { mutableStateOf(false) }

    var web0Progress by remember { mutableStateOf(0f) }
    var web1Progress by remember { mutableStateOf(0f) }
    var web2Progress by remember { mutableStateOf(0f) }
    var web3Progress by remember { mutableStateOf(0f) }

    // Active web back states handler
    val activeWebView = when (selectedTab) {
        0 -> webViewRef0
        1 -> webViewRef1
        2 -> if (profileSubTab == "card") webViewRef2 else webViewRef3
        else -> null
    }
    val activeCanGoBack = when (selectedTab) {
        0 -> web0CanGoBack
        1 -> web1CanGoBack
        2 -> if (profileSubTab == "card") web2CanGoBack else web3CanGoBack
        else -> false
    }
    val activeCanGoForward = when (selectedTab) {
        0 -> web0CanGoForward
        1 -> web1CanGoForward
        2 -> if (profileSubTab == "card") web2CanGoForward else web3CanGoForward
        else -> false
    }

    BackHandler(enabled = activeWebView != null && activeCanGoBack) {
        activeWebView?.goBack()
    }

    // Navigation bar background matching the screenshot (very dark navy/black slate, active in turquoise cyan, inactive in gray-blue)
    val bottomBarBg = Color(0xFF0F1E36)
    val activeTabColor = Color(0xFF00FFD1) // Cyan-Turquoise matching photo exactly
    val inactiveTabColor = Color(0xFF90A4AE)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "高科大圖書館",
                            tint = NkustAmber,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "高科大圖書館",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "NKUST Library Portal",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Light
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NkustNavy,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    if (studentProfile != null) {
                        // User badge
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NkustAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = studentProfile?.name ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = NkustAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    
                    // 1. Back Arrow Button
                    IconButton(
                        onClick = { activeWebView?.goBack() },
                        enabled = activeCanGoBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "上一頁",
                            tint = if (activeCanGoBack) Color.White else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 2. Forward Arrow Button
                    IconButton(
                        onClick = { activeWebView?.goForward() },
                        enabled = activeCanGoForward,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "下一頁",
                            tint = if (activeCanGoForward) Color.White else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 3. Refresh Button
                    IconButton(
                        onClick = {
                            if (activeWebView != null) {
                                activeWebView.reload()
                                Toast.makeText(context, "正在重新整理頁面...", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "沒有可重新整理的網頁", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "重新整理",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 4. Open in External Browser Button
                    IconButton(
                        onClick = {
                            try {
                                val currentUrl = activeWebView?.url ?: when (selectedTab) {
                                    0 -> "https://www.lib.nkust.edu.tw/portal/"
                                    1 -> "https://space.lib.nkust.edu.tw/"
                                    2 -> if (profileSubTab == "card") "https://www.lib.nkust.edu.tw/portal/portal_login.php" else "https://www.lib.nkust.edu.tw/portal/portal_people.php?id=8"
                                    else -> "https://www.lib.nkust.edu.tw/portal/"
                                }
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "無法以系統瀏覽器開啟", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "外部瀏覽器開啟",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Replicating bottom toolbar EXACTLY as shown in image
            Surface(
                color = bottomBarBg,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomNavItem(
                        label = "首頁",
                        icon = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                        isSelected = selectedTab == 0,
                        activeColor = activeTabColor,
                        inactiveColor = inactiveTabColor,
                        onClick = { selectedTab = 0 },
                        tagSuffix = "home"
                    )
                    bottomNavItem(
                        label = "空間預約",
                        icon = if (selectedTab == 1) Icons.Filled.DateRange else Icons.Outlined.DateRange,
                        isSelected = selectedTab == 1,
                        activeColor = activeTabColor,
                        inactiveColor = inactiveTabColor,
                        onClick = { selectedTab = 1 },
                        tagSuffix = "booking"
                    )
                    bottomNavItem(
                        label = "個人",
                        icon = if (selectedTab == 2) Icons.Filled.Person else Icons.Outlined.Person,
                        isSelected = selectedTab == 2,
                        activeColor = activeTabColor,
                        inactiveColor = inactiveTabColor,
                        onClick = { selectedTab = 2 },
                        tagSuffix = "me"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Conditional switcher for active Tab to ensure perfect touch events
            when (selectedTab) {
                0 -> {
                    TabWebViewContainer(
                        url = "https://www.lib.nkust.edu.tw/portal/",
                        onWebViewCreated = { webViewRef0 = it },
                        canGoBackChanged = { web0CanGoBack = it },
                        canGoForwardChanged = { web0CanGoForward = it },
                        isLoadingChanged = { web0IsLoading = it },
                        progressChanged = { web0Progress = it },
                        webViewInstance = webViewRef0,
                        canGoBack = web0CanGoBack,
                        isLoading = web0IsLoading,
                        progress = web0Progress
                    )
                }
                1 -> {
                    TabWebViewContainer(
                        url = "https://space.lib.nkust.edu.tw/",
                        onWebViewCreated = { webViewRef1 = it },
                        canGoBackChanged = { web1CanGoBack = it },
                        canGoForwardChanged = { web1CanGoForward = it },
                        isLoadingChanged = { web1IsLoading = it },
                        progressChanged = { web1Progress = it },
                        webViewInstance = webViewRef1,
                        canGoBack = web1CanGoBack,
                        isLoading = web1IsLoading,
                        progress = web1Progress
                    )
                }
                2 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Elevated tab selector for Tab 2 (個人)
                        Surface(
                            color = NkustNavy,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E2D4A))
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (profileSubTab == "card") NkustTeal else Color.Transparent)
                                        .clickable { profileSubTab = "card" }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Badge,
                                            contentDescription = null,
                                            tint = if (profileSubTab == "card") Color.White else Color.LightGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "行動借閱證",
                                            color = if (profileSubTab == "card") Color.White else Color.LightGray,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (profileSubTab == "service") NkustTeal else Color.Transparent)
                                        .clickable { profileSubTab = "service" }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Book,
                                            contentDescription = null,
                                            tint = if (profileSubTab == "service") Color.White else Color.LightGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "讀者服務",
                                            color = if (profileSubTab == "service") Color.White else Color.LightGray,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }

                        if (profileSubTab == "card") {
                            TabWebViewContainer(
                                url = "https://www.lib.nkust.edu.tw/portal/portal_login.php",
                                onWebViewCreated = { webViewRef2 = it },
                                canGoBackChanged = { web2CanGoBack = it },
                                canGoForwardChanged = { web2CanGoForward = it },
                                isLoadingChanged = { web2IsLoading = it },
                                progressChanged = { web2Progress = it },
                                webViewInstance = webViewRef2,
                                canGoBack = web2CanGoBack,
                                isLoading = web2IsLoading,
                                progress = web2Progress
                            )
                        } else {
                            TabWebViewContainer(
                                url = "https://www.lib.nkust.edu.tw/portal/portal_people.php?id=8",
                                onWebViewCreated = { webViewRef3 = it },
                                canGoBackChanged = { web3CanGoBack = it },
                                canGoForwardChanged = { web3CanGoForward = it },
                                isLoadingChanged = { web3IsLoading = it },
                                progressChanged = { web3Progress = it },
                                webViewInstance = webViewRef3,
                                canGoBack = web3CanGoBack,
                                isLoading = web3IsLoading,
                                progress = web3Progress
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabWebViewContainer(
    url: String,
    onWebViewCreated: (WebView) -> Unit,
    canGoBackChanged: (Boolean) -> Unit,
    canGoForwardChanged: (Boolean) -> Unit,
    isLoadingChanged: (Boolean) -> Unit,
    progressChanged: (Float) -> Unit,
    webViewInstance: WebView?,
    canGoBack: Boolean,
    isLoading: Boolean,
    progress: Float
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // HTML Mixed Content Loading Bar
        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = NkustAmber,
                trackColor = Color.LightGray.copy(alpha = 0.2f)
            )
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, urlStr: String?, favicon: Bitmap?) {
                            isLoadingChanged(true)
                            canGoBackChanged(view?.canGoBack() ?: false)
                            canGoForwardChanged(view?.canGoForward() ?: false)
                        }
                        override fun onPageFinished(view: WebView?, urlStr: String?) {
                            isLoadingChanged(false)
                            canGoBackChanged(view?.canGoBack() ?: false)
                            canGoForwardChanged(view?.canGoForward() ?: false)
                        }
                        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                            handler?.proceed() // bypass SSL self-signed cert on school portal
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progressChanged(newProgress / 100f)
                        }
                    }
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = false
                        builtInZoomControls = true
                        displayZoomControls = false
                        textZoom = 100 // Prevent system font size scaling from overlaying or squeezing cards
                        userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        allowFileAccess = true
                        allowContentAccess = true
                    }
                    CookieManager.getInstance().setAcceptCookie(true)
                    onWebViewCreated(this)
                    loadUrl(url)
                }
            },
            update = { webView ->
                canGoBackChanged(webView.canGoBack())
                canGoForwardChanged(webView.canGoForward())
            },
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        )
    }
}

// Styled Bottom Navigation Item Row
@Composable
fun bottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
    tagSuffix: String
) {
    val animatedColor by animateColorAsState(targetValue = if (isSelected) activeColor else inactiveColor)
    
    Column(
        modifier = Modifier
            .width(55.dp)
            .clickable(onClick = onClick)
            .testTag("toolbar_$tagSuffix")
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = animatedColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.5.sp,
                color = animatedColor
            )
        )
    }
}

// ==================== [TABS 0: NATIVE HOME SCREEN] ====================
@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToBooking: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLaunchWebPortal: (String) -> Unit
) {
    val context = LocalContext.current
    
    // Quick campus list reference popup details
    val branchShortList = remember {
        listOf(
            Pair("建工分館", "三民區建工路415號"),
            Pair("第一分館", "燕巢區大學路1號"),
            Pair("楠梓分館", "楠梓區海專路142號"),
            Pair("燕巢分館", "燕巢區深中路58號"),
            Pair("旗津分館", "旗津區中洲三路482號")
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Card with Dynamic Accents
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = NkustNavy)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(NkustNavy, NkustTeal)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NkustAmber)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "五校卓越．高科鼎立",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = NkustNavy,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "高科大智慧圖書館行動門戶",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "提供跨校區圖書通閱及空間預約服務，即刻登入您的行動借閱證暢行無阻！",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.8f),
                                lineHeight = 18.2.sp
                            )
                        )
                    }
                }
            }
        }

        // Quick Portal Access bar & web bridge integration
        item {
            Column {
                Text(
                    text = "快速啟動服務",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NkustNavy
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Portal Quick Access button
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onLaunchWebPortal("https://www.lib.nkust.edu.tw/portal/") },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Launch,
                                contentDescription = "讀取首頁",
                                tint = NkustTeal,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("開啓官方網頁", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text("最新消息與電子資料", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 9.sp)
                        }
                    }

                    // Direct Seat Reservation selector shortcut
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToBooking() },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Chair,
                                contentDescription = "空間座位",
                                tint = NkustAmber,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("線上快速預約", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text("自修室、研討討論室", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        // Search redirection card
        item {
            Card(
                onClick = onNavigateToSearch,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NkustTeal.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "搜尋", tint = NkustTeal)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("館藏圖書搜尋", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = NkustNavy)
                            Text("快速檢索全五校校區藏書資源與狀態", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "前往", tint = Color.LightGray)
                }
            }
        }

        // Library News announcements row
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "本週圖書館要聞",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NkustNavy
                        )
                    )
                    Text(
                        text = "查看更多",
                        style = MaterialTheme.typography.bodySmall.copy(color = NkustTeal, fontWeight = FontWeight.Bold),
                        modifier = Modifier.clickable { onLaunchWebPortal("https://www.lib.nkust.edu.tw/portal/") }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                
                // Announcements stack
                listOf(
                    Triple("開館資訊", "115年端午連假各校區圖書館調整開放時間公告", "2026-05-20"),
                    Triple("熱門活動", "「電子資源學習坊」開始報名囉！精美禮品等您拿", "2026-05-18"),
                    Triple("系統升級", "圖書空間座位預約管理系統進行伺服器維護重啟通知", "2026-05-15")
                ).forEach { (badge, title, date) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clickable { onLaunchWebPortal("https://www.lib.nkust.edu.tw/portal/") },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (badge == "開館資訊") Color.Red.copy(alpha = 0.08f)
                                            else NkustTeal.copy(alpha = 0.08f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = badge,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (badge == "開館資訊") Color.Red else NkustTeal
                                        )
                                    )
                                }
                                Text(text = date, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black.copy(alpha = 0.85f)
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Five campuses guidelines info card
        item {
            Column {
                Text(
                    text = "校區分館指南 (通借通還)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NkustNavy
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(branchShortList) { (name, address) ->
                        Card(
                            modifier = Modifier
                                .width(200.dp)
                                .height(100.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(NkustAmber)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NkustNavy
                                    )
                                }
                                Column {
                                    Text(text = "服務地址：", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 9.sp)
                                    Text(
                                        text = address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.DarkGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== [TABS 1: NATIVE BOOK SEARCH SCREEN] ====================
@Composable
fun CatalogSearchScreen(
    viewModel: LibraryViewModel,
    onLaunchOfficialCatalogue: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    
    // Multi-dimensional filters state
    var selectedCampusFilter by remember { mutableStateOf("全部校區") }
    var selectedTypeFilter by remember { mutableStateOf("全部類型") }
    var selectedAvailabilityFilter by remember { mutableStateOf("全部狀態") }
    var selectedLanguageFilter by remember { mutableStateOf("全部語言") }
    var selectedSortBy by remember { mutableStateOf("熱門推薦") }

    var isCampusExpanded by remember { mutableStateOf(false) }
    var isLanguageExpanded by remember { mutableStateOf(false) }
    var isSortExpanded by remember { mutableStateOf(false) }

    // Detail Pop-up Dialog State
    var selectedBookForDetail by remember { mutableStateOf<CatalogueBook?>(null) }

    val catalogueBooks by viewModel.catalogue.collectAsStateWithLifecycle()
    val studentProfile by viewModel.studentProfile.collectAsStateWithLifecycle()

    val campuses = listOf("全部校區", "建工校區", "第一校區", "楠梓校區", "燕巢校區", "旗津校區")
    val types = listOf("全部類型", "圖書", "期刊", "電子書", "學位論文")
    val availabilities = listOf("全部狀態", "可借閱", "借出中", "已預約")
    val languages = listOf("全部語言", "中文", "英文")
    val sortOptions = listOf("熱門推薦", "出版年份", "索書號順序")

    // Robust search engine: queries title, author, publisher, call number, type, description, etc.
    val filteredBooks = catalogueBooks.filter { book ->
        val matchesQuery = searchQuery.isBlank() || 
            book.title.contains(searchQuery, ignoreCase = true) ||
            book.author.contains(searchQuery, ignoreCase = true) ||
            book.publisher.contains(searchQuery, ignoreCase = true) ||
            book.callNumber.contains(searchQuery, ignoreCase = true) ||
            book.description.contains(searchQuery, ignoreCase = true)

        val matchesCampus = selectedCampusFilter == "全部校區" || book.branch == selectedCampusFilter
        val matchesType = selectedTypeFilter == "全部類型" || book.type == selectedTypeFilter
        val matchesAvailability = selectedAvailabilityFilter == "全部狀態" || book.status == selectedAvailabilityFilter
        val matchesLanguage = selectedLanguageFilter == "全部語言" || book.language == selectedLanguageFilter

        matchesQuery && matchesCampus && matchesType && matchesAvailability && matchesLanguage
    }.sortedWith { a, b ->
        when (selectedSortBy) {
            "熱門推薦" -> b.recommendCount.compareTo(a.recommendCount)
            "出版年份" -> b.publishYear.compareTo(a.publishYear)
            "索書號順序" -> a.callNumber.compareTo(b.callNumber)
            else -> 0
        }
    }

    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        // High-fidelity Search Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NkustNavy)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Column {
                Text(
                    text = "學術資源與館藏目錄檢索",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                // Keyword Search input bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("搜尋書名、作者、關鍵字、索書號...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜尋") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("catalogue_search_text"),
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }

        // Dropdown interactive filters toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Campus selector
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .clickable { isCampusExpanded = true }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedCampusFilter, 
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = NkustNavy),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                }
                DropdownMenu(
                    expanded = isCampusExpanded,
                    onDismissRequest = { isCampusExpanded = false }
                ) {
                    campuses.forEach { camp ->
                        DropdownMenuItem(
                            text = { Text(camp, fontSize = 12.sp) },
                            onClick = {
                                selectedCampusFilter = camp
                                isCampusExpanded = false
                            }
                        )
                    }
                }
            }

            // Language selector
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .clickable { isLanguageExpanded = true }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedLanguageFilter, 
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = NkustNavy),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                }
                DropdownMenu(
                    expanded = isLanguageExpanded,
                    onDismissRequest = { isLanguageExpanded = false }
                ) {
                    languages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang, fontSize = 12.sp) },
                            onClick = {
                                selectedLanguageFilter = lang
                                isLanguageExpanded = false
                            }
                        )
                    }
                }
            }

            // Sorter dropdown
            Box(modifier = Modifier.weight(1.2f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .clickable { isSortExpanded = true }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "排序: $selectedSortBy", 
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = NkustNavy),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                }
                DropdownMenu(
                    expanded = isSortExpanded,
                    onDismissRequest = { isSortExpanded = false }
                ) {
                    sortOptions.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(sort, fontSize = 12.sp) },
                            onClick = {
                                selectedSortBy = sort
                                isSortExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Horizontal sliding filter chips for Material Type
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(types) { type ->
                SearchFilterChip(
                    label = when(type) {
                        "全部類型" -> "📚 全部類型"
                        "圖書" -> "📖 圖書"
                        "期刊" -> "📰 期刊"
                        "電子書" -> "💻 電子書"
                        "學位論文" -> "🎓 學位論文"
                        else -> type
                    },
                    isSelected = selectedTypeFilter == type,
                    activeColor = NkustTeal,
                    onClick = { selectedTypeFilter = type }
                )
            }
        }

        // Horizontal sliding filter chips for Availability status
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availabilities) { avail ->
                SearchFilterChip(
                    label = when(avail) {
                        "全部狀態" -> "🔍 全部狀態"
                        "可借閱" -> "✅ 可借閱"
                        "借出中" -> "❌ 借出中"
                        "已預約" -> "⏰ 已預約"
                        else -> avail
                    },
                    isSelected = selectedAvailabilityFilter == avail,
                    activeColor = NkustAmber,
                    onClick = { selectedAvailabilityFilter = avail }
                )
            }
        }

        // Active filters & counters row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NkustTeal.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "篩選中: $selectedTypeFilter | $selectedAvailabilityFilter",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NkustTeal,
                            fontSize = 11.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "共計 ${filteredBooks.size} 筆符合",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.Gray
                )
            }
            
            TextButton(
                onClick = onLaunchOfficialCatalogue,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Launch, contentDescription = "官方", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("官方圖書館系統", fontSize = 11.sp, color = NkustTeal, fontWeight = FontWeight.Bold)
            }
        }

        // Search Results List
        if (filteredBooks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "無符合書籍",
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("無符合篩選條件的學術館藏", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
                    Text("請重設篩選條件或輸入不同關鍵字再試一次", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredBooks, key = { it.id }) { book ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBookForDetail = book }
                    ) {
                        Row(modifier = Modifier.padding(14.dp)) {
                            // High-quality colorful side stripe based on type
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when (book.type) {
                                            "圖書" -> NkustNavy
                                            "期刊" -> NkustTeal
                                            "電子書" -> NkustAmber
                                            else -> Color(0xFF9C27B0) // Degree Thesis
                                        }
                                    )
                            )
                            
                            Spacer(modifier = Modifier.width(14.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Title (Clickable details)
                                    Text(
                                        text = book.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black.copy(alpha = 0.85f),
                                            fontSize = 15.sp
                                        ),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Material Type and Status badges side-by-side
                                    Column(horizontalAlignment = Alignment.End) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    when (book.status) {
                                                        "可借閱" -> Color(0xFFE8F5E9)
                                                        "借出中" -> Color(0xFFFFEBEE)
                                                        else -> Color(0xFFFFF3E0)
                                                    }
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = book.status,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (book.status) {
                                                        "可借閱" -> Color(0xFF2E7D32)
                                                        "借出中" -> Color(0xFFC62828)
                                                        else -> Color(0xFFEF6C00)
                                                    }
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(NkustNavy.copy(alpha = 0.08f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = book.type,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 8.sp,
                                                    color = NkustNavy,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Text(
                                    text = "作者：${book.author} | ${book.publishYear}年 | ${book.language}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "索書號：${book.callNumber} | 館藏：${book.branch}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = NkustNavy
                                    )
                                )
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Native Action buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // View Detail button
                                    OutlinedButton(
                                        onClick = { selectedBookForDetail = book },
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, NkustNavy),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NkustNavy),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = "詳情", modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("詳細資訊", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    // Primary operation button
                                    if (book.status == "可借閱") {
                                        Button(
                                            onClick = {
                                                if (studentProfile == null) {
                                                    Toast.makeText(context, "請先至『個人』頁面登入行動借閱證！", Toast.LENGTH_LONG).show()
                                                } else {
                                                    val result = viewModel.borrowCatalogueBook(book.id)
                                                    Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = NkustTeal,
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(Icons.Default.Book, contentDescription = "借書", modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("借閱此書", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                Toast.makeText(context, "該書目前非可借狀態。已為您送出『跨校預約調撥申請』！書籍送達${studentProfile?.campus ?: "建工校區"}分館時會發送 Email 提醒並保留登記三天。", Toast.LENGTH_LONG).show()
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.LightGray,
                                                contentColor = Color.DarkGray
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("跨校預約調撥", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Book Detail dialog
    selectedBookForDetail?.let { book ->
        AlertDialog(
            onDismissRequest = { selectedBookForDetail = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (book.status == "可借閱") {
                            if (studentProfile == null) {
                                Toast.makeText(context, "請先至『個人』頁面登入行動借閱證！", Toast.LENGTH_LONG).show()
                            } else {
                                val result = viewModel.borrowCatalogueBook(book.id)
                                Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "已幫您送出『跨校代借調撥申請』！將依預定志願順序安排跨分館遞送機制。", Toast.LENGTH_LONG).show()
                        }
                        selectedBookForDetail = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = NkustTeal)
                ) {
                    Text(
                        text = if (book.status == "可借閱") "立即借閱" else "申請預約/調撥", 
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBookForDetail = null }) {
                    Text("關閉", color = Color.Gray)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(NkustTeal)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "館藏詳細資訊",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NkustNavy.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(book.type, fontSize = 9.sp, color = NkustNavy, fontWeight = FontWeight.Bold)
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when (book.status) {
                                        "可借閱" -> Color(0xFFE8F5E9)
                                        "借出中" -> Color(0xFFFFEBEE)
                                        else -> Color(0xFFFFF3E0)
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = book.status,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (book.status) {
                                    "可借閱" -> Color(0xFF2E7D32)
                                    "借出中" -> Color(0xFFC62828)
                                    else -> Color(0xFFEF6C00)
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.LightGray.copy(alpha = 0.4f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(book.language, fontSize = 9.sp, color = Color.DarkGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    DetailRow(label = "主要作者", value = book.author)
                    DetailRow(label = "出版機構", value = book.publisher)
                    DetailRow(label = "出版年份", value = "${book.publishYear} 年")
                    DetailRow(label = "索書編號", value = book.callNumber)
                    DetailRow(label = "典藏校區", value = book.branch)
                    DetailRow(label = "學術評等", value = "★".repeat(minOf(5, maxOf(1, book.recommendCount / 40 + 1))) + " (${book.recommendCount} 次推薦 / 借閱熱門)")

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("學術簡介", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Gray))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = book.description.ifBlank { "暫無相關書籍簡介描述。本校為加強圖書資源，提供多元學術查找與跨校代借代還服務。" },
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray, lineHeight = 16.sp)
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun SearchFilterChip(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) activeColor else Color.White)
            .border(
                width = 1.dp,
                color = if (isSelected) activeColor else Color(0xFFDDDDDD),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (isSelected) {
                    if (activeColor == NkustAmber) NkustNavy else Color.White
                } else Color.DarkGray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp),
            modifier = Modifier.width(75.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(color = Color.Black, fontSize = 11.sp)
        )
    }
}

// ==================== [TABS 2: NATIVE INTERACTIVE BOOKING SCREEN] ====================
@Composable
fun InteractiveBookingScreen(
    viewModel: LibraryViewModel,
    onLaunchOfficialBooking: () -> Unit
) {
    val context = LocalContext.current
    
    // Seat filters
    var selectedCampus by remember { mutableStateOf("建工校區") }
    var selectedArea by remember { mutableStateOf("2F 經典自修區") }

    val campuses = listOf("建工校區", "第一校區", "楠梓校區", "燕巢校區", "旗津校區")
    val areas = listOf("2F 經典自修區", "3F 單人自修區", "4F 數位閱覽座", "5F 研討論壇討論區")

    val selectedSeatNo by viewModel.selectedSeat.collectAsStateWithLifecycle()
    val studentProfile by viewModel.studentProfile.collectAsStateWithLifecycle()
    
    // Live query occupied seats based on filters
    val occupiedSeats = remember(selectedCampus, selectedArea) {
        viewModel.getOccupiedSeatsFor(selectedCampus, selectedArea)
    }

    var selectedDurationHours by remember { mutableStateOf(2) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper banner introducing the seat mapping
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "預約空間及座位",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NkustNavy
                            )
                        )
                        
                        TextButton(onClick = onLaunchOfficialBooking) {
                            Icon(Icons.Default.Launch, contentDescription = "SSO", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("開啓官方預約頁面", fontSize = 11.sp, color = NkustTeal, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "線上即時預定校區自修或研討座位。預定後請在定時內以行動借閱證條碼於讀卡機靠卡感應報到，即可開啓空間座位權限！",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }
            }
        }

        // Dropdown location selector settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("1. 選擇預定分館與區域", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Location filter selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Campus drop filter
                        var campusDropExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { campusDropExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, NkustNavy),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedCampus, style = MaterialTheme.typography.bodyMedium, color = NkustNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "選擇", tint = NkustNavy)
                                }
                            }
                            DropdownMenu(
                                expanded = campusDropExpanded,
                                onDismissRequest = { campusDropExpanded = false }
                            ) {
                                campuses.forEach { camp ->
                                    DropdownMenuItem(
                                        text = { Text(camp) },
                                        onClick = {
                                            selectedCampus = camp
                                            viewModel.selectSeat(null) // clear active seat
                                            campusDropExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Area drop filter
                        var areaDropExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1.3f)) {
                            OutlinedButton(
                                onClick = { areaDropExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, NkustTeal),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedArea, style = MaterialTheme.typography.bodyMedium, color = NkustTeal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "選擇", tint = NkustTeal)
                                }
                            }
                            DropdownMenu(
                                expanded = areaDropExpanded,
                                onDismissRequest = { areaDropExpanded = false }
                            ) {
                                areas.forEach { area ->
                                    DropdownMenuItem(
                                        text = { Text(area) },
                                        onClick = {
                                            selectedArea = area
                                            viewModel.selectSeat(null) // clear active seat
                                            areaDropExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Interactive Seat matrix map
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("2. 平面方位座位圖 (請點選空位)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Dashboard Front screen pointer direction
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(20.dp)
                            .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                            .background(Color.LightGray.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("【講台螢幕 / 入口在此方向】", style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color.Gray)
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    // Reconstruct 24 Seats Matrix
                    val seatColumns = 4
                    val totalSeats = 24
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (row in 0 until (totalSeats / seatColumns)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    for (col in 0 until seatColumns) {
                                        val seatIndex = row * seatColumns + col + 1
                                        val seatNo = String.format(Locale.US, "%02d", seatIndex)
                                        
                                        val isOccupied = occupiedSeats.contains(seatNo)
                                        val isSelected = selectedSeatNo == seatNo
                                        
                                        // Compute styling based on states
                                        val seatBg = when {
                                            isOccupied -> Color.LightGray.copy(alpha = 0.6f)
                                            isSelected -> NkustAmber
                                            else -> Color.White
                                        }
                                        val seatText = when {
                                            isOccupied -> Color.Gray
                                            isSelected -> NkustNavy
                                            else -> NkustNavy
                                        }
                                        val seatBorder = when {
                                            isOccupied -> BorderStroke(1.dp, Color.Transparent)
                                            isSelected -> BorderStroke(1.5.dp, NkustNavy)
                                            else -> BorderStroke(1.dp, NkustTeal)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(width = 54.dp, height = 42.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(seatBg)
                                                .clickable(enabled = !isOccupied) {
                                                    if (selectedSeatNo == seatNo) {
                                                        viewModel.selectSeat(null)
                                                    } else {
                                                        viewModel.selectSeat(seatNo)
                                                    }
                                                }
                                                .then(if (!isOccupied) Modifier.background(seatBg) else Modifier),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Handle custom borders
                                            Surface(
                                                color = Color.Transparent,
                                                shape = RoundedCornerShape(6.dp),
                                                border = seatBorder,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = seatNo,
                                                        fontWeight = FontWeight.Bold,
                                                        color = seatText,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Legend status explanations row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        LegendItem("已佔用", Color.LightGray.copy(alpha = 0.6f), BorderStroke(0.dp, Color.Transparent))
                        LegendItem("可預訂", Color.White, BorderStroke(1.dp, NkustTeal))
                        LegendItem("您的選擇", NkustAmber, BorderStroke(1.dp, NkustNavy))
                    }
                }
            }
        }

        // Seat booking settings duration
        if (selectedSeatNo != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("3. 設定預約時間段", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "所選座位：${selectedCampus} ${selectedArea} - 座位 ${selectedSeatNo}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NkustNavy
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Text("預計借用時間長度：", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(1, 2, 4, 8).forEach { hrs ->
                                val activeBtn = selectedDurationHours == hrs
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (activeBtn) NkustTeal else Color(0xFFECEFF1))
                                        .clickable { selectedDurationHours = hrs }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$hrs 小時",
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeBtn) Color.White else Color.DarkGray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (studentProfile == null) {
                                    Toast.makeText(context, "請先至『個人』頁面登入行動借閱證，以保存預約空間座位！", Toast.LENGTH_LONG).show()
                                } else {
                                    val success = viewModel.reserveSeat(
                                        campus = selectedCampus,
                                        area = selectedArea,
                                        seatNo = selectedSeatNo!!,
                                        durationHours = selectedDurationHours
                                    )
                                    if (success) {
                                        Toast.makeText(
                                            context,
                                            "預約成功！座位 ${selectedSeatNo} 已為您保留，可至『個人』分頁查詢。",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Toast.makeText(context, "該座已被佔用，請重新選擇", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("seat_booking_submit"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NkustNavy,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.EventAvailable, contentDescription = "預約")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("確認預約座位", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color, border: BorderStroke) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(16.dp),
            color = color,
            shape = RoundedCornerShape(3.dp),
            border = border
        ) {}
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 11.sp)
    }
}

// ==================== [TABS 3: PROFILE SCREEN MATCHING SCREENSHOT EXACTLY] ====================
@Composable
fun ProfilePassScreen(
    viewModel: LibraryViewModel
) {
    val context = LocalContext.current
    
    val studentProfile by viewModel.studentProfile.collectAsStateWithLifecycle()
    val borrowedBooks by viewModel.borrowedBooks.collectAsStateWithLifecycle()
    val spaceReservations by viewModel.spaceReservations.collectAsStateWithLifecycle()

    var activeStudentIdInput by remember { mutableStateOf("C110156789") }
    var activeNameInput by remember { mutableStateOf("王小明") }
    var activeDeptInput by remember { mutableStateOf("資管系") }

    var selectedCampus by remember { mutableStateOf("燕巢校區") }
    val campuses = listOf("建工校區", "第一校區", "楠梓校區", "燕巢校區", "旗津校區")

    if (studentProfile == null) {
        // Redesigned beautiful university single login entry as user requested "登入"的地方
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA)),
            contentPadding = PaddingValues(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "登入",
                    tint = NkustNavy,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "高科大單一入口帳號登入",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NkustNavy
                    )
                )
                Text(
                    text = "NKUST Single Sign-On Portal",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // School ID account input
                        OutlinedTextField(
                            value = activeStudentIdInput,
                            onValueChange = { activeStudentIdInput = it },
                            label = { Text("學號 (例: C110156789)") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = "學號") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_input_id"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Student Name input
                        OutlinedTextField(
                            value = activeNameInput,
                            onValueChange = { activeNameInput = it },
                            label = { Text("姓名 (例: 王小明)") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "姓名") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_input_name"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Department input
                        OutlinedTextField(
                            value = activeDeptInput,
                            onValueChange = { activeDeptInput = it },
                            label = { Text("科系科別 (例: 資管系)") },
                            leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = "科系") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_input_dept"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Campus drop matching user choices
                        Column {
                            Text("所屬校區圖書館：", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                campuses.take(3).forEach { camp ->
                                    val isSel = selectedCampus == camp
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) NkustTeal else Color(0xFFF1F3F5))
                                            .clickable { selectedCampus = camp }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(camp.take(2), color = if (isSel) Color.White else Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                campuses.drop(3).forEach { camp ->
                                    val isSel = selectedCampus == camp
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) NkustTeal else Color(0xFFF1F3F5))
                                            .clickable { selectedCampus = camp }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(camp.take(2), color = if (isSel) Color.White else Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Password placeholder
                        OutlinedTextField(
                            value = "••••••••••••",
                            onValueChange = {},
                            label = { Text("單一登入密碼") },
                            readOnly = true,
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "密碼") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = Color.LightGray
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                val success = viewModel.login(
                                    studentId = activeStudentIdInput,
                                    name = activeNameInput,
                                    department = activeDeptInput,
                                    campus = selectedCampus
                                )
                                if (success) {
                                    Toast.makeText(context, "登入成功！歡迎 ${activeNameInput} 同學！", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("login_submit_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NkustNavy,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("單一入口安全認證登入", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        // Redesigned user profile student pass screen matching user's reference mockup exactly!
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card replication modeled after screenshot
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF02AAB0), // Cyber-green/turquoise gradient matching photo
                                        Color(0xFF00CDAC),
                                        NkustNavy
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            // Row 1: Header tags
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "NKUST LIBRARY PASS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "在學/有效",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF00AA66),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "高科大行動借閱證",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // Barcode White box overlay
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Custom beautifully programmed high-fidelity Barcode drawing block!
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .height(42.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Generates realistic barcode bars natively!
                                        listOf(
                                            2, 1, 3, 1, 5, 2, 1, 4, 1, 2, 4, 1, 3, 1, 2, 3, 1, 4, 1, 2, 5, 1, 2, 1, 4
                                        ).forEachIndexed { i, widthMultiplier ->
                                            Spacer(
                                                modifier = Modifier
                                                    .width((widthMultiplier * 1.5).dp)
                                                    .fillMaxHeight()
                                                    .background(if (i % 2 == 0) Color.Black else Color.White)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    Text(
                                        text = "${studentProfile?.studentId ?: ""} (${studentProfile?.campus ?: ""})",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.Black.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 9.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Bottom row info matching user's photo
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "姓名",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 10.sp
                                        )
                                    )
                                    Text(
                                        text = "${studentProfile?.name ?: ""} (${studentProfile?.department ?: ""})",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "身分識別",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 10.sp
                                        )
                                    )
                                    Text(
                                        text = studentProfile?.identity ?: "大學部學生",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Loans statistics row matching user's screenshot exactly!
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricWidget(
                        value = "${borrowedBooks.size}",
                        label = "借閱中",
                        valueColor = Color(0xFF673AB7), // Deep purple as shown in screen
                        modifier = Modifier.weight(1f)
                    )
                    MetricWidget(
                        value = "0",
                        label = "已逾期",
                        valueColor = Color(0xFFF44336), // Bright red as shown in screen
                        modifier = Modifier.weight(1f)
                    )
                    MetricWidget(
                        value = "30",
                        label = "最大借閱額",
                        valueColor = Color(0xFF009688), // Solid blue teal as shown in screen
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Loaned book lists segment
            item {
                Text(
                    text = "📖 目前借閱中圖書",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = NkustNavy
                    )
                )
            }

            if (borrowedBooks.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("目前尚無借閱中的實體書籍", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(borrowedBooks, key = { it.id }) { loan ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Book Header & Title
                            Text(
                                text = loan.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black.copy(alpha = 0.85f)
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Book properties
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "還書期限：${loan.dueDate}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (loan.renewCount == 0) Color.DarkGray else Color(0xFF00AA66),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "借閱地點：${loan.branch} | 索書號：${loan.callNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                
                                // Renew Count Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NkustTeal.copy(alpha = 0.08f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "續借次數：${loan.renewCount}/${loan.maxRenew}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NkustTeal
                                        )
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Active online renewal action button matching mock screen
                            Button(
                                onClick = {
                                    val (success, message) = viewModel.renewBook(loan.id)
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFECEFF1),
                                    contentColor = Color.DarkGray
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .height(34.dp)
                            ) {
                                Icon(Icons.Default.Autorenew, contentDescription = "續借", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("線上續借", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Reserved space bookings lists segment
            item {
                Text(
                    text = "📅 已預約圖書館空間座位",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = NkustNavy
                    )
                )
            }

            if (spaceReservations.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("目前尚無預約自修室或座位登記", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(spaceReservations, key = { it.id }) { reservation ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        text = "${reservation.campus} - ${reservation.area}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = NkustNavy
                                    )
                                    Text(
                                        text = "預約座位代號：${reservation.seatNo} 號座位",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NkustAmber
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NkustTeal.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "保留期：${reservation.durationHours}小時",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NkustTeal
                                        )
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "時段：${reservation.dateString} ${reservation.timeSlot}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        viewModel.cancelReservation(reservation)
                                        Toast.makeText(context, "已取消該座位之預約保留！", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "取消預約", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("釋放座位 / 取消預約", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Logout row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.logout()
                            Toast.makeText(context, "已成功登出借閱證", Toast.LENGTH_SHORT).show()
                        },
                        border = BorderStroke(1.dp, Color.Gray),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "登出", tint = Color.Gray)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("安全登出單一登入證", color = Color.Gray)
                    }
                }
            }
        }
    }
}

// Stats boxes under pass metric
@Composable
fun MetricWidget(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = valueColor
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==================== [TABS 4: SMART AI LIBRARIAN ASSISTANT] ====================
@Composable
fun SmartLibrarianAssistant(
    viewModel: LibraryViewModel
) {
    val context = LocalContext.current
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading = viewModel.isChatLoading
    
    var typedMessageText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F3F5))
    ) {
        // Welcome and intro guide banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NkustNavy)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NkustAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = "智能諮詢", tint = NkustAmber)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("AI 智慧諮詢圖書管員", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    Text("解答您的借領說明、分館開館與推薦好書", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.7f)))
                }
            }
        }

        // Messages list history
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(chatMessages, key = { it.id }) { msg ->
                val alignment = if (msg.isUser) Alignment.End else Alignment.Start
                val bubbleBg = if (msg.isUser) NkustTeal else Color.White
                val bubbleTextColor = if (msg.isUser) Color.White else Color.Black.copy(alpha = 0.85f)
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = alignment
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .wrapContentWidth(align = if (msg.isUser) Alignment.End else Alignment.Start)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (msg.isUser) 12.dp else 0.dp,
                                    bottomEnd = if (msg.isUser) 0.dp else 12.dp
                                )
                            )
                            .background(bubbleBg)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = bubbleTextColor,
                                lineHeight = 19.sp
                            )
                        )
                    }
                }
            }

            // Streaming / Processing indicator
            if (isChatLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = NkustNavy)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("智慧助理正在整理答案...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // Quick query chips row for instant help
        val quickQueries = listOf("開館時間", "借書上限與規則", "推薦 Python 書籍", "如何續借書籍")
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickQueries) { item ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .clickable {
                            typedMessageText = ""
                            viewModel.sendChatMessage(item)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NkustNavy
                        )
                    )
                }
            }
        }

        // Typing entry search message box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 8.dp,
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = typedMessageText,
                    onValueChange = { typedMessageText = it },
                    placeholder = { Text("請輸入您的圖書館諮詢...", fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_input_text"),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (typedMessageText.isNotBlank()) {
                                viewModel.sendChatMessage(typedMessageText)
                                typedMessageText = ""
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF1F3F5),
                        unfocusedContainerColor = Color(0xFFF1F3F5),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (typedMessageText.isNotBlank()) NkustNavy else Color.LightGray)
                        .clickable(enabled = typedMessageText.isNotBlank()) {
                            viewModel.sendChatMessage(typedMessageText)
                            typedMessageText = ""
                            focusManager.clearFocus()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "傳送",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
