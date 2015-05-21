package com.med101.obdobdobd;

import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import static android.net.wifi.WifiManager.*;


public class MainActivity extends ActionBarActivity {

    public static final String ssid = "WiFiOBD";
    public static final int Port = 35000;
    List<ScanResult> results;
    InetAddress broadcastAddress;
    EditText editText;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = (EditText)findViewById(R.id.editText);
        button = (Button)findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = editText.getText().toString();
                sendUdp(message);
            }
        });

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        results = new ArrayList<>();

        WifiManager wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);

        //check wifi state, if disabled enable it
        if(wifiManager.isWifiEnabled() == false){
            wifiManager.setWifiEnabled(true);
        }

        //get list of networks
        results = wifiManager.getScanResults();

        //connect to a specific object
        int networkId = wifiManager.addNetwork(getConfigurationObject());
        if(networkId!= -1){
            Log.d("MainActivity", "Can connect to that network.");
            wifiManager.enableNetwork(networkId, true);// to connect
            try {
                //String ipAddress = getLocalIpAddress();
                //String broadCastAddr = getBroadcast();

//getMultiCastLock();
                broadcastAddress = getBroadcastAddress();//InetAddress.getByName("192.168.0.245");
               // _mcastLock.release();
                Log.d("Address", "addres is " + broadcastAddress.toString());


               // WifiManager.MulticastLock lock = wifiManager.createMulticastLock("dk.aboaya.pingpong");
                //lock.acquire();
                sendUdp("AT D\n" + //send all to defaults
                        "AT Z\n" + // reset obd
                        "AT E0\n" + // echo off
                        "AT L0\n" + // line feed off
                        "AT S0\n" + // spaces of
                        "AT H0\n" +//headers off
                        "AT SP 0\n");//set protocol to 0 -> auto

                //lock.release();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }
    private InetAddress getBroadcastAddress() throws IOException {
        WifiManager myWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        DhcpInfo myDhcpInfo = myWifiManager.getDhcpInfo();
        if (myDhcpInfo == null) {
            System.out.println("Could not get broadcast address");
            return null;
        }

        //myDhcpInfo.netmask = parseIp("255.255.255.0");
        int broadcast = (myDhcpInfo.ipAddress & myDhcpInfo.netmask) | ~myDhcpInfo.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }


boolean sendUdp = false;
    public void sendUdp(String udpMsg) {

        udpOutputData = udpMsg;
        sendUdp = true;
        udpSendThread.run();
    }

    String udpOutputData="";
    Thread udpSendThread = new Thread(new Runnable() {

        @Override
        public void run() {


            while (true) {

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

                if (sendUdp == true) {

                    try {

                        // get server name
                        //InetAddress serverAddr = InetAddress.getByName("192.168.0.10");
                        InetAddress serverAddr = broadcastAddress;
                        Log.d("UDP", "C: Connecting...");


                        // create new UDP socket
                        DatagramSocket socket = new DatagramSocket(Port);

                        socket.setBroadcast(true);
                        socket.setSoTimeout(15000);

                        // prepare data to be sent
                        byte[] buf = udpOutputData.getBytes();

                        // create a UDP packet with data and its destination ip & port
                        DatagramPacket packetSend = new DatagramPacket(buf, buf.length, getBroadcastAddress(), Port);
                        Log.d("UDP", "C: Sending: '" + new String(buf) + "'");

                        socket.setBroadcast(true);
                        boolean x = socket.isConnected();

                        // send the UDP packet
                        socket.send(packetSend);


                        Log.d("UDP", "C: Sent.");
                        Log.d("UDP", "C: Done.");

                        //wait for response
                        byte[] buf2 = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buf2, buf.length);
                        socket.getLocalAddress();
                        socket.receive(packet);

                        String x3 = bytesToStringUTFCustom(buf2);
                        Log.d("UDP", "C: receive.");
                        Log.d("UDP", "C: Done.");
                        //Thread.sleep(5000);
                        socket.close();


                    } catch (Exception e) {
                        Log.e("UDP", "C: Error", e);

                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }


                    sendUdp = false;
                }

            }
        }
    });

    public static String bytesToStringUTFCustom(byte[] bytes) {

        char[] buffer = new char[bytes.length >> 1];

        for(int i = 0; i < buffer.length; i++) {

            int bpos = i << 1;

            char c = (char)(((bytes[bpos]&0x00FF)<<8) + (bytes[bpos+1]&0x00FF));

            buffer[i] = c;

        }

        return new String(buffer);

    }


    private WifiConfiguration getConfigurationObject() {

        WifiConfiguration wfc = new WifiConfiguration();

        wfc.SSID = "\"".concat(ssid).concat("\"");
        wfc.status = WifiConfiguration.Status.ENABLED;
        wfc.priority = 40;
        wfc.preSharedKey = "\"12345678\"";
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        return wfc;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(this,SecondActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
