package com.mbientlab.activitytracker.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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
public abstract class DbHelper extends SQLiteOpenHelper {

    protected static final String TEXT_TYPE = " TEXT";
    protected static final String INT_TYPE = " INT";
    protected static final String REAL_TYPE = " REAL";
    protected static final String COMMA_SEP = ",";
    public static String[] CREATE_ENTRIES_SQL;
    public static String[] DELETE_ENTRIES_SQL;
    // probably need to remove this

    public DbHelper(Context context, String databaseName, SQLiteDatabase.CursorFactory cursorFactory, int dbVersion){
        super(context, databaseName, cursorFactory, dbVersion);
    }

    public DbHelper(Context context, String databaseName, SQLiteDatabase.CursorFactory cursorFactory, int dbVersion, String[] createEntriesSql, String[] deleteEntriesSql) {
        this(context, databaseName, cursorFactory, dbVersion);
        CREATE_ENTRIES_SQL = createEntriesSql;
        DELETE_ENTRIES_SQL = deleteEntriesSql;
    }

    public void onCreate(SQLiteDatabase db) {
        for(int i = 0; i < CREATE_ENTRIES_SQL.length; i++) {
            db.execSQL(CREATE_ENTRIES_SQL[i]);
        }
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        for(int i = 0; i < DELETE_ENTRIES_SQL.length; i++) {
            db.execSQL(DELETE_ENTRIES_SQL[i]);
        }
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

}

