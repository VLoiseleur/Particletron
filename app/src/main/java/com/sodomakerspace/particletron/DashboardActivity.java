package com.sodomakerspace.particletron;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;

public class DashboardActivity extends AppCompatActivity {

    // UI elements
    private static Spinner deviceList;
    public static Spinner functionList;

    private static List<ParticleDevice> myDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Get our UI elements
        deviceList = (Spinner) findViewById(R.id.deviceName_spinner);
        functionList = (Spinner) findViewById(R.id.functionName_spinner);

        // Create our toolbar
        Toolbar dashboardToolbar = (Toolbar) findViewById(R.id.dashboard_toolbar);
        if (dashboardToolbar != null) {
            dashboardToolbar.setTitle("Dashboard");
            setSupportActionBar(dashboardToolbar);
        }

        // Populate a spinner with every registered Particle device
        LinearLayout dashboardLayout = (LinearLayout) findViewById(R.id.view_dashboard);
        populateDeviceSpinner(dashboardLayout);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dashboard_buttons, menu);
        return true;
    }

    private void populateDeviceSpinner (final View view) {
        // Attempt to get the list of devices that belong to the currently logged-in user
        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, List<ParticleDevice>>() {
            @Override
            public List<ParticleDevice> callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                myDevices = ParticleCloudSDK.getCloud().getDevices();
                return ParticleCloudSDK.getCloud().getDevices();
            }

            @Override
            // Populate a spinner with each registered device
            public void onSuccess(List<ParticleDevice> value) {
                // Populate a string list to use with our array adapter
                List<String> deviceNames = new ArrayList<>();
                for (int i = 0; i < value.size(); i++) {
                    deviceNames.add(value.get(i).getName());
                }

                // Create an ArrayAdapter using the string list and a default spinner layout
                ArrayAdapter<String> adapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, deviceNames);
                // Apply the adapter to the spinner
                deviceList.setAdapter(adapter);
                // Set up our spinner listener to populate our function spinner
                DeviceSpinnerActivity mySpinnerActivity = new DeviceSpinnerActivity();
                deviceList.setOnItemSelectedListener(mySpinnerActivity);
            }

            @Override
            public void onFailure(ParticleCloudException exception) {
                exception.printStackTrace();
            }
        });
    }

    public void sendFunction (View view) {
        String deviceName = deviceList.getSelectedItem().toString();
        ParticleDevice targetDevice = null;

        for (ParticleDevice d: myDevices) {
            if (d.getName().equals(deviceName)) {
                targetDevice = d;
            }
        }

        if (targetDevice != null)
            callDeviceFunction(targetDevice);
    }

    public void logOut (View view) {
        ParticleCloudSDK.getCloud().logOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public static List<String> listDeviceFunction (String deviceName) {
        final List<String> functionNames = new ArrayList<>();

        // Find our target device
        for (ParticleDevice d: myDevices) {
            if (d.getName().equals(deviceName)) {
                if (d.isConnected()) {
                    // Add all available function names on target device to our return list
                    for (String s : d.getFunctions()) {
                        functionNames.add(s);
                    }
                }
            }
        }
        return functionNames;
    }

    private void callDeviceFunction (final ParticleDevice device) {
        final String function = (String) functionList.getSelectedItem();
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
                    // TODO: Should put a success toast here or something.
                }

                @Override
                public void onFailure(ParticleCloudException exception) {
                    exception.printStackTrace();
                }
            });
        }
    }
}
