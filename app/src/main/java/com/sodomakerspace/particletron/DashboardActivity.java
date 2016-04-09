package com.sodomakerspace.particletron;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;

public class DashboardActivity extends AppCompatActivity {

    // UI elements
    public TextView output;

    private ParticleDevice testDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Get our UI elements
        output = (TextView) findViewById(R.id.output_textiView);
        Toolbar dashboardToolbar = (Toolbar) findViewById(R.id.dashboard_toolbar);
        setSupportActionBar(dashboardToolbar);

        // Print the list of registered Particle devices for this account to our output log
        getDevices();
    }

    private void getDevices () {
        // Attempt to get the list of devices that belong to the currently logged-in user
        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Object>() {
            @Override
            public Object callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                List<ParticleDevice> devices = ParticleCloudSDK.getCloud().getDevices();
                for (int i = 0; i < devices.size(); i++) {
                    writeLine(devices.get(i).getName());
                    writeLine(devices.get(i).getID());
                    testDevice = devices.get(i);
                }
                return 0;
            }

            @Override
            public void onSuccess(Object o) {
            }

            @Override
            public void onFailure(ParticleCloudException exception) {
                exception.printStackTrace();
            }
        });
    }

    public void toggleLight (View view) {
        writeLine("Listing available functions on device: " + testDevice.getID());
        for (String name : testDevice.getFunctions()) {
            writeLine("Device has function: " + name);
        }

        Map<String, ParticleDevice.VariableType> variables = testDevice.getVariables();
        for (String name : variables.keySet()) {
            writeLine(String.format("variable '%s' type is '%s'", name, variables.get(name)));
        }
        //lightSwitch(testDevice);
    }

    private void lightSwitch(final ParticleDevice device) {
        final List<String> commands = Arrays.asList("D7", "high");
        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Object>() {
            @Override
            public Object callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                try {
                    int resultCode = device.callFunction("digitalwrite", commands);
                    writeLine("Result of running command: " + resultCode);
                } catch (ParticleDevice.FunctionDoesNotExistException e) {
                    e.printStackTrace();
                }
                return 0;
            }

            @Override
            public void onSuccess(Object o) {
                writeLine("Successfully toggled light!");
            }

            @Override
            public void onFailure(ParticleCloudException exception) {
                writeLine("Error attempting to toggle light!");
                exception.printStackTrace();
            }
        });
    }

    // Write some text to the output text view.
    // Care is taken to do this on the main UI thread so writeLine can be called
    // from any thread (like the BTLE callback).
    private void writeLine(final CharSequence text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                output.append(text);
                output.append("\n");
            }
        });
    }
}
