package com.mbientlab.activitytracker;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Highlight;
import com.mbientlab.activitytracker.MWDeviceConfirmFragment.DeviceConfirmCallback;
import com.mbientlab.activitytracker.db.ActivitySampleDbHelper;
import com.mbientlab.activitytracker.model.ActivitySampleContract;
import com.mbientlab.bletoolbox.scanner.BleScannerFragment;
import com.mbientlab.activitytracker.GraphFragment.GraphCallback;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Debug;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Copyright 2014 MbientLab Inc. All rights reserved.
 * <p/>
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 * <p/>
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 * <p/>
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 * <p/>
 * <p/>
 * Created by Lance Gleason of Polyglot Programming LLC. on 4/26/15.
 * http://www.polyglotprogramminginc.com
 * https://github.com/lgleasain
 * Twitter: @lgleasain
 */

public class MainActivity extends ActionBarActivity implements BleScannerFragment.ScannerCommunicationBus,
        ServiceConnection, DeviceConfirmCallback, GraphCallback, AccelerometerFragment.AccelerometerCallback {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private GraphFragment mGraphFragment;
    private final static int REQUEST_ENABLE_BT = 0;
    private final static String ACCELEROMETER_FRAGMENT_KEY = "com.mbientlab.activitytracker.AccelerometerFragment.ACCELEROMETER_FRAGMENT_KEY";
    private final static String GRAPH_FRAGMENT_KEY = "com.mbientlab.activitytracker.GraphFragment.GRAPH_FRAGMENT_KEY";
    private MetaWearBleService.LocalBinder metaWearBinder = null;
    private MetaWearBoard metaWearBoard = null;
    private MWScannerFragment mwScannerFragment = null;
    private AccelerometerFragment accelerometerFragment = null;
    private SharedPreferences sharedPreferences;
    private Editor editor;
    private BluetoothDevice bluetoothDevice;
    private BluetoothAdapter btAdapter;
    private Menu menu;
    private SQLiteDatabase activitySampleDb;
    private boolean btDeviceSelected = false;
    private boolean btDeviceReset = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getApplicationContext().getSharedPreferences("com.mbientlab.metatracker", 0); // 0 - for private mode
        editor = sharedPreferences.edit();
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            PlaceholderFragment mainFragment = new PlaceholderFragment();
            accelerometerFragment = new AccelerometerFragment();
            getFragmentManager().beginTransaction().add(R.id.container, mainFragment)
                    .add(R.id.container, accelerometerFragment, ACCELEROMETER_FRAGMENT_KEY).commit();
            mGraphFragment = (GraphFragment)
                    getFragmentManager().findFragmentById(R.id.graph);
        } else {
            accelerometerFragment = (AccelerometerFragment) getFragmentManager().getFragment(savedInstanceState, ACCELEROMETER_FRAGMENT_KEY);
            mGraphFragment = (GraphFragment) getFragmentManager().getFragment(savedInstanceState, GRAPH_FRAGMENT_KEY);
        }

        btAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        if (btAdapter == null) {
            new AlertDialog.Builder(this).setTitle(R.string.error_title)
                    .setMessage(R.string.error_no_bluetooth)
                    .setCancelable(false)
                    .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            MainActivity.this.finish();
                        }
                    })
                    .create()
                    .show();
        } else if (!btAdapter.isEnabled()) {
            final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class),
                this, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();

        String bleMacAddress = sharedPreferences.getString("ble_mac_address", null);
        if (bleMacAddress != null) {
            TextView connectionStatus = (TextView) findViewById(R.id.connection_status);
            connectionStatus.setText(getText(R.string.toast_disconnected));
        }else {
            TextView connectionStatus = (TextView) findViewById(R.id.connection_status);
            connectionStatus.setText(getText(R.string.no_metawear_connected));
        }

        if (bleMacAddress != null && metaWearBinder != null) {
            bluetoothDevice = btAdapter.getRemoteDevice(bleMacAddress);
            connectDevice(bluetoothDevice);
            metaWearBoard.connect();
        }

        accelerometerFragment = (AccelerometerFragment) getFragmentManager().findFragmentByTag(ACCELEROMETER_FRAGMENT_KEY);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupDatabase();
    }

    @Override
    protected void onStop() {
        super.onStop();
        activitySampleDb.close();
    }

    private MetaWearBoard.ConnectionStateHandler connectionStateHandler = new MetaWearBoard.ConnectionStateHandler() {
        @Override
        public void connected() {
            Log.i("Metawear Controller", "Device Connected");
            runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  Toast.makeText(getApplicationContext(), R.string.toast_connected, Toast.LENGTH_SHORT).show();
                                  TextView connectionStatus = (TextView) findViewById(R.id.connection_status);
                                  connectionStatus.setText(getText(R.string.metawear_connected));
                              }
                          }
            );

            if (btDeviceSelected) {
                MWDeviceConfirmFragment mwDeviceConfirmFragment = new MWDeviceConfirmFragment();
                mwDeviceConfirmFragment.flashDeviceLight(metaWearBoard, getFragmentManager());
                btDeviceSelected = false;
            } else {
                accelerometerFragment.startLogDownload(metaWearBoard, sharedPreferences, activitySampleDb);
            }
        }

        @Override
        public void disconnected() {
            Log.i("Metawear Controler", "Device Disconnected");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.toast_disconnected, Toast.LENGTH_SHORT).show();
                    TextView connectionStatus = (TextView) findViewById(R.id.connection_status);
                    if(btDeviceReset){
                        connectionStatus.setText(getText(R.string.no_metawear_connected));
                    }else {
                        connectionStatus.setText(getText(R.string.toast_disconnected));
                    }
                }
            });
        }
    };

    private void connectDevice(BluetoothDevice device) {
        metaWearBoard = metaWearBinder.getMetaWearBoard(device);
        metaWearBoard.setConnectionStateHandler(connectionStateHandler);

        String bleMacAddress = sharedPreferences.getString("ble_mac_address", null);
        if (bleMacAddress != null) {
            // probably need to move this
            String boardState = sharedPreferences.getString(bleMacAddress, null);
            if (boardState != null) {
                metaWearBoard.deserializeState(boardState.getBytes());
                Log.i("connect device", "Found instance state");
            }
        }
        metaWearBoard.connect();

        if (menu != null) {
            MenuItem connectMenuItem = menu.findItem(R.id.action_connect);
            connectMenuItem.setTitle(R.string.disconnect);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Only show items in the action bar relevant to this screen
        // if the drawer is not showing. Otherwise, let the drawer
        // decide what to show in the action bar.
        getMenuInflater().inflate(R.menu.main, menu);
        this.menu = menu;
        String bleMacAddress = sharedPreferences.getString("ble_mac_address", null);
        if (bleMacAddress != null) {
            MenuItem connectMenuItem = menu.findItem(R.id.action_connect);
            connectMenuItem.setTitle(R.string.disconnect);
        }
        return true;
    }

    public void pairDevice() {
        editor.putString("ble_mac_address", bluetoothDevice.getAddress());
        editor.commit();
        accelerometerFragment.setupAccelerometerAndLogs(metaWearBoard, editor);
    }

    public void dontPairDevice() {
        metaWearBoard.disconnect();
        bluetoothDevice = null;
        mwScannerFragment.show(getFragmentManager(), "metawear_scanner_fragment");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getApplicationContext().unbindService(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (metaWearBoard != null) {
            metaWearBoard.disconnect();
            editor.putString("ble_mac_address", metaWearBoard.getMacAddress());
            state.putByteArray(metaWearBoard.getMacAddress(), metaWearBoard.serializeState());
            editor.putString(metaWearBoard.getMacAddress(), new String(metaWearBoard.serializeState()));
            editor.apply();
            editor.commit();
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        ///< Get a reference to the MetaWear service from the binder
        metaWearBinder = (MetaWearBleService.LocalBinder) service;
        String bleMacAddress = sharedPreferences.getString("ble_mac_address", null);

        if (bleMacAddress != null) {
            bluetoothDevice = btAdapter.getRemoteDevice(bleMacAddress);
            connectDevice(bluetoothDevice);
        }
        Log.i("Service Connected", "Stored mac address is " + bleMacAddress);
    }

    @Override
    public GraphFragment getGraphFragment() {
        return mGraphFragment;
    }

    @Override
    public void setGraphFragment(GraphFragment graphFragment) {
        mGraphFragment = graphFragment;
    }

    @Override
    public void updateCaloriesAndSteps(int calories, int steps) {
        TextView activeCaloriesBurned = (TextView) findViewById(R.id.active_calories_burned);
        activeCaloriesBurned.setText(getString(R.string.active_calories_burned) + String.valueOf(calories));
    }

    @Override
    public void startDownload() {
        TextView connectionStatus = (TextView) findViewById(R.id.connection_status);
        connectionStatus.setText(getText(R.string.metawear_syncing));
    }

    @Override
    public void totalDownloadEntries(int entries) {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.download_progress);
        progressBar.setMax(entries);
    }

    @Override
    public void downloadProgress(int entriesDownloaded) {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.download_progress);
        progressBar.setProgress(entriesDownloaded);
    }

    @Override
    public void downloadFinished() {
        TextView connectionStatus = (TextView) findViewById(R.id.connection_status);
        connectionStatus.setText(getText(R.string.metawear_connected));
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.download_progress);
        progressBar.setProgress(0);
    }

    ///< Don't need this callback method but we must implement it
    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_connect:
                if ((metaWearBoard != null) && metaWearBoard.isConnected()) {
                    MenuItem connectMenuItem = menu.findItem(R.id.action_connect);
                    connectMenuItem.setTitle(R.string.connect);
                    try {
                    if (metaWearBoard != null && metaWearBoard.getModule(Debug.class) != null)
                        metaWearBoard.getModule(Debug.class).resetDevice();
                    else
                        Toast.makeText(getApplicationContext(), R.string.toast_please_connect, Toast.LENGTH_SHORT).show();

                    } catch (UnsupportedModuleException e) {
                        Toast.makeText(this, R.string.error_soft_reset, Toast.LENGTH_SHORT).show();
                    }

                    editor.remove("ble_mac_address");
                    editor.commit();
                    metaWearBoard.disconnect();
                    btDeviceReset = true;
                } else {
                    if (mwScannerFragment == null) {
                        mwScannerFragment = new MWScannerFragment();
                        mwScannerFragment.setCancelable(false);
                        mwScannerFragment.show(getFragmentManager(), "metawear_scanner_fragment");
                    } else {
                        mwScannerFragment.show(getFragmentManager(), "metawear_scanner_fragment");
                    }
                }
                break;
            case R.id.action_clear_log:
                activitySampleDb.execSQL("delete from " + ActivitySampleContract.ActivitySampleEntry.TABLE_NAME);
                mGraphFragment.updateGraph();
                break;
            case R.id.action_refresh:
                if ((metaWearBoard != null) && !metaWearBoard.isConnected()) {
                    String bleMacAddress = sharedPreferences.getString("ble_mac_address", null);
                    if (bleMacAddress != null) {
                        bluetoothDevice = btAdapter.getRemoteDevice(bleMacAddress);
                        connectDevice(bluetoothDevice);
                        metaWearBoard.connect();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.toast_please_connect, Toast.LENGTH_SHORT).show();
                    }
                } else if (metaWearBoard == null) {
                    Toast.makeText(getApplicationContext(), R.string.toast_please_connect, Toast.LENGTH_SHORT).show();
                } else {
                    accelerometerFragment.startLogDownload(metaWearBoard, sharedPreferences, activitySampleDb);
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupDatabase() {
        ActivitySampleDbHelper activitySampleDbHelper = new ActivitySampleDbHelper(this);
        activitySampleDb = activitySampleDbHelper.getWritableDatabase();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements ServiceConnection, OnChartValueSelectedListener {


        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //mwService= null;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);


            Switch demoSwitch = (Switch) rootView.findViewById(R.id.demo);
            demoSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton b, boolean isChecked) {

                    GraphFragment graphFragment = (GraphFragment) getChildFragmentManager().findFragmentById(R.id.graph);
                    if (graphFragment == null) {
                        graphFragment = (GraphFragment) getFragmentManager().findFragmentById(R.id.graph);
                    }
                    graphFragment.toggleDemoData(isChecked);
                }
            });
            GraphFragment graphFragment = getGraphFragment();
            graphFragment.getmChart().setOnChartValueSelectedListener(this);
            return rootView;
        }


        @Override
        public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
            GraphFragment graphFragment = getGraphFragment();
            int steps = Float.valueOf(e.getVal()).intValue();
            TextView readingTime = (TextView) getActivity().findViewById(R.id.reading_time);
            String formattedDate;
            try {
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                Date date = dateFormat.parse(graphFragment.getActivitySample(e.getXIndex()).getDate());
                DateFormat outputDateFormat = new SimpleDateFormat("MMM dd, yyyy   HH:mm");
                formattedDate = outputDateFormat.format(date);
            } catch (ParseException pe) {
                Log.i("MainActivity", "Date Parse Exception OnValueSelected " + pe.toString());
                formattedDate = "";
            }

            readingTime.setText(formattedDate);
            TextView stepsView = (TextView) getActivity().findViewById(R.id.steps);
            stepsView.setText(String.valueOf(steps) + " " + getString(R.string.steps));
        }

        @Override
        public void onNothingSelected() {

        }

        private GraphFragment getGraphFragment() {
            GraphFragment graphFragment = (GraphFragment) getChildFragmentManager().findFragmentById(R.id.graph);
            if (graphFragment == null) {
                graphFragment = (GraphFragment) getFragmentManager().findFragmentById(R.id.graph);
            }
            return graphFragment;
        }

    }


    /**
     * callbacks for Bluetooth device scan
     */
    @Override
    public void onDeviceSelected(BluetoothDevice device) {
        bluetoothDevice = device;
        btDeviceSelected = true;
        connectDevice(device);
        Fragment metawearBlescannerPopup = getFragmentManager().findFragmentById(R.id.metawear_blescanner_popup_fragment);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.remove(metawearBlescannerPopup);
        fragmentTransaction.commit();
        mwScannerFragment.dismiss();
    }

    @Override
    public UUID[] getFilterServiceUuids() {
        ///< Only return MetaWear boards in the scan
        return new UUID[]{UUID.fromString("326a9000-85cb-9195-d9dd-464cfbbae75a")};
    }

    @Override
    public long getScanDuration() {
        ///< Scan for 10000ms (10 seconds)
        return 10000;
    }

}
