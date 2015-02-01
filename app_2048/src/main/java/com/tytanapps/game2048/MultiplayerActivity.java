package com.tytanapps.game2048;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.example.games.basegameutils.BaseGameActivity;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MultiplayerActivity extends BaseGameActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener, RealTimeMessageReceivedListener,
        RoomStatusUpdateListener, RoomUpdateListener, OnInvitationReceivedListener {

    final static String LOG_TAG = MultiplayerActivity.class.getSimpleName();

    // Request codes for the UIs that we show with startActivityForResult:
    final static int RC_SELECT_PLAYERS = 10000;
    final static int RC_INVITATION_INBOX = 10001;
    final static int RC_WAITING_ROOM = 10002;

    // Request code used to invoke sign in user interactions.
    private static final int RC_SIGN_IN = 9001;

    public static final char SEND_SCORE = 's';
    public static final char SEND_REMATCH = 'r';
    public static final char SEND_LOADED = 'l';
    public static final char SEND_NAME = 'n';
    public static final char SEND_PIC_URL = 'p';
    public static final char SEND_ATTACK_SHUFFLE = 'h';
    public static final char SEND_ATTACK_ICE = 'i';
    public static final char SEND_ATTACK_GHOST = 'g';
    public static final char SEND_ATTACK_X = 'x';

    // The % chance that a bonus will be received this second
    private final static double BONUS_CHANCE = 0.20;

    // The number of seconds a multiplayer game is played for
    public static final int MULTIPLAYER_GAME_LENGTH = 60;

    // Client used to interact with Google APIs.
    private GoogleApiClient mGoogleApiClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // Has the user clicked the sign-in button?
    private boolean mSignInClicked = false;

    // Set to true to automatically start the sign in flow when the Activity starts.
    // Set to false to require the user to click the button in order to sign in.
    private boolean mAutoStartSignInFlow = true;

    // Room ID where the currently active game is taking place; null if we're
    // not playing.
    String mRoomId = null;

    // Are we playing in multiplayer mode?
    private boolean multiplayerActive = false;

    // The participants in the currently active game
    ArrayList<Participant> mParticipants = null;

    // My participant ID in the currently active game
    String mMyId = null;

    // If non-null, this is the id of the invitation we received via the
    // invitation listener
    String mIncomingInvitationId = null;

    GameFragment gameFragment;

    // Message buffer for sending messages
    byte[] mMsgBuf;

    private boolean iRequestedRematch = false;
    private boolean opponentRequestedRematch = false;

    private boolean gameLoaded = false;
    private boolean opponentLoaded = false;

    private String opponentName;
    private String opponentPicUrl;

    private boolean hideIdentity = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.multiplayer_frame_layout, new PlaceholderFragment())
                    .commit();
        }

        // Create the Google Api Client with access to Plus and Games
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        hideIdentity = prefs.getBoolean(getString(R.string.preference_hide_identity), false);

        // If the room should be created automatically
        if(getIntent().getExtras().getBoolean("startMultiplayer", false)) {
            mGoogleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {
                    startQuickGame();
                }
                @Override
                public void onConnectionSuspended(int i) {}
            });
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.recreate_multiplayer_button:
                // Create a multiplayer game
                findViewById(R.id.creating_multiplayer_progressbar).setVisibility(View.VISIBLE);
                findViewById(R.id.recreate_multiplayer_button).setVisibility(View.INVISIBLE);

                TextView multiplayer_message_textview = (TextView) findViewById(R.id.multiplayer_message);
                multiplayer_message_textview.setText(R.string.creating_multiplayer_game);

                startQuickGame();
                break;
            case R.id.to_game_button:
                switchToGame();
                break;
        }
    }

    private void switchToGame() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        gameFragment = new GameFragment();

        ft.replace(R.id.multiplayer_frame_layout, gameFragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.addToBackStack(null);
        ft.commit();

        multiplayerActive = true;

        if(! hideIdentity) {
            sendMessage(SEND_NAME + getPlayerName(), true);
            sendMessage(SEND_PIC_URL + getPlayer().getImage().getUrl(), true);
        }
    }

    /**
     * A quick game with 1 random opponent
     */
    private void startQuickGame() {
        final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 1;
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(MIN_OPPONENTS,
                MAX_OPPONENTS, 0);
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        keepScreenOn();

        Games.RealTimeMultiplayer.create(mGoogleApiClient, rtmConfigBuilder.build());
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode,
                                 Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);

        switch (requestCode) {
            case RC_SELECT_PLAYERS:
                // we got the result from the "select players" UI -- ready to create the room
                handleSelectPlayersResult(responseCode, intent);
                break;
            case RC_INVITATION_INBOX:
                // we got the result from the "select invitation" UI (invitation inbox). We're
                // ready to accept the selected invitation:
                handleInvitationInboxResult(responseCode, intent);
                break;
            case RC_WAITING_ROOM:
                // we got the result from the "waiting room" UI.
                if (responseCode == Activity.RESULT_OK) {
                    // ready to start playing
                    Log.d(LOG_TAG, "Starting game (waiting room returned OK).");
                    multiplayerActive = true;
                    switchToGame();
                } else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                    // player indicated that they want to leave the room
                    leaveRoom();
                    switchToMainActivity();
                }
                else if (responseCode == Activity.RESULT_CANCELED) {
                    // Dialog was cancelled (user pressed back key, for instance). In our game,
                    // this means leaving the room too. In more elaborate games, this could mean
                    // something else (like minimizing the waiting room UI).
                    leaveRoom();
                    switchToMainActivity();
                }
                else {
                    unableToCreateRoom();
                }

                break;
            case RC_SIGN_IN:
                Log.d(LOG_TAG, "onActivityResult with requestCode == RC_SIGN_IN, responseCode="
                        + responseCode + ", intent=" + intent);
                mSignInClicked = false;
                mResolvingConnectionFailure = false;
                if (responseCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                }
                break;
        }
        super.onActivityResult(requestCode, responseCode, intent);
    }

    protected void createMultiplayerTimer(final int seconds) {
        ((TextView) findViewById(R.id.time_left_textview)).setText(""+seconds);

        final Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            int times = 0;
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        times++;

                        // If the time is up or the user switched screens then stop the timer
                        if(times > seconds || findViewById(R.id.multiplayerProgressBar) == null) {
                            t.cancel();
                            t.purge();
                            return;
                        }

                        decreaseTimeLeft(1);
                        updateScoreProgressbar();

                    }
                });

            }}, 1000, 1000);
    }

    private int decreaseTimeLeft(int seconds) {
        TextView timerTextView = (TextView) findViewById(R.id.time_left_textview);
        int secondsLeft = Integer.parseInt(timerTextView.getText().toString());
        secondsLeft -= seconds;
        timerTextView.setText(""+secondsLeft);

        Log.d(LOG_TAG, "Seconds Left: "+secondsLeft);

        if(secondsLeft <= 0)
            multiplayerTimeUp();
        else if(Math.random() < BONUS_CHANCE)
                addBonus();

        return secondsLeft;
    }

    private void multiplayerTimeUp() {
        int myScore = gameFragment.getGame().getScore();
        int opponentScore = gameFragment.getGame().getOpponentScore();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.time_is_up));

        if (myScore > opponentScore)
            builder.setMessage(getString(R.string.win_multiplayer));
        else if (myScore == opponentScore)
            builder.setMessage(getString(R.string.tie_multiplayer));
        else
            builder.setMessage(getString(R.string.lose_multiplayer));

        builder.setNegativeButton(getString(R.string.leave_game), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendMessage(getString(R.string.opponent_left_game), true);
                leaveRoom();
                switchToMainActivity();
            }
        });

        if(multiplayerActive)
            builder.setPositiveButton(getString(R.string.rematch), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    requestRematch();
                }
            });

        AlertDialog dialog = builder.create();
        // You must click on one of the buttons in order to dismiss the dialog
        dialog.setCanceledOnTouchOutside(false);
        // Show the dialog
        dialog.show();

        if(getApiClient().isConnected())
            Games.Events.increment(this.getApiClient(), getString(R.string.event_multiplayer_games_played), 1);
    }

    private void updateScoreProgressbar() {
        int myScore = gameFragment.getGame().getScore();
        int theirScore = gameFragment.getGame().getOpponentScore();

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.multiplayerProgressBar);
        progressBar.setMax(myScore + theirScore);
        progressBar.setProgress(myScore);

        View myCrown = gameFragment.getView().findViewById(R.id.player_crown);
        View opponentCrown = gameFragment.getView().findViewById(R.id.opponent_crown);
        if(myScore >= theirScore) {
            myCrown.setVisibility(View.VISIBLE);
            opponentCrown.setVisibility(View.INVISIBLE);
        }
        else {
            myCrown.setVisibility(View.INVISIBLE);
            opponentCrown.setVisibility(View.VISIBLE);
        }
    }

    private void requestRematch() {
        iRequestedRematch = true;
        sendMessage("" + SEND_REMATCH, true);
        if(opponentRequestedRematch)
            startRematch();
        else
            Toast.makeText(this, getString(R.string.rematch_button_pressed), Toast.LENGTH_LONG).show();
    }

    private void startRematch() {
        opponentRequestedRematch = false;
        iRequestedRematch = false;

        gameFragment.clearMultiplayerInventory();
        gameFragment.setGame(GameModes.practiceMode());
        gameFragment.updateGame();

        gameFragment.createCountdown(3 * 1000);
    }

    public void gameHasLoaded() {
        gameLoaded = true;
        sendMessage(""+SEND_LOADED, true);
        if(opponentLoaded)
            gameFragment.createCountdown(3 * 1000);
    }

    private void addBonus() {
        double random = Math.random();

        if(random < .5)
            if(random < .25)
                if(random < .125)
                    addAttackBonus(SEND_ATTACK_GHOST);
                else
                    addAttackBonus(SEND_ATTACK_ICE);
            else if(random < .375)
                addAttackBonus(SEND_ATTACK_SHUFFLE);
            else
                addAttackBonus(SEND_ATTACK_X);
        else if (random < .75)
            addPowerupBonus();
        else
            addUndoBonus();
    }

    private void addPowerupBonus() {
        final ImageButton powerupButton = new ImageButton(this);
        final LinearLayout bonusesLinearLayout = (LinearLayout) gameFragment.getView().findViewById(R.id.bonuses_linear_layout);

        powerupButton.setBackgroundResource(R.drawable.powerup_button);
        powerupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gameFragment.showPowerupDialog();
                        bonusesLinearLayout.removeView(powerupButton);
                    }
                });
            }
        });
        bonusesLinearLayout.addView(powerupButton);
    }

    private void addUndoBonus() {
        final ImageButton undoButton = new ImageButton(this);
        final LinearLayout bonusesLinearLayout = (LinearLayout) gameFragment.getView().findViewById(R.id.bonuses_linear_layout);

        undoButton.setBackgroundResource(R.drawable.undo_button);
        undoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gameFragment.undo();
                        bonusesLinearLayout.removeView(undoButton);
                    }
                });
            }
        });
        bonusesLinearLayout.addView(undoButton);
    }

    private void addAttackBonus(char attack) {
        final ImageButton attackButton = new ImageButton(this);
        final LinearLayout bonusesLinearLayout = (LinearLayout) gameFragment.getView().findViewById(R.id.bonuses_linear_layout);

        switch (attack){
            case SEND_ATTACK_GHOST:
                attackButton.setBackgroundResource(R.drawable.ghost_attack_button);
                bonusesLinearLayout.addView(attackButton);
                attackButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                animateAttackUsed(attackButton);
                                sendMessage(""+SEND_ATTACK_GHOST, true);
                            }
                        });
                    }
                });
                break;
            case SEND_ATTACK_SHUFFLE:
                attackButton.setBackgroundResource(R.drawable.shuffle_attack_button);
                bonusesLinearLayout.addView(attackButton);
                attackButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                animateAttackUsed(attackButton);
                                sendMessage(""+SEND_ATTACK_SHUFFLE, true);
                            }
                        });
                    }
                });
                break;
            case SEND_ATTACK_ICE:
                attackButton.setBackgroundResource(R.drawable.ice_attack_button);
                bonusesLinearLayout.addView(attackButton);
                attackButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                animateAttackUsed(attackButton);
                                sendMessage(""+SEND_ATTACK_ICE, true);
                            }
                        });
                    }
                });
                break;
            case SEND_ATTACK_X:
                attackButton.setBackgroundResource(R.drawable.x_attack_button);
                bonusesLinearLayout.addView(attackButton);
                attackButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                animateAttackUsed(attackButton);
                                sendMessage(""+SEND_ATTACK_X, true);
                            }
                        });
                    }
                });
                break;
        }
    }

    private void animateAttackUsed(final View view) {
        final LinearLayout bonusesLinearLayout = (LinearLayout) gameFragment.getView().findViewById(R.id.bonuses_linear_layout);
        bonusesLinearLayout.bringToFront();
        bonusesLinearLayout.setClipChildren(false);

        int[] positionOpponent = new int[2];
        findViewById(R.id.opponent_imageview).getLocationInWindow(positionOpponent);

        int[] positionAttack = new int[2];
        view.getLocationInWindow(positionAttack);

        int deltaX = positionOpponent[0] - positionAttack[0];
        int deltaY = 0 - (positionAttack[1] - positionOpponent[1]);

        ObjectAnimator translateX = ObjectAnimator.ofFloat(view, "translationX", deltaX);
        ObjectAnimator translateY = ObjectAnimator.ofFloat(view, "translationY", deltaY);

        translateX.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                bonusesLinearLayout.removeView(view);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                bonusesLinearLayout.removeView(view);
            }

            @Override
            public void onAnimationStart(Animator animation) {}
            @Override
            public void onAnimationRepeat(Animator animation) {}
        });

        translateX.start();
        translateY.start();
    }

    protected void setImageView(final ImageView imageView, final Bitmap bitmap) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(bitmap);
            }
        });
    }

    private void switchToMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
    }

    // Handle the result of the "Select players UI" we launched when the user clicked the
    // "Invite friends" button. We react by creating a room with those players.
    private void handleSelectPlayersResult(int response, Intent data) {
        if (response != Activity.RESULT_OK) {
            Log.w(LOG_TAG, "*** select players UI cancelled, " + response);
            //switchToMainScreen();
            return;
        }

        Log.d(LOG_TAG, "Select players UI succeeded.");

        // get the invitee list
        final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
        Log.d(LOG_TAG, "Invitee count: " + invitees.size());

        // get the automatch criteria
        Bundle autoMatchCriteria = null;
        int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
        int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
        if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
            autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                    minAutoMatchPlayers, maxAutoMatchPlayers, 0);
            Log.d(LOG_TAG, "Automatch criteria: " + autoMatchCriteria);
        }

        // create the room
        Log.d(LOG_TAG, "Creating room...");
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.addPlayersToInvite(invitees);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        if (autoMatchCriteria != null) {
            rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        }
        //switchToScreen(R.id.screen_wait);
        keepScreenOn();
        //resetGameVars();
        Games.RealTimeMultiplayer.create(mGoogleApiClient, rtmConfigBuilder.build());
        Log.d(LOG_TAG, "Room created, waiting for it to be ready...");
    }

    // Handle the result of the invitation inbox UI, where the player can pick an invitation
    // to accept. We react by accepting the selected invitation, if any.
    private void handleInvitationInboxResult(int response, Intent data) {
        if (response != Activity.RESULT_OK) {
            Log.w(LOG_TAG, "*** invitation inbox UI cancelled, " + response);
            //switchToMainScreen();
            return;
        }

        Log.d(LOG_TAG, "Invitation inbox UI succeeded.");
        Invitation inv = data.getExtras().getParcelable(Multiplayer.EXTRA_INVITATION);

        // accept invitation
        acceptInviteToRoom(inv.getInvitationId());
    }

    // Accept the given invitation.
    void acceptInviteToRoom(String invId) {
        // accept the invitation
        Log.d(LOG_TAG, "Accepting invitation: " + invId);
        RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this);
        roomConfigBuilder.setInvitationIdToAccept(invId)
                .setMessageReceivedListener(this)
                .setRoomStatusUpdateListener(this);
        //switchToScreen(R.id.screen_wait);
        keepScreenOn();
        //resetGameVars();
        Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfigBuilder.build());
    }

    // Activity is going to the background. We have to leave the current room.
    @Override
    public void onStop() {
        // if we're in a room, leave it.
        leaveRoom();

        // stop trying to keep the screen on
        stopKeepingScreenOn();

        super.onStop();
    }

    // Activity just got to the foreground. We switch to the wait screen because we will now
    // go through the sign-in flow (remember that, yes, every time the Activity comes back to the
    // foreground we go through the sign-in flow -- but if the user is already authenticated,
    // this flow simply succeeds and is imperceptible).
    @Override
    public void onStart() {
        //switchToScreen(R.id.screen_wait);
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.w(LOG_TAG,
                    "GameHelper: client was already connected on onStart()");
        } else {
            Log.d(LOG_TAG,"Connecting client.");
            mGoogleApiClient.connect();
        }
        super.onStart();
    }

    @Override
    public void onRealTimeMessageReceived(RealTimeMessage rtm) {
        if(! multiplayerActive)
            return;

        // Convert the message to a string
        byte[] buf = rtm.getMessageData();
        String message = "";
        for(byte b : buf) {
            char letter = (char) b;
            message += letter;
        }

        switch(message.charAt(0)) {
            // The score was sent
            case SEND_SCORE:
                int opponentScore = Integer.parseInt(message.substring(1));
                gameFragment.getGame().setOpponentScore(opponentScore);
                break;
            case SEND_REMATCH:
                opponentRequestedRematch = true;
                if(iRequestedRematch)
                    startRematch();
                break;
            case SEND_NAME:
                opponentName = message.substring(1);
                if(findViewById(R.id.multiplayerProgressBar) != null)
                    gameFragment.updateOpponentName();
                break;
            case SEND_PIC_URL:
                opponentPicUrl = message.substring(1);
                if(findViewById(R.id.multiplayerProgressBar) != null)
                    gameFragment.updateOpponentPic();
                break;
            case SEND_ATTACK_SHUFFLE:
                gameFragment.shuffleGame();
                Toast.makeText(this, getString(R.string.multiplayer_recieved_attack), Toast.LENGTH_SHORT).show();
                break;
            case SEND_ATTACK_GHOST:
                gameFragment.ghostAttack();
                gameFragment.updateTextviews();
                break;
            case SEND_ATTACK_ICE:
                gameFragment.ice();
                gameFragment.updateTextviews();
                break;
            case SEND_ATTACK_X:
                gameFragment.XTileAttack();
                gameFragment.updateTextviews();
                break;
            case SEND_LOADED:
                opponentLoaded = true;
                if(gameLoaded)
                    gameFragment.createCountdown(3 * 1000);
                break;
            default:
                Toast.makeText(this, message , Toast.LENGTH_LONG).show();
        }
    }

    // Handle back key to make sure we cleanly leave a game if we are in the middle of one
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(mRoomId == null) {
                return super.onKeyDown(keyCode, e);
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.confirm_leave_multiplayer_game));
            builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    leaveRoom();
                    switchToMainActivity();
                }
            });
            builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            });
            builder.create().show();
            return true;
        }

        return super.onKeyDown(keyCode, e);
    }

    // Leave the room.
    void leaveRoom() {
        stopKeepingScreenOn();
        if (mRoomId != null) {
            Games.RealTimeMultiplayer.leave(mGoogleApiClient, this, mRoomId);
            mRoomId = null;
        }
    }

    protected String getPlayerName() {
        if(hideIdentity)
            return getString(R.string.player);

        return Plus.PeopleApi.getCurrentPerson(mGoogleApiClient).getName().getGivenName();
    }

    protected Person getPlayer() {

        if(hideIdentity)
            return null;

        return Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
    }

    protected void setImageViewBackground(ImageView imageView, String url) {
        imageView.setTag(url);
        new DownloadImagesTask().execute(imageView);
    }

    // Show the waiting room UI to track the progress of other players as they enter the
    // room and get connected.
    void showWaitingRoom(Room room) {
        // minimum number of players required for our game
        // For simplicity, we require everyone to join the game before we start it
        // (this is signaled by Integer.MAX_VALUE).
        final int MIN_PLAYERS = Integer.MAX_VALUE;
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MIN_PLAYERS);

        // show waiting room UI
        startActivityForResult(i, RC_WAITING_ROOM);
    }

    // Called when we get an invitation to play a game. We react by showing that to the user.
    @Override
    public void onInvitationReceived(Invitation invitation) {

        /*
        // We got an invitation to play a game! So, store it in
        // mIncomingInvitationId
        // and show the popup on the screen.
        mIncomingInvitationId = invitation.getInvitationId();
        ((TextView) findViewById(R.id.incoming_invitation_text)).setText(
                invitation.getInviter().getDisplayName() + " " +
                        getString(R.string.is_inviting_you));
        switchToScreen(mCurScreen); // This will show the invitation popup
        */
    }

    @Override
    public void onInvitationRemoved(String invitationId) {
        /*
        if (mIncomingInvitationId.equals(invitationId)) {
            mIncomingInvitationId = null;
            switchToScreen(mCurScreen); // This will hide the invitation popup
        }
        */
    }

    /*
     * CALLBACKS SECTION. This section shows how we implement the several games
     * API callbacks.
     */

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(LOG_TAG, "onConnected() called. Sign in successful!");
        Log.d(LOG_TAG, "Sign-in succeeded.");

        // register listener so we are notified if we receive an invitation to play
        // while we are in the game
        Games.Invitations.registerInvitationListener(mGoogleApiClient, this);

        if (connectionHint != null) {
            Log.d(LOG_TAG, "onConnected: connection hint provided. Checking for invite.");
            Invitation inv = connectionHint
                    .getParcelable(Multiplayer.EXTRA_INVITATION);
            if (inv != null && inv.getInvitationId() != null) {
                // retrieve and cache the invitation ID
                Log.d(LOG_TAG,"onConnected: connection hint has a room invite!");
                acceptInviteToRoom(inv.getInvitationId());
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "ConnectionSuspended", Toast.LENGTH_LONG).show();
        Log.d(LOG_TAG, "onConnectionSuspended() called. Trying to reconnect.");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "ConnectionFailed", Toast.LENGTH_LONG).show();

        Log.d(LOG_TAG, "onConnectionFailed() called, result: " + connectionResult);

        if (mResolvingConnectionFailure) {
            Log.d(LOG_TAG, "onConnectionFailed() ignoring connection failure; already resolving.");
            return;
        }

        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            //mResolvingConnectionFailure = BaseGameUtils.resolveConnectionFailure(this, mGoogleApiClient,
            //        connectionResult, RC_SIGN_IN, getString(R.string.signin_other_error));
        }

        //switchToScreen(R.id.screen_sign_in);
    }

    // Called when we are connected to the room. We're not ready to play yet! (maybe not everybody
    // is connected yet).
    @Override
    public void onConnectedToRoom(Room room) {
        Log.d(LOG_TAG, "onConnectedToRoom.");

        // get room ID, participants and my ID:
        mRoomId = room.getRoomId();
        mParticipants = room.getParticipants();
        mMyId = room.getParticipantId(Games.Players.getCurrentPlayerId(mGoogleApiClient));

        // print out the list of participants (for debug purposes)
        Log.d(LOG_TAG, "Room ID: " + mRoomId);
        Log.d(LOG_TAG, "My ID " + mMyId);
        Log.d(LOG_TAG, "Is mParticipants null " + (mParticipants == null));

        Log.d(LOG_TAG, "<< CONNECTED TO ROOM>>");
    }

    // Called when we've successfully left the room (this happens a result of voluntarily leaving
    // via a call to leaveRoom(). If we get disconnected, we get onDisconnectedFromRoom()).
    @Override
    public void onLeftRoom(int statusCode, String roomId) {
        // we have left the room; return to main screen.
        Log.d(LOG_TAG, "onLeftRoom, code " + statusCode);
        //switchToMainScreen();
    }

    // Called when we get disconnected from the room. We return to the main screen.
    @Override
    public void onDisconnectedFromRoom(Room room) {
        mRoomId = null;
        opponentLeft();
    }

    // Called when room has been created
    @Override
    public void onRoomCreated(int statusCode, Room room) {
        Log.d(LOG_TAG, "onRoomCreated(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK)
            unableToCreateRoom();
        else
            // show the waiting room UI
            showWaitingRoom(room);
    }

    private void unableToCreateRoom() {
        findViewById(R.id.creating_multiplayer_progressbar).setVisibility(View.INVISIBLE);
        findViewById(R.id.recreate_multiplayer_button).setVisibility(View.VISIBLE);

        TextView multiplayer_message_textview = (TextView) findViewById(R.id.multiplayer_message);
        multiplayer_message_textview.setText(R.string.error_create_multiplayer_game);
    }

    // Called when room is fully connected.
    @Override
    public void onRoomConnected(int statusCode, Room room) {
        Log.d(LOG_TAG, "onRoomConnected(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(LOG_TAG, "*** Error: onRoomConnected, status " + statusCode);
            unableToCreateRoom();
            return;
        }
        updateRoom(room);
    }

    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        Log.d(LOG_TAG, "onJoinedRoom(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(LOG_TAG, "*** Error: onRoomConnected, status " + statusCode);
            unableToCreateRoom();
            return;
        }

        // show the waiting room UI
        showWaitingRoom(room);
    }

    // We treat most of the room update callbacks in the same way: we update our list of
    // participants and update the display. In a real game we would also have to check if that
    // change requires some action like removing the corresponding player avatar from the screen,
    // etc.
    @Override
    public void onPeerDeclined(Room room, List<String> arg1) {
        updateRoom(room);
    }
    @Override
    public void onPeerInvitedToRoom(Room room, List<String> arg1) {
        updateRoom(room);
    }
    @Override
    public void onP2PDisconnected(String participant) {}
    @Override
    public void onP2PConnected(String participant) {}
    @Override
    public void onPeerJoined(Room room, List<String> arg1) {
        updateRoom(room);
    }
    @Override
    public void onPeerLeft(Room room, List<String> peersWhoLeft) {
        //Toast.makeText(this, "onPeerLeft", Toast.LENGTH_LONG).show();

        opponentLeft();
        updateRoom(room);
    }
    @Override
    public void onRoomAutoMatching(Room room) {
        updateRoom(room);
    }
    @Override
    public void onRoomConnecting(Room room) {
        updateRoom(room);
    }
    @Override
    public void onPeersConnected(Room room, List<String> peers) {
        updateRoom(room);
    }
    @Override
    public void onPeersDisconnected(Room room, List<String> peers) {
        //Toast.makeText(this, "onPeersDisconnected", Toast.LENGTH_LONG).show();

        opponentLeft();
        updateRoom(room);
    }

    private void opponentLeft() {
        multiplayerActive = false;

        View activeAttacks = gameFragment.getView().findViewById(R.id.active_attacks_textview);
        View attackInventory = gameFragment.getView().findViewById(R.id.bonuses_linear_layout);

        if(activeAttacks != null)
            ((TextView) activeAttacks).setText(getString(R.string.opponent_left_game));
        if(attackInventory != null)
            attackInventory.setVisibility(View.INVISIBLE);
    }

    void updateRoom(Room room) {
        //Toast.makeText(this, "update room", Toast.LENGTH_LONG).show();
        if (room != null) {
            mParticipants = room.getParticipants();
            //Toast.makeText(this, "partic: " + mParticipants.size(), Toast.LENGTH_LONG).show();


            //for(Participant p : mParticipants)
            //    if(! p.isConnectedToRoom())
            //        roomIsEmpty();


            //if(room.getRoomId() == null || mParticipants.size() <= 1)
            //    mParticipants.get

            //    roomIsEmpty();

        }
    }

    private void roomIsEmpty() {
        Toast.makeText(this, getString(R.string.everybody_left_game), Toast.LENGTH_SHORT).show();
    }

    public String getOpponentName() {
        return opponentName;
    }

    public String getOpponentPicUrl() {
        return opponentPicUrl;
    }

    /**
     * Broadcast a message to the other players
     * @param message The message to send
     * @param reliable Whether or not the sent message will be reliable
      */
    protected void sendMessage(String message, boolean reliable) {

        if(mRoomId == null) {
            //Toast.makeText(this, "You are not in a game", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert the string message to an array of bytes
        mMsgBuf = new byte[message.length()];
        for(int position = 0; position < message.length(); position++)
            mMsgBuf[position] = (byte) message.charAt(position);

        // Send to every other participant.
        for (Participant p : mParticipants) {
            // Only send to joined players other than yourself
            if (!p.getParticipantId().equals(mMyId) && (p.getStatus() == Participant.STATUS_JOINED)) {
                if (reliable)
                    Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, mMsgBuf,
                          mRoomId, p.getParticipantId());
                else
                    Games.RealTimeMultiplayer.sendUnreliableMessage(mGoogleApiClient, mMsgBuf, mRoomId,
                            p.getParticipantId());
            }
        }
    }


    // Sets the flag to keep this screen on. It's recommended to do that during the handshake
    // when setting up a game, because if the screen turns off, the game will be cancelled.
    void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Clears the flag that keeps the screen on.
    void stopKeepingScreenOn() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onSignInFailed() {    }

    @Override
    public void onSignInSucceeded() {    }

    public boolean isMultiplayerActive() {
        return multiplayerActive;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment{

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_multiplayer, container, false);

            return rootView;
        }
    }

    public static class DownloadImagesTask extends AsyncTask<ImageView, Void, Bitmap> {

        ImageView imageView = null;

        @Override
        protected Bitmap doInBackground(ImageView... imageViews) {
            this.imageView = imageViews[0];
            return download_Image((String) imageView.getTag());
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            new MultiplayerActivity().setImageView(imageView, result);
        }

        private Bitmap download_Image(String stringUrl) {
            try {
                URL url = new URL(stringUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (IOException e) {
                // Log exception
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}
