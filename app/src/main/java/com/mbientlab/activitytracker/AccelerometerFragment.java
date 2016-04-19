package com.mbientlab.activitytracker;

import android.app.Fragment;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;
import android.util.Log;

import com.mbientlab.activitytracker.model.ActivitySampleContract;
import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.DataProcessor;
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.module.Mma8452qAccelerometer;
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
    private DataProcessor dataProcessorController;
    private Editor editor;
    private MetaWearBoard mwBoard;
    private int totalEntryCount;
    private SQLiteDatabase activitySampleDb;
    private AccelerometerCallback accelerometerCallback;
    private final byte ACTIVITY_DATA_SIZE = 4;
    private final int TIME_DELAY_PERIOD = 60000;

    private byte rmsFilterId = -1, accumFilterId = -1, timeFilterId = -1, timeTriggerId = -1;

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

    /*    private final DataProcessor.Callbacks dpCallbacks = new DataProcessor.Callbacks() {
            @Override
            public void receivedFilterId(byte filterId) {
                byte filterArray[] = {filterId};
                if (rmsFilterId == -1) {
                    rmsFilterId = filterId;
                    editor.putString("rmsFilterId", Base64.encodeToString(filterArray, Base64.NO_WRAP));
                    editor.commit();
                    FilterConfig accumFilter = new FilterConfigBuilder.AccumulatorBuilder()
                            .withInputSize(LoggingTrigger.ACCELEROMETER_X_AXIS.length())
                            .withOutputSize(ACTIVITY_DATA_SIZE)
                            .build();

                    dataProcessorController.chainFilters(rmsFilterId, ACTIVITY_DATA_SIZE, accumFilter);
                } else if (accumFilterId == -1) {
                    accumFilterId = filterId;
                    editor.putString("accumFilterId", Base64.encodeToString(filterArray, Base64.NO_WRAP));
                    editor.commit();
                    FilterConfig timeFilter = new FilterConfigBuilder.TimeDelayBuilder()
                            .withFilterMode(FilterConfigBuilder.TimeDelayBuilder.FilterMode.ABSOLUTE)
                            .withPeriod(TIME_DELAY_PERIOD)
                            .withDataSize(ACTIVITY_DATA_SIZE)
                            .build();
                    dataProcessorController.chainFilters(accumFilterId, ACTIVITY_DATA_SIZE, timeFilter);
                } else {
                    if (timeFilterId == -1) {
                        timeFilterId = filterId;
                        editor.putString("timeFilterId", Base64.encodeToString(filterArray, Base64.NO_WRAP));
                        editor.commit();
                        loggingModule.addTrigger(TriggerBuilder.buildDataFilterTrigger(timeFilterId, ACTIVITY_DATA_SIZE));
                    }
                    mwBoard.removeModuleCallback(this);
                }

            }

        };
    */
    private String getDateTime(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(date);
    }
