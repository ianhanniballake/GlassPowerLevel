package com.ianhanniballake.glasspowerlevel;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.widget.RemoteViews;

public class PowerLevelService extends Service {
    public static final String PREF_PLAY_AUDIO = "pref_play_audio";

    private static final String LIVE_CARD_TAG = "power_level";
    private static final long DELAY_MILLIS = DateUtils.MINUTE_IN_MILLIS;

    private LiveCard mLiveCard;
    private RemoteViews mLiveCardView;

    private CountDownTimer mCountDownTimer = new CountDownTimer(DELAY_MILLIS, DELAY_MILLIS) {
        @Override
        public void onTick(final long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            updateLiveCard();
            mCountDownTimer.start();
        }
    };

    private void updateLiveCard() {
        IntentFilter batteryChangedFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, batteryChangedFilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int batteryPct = (int)(level / (float)scale * 100);
        CharSequence powerLevel;
        switch (status) {
            case BatteryManager.BATTERY_STATUS_FULL:
                powerLevel = getString(R.string.battery_level_full);
                break;
            case BatteryManager.BATTERY_STATUS_CHARGING:
                powerLevel = getString(R.string.battery_level_charging, batteryPct);
                break;
            default:
                powerLevel = getString(R.string.battery_level, batteryPct);
        }
        mLiveCardView.setTextViewText(R.id.power_level, powerLevel);
        mLiveCard.setViews(mLiveCardView);
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mLiveCard == null) {
            mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
            mLiveCardView = new RemoteViews(getPackageName(), R.layout.power_level);

            // Set up the CountDownTimer and the remote view
            mCountDownTimer.onFinish();

            // Create the required menu activity intent
            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));

            // Publish the live card
            mLiveCard.publish(PublishMode.REVEAL);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (sharedPreferences.getBoolean(PREF_PLAY_AUDIO, true)) {
                playAudio();
            }
        } else {
            // Make sure we have fresh data
            updateLiveCard();
            mLiveCard.navigate();
        }
        return START_STICKY;
    }

    private void playAudio() {
        final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        final AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(final int focusChange) {
                }
            };
        int result = audioManager.requestAudioFocus(
                onAudioFocusChangeListener,
                AudioManager.USE_DEFAULT_STREAM_TYPE,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.over_9000);
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(final MediaPlayer mp) {
                    mp.release();
                    audioManager.abandonAudioFocus(onAudioFocusChangeListener);
                }
            });
            mediaPlayer.start();
        }
    }

    @Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {
            // Stop the timer
            mCountDownTimer.cancel();

            mLiveCard.unpublish();
            mLiveCard = null;
        }
        super.onDestroy();
    }
}