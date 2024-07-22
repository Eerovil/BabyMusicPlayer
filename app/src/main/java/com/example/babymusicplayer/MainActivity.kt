package com.example.babymusicplayer

import SongAdapter
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.GridView
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {
    private val TAG: String = "MyActivity"

    private val REQUEST_PERMISSION = 1
    private lateinit var songGridView: GridView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val songsList = mutableListOf<Song>()
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Log to logcat
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main)

        songGridView = findViewById(R.id.songGridView)

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)

        swipeRefreshLayout.setOnRefreshListener {
            loadSongs()
        }
        loadSongs()
        songGridView.setOnItemClickListener { _, _, position, _ ->
            playSong(position)
        }
    }

    @SuppressLint("Range")
    private fun loadSongs() {
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM
        )
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)

        data class SongMetadata(val title: String, val albumTitle: String, val artistName: String)

        val metadataBySongId = mutableMapOf<Long, SongMetadata>()

        songsList.clear()
        cursor?.use { it ->
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndex(MediaStore.Audio.Media._ID))
                val title = it.getString(it.getColumnIndex(MediaStore.Audio.Media.TITLE))
                val albumId = it.getLong(it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                var artistName = ""
                try {
                    artistName = it.getString(it.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                    Log.d(TAG, "Artist name: $artistName")
                } catch (e: Exception) {
                    Log.d(TAG, "Error getting artist name")
                }
                var albumName = ""
                try {
                    albumName = it.getString(it.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                    Log.d(TAG, "Album name: $albumName")
                } catch (e: Exception) {
                    Log.d(TAG, "Error getting album name")
                }
                val albumArtUri = getAlbumArtUri(albumId)
                metadataBySongId[id] = SongMetadata(title, albumName, artistName)
                songsList.add(Song(id, title, albumArtUri))
            }
        }

        // Sort the songs by artist, album, title
        songsList.sortWith(compareBy({ metadataBySongId[it.id]?.artistName ?: "" },
            { metadataBySongId[it.id]?.albumTitle ?: "" },
            { metadataBySongId[it.id]?.title ?: "" }))

        val songAdapter = SongAdapter(this, songsList)
        songGridView.adapter = songAdapter
        swipeRefreshLayout.isRefreshing = false
    }

    fun albumArtExists(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.close()
            true
        } catch (e: Exception) {
            false
        }
    }
    fun getAlbumArtUri(albumID: Long): Uri? {
        val sArt = Uri.parse("content://media/external/audio/albumart")
        val uri = ContentUris.withAppendedId(sArt, albumID)

        if (!albumArtExists(this, uri)) {
            Log.d(TAG, "Album art does not exist for album ID: $albumID")
            return null
        }

        Log.d(TAG, "Album art uri: $uri")
        return uri

    }

    private fun playSong(position: Int) {
        // Find currently playing song
        val songAdapter = songGridView.adapter as SongAdapter
        val currentlyPlayingPosition = songAdapter.getCurrentlyPlayingPosition()
        // If the new song is the same as the currently playing song, stop
        mediaPlayer?.release()

        if (currentlyPlayingPosition == position) {
            songAdapter.setCurrentlyPlayingPosition(-1)
            return
        }
        val song = songsList[position]
        val songUri: Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        mediaPlayer = MediaPlayer.create(this, songUri)
        mediaPlayer?.start()

        songAdapter.setCurrentlyPlayingPosition(position)

        // Clear on completion listener
        mediaPlayer?.setOnCompletionListener(null)
        // When the song finishes, play the next song
        if (position < songsList.size - 1) {
            mediaPlayer?.setOnCompletionListener {
                playSong(position + 1)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
