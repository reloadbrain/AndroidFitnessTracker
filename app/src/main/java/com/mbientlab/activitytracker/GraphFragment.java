package com.mbientlab.activitytracker;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.ChartData;
import com.mbientlab.activitytracker.db.ActivitySampleDbHelper;
import com.mbientlab.activitytracker.model.ActivitySample;
import com.mbientlab.activitytracker.model.ActivitySampleContract;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
public class GraphFragment extends Fragment {

    public static Fragment newInstance() {
        return new GraphFragment();
    }

    final int colors[] = {Color.parseColor("#FF9500"), Color.parseColor("#FF4B30")};

    private BarChart mChart;
    private int totalSteps = 66;
    private int totalCalories = 200;
    private boolean demo = false;
    private SQLiteDatabase activitySampleDb;
    private ActivitySample[] activitySamples = new ActivitySample[61];
    private GraphCallback callback;
    private boolean bmi160 = false;

    public interface GraphCallback {
        public void updateCaloriesAndSteps(int totalCalories, int totalSteps);
        public void setGraphFragment(GraphFragment graphFragment);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        for (int i = 0; i < activitySamples.length; i++) {
            activitySamples[i] = new ActivitySample();
        }

        View v = inflater.inflate(R.layout.fragment_graph, container, false);
        mChart = (BarChart) v.findViewById(R.id.gragh_layout);
        mChart.setDescription("");
        mChart.setDrawGridBackground(false);
        mChart.setDrawBarShadow(false);
        mChart.setDrawBorders(false);
        mChart.setMaxVisibleValueCount(1);
        mChart.setBackgroundColor(Color.BLACK);

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        if (!(activity instanceof GraphCallback)) {
            throw new RuntimeException("Acitivty does not implement DeviceConfirmationCallback interface");
        }
        callback = (GraphCallback) activity;
        super.onAttach(activity);
    }

    @Override
    public void onStart(){
        super.onStart();
        ActivitySampleDbHelper activitySampleDbHelper = new ActivitySampleDbHelper(getActivity());
        activitySampleDb = activitySampleDbHelper.getWritableDatabase();
    }

    @Override
    public void onStop(){
        super.onStop();
        activitySampleDb.close();
    }

    @Override
    public void onResume(){
        super.onResume();

        callback.setGraphFragment(this);
        updateGraph();
        Legend l = mChart.getLegend();
        l.setEnabled(false);

        YAxis leftAxis = mChart.getAxisLeft();

        mChart.getAxisRight().setEnabled(false);
        mChart.getXAxis().setEnabled(false);

        XAxis xAxis = mChart.getXAxis();
        xAxis.setEnabled(false);
        mChart.getAxisLeft().setEnabled(false);
    }

    public BarChart getmChart() {
        return mChart;
    }

    public void toggleDemoData(boolean isChecked) {
        demo = isChecked;
        updateGraph();
    }

    public void updateGraph() {
        if (demo) {
            generateBarData(1, 2000000, 60);
            mChart.setData(getCurrentReadings());
        } else {
            readPersistedValues();
            mChart.setData(getCurrentReadings());
        }
        mChart.invalidate();
        callback.updateCaloriesAndSteps(totalCalories, totalSteps);
    }

    private void readPersistedValues() {
        String activitySamplerQuery = "SELECT * FROM " + ActivitySampleContract.ActivitySampleEntry.TABLE_NAME + " ORDER BY " +
                ActivitySampleContract.ActivitySampleEntry.COLUMN_NAME_SAMPLE_TIME + " DESC LIMIT 61";

        if (activitySampleDb != null) {
            Cursor activitySampleCursor = activitySampleDb.rawQuery(activitySamplerQuery, null);

            int activitytSampleCount = activitySampleCursor.getCount();

            int dbStartIndex = 0;

            if (activitytSampleCount < 61) {
                dbStartIndex = 61 - activitytSampleCount;
                int zeroDbStartIndex = (dbStartIndex == 61) ? 60 : dbStartIndex;
                for (int i = 0; i <= zeroDbStartIndex; i++) {
                    activitySamples[i].setDate("");
                    activitySamples[i].setTotalMilliG(0L);
                    activitySamples[i].setRawSteps(0);
                }
            }

            for (int i = 60; i >= dbStartIndex; i--) {
                activitySampleCursor.moveToNext();
                String date = activitySampleCursor.getString(activitySampleCursor.getColumnIndex(ActivitySampleContract.ActivitySampleEntry.COLUMN_NAME_SAMPLE_TIME));
                Long milliG = activitySampleCursor.getLong(activitySampleCursor.getColumnIndex(ActivitySampleContract.ActivitySampleEntry.COLUMN_NAME_MILLIG));
                Integer steps = activitySampleCursor.getInt(activitySampleCursor.getColumnIndex(ActivitySampleContract.ActivitySampleEntry.COLUMN_NAME_STEPS));
                activitySamples[i].setDate(date);
                activitySamples[i].setTotalMilliG(milliG);
                activitySamples[i].setRawSteps(steps);

                if(steps > 0) {
                    bmi160 = true;
                }

                Log.i("GraphFragment data time ", date);
                Log.i("GraphFragment data value ", String.valueOf(milliG));
            }
            activitySampleCursor.close();
        }
    }

    public BarData getCurrentReadings() {
        ArrayList<BarDataSet> sets = new ArrayList<BarDataSet>();

        ArrayList<BarEntry> entries = new ArrayList<BarEntry>();

        totalCalories = 0;
        totalSteps = 0;

        for (int j = 1; j < 61; j++) {
            if(bmi160){
                activitySamples[j].setSteps(activitySamples[j].getRawSteps() - activitySamples[j - 1].getRawSteps());
            }else {
                activitySamples[j].setIndividualMilliG(activitySamples[j].getTotalMilliG() - activitySamples[j - 1].getTotalMilliG());
            }
            totalCalories += activitySamples[j].getCalories();
            totalSteps += activitySamples[j].getSteps();
            entries.add(new BarEntry(activitySamples[j].getSteps(), j));
            Log.i("GraphFragment", "Total MilliG prev: " + activitySamples[j-1].getTotalMilliG());
            Log.i("GraphFragment", "Total MilliG: " + activitySamples[j].getTotalMilliG());
            Log.i("GraphFragment", "Individual MilliG: " + activitySamples[j].getIndividualMilliG());
            Log.i("GraphFragment", "Steps: " + activitySamples[j].getSteps());
            Log.i("GraphFragment", "Calories: " + activitySamples[j].getCalories());
        }

        BarDataSet ds = new BarDataSet(entries, getLabel(0));

        ds.setColors(colors);
        sets.add(ds);

        return new BarData(ChartData.generateXVals(0, 60), sets);
    }

    public ActivitySample getActivitySample(int index) {
        return activitySamples[index];
    }

    protected void generateBarData(int dataSets, float range, int count) {

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

            Long runningTotal = 0L;
            for (int j = 0; j < count; j++) {
                activitySamples[j].setDate(dateFormat.format(new Date(System.currentTimeMillis() - (60000 * (count - j)))));
                runningTotal += (long) ((Math.random() * range) + range / 4);
                activitySamples[j].setTotalMilliG(runningTotal);
            }
    }

    private String[] mLabels = new String[]{"Activity A", "Activity B", "Activity C", "Activity D", "Activity E", "Activity F"};

    private String getLabel(int i) {
        return mLabels[i];
    }
}
