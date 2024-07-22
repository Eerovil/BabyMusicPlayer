package com.example.babymusicplayer

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.GridView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.babymusicplayer.R
import com.example.babymusicplayer.Song
import com.example.babymusicplayer.SongAdapter

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
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO), REQUEST_PERMISSION)
        } else {
            loadSongs()
        }

        songGridView.setOnItemClickListener { _, _, position, _ ->
            playSong(position)
        }
    }

    private fun loadSongs() {
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE)
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)

        // Log to logcat
        Log.d(TAG, "loadSongs");

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndex(MediaStore.Audio.Media._ID))
                val title = it.getString(it.getColumnIndex(MediaStore.Audio.Media.TITLE))
                songsList.add(Song(id, title))
            }
        }

        Log.d(TAG, "SongList: " + songsList);

        val songAdapter = SongAdapter(this, songsList)
        songGridView.adapter = songAdapter
    }

    private fun playSong(position: Int) {
        mediaPlayer?.release()
        val song = songsList[position]
        val songUri: Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        mediaPlayer = MediaPlayer.create(this, songUri)
        mediaPlayer?.start()

        Toast.makeText(this, "Playing: ${song.title}", Toast.LENGTH_SHORT).show()
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
