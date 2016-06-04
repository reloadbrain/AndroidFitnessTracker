package com.mbientlab.activitytracker;

import android.app.Fragment;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Base64;
import android.util.Log;

import com.mbientlab.activitytracker.model.ActivitySampleContract;
import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Bmi160Accelerometer;
import com.mbientlab.metawear.module.DataProcessor;
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.module.Mma8452qAccelerometer;
import com.mbientlab.metawear.module.Timer;
import com.mbientlab.metawear.processor.Accumulator;
import com.mbientlab.metawear.processor.Rms;
import com.mbientlab.metawear.processor.Time;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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
public class AccelerometerFragment extends Fragment {
    private Accelerometer accelModule;
    private Logging loggingModule;
    private Editor editor;
    private MetaWearBoard mwBoard;
    private SQLiteDatabase activitySampleDb;
    private AccelerometerCallback accelerometerCallback;
    private final int TIME_DELAY_PERIOD = 10000;

    public interface AccelerometerCallback {
        public void startDownload();

        public void totalDownloadEntries(int entries);

        public void downloadProgress(int entriesDownloaded);

        public void downloadFinished();

        public GraphFragment getGraphFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        accelerometerCallback = (AccelerometerCallback) getActivity();
    }

    private String getDateTime(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(date);
    }

    private final RouteManager.MessageHandler mma8452LoggingMessageHandler = new RouteManager.MessageHandler() {
        @Override
        public void process(Message msg) {
            java.sql.Date date = new java.sql.Date(msg.getTimestamp().getTimeInMillis());

            int activityMilliG = msg.getData(Integer.class).intValue();

            Log.i("LoggingExample", "Time Trigger Id " + date.toString() + String.valueOf(activityMilliG));//String.format(outputFormat, "Z-Axis", entryTime, Gs));
            Log.i("ActivityTracker", String.format(Locale.US, "%.3f",
                    activityMilliG / 1000.0));
            ContentValues contentValues = new ContentValues();
            contentValues.put(ActivitySampleContract.ActivitySampleEntry.COLUMN_NAME_MILLIG, activityMilliG);
            contentValues.put(ActivitySampleContract.ActivitySampleEntry.COLUMN_NAME_SAMPLE_TIME, getDateTime(date));
            activitySampleDb.insert(ActivitySampleContract.ActivitySampleEntry.TABLE_NAME, null, contentValues);
        }
    };

    private final AsyncOperation.CompletionHandler<RouteManager> mma8452AcceleromterHandler = new AsyncOperation.CompletionHandler<RouteManager>() {
        @Override
        public void success(RouteManager result) {
            result.setLogMessageHandler("mystream", mma8452LoggingMessageHandler);
            editor.putInt(mwBoard.getMacAddress() + "_log_id", result.id());
            editor.apply();
            Mma8452qAccelerometer mma8452qAccelerometer = (Mma8452qAccelerometer) accelModule;
            mma8452qAccelerometer.configureAxisSampling().setFullScaleRange(Mma8452qAccelerometer.FullScaleRange.FSR_8G)
                .enableHighPassFilter((byte) 0).commit();
            accelModule.enableAxisSampling();
            accelModule.start();
        }
    };

    private final RouteManager.MessageHandler bmi160LoggingMessageHandler = new RouteManager.MessageHandler() {
        @Override
        public void process(Message msg) {
            java.sql.Date date = new java.sql.Date(msg.getTimestamp().getTimeInMillis());

            int steps = msg.getData(Integer.class).intValue();

            Log.i("LoggingExample", "Time Trigger Id " + date.toString() + String.valueOf(steps));//String.format(outputFormat, "Z-Axis", entryTime, Gs));
            ContentValues contentValues = new ContentValues();
            contentValues.put(ActivitySampleContract.ActivitySampleEntry.COLUMN_NAME_STEPS, steps);
            contentValues.put(ActivitySampleContract.ActivitySampleEntry.COLUMN_NAME_SAMPLE_TIME, getDateTime(date));
            activitySampleDb.insert(ActivitySampleContract.ActivitySampleEntry.TABLE_NAME, null, contentValues);
        }
    };