/*
    private final Logging.Callbacks logCallbacks = new Logging.Callbacks() {
        private final float notifyRatio = 0.01f;
        private boolean isDownloading;
        private ReferenceTick refTick;
        private LogEntry firstEntry = null;

        @Override
        public void receivedLogEntry(final LogEntry entry) {
            if (firstEntry == null) {
                firstEntry = entry;
            }

            int activityMilliG = ByteBuffer.wrap(entry.data())
                    .order(ByteOrder.LITTLE_ENDIAN).getInt();


            byte tId = entry.triggerId();
            Date entryTime = entry.timestamp(refTick).getTime();

            if (tId == timeTriggerId) {
                Log.i("LoggingExample", "Time Trigger Id " + entryTime.toString() + String.valueOf(activityMilliG));//String.format(outputFormat, "Z-Axis", entryTime, Gs));
                Log.i("ActivityTracker", String.format(Locale.US, "%.3f,%.3f",
                        entry.offset(firstEntry) / 1000.0, activityMilliG / 1000.0));
                ContentValues contentValues = new ContentValues();
                contentValues.put(ActivitySampleContract.ActivitySampleEntry.COLUMN_NAME_MILLIG, activityMilliG);
                contentValues.put(ActivitySampleContract.ActivitySampleEntry.COLUMN_NAME_SAMPLE_TIME, getDateTime(entryTime));
                activitySampleDb.insert(ActivitySampleContract.ActivitySampleEntry.TABLE_NAME, null, contentValues);
            } else {
                Log.i("LoggingExample", String.format("Unkown Trigger ID, (%d, %s)",
                        tId, Arrays.toString(entry.data())));
            }
        }


        @Override
        public void receivedReferenceTick(ReferenceTick reference) {
            refTick = reference;

            Log.i("LoggingExample", String.format("Received the reference tick = %s, %d", reference, reference.tickCount()));
            // Got the reference tick, make lets get
            // the log entry count
            loggingModule.readTotalEntryCount();
        }

        @Override
        public void receivedTriggerId(byte triggerId) {
            byte triggerArray[] = {triggerId};
            timeTriggerId = triggerId;
            editor.putString("timeTriggerId", Base64.encodeToString(triggerArray, Base64.NO_WRAP));
            editor.commit();
            startLog();
            Log.i("receivedTrigger", "Received trigger id " + String.valueOf(triggerId));
            Log.i("encoded trigger", Base64.encodeToString(triggerArray, Base64.NO_WRAP));
            Log.i("decoded trigger", String.valueOf(Base64.decode(Base64.encodeToString(triggerArray, Base64.NO_WRAP), Base64.NO_WRAP)[0]));
        }

        @Override
        public void receivedTotalEntryCount(int totalEntries) {
            if (!isDownloading && (totalEntries > 0)) {
                totalEntryCount = totalEntries;
                isDownloading = true;
                Log.i("LoggingExample", "Download begin");

                //Got the entry count, lets now download the log
                loggingModule.downloadLog(totalEntries, (int) (totalEntries * notifyRatio));
                accelerometerCallback.totalDownloadEntries(totalEntries);
            } else {
                accelerometerCallback.downloadFinished();
                Log.i("LoggingExample", "Total Entries count " + String.valueOf(totalEntries));
            }
        }

        @Override
        public void receivedDownloadProgress(int nEntriesLeft) {
            Log.i("LoggingExample", String.format("Entries remaining= %d", nEntriesLeft));
            accelerometerCallback.downloadProgress(totalEntryCount - nEntriesLeft);
        }

        @Override
        public void downloadCompleted() {
            isDownloading = false;
            Log.i("removing ", String.valueOf((short) totalEntryCount) + " entries");
            loggingModule.removeLogEntries((short) totalEntryCount);
            Log.i("LoggingExample", "Download completed");
            mwBoard.waitToClose(false);
            GraphFragment graphFragment = accelerometerCallback.getGraphFragment();
            graphFragment.updateGraph();
            accelerometerCallback.downloadFinished();
        }
    };

    public void restoreState(SharedPreferences sharedPreferences) {
        String rmsFilterString = sharedPreferences.getString("rmsFilterId", null);
        String accumFilterString = sharedPreferences.getString("accumFilterId", null);
        String timeFilterString = sharedPreferences.getString("timeFilterId", null);
        String timeTriggerString = sharedPreferences.getString("timeTriggerId", null);
        Log.i("Accelerometer", "Time Trigger Id is " + timeTriggerString);

        if ((rmsFilterString != null) && (timeFilterString != null) && (timeTriggerString != null)) {
            rmsFilterId = Base64.decode(rmsFilterString, Base64.NO_WRAP)[0];
            accumFilterId = Base64.decode(accumFilterString, Base64.NO_WRAP)[0];
            timeFilterId = Base64.decode(timeFilterString, Base64.NO_WRAP)[0];
            timeTriggerId = Base64.decode(timeTriggerString, Base64.NO_WRAP)[0];
        }
        Log.i("Accelerometer", "Time Trigger Id is " + String.valueOf(timeTriggerId));
    }

    public void removeTriggers(Editor editor) {
        loggingModule.removeTrigger(timeTriggerId);
        removePersistedTriggers(editor);
    }

    public void removePersistedTriggers(Editor editor) {
        editor.remove("rmsFilterId");
        editor.remove("accumFilterId");
        editor.remove("timeFilterId");
        editor.remove("timeTriggerId");
        editor.commit();
        // Reset the IDs to -1
        rmsFilterId = -1;
        accumFilterId = -1;
        timeFilterId = -1;
        timeTriggerId = -1;
    }

    public void addTriggers(MetaWearController mwController, Editor editor) {
        /*
         * The board will start logging once all triggers have been registered.  This is done
         * by having the receivedTriggerId callback fn start the logger when the ID for the
         * Z axis has been received
         */
