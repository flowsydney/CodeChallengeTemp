package com.example.android.pictureinpicture

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.util.Linkify
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.android.pictureinpicture.databinding.MovieActivityBinding
import com.example.android.pictureinpicture.widget.MovieView
import kotlinx.android.synthetic.main.stopwatch_fragment.view.*
import kotlinx.android.synthetic.main.view_movie.*

class MovieFragment : Fragment() {

    private var _binding: MovieActivityBinding? = null
    private val binding get() = _binding!!
    private lateinit var session: MediaSessionCompat
    private val viewModel: MainViewModel by activityViewModels()
    private var isInPiP = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MovieActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Linkify.addLinks(binding.explanation, Linkify.ALL)
        binding.pip.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        binding.pip.setOnClickListener { minimize() }
        binding.pipAndroidVersionError.visibility =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) View.GONE else View.VISIBLE
        binding.switchExample.setOnClickListener {
            findNavController().navigate(R.id.action_movie_to_stopwatch)
        }
        viewModel.getMovieControlState().observe(viewLifecycleOwner) {
            when (it) {
                MovieControlState.HIDE_CONTROL -> {
                    binding.movie.hideControls()
                }
                else -> {
                    binding.movie.showControls()
                }
            }
        }
        // Configure parameters for the picture-in-picture mode. We do this at the first layout of
        // the MovieView because we use its layout position and size.
        binding.movie.doOnLayout {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                updatePictureInPictureParams()
            }
        }

        // Set up the video; it automatically starts.
        binding.movie.setMovieListener(movieListener)

        view.viewTreeObserver?.addOnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
                adjustFullScreen(resources.configuration)
            }
        }

        viewModel.getMovieState().observe(viewLifecycleOwner) { state ->
            if (isInPiP) {
                state?.let { safeState ->
                    when (safeState) {
                        MovieState.PLAY -> {
                            binding.movie.play()
                        }
                        MovieState.PAUSE -> {
                            binding.movie.pause()
                        }
                        MovieState.FASTFORWARD -> {
                            binding.movie.fastForward()
                        }
                        MovieState.REWIND -> {
                            binding.movie.fastRewind()
                        }
                    }
                    updatePictureInPictureParams()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        isInPiP = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            // Hide the controls in picture-in-picture mode.
            binding.movie.hideControls()
        } else {
            // Show the video controls if the video is not playing
            if (!binding.movie.isPlaying) {
                binding.movie.showControls()
            }
        }
    }

    private fun updatePictureInPictureParams(): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Calculate the aspect ratio of the PiP screen.
            val aspectRatio = Rational(binding.movie.width, binding.movie.height)
            // The movie view turns into the picture-in-picture mode.
            val visibleRect = Rect()
            binding.movie.getGlobalVisibleRect(visibleRect)
            val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .setActions(
                        listOf(
                            getRewindAction(),
                            getStartPauseRemoteAction(),
                            getForwardAction()
                        )
                    )
                    // Specify the portion of the screen that turns into the picture-in-picture mode.
                    // This makes the transition animation smoother.
                    .setSourceRectHint(visibleRect)
                    // The screen automatically turns into the picture-in-picture mode when it is hidden
                    // by the "Home" button.
                    .setAutoEnterEnabled(true)
                    .build()
            } else {
                PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    // Specify the portion of the screen that turns into the picture-in-picture mode.
                    // This makes the transition animation smoother.
                    .setActions(
                        listOf(
                            getRewindAction(),
                            getStartPauseRemoteAction(),
                            getForwardAction()
                        )
                    )
                    .setSourceRectHint(visibleRect)
                    // The screen automatically turns into the picture-in-picture mode when it is hidden
                    // by the "Home" button.
                    .build()
            }
            activity?.setPictureInPictureParams(params)
            return params
        }
        return null
    }

    /**
     * Enters Picture-in-Picture mode.
     */
    private fun minimize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pipParams = updatePictureInPictureParams()
            pipParams?.let {
                activity?.enterPictureInPictureMode(it)
            }
        }
    }

    /**
     * Adjusts immersive full-screen flags depending on the screen orientation.

     * @param config The current [Configuration].
     */
    private fun adjustFullScreen(config: Configuration) {
        val decorView = activity?.window?.decorView

        decorView?.let {
            val insetsController = ViewCompat.getWindowInsetsController(it)
            insetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                insetsController?.hide(WindowInsetsCompat.Type.systemBars())
                binding.scroll.visibility = View.GONE
                binding.movie.setAdjustViewBounds(false)
            } else {
                insetsController?.show(WindowInsetsCompat.Type.systemBars())
                binding.scroll.visibility = View.VISIBLE
                binding.movie.setAdjustViewBounds(true)
            }
        }
    }

    /**
     * Overloaded method that persists previously set media actions.

     * @param state The state of the video, e.g. playing, paused, etc.
     * @param position The position of playback in the video.
     * @param mediaId The media id related to the video in the media session.
     */
    private fun updatePlaybackState(
        @PlaybackStateCompat.State state: Int,
        position: Int,
        mediaId: Int
    ) {
        val actions = session.controller.playbackState.actions
        updatePlaybackState(state, actions, position, mediaId)
    }

    private fun updatePlaybackState(
        @PlaybackStateCompat.State state: Int,
        playbackActions: Long,
        position: Int,
        mediaId: Int
    ) {
        val builder = PlaybackStateCompat.Builder()
            .setActions(playbackActions)
            .setActiveQueueItemId(mediaId.toLong())
            .setState(state, position.toLong(), 1.0f)
        session.setPlaybackState(builder.build())
    }

    private val movieListener = object : MovieView.MovieListener() {

        override fun onMovieStarted() {
            // We are playing the video now. Update the media session state and the PiP window will
            // update the actions.
//            viewModel.setMovieState(MovieState.PLAY)
            updatePlaybackState(
                PlaybackStateCompat.STATE_PLAYING,
                binding.movie.getCurrentPosition(),
                binding.movie.getVideoResourceId()
            )
        }

        override fun onMovieStopped() {
            // The video stopped or reached its end. Update the media session state and the PiP
            // window will update the actions.
//            viewModel.setMovieState(MovieState.PAUSE)
            updatePlaybackState(
                PlaybackStateCompat.STATE_PAUSED,
                binding.movie.getCurrentPosition(),
                binding.movie.getVideoResourceId()
            )
        }

        override fun onMovieMinimized() {
            // The MovieView wants us to minimize it. We enter Picture-in-Picture mode now.
            minimize()
        }
    }

    override fun onStart() {
        super.onStart()
        initializeMediaSession()
    }

    private fun initializeMediaSession() {
        context?.let {
            session = MediaSessionCompat(it, TAG)
            session.isActive = true
            MediaControllerCompat.setMediaController(activity as Activity, session.controller)

            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, binding.movie.title)
                .build()
            session.setMetadata(metadata)

            session.setCallback(MediaSessionCallback(binding.movie))

            val state = if (binding.movie.isPlaying) {
                PlaybackStateCompat.STATE_PLAYING
            } else {
                PlaybackStateCompat.STATE_PAUSED
            }
            updatePlaybackState(
                state,
                MEDIA_ACTIONS_ALL,
                binding.movie.getCurrentPosition(),
                binding.movie.getVideoResourceId()
            )
        }
    }


    private fun checkPictureInPicture() =
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) && activity?.isInPictureInPictureMode == true

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustFullScreen(newConfig)
    }

    /**
     * Updates the [MovieView] based on the callback actions. <br></br>
     * Simulates a playlist that will disable actions when you cannot skip through the playlist in a
     * certain direction.
     */
    private inner class MediaSessionCallback(
        private val movieView: MovieView
    ) : MediaSessionCompat.Callback() {

        private var indexInPlaylist: Int = 1

        override fun onPlay() {
            movieView.play()
        }

        override fun onPause() {
            movieView.pause()
        }

        override fun onSkipToNext() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                movieView.startVideo()
                if (indexInPlaylist < PLAYLIST_SIZE) {
                    indexInPlaylist++
                    if (indexInPlaylist >= PLAYLIST_SIZE) {
                        updatePlaybackState(
                            PlaybackStateCompat.STATE_PLAYING,
                            MEDIA_ACTIONS_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS,
                            movieView.getCurrentPosition(),
                            movieView.getVideoResourceId()
                        )
                    } else {
                        updatePlaybackState(
                            PlaybackStateCompat.STATE_PLAYING,
                            MEDIA_ACTIONS_ALL,
                            movieView.getCurrentPosition(),
                            movieView.getVideoResourceId()
                        )
                    }
                }
            }
        }

        override fun onSkipToPrevious() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                movieView.startVideo()
                if (indexInPlaylist > 0) {
                    indexInPlaylist--
                    if (indexInPlaylist <= 0) {
                        updatePlaybackState(
                            PlaybackStateCompat.STATE_PLAYING,
                            MEDIA_ACTIONS_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT,
                            movieView.getCurrentPosition(),
                            movieView.getVideoResourceId()
                        )
                    } else {
                        updatePlaybackState(
                            PlaybackStateCompat.STATE_PLAYING,
                            MEDIA_ACTIONS_ALL,
                            movieView.getCurrentPosition(),
                            movieView.getVideoResourceId()
                        )
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getRewindAction(): RemoteAction {
        return createRemoteAction(
            R.drawable.ic_fast_rewind_24dp,
            R.string.fast_rewind,
            REQUEST_FAST_REWIND,
            CONTROL_TYPE_REWIND
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getForwardAction(): RemoteAction {
        return createRemoteAction(
            R.drawable.ic_fast_forward_24dp,
            R.string.fast_forward,
            REQUEST_FAST_FORWARD,
            CONTROL_TYPE_FORWARD
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getStartPauseRemoteAction(): RemoteAction {
        return createRemoteAction(
            if (binding.movie.isPlaying)  R.drawable.ic_pause_24dp else R.drawable.ic_play_arrow_24dp,
            if (binding.movie.isPlaying) R.string.pause  else R.string.start,
            REQUEST_START_OR_PAUSE,
            CONTROL_TYPE_START_OR_PAUSE
        )
    }

    /**
     * Creates a [RemoteAction]. It is used as an action icon on the overlay of the
     * picture-in-picture mode.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRemoteAction(
        @DrawableRes iconResId: Int,
        @StringRes titleResId: Int,
        requestCode: Int,
        controlType: Int
    ): RemoteAction {
        return RemoteAction(
            Icon.createWithResource(context, iconResId),
            getString(titleResId),
            getString(titleResId),
            PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(ACTION_MOVIE_CONTROL)
                    .putExtra(EXTRA_CONTROL_TYPE, controlType),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {

        private const val TAG = "MediaSessionPlaybackActivity"

        private const val MEDIA_ACTIONS_PLAY_PAUSE =
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE

        private const val MEDIA_ACTIONS_ALL =
            MEDIA_ACTIONS_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS

        private const val PLAYLIST_SIZE = 2
    }

}