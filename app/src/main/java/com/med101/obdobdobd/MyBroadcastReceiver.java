package com.med101.obdobdobd;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.Collection;

/**
 * Created by alexgaluska on 05/05/15.
 */
public class MyBroadcastReceiver extends BroadcastReceiver {

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    Activity mActivity;

    WifiP2pManager.PeerListListener myPeerListListener;

    public MyBroadcastReceiver(WifiP2pManager manager,WifiP2pManager.Channel channel,Activity activity){
        mManager = manager;
        mChannel = channel;
        mActivity = activity;
        myPeerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                Log.d("PeerListener","Peers size is " + peers.getDeviceList().size());
                Collection<WifiP2pDevice> devices = peers.getDeviceList();

                while(devices.iterator().hasNext()){
                    Log.d("ForListener"," " + devices.iterator().toString());
                    devices.iterator().next();
                }
            }
        };
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
                Log.d("BR","P2p enabled");
            } else {
                // Wi-Fi P2P is not enabled

                Log.d("BR", "P2p disabled");
            }
        }

        if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
            if(mManager!=null){
                mManager.requestPeers(mChannel,myPeerListListener);
            }
        }
    }
}
