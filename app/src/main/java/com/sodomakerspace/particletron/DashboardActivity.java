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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

public class DashboardActivity extends AppCompatActivity {

    // UI elements
    private static Spinner deviceList;
    private static Spinner functionList;
    private static LinearLayout dashboardLayout;
    private static ImageView deviceStatus;

    private static List<ParticleDevice> _myDevices;
    private static List<String> _deviceNames;
    private static List<String> deviceFunctions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Get our UI elements
        deviceList = (Spinner) findViewById(R.id.deviceName_spinner);
        functionList = (Spinner) findViewById(R.id.functionName_spinner);
        dashboardLayout = (LinearLayout) findViewById(R.id.view_dashboard);
        deviceStatus = (ImageView) findViewById(R.id.deviceStatus_image);

        // Create our toolbar
        Toolbar dashboardToolbar = (Toolbar) findViewById(R.id.dashboard_toolbar);
        if (dashboardToolbar != null) {
            dashboardToolbar.setTitle("Dashboard");
            setSupportActionBar(dashboardToolbar);
        }

        // Query our devices
        getDevices();
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dashboard_buttons, menu);
        return true;
    }

    private static void populateDeviceSpinner (View view) {
        if (_deviceNames != null) {
            // TODO: This is pretty hacky to refresh so find a better solution
            _deviceNames.add("Refresh list");

            // Create an ArrayAdapter using the string list and a default spinner layout
            ArrayAdapter<String> adapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, _deviceNames);
            // Apply the adapter to the spinner
            deviceList.setAdapter(adapter);
            // Set up our spinner listener to populate our function spinner
            DeviceSpinnerActivity mySpinnerActivity = new DeviceSpinnerActivity();
            deviceList.setOnItemSelectedListener(mySpinnerActivity);
        }
    }

    public void sendFunction (View view) {
        String deviceName = deviceList.getSelectedItem().toString();
        ParticleDevice targetDevice = null;

        for (ParticleDevice d: _myDevices) {
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

    public static void getDevices () {
        // Attempt to get the list of devices that belong to the currently logged-in user
        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Integer>() {
            @Override
            public Integer callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                _myDevices = ParticleCloudSDK.getCloud().getDevices();
                return 1;
            }

            @Override
            // Populate a spinner with each registered device
            public void onSuccess(Integer i) {
                getDeviceNames(_myDevices);
                populateDeviceSpinner(dashboardLayout);
            }

            @Override
            public void onFailure(ParticleCloudException exception) {
                exception.printStackTrace();
            }
        });
    }

    public static void updateDeviceStatus (String deviceName) {
        if (_myDevices != null && deviceName != null && deviceStatus != null) {
            if (deviceName.equals("Refresh list")) {
                deviceStatus.setImageResource(R.drawable.ic_flash_off_black_24dp);
                deviceFunctions = new ArrayList<>();
                deviceFunctions.add("No functions found");
                // Create an ArrayAdapter using the string list and a default spinner layout
                ArrayAdapter<String> adapter = new ArrayAdapter<>(dashboardLayout.getContext(), android.R.layout.simple_spinner_dropdown_item, deviceFunctions);
                // Apply the adapter to the spinner
                if (functionList != null)
                    functionList.setAdapter(adapter);
            }

            // Find our target device
            for (ParticleDevice d : _myDevices) {
                if (d.getName().equals(deviceName)) {
                    final ParticleDevice particleDevice = d;

                    // Attempt to wake the device
                    Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Integer>() {
                        @Override
                        public Integer callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                            particleDevice.refresh();
                            return 1;
                        }

                        @Override
                        public void onSuccess(Integer i) {
                            if (particleDevice.isConnected()) {
                                deviceStatus.setImageResource(R.drawable.ic_flash_on_black_24dp);
                            } else
                                deviceStatus.setImageResource(R.drawable.ic_flash_off_black_24dp);

                            // Populate available functions in dropdown menu
                            getDeviceFunction(particleDevice.getName());
                            if (deviceFunctions == null || deviceFunctions.size() == 0) {
                                deviceFunctions = new ArrayList<>();
                                deviceFunctions.add("No functions found");
                            }
                            // Create an ArrayAdapter using the string list and a default spinner layout
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(dashboardLayout.getContext(), android.R.layout.simple_spinner_dropdown_item, deviceFunctions);
                            // Apply the adapter to the spinner
                            if (functionList != null)
                                functionList.setAdapter(adapter);
                        }

                        @Override
                        public void onFailure(ParticleCloudException exception) {
                            exception.printStackTrace();
                        }
                    });
                }
            }
        }
    }

    public static void getDeviceNames (List<ParticleDevice> devices) {
        List<String> names = new ArrayList<>();

        if (devices != null ) {
            for (int i = 0; i < devices.size(); i++) {
                names.add(devices.get(i).getName());
            }

            _deviceNames = names;
        }
    }

    public static void getDeviceFunction(String deviceName) {
        List<String> functionNames = new ArrayList<>();

        if (_myDevices != null && deviceName != null) {
            // Find our target device
            for (ParticleDevice d : _myDevices) {
                if (d.getName().equals(deviceName)) {
                    if (d.isConnected()) {
                        // Add all available function names on target device to our return list
                        for (String s : d.getFunctions()) {
                            functionNames.add(s);
                        }
                    }
                }
            }
            deviceFunctions = functionNames;
        }
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
                    if (i != -1)
                        Toaster.l(DashboardActivity.this, "Function ran successfully with return value of " + i);
                    else
                        Toaster.l(DashboardActivity.this, "Error when attempting to call function!");
                }

                @Override
                public void onFailure(ParticleCloudException exception) {
                    exception.printStackTrace();
                }
            });
        }
    }
}
