package com.sodomakerspace.particletron;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

public class ControllerActivity extends AppCompatActivity {

    // UI elements
    private static LinearLayout controllerLayout;
    private static ImageView deviceStatus;
    private static Spinner deviceList;
    private static Spinner functionList;
    private static Spinner variableList;
    private static EditText functionParameters;
    private static Button functionSend;
    private static Button functionSendBool;
    private static Button variableRead;
    private static TextView outputLog;

    private static List<ParticleDevice> _myDevices;
    private static List<String> _deviceNames;

    private boolean toggled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);

        // Get our UI elements
        controllerLayout = (LinearLayout) findViewById(R.id.view_controller);
        deviceStatus = (ImageView) findViewById(R.id.deviceStatus_image);
        deviceList = (Spinner) findViewById(R.id.deviceName_spinner);
        functionList = (Spinner) findViewById(R.id.functionName_spinner);
        variableList = (Spinner) findViewById(R.id.variableName_spinner);
        functionParameters = (EditText) findViewById(R.id.function_parameters);
        functionSend = (Button) findViewById(R.id.send_button);
        functionSendBool = (Button) findViewById(R.id.sendBool_button);
        variableRead = (Button) findViewById(R.id.read_button);
        outputLog = (TextView) findViewById(R.id.output_textView);

        // Disable initial user input
        disableUserInput(true);

        // Create our toolbar
        Toolbar controllerToolbar = (Toolbar) findViewById(R.id.controller_toolbar);
        if (controllerToolbar != null) {
            // TODO: Figure out how to do this in the XML?
            controllerToolbar.setTitle(R.string.appbarTitle_controller);
            setSupportActionBar(controllerToolbar);
        }

        // Set up our spinner listener
        DeviceSpinnerActivity deviceSpinnerActivity = new DeviceSpinnerActivity();
        deviceList.setOnItemSelectedListener(deviceSpinnerActivity);

        // Query our devices
        getDevices();

        // Listener for user calling device function
        functionParameters.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                String myDeviceName = deviceList.getSelectedItem().toString();
                ParticleDevice myDevice = null;

                for (ParticleDevice d: _myDevices) {
                    if (d.getName().equals(myDeviceName))
                        myDevice = d;
                }

                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    // Call device function
                    if (myDevice != null)
                        callDeviceFunction(myDevice);
                    handled = true;
                }
                return handled;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.controller_buttons, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                // Refresh device, function, and variable lists
                disableUserInput(true);
                getDevices();
                return true;

            case R.id.action_logout:
                logOut();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    private static void populateSpinners(View view) {
        if (_deviceNames != null) {
            // Create an ArrayAdapter using the string list and a default spinner layout
            ArrayAdapter<String> adapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, _deviceNames);
            // Apply the adapter to the spinner
            deviceList.setAdapter(adapter);
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

    public void sendBoolFunction (View view) {
        String deviceName = deviceList.getSelectedItem().toString();
        ParticleDevice targetDevice = null;

        for (ParticleDevice d: _myDevices) {
            if (d.getName().equals(deviceName)) {
                targetDevice = d;
            }
        }

        if (targetDevice != null)
            callDeviceBoolFunction(targetDevice);
    }

    public void readDeviceVariable (View view) {
        String deviceName = deviceList.getSelectedItem().toString();
        ParticleDevice targetDevice = null;

        for (ParticleDevice d: _myDevices) {
            if (d.getName().equals(deviceName)) {
                targetDevice = d;
            }
        }

        if (targetDevice != null)
            queryDeviceVariable(targetDevice);
    }

    private static void disableUserInput (boolean disabled) throws NullPointerException {
        if (disabled) {
            deviceList.setEnabled(false);
            functionList.setEnabled(false);
            variableList.setEnabled(false);
            functionSend.setEnabled(false);
            functionSendBool.setEnabled(false);
            variableRead.setEnabled(false);
        }
        else {
            deviceList.setEnabled(true);
            functionList.setEnabled(true);
            variableList.setEnabled(true);
            functionSend.setEnabled(true);
            functionSendBool.setEnabled(true);
            variableRead.setEnabled(true);
        }
    }

    public void logOut () {
        ParticleCloudSDK.getCloud().logOut();
        Toaster.l(ControllerActivity.this, "Logged out successfully");
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
                populateSpinners(controllerLayout);
            }

            @Override
            public void onFailure(ParticleCloudException exception) {
                exception.printStackTrace();
            }
        });
    }

    public static void updateDeviceStatus (String deviceName) {
        if (_myDevices != null && deviceName != null && deviceStatus != null) {
            // Find our target device
            for (ParticleDevice d : _myDevices) {
                if (d.getName().equals(deviceName)) {
                    updateFunctionList(d);
                    updateVariableList(d);
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

    public static void updateVariableList (ParticleDevice device) {
        final List<String> variableNames = new ArrayList<>();
        final ParticleDevice particleDevice = device;

        disableUserInput(true);

        // Attempt to wake the device before querying its variables
        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, List<String>>() {
            @Override
            public List<String> callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                particleDevice.refresh();
                Map<String, ParticleDevice.VariableType> variables = particleDevice.getVariables();
                for (String name : variables.keySet()) {
                    variableNames.add(name);
                }
                return variableNames;
            }

            // Populate available variables in dropdown menu
            @Override
            public void onSuccess (List<String> variables) {
                if (particleDevice.isConnected())
                    deviceStatus.setImageResource(R.drawable.ic_flash_on_black_24dp);
                else
                    deviceStatus.setImageResource(R.drawable.ic_flash_off_black_24dp);

                if (variables.size() == 0)
                    variables.add("No variables found");

                // Create an ArrayAdapter using the string list and a default spinner layout
                ArrayAdapter<String> adapter = new ArrayAdapter<>(controllerLayout.getContext(), android.R.layout.simple_spinner_dropdown_item, variables);
                // Apply the adapter to the spinner
                if (variableList != null)
                    variableList.setAdapter(adapter);

                disableUserInput(false);
            }

            @Override
            public void onFailure(ParticleCloudException exception) {
                exception.printStackTrace();
                disableUserInput(false);
            }
        });
    }

    public static void updateFunctionList (ParticleDevice device) {
        final List<String> functionNames = new ArrayList<>();
        final ParticleDevice particleDevice = device;

        disableUserInput(true);

        // Attempt to wake the device before querying its functions
        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, List<String>>() {
            @Override
            public List<String> callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                particleDevice.refresh();
                for (String s : particleDevice.getFunctions())
                    functionNames.add(s);
                return functionNames;
            }

            // Populate available functions in dropdown menu
            @Override
            public void onSuccess (List<String> functions) {
                if (particleDevice.isConnected())
                    deviceStatus.setImageResource(R.drawable.ic_flash_on_black_24dp);
                else
                    deviceStatus.setImageResource(R.drawable.ic_flash_off_black_24dp);

                if (functions.size() == 0)
                    functions.add("No functions found");

                // Create an ArrayAdapter using the string list and a default spinner layout
                ArrayAdapter<String> adapter = new ArrayAdapter<>(controllerLayout.getContext(), android.R.layout.simple_spinner_dropdown_item, functions);
                // Apply the adapter to the spinner
                if (functionList != null)
                    functionList.setAdapter(adapter);

                disableUserInput(false);
            }

            @Override
            public void onFailure(ParticleCloudException exception) {
                exception.printStackTrace();
                disableUserInput(false);
            }
        });
    }

    private void callDeviceBoolFunction (ParticleDevice device) {
        final ParticleDevice particleDevice = device;
        final String function = (String) functionList.getSelectedItem();

        disableUserInput(true);

        if (particleDevice != null) {
            Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Integer>() {
                @Override
                public Integer callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                    List<String> commands = null;

                    // Check for global status variable
                    try {
                        if (particleDevice.getVariable("status").toString().equals("1.0"))
                            commands = Arrays.asList("0");
                        else if (particleDevice.getVariable("status").toString().equals("0.0"))
                            commands = Arrays.asList("1");
                    } catch (ParticleDevice.VariableDoesNotExistException e) {
                        e.printStackTrace();
                    }

                    // Could not find a global status variable on target device
                    if (commands == null) {
                        if (toggled) {
                            commands = Arrays.asList("0");
                            toggled = false;
                        }
                        else {
                            commands = Arrays.asList("1");
                            toggled = true;
                        }
                    }

                    try {
                        return particleDevice.callFunction(function, commands);
                    } catch (ParticleDevice.FunctionDoesNotExistException e) {
                        e.printStackTrace();
                    }
                    return 1;
                }

                @Override
                public void onSuccess (Integer i) {
                    Toaster.l(ControllerActivity.this, "Function ran with return value of " + i);
                    disableUserInput(false);
                }

                @Override
                public void onFailure(ParticleCloudException exception) {
                    Toaster.l(ControllerActivity.this, "Error attempting to run function!");
                    exception.printStackTrace();
                    disableUserInput(false);
                }
            });
        }
    }

    private void callDeviceFunction (ParticleDevice device) {
        final ParticleDevice particleDevice = device;
        final String function = (String) functionList.getSelectedItem();
        final List<String> commands = Arrays.asList(functionParameters.getText().toString());

        disableUserInput(true);

        if (particleDevice != null) {
            Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Integer>() {
                @Override
                public Integer callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                    try {
                        return particleDevice.callFunction(function, commands);
                    } catch (ParticleDevice.FunctionDoesNotExistException e) {
                        e.printStackTrace();
                    }
                    return 1;
                }

                @Override
                public void onSuccess (Integer i) {
                    Toaster.l(ControllerActivity.this, "Function ran with return value of " + i);
                    functionParameters.setText("");
                    disableUserInput(false);
                }

                @Override
                public void onFailure(ParticleCloudException exception) {
                    Toaster.l(ControllerActivity.this, "Error attempting to run function!");
                    exception.printStackTrace();
                    disableUserInput(false);
                }
            });
        }
    }

    public static void queryDeviceVariable (ParticleDevice device) {
        final ParticleDevice particleDevice = device;
        final String variableName = (String) variableList.getSelectedItem();

        if (particleDevice != null) {
            disableUserInput(true);

            // Attempt to wake the device before querying the selected variable
            Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, String>() {
                @Override
                public String callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                    try {
                        String value = null;
                        particleDevice.refresh();
                        Map<String, ParticleDevice.VariableType> variables = particleDevice.getVariables();

                        for (String name : variables.keySet()) {
                            if (name.equals(variableName)) {
                                value = particleDevice.getVariable(name).toString();
                            }
                        }

                        return value;
                    }
                    catch (ParticleDevice.VariableDoesNotExistException e) {
                        return null;
                    }
                }

                // Print variable name to text view
                @Override
                public void onSuccess(String variable) {
                    if (particleDevice.isConnected()) {
                        deviceStatus.setImageResource(R.drawable.ic_flash_on_black_24dp);
                        if (variable == null || variable.equals(""))
                            outputLog.append("No variables found" + "\n");
                        else
                            outputLog.append("Variable " + variableName + " has value of: " + variable + "\n");
                    }
                    else
                        deviceStatus.setImageResource(R.drawable.ic_flash_off_black_24dp);

                    disableUserInput(false);
                }

                @Override
                public void onFailure(ParticleCloudException exception) {
                    exception.printStackTrace();
                    disableUserInput(false);
                }
            });
        }
    }
}