    private final AsyncOperation.CompletionHandler<RouteManager> bmi160AccelerometerHandler = new AsyncOperation.CompletionHandler<RouteManager>(){
        @Override
        public void success(RouteManager result){
            result.setLogMessageHandler("mystream", bmi160LoggingMessageHandler);
            editor.putInt(mwBoard.getMacAddress() + "_log_id", result.id());
            editor.apply();
            final Bmi160Accelerometer bmi160Accelerometer = (Bmi160Accelerometer) accelModule;

            bmi160Accelerometer.resetStepCounter();
            bmi160Accelerometer.configureStepDetection()
                    .setSensitivity(Bmi160Accelerometer.StepSensitivity.NORMAL)
                    .enableStepCounter()
                    .commit();
            bmi160Accelerometer.start();

            try{
                AsyncOperation<Timer.Controller> taskResult = mwBoard.getModule(Timer.class)
                        .scheduleTask(new Timer.Task(){
                            @Override
                            public void commands(){
                                bmi160Accelerometer.readStepCounter(false);
                            }
                        }, TIME_DELAY_PERIOD, false);
                taskResult.onComplete(new AsyncOperation.CompletionHandler<Timer.Controller>(){
                    @Override
                    public void success(Timer.Controller result){
                        result.start();
                    }
                });
            } catch (UnsupportedModuleException e){
                Log.e("Temperature Fragment", e.toString());
            }

        }
    };

    public boolean setupAccelerometerAndLogs(MetaWearBoard mwBoard, Editor editor) {
        this.editor = editor;
        this.mwBoard = mwBoard;

        try {
            accelModule = mwBoard.getModule(Accelerometer.class);
        } catch (UnsupportedModuleException e) {
            Log.e("Thermistor Fragment", e.toString());
        }

        if(accelModule instanceof Mma8452qAccelerometer) {
            accelModule.setOutputDataRate(100.f);
            Mma8452qAccelerometer mma8452qAccelerometer = (Mma8452qAccelerometer) accelModule;
            mma8452qAccelerometer.routeData().fromAxes().process(new Rms())
                    .process(new Accumulator((byte) 4))
                    .process(new Time(Time.OutputMode.ABSOLUTE, TIME_DELAY_PERIOD))
                    .log("log_stream")
                    .commit().onComplete(mma8452AcceleromterHandler);
        }else{
            Bmi160Accelerometer bmi160Accelerometer = (Bmi160Accelerometer) accelModule;
            bmi160Accelerometer.routeData().fromStepCounter(false).log("log_stream").commit()
                    .onComplete(bmi160AccelerometerHandler);
        }

        try {
            loggingModule = mwBoard.getModule(Logging.class);
            loggingModule.startLogging();
        } catch (UnsupportedModuleException e) {
            Log.e("Thermistor Fragment", e.toString());
            return false;
        }
        return true;
    }

    public void startLogDownload(MetaWearBoard mwBoard, SharedPreferences sharedPreferences, SQLiteDatabase activitySampleDb) {
        /*
           Before actually calling the downloadLog method, we will first gather the required
           data to compute the log timestamps and setup progress notifications.
           This means we will call downloadLog in one of the logging callback functions, and
           will start the callback chain here
         */
        this.activitySampleDb = activitySampleDb;

        this.mwBoard = mwBoard;

        try {
            loggingModule = mwBoard.getModule(Logging.class);
            loggingModule.startLogging();
            accelModule = mwBoard.getModule(Accelerometer.class);
        } catch (UnsupportedModuleException e) {
            Log.e("Thermistor Fragment", e.toString());
        }

        RouteManager route = mwBoard.getRouteManager(sharedPreferences.getInt(mwBoard.getMacAddress() + "_log_id", 0));
        if(accelModule instanceof Mma8452qAccelerometer) {
            route.setLogMessageHandler("log_stream", mma8452LoggingMessageHandler);
        }else{
            route.setLogMessageHandler("log_stream", bmi160LoggingMessageHandler);
        }

        loggingModule.downloadLog((float) 0.1, new Logging.DownloadHandler() {
            @Override
            public void onProgressUpdate(int nEntriesLeft, int totalEntries) {
                Log.i("Thermistor", String.format("Progress= %d / %d", nEntriesLeft,
                        totalEntries));
                accelerometerCallback.totalDownloadEntries(totalEntries);
                accelerometerCallback.downloadProgress(totalEntries - nEntriesLeft);
                if (nEntriesLeft == 0) {
                    getActivity()
                            .runOnUiThread(new Runnable() {
                                               @Override
                                               public void run() {
                                                   GraphFragment graphFragment = accelerometerCallback.getGraphFragment();
                                                   graphFragment.updateGraph();
                                                   accelerometerCallback.downloadFinished();
                                               }
                                           }

                            );

                }
            }
        });
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                accelerometerCallback.startDownload();
            }
        });
    }

}