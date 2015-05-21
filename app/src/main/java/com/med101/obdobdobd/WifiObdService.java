package com.med101.obdobdobd;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by alexgaluska on 05/05/15.
 */
public class WifiObdService extends Service {


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
