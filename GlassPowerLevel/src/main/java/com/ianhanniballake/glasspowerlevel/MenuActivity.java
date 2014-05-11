package com.ianhanniballake.glasspowerlevel;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.glass.media.Sounds;

/**
 * Activity showing the options menu.
 */
public class MenuActivity extends Activity {
    private final Handler mHandler = new Handler();

    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        openOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        boolean playAudio = mSharedPreferences.getBoolean(PowerLevelService.PREF_PLAY_AUDIO, true);
        MenuItem mute = menu.findItem(R.id.menu_mute);
        mute.setVisible(playAudio);
        MenuItem unmute = menu.findItem(R.id.menu_unmute);
        unmute.setVisible(!playAudio);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        // Handle item selection.
        switch (item.getItemId()) {
            case R.id.menu_mute:
                mSharedPreferences.edit().putBoolean(PowerLevelService.PREF_PLAY_AUDIO, false).commit();
                audioManager.playSoundEffect(Sounds.SUCCESS);
                return true;
            case R.id.menu_unmute:
                mSharedPreferences.edit().putBoolean(PowerLevelService.PREF_PLAY_AUDIO, true).commit();
                audioManager.playSoundEffect(Sounds.SUCCESS);
                return true;
            case R.id.menu_stop:
                // Stop the service at the end of the message queue for proper options menu
                // animation. This is only needed when starting a new Activity or stopping a Service
                // that published a LiveCard.
                post(new Runnable() {
                    @Override
                    public void run() {
                        stopService(new Intent(MenuActivity.this, PowerLevelService.class));
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        finish();
    }

    /**
     * Posts a {@link Runnable} at the end of the message loop, overridable for testing.
     */
    protected void post(Runnable runnable) {
        mHandler.post(runnable);
    }
}
