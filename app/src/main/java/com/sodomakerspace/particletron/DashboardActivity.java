package com.sodomakerspace.particletron;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;

public class DashboardActivity extends AppCompatActivity {

    // UI elements
    public TextView output;
    private LinearLayout topLayout;

    private ParticleDevice testDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Get our UI elements
        output = (TextView) findViewById(R.id.output_textiView);

        // Create our toolbar
        Toolbar dashboardToolbar = (Toolbar) findViewById(R.id.dashboard_toolbar);
        dashboardToolbar.setTitle("Dashboard");
        setSupportActionBar(dashboardToolbar);

        // Generate a button for every registered Particle device
        topLayout = (LinearLayout) findViewById(R.id.view_dashboardTop);
        createDeviceButtons(topLayout);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dashboard_buttons, menu);
        return true;
    }

    private void createDeviceButtons (final View view) {
        // Attempt to get the list of devices that belong to the currently logged-in user
        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, List<ParticleDevice>>() {
            @Override
            public List<ParticleDevice> callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                return ParticleCloudSDK.getCloud().getDevices();
            }

            @Override
            // Generate a new button for each registered device
            public void onSuccess(List<ParticleDevice> value) {
                // Populate an array to use with our array adapter
                String[] buttons = new String[value.size()];
                for (int i = 0; i < value.size(); i++) {
                    buttons[i] = value.get(i).getName();
                }

                // Populate our listview with all of our device names
                if (buttons != null) {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_list_item_1, buttons);
                    ListView listView = (ListView) findViewById(R.id.myListView);
                    listView.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(ParticleCloudException exception) {
                exception.printStackTrace();
            }
        });
    }

    public void toggleLight (View view) {
        lightSwitch(testDevice);
    }

    public void listDeviceFunctions (View view) {
        writeLine("Listing available functions on device: " + testDevice.getID());
        for (String name : testDevice.getFunctions()) {
            writeLine("Device has function: " + name);
        }
    }

    private void lightSwitch(final ParticleDevice device) {
        final List<String> commands = Arrays.asList("on");
        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Object>() {
            @Override
            public Object callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                try {
                    int resultCode = device.callFunction("led", commands);
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
