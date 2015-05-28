package com.med101.obdobdobd;

import android.app.AlertDialog;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    public static final String ssid = "WiFiOBD";
    public static final String password = "12345678";

    final Handler myHandler = new Handler();

    List<ScanResult> results;
    Button button;
    ObdCommunication obdCommunication;
    WifiManager wifiManager;
    TextView speedTextView,rpmTextView, interfaceTextView,ambientAirTV, fuelLevelPercentTV,vehicleEcuNameTV, fuelStatusTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        initViewStuffs();
        initWifiStuffs();
        initObdStuffs();
    }

    private void initWifiStuffs() {
        results = new ArrayList<>();
        wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);

        //check wifi state, if disabled enable it
        if(wifiManager.isWifiEnabled() == false){
            wifiManager.setWifiEnabled(true);
        }

        //get list of networks
        results = wifiManager.getScanResults();

        //connect to a specific object
        int networkId = wifiManager.addNetwork(getConfigurationObject());
        if(networkId!= -1)
        {
            Log.d("MainActivity", "Can connect to that network.");
            wifiManager.enableNetwork(networkId, true);// to connect
        }
    }

    private WifiConfiguration getConfigurationObject() {

        WifiConfiguration wfc = new WifiConfiguration();

        wfc.SSID = "\"".concat(ssid).concat("\"");
        wfc.status = WifiConfiguration.Status.ENABLED;
        wfc.priority = 40;
        //wfc.preSharedKey = "\"12345678\"";
        wfc.preSharedKey = password;
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        return wfc;
    }

    private void initViewStuffs(){
        ActionBar actionBar = getSupportActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);
       // actionBar.setDisplayShowHomeEnabled(true);
       // actionBar.setIcon(R.drawable.app_icon);

        speedTextView = (TextView) findViewById(R.id.speed);
        interfaceTextView = (TextView) findViewById(R.id.interfaceVersion);
        ambientAirTV = (TextView) findViewById(R.id.ambientAir);
        fuelLevelPercentTV = (TextView) findViewById(R.id.fuelLevelPercent);
        fuelStatusTV = (TextView) findViewById(R.id.fuelStatus);
        rpmTextView = (TextView) findViewById(R.id.rpmStatus);
        button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wifiManager.isWifiEnabled()) {
                    /*WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    //get network ssid
                    String networkSSID = wifiInfo.getSSID();
                    //if we are in the correct network do work
                    if(networkSSID.equals(ssid)){*/
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            doStuffs();
                        }
                    });
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    AlertDialog dialog = builder.create();
                    dialog.setCancelable(true);
                    dialog.setTitle("Error");
                    dialog.setMessage("You are not connected to wifi adapter or you don't have wifi connection.");
                    dialog.show();
                }

            }

        });
    }

    private void initObdStuffs() {
        //check again to see if the wifi is enabled
        if (wifiManager.isWifiEnabled()){
           /* WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            //get network ssid
            String networkSSID = wifiInfo.getSSID();
            //if we are in the correct network do work
            if(networkSSID.equals(ssid)){*/
                obdCommunication = new ObdCommunication();
                try {
                    obdCommunication.init();

                    interfaceTextView.setText(obdCommunication.getInterfaceVersionNumber());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                AlertDialog dialog  = builder.create();
                dialog.setCancelable(true);
                dialog.setTitle("Error");
                dialog.setMessage("You are not connected to wifi adapter or you don't have wifi connection.");
                dialog.show();
            }


    }

    private void doStuffs() {
            try {

                //speedTextView.setText(obdCommunication.getVehicleSpeed());


                //ambientAirTV.setText(obdCommunication.getAmbientalAirTemperature());

                int xd = obdCommunication.getSpeed();

                speedTextView.setText(xd + " km/h");


                obdCommunication.getAmbientalAirTemperature();

                obdCommunication.stringResponse = obdCommunication.stringResponse.replace("SEARCHING...","");
                int iv = obdCommunication.getCoolantTemperature();

                ambientAirTV.setText(iv + " C");

                int status = obdCommunication.getInTankTemp();
                fuelLevelPercentTV.setText(status + " C");

                double c = obdCommunication.getEngineRPM();
                rpmTextView.setText(c + " rpm");

                int x = obdCommunication.getFuelSystemStatus();
                fuelStatusTV.setText(x + " l");

            } catch (IOException e) {
                e.printStackTrace();
            }

    }

    public String convertHexToString(String hex){

        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        //49204c6f7665204a617661 split into two characters 49, 20, 4c...
        for( int i=0; i<hex.length()-1; i+=2 ){

            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char)decimal);

            temp.append(decimal);
        }
        System.out.println("Decimal : " + temp.toString());

        return sb.toString();
    }



}
