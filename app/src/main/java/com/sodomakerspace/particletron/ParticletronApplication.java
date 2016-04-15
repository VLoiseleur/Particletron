package com.sodomakerspace.particletron;

import android.app.Application;

import io.particle.android.sdk.cloud.ParticleCloudSDK;

public class ParticletronApplication extends Application {

    // Need to initialize our Particle SDK before use elsewhere in the app
    @Override
    public void onCreate () {
        super.onCreate();
        ParticleCloudSDK.init(this);
    }
}
