package com.mbientlab.activitytracker;

import android.app.Activity;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Locale;

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
public class MWScannerFragment extends DialogFragment {
    public interface ScannerCallback {
        public void btDeviceSelected(BluetoothDevice device);
    }

    private final static int RSSI_BAR_LEVELS= 5;
    private final static int RSSI_BAR_SCALE= 100 / RSSI_BAR_LEVELS;
    private final static long SCAN_PERIOD= 10000;

    private BluetoothAdapter mBluetoothAdapter= null;
    private BLEDeviceListAdapter mLeDeviceListAdapter= null;
    private boolean isScanning;
    private Handler mHandler;

    private final BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            BtDeviceRssi temp= new BtDeviceRssi();
                            temp.device= device;
                            temp.rssi= rssi;

                            if (mLeDeviceListAdapter.getPosition(temp) == -1) {
                                mLeDeviceListAdapter.add(temp);
                                mLeDeviceListAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            };

    public class BtDeviceRssi {
        public BluetoothDevice device;
        public int rssi;

        @Override
        public boolean equals(Object obj) {
            return (obj == this) ||
                    ((obj instanceof BtDeviceRssi) && device.equals(((BtDeviceRssi) obj).device));
        }
    }
    public class BLEDeviceListAdapter extends ArrayAdapter<BtDeviceRssi> {
        private final LayoutInflater mInflator;

        public BLEDeviceListAdapter(Context context, int resource, LayoutInflater inflator) {
            super(context, resource);
            this.mInflator= inflator;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView= mInflator.inflate(R.layout.metawear_ble_info, null);
                viewHolder= new ViewHolder();
                viewHolder.deviceAddress= (TextView) convertView.findViewById(R.id.device_address);
                viewHolder.deviceName= (TextView) convertView.findViewById(R.id.device_name);
                viewHolder.deviceRSSI= (TextView) convertView.findViewById(R.id.rssi);
                viewHolder.rssiChart= (ImageView) convertView.findViewById(R.id.imageView1);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            BtDeviceRssi info= (BtDeviceRssi)getItem(position);
            final String deviceName= info.device.getName();

            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("Unknown Device");
            viewHolder.deviceAddress.setText(info.device.getAddress());
            viewHolder.deviceRSSI.setText(String.format(Locale.US, "%d dBm", info.rssi));
            viewHolder.rssiChart.setImageLevel(Math.min(RSSI_BAR_LEVELS - 1, (127 + info.rssi + 5) / RSSI_BAR_SCALE));
            return convertView;
        }

        private class ViewHolder {
            public TextView deviceAddress;
            public TextView deviceName;
            public TextView deviceRSSI;
            public ImageView rssiChart;
        }

    }

    private Button scanControl;
    private ScannerCallback callback;

    @Override
    public void onAttach(Activity activity) {
        if (!(activity instanceof ScannerCallback)) {
            throw new RuntimeException("Acitivty does not implement ScannerCallback interface");
        }

        callback= (ScannerCallback) activity;
        super.onAttach(activity);
    }
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mLeDeviceListAdapter= new BLEDeviceListAdapter(getActivity(), R.id.mw_ble_info_layout, inflater);
        mHandler= new Handler();
        return inflater.inflate(R.layout.metawear_device_selection, container);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ListView scannedDevices= (ListView) view.findViewById(R.id.scanned_devices);
        scannedDevices.setAdapter(mLeDeviceListAdapter);
        scannedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                callback.btDeviceSelected(mLeDeviceListAdapter.getItem(position).device);
                dismiss();
            }
        });

        scanControl= (Button) view.findViewById(R.id.scan_control);
        scanControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isScanning) {
                    stopBleScan();
                } else {
                    startBleScan();
                }
            }
        });

        //Main activity has already checked a bluetooth manager exists
        mBluetoothAdapter= ((BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        startBleScan();
    }

    @Override
    public void onDestroyView() {
        stopBleScan();
        super.onDestroyView();
    }

    private void startBleScan() {
        if (!isScanning) {
            mLeDeviceListAdapter.clear();
            isScanning= true;
            scanControl.setText(R.string.label_stop);

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopBleScan();
                }
            }, SCAN_PERIOD);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
    }

    private void stopBleScan() {
        if (isScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);

            isScanning= false;
            scanControl.setText(R.string.label_scan);
        }
    }
}
