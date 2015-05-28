package com.med101.obdobdobd;

import android.view.View;
import android.widget.TextView;

import java.io.IOException;

/**
 * Created by alexgaluska on 28/05/15.
 */
public class ObdUtilMethods {

    public static void getRpm(View v){
        ObdCommunication obdCommunication = new ObdCommunication();
        try {
            int x = obdCommunication.sendOBDCommand(Constants.ENGINE_SPEED_RPM);
           // obdCommunication.stringResponse;
            if(v instanceof TextView){
                ((TextView) v).setText(x/4);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
