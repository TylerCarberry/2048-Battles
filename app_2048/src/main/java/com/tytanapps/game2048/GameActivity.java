package com.tytanapps.game2048;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ShareActionProvider;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.google.example.games.basegameutils.GameHelper;

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
			Intent showSettings = new Intent(this, com.tytanapps.game2048.SettingsActivity.class);
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

    public static class AdFragment extends Fragment {
        private AdView mAdView;

        public AdFragment() {
        }

        @Override
        public void onActivityCreated(Bundle bundle) {
            super.onActivityCreated(bundle);

            // Gets the ad view defined in layout/ad_fragment.xml with ad unit ID set in
            // values/strings.xml.
            mAdView = (AdView) getView().findViewById(R.id.adView);

            // The current version of AdMob breaks rotation animations on some devices.
            // Setting the layer type of the ad to Software or Hardware fixes it.
            // https://code.google.com/p/android/issues/detail?id=70914
            mAdView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            // Create an ad request.
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .addTestDevice(getString(R.string.test_device_id))
                    .build();

            // Start loading the ad in the background.
            mAdView.loadAd(adRequest);
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(int errorCode) {
                    mAdView.setVisibility(View.GONE);
                    super.onAdFailedToLoad(errorCode);
                }
            });
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_ad, container, false);
        }

        /** Called when leaving the activity */
        @Override
        public void onPause() {
            if (mAdView != null) {
                mAdView.pause();
            }
            super.onPause();
        }

        /** Called when returning to the activity */
        @Override
        public void onResume() {
            super.onResume();
            if (mAdView != null) {
                mAdView.resume();
            }
        }

        /** Called before the activity is destroyed */
        @Override
        public void onDestroy() {
            if (mAdView != null) {
                mAdView.destroy();
            }
            super.onDestroy();
        }
    }
}