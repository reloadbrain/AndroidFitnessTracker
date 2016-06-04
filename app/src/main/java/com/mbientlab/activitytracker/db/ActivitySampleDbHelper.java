package com.mbientlab.activitytracker.db;

import android.content.Context;

import com.mbientlab.activitytracker.model.ActivitySampleContract;

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
public class ActivitySampleDbHelper extends DbHelper{

    private static final String[] SQL_CREATE_ENTRIES = {
            "CREATE TABLE " + ActivitySampleContract.ActivitySampleEntry.TABLE_NAME + " (" +
                    ActivitySampleContract.ActivitySampleEntry._ID + " INTEGER PRIMARY KEY," +
                    ActivitySampleContract.ActivitySampleEntry.COLUMN_NAME_SAMPLE_TIME + TEXT_TYPE + COMMA_SEP +
                    ActivitySampleContract.ActivitySampleEntry.COLUMN_NAME_MILLIG + INT_TYPE + COMMA_SEP +
                    ActivitySampleContract.ActivitySampleEntry.COLUMN_NAME_STEPS + INT_TYPE +
                    " )"};


    private static final String[] SQL_DELETE_ENTRIES = {
            "DROP TABLE IF EXISTS " + ActivitySampleContract.ActivitySampleEntry.TABLE_NAME
    };

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "MetaTracker.db";

    public ActivitySampleDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, SQL_CREATE_ENTRIES, SQL_DELETE_ENTRIES);
    }

}
