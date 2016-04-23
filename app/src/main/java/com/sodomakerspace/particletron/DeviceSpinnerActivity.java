package com.sodomakerspace.particletron;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;

public class DeviceSpinnerActivity extends Activity implements AdapterView.OnItemSelectedListener {

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        String deviceName = (String) parent.getItemAtPosition(pos);

        // TODO: Lock the function field during the query
        // Query the device
        DashboardActivity.updateDeviceStatus(deviceName);
    }

    public void onNothingSelected(AdapterView<?> parent) {}
}
