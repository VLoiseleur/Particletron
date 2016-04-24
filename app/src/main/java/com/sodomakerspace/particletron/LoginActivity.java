package com.sodomakerspace.particletron;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

public class LoginActivity extends AppCompatActivity {

    // UI Elements
    private EditText emailField;
    private EditText passwordField;
    private Button loginButton;

    private SharedPreferences loginPref;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Grab our UI elements
        emailField = (EditText) findViewById(R.id.email_editText);
        passwordField = (EditText) findViewById(R.id.password_editText);
        loginButton = (Button) findViewById(R.id.login_button);

        // Populate our login field if we can find stored data
        loginPref = this.getPreferences(Context.MODE_PRIVATE);
        String defaultField = "";
        String loginEmail = loginPref.getString(getString(R.string.saved_email), defaultField);
        if (!loginEmail.isEmpty() && emailField != null)
            emailField.setText(loginEmail);

        String loginPassword = loginPref.getString(getString(R.string.saved_password), defaultField);
        if (!loginPassword.isEmpty() && passwordField != null)
            passwordField.setText(loginPassword);

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

        // Save login email
        String emailLogin = emailField.getText().toString();

        // TODO: Encrypt password
        String passwordLogin = passwordField.getText().toString();

        SharedPreferences.Editor editor = loginPref.edit();
        // Does getString() return the value or the key?
        editor.putString(getString(R.string.saved_email), emailLogin);
        editor.putString(getString(R.string.saved_password), passwordLogin);
        editor.apply();
    }

    public void sendLogin(View view) {

        // Get the user's entered email and password
        final String email = emailField.getText().toString();
        final String password = passwordField.getText().toString();

        loginButton.setEnabled(false);

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
                loginButton.setEnabled(true);
            }

            @Override
            public void onFailure(ParticleCloudException exception) {
                Toaster.l(LoginActivity.this, "Uh oh, those credentials don't match our records.");
                exception.printStackTrace();
                loginButton.setEnabled(true);
            }
        });
    }

    private void launchDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
    }
}
