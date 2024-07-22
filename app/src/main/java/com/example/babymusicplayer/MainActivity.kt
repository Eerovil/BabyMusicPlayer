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
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {
    private val TAG: String = "MyActivity"

    private val REQUEST_PERMISSION = 1
    private lateinit var songGridView: GridView
    private val songsList = mutableListOf<Song>()
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Log to logcat
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main)

        songGridView = findViewById(R.id.songGridView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "no permission")
            // If api level is 33 or higher, use this
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_PERMISSION)
            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_MEDIA_AUDIO), REQUEST_PERMISSION)
            }
        } else {
            loadSongs()
        }

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
            MediaStore.Audio.Media.ALBUM_ID
        )
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndex(MediaStore.Audio.Media._ID))
                val title = it.getString(it.getColumnIndex(MediaStore.Audio.Media.TITLE))
                val albumId = it.getLong(it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                val albumArtUri = getAlbumArtUri(albumId)
                songsList.add(Song(id, title, albumArtUri))
            }
        }

        val songAdapter = SongAdapter(this, songsList)
        songGridView.adapter = songAdapter
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
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                loadSongs()
            } else {
                Log.d(TAG, "Permission denied" + grantResults[0])
                Toast.makeText(this, "Permission denied" + grantResults[0], Toast.LENGTH_SHORT).show()
            }
        }
    }
}
