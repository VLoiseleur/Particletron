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

public class DashboardActivity extends AppCompatActivity {

    // UI elements

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Get our UI elements

        // Create our toolbar
        Toolbar dashboardToolbar = (Toolbar) findViewById(R.id.dashboard_toolbar);
        if (dashboardToolbar != null) {
            // TODO: Figure out how to do this in the XML file?
            dashboardToolbar.setTitle(R.string.appbarTitle_dashboard);
            setSupportActionBar(dashboardToolbar);
        }
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dashboard_buttons, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_controller:
                // Launch the controller, where the user can directly interact with their particle devices
                launchController();
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

    @Override
    public void onBackPressed() {
        logOut();
    }

    private void launchController () {
        Intent intent = new Intent(this, ControllerActivity.class);
        startActivity(intent);
    }

    public void logOut () {
        ParticleCloudSDK.getCloud().logOut();
        Toaster.l(DashboardActivity.this, "Logged out successfully");
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }
}
