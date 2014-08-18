package com.tytanapps.game2048;

import java.io.IOException;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;
import android.util.Log;


public class TheBackupAgent extends BackupAgentHelper {
	
	final static String LOG_TAG = TheBackupAgent.class.getSimpleName();
	
	// The name of the SharedPreferences file
	public final String CURRENT_GAME_FILENAME = "CURRENT_GAME";

	// The name of the SharedPreferences file
	public final String GAME_STATS_FILENAME = "GAME_STATS";

	// The name of the SharedPreferences file
	public final String GAME_PREFS_FILENAME = "prefs";

	// A key to uniquely identify the set of backup data
	static final String FILES_BACKUP_KEY = "myfiles";

	// Allocate a helper and add it to the backup agent
	@Override
	public void onCreate() {
		
		Log.d(LOG_TAG, "on create backup agent");
		Log.d(LOG_TAG, CURRENT_GAME_FILENAME);
		Log.d(LOG_TAG, GAME_STATS_FILENAME);
		
		//FileBackupHelper helper = new FileBackupHelper(this, CURRENT_GAME_FILENAME, GAME_STATS_FILENAME);
        
		SharedPreferencesBackupHelper helper =
                new SharedPreferencesBackupHelper(this, GAME_PREFS_FILENAME);
		addHelper(FILES_BACKUP_KEY, helper);
	}

	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
			ParcelFileDescriptor newState) throws IOException {
		
		Log.d(LOG_TAG, "on backup");
		
		super.onBackup(oldState, data, newState);
	}
	
	@Override
	public void onRestore(BackupDataInput data, int appVersionCode,
			ParcelFileDescriptor newState) throws IOException {
		Log.d(LOG_TAG, "on restore");
		
		Log.d(LOG_TAG, data.toString());
		
		super.onRestore(data, appVersionCode, newState);
	}


}