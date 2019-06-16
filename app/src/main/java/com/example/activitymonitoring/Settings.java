package com.example.activitymonitoring;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

public class Settings extends AppCompatActivity {

    private static Context appContext;
    SharedPreferences sharedPreferences;

    EditText stepLengthEditText;
    EditText stepPeriodeEditText;
    EditText directionOffsetEditText;
    Switch enableManualDirection;
    Switch enableStatisticalParticles;
    Switch enableCompassTrueDirection;  //else north

    int stepLengthmm;
    int stepPeriodems;
    int directionOffset;
    boolean manualDirectionEnabled;
    boolean statisticalParticlesEnabled;
    boolean compassDirectionIsTrueDirection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
//TODO make reset preferences
        appContext = MainActivity.getAppContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);

        stepLengthEditText= (EditText)findViewById(R.id.editTextStepLength);
        stepPeriodeEditText= (EditText)findViewById(R.id.editTextStepPeriode);
        directionOffsetEditText = (EditText)findViewById(R.id.editTextDirectionOffset);
        enableManualDirection = findViewById(R.id.switchManualDirection);
        enableStatisticalParticles = findViewById(R.id.switchStatistical);
        enableCompassTrueDirection = findViewById(R.id.switchCompassTrueNorth);

        stepLengthmm = sharedPreferences.getInt("step_length", 0);
        stepPeriodems = sharedPreferences.getInt("step_periode", 0);
        directionOffset = sharedPreferences.getInt("direction_offset", 0);
        manualDirectionEnabled = sharedPreferences.getBoolean("manual_direction_enabled", false);
        statisticalParticlesEnabled = sharedPreferences.getBoolean("statistical_particles_enabled", false);
        compassDirectionIsTrueDirection = sharedPreferences.getBoolean("compass_is_true_direction", true);

        stepLengthEditText.setText(String.format("%d", stepLengthmm));
        stepPeriodeEditText.setText(String.format("%d", stepPeriodems));
        directionOffsetEditText.setText(String.format("%d", directionOffset));
        enableManualDirection.setChecked(manualDirectionEnabled);
        enableStatisticalParticles.setChecked(statisticalParticlesEnabled);
        enableCompassTrueDirection.setChecked(compassDirectionIsTrueDirection);

        if(manualDirectionEnabled) {
            enableManualDirection.setText(R.string.switch_manual_magnetometer_1);
        } else {
            enableManualDirection.setText(R.string.switch_manual_magnetometer_2);
        }
        if(statisticalParticlesEnabled) {
            enableStatisticalParticles.setText(R.string.switch_particles_statistical_random_1);
        } else {
            enableStatisticalParticles.setText(R.string.switch_particles_statistical_random_2);
        }
        if(compassDirectionIsTrueDirection) {
            enableCompassTrueDirection.setText(R.string.switch_compass_direction_true_north_1);
        } else {
            enableCompassTrueDirection.setText(R.string.switch_compass_direction_true_north_2);
        }

        stepLengthEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Log.i("settings", "before text changed\n");
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Log.i("settings", "changed:" + charSequence + String.format("i:%d, i1:%d, i2:%d", i, i1, i2));
            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    stepLengthmm = Integer.parseInt(stepLengthEditText.getText().toString());
                }catch (Exception e) {
                    Log.i("settings", String.format("parsed invalid stepLength %d\n", stepLengthmm));
                }

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("step_length", stepLengthmm);
                editor.commit();
                Log.i("settings", String.format("stepLength changed %d\n", stepLengthmm));
            }
        });

        stepPeriodeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    stepPeriodems = Integer.parseInt(stepPeriodeEditText.getText().toString());
                }catch (Exception e) {
                    Log.i("settings", String.format("parsed invalid stepPeriode %d\n", stepPeriodems));
                }

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("step_periode", stepPeriodems);
                editor.commit();
                Log.i("settings", String.format("step-periode changed %d\n", stepPeriodems));
            }
        });

        directionOffsetEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //
            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    directionOffset = Integer.parseInt(directionOffsetEditText.getText().toString());
                }catch (Exception e) {
                    Log.i("settings", String.format("parsed invalid direction offset %d\n", directionOffset));
                }

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("direction_offset", directionOffset);
                editor.commit();
                Log.i("settings", String.format("direction offset changed %d\n", directionOffset));
            }
        });

        enableManualDirection.setOnCheckedChangeListener(   new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                manualDirectionEnabled = enableManualDirection.isChecked();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("manual_direction_enabled", manualDirectionEnabled);
                editor.commit();

                if(manualDirectionEnabled) {
                    enableManualDirection.setText(R.string.switch_manual_magnetometer_1);
                } else {
                    enableManualDirection.setText(R.string.switch_manual_magnetometer_2);
                }
            }
        });

        enableStatisticalParticles.setOnCheckedChangeListener(   new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                statisticalParticlesEnabled = buttonView.isChecked();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("statistical_particles_enabled", statisticalParticlesEnabled);
                editor.commit();

                if(statisticalParticlesEnabled) {
                    buttonView.setText(R.string.switch_particles_statistical_random_1);
                } else {
                    buttonView.setText(R.string.switch_particles_statistical_random_2);
                }
            }
        });

        enableCompassTrueDirection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                compassDirectionIsTrueDirection = buttonView.isChecked();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("compass_is_true_direction", compassDirectionIsTrueDirection);
                editor.commit();

                if(compassDirectionIsTrueDirection) {
                    buttonView.setText(R.string.switch_compass_direction_true_north_1);
                } else {
                    buttonView.setText(R.string.switch_compass_direction_true_north_2);
                }
            }
        });


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
