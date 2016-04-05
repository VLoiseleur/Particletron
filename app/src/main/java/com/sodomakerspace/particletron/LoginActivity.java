package com.sodomakerspace.particletron;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import java.io.IOException;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.utils.Async;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize our Particle SDK
        ParticleCloudSDK.init(this);
    }

    public void sendLogin(View view) {

        // Get the user's entered email and password
        final String email = ((EditText) findViewById(R.id.email_editText)).getText().toString();
        final String password = ((EditText) findViewById(R.id.password_editText)).getText().toString();

        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Object>() {
            @Override
            public Object callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                ParticleCloudSDK.getCloud().logIn(email, password);
                return -1;
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
