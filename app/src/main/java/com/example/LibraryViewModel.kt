package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// Student account state
data class StudentProfile(
    val studentId: String,
    val name: String,
    val department: String,
    val campus: String,
    val identity: String,
    val status: String = "在學/有效"
)

// Book loan model
data class BookLoan(
    val id: String,
    val title: String,
    val author: String,
    val dueDate: String,
    val branch: String,
    val renewCount: Int,
    val maxRenew: Int = 2,
    val callNumber: String = ""
)

// Library catalog item model
data class CatalogueBook(
    val id: String,
    val title: String,
    val author: String,
    val publisher: String,
    val branch: String,
    val callNumber: String,
    var status: String, // "可借閱", "借出中", "已預約"
    val coverAccent: String = "NkustNavy",
    val type: String = "圖書", // "圖書", "期刊", "電子書", "學位論文"
    val publishYear: Int = 2023,
    val language: String = "中文", // "中文", "英文"
    val recommendCount: Int = 0,
    val description: String = ""
)

// Space reservation model
data class SpaceReservation(
    val id: String,
    val campus: String,
    val area: String,
    val seatNo: String,
    val dateString: String,
    val timeSlot: String,
    val durationHours: Int
)

// Message model for AI chat
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class LibraryViewModel : ViewModel() {

    // Login profile state
    private val _studentProfile = MutableStateFlow<StudentProfile?>(null)
    val studentProfile: StateFlow<StudentProfile?> = _studentProfile.asStateFlow()

    // Borrowed books list
    private val _borrowedBooks = MutableStateFlow<List<BookLoan>>(emptyList())
    val borrowedBooks: StateFlow<List<BookLoan>> = _borrowedBooks.asStateFlow()

    // Space bookings list
    private val _spaceReservations = MutableStateFlow<List<SpaceReservation>>(emptyList())
    val spaceReservations: StateFlow<List<SpaceReservation>> = _spaceReservations.asStateFlow()

    // Book catalog items
    private val _catalogue = MutableStateFlow<List<CatalogueBook>>(emptyList())
    val catalogue: StateFlow<List<CatalogueBook>> = _catalogue.asStateFlow()

    // Chatbot history
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    var isChatLoading by mutableStateOf(false)
        private set

    // Selected seat states for simulation in interactive map
    // Key: "Campus|Area", Value: Set of occupied seat numbers (Strings like "01", "08")
    private val _occupiedSeatsMap = mutableMapOf<String, Set<String>>()
    private val _selectedSeat = MutableStateFlow<String?>(null)
    val selectedSeat: StateFlow<String?> = _selectedSeat.asStateFlow()

    init {
        loadCatalog()
        setupDefaultMessages()
    }

    private fun loadCatalog() {
        val books = listOf(
            CatalogueBook(
                "c1", "Python 資料分析與視覺化實戰指南", "林俊傑", "博碩文化",
                "燕巢校區", "005.133 / 8852", "可借閱", "NkustNavy",
                "圖書", 2024, "中文", 142, "深入探討 Pandas、NumPy 與 Matplotlib 等資料分析套件，附帶股票與銷售數據之精彩實例操作。"
            ),
            CatalogueBook(
                "c2", "演算法與資料結構圖解入門指南", "張子健", "深智數位",
                "第一校區", "005.13 / 3491", "借出中", "NkustTeal",
                "圖書", 2023, "中文", 98, "以精美色彩圖解、白話文解說與完整虛擬代碼，輕鬆突破樹、圖、動態規劃等高難度演算法瓶頸。"
            ),
            CatalogueBook(
                "c3", "人工智慧與專家系統核心原理", "Stuart Russell", "歐亞書局",
                "建工校區", "004.8 / 4421", "可借閱", "NkustAmber",
                "圖書", 2020, "英文", 76, "國際經典教材繁譯本，介紹智慧代理人架構、不確定性推理、馬可夫決策與經典機器學習。"
            ),
            CatalogueBook(
                "c4", "生成式 AI 精實商務開發應用指引", "陳曉梅", "旗標出版",
                "燕巢校區", "004.83 / 1120", "可借閱", "NkustTeal",
                "圖書", 2024, "中文", 205, "實用提示工程與 LLM API 開發技術，手把手教您如何串接 RAG 知識庫與 OpenAI / Gemini 助理。"
            ),
            CatalogueBook(
                "c5", "現代離岸風力發電與海洋工程導論", "黃國防", "高科大出版社",
                "楠梓校區", "551.46 / 7748", "可借閱", "NkustNavy",
                "圖書", 2022, "中文", 61, "本校水產與海洋工程專家精選專書，系統化論述水下土力學、風機基樁穩固與高雄外海大氣資料研判。"
            ),
            CatalogueBook(
                "c6", "航海避碰學與現代雷達觀測實務", "王海清", "五南圖書",
                "旗津校區", "557.82 / 9011", "可借閱", "NkustTeal",
                "圖書", 2021, "中文", 45, "針對避碰避險規劃與航向雷達(ARPA)判讀，提供詳盡海事航運實際模擬與國際公約避碰規則判斷系統。"
            ),
            CatalogueBook(
                "c7", "使用者體驗與 App 介面優雅美學設計", "劉美德", "全華圖書",
                "建工校區", "005.43 / 2219", "可借閱", "NkustAmber",
                "圖書", 2023, "中文", 120, "深入講解 Material Design 3、介面排版黃金比例、色調美學引導、互動微動畫與可用性測試指標。"
            ),
            CatalogueBook(
                "c8", "FinTech 數位金融創新與區塊鏈實務", "趙自強", "東華書局",
                "燕巢校區", "311.24 / 5600", "借出中", "NkustNavy",
                "圖書", 2023, "中文", 82, "解析加密金融、智能合約安全性、去中心化金融(DeFi)以及生成式 AI 在純網銀風險信用評估的運用。"
            ),
            CatalogueBook(
                "c9", "Kotlin 跨平台行動應用程式開發黃金法則", "周傑斯", "碁峰資訊",
                "第一校區", "005.133 / 1234", "可借閱", "NkustTeal",
                "圖書", 2024, "中文", 131, "探討 Kotlin Multiplatform (KMP) 在 Android 與 iOS 雙平台共用商業邏輯的實用架構設計與優化。"
            ),
            CatalogueBook(
                "c10", "海洋生態學與近岸水產養殖技術指南", "廖海水", "水產出版社",
                "楠梓校區", "548.33 / 8820", "可借閱", "NkustNavy",
                "圖書", 2022, "中文", 38, "高科大海洋生物科技專精論文精華，介紹智慧遠程水質監測系統、多頻段循環水巡檢與抗震網箱。"
            ),
            CatalogueBook(
                "c11", "IEEE Transactions on Mobile Computing (Vol.24)", "IEEE Society", "IEEE Publisher",
                "第一校區", "005.43 / IEEE-24", "可借閱", "NkustNavy",
                "期刊", 2025, "英文", 50, "國際行動通訊頂尖期刊，收錄前沿之 5G/6G 通訊訊號調變、網路切片、高吞吐低延遲定位與高併發路由。"
            ),
            CatalogueBook(
                "c12", "Harvard Business Review (中文版第 210 期)", "HBR 編譯組", "天下文化",
                "建工校區", "311.05 / HBR-210", "可借閱", "NkustTeal",
                "期刊", 2024, "中文", 112, "焦點文章探討：企業如何在生成式人工智慧時代重建工作流程，以及企業在 ESG 氣候變遷戰略上的投資選擇。"
            ),
            CatalogueBook(
                "c13", "國立高雄科技大學學報 (電資與工程類-第53期)", "高科大校刊編輯委員會", "高科大出版社",
                "建工校區", "051 / NKUST-53", "可借閱", "NkustAmber",
                "期刊", 2024, "中文", 35, "本校學術學報，收錄工學院、電機資訊學院及海事系統之多項跨國性重大專案技術之年度發表論文。"
            ),
            CatalogueBook(
                "c14", "基於深度學習之高科大學生選課與學習成效推薦系統", "林書豪", "高科大資管所學碩論文",
                "燕巢校區", "TH / MIS-112-08", "可借閱", "NkustNavy",
                "學位論文", 2024, "中文", 28, "112學年度本校優秀碩士論文，設計改進之協同過濾演算法，解決多校區跨域選課之學生合適課程推薦問題。"
            ),
            CatalogueBook(
                "c15", "離岸風機基礎結構之海域土壤力學與疲勞壽命分析研究", "林俊臣", "高科大水海所學碩論文",
                "楠梓校區", "TH / MAR-111-12", "可借閱", "NkustTeal",
                "學位論文", 2023, "中文", 19, "針對高雄與苗栗外海特定海床，運用 finite element method 分析風力發電機組樁體承受洋流循環震盪之疲勞程度分析。"
            ),
            CatalogueBook(
                "c16", "港口起重機與貨櫃卡車路徑排程之大數據調度優化演算法", "陳建宇", "高科大航管所學碩論文",
                "旗津校區", "TH / SHP-110-15", "已預約", "NkustAmber",
                "學位論文", 2022, "中文", 22, "優化高雄港特定貨櫃中心起重機吊車群在不確定天候下的最佳裝卸順序規劃，有效提升多點停靠裝貨效率。"
            ),
            CatalogueBook(
                "c17", "雲端運算、微服務架構與 Kubernetes 自動化部署實戰", "Cloud Native Alliance", "O'Reilly (台譯本)",
                "第一校區", "005.44 / CLD-24", "可借閱", "NkustTeal",
                "電子書", 2024, "中文", 154, "【電子書資源】探討實作 Docker 與 Kubernetes K8s 指令、Istio 服務網格控制、日誌監控與滾動部署的最佳實踐。"
            ),
            CatalogueBook(
                "c18", "Git 與 GitHub 團隊協作完全攻略指南", "版本控制研究小組", "軟體工程出版社",
                "建工校區", "005.13 / GIT-23", "可借閱", "NkustNavy",
                "電子書", 2023, "中文", 180, "【電子書資源】針對大型專案，提供分支策略 Git Flow、Merge、Rebase、Cherry-pick 重點解析，以及 GitHub Actions 行動工作流部署。"
            ),
            CatalogueBook(
                "c19", "Rust Systems Programming & Concurrency Patterns", "Steve Klabnik", "No Starch Press",
                "第一校區", "005.26 / RUST-24", "可借閱", "NkustTeal",
                "電子書", 2024, "英文", 94, "【電子書資源】深入學習 Rust 記憶體 Ownership 獨佔所有權、生命週期 Lifetime、免除 Data Race 的 Threading 設計原則。"
            ),
            CatalogueBook(
                "c20", "物聯網與智慧校園節能及大樓自動化監控實務", "高科大能源研究中心", "全華圖書",
                "建工校區", "551.3 / NK-IO-23", "可借閱", "NkustAmber",
                "圖書", 2023, "中文", 49, "評估使用 ESP32 與 LoRa 感測模組，架設跨校區大樓之冰水主機與室內空氣品質自動監控與節電警報架構。"
            )
        )
        _catalogue.value = books
    }

    private fun setupDefaultMessages() {
        _chatMessages.value = listOf(
            ChatMessage(
                text = "哈囉！我是高科大圖書館的 AI 智慧館員 👋\n我可以為您提供以下協助：\n" +
                        "1. 查詢各分館的開館時間與聯絡電話\n" +
                        "2. 解答書籍借閱與續借規則（大學部 30 本 / 30 天）\n" +
                        "3. 推薦 Python、人工智慧、海洋工程等熱門領域圖書\n" +
                        "4. 指引空間以及座位的預約方式\n\n請問今天有什麼我可以幫您的呢？",
                isUser = false
            )
        )
    }

    // Interactive Seat map generation
    fun getOccupiedSeatsFor(campus: String, area: String): Set<String> {
        val key = "$campus|$area"
        if (!_occupiedSeatsMap.containsKey(key)) {
            // Generate some random random occupied seats based on hour/campus name length
            val seed = Math.abs(campus.hashCode() + area.hashCode())
            val random = Random(seed.toLong())
            val count = random.nextInt(8, 16) // 8 to 15 occupied seats out of 24
            val seats = mutableSetOf<String>()
            while (seats.size < count) {
                val seatNum = String.format(Locale.US, "%02d", random.nextInt(1, 25))
                seats.add(seatNum)
            }
            _occupiedSeatsMap[key] = seats
        }
        return _occupiedSeatsMap[key] ?: emptySet()
    }

    fun selectSeat(seatNo: String?) {
        _selectedSeat.value = seatNo
    }

    // Interactive Reservation Action
    fun reserveSeat(campus: String, area: String, seatNo: String, durationHours: Int): Boolean {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.TAIWAN)
        val todayStr = dateFormat.format(Date())
        
        val timeFormat = SimpleDateFormat("HH:mm", Locale.TAIWAN)
        val startStr = timeFormat.format(Date())
        val cal = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, durationHours)
        }
        val endStr = timeFormat.format(cal.time)
        val timeLabel = "$startStr ~ $endStr"

        val key = "$campus|$area"
        val activeOccupied = _occupiedSeatsMap[key]?.toMutableSet() ?: mutableSetOf()
        
        if (activeOccupied.contains(seatNo)) {
            return false // Already occupied
        }

        // Add to reservation system
        val booking = SpaceReservation(
            id = UUID.randomUUID().toString(),
            campus = campus,
            area = area,
            seatNo = seatNo,
            dateString = todayStr,
            timeSlot = timeLabel,
            durationHours = durationHours
        )

        // Lock of seat in search matrix local map
        activeOccupied.add(seatNo)
        _occupiedSeatsMap[key] = activeOccupied

        // Append to list of bookings
        _spaceReservations.value = _spaceReservations.value + booking
        _selectedSeat.value = null
        return true
    }

    fun cancelReservation(reservation: SpaceReservation) {
        val key = "${reservation.campus}|${reservation.area}"
        val activeOccupied = _occupiedSeatsMap[key]?.toMutableSet() ?: mutableSetOf()
        activeOccupied.remove(reservation.seatNo)
        _occupiedSeatsMap[key] = activeOccupied

        _spaceReservations.value = _spaceReservations.value.filter { it.id != reservation.id }
    }

    // SSO login simulator
    fun login(studentId: String, name: String, department: String, campus: String): Boolean {
        if (studentId.isBlank()) return false
        
        val parsedDept = if (department.isBlank()) "資訊管理系" else department
        val parsedName = if (name.isBlank()) "王小明" else name
        val parsedCampus = if (campus.isBlank()) "燕巢校區" else campus

        val profile = StudentProfile(
            studentId = studentId,
            name = parsedName,
            department = parsedDept,
            campus = parsedCampus,
            identity = "大學部學生"
        )
        _studentProfile.value = profile

        // Populate mock loans matching profile C110156789 or new details
        _borrowedBooks.value = listOf(
            BookLoan("loan_1", "Python 資料分析與視覺化實戰指南", "林俊傑", getFutureDate(15), parsedCampus, 0, 2, "005.133 / 8852"),
            BookLoan("loan_2", "演算法與資料結構圖解入門指南", "張子健", getFutureDate(20), "第一校區", 1, 2, "005.13 / 3491")
        )
        return true
    }

    fun logout() {
        _studentProfile.value = null
        _borrowedBooks.value = emptyList()
        _spaceReservations.value = emptyList()
    }

    // Renew book locally and adjust due date realistically!
    fun renewBook(loanId: String): Pair<Boolean, String> {
        val loans = _borrowedBooks.value
        val targetIndex = loans.indexOfFirst { it.id == loanId }
        if (targetIndex == -1) return Pair(false, "找不到此項借閱記錄")
        
        val loan = loans[targetIndex]
        if (loan.renewCount >= loan.maxRenew) {
            return Pair(false, "已達最大續借次數 (最多續借 ${loan.maxRenew} 次)")
        }

        val updatedCount = loan.renewCount + 1
        
        // Parse original date or generate +14 days from active due date
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.TAIWAN)
        val newDueDateString = try {
            val date = sdf.parse(loan.dueDate) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.DAY_OF_YEAR, 14) // Extend library limit by 14 days
            }
            sdf.format(cal.time)
        } catch (e: Exception) {
            getFutureDate(28) // Fallback default
        }

        val updatedLoan = loan.copy(
            renewCount = updatedCount,
            dueDate = newDueDateString
        )

        _borrowedBooks.value = loans.toMutableList().apply {
            set(targetIndex, updatedLoan)
        }

        return Pair(true, "續借成功！新歸還期限為：$newDueDateString")
    }

    // Borrow / Request from search catalog natively
    fun borrowCatalogueBook(bookId: String): String {
        val index = _catalogue.value.indexOfFirst { it.id == bookId }
        if (index == -1) return "找不到此書籍"
        val book = _catalogue.value[index]
        if (book.status != "可借閱") {
            return "此書籍目前無法借閱 (狀態：${book.status})"
        }

        if (_studentProfile.value == null) {
            return "請先至『個人』頁面登入行動借閱證！"
        }

        if (_borrowedBooks.value.size >= 30) {
            return "已達個人借閱總上限 (30本)"
        }

        // Add to loans
        val newLoan = BookLoan(
            id = "loan_${UUID.randomUUID().toString().take(6)}",
            title = book.title,
            author = book.author,
            dueDate = getFutureDate(30), // Borrow limit is 30 days
            branch = book.branch,
            renewCount = 0,
            maxRenew = 2,
            callNumber = book.callNumber
        )

        // Update book state
        val updatedList = _catalogue.value.toMutableList()
        updatedList[index] = book.copy(status = "借出中")
        _catalogue.value = updatedList

        _borrowedBooks.value = _borrowedBooks.value + newLoan
        return "借閱成功！已加入您的行動借閱清單。"
    }

    // AI Chat action connected to Gemini
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        
        // Add user message
        val userMsg = ChatMessage(text = text, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg

        isChatLoading = true

        viewModelScope.launch {
            val replyText = try {
                callGeminiAPI(text)
            } catch (e: Exception) {
                // Return smart local default if service is disconnected/unkeyed
                getLocalSmartResponse(text)
            }
            
            _chatMessages.value = _chatMessages.value + ChatMessage(text = replyText, isUser = false)
            isChatLoading = false
        }
    }

    private suspend fun callGeminiAPI(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getLocalSmartResponse(prompt)
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val systemInstruction = "你是高科大圖書館 AI 智慧館員。請用親切熱心、學術氣質與專業的口吻輔助師生，" +
                "回答有關校區分館開館、空間座位預約、藏書逾期、借領規定與論文學習資源的諮詢。對答請務必使用繁體中文。" +
                "本校共有5個校區圖書館：建工分館、第一分館、楠梓分館、燕巢分館、旗津分館。師生單次最大借閱20或30本，借期30日，續借最多2次。"

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", prompt)
                }))
            }))
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", systemInstruction)
                }))
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)

        // Let's use gemini-3.5-flash which is the default for simple Q&A as requested in skill
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(requestBody)
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext getLocalSmartResponse(prompt)
            }
            val respString = response.body?.string() ?: ""
            if (respString.isNotBlank()) {
                val jsonResult = JSONObject(respString)
                val candidates = jsonResult.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentObj = candidate.optJSONObject("content")
                    if (contentObj != null) {
                        val parts = contentObj.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "未能獲取有用回覆。")
                        }
                    }
                }
            }
            getLocalSmartResponse(prompt)
        }
    }

    private fun getLocalSmartResponse(prompt: String): String {
        val lowerPrompt = prompt.lowercase()
        return when {
            lowerPrompt.contains("預約") || lowerPrompt.contains("空間") || lowerPrompt.contains("座位") -> {
                "【空間座位預約指引】\n" +
                        "1. 您可以直接在我們 App 下方的『預約』分頁點擊您心儀的座位進行『快速預約』，預約後即可至『個人』頁面查看並管理預約進度。\n" +
                        "2. 本校提供多重研討空間、自修室及多媒體體驗區。若需要完整預定大型團體研討室，歡迎點選官方網頁空間預約連結進行校外 SSO 單一入口認證預約。\n\n請問還有其他需要協助的部分嗎？"
            }
            lowerPrompt.contains("時間") || lowerPrompt.contains("開館") || lowerPrompt.contains("分館") -> {
                "【高科大各校區分館開放時間】\n" +
                        "🏫 建工、第一、楠梓、燕巢校區：\n" +
                        "  * 週一至週五：08:20 - 22:00\n" +
                        "  * 週六、週日：09:00 - 17:00 (燕巢週日不開放)\n" +
                        "🚢 旗津校區圖書館：\n" +
                        "  * 週一至週五：08:20 - 17:00 (週末不開放)\n\n" +
                        "各校區皆支援跨校區代借及跨校區還書，十分便利！"
            }
            lowerPrompt.contains("借") || lowerPrompt.contains("還書") || lowerPrompt.contains("續借") || lowerPrompt.contains("規則") -> {
                "【借閱圖書規則與額度說明】\n" +
                        "🎓 借閱上限：大學部學生單次最多可借閱 **30 本**圖書。\n" +
                        "🗓️ 借閱天數：一般圖書借期為 **30 天**。\n" +
                        "🔄 線上續借：若無其他讀者預約，且在逾期之前，可於線上續借最多 **2 次**，每次延長 14 天。\n" +
                        "💡 續借操作：可直接在本 App 下方的『個人』分頁，在『目前借閱中圖書』清單對特定书籍點按『線上續借』，系統便會即時更新還書期限！"
            }
            lowerPrompt.contains("python") || lowerPrompt.contains("程式") || lowerPrompt.contains("ai") || lowerPrompt.contains("人工智慧") || lowerPrompt.contains("推薦") -> {
                "【熱門與推薦學術書籍】\n" +
                        "經為您盤點高科大館藏，向您推薦以下本月最熱門圖書：\n" +
                        "1. 《Python 資料分析與視覺化實戰指南》（燕巢校區庫存：可借閱）\n" +
                        "2. 《演算法與資料結構圖解入門指南》（第一校區庫存：可借閱）\n" +
                        "3. 《生成式 AI 精實商務開發應用指引》（建工校區庫存：可借閱）\n" +
                        "4. 《使用者體驗與 App 介面優雅美學設計》（建工校區庫存：可借閱）\n\n" +
                        "您可以前往『搜尋』分頁直接輸入關鍵字預借，或直接借閱至您的行動借閱帳戶中喔！"
            }
            else -> {
                "好的，已收到您的諮詢！關於「${prompt.take(15)}...」的相關業務，高科大圖書館目前提供完善的學術查找、多媒體跨校共用及線上預約系統。\n\n" +
                        "如果您有更具體的問題，如借書證掛失、跨校圖書調撥申請，您可以直接撥打各分館分機，或再次向我詢問。我隨時保持待命！"
            }
        }
    }

    private fun getFutureDate(days: Int): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.TAIWAN)
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, days)
        }
        return sdf.format(cal.time)
    }
}
