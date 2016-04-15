package com.sodomakerspace.particletron;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
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

    private List<ParticleDevice> myDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Get our UI elements
        output = (TextView) findViewById(R.id.output_textView);

        // Create our toolbar
        Toolbar dashboardToolbar = (Toolbar) findViewById(R.id.dashboard_toolbar);
        dashboardToolbar.setTitle("Dashboard");
        setSupportActionBar(dashboardToolbar);

        // Generate a button for every registered Particle device
        LinearLayout dashboardLayout = (LinearLayout) findViewById(R.id.view_dashboard);
        createDeviceButtons(dashboardLayout);
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
                myDevices = ParticleCloudSDK.getCloud().getDevices();
                writeLine("Found " + myDevices.size() + " registered devices.");
                return ParticleCloudSDK.getCloud().getDevices();
            }

            @Override
            // Generate a new button for each registered device
            public void onSuccess(List<ParticleDevice> value) {
                // Populate an array to use with our array adapter
                String[] deviceNames = new String[value.size()];
                writeLine("Listing all devices registered to this account:");
                for (int i = 0; i < value.size(); i++) {
                    deviceNames[i] = value.get(i).getName();
                    writeLine(deviceNames[i]);
                }

//                // Populate our listview with all of our device names
//                if (deviceNames != null) {
//                    DeviceAdapter<String> adapter = new DeviceAdapter<>(view.getContext(), android.R.layout.simple_list_item_1, deviceNames);
//                    ListView listView = (ListView) findViewById(R.id.device_listView);
//                    listView.setAdapter(adapter);
//                }
            }

            @Override
            public void onFailure(ParticleCloudException exception) {
                exception.printStackTrace();
            }
        });
    }

    public void toggleLight (View view) {
        String deviceName = ((EditText) findViewById(R.id.device_name)).getText().toString();
        ParticleDevice targetDevice = null;

        for (ParticleDevice d: myDevices) {
            if (d.getName().equals(deviceName)) {
                targetDevice = d;
            }
        }

        // TODO: Should probably add some verification that the function exists on the device
        if (targetDevice != null) {
            writeLine("Attempting to run function");
            callDeviceFunction(targetDevice);
        }
        else writeLine("Target device not found!");
    }

    public void getDeviceFunctions (View view) {
        String deviceName = ((EditText) findViewById(R.id.device_name)).getText().toString();
        ParticleDevice targetDevice = null;

        for (ParticleDevice d: myDevices) {
            if (d.getName().equals(deviceName)) {
                targetDevice = d;
            }
        }

        if (targetDevice != null) {
            listDeviceFunctions(targetDevice);
        }
    }

    public void logOut (View view) {
        ParticleCloudSDK.getCloud().logOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void listDeviceFunctions (ParticleDevice device) {
        writeLine("Listing available functions on device: " + device.getName());
        for (String name : device.getFunctions()) {
            writeLine("Device has function: " + name);
        }
    }

    private void callDeviceFunction (final ParticleDevice device) {
        final String function = ((EditText) findViewById(R.id.function_name)).getText().toString();
        final List<String> commands = Arrays.asList(((EditText) findViewById(R.id.function_parameters)).getText().toString());

        if (device != null) {
            Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Integer>() {
                @Override
                public Integer callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                    try {
                        return device.callFunction(function, commands);
                    } catch (ParticleDevice.FunctionDoesNotExistException e) {
                        e.printStackTrace();
                    }
                    return -1;
                }

                @Override
                public void onSuccess (Integer i) {
                    if (i == 1)
                        writeLine("Successfully called: " + function);
                    else
                        writeLine("Function call returned with error code: " + i);
                }

                @Override
                public void onFailure(ParticleCloudException exception) {
                    writeLine("Error attempting to toggle light!");
                    exception.printStackTrace();
                }
            });
        }
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
