package com.tytanapps.game2048;

import android.app.Application;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;

public class MainApplication extends Application{

	@Override
    public void onCreate() {
        super.onCreate();
        getTracker(TrackerName.APP_TRACKER);
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
        analytics.setLogger(new Logger() {
            @Override
            public void verbose(String s) {
                Log.v("a",s);
            }

            @Override
            public void info(String s) {
                Log.i("a",s);
            }

            @Override
            public void warn(String s) {
                Log.w("a",s);
            }

            @Override
            public void error(String s) {
                Log.e("a",s);
            }

            @Override
            public void error(Exception e) {
                Log.v("a",e.toString());
            }

            @Override
            public void setLogLevel(int i) {

            }

            @Override
            public int getLogLevel() {
                return 0;
            }
        });
        analytics.enableAutoActivityReports(this);
    }



    /**
	 * Enum used to identify the tracker that needs to be used for tracking.
	 *
	 * A single tracker is usually enough for most purposes. In case you do need multiple trackers,
	 * storing them all in Application object helps ensure that they are created only once per
	 * application instance.
	 */
	public enum TrackerName {
		APP_TRACKER, // Tracker used only in this app.
		GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
		ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
    }

	HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

	synchronized Tracker getTracker(TrackerName trackerId) {
		if (!mTrackers.containsKey(trackerId)) {
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            analytics.enableAutoActivityReports(this);

            if(trackerId == TrackerName.APP_TRACKER) {
                Tracker t = analytics.newTracker(R.xml.app_tracker);
                t.enableAdvertisingIdCollection(true);
                mTrackers.put(trackerId, t);
            }
		}
		return mTrackers.get(trackerId);
	}
}
