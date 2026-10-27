package com.son.streamplayertv

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.google.gson.Gson

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private enum class HomeSection(val label: String) {
        INICIO("Inicio"), SERIES("Series"), PELICULAS("Películas"), EN_VIVO("En vivo")
    }

    private data class OfficialLiveLink(val name: String, val detail: String, val url: String)

    private val officialPeruLiveLinks = listOf(
        OfficialLiveLink("TV Perú", "Señal oficial del IRTP", "https://www.tvperu.gob.pe/play"),
        OfficialLiveLink("IRTP Play", "Cuatro señales oficiales del IRTP", "https://www.irtpplay.gob.pe/tvperunoticias/envivo"),
        OfficialLiveLink("América TV", "Transmisión oficial por América TVGO", "https://tvgo.americatv.com.pe/"),
        OfficialLiveLink("Panamericana", "Señal oficial en vivo", "https://m.panamericana.pe/tvenvivo/hd")
    )

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var mainLayout: FrameLayout
    private lateinit var overlayContainer: RelativeLayout
    private lateinit var btnMenu: TextView
    private lateinit var btnPrevCategory: TextView
    private lateinit var btnNextCategory: TextView
    private lateinit var txtHeaderInfo: TextView
    private lateinit var txtCategoryTitle: TextView
    private lateinit var sideMenu: LinearLayout
    private lateinit var listChannels: ListView
    private lateinit var homeContainer: LinearLayout
    private lateinit var homeContent: LinearLayout
    private lateinit var homeScroll: ScrollView
    private lateinit var homeBottomNav: LinearLayout
    private lateinit var homeTitle: TextView
    private lateinit var homeSubtitle: TextView

    private var selectedHomeSection = HomeSection.INICIO
    private var swipeStartX = 0f
    private var swipeStartY = 0f

    private var currentCategoryIndex = 0
    private var currentChannelIndex = 0
    private var filteredChannels: List<Channel> = emptyList()
    private var catalog = CatalogDto()
    private var currentVodItems: List<PlaybackItem> = emptyList()
    private var currentVodIndex = -1
    private var currentVodSeriesTitle = ""
    private var isVodPlayback = false

    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable {
        if (sideMenu.visibility != View.VISIBLE) {
            overlayContainer.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        mainLayout = findViewById(R.id.mainLayout)
        overlayContainer = findViewById(R.id.overlayContainer)
        btnMenu = findViewById(R.id.btnMenu)
        btnPrevCategory = findViewById(R.id.btnPrevCategory)
        btnNextCategory = findViewById(R.id.btnNextCategory)
        txtHeaderInfo = findViewById(R.id.txtHeaderInfo)
        txtCategoryTitle = findViewById(R.id.txtCategoryTitle)
        sideMenu = findViewById(R.id.sideMenu)
        listChannels = findViewById(R.id.listChannels)
        homeContainer = findViewById(R.id.homeContainer)
        homeContent = findViewById(R.id.homeContent)
        homeScroll = findViewById(R.id.homeScroll)
        homeBottomNav = findViewById(R.id.homeBottomNav)
        homeTitle = findViewById(R.id.homeTitle)
        homeSubtitle = findViewById(R.id.homeSubtitle)

        overlayContainer.visibility = View.GONE
        sideMenu.visibility = View.GONE
        homeContainer.visibility = View.VISIBLE
        loadBundledChannels()
        loadBundledCatalog()
        setupHomeNavigation()
        setupListeners()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    sideMenu.visibility == View.VISIBLE -> {
                        sideMenu.visibility = View.GONE
                        showOverlayTemporarily()
                    }
                    homeContainer.visibility != View.VISIBLE -> returnToPreviousInterface()
                    else -> finish()
                }
            }
        })
        applySafeAreaInsets()
        updateCategoryFilter()
        renderHomeSection(HomeSection.INICIO)

        fetchRemoteChannels()
        fetchRemoteCatalog()
    }

    private fun fetchRemoteChannels() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val remoteChannelsDto = RetrofitClient.apiService.getChannels()

                val updatedChannels = remoteChannelsDto.map(::toChannel)

                withContext(Dispatchers.Main) {
                    if (updatedChannels.isNotEmpty()) {
                        ChannelRepository.channels = updatedChannels
                        updateCategoryFilter()
                        if (homeContainer.visibility == View.VISIBLE) {
                            renderHomeSection(selectedHomeSection)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchRemoteCatalog() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val remoteCatalog = RetrofitClient.apiService.getCatalog()
                withContext(Dispatchers.Main) {
                    catalog = remoteCatalog
                    if (homeContainer.visibility == View.VISIBLE) {
                        renderHomeSection(selectedHomeSection)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun loadBundledChannels() {
        try {
            val bundledChannels = assets.open("channels.json").bufferedReader().use { reader ->
                Gson().fromJson(reader, Array<ChannelDto>::class.java).toList()
            }
            if (bundledChannels.isNotEmpty()) {
                ChannelRepository.channels = bundledChannels.map(::toChannel)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadBundledCatalog() {
        try {
            catalog = assets.open("catalog.json").bufferedReader().use { reader ->
                Gson().fromJson(reader, CatalogDto::class.java)
            } ?: CatalogDto()
        } catch (_: Exception) {
            catalog = CatalogDto()
        }
    }

    private fun toChannel(dto: ChannelDto) = Channel(
        id = dto.id,
        name = dto.name,
        categories = dto.categories.mapNotNull(Category::fromRemoteName),
        streamUrl = dto.streamUrl
    )

    private fun applySafeAreaInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { _, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutoutInsets = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val safeLeft = maxOf(systemInsets.left, cutoutInsets.left)
            val safeRight = maxOf(systemInsets.right, cutoutInsets.right)
            val safeInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            homeContainer.setPadding(safeInsets.left, safeInsets.top, safeInsets.right, safeInsets.bottom)
            overlayContainer.setPadding(
                safeLeft + dp(12),
                systemInsets.top + dp(4),
                safeRight + dp(12),
                systemInsets.bottom + dp(8)
            )

            val menuLayout = sideMenu.layoutParams as FrameLayout.LayoutParams
            val availableWidth = mainLayout.width - safeLeft - safeRight - dp(24)
            menuLayout.width = minOf(dp(320), availableWidth.takeIf { it > 0 } ?: dp(240))
            menuLayout.topMargin = systemInsets.top + dp(8)
            menuLayout.bottomMargin = systemInsets.bottom + dp(16)
            menuLayout.leftMargin = safeLeft + dp(12)
            menuLayout.rightMargin = safeRight + dp(12)
            sideMenu.layoutParams = menuLayout
            insets
        }
        ViewCompat.requestApplyInsets(mainLayout)
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
        setKeepScreenOn(true)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applySafeAreaInsets()
    }

    override fun onStop() {
        super.onStop()
        hideHandler.removeCallbacks(hideRunnable)
        setKeepScreenOn(false)
        releasePlayer()
        sideMenu.visibility = View.GONE
        overlayContainer.visibility = View.GONE
        homeContainer.visibility = View.VISIBLE
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer
        }
    }

    private fun setKeepScreenOn(keepScreenOn: Boolean) {
        playerView.keepScreenOn = keepScreenOn
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun updateCategoryFilter() {
        val categories = Category.values()
        val activeCategory = categories[currentCategoryIndex]
        filteredChannels = ChannelRepository.getChannelsByCategory(activeCategory)
        txtCategoryTitle.text = if (isVodPlayback) {
            "Episodios: $currentVodSeriesTitle"
        } else {
            "Categoría: ${activeCategory.displayName}"
        }
        setupMenuList()
    }

    private fun setupHomeNavigation() {
        HomeSection.values().forEach { section ->
            val tab = TextView(this).apply {
                text = when (section) {
                    HomeSection.INICIO -> "⌂\n${section.label}"
                    HomeSection.SERIES -> "▤\n${section.label}"
                    HomeSection.PELICULAS -> "▶\n${section.label}"
                    HomeSection.EN_VIVO -> "●\n${section.label}"
                }
                gravity = Gravity.CENTER
                textSize = 12f
                isFocusable = true
                isClickable = true
                setPadding(dp(4), dp(4), dp(4), dp(4))
                layoutParams = LinearLayout.LayoutParams(0, dp(64), 1f)
                setOnClickListener { renderHomeSection(section) }
            }
            homeBottomNav.addView(tab)
        }

        homeScroll.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    swipeStartX = event.x
                    swipeStartY = event.y
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.x - swipeStartX
                    val dy = event.y - swipeStartY
                    if (kotlin.math.abs(dx) > dp(72) && kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                        moveHomeSection(if (dx < 0) 1 else -1)
                    }
                }
            }
            false
        }
    }

    private fun moveHomeSection(delta: Int) {
        val sections = HomeSection.values()
        val nextIndex = (selectedHomeSection.ordinal + delta + sections.size) % sections.size
        renderHomeSection(sections[nextIndex])
    }

    private fun renderHomeSection(section: HomeSection) {
        selectedHomeSection = section
        currentVodItems = emptyList()
        currentVodIndex = -1
        currentVodSeriesTitle = ""
        isVodPlayback = false
        homeContent.removeAllViews()
        homeScroll.scrollTo(0, 0)
        when (section) {
            HomeSection.INICIO -> {
                homeTitle.text = "¿Qué quieres ver hoy?"
                homeSubtitle.text = "Canales, series y películas en un solo lugar"
                addHeroCard(
                    "ENTRETENIMIENTO PARA TI",
                    "Entra a TV en vivo, explora canales temáticos o descubre señales peruanas oficiales.",
                    "Explorar TV en vivo"
                ) { renderHomeSection(HomeSection.EN_VIVO) }
                addVodSeriesRow("Series para ver cuando quieras", catalog.series)
                addVodMovieRow("Películas a la carta", catalog.movies)
                addChannelRow("Series y anime", channelsFor(Category.ANIME, Category.DIBUJOS))
                addChannelRow("Películas", channelsFor(Category.PELICULAS))
                addOfficialPeruRow()
            }
            HomeSection.SERIES -> {
                homeTitle.text = "Series y anime"
                homeSubtitle.text = "Elige una serie o un canal temático"
                addVodSeriesRow("Series a la carta", catalog.series)
                if (catalog.series.isEmpty()) {
                    addInfoCard("Añade tus series", "Agrega series y episodios autorizados en catalog.json para verlos desde el inicio o elegir cualquier episodio.")
                }
                addChannelRow("Anime", channelsFor(Category.ANIME))
                addChannelRow("Dibujos y series", channelsFor(Category.DIBUJOS))
            }
            HomeSection.PELICULAS -> {
                homeTitle.text = "Películas"
                homeSubtitle.text = "Películas a la carta y canales de cine"
                addVodMovieRow("Películas para ver cuando quieras", catalog.movies)
                if (catalog.movies.isEmpty()) {
                    addInfoCard("Añade películas", "Agrega películas autorizadas en catalog.json para reproducirlas desde el principio.")
                }
                addChannelRow("Canales de películas", channelsFor(Category.PELICULAS))
            }
            HomeSection.EN_VIVO -> {
                homeTitle.text = "TV en vivo"
                homeSubtitle.text = "Elige un canal y empieza a verlo"
                addSectionTitle("Señales oficiales del Perú")
                addLinkRow(officialPeruLiveLinks)
                addChannelRow("Canales incluidos en el proyecto", ChannelRepository.channels)
            }
        }
        updateHomeTabStyles()
    }

    private fun addVodSeriesRow(title: String, series: List<SeriesDto>) {
        addSectionTitle(title)
        if (series.isEmpty()) {
            homeContent.addView(label("Todavía no hay series a la carta.", 14f, "#AAB4C8", false), verticalSpacing())
            return
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        series.forEach { item ->
            val card = verticalCard("#182235").apply {
                setPadding(dp(14), dp(14), dp(14), dp(14))
                isFocusable = true
                isClickable = true
                setOnClickListener { showSeriesEpisodes(item) }
            }
            addPoster(card, item.posterUrl, item.title)
            card.addView(label("SERIE · ${item.episodes.size} episodios", 10f, "#54E0C4", true))
            card.addView(label(item.title, 15f, "#FFFFFF", true).apply { setPadding(0, dp(10), 0, dp(8)) })
            card.addView(label(item.description ?: "Ver episodios", 12f, "#AAB4C8", false))
            row.addView(card, LinearLayout.LayoutParams(dp(210), dp(164)).apply { marginEnd = dp(10) })
        }
        homeContent.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }, verticalSpacing())
    }

    private fun addVodMovieRow(title: String, movies: List<MovieDto>) {
        addSectionTitle(title)
        if (movies.isEmpty()) {
            homeContent.addView(label("Todavía no hay películas a la carta.", 14f, "#AAB4C8", false), verticalSpacing())
            return
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        movies.forEach { movie ->
            val card = verticalCard("#182235").apply {
                setPadding(dp(14), dp(14), dp(14), dp(14))
                isFocusable = true
                isClickable = true
                setOnClickListener { playMovie(movie) }
            }
            addPoster(card, movie.posterUrl, movie.title)
            card.addView(label("PELÍCULA", 10f, "#54E0C4", true))
            card.addView(label(movie.title, 15f, "#FFFFFF", true).apply { setPadding(0, dp(10), 0, dp(8)) })
            card.addView(label(movie.description ?: "Reproducir desde el inicio", 12f, "#AAB4C8", false))
            row.addView(card, LinearLayout.LayoutParams(dp(210), dp(164)).apply { marginEnd = dp(10) })
        }
        homeContent.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }, verticalSpacing())
    }

    private fun addPoster(card: LinearLayout, posterUrl: String?, title: String) {
        if (posterUrl.isNullOrBlank()) return
        val poster = ImageView(this).apply {
            contentDescription = title
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = roundedBackground("#243146", 8)
        }
        Glide.with(this).load(posterUrl).centerCrop().into(poster)
        card.addView(poster, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(72)
        ).apply { bottomMargin = dp(8) })
    }

    private fun showSeriesEpisodes(series: SeriesDto) {
        currentVodSeriesTitle = series.title
        currentVodItems = series.episodes.map { episode ->
            PlaybackItem(episode.title, episode.streamUrl)
        }
        currentVodIndex = -1
        homeContent.removeAllViews()
        homeScroll.scrollTo(0, 0)
        homeTitle.text = series.title
        homeSubtitle.text = series.description ?: "Elige el episodio que quieras ver"
        val backButton = label("‹  Volver a series", 14f, "#54E0C4", true).apply {
            isFocusable = true
            isClickable = true
            setPadding(dp(8), dp(10), dp(8), dp(10))
            setOnClickListener { renderHomeSection(HomeSection.SERIES) }
        }
        homeContent.addView(backButton, verticalSpacing())
        addSectionTitle("Episodios")
        if (series.episodes.isEmpty()) {
            addInfoCard("Sin episodios", "Añade episodios con sus enlaces de reproducción en catalog.json.")
        } else {
            series.episodes.forEachIndexed { index, episode ->
                val episodeLabel = episode.episodeNumber?.let { "Episodio $it · ${episode.title}" } ?: episode.title
                val card = verticalCard("#182235").apply {
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                    isFocusable = true
                    isClickable = true
                    setOnClickListener { playVodItem(index) }
                }
                card.addView(label(episodeLabel, 15f, "#FFFFFF", true))
                card.addView(label("Reproducir desde el principio", 12f, "#AAB4C8", false).apply {
                    setPadding(0, dp(6), 0, 0)
                })
                homeContent.addView(card, verticalSpacing())
            }
        }
        updateHomeTabStyles()
    }

    private fun playMovie(movie: MovieDto) {
        currentVodSeriesTitle = "Películas"
        currentVodItems = listOf(PlaybackItem(movie.title, movie.streamUrl))
        currentVodIndex = 0
        isVodPlayback = true
        playVodItem(0)
    }

    private fun playVodItem(index: Int) {
        if (index !in currentVodItems.indices) return
        currentVodIndex = index
        isVodPlayback = true
        val item = currentVodItems[index]
        txtCategoryTitle.text = "Episodios: $currentVodSeriesTitle"
        startPlayback(item.streamUrl, item.title)
        setupMenuList()
    }

    private fun startPlayback(streamUrl: String, title: String) {
        if (streamUrl.isBlank()) return
        homeContainer.visibility = View.GONE
        sideMenu.visibility = View.GONE
        overlayContainer.visibility = View.VISIBLE
        setKeepScreenOn(true)
        player?.let { exoPlayer ->
            exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
        txtHeaderInfo.text = title
        showOverlayTemporarily()
    }

    private fun channelsFor(vararg categories: Category): List<Channel> =
        ChannelRepository.channels.filter { channel -> channel.categories.any { it in categories } }

    private fun addHeroCard(title: String, description: String, buttonLabel: String, action: () -> Unit) {
        val card = verticalCard("#172B3C").apply { setPadding(dp(20), dp(20), dp(20), dp(20)) }
        card.addView(label(title, 12f, "#54E0C4", true))
        card.addView(label(description, 16f, "#FFFFFF", true).apply { setPadding(0, dp(10), 0, dp(16)) })
        val button = label(buttonLabel, 14f, "#07131B", true).apply {
            gravity = Gravity.CENTER
            background = roundedBackground("#54E0C4", 14)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            isFocusable = true
            isClickable = true
            setOnClickListener { action() }
        }
        card.addView(button, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        homeContent.addView(card, verticalSpacing())
    }

    private fun addInfoCard(title: String, description: String) {
        val card = verticalCard("#172033").apply { setPadding(dp(16), dp(14), dp(16), dp(14)) }
        card.addView(label(title, 15f, "#FFFFFF", true))
        card.addView(label(description, 13f, "#AAB4C8", false).apply { setPadding(0, dp(6), 0, 0) })
        homeContent.addView(card, verticalSpacing())
    }

    private fun addSectionTitle(title: String) {
        homeContent.addView(label(title, 18f, "#FFFFFF", true).apply {
            setPadding(0, dp(12), 0, dp(10))
        }, verticalSpacing())
    }

    private fun addChannelRow(title: String, channels: List<Channel>) {
        addSectionTitle(title)
        if (channels.isEmpty()) {
            homeContent.addView(label("No hay canales en esta categoría todavía.", 14f, "#AAB4C8", false), verticalSpacing())
            return
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        channels.forEach { channel ->
            val card = verticalCard("#182235").apply {
                setPadding(dp(14), dp(14), dp(14), dp(14))
                isFocusable = true
                isClickable = true
                setOnClickListener { playChannel(channel) }
            }
            val categoryLabel = channel.categories.joinToString(" · ") { it.displayName.uppercase() }
                .ifBlank { "CANAL" }
            card.addView(label(categoryLabel, 10f, "#54E0C4", true))
            card.addView(label(channel.name, 15f, "#FFFFFF", true).apply { setPadding(0, dp(10), 0, dp(8)) })
            card.addView(label("Ver canal  ›", 12f, "#AAB4C8", false))
            row.addView(card, LinearLayout.LayoutParams(dp(190), dp(122)).apply { marginEnd = dp(10) })
        }
        val horizontalScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }
        homeContent.addView(horizontalScroll, verticalSpacing())
    }

    private fun addOfficialPeruRow() {
        addSectionTitle("Señales peruanas oficiales")
        addLinkRow(officialPeruLiveLinks)
    }

    private fun addLinkRow(links: List<OfficialLiveLink>) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        links.forEach { link ->
            val card = verticalCard("#182235").apply {
                setPadding(dp(14), dp(14), dp(14), dp(14))
                isFocusable = true
                isClickable = true
                setOnClickListener { openOfficialLink(link.url) }
            }
            card.addView(label("PERÚ · OFICIAL", 10f, "#54E0C4", true))
            card.addView(label(link.name, 15f, "#FFFFFF", true).apply { setPadding(0, dp(9), 0, dp(5)) })
            card.addView(label("${link.detail} · Abrir sitio oficial ↗", 11f, "#AAB4C8", false))
            row.addView(card, LinearLayout.LayoutParams(dp(210), dp(122)).apply { marginEnd = dp(10) })
        }
        val horizontalScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }
        homeContent.addView(horizontalScroll, verticalSpacing())
    }

    private fun openOfficialLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            addInfoCard("No se pudo abrir el enlace", "Revisa que el dispositivo tenga un navegador instalado.")
        }
    }

    private fun playChannel(channel: Channel) {
        currentVodItems = emptyList()
        currentVodIndex = -1
        currentVodSeriesTitle = ""
        isVodPlayback = false
        val matchingCategory = channel.categories.firstOrNull() ?: Category.TODOS
        currentCategoryIndex = Category.values().indexOf(matchingCategory).coerceAtLeast(0)
        updateCategoryFilter()
        val channelIndex = filteredChannels.indexOf(channel).coerceAtLeast(0)
        loadChannel(channelIndex)
    }

    private fun updateHomeTabStyles() {
        for (index in 0 until homeBottomNav.childCount) {
            val tab = homeBottomNav.getChildAt(index) as? TextView ?: continue
            val selected = HomeSection.values()[index] == selectedHomeSection
            tab.setTextColor(if (selected) Color.parseColor("#54E0C4") else Color.parseColor("#AAB4C8"))
        }
    }

    private fun verticalCard(color: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = roundedBackground(color, 16)
    }

    private fun label(text: String, size: Float, color: String, bold: Boolean): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(Color.parseColor(color))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun roundedBackground(color: String, radiusDp: Int) = GradientDrawable().apply {
        setColor(Color.parseColor(color))
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun verticalSpacing() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = dp(8) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun loadChannel(indexInFilter: Int) {
        if (filteredChannels.isEmpty()) return

        currentChannelIndex = indexInFilter % filteredChannels.size
        if (currentChannelIndex < 0) currentChannelIndex = filteredChannels.size - 1

        val channel = filteredChannels[currentChannelIndex]

        val categoriesStr = channel.categories.joinToString(", ") { it.displayName }
        startPlayback(channel.streamUrl, "${channel.name}  •  [$categoriesStr]")
    }

    private fun changeCategory(delta: Int) {
        val categories = Category.values()
        currentCategoryIndex = (currentCategoryIndex + delta + categories.size) % categories.size
        updateCategoryFilter()
        loadChannel(0)
    }

    private fun setupListeners() {
        btnPrevCategory.setOnClickListener {
            if (isVodPlayback) playVodItem(currentVodIndex - 1) else changeCategory(-1)
        }
        btnNextCategory.setOnClickListener {
            if (isVodPlayback) playVodItem(currentVodIndex + 1) else changeCategory(1)
        }
        btnMenu.setOnClickListener { toggleSideMenu() }

        listChannels.setOnItemClickListener { _, _, position, _ ->
            if (isVodPlayback) playVodItem(position) else loadChannel(position)
            sideMenu.visibility = View.GONE
            showOverlayTemporarily()
        }

        mainLayout.setOnClickListener {
            if (homeContainer.visibility != View.VISIBLE) showOverlayTemporarily()
        }
    }

    private fun showOverlayTemporarily() {
        overlayContainer.visibility = View.VISIBLE
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, 5000)
    }

    private fun returnToPreviousInterface() {
        hideHandler.removeCallbacks(hideRunnable)
        player?.stop()
        sideMenu.visibility = View.GONE
        overlayContainer.visibility = View.GONE
        homeContainer.visibility = View.VISIBLE
        setKeepScreenOn(false)
    }

    private fun toggleSideMenu() {
        if (sideMenu.visibility == View.VISIBLE) {
            sideMenu.visibility = View.GONE
            showOverlayTemporarily()
        } else {
            sideMenu.visibility = View.VISIBLE
            overlayContainer.visibility = View.VISIBLE
            hideHandler.removeCallbacks(hideRunnable)

            val selectedIndex = if (isVodPlayback) currentVodIndex else currentChannelIndex
            if (selectedIndex >= 0) {
                listChannels.setItemChecked(selectedIndex, true)
                listChannels.setSelection(selectedIndex)
            }
            listChannels.requestFocus()
        }
    }

    private fun setupMenuList() {
        val items = if (isVodPlayback) currentVodItems.map { it.title } else filteredChannels.map { it.name }
        if (isVodPlayback) txtCategoryTitle.text = "Episodios: $currentVodSeriesTitle"
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        listChannels.adapter = adapter
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (homeContainer.visibility != View.VISIBLE) showOverlayTemporarily()
        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (homeContainer.visibility == View.VISIBLE) {
            return super.onKeyDown(keyCode, event)
        }
        showOverlayTemporarily()

        if (sideMenu.visibility == View.VISIBLE) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                toggleSideMenu()
                return true
            }
            return super.onKeyDown(keyCode, event)
        }

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (isVodPlayback) playVodItem(currentVodIndex - 1) else loadChannel(currentChannelIndex - 1)
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (isVodPlayback) playVodItem(currentVodIndex + 1) else loadChannel(currentChannelIndex + 1)
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (isVodPlayback) seekBy(-10_000L) else changeCategory(-1)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isVodPlayback) seekBy(10_000L) else changeCategory(1)
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                player?.let { if (it.isPlaying) it.pause() else it.play() }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun seekBy(offsetMs: Long) {
        val exoPlayer = player ?: return
        val targetPosition = (exoPlayer.currentPosition + offsetMs).coerceAtLeast(0L)
        val duration = exoPlayer.duration
        exoPlayer.seekTo(if (duration > 0L) minOf(targetPosition, duration) else targetPosition)
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }
}
