package com.sodomakerspace.particletron;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.security.keystore.KeyProperties;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

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

    // SharedPreferences file properties
    public static final String PREFS_NAME = "loginPrefs";
    private SharedPreferences loginPref;
    private SecurePreferences securePreferences;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Reference our UI elements
        emailField = (EditText) findViewById(R.id.email_editText);
        passwordField = (EditText) findViewById(R.id.password_editText);
        loginButton = (Button) findViewById(R.id.login_button);

        // Reference our SharedPreferences file
        loginPref = this.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String defaultField = "";

        /* TODO: If we ever update this app to API23 or higher, switch to Android Keystore,
         using KeyGenParameterSpec and a PBE-based algorithm to generate and store the key
        */

        // Check for an existing encryption key from SharedPreferences
        if (loginPref.getString(getString(R.string.encryption_key), defaultField).equals("")) {
            // If we don't have a key, generate a new one
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES);
                keyGen.init(256);
                SecretKey secretKey = keyGen.generateKey();

                // Then store the new key in SharedPreferences
                SharedPreferences.Editor editor = loginPref.edit();
                editor.putString(getString(R.string.encryption_key), secretKey.getEncoded().toString());
                editor.apply();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Instantiate new SecurePreferences using our encryption key
        securePreferences = new SecurePreferences(this, PREFS_NAME, loginPref.getString(getString(R.string.encryption_key), defaultField), true);

        // Populate our login field if we can find stored data
        String loginEmail = loginPref.getString(getString(R.string.saved_email), defaultField);
        if (!loginEmail.isEmpty() && emailField != null)
            emailField.setText(loginEmail);

        if (securePreferences.getString(getString(R.string.saved_password)) == null) {
            passwordField.setText("");
        }
        else if (!securePreferences.getString(getString(R.string.saved_password)).isEmpty() && passwordField != null) {
            passwordField.setText(securePreferences.getString(getString(R.string.saved_password)));
        }

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

        // Save login email and password
        String emailLogin = emailField.getText().toString();
        String passwordLogin = passwordField.getText().toString();

        SharedPreferences.Editor editor = loginPref.edit();
        editor.putString(getString(R.string.saved_email), emailLogin);
        securePreferences.put(getString(R.string.saved_password), passwordLogin);
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
