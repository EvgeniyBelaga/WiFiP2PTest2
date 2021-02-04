package com.example.wifip2ptest2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private  WifiP2pManager.Channel mChanel;
    private  MainActivity mActivity;

    public WiFiDirectBroadcastReceiver(WifiP2pManager mManager, WifiP2pManager.Channel mChanel, MainActivity mActivity){
        this.mManager= mManager;
        this.mChanel= mChanel;
        this.mActivity= mActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            //do something
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Toast.makeText(context, "Wifi is ON", Toast.LENGTH_LONG).show();
                Log.d("MyP2PTest", "Broadcast Wifi is ON");
            } else {
                Toast.makeText(context, "Wifi is OFF", Toast.LENGTH_LONG).show();
                Log.d("MyP2PTest", "Broadcast Wifi is OFF");
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            //do something
            if(mManager!=null){
                mManager.requestPeers(mChanel, mActivity.peerListListener);
                Log.d("MyP2PTest", "Broadcast WIFI_P2P_PEERS_CHANGED_ACTION");
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            if(mManager==null){
                Log.d("MyP2PTest", "Broadcast mManager==null");
                return;
            }
            NetworkInfo networkInfo= intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if(networkInfo.isConnected()){
                mManager.requestConnectionInfo(mChanel, mActivity.connectionInfoListener);
                Log.d("MyP2PTest", "Broadcast networkInfo.isConnected()");
            }
            else{
                mActivity.tvConnectionStatus.setText("Device Disconnected");
                Log.d("MyP2PTest", "Broadcast Device Disconnected");
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
        }

    }
}
