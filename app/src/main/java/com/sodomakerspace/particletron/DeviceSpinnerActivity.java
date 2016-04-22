package com.sodomakerspace.particletron;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import io.particle.android.sdk.cloud.ParticleDevice;

public class DeviceSpinnerActivity extends Activity implements AdapterView.OnItemSelectedListener {

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Spinner myFunctionList = DashboardActivity.functionList;
        String deviceName = (String) parent.getItemAtPosition(pos);
        List<String> availableFunctions = DashboardActivity.listDeviceFunction(deviceName);

        // Populate the function name spinner
        // Create an ArrayAdapter using the string list and a default spinner layout
        ArrayAdapter<String> adapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, availableFunctions);
        // Apply the adapter to the spinner
        if (myFunctionList != null && availableFunctions.size() > 0)
            myFunctionList.setAdapter(adapter);
    }

    public void onNothingSelected(AdapterView<?> parent) {}
}
