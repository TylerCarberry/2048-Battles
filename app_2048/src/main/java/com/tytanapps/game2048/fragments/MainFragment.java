package com.tytanapps.game2048.fragments;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.tytanapps.game2048.R;
import com.tytanapps.game2048.activities.MainActivity;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainFragment extends Fragment {

    public MainFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ImageButton achievementsButton = (ImageButton) rootView.findViewById(R.id.achievements_button);
        ImageButton leaderboardsButton = (ImageButton) rootView.findViewById(R.id.leaderboards_button);
        ImageButton giftsButton = (ImageButton) rootView.findViewById(R.id.gifts_button);
        ImageButton questsButton = (ImageButton) rootView.findViewById(R.id.quests_button);
        ImageButton settingsButton = (ImageButton) rootView.findViewById(R.id.settings_button);
        ImageButton helpButton = (ImageButton) rootView.findViewById(R.id.help_button);
        ImageButton singlePlayerButton = (ImageButton) rootView.findViewById(R.id.single_player_imagebutton);
        ImageButton multiplayerButton = (ImageButton) rootView.findViewById(R.id.multiplayer_imagebutton);
        ImageButton shareButton = (ImageButton) rootView.findViewById(R.id.share_button);

        achievementsButton.setOnTouchListener(createOnTouchListener
                (achievementsButton, R.drawable.games_achievements, R.drawable.games_achievements_pressed));

        leaderboardsButton.setOnTouchListener(createOnTouchListener
                (leaderboardsButton, R.drawable.games_leaderboards, R.drawable.games_leaderboards_pressed));

        giftsButton.setOnTouchListener(createOnTouchListener
                (giftsButton, R.drawable.games_gifts, R.drawable.games_gifts_pressed));

        questsButton.setOnTouchListener(createOnTouchListener
                (questsButton, R.drawable.games_quests, R.drawable.games_quests_pressed));

        helpButton.setOnTouchListener(createOnTouchListener
                (helpButton, R.drawable.help_button, R.drawable.help_button_pressed));

        singlePlayerButton.setOnTouchListener(createOnTouchListener
                (singlePlayerButton, R.drawable.single_player_icon, R.drawable.single_player_icon_pressed));

        multiplayerButton.setOnTouchListener(createOnTouchListener
                (multiplayerButton, R.drawable.multiplayer_icon, R.drawable.multiplayer_icon_pressed));

        settingsButton.setOnTouchListener(createOnTouchListener
                (settingsButton, R.drawable.settings_button, R.drawable.settings_button_pressed));

        shareButton.setOnTouchListener(createOnTouchListener
                (shareButton, R.drawable.share, R.drawable.share_white));

        ImageView appLogo = (ImageView) rootView.findViewById(R.id.logo_imageview);
        appLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(view.getRotation() % 360 == 0) {
                    view.setRotation(0);
                    Animator animator = ObjectAnimator.ofFloat(view, View.ROTATION, 360);
                    animator.setDuration(1500);
                    animator.start();
                }
            }
        });

        animateSettingsButton(settingsButton);

        View signInButton = rootView.findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)getActivity()).signIn();
            }
        });

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

    private View.OnTouchListener createOnTouchListener(final ImageView view, final int defaultResource, final int pressedResource) {
        View.OnTouchListener onTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    view.setImageResource(pressedResource);
                }
                else if (event.getAction() == MotionEvent.ACTION_UP) {
                    view.setImageResource(defaultResource);

                    if(event.getX() > 0 && event.getX() < view.getWidth() &&
                            event.getY() > 0 && event.getY() < view.getHeight())
                        ((MainActivity)getActivity()).onClick(view);
                }
                return true;
            }
        };

        return onTouchListener;
    }
}