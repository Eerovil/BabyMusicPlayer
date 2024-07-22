import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.babymusicplayer.R
import com.example.babymusicplayer.Song

class SongAdapter(private val context: Context, private val songs: List<Song>) : BaseAdapter() {

    private var currentlyPlayingPosition: Int = -1

    override fun getCount(): Int = songs.size

    override fun getItem(position: Int): Any = songs[position]

    override fun getItemId(position: Int): Long = position.toLong()

    fun setCurrentlyPlayingPosition(position: Int) {
        currentlyPlayingPosition = position
        notifyDataSetChanged() // Refresh the list to update the background
    }

    fun getCurrentlyPlayingPosition(): Int {
        return currentlyPlayingPosition
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.song_item, parent, false)
        val songTitle: TextView = view.findViewById(R.id.songTitle)
        val albumArt: ImageView = view.findViewById(R.id.albumArt)

        val song = songs[position]
        songTitle.text = song.title

        // Load album art using Glide or any other image loading library
        val albumArtUri = song.albumArtUri
        if (albumArtUri != null) {
            Glide.with(context).load(albumArtUri).into(albumArt)
        } else {
            Glide.with(context).load(R.drawable.default_album_art).into(albumArt) // default placeholder
        }

        // Update the background based on whether this is the currently playing song
        if (position == currentlyPlayingPosition) {
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.currently_playing_background))
        } else {
            // Unset the background color
            view.background = null
        }

        return view
    }

}
