package com.example.mediaplayer

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.core.app.NotificationCompat

class MusicService : Service() {

    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null
    private val notificationHelper by lazy { NotificationHelper(this) }

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    private val _currentTrack = MutableLiveData<Track?>()
    val currentTrack: LiveData<Track?> get() = _currentTrack

    private val trackQueue = mutableListOf<Track>()
    private var currentIndex = 0

    private val handler = Handler()

    override fun onCreate() {
        super.onCreate()

        // Create Notification Channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    NotificationHelper.CHANNEL_ID,
                    "Music Playback",
                    NotificationManager.IMPORTANCE_LOW
                )
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_PLAY_PAUSE -> togglePlayPause()
                ACTION_NEXT -> playNextTrack()
                ACTION_PREV -> playPreviousTrack()
            }
        }
        return START_STICKY
    }

    fun setTrackQueue(tracks: List<Track>) {
        trackQueue.clear()
        trackQueue.addAll(tracks)
        if (tracks.isNotEmpty()) {
            playTrack(tracks[0])
        }
    }

    fun playTrack(track: Track) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(track.path)
            prepare()
            start()
            setOnCompletionListener { playNextTrack() }
        }
        currentIndex = trackQueue.indexOf(track)
        _currentTrack.postValue(track)
        _isPlaying.postValue(true)
        showNotification(track, true)
    }

    fun playNextTrack() {
        if (trackQueue.isNotEmpty()) {
            currentIndex = (currentIndex + 1) % trackQueue.size
            playTrack(trackQueue[currentIndex])
        }
    }

    fun playPreviousTrack() {
        if (trackQueue.isNotEmpty()) {
            currentIndex = (currentIndex - 1 + trackQueue.size) % trackQueue.size
            playTrack(trackQueue[currentIndex])
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.postValue(false)
            } else {
                it.start()
                _isPlaying.postValue(true)
            }
        }
        _currentTrack.value?.let { track ->
            showNotification(track, _isPlaying.value == true)
        }
    }

    private fun showNotification(track: Track, isPlaying: Boolean) {
        val playPauseIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, MediaPlayerReceiver::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getBroadcast(
            this,
            1,
            Intent(this, MediaPlayerReceiver::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = PendingIntent.getBroadcast(
            this,
            2,
            Intent(this, MediaPlayerReceiver::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = notificationHelper.createNotification(
            track,
            isPlaying,
            playPauseIntent,
            nextIntent,
            prevIntent
        )


        startForeground(NotificationHelper.NOTIFICATION_ID, notification)
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
    private val _currentPosition = MutableLiveData<Int>()
    val currentPosition: LiveData<Int> get() = _currentPosition

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }
    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.mediaplayer.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.mediaplayer.ACTION_NEXT"
        const val ACTION_PREV = "com.example.mediaplayer.ACTION_PREV"
    }
}
