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
    Switch enableManualDirection;
    Switch enableStatisticalParticles;

    int stepLengthmm;
    int stepPeriodems;
    boolean manualDirectionEnabled;
    boolean statisticalParticlesEnabled;

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
        stepPeriodeEditText= (EditText)findViewById(R.id.editTextPeriode);
        enableManualDirection = findViewById(R.id.switchManualDirection);
        enableStatisticalParticles = findViewById(R.id.switchStatistical);

        stepLengthmm = sharedPreferences.getInt("step_length", 0);
        stepPeriodems = sharedPreferences.getInt("step_periode", 0);
        manualDirectionEnabled = sharedPreferences.getBoolean("manual_direction_enabled", false);
        statisticalParticlesEnabled = sharedPreferences.getBoolean("statistical_particles_enabled", false);

        stepLengthEditText.setText(String.format("%d", stepLengthmm));
        stepPeriodeEditText.setText(String.format("%d", stepPeriodems));
        enableManualDirection.setChecked(manualDirectionEnabled);
        enableStatisticalParticles.setChecked(statisticalParticlesEnabled);


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
                    stepPeriodems= Integer.parseInt(stepPeriodeEditText.getText().toString());
                }catch (Exception e) {
                    Log.i("settings", String.format("parsed invalid stepPeriode %d\n", stepPeriodems));
                }

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("step_periode", stepPeriodems);
                editor.commit();
                Log.i("settings", String.format("step-periode changed %d\n", stepPeriodems ));
            }
        });

        enableManualDirection.setOnCheckedChangeListener(   new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                manualDirectionEnabled = enableManualDirection.isChecked();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("manual_direction_enabled", manualDirectionEnabled);
                editor.commit();

                if(manualDirectionEnabled) {
                    enableManualDirection.setText("manual mode is enabled");
                } else {
                    enableManualDirection.setText("magnetometer mode is enabled");
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
                    buttonView.setText("statistical particles are enabled");
                } else {
                    buttonView.setText("particle mode random");
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
