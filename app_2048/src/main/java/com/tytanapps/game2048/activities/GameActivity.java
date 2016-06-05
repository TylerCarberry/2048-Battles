package com.tytanapps.game2048.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ShareActionProvider;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.google.example.games.basegameutils.GameHelper;
import com.tytanapps.game2048.R;

public class GameActivity extends BaseGameActivity {
	
	private final static String LOG_TAG = GameActivity.class.getSimpleName();

    public static final String GITHUB_URL = "https://github.com/TylerCarberry/2048-for-Android";
    public static final String APP_URL = "https://play.google.com/store/apps/details?id=com.tytanapps.game2048";

    // Handles the share button in the menu bar
    private ShareActionProvider mShareActionProvider;

    // A full screen ad that is show after the game is lost
    private InterstitialAd interstitial;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);

        if (mHelper == null) {
            getGameHelper();
        }

        // Create the interstitial ad.
        interstitial = new InterstitialAd(this);
        interstitial.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice(getString(R.string.test_device_id))
                .build();

        // Begin loading the interstitial ad. It is not show until the game is isGameLost.
        interstitial.loadAd(adRequest);
	}

    public GameHelper getGameHelper() {
        if (mHelper == null) {
            mHelper = new GameHelper(this, mRequestedClients);
            mHelper.enableDebugLog(mDebugLog);
        }
        return mHelper;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here
		int id = item.getItemId();

		// When the settings menu item is pressed switch to SettingsActivity
		if(id == R.id.action_settings) {
			Intent showSettings = new Intent(this, SettingsActivity.class);
			startActivity(showSettings);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

    /**
     * Display the full screen interstitial ad
     */
    public void displayInterstitial() {
        if (interstitial.isLoaded()) {
            interstitial.show();
        }
    }

    @Override
    public void onSignInFailed() {}
    @Override
    public void onSignInSucceeded() {}

}