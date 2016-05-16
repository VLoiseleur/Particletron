package com.sodomakerspace.particletron;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

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
    private RelativeLayout _dashboardLayout;

    private boolean _editMode;

    private List<ImageButton> _widgetList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Get our UI elements
        _dashboardLayout = (RelativeLayout) findViewById(R.id.dashboard_layout);

        // Set mode type
        _editMode = false;

        // Create our toolbar
        Toolbar dashboardToolbar = (Toolbar) findViewById(R.id.dashboard_toolbar);
        if (dashboardToolbar != null) {
            // TODO: Figure out how to do this in the XML file?
            dashboardToolbar.setTitle(R.string.appbarTitle_dashboard);
            setSupportActionBar(dashboardToolbar);
        }

        // Set up the user's widget grid
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
            case R.id.action_edit:
                // Enable drag and drop for widgets
                if (_editMode) {
                    item.setIcon(R.drawable.ic_create_black_48dp);
                    disableEditMode();
                    _editMode = false;
                }
                else {
                    item.setIcon(R.drawable.ic_create_white_48dp);
                    toggleEditMode();
                    _editMode = true;
                }
                return true;

            case R.id.action_new:
                // Create a new widget on the dashboard
                if (!_editMode)
                    createNewWidget();
                return true;

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

    public void createNewWidget () {
        final ImageButton imageButton = new ImageButton(this);

        // Sets the image resource to be used for our ImageView
        imageButton.setImageResource(R.drawable.ic_settings_black_48dp);

        // Set our on-click function
        imageButton.setOnClickListener(new View.OnClickListener() {
            final String function = "led";
            final List<String> commands = Arrays.asList("1");

            public void onClick(View v) {
                Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Integer>() {
                    @Override
                    public Integer callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                        try {
                            ParticleDevice particleDevice = ParticleCloudSDK.getCloud().getDevices().get(1);
                            return particleDevice.callFunction(function, commands);
                        } catch (ParticleDevice.FunctionDoesNotExistException e) {
                            e.printStackTrace();
                        }
                        return 1;
                    }

                    @Override
                    public void onSuccess (Integer i) {
                    }

                    @Override
                    public void onFailure(ParticleCloudException exception) {
                    }
                });
            }
        });

        // Add our widget to our view
        _widgetList.add(imageButton);
        _dashboardLayout.addView(imageButton, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void toggleEditMode () {
        // Disable touch listener on each widget and set drag and drop listener
        for (ImageButton imageButton: _widgetList) {
            imageButton.setOnClickListener(null);

            imageButton.setOnTouchListener(new View.OnTouchListener() {
                float deltaX;
                float deltaY;

                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            deltaX = view.getX() - event.getRawX();
                            deltaY = view.getY() - event.getRawY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            view.animate()
                                    .x(event.getRawX() + deltaX)
                                    .y(event.getRawY() + deltaY)
                                    .setDuration(0)
                                    .start();
                            break;
                    }
                    return true;
                }
            });
        }
    }

    private void disableEditMode () {
        // Disable drag and drop and re-enable click listener
        for (ImageButton imageButton: _widgetList) {
            imageButton.setOnTouchListener(null);

            imageButton.setOnClickListener(new View.OnClickListener() {
                final String function = "led";
                final List<String> commands = Arrays.asList("1");

                public void onClick(View v) {
                    Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Integer>() {
                        @Override
                        public Integer callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                            try {
                                ParticleDevice particleDevice = ParticleCloudSDK.getCloud().getDevices().get(1);
                                return particleDevice.callFunction(function, commands);
                            } catch (ParticleDevice.FunctionDoesNotExistException e) {
                                e.printStackTrace();
                            }
                            return 1;
                        }

                        @Override
                        public void onSuccess (Integer i) {
                        }

                        @Override
                        public void onFailure(ParticleCloudException exception) {
                        }
                    });
                }
            });
        }
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
