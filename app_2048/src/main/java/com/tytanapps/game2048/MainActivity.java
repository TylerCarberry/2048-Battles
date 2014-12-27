package com.tytanapps.game2048;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.quest.Quests;
import com.google.android.gms.games.request.GameRequest;
import com.google.android.gms.games.request.Requests;
import com.google.example.games.basegameutils.BaseGameActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;


public class MainActivity extends BaseGameActivity {

    private final static int SEND_REQUEST_CODE = 1001;
    private final static int SEND_GIFT_CODE = 1002;
    private final static int SHOW_INBOX = 1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        // Give the user a bonus if a day has past since they last played
        addWelcomeBackBonus();

        updateInventoryTextView();
        checkIfQuestActive();
        checkPendingPlayGifts();

        super.onStart();
    }

    /**
     * Adds a power up or undo to the user's inventory if a day has passed since they last played
     * If the user changed the date to cheat they must wait 3 days
     */
    public void addWelcomeBackBonus() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // The number of days since the epoch
        long lastDatePlayed = prefs.getLong("lastDatePlayed", -1);
        long currentDate = TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().getTimeInMillis());

        if (currentDate > lastDatePlayed) {
            displayWelcomeBackBonus();
        }
        else
            // The time was changed
            if (currentDate < lastDatePlayed) {
                Toast.makeText(this, "You changed the date", Toast.LENGTH_LONG).show();

                // The user must wait another 3 days
                currentDate = lastDatePlayed + 3;
            }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("lastDatePlayed", currentDate);
        editor.commit();
    }

    private void displayWelcomeBackBonus() {
        // Create a new dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.welcome_back));

        // Add either 1 or 2 bonus items
        int bonusAmount = (int) (Math.random() * 2 + 1);

        try {
            if (Math.random() < 0.5) {
                //Toast.makeText(this, "1", Toast.LENGTH_SHORT).show();
                incrementPowerupInventory(bonusAmount);
                builder.setMessage("You Gained " + bonusAmount + " Powerup!\n" +
                        "Come back tomorrow for more.");
            }
            else {
                //Toast.makeText(this, "2", Toast.LENGTH_SHORT).show();
                incrementUndoInventory(bonusAmount);
                builder.setMessage("You Gained " + bonusAmount + " Undo!\n" +
                        "Come back tomorrow for more.");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.error_claim_bonus), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to access save file to add random bonus", Toast.LENGTH_LONG).show();
        }

        // Show the message to the player
        builder.create().show();
    }

    public void setQuestButtonEnabled(boolean enabled) {
        ImageButton questsButton = (ImageButton) findViewById(R.id.quests_button);
        questsButton.setImageResource((enabled) ? R.drawable.games_quests_green : R.drawable.games_quests);
    }

    public void updateInventoryTextView() {
        File gameDataFile = new File(getFilesDir(), getString(R.string.file_game_stats));
        GameData gameData = new GameData();
        try {
            gameData = (GameData) Save.load(gameDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        TextView undoTextView = (TextView) findViewById(R.id.undo_inventory);
        undoTextView.setText(""+gameData.getUndoInventory());

        TextView powerupTextView = (TextView) findViewById(R.id.powerup_inventory);
        powerupTextView.setText(""+gameData.getPowerupInventory());
    }

    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.single_player_imagebutton:
                startActivity(new Intent(this, SelectModeActivity.class));
                break;
            case R.id.multiplayer_imagebutton:
                Intent multiplayerIntent = new Intent(getBaseContext(), MultiplayerActivity.class);
                multiplayerIntent.putExtra("startMultiplayer", true);
                startActivity(multiplayerIntent);
                break;
            case R.id.help_button:
                showHelpDialog();
                break;
            case R.id.settings_button:
                Intent showSettings = new Intent(this, SettingsActivity.class);
                startActivity(showSettings);
                break;
        }
    }

    /**
     * Called when either the achievements, leaderboards, or quests buttons are pressed
     * @param view The button that was pressed
     */
    public void playGames(View view) {
        if(getApiClient().isConnected()) {
            switch (view.getId()) {
                case R.id.achievements_button:
                    startActivityForResult(Games.Achievements.getAchievementsIntent(getApiClient()), 1);
                    break;
                case R.id.quests_button:
                    showQuests();
                    break;
                case R.id.leaderboards_button:
                    startActivityForResult(Games.Leaderboards.getAllLeaderboardsIntent(getApiClient()), 0);
                    break;
                case R.id.gifts_button:
                    sendGift();
                    break;
                case R.id.inbox_button:
                    startActivityForResult(Games.Requests.getInboxIntent(getApiClient()), SHOW_INBOX);
                    break;
            }
        }
        else {
            Toast.makeText(this, getString(R.string.not_signed_in_error), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Display all quests
     */
    protected void showQuests() {
        // In the developer tutorial they use Quests.SELECT_ALL_QUESTS
        // but that is not valid for me. That may require an update
        // but for now selecting all possibilities works the same way
        int[] questParams = new int[8];
        questParams[0] = Games.Quests.SELECT_ACCEPTED;
        questParams[1] = Games.Quests.SELECT_OPEN;
        questParams[2] = Games.Quests.SELECT_ENDING_SOON;
        questParams[3] = Games.Quests.SELECT_UPCOMING;
        questParams[4] = Games.Quests.SELECT_COMPLETED;
        questParams[5] = Games.Quests.SELECT_COMPLETED_UNCLAIMED;
        questParams[6] = Games.Quests.SELECT_FAILED;
        questParams[7] = Games.Quests.SELECT_EXPIRED;

        Intent questsIntent = Games.Quests.getQuestsIntent(getApiClient(), questParams);

        // 0 is an arbitrary integer
        startActivityForResult(questsIntent, 0);
    }

    protected void showHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.how_to_play));

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        TextView textView = new TextView(this);
        textView.setText(getString(R.string.instructions_to_play));
        textView.setTextSize(20);
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setPadding(0, (int) getResources().getDimension(R.dimen.activity_horizontal_margin),
                0, (int) getResources().getDimension(R.dimen.activity_horizontal_margin));


        VideoView videoview = new VideoView(this);
        Uri uri = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.how_to_play_2048);

        videoview.setVideoURI(uri);
        videoview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        });
        videoview.setZOrderOnTop(true);
        videoview.start();

        linearLayout.addView(textView);
        linearLayout.addView(videoview);

        builder.setView(linearLayout);
        builder.create().show();
    }

    protected void sendGift() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.prompt_choose_gift)).setItems(R.array.gifts, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent ;
                // The 'which' argument contains the index position
                // of the selected item
                switch (which) {
                    // Powerup
                    case 0:
                        intent = Games.Requests.getSendIntent(getApiClient(), GameRequest.TYPE_GIFT,
                                "p".getBytes(), Requests.REQUEST_DEFAULT_LIFETIME_DAYS, BitmapFactory.decodeResource(getResources(),
                                        R.drawable.powerup_button), getString(R.string.powerup));
                        startActivityForResult(intent, SEND_GIFT_CODE);
                        break;
                    case 1:
                        intent = Games.Requests.getSendIntent(getApiClient(), GameRequest.TYPE_GIFT,
                                "u".getBytes(), Requests.REQUEST_DEFAULT_LIFETIME_DAYS, BitmapFactory.decodeResource(getResources(),
                                        R.drawable.undo_button), getString(R.string.undo));
                        startActivityForResult(intent, SEND_GIFT_CODE);
                        break;
                }
            }
        });
        builder.create().show();
    }

    protected void checkIfQuestActive() {
        PendingResult<Quests.LoadQuestsResult> s = Games.Quests.load(getApiClient(),
                new int[]{Games.Quests.SELECT_OPEN, Quests.SELECT_ACCEPTED},
                Quests.SORT_ORDER_ENDING_SOON_FIRST, false);

        s.setResultCallback(new ResultCallback<Quests.LoadQuestsResult>() {
            @Override
            public void onResult(Quests.LoadQuestsResult loadQuestsResult) {
                setQuestButtonEnabled(loadQuestsResult.getQuests().getCount() > 0);
            }
        });
    }

    protected void checkPendingPlayGifts() {
        PendingResult<Requests.LoadRequestsResult> pendingGifts = Games.Requests.loadRequests(getApiClient(), Requests.REQUEST_DIRECTION_INBOUND,
                GameRequest.TYPE_GIFT, Requests.SORT_ORDER_EXPIRING_SOON_FIRST);
        pendingGifts.setResultCallback(new ResultCallback<Requests.LoadRequestsResult>() {
            @Override
            public void onResult(Requests.LoadRequestsResult loadRequestsResult) {
                if (loadRequestsResult.getRequests(GameRequest.TYPE_GIFT).getCount() > 0) {
                    Button inboxButton = (Button) findViewById(R.id.inbox_button);
                    inboxButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onSignInFailed() {

    }

    @Override
    public void onSignInSucceeded() {
        // The sign in button is not a normal button, so keep it as a default view
        View signInButton = findViewById(R.id.sign_in_button);

        // If the user has switched views before the sign in failed then the buttons
        // are null and this will cause an error
        if(signInButton != null)
            signInButton.setVisibility(View.GONE);

        /*
        Button signOutButton = (Button) findViewById(R.id.sign_out_button);
        if(signOutButton != null)
            signOutButton.setVisibility(View.VISIBLE);
            */
    }

    private void handleInboxResult(ArrayList<GameRequest> gameRequests) {
        for(GameRequest request : gameRequests) {
            String senderName = request.getSender().getDisplayName();
            String message;

            if(new String(request.getData()).equals("p")) {
                message = String.format(getString(R.string.powerup_gift_received), senderName);
                try {
                    incrementPowerupInventory(1);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            else {
                message = String.format(getString(R.string.undo_gift_received), senderName);
                try {
                    incrementUndoInventory(1);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            Games.Requests.acceptRequest(getApiClient(), request.getRequestId());
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void incrementPowerupInventory(int amount) throws IOException, ClassNotFoundException {
        File gameDataFile = new File(getFilesDir(), getString(R.string.file_game_stats));
        GameData gameData = (GameData) Save.load(gameDataFile);
        gameData.incrementPowerupInventory(amount);
        Save.save(gameData, gameDataFile);
        updateInventoryTextView();
    }

    private void incrementUndoInventory(int amount) throws IOException, ClassNotFoundException {
        File gameDataFile = new File(getFilesDir(), getString(R.string.file_game_stats));
        GameData gameData = (GameData) Save.load(gameDataFile);
        gameData.incrementUndoInventory(amount);
        Save.save(gameData, gameDataFile);
        updateInventoryTextView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SEND_REQUEST_CODE:
                if (resultCode == GamesActivityResultCodes.RESULT_SEND_REQUEST_FAILED) {
                    Toast.makeText(this, R.string.error_send_request, Toast.LENGTH_LONG).show();
                }
                break;
            case SEND_GIFT_CODE:
                if (resultCode == GamesActivityResultCodes.RESULT_SEND_REQUEST_FAILED) {
                    Toast.makeText(this, getString(R.string.error_send_gift), Toast.LENGTH_LONG).show();
                }
                break;
            case SHOW_INBOX:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    handleInboxResult(Games.Requests
                            .getGameRequestsFromInboxResponse(data));
                } else {
                    // handle failure to process inbox result
                    if(resultCode != Activity.RESULT_CANCELED)
                        Toast.makeText(this, getString(R.string.error_claim_gift), Toast.LENGTH_LONG).show();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            View.OnTouchListener gamesOnClickListener = new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        view.setBackgroundColor(getResources().getColor(R.color.PaleTurquoise));
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP) {
                        view.setBackgroundColor(getResources().getColor(R.color.LightBlue));
                        ((MainActivity)getActivity()).playGames(view);
                    }
                    return true;
                }
            };

            ImageButton achievementsButton = (ImageButton) rootView.findViewById(R.id.achievements_button);
            ImageButton leaderboardsButton = (ImageButton) rootView.findViewById(R.id.leaderboards_button);
            ImageButton giftsButton = (ImageButton) rootView.findViewById(R.id.gifts_button);
            ImageButton questsButton = (ImageButton) rootView.findViewById(R.id.quests_button);

            achievementsButton.setOnTouchListener(gamesOnClickListener);
            leaderboardsButton.setOnTouchListener(gamesOnClickListener);
            giftsButton.setOnTouchListener(gamesOnClickListener);
            questsButton.setOnTouchListener(gamesOnClickListener);


            rootView.findViewById(R.id.single_player_imagebutton).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        view.setBackgroundResource(R.drawable.single_player_icon_pressed);
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP) {
                        view.setBackgroundResource(R.drawable.single_player_icon);
                        ((MainActivity)getActivity()).onClick(view);
                    }
                    return true;
                }
            });
            rootView.findViewById(R.id.multiplayer_imagebutton).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        view.setBackgroundResource(R.drawable.multiplayer_icon_pressed);
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP) {
                        view.setBackgroundResource(R.drawable.multiplayer_icon);
                        ((MainActivity)getActivity()).onClick(view);
                    }
                    return true;
                }
            });
            rootView.findViewById(R.id.help_button).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        view.setBackgroundResource(R.drawable.help_button_pressed);
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP) {
                        view.setBackgroundResource(R.drawable.help_button);
                        ((MainActivity)getActivity()).onClick(view);
                    }
                    return true;
                }
            });

            ImageButton settingsButton = (ImageButton) rootView.findViewById(R.id.settings_button);
            settingsButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        view.setBackgroundResource(R.drawable.settings_button_pressed);
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        view.setBackgroundResource(R.drawable.settings_button);
                        ((MainActivity) getActivity()).onClick(view);
                    }
                    return true;
                }
            });

            animateSettingsButton(settingsButton);

            return rootView;
        }

        private void animateSettingsButton(ImageButton settingsButton) {
            ObjectAnimator spinAnimation = ObjectAnimator.ofFloat(settingsButton, View.ROTATION, 360);

            spinAnimation.setDuration(7000);
            spinAnimation.setRepeatMode(Animation.RESTART);
            spinAnimation.setRepeatCount(Animation.INFINITE);
            spinAnimation.setInterpolator(new LinearInterpolator());

            spinAnimation.start();
        }
    }
}
