/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.widget;

import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.android.launcher3.R;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MusicWidgetView extends FrameLayout {

    private TextView mTitleView;
    private TextView mArtistView;
    private ImageButton mPlayPauseButton;
    private MediaSessionManager mMediaSessionManager;
    private MediaController mMediaController;
    private Handler mUpdateHandler;
    private final Runnable mUpdateRunnable = this::updateMediaInfo;
    private static final long UPDATE_DELAY_MS = 500L;

    public MusicWidgetView(Context context) {
        super(context);
        init(context);
    }

    public MusicWidgetView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MusicWidgetView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setBackgroundColor(0xFF121212);
        setPadding(16, 16, 16, 16);

        mTitleView = new TextView(context);
        mTitleView.setTextColor(0xFFFFFFFF);
        mTitleView.setTextSize(16);
        mTitleView.setText(R.string.no_music);
        addView(mTitleView);

        mArtistView = new TextView(context);
        mArtistView.setTextColor(0xFFBDBDBD);
        mArtistView.setTextSize(14);
        mArtistView.setText("");
        addView(mArtistView);

        mPlayPauseButton = new ImageButton(context);
        mPlayPauseButton.setBackgroundColor(0xFF1F1F1F);
        mPlayPauseButton.setImageResource(android.R.drawable.ic_media_play);
        mPlayPauseButton.setOnClickListener(v -> togglePlayPause());
        addView(mPlayPauseButton);

        mUpdateHandler = new Handler(Looper.getMainLooper());

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mMediaSessionManager = (MediaSessionManager) context.getSystemService(
                        Context.MEDIA_SESSION_SERVICE);
                if (mMediaSessionManager != null) {
                    updateMediaInfo();
                }
            }
        } catch (Exception e) {
            // Safe: no crash if media session unavailable
        }
    }

    private void updateMediaInfo() {
        mUpdateHandler.removeCallbacks(mUpdateRunnable);

        if (mMediaController == null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                        && mMediaSessionManager != null) {
                    java.util.List<MediaController> controllers =
                            mMediaSessionManager.getActiveSessions(null);
                    if (!controllers.isEmpty()) {
                        mMediaController = controllers.get(0);
                    }
                }
            } catch (SecurityException | RuntimeException e) {
                // Safe: permissions may not be granted
            }
        }

        if (mMediaController != null) {
            try {
                MediaMetadata metadata = mMediaController.getMetadata();
                if (metadata != null) {
                    String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                    String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);

                    mTitleView.setText(title != null ? title : "Unknown Title");
                    mArtistView.setText(artist != null ? artist : "Unknown Artist");

                    int icon = mMediaController.getPlaybackState() != null
                            && mMediaController.getPlaybackState().getState()
                            == android.media.session.PlaybackState.STATE_PLAYING
                            ? android.R.drawable.ic_media_pause
                            : android.R.drawable.ic_media_play;
                    mPlayPauseButton.setImageResource(icon);
                } else {
                    resetView();
                }
            } catch (Exception e) {
                resetView();
            }
        } else {
            resetView();
        }

        mUpdateHandler.postDelayed(mUpdateRunnable, UPDATE_DELAY_MS);
    }

    private void resetView() {
        mTitleView.setText(R.string.no_music);
        mArtistView.setText("");
        mPlayPauseButton.setImageResource(android.R.drawable.ic_media_play);
        mMediaController = null;
    }

    private void togglePlayPause() {
        if (mMediaController != null && mMediaController.getTransportControls() != null) {
            try {
                int state = mMediaController.getPlaybackState() != null
                        ? mMediaController.getPlaybackState().getState()
                        : android.media.session.PlaybackState.STATE_STOPPED;

                if (state == android.media.session.PlaybackState.STATE_PLAYING) {
                    mMediaController.getTransportControls().pause();
                } else {
                    mMediaController.getTransportControls().play();
                }
                updateMediaInfo();
            } catch (Exception e) {
                // Safe: transport controls may not be available
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mUpdateHandler != null) {
            updateMediaInfo();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mUpdateHandler != null) {
            mUpdateHandler.removeCallbacks(mUpdateRunnable);
        }
    }
}
