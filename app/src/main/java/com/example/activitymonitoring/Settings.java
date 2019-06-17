package com.example.activitymonitoring;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

public class Settings extends AppCompatActivity {

    private static Context appContext;
    SharedPreferences sharedPreferences;

    EditText stepLengthEditText;
    EditText stepPeriodEditText;
    EditText directionOffsetEditText;
    EditText directionUncertaintyEditText;
    EditText lengthUncertaintyEditText;
    Switch enableManualDirection;
    Switch enableLowVarianceResampling;
    Switch enableCompassTrueDirection;  //else north
    Button btnReset;

    int stepLengthmm;
    int stepPeriodms;
    int directionOffset;
    int directionUncertainty;
    int lengthUncertainty;
    boolean manualDirectionEnabled;
    boolean lowVarianceResamplingEnabled;
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
        stepPeriodEditText = (EditText)findViewById(R.id.editTextStepPeriode);
        directionOffsetEditText = (EditText)findViewById(R.id.editTextDirectionOffset);
        directionUncertaintyEditText = (EditText)findViewById(R.id.editTextDirectionUncertainty);
        lengthUncertaintyEditText = (EditText)findViewById(R.id.editTextLengthUncertainty);
        enableManualDirection = findViewById(R.id.switchManualDirection);
        enableLowVarianceResampling = findViewById(R.id.switchStatistical);
        enableCompassTrueDirection = findViewById(R.id.switchCompassTrueNorth);

        loadPreferences();
        updateSettingElements();


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

        stepPeriodEditText.addTextChangedListener(new TextWatcher() {
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
                    stepPeriodms = Integer.parseInt(stepPeriodEditText.getText().toString());
                }catch (Exception e) {
                    Log.i("settings", String.format("parsed invalid stepPeriod %d\n", stepPeriodms));
                }

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("step_period", stepPeriodms);
                editor.commit();
                Log.i("settings", String.format("step-period changed %d\n", stepPeriodms));
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

        directionUncertaintyEditText.addTextChangedListener(new TextWatcher() {
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
                    directionUncertainty = Integer.parseInt(directionUncertaintyEditText.getText().toString());
                }catch (Exception e) {
                    Log.i("settings", String.format("parsed invalid direction uncertainty %d\n", directionUncertainty));
                }

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("direction_uncertainty", directionUncertainty);
                editor.commit();
                Log.i("settings", String.format("direction uncertainty changed %d\n", directionUncertainty));

            }
        });

        lengthUncertaintyEditText.addTextChangedListener(new TextWatcher() {
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
                    lengthUncertainty = Integer.parseInt(lengthUncertaintyEditText.getText().toString());
                }catch (Exception e) {
                    Log.i("settings", String.format("parsed invalid length uncertainty %d\n", lengthUncertainty));
                }

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("length_uncertainty", lengthUncertainty);
                editor.commit();
                Log.i("settings", String.format("length uncertainty changed %d\n", lengthUncertainty));

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

        enableLowVarianceResampling.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                lowVarianceResamplingEnabled = buttonView.isChecked();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("low_variance_resampling_enabled", lowVarianceResamplingEnabled);
                editor.commit();

                if(lowVarianceResamplingEnabled) {
                    buttonView.setText(R.string.switch_particles_low_variance_resampling_1);
                } else {
                    buttonView.setText(R.string.switch_particles_low_variance_resampling_2);
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

        btnReset = findViewById(R.id.btnReset);
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("step_length", 850);
                editor.putInt("step_period",  800);
                editor.putInt("direction_offset", -50); //TODO
                editor.putInt("direction_uncertainty", 20); //TODO
                editor.putInt("length_uncertainty", 10);
                editor.putBoolean("low_variance_resampling_enabled", true);
                editor.putBoolean("manual_direction_enabled", false);
                editor.putBoolean("compass_is_true_direction", true);
                editor.commit();

                loadPreferences();
                updateSettingElements();
            }
        });

        //FloatingActionButton fab = findViewById(R.id.fab);
        //fab.setOnClickListener(new View.OnClickListener() {
        //    @Override
        //    public void onClick(View view) {
        //        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //                .setAction("Action", null).show();
        //    }
        //});
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void updateSettingElements() {
        stepLengthEditText.setText(String.format("%d", stepLengthmm));
        stepPeriodEditText.setText(String.format("%d", stepPeriodms));
        directionOffsetEditText.setText(String.format("%d", directionOffset));
        directionUncertaintyEditText.setText(String.format("%d", directionUncertainty));
        lengthUncertaintyEditText.setText(String.format("%d", lengthUncertainty));

        enableManualDirection.setChecked(manualDirectionEnabled);
        enableLowVarianceResampling.setChecked(lowVarianceResamplingEnabled);
        enableCompassTrueDirection.setChecked(compassDirectionIsTrueDirection);

        if(manualDirectionEnabled) {
            enableManualDirection.setText(R.string.switch_manual_magnetometer_1);
        } else {
            enableManualDirection.setText(R.string.switch_manual_magnetometer_2);
        }
        if(lowVarianceResamplingEnabled) {
            enableLowVarianceResampling.setText(R.string.switch_particles_low_variance_resampling_1);
        } else {
            enableLowVarianceResampling.setText(R.string.switch_particles_low_variance_resampling_2);
        }
        if(compassDirectionIsTrueDirection) {
            enableCompassTrueDirection.setText(R.string.switch_compass_direction_true_north_1);
        } else {
            enableCompassTrueDirection.setText(R.string.switch_compass_direction_true_north_2);
        }

    }

    public void loadPreferences() {
        stepLengthmm = sharedPreferences.getInt("step_length", 0);
        stepPeriodms = sharedPreferences.getInt("step_period", 0);
        directionOffset = sharedPreferences.getInt("direction_offset", 0);
        directionUncertainty = sharedPreferences.getInt("direction_uncertainty", 0);
        lengthUncertainty = sharedPreferences.getInt("length_uncertainty", 0);

        manualDirectionEnabled = sharedPreferences.getBoolean("manual_direction_enabled", false);
        lowVarianceResamplingEnabled = sharedPreferences.getBoolean("low_variance_resampling_enabled", true);
        compassDirectionIsTrueDirection = sharedPreferences.getBoolean("compass_is_true_direction", true);
    }

}
