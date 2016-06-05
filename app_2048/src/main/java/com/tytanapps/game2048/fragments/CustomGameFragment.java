package com.tytanapps.game2048.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.NumberPicker;

import com.tytanapps.game2048.R;
import com.tytanapps.game2048.activities.CustomGameActivity;

/**
 * A placeholder fragment containing a simple view.
 */
public class CustomGameFragment extends Fragment {

    public CustomGameFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_custom_game, container, false);

        NumberPicker widthNumberPicker = (NumberPicker) rootView.findViewById(R.id.width_number_picker);
        NumberPicker heightNumberPicker = (NumberPicker) rootView.findViewById(R.id.height_number_picker);

        String[] values=new String[CustomGameActivity.MAX_GRID_SIZE];
        for(int i = 0; i < values.length; i++){
            values[i] = ""+(i+1);
        }

        widthNumberPicker.setMaxValue(CustomGameActivity.MAX_GRID_SIZE);
        widthNumberPicker.setMinValue(1);
        widthNumberPicker.setDisplayedValues(values);
        widthNumberPicker.setValue(4);

        heightNumberPicker.setMaxValue(CustomGameActivity.MAX_GRID_SIZE);
        heightNumberPicker.setMinValue(1);
        heightNumberPicker.setDisplayedValues(values);
        heightNumberPicker.setValue(4);


        widthNumberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                ((CustomGameActivity)getActivity()).updateGamePreview();
            }
        });

        heightNumberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                ((CustomGameActivity) getActivity()).updateGamePreview();
            }
        });

        CheckBox xModeCheckbox = (CheckBox) rootView.findViewById(R.id.xmode_checkbox);
        CheckBox cornerCheckbox = (CheckBox) rootView.findViewById(R.id.corner_mode_checkbox);
        CheckBox arcadeCheckbox = (CheckBox) rootView.findViewById(R.id.arcade_mode_checkbox);
        CheckBox speedCheckbox = (CheckBox) rootView.findViewById(R.id.speed_mode_checkbox);
        CheckBox survivalCheckbox = (CheckBox) rootView.findViewById(R.id.survival_mode_checkbox);
        CheckBox rushCheckbox = (CheckBox) rootView.findViewById(R.id.rush_mode_checkbox);
        CheckBox ghostCheckbox = (CheckBox) rootView.findViewById(R.id.ghost_mode_checkbox);


        CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ((CustomGameActivity)getActivity()).updateGamePreview();
            }
        };

        xModeCheckbox.setOnCheckedChangeListener(onCheckedChangeListener);
        cornerCheckbox.setOnCheckedChangeListener(onCheckedChangeListener);
        arcadeCheckbox.setOnCheckedChangeListener(onCheckedChangeListener);
        speedCheckbox.setOnCheckedChangeListener(onCheckedChangeListener);
        survivalCheckbox.setOnCheckedChangeListener(onCheckedChangeListener);
        rushCheckbox.setOnCheckedChangeListener(onCheckedChangeListener);
        ghostCheckbox.setOnCheckedChangeListener(onCheckedChangeListener);

        return rootView;
    }
}