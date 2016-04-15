package com.sodomakerspace.particletron;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.utils.Async;

public class LoginActivity extends AppCompatActivity {

    // UI Elements
    private EditText emailField;
    private CheckBox saveUserCredentials;

    private SharedPreferences loginPref;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Grab our UI elements
        emailField = (EditText) findViewById(R.id.email_editText);
        saveUserCredentials = (CheckBox) findViewById(R.id.saveUserCredentials);
        final EditText passwordField = (EditText) findViewById(R.id.password_editText);

        // Populate our login field if we can find stored data
        loginPref = this.getPreferences(Context.MODE_PRIVATE);
        String defaultEmail = "";
        String loginEmail = loginPref.getString(getString(R.string.saved_email), defaultEmail);
        if (!loginEmail.isEmpty() && emailField != null)
            emailField.setText(loginEmail);

        final View view = this.getCurrentFocus();

        // Listener for user submitting their login information
        passwordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    // Submit info for login
                    sendLogin(view);
                    handled = true;
                }
                return handled;
            }
        });
    }

    @Override
    protected void onPause () {
        super.onPause();
        // Save any user information
        if (saveUserCredentials.isChecked()) {
            // Save login email
            String emailLogin = emailField.getText().toString();

            // Save boolean state of checkbox
            String checkbox = "true";

            SharedPreferences.Editor editor = loginPref.edit();
            editor.putString(getString(R.string.saved_email), emailLogin);
            editor.putString(getString(R.string.saved_preferences), checkbox);

            editor.apply();
        }
        else {
            SharedPreferences.Editor editor = loginPref.edit();
            editor.clear();
            editor.apply();
        }
    }

    public void sendLogin(View view) {

        // Get the user's entered email and password
        final String email = ((EditText) findViewById(R.id.email_editText)).getText().toString();
        final String password = ((EditText) findViewById(R.id.password_editText)).getText().toString();

        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Object>() {
            @Override
            public Object callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                ParticleCloudSDK.getCloud().logIn(email, password);
                return 0;
            }

            @Override
            public void onSuccess(Object o) {
                // We've logged in successfully so switch to the user's dashboard
                launchDashboard();
            }

            @Override
            public void onFailure(ParticleCloudException exception) {
                exception.printStackTrace();
            }
        });
    }

    private void launchDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
    }
}
