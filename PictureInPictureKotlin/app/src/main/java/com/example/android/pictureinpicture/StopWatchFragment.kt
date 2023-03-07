package com.example.android.pictureinpicture

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.android.pictureinpicture.databinding.StopwatchFragmentBinding

class StopWatchFragment: Fragment() {

    private var _binding: StopwatchFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = StopwatchFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        // Event handlers
        binding.clear.setOnClickListener { viewModel.clear() }
        binding.startOrPause.setOnClickListener { viewModel.startOrPause(ACTION_STOPWATCH_CONTROL) }
        binding.pip.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pipParams = updatePictureInPictureParams(viewModel.started.value == true)
                pipParams?.let {
                    activity?.enterPictureInPictureMode(it)
                }
            }
        }

        binding.pip.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        binding.pipAndroidVersionError.visibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) View.GONE else View.VISIBLE

        binding.switchExample.setOnClickListener {
            findNavController().navigate(R.id.action_stopwatch_to_movie)
        }
        // Observe data from the viewModel.
        viewModel.time.observe(viewLifecycleOwner) { time -> binding.time.text = time }
        viewModel.started.observe(viewLifecycleOwner) { started ->
            binding.startOrPause.setImageResource(
                if (started) R.drawable.ic_pause_24dp else R.drawable.ic_play_arrow_24dp
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                updatePictureInPictureParams(started)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            // Hide in-app buttons. They cannot be interacted in the picture-in-picture mode, and
            // their features are provided as the action icons.
            binding.clear.visibility = View.GONE
            binding.startOrPause.visibility = View.GONE
        } else {
            binding.clear.visibility = View.VISIBLE
            binding.startOrPause.visibility = View.VISIBLE
        }
    }

    /**
     * Updates the parameters of the picture-in-picture mode for this activity based on the current
     * [started] state of the stopwatch.
     */
    private fun updatePictureInPictureParams(started: Boolean): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val visibleRect = Rect()
            binding.stopwatchBackground.getGlobalVisibleRect(visibleRect)
            val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                getPictureInPictureForS(started, visibleRect)
            else
                getPictureInPictureBelowS(started, visibleRect)
            activity?.setPictureInPictureParams(params)
            return params
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getPictureInPictureForS(started: Boolean, visibleRect: Rect): PictureInPictureParams {
        return PictureInPictureParams.Builder()
            // Set action items for the picture-in-picture mode. These are the only custom controls
            // available during the picture-in-picture mode.
            .setActions(
                listOf(
                    // "Clear" action.
                    createRemoteAction(
                        R.drawable.ic_refresh_24dp,
                        R.string.clear,
                        REQUEST_CLEAR,
                        CONTROL_TYPE_CLEAR
                    ),
                    getStartPauseRemoteAction(started)
                )
            )
            // Set the aspect ratio of the picture-in-picture mode.
            .setAspectRatio(Rational(16, 9))
            // Specify the portion of the screen that turns into the picture-in-picture mode.
            // This makes the transition animation smoother.
            .setSourceRectHint(visibleRect)
            // Turn the screen into the picture-in-picture mode if it's hidden by the "Home" button.
            .setAutoEnterEnabled(true)
            // Disables the seamless resize. The seamless resize works great for videos where the
            // content can be arbitrarily scaled, but you can disable this for non-video content so
            // that the picture-in-picture mode is resized with a cross fade animation.
            .setSeamlessResizeEnabled(false)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getPictureInPictureBelowS(started: Boolean, visibleRect: Rect): PictureInPictureParams {
        return PictureInPictureParams.Builder()
            .setActions(
                listOf(
                    createRemoteAction(
                        R.drawable.ic_refresh_24dp,
                        R.string.clear,
                        REQUEST_CLEAR,
                        CONTROL_TYPE_CLEAR
                    ),
                    getStartPauseRemoteAction(started)
                )
            )
            .setAspectRatio(Rational(16, 9))
            .setSourceRectHint(visibleRect)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getStartPauseRemoteAction(started: Boolean): RemoteAction {
        return createRemoteAction(
            if (started) R.drawable.ic_pause_24dp else R.drawable.ic_play_arrow_24dp,
            if (started) R.string.pause else R.string.start,
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
                Intent(ACTION_STOPWATCH_CONTROL)
                    .putExtra(EXTRA_CONTROL_TYPE, controlType),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
    }
}