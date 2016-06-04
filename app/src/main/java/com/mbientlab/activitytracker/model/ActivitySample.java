package com.mbientlab.activitytracker.model;

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
 * Created by Lance Gleason of Polyglot Programming LLC. on 4/27/15.
 * http://www.polyglotprogramminginc.com
 * https://github.com/lgleasain
 * Twitter: @lgleasain
 */
public class ActivitySample {
    // Rough estimate of how many raw accelerometer counts are in a step
    public final static int ACTIVITY_PER_STEP = 20000;
    // Estimate of calories burned per step assuming casual walking speed @150 pounds
    public final static double CALORIES_PER_STEP = 0.045;
    private String date = "";
    private Long totalMilliG = 0L;
    private int steps = 0;
    private int rawSteps = 0;
    private int calories = 0;
    private Long individualMilliG = 0L;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Long getTotalMilliG() {
        return totalMilliG;
    }

    public void setIndividualMilliG(Long milliG) {
        this.individualMilliG = milliG;
        if(milliG > 0) {
            steps = (int) (milliG / ACTIVITY_PER_STEP);
            calories = (int) (steps * CALORIES_PER_STEP);
        } else {
            steps = 0;
            calories = 0;
        }
    }

    public void setSteps(int steps){
        this.steps = steps;
        if(steps >0){
            calories = (int) (steps * CALORIES_PER_STEP);
        }else{
            calories = 0;
        }
    }

    public Long getIndividualMilliG() {
        return individualMilliG;
    }

    public void setTotalMilliG(Long milliG) {
        this.totalMilliG = milliG;
    }

    public int getSteps() {
        return steps;
    }

    public int getCalories() {
        return calories;
    }

    public void setRawSteps(int steps) {
        this.rawSteps = steps;
    }

    public int getRawSteps(){
        return rawSteps;
    }
}
