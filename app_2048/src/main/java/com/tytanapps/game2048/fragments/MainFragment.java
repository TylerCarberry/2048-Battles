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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainFragment extends Fragment {

    private Unbinder unbinder;
    @BindView(R.id.achievements_button) ImageButton achievementsButton;
    @BindView(R.id.leaderboards_button) ImageButton leaderboardsButton;
    @BindView(R.id.gifts_button) ImageButton giftsButton;
    @BindView(R.id.quests_button) ImageButton questsButton;
    @BindView(R.id.settings_button) ImageButton settingsButton;
    @BindView(R.id.help_button) ImageButton helpButton;
    @BindView(R.id.single_player_imagebutton) ImageButton singlePlayerButton;
    @BindView(R.id.multiplayer_imagebutton) ImageButton multiplayerButton;
    @BindView(R.id.share_button) ImageButton shareButton;

    public MainFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        achievementsButton.setOnTouchListener(createOnTouchListener
                (R.drawable.games_achievements, R.drawable.games_achievements_pressed));

        leaderboardsButton.setOnTouchListener(createOnTouchListener
                (R.drawable.games_leaderboards, R.drawable.games_leaderboards_pressed));

        giftsButton.setOnTouchListener(createOnTouchListener
                (R.drawable.games_gifts, R.drawable.games_gifts_pressed));

        questsButton.setOnTouchListener(createOnTouchListener
                (R.drawable.games_quests, R.drawable.games_quests_pressed));

        helpButton.setOnTouchListener(createOnTouchListener
                (R.drawable.help_button, R.drawable.help_button_pressed));

        singlePlayerButton.setOnTouchListener(createOnTouchListener
                (R.drawable.single_player_icon, R.drawable.single_player_icon_pressed));

        multiplayerButton.setOnTouchListener(createOnTouchListener
                (R.drawable.multiplayer_icon, R.drawable.multiplayer_icon_pressed));

        settingsButton.setOnTouchListener(createOnTouchListener
                (R.drawable.settings_button, R.drawable.settings_button_pressed));

        shareButton.setOnTouchListener(createOnTouchListener
                (R.drawable.share, R.drawable.share_white));

        animateSettingsButton(settingsButton);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private void animateSettingsButton(ImageButton settingsButton) {
        ObjectAnimator spinAnimation = ObjectAnimator.ofFloat(settingsButton, View.ROTATION, 360);

        spinAnimation.setDuration(7000);
        spinAnimation.setRepeatMode(Animation.RESTART);
        spinAnimation.setRepeatCount(Animation.INFINITE);
        spinAnimation.setInterpolator(new LinearInterpolator());

        spinAnimation.start();
    }

    private View.OnTouchListener createOnTouchListener(final int defaultResource, final int pressedResource) {
        View.OnTouchListener onTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                ImageView imageView = ((ImageView) view);
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    imageView.setImageResource(pressedResource);
                }
                else if (event.getAction() == MotionEvent.ACTION_UP) {
                    imageView.setImageResource(defaultResource);

                    if(event.getX() > 0 && event.getX() < view.getWidth() &&
                            event.getY() > 0 && event.getY() < view.getHeight())
                        ((MainActivity)getActivity()).onClick(view);
                }
                return true;
            }
        };

        return onTouchListener;
    }


    @OnClick(R.id.logo_imageview) protected void spinView(View view) {
        if(view.getRotation() % 360 == 0) {
            view.setRotation(0);
            Animator animator = ObjectAnimator.ofFloat(view, View.ROTATION, 360);
            animator.setDuration(1500);
            animator.start();
        }
    }

    @OnClick(R.id.sign_in_button) protected void signIn() {
        ((MainActivity)getActivity()).signIn();
    }
}