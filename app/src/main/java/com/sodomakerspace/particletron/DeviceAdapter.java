package com.sodomakerspace.particletron;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;

public class DeviceAdapter<String> extends ArrayAdapter<String> {
    private String[] dataset;

    public DeviceAdapter(Context context, int resource, Object[] objects) {
        super(context, resource, (String[]) objects);
        dataset = (String[]) objects;
    }

    @Override
    public Button getView(final int position, View convertView, ViewGroup parent) {
        final Button myButton = new Button(this.getContext());
        myButton.setText(dataset[position].toString());
        myButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                myButton.setText("foo");
            }
        });
        return myButton;
    }
}
