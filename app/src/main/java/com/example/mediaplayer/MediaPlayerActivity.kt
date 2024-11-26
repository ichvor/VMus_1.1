package com.example.mediaplayer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.AnimatedVectorDrawable
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.mediaplayer.databinding.ActivityMediaPlayerBinding

class MediaPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaPlayerBinding
    private val sharedViewModel: SharedViewModel by viewModels()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        setupSeekBar()
    }

    private fun setupUI() {
        // Кнопка воспроизведения/паузы
        binding.playPauseButton.setOnClickListener {
            sharedViewModel.togglePlayPause()
        }

        // Кнопка следующего трека
        binding.nextButton.setOnClickListener {
            sharedViewModel.playNextTrack()
        }

        // Кнопка предыдущего трека
        binding.prevButton.setOnClickListener {
            sharedViewModel.playPreviousTrack()
        }

        // Кнопка сворачивания
        binding.collapseButton.setOnClickListener {
            finish() // Закрыть текущую активность
        }
    }

    private fun setupObservers() {
        // Обновление информации о текущем треке
        sharedViewModel.currentTrack.observe(this) { track ->
            binding.trackTitle.text = track?.title ?: "Unknown Title"
            binding.trackArtist.text = track?.artist ?: "Unknown Artist"

            // Обновление обложки трека
            val coverPath = track?.coverPath
            if (!coverPath.isNullOrEmpty()) {
                Glide.with(this)
                    .load(coverPath)
                    .apply(RequestOptions().transform(RoundedCorners(32))) // Применяем закругленные углы
                    .error(R.drawable.default_cover) // Если загрузка не удалась
                    .into(binding.trackCover)
            } else if (!track?.path.isNullOrEmpty()) {
                // Если обложка не указана, попробуем извлечь из файла
                val albumArt = getAlbumArt(track?.path ?: "")
                if (albumArt != null) {
                    binding.trackCover.setImageBitmap(albumArt)
                } else {
                    binding.trackCover.setImageResource(R.drawable.default_cover) // Загружаем обложку по умолчанию
                }
            } else {
                binding.trackCover.setImageResource(R.drawable.default_cover) // Загружаем обложку по умолчанию
            }
        }

        // Обновление состояния воспроизведения
        sharedViewModel.isPlaying.observe(this) { isPlaying ->
            togglePlayPauseAnimation(isPlaying)
        }
    }

    private fun setupSeekBar() {
        // Управление SeekBar
        binding.trackSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) sharedViewModel.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Обновление позиции воспроизведения
        handler.post(object : Runnable {
            override fun run() {
                val position = sharedViewModel.getCurrentPosition()
                val duration = sharedViewModel.getDuration()

                binding.trackSeekBar.progress = position
                binding.trackSeekBar.max = duration

                binding.elapsedTime.text = formatTime(position)
                binding.totalTime.text = formatTime(duration)

                handler.postDelayed(this, 500)
            }
        })
    }

    private fun togglePlayPauseAnimation(isPlaying: Boolean) {
        val drawable = if (isPlaying) {
            binding.playPauseButton.setImageResource(R.drawable.pause)
            binding.playPauseButton.drawable
        } else {
            binding.playPauseButton.setImageResource(R.drawable.play)
            binding.playPauseButton.drawable
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && drawable is AnimatedVectorDrawable) {
            drawable.start()
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun getAlbumArt(path: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val art = retriever.embeddedPicture
            if (art != null) BitmapFactory.decodeByteArray(art, 0, art.size) else null
        } catch (e: Exception) {
            Log.e("MediaPlayerActivity", "Error retrieving album art", e)
            null
        } finally {
            retriever.release()
        }
    }
}
