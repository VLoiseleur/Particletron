package com.sodomakerspace.particletron;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import io.particle.android.sdk.cloud.ParticleCloudSDK;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize our Particle SDK
        ParticleCloudSDK.init(this);
    }

    public void sendLogin(View view){

    }
}
