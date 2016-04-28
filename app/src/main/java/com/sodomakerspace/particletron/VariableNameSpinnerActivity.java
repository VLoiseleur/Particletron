package com.sodomakerspace.particletron;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;

public class VariableNameSpinnerActivity extends Activity implements AdapterView.OnItemSelectedListener {

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        String variableName = (String) parent.getItemAtPosition(pos);

        // Query the device
        DashboardActivity.queryDeviceVariable(variableName);
    }

    public void onNothingSelected(AdapterView<?> parent) {}
}
