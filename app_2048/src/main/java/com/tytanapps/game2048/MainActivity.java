package com.tytanapps.game2048;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.request.GameRequest;
import com.google.android.gms.games.request.Requests;
import com.google.example.games.basegameutils.BaseGameActivity;

import java.io.File;
import java.io.IOException;


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
        updateInventoryTextView();

        super.onStart();
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
                startActivity(new Intent(this, InfoActivity.class));
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

    protected void sendGift() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.prompt_choose_powerup)).setItems(R.array.gifts, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent ;
                // The 'which' argument contains the index position
                // of the selected item
                switch (which) {
                    // Powerup
                    case 0:
                        intent = Games.Requests.getSendIntent(getApiClient(), GameRequest.TYPE_GIFT,
                                "p".getBytes(), Requests.REQUEST_DEFAULT_LIFETIME_DAYS, BitmapFactory.decodeResource(getResources(),
                                        R.drawable.powerup_button), "Powerup Desc");
                        startActivityForResult(intent, SEND_GIFT_CODE);
                        break;
                    case 1:
                        intent = Games.Requests.getSendIntent(getApiClient(), GameRequest.TYPE_GIFT,
                                "u".getBytes(), Requests.REQUEST_DEFAULT_LIFETIME_DAYS, BitmapFactory.decodeResource(getResources(),
                                        R.drawable.undo_button), "Undo Desc");
                        startActivityForResult(intent, SEND_GIFT_CODE);
                        break;
                }
            }
        });
        builder.create().show();
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


            return rootView;
        }
    }
}
