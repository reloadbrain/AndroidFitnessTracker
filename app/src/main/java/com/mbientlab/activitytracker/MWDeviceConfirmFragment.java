package com.mbientlab.activitytracker;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Led;


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
public class MWDeviceConfirmFragment extends DialogFragment {
    public interface DeviceConfirmCallback {
        public void pairDevice();
        public void dontPairDevice();
    }


    private Led ledModule = null;
    private Button yesButton = null;
    private Button noButton = null;
    private DeviceConfirmCallback callback = null;
    private String currentState = null;


    public void flashDeviceLight(MetaWearBoard metaWearBoard, FragmentManager fragmentManager) {
         try {
            ledModule = metaWearBoard.getModule(Led.class);
        }catch(UnsupportedModuleException e){
            Log.e("Led Fragment", e.toString());
        }
        ledModule.configureColorChannel(Led.ColorChannel.BLUE)
                .setRiseTime((short)750).setPulseDuration((short) 2000)
                .setRepeatCount((byte)-1).setHighTime((short) 500)
                .setFallTime((short) 750).setLowIntensity((byte)0)
                .setHighIntensity((byte) 31).commit();

        ledModule.play(true);

        show(fragmentManager, "device_confirm_callback");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.metawear_device_confirm, container);
    }

    @Override
    public void onAttach(Activity activity) {
        if (!(activity instanceof DeviceConfirmCallback)) {
            throw new RuntimeException("Acitivty does not implement DeviceConfirmationCallback interface");
        }

        callback= (DeviceConfirmCallback) activity;
        super.onAttach(activity);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        noButton = (Button) view.findViewById(R.id.confirm_no);
        noButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ledModule.stop(false);
                callback.dontPairDevice();
                dismiss();
            }
        });

        yesButton = (Button) view.findViewById(R.id.confirm_yes);
        yesButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ledModule.stop(false);
                callback.pairDevice();
                dismiss();
            }
        });

    }
}