/*        this.editor = editor;
        this.mwBoard = mwController;
        accelModule = (Accelerometer) mwController.getModuleController(Module.ACCELEROMETER);
        Trigger accelerometerTrigger = TriggerBuilder.buildAccelerometerTrigger();

        setupLogginController(mwController);

        FilterConfig rms = new FilterConfigBuilder.RMSBuilder().withInputCount((byte) 3)
                .withSignedInput().withOutputSize(LoggingTrigger.ACCELEROMETER_X_AXIS.length())
                .withInputSize(LoggingTrigger.ACCELEROMETER_X_AXIS.length())
                .build();

        dataProcessorController.addFilter(accelerometerTrigger, rms);

        final Accelerometer accelCtrllr = (Accelerometer) mwController.getModuleController(Module.ACCELEROMETER);
        accelCtrllr.enableXYZSampling().withFullScaleRange(SamplingConfig.FullScaleRange.FSR_8G)
                .withHighPassFilter((byte) 0).withOutputDataRate(SamplingConfig.OutputDataRate.ODR_100_HZ)
                .withSilentMode();
        accelCtrllr.startComponents();
    }

    private void startLog() {
        loggingModule.startLogging();

        SamplingConfig samplingConfig = accelModule.enableXYZSampling();
        samplingConfig.withFullScaleRange(SamplingConfig.FullScaleRange.FSR_8G)
                .withOutputDataRate(SamplingConfig.OutputDataRate.ODR_100_HZ)
                .withSilentMode();

        accelModule.startComponents();
    }

    public void stopLog(MetaWearController mwController) {
        setupLogginController(mwController);
        loggingModule.stopLogging();

        if (accelModule == null) {
            accelModule = (Accelerometer) mwController.getModuleController(Module.ACCELEROMETER);
        }

        accelModule.stopComponents();
    }

    private void setupLogginController(MetaWearController mwController) {
        if (loggingModule == null) {
            loggingModule = (Logging) mwController.getModuleController(Module.LOGGING);
            mwController.addModuleCallback(logCallbacks);
        }
        if (dataProcessorController == null) {
            dataProcessorController = (DataProcessor) mwController.getModuleController(Module.DATA_PROCESSOR);
        }
        mwController.addModuleCallback(dpCallbacks);
    }
*/

    private final RouteManager.MessageHandler loggingMessageHandler = new RouteManager.MessageHandler() {
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

    private final AsyncOperation.CompletionHandler<RouteManager> acceleromterHandler = new AsyncOperation.CompletionHandler<RouteManager>() {
        @Override
        public void success(RouteManager result) {
            result.setLogMessageHandler("mystream", loggingMessageHandler);
            editor.putInt(mwBoard.getMacAddress() + "_log_id", result.id());
            editor.apply();
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

        accelModule.setOutputDataRate(100.f);
        Mma8452qAccelerometer mma8452qAccelerometer = (Mma8452qAccelerometer) accelModule;
        mma8452qAccelerometer.configureAxisSampling().setFullScaleRange(Mma8452qAccelerometer.FullScaleRange.FSR_8G)
                .enableHighPassFilter((byte) 0).commit();
        mma8452qAccelerometer.routeData().fromAxes().process(new Rms())
                .process(new Accumulator((byte) 4))
                .process(new Time(Time.OutputMode.ABSOLUTE, TIME_DELAY_PERIOD))
                .log("log_stream")
                .commit().onComplete(acceleromterHandler);

        accelModule.start();

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
        route.setLogMessageHandler("log_stream", loggingMessageHandler);

        loggingModule.downloadLog((float) 0.1, new Logging.DownloadHandler() {
            @Override
            public void onProgressUpdate(int nEntriesLeft, int totalEntries) {
                Log.i("Thermistor", String.format("Progress= %d / %d", nEntriesLeft,
                        totalEntries));
                //mwController.waitToClose(false);
                accelerometerCallback.totalDownloadEntries(totalEntries);
                accelerometerCallback.downloadProgress(totalEntries - nEntriesLeft);
                if (nEntriesLeft == 0) {
                    GraphFragment graphFragment = accelerometerCallback.getGraphFragment();
                    graphFragment.updateGraph();
                    accelerometerCallback.downloadFinished();
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

/*    public void startLogDownload(MetaWearController mwController, SQLiteDatabase activitySampleDb) {
        /*
           Before actually calling the downloadLog method, we will first gather the required
           data to compute the log timestamps and setup progress notifications.
           This means we will call downloadLog in one of the logging callback functions, and
           will start the callback chain here
         */
/*        this.activitySampleDb = activitySampleDb;
        this.mwBoard = mwController;
        setupLogginController(mwController);
        Log.i("LoggingExample", String.format("Starting Log Download"));
        loggingModule.readReferenceTick();
        accelerometerCallback.startDownload();
    }*/
}