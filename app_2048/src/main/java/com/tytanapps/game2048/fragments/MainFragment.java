package com.tytanapps.game2048.fragments;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;

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
    @BindView(R.id.settings_button) ImageButton settingsButton;
    
    public MainFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        unbinder = ButterKnife.bind(this, rootView);

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

        spinAnimation.setDuration(10000);
        spinAnimation.setRepeatMode(Animation.RESTART);
        spinAnimation.setRepeatCount(Animation.INFINITE);
        spinAnimation.setInterpolator(new LinearInterpolator());

        spinAnimation.start();
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

    @OnClick({  R.id.help_button, R.id.single_player_imagebutton, R.id.multiplayer_imagebutton,
                R.id.achievements_button, R.id.leaderboards_button, R.id.gifts_button,
                R.id.quests_button, R.id.share_button, R.id.settings_button})
    protected void onClick(View view) {
        ((MainActivity)getActivity()).onClick(view);
    }
}