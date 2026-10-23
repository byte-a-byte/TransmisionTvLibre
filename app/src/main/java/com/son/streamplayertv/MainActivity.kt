package com.son.streamplayertv

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity // <-- Importación Corregida
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() { // <-- Herencia Corregida

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

    private var currentCategoryIndex = 0
    private var currentChannelIndex = 0
    private var filteredChannels: List<Channel> = emptyList()

    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable {
        if (sideMenu.visibility != View.VISIBLE) {
            overlayContainer.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        setupListeners()
        updateCategoryFilter()

        // Descarga la lista actualizada desde GitHub al iniciar
        fetchRemoteChannels()
    }

    private fun fetchRemoteChannels() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val remoteChannelsDto = RetrofitClient.apiService.getChannels()

                val updatedChannels = remoteChannelsDto.map { dto ->
                    Channel(
                        id = dto.id,
                        name = dto.name,
                        categories = dto.categories.mapNotNull { catName ->
                            try { Category.valueOf(catName) } catch (e: Exception) { null }
                        },
                        streamUrl = dto.streamUrl
                    )
                }

                withContext(Dispatchers.Main) {
                    if (updatedChannels.isNotEmpty()) {
                        ChannelRepository.channels = updatedChannels
                        updateCategoryFilter()
                        loadChannel(0)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace() // Si falla la red, conserva la lista local de ChannelRepository
            }
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer
            loadChannel(0)
        }
    }

    private fun updateCategoryFilter() {
        val categories = Category.values()
        val activeCategory = categories[currentCategoryIndex]
        filteredChannels = ChannelRepository.getChannelsByCategory(activeCategory)
        txtCategoryTitle.text = "Categoría: ${activeCategory.displayName}"
        setupMenuList()
    }

    private fun loadChannel(indexInFilter: Int) {
        if (filteredChannels.isEmpty()) return

        currentChannelIndex = indexInFilter % filteredChannels.size
        if (currentChannelIndex < 0) currentChannelIndex = filteredChannels.size - 1

        val channel = filteredChannels[currentChannelIndex]

        player?.let { exoPlayer ->
            val mediaItem = MediaItem.fromUri(channel.streamUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }

        val categoriesStr = channel.categories.joinToString(", ") { it.displayName }
        txtHeaderInfo.text = "${channel.name}  •  [$categoriesStr]"
        showOverlayTemporarily()
    }

    private fun changeCategory(delta: Int) {
        val categories = Category.values()
        currentCategoryIndex = (currentCategoryIndex + delta + categories.size) % categories.size
        updateCategoryFilter()
        loadChannel(0)
    }

    private fun setupListeners() {
        btnPrevCategory.setOnClickListener { changeCategory(-1) }
        btnNextCategory.setOnClickListener { changeCategory(1) }
        btnMenu.setOnClickListener { toggleSideMenu() }

        listChannels.setOnItemClickListener { _, _, position, _ ->
            loadChannel(position)
            toggleSideMenu()
        }

        mainLayout.setOnClickListener { showOverlayTemporarily() }
    }

    private fun showOverlayTemporarily() {
        overlayContainer.visibility = View.VISIBLE
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, 5000)
    }

    private fun toggleSideMenu() {
        if (sideMenu.visibility == View.VISIBLE) {
            sideMenu.visibility = View.GONE
            showOverlayTemporarily()
        } else {
            sideMenu.visibility = View.VISIBLE
            overlayContainer.visibility = View.VISIBLE
            hideHandler.removeCallbacks(hideRunnable)

            listChannels.setItemChecked(currentChannelIndex, true)
            listChannels.setSelection(currentChannelIndex)
            listChannels.requestFocus()
        }
    }

    private fun setupMenuList() {
        val channelNames = filteredChannels.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, channelNames)
        listChannels.adapter = adapter
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        showOverlayTemporarily()
        return super.onGenericMotionEvent(event)
    }

    @SuppressLint("all")
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        showOverlayTemporarily()

        if (sideMenu.visibility == View.VISIBLE) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                toggleSideMenu()
                return true
            }
            return super.onKeyDown(keyCode, event)
        }

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> { loadChannel(currentChannelIndex - 1); true } // <-- Asegúrate del guion '-' aquí también
            KeyEvent.KEYCODE_DPAD_DOWN -> { loadChannel(currentChannelIndex + 1); true }
            KeyEvent.KEYCODE_DPAD_LEFT -> { changeCategory(-1); true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { changeCategory(1); true }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                player?.let { if (it.isPlaying) it.pause() else it.play() }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }
}