package com.example.wifip2ptest2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button btnOnOff, btnDiscover, btnSend;
    ListView listView;
    TextView tvRead_msg_box, tvConnectionStatus;
    EditText etWriteMsg;

    WifiManager wifiManager;
    WifiP2pManager mWifiP2pManager;
    WifiP2pManager.Channel mChannel;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    List<WifiP2pDevice> peers= new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    static final int MESSAGE_READ= 1;

    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialWork();

        exqListener();
    }

    Handler handler= new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){

                case MESSAGE_READ:
                    byte[] readBuff= (byte[]) msg.obj;
                    String tempMsg= new String(readBuff, 0, msg.arg1);
                    tvRead_msg_box.setText(tempMsg);
                    break;
            }
            return true;
        }
    });

    private void initialWork(){
        btnOnOff= findViewById(R.id.onOff);
        btnDiscover= findViewById(R.id.discover);
        btnSend= findViewById(R.id.sendButton);
        listView= findViewById(R.id.peerListView);
        tvRead_msg_box= findViewById(R.id.readMsg);
        tvConnectionStatus= findViewById(R.id.connectionStatus);
        etWriteMsg= findViewById(R.id.writeMsg);

        wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mWifiP2pManager= (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel= mWifiP2pManager.initialize(this, getMainLooper(), null);

        mReceiver= new WiFiDirectBroadcastReceiver(mWifiP2pManager, mChannel, this);

        mIntentFilter= new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    }

    private void exqListener(){

        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(wifiManager.isWifiEnabled()){
                    wifiManager.setWifiEnabled(false);
                    btnOnOff.setText("WIFI ON");
                    Log.d("MyP2PTest", "MainActivity WIFI ON");
                }
                else{
                    wifiManager.setWifiEnabled(true);
                    btnOnOff.setText("WIFI OFF");
                    Log.d("MyP2PTest", "MainActivity WIFI OFF");
                }
            }
        });


        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        tvConnectionStatus.setText("Discovery Started");
                        Log.d("MyP2PTest", "MainActivity Discovery Started");
                    }

                    @Override
                    public void onFailure(int reason) {
                        tvConnectionStatus.setText("Discovery Starting Failed");
                        Log.d("MyP2PTest", "MainActivity Discovery Starting Failed");
                    }
                });
            }
        });


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final WifiP2pDevice device= deviceArray[position];
                WifiP2pConfig config= new WifiP2pConfig();
                config.deviceAddress= device.deviceAddress;

                mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Connected to "+device.deviceName, Toast.LENGTH_SHORT).show();
                        Log.d("MyP2PTest", "MainActivity Connected to "+device.deviceName);
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(MainActivity.this, "Not connected", Toast.LENGTH_SHORT).show();
                        Log.d("MyP2PTest", "MainActivityNot connected");
                    }
                });
            }
        });


        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String msg= etWriteMsg.getText().toString();
                sendReceive.write(msg.getBytes());
            }
        });
    }

    WifiP2pManager.PeerListListener peerListListener= new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if(!peerList.getDeviceList().equals(peers)){
                peers.clear();
                peers.addAll(peerList.getDeviceList());

                Log.d("MyP2PTest", "MainActivity Size: "+peerList.getDeviceList().size());

                deviceNameArray= new String[peerList.getDeviceList().size()];
                deviceArray= new WifiP2pDevice[peerList.getDeviceList().size()];

                int index= 0;

                for(WifiP2pDevice device: peerList.getDeviceList()){
                    deviceNameArray[index]= device.deviceName;
                    Log.d("MyP2PTest", "MainActivity Device:"+device.deviceName+" "+device.primaryDeviceType+" "+device.secondaryDeviceType+" "+device.deviceAddress+" "+device.describeContents());
                    deviceArray[index]= device;
                    index++;
                }

                ArrayAdapter<String> adapter= new ArrayAdapter<String>(getApplicationContext(),
                        android.R.layout.simple_list_item_1, deviceNameArray);

                listView.setAdapter(adapter);

                if(peers.size()==0){
                    Toast.makeText(MainActivity.this, "No Device Found", Toast.LENGTH_SHORT).show();
                    Log.d("MyP2PTest", "MainActivity No Device Found");
                    return;
                }

            }
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener= new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {

            final InetAddress groupOwnerAddress= wifiP2pInfo.groupOwnerAddress;

            if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){
                tvConnectionStatus.setText("Host");
                Log.d("MyP2PTest", "MainActivity Host");
                serverClass= new ServerClass();
                serverClass.start();
            }
            else if(wifiP2pInfo.groupFormed){
                tvConnectionStatus.setText("Client");
                Log.d("MyP2PTest", "MainActivity Client");
                clientClass= new ClientClass(groupOwnerAddress);
                clientClass.start();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public class ServerClass extends Thread{

        Socket socket;
        ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                serverSocket= new ServerSocket(8888);
                socket= serverSocket.accept();
                sendReceive= new SendReceive(socket);
                sendReceive.start();
                Log.d("MyP2PTest", "MainActivity sendReceive.start()");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SendReceive extends Thread{

        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket skt){
            socket= skt;
            try {
                inputStream= socket.getInputStream();
                outputStream= socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {

            byte[] buffer= new byte[1024];
            int bytes;

            while (socket!=null){
                try {
                    bytes= inputStream.read(buffer);
                    if(bytes>0){
                        handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                        Log.d("MyP2PTest", "MainActivity handler.obtainMessage(MESSAGE_READ");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes){


            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try  {
                        //Your code goes here
                        try {
                            outputStream.write(bytes);
                            Log.d("MyP2PTest", "MainActivity outputStream.write(bytes);");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            thread.start();

//            try {
//                outputStream.write(bytes);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }

    public class ClientClass extends Thread{

        Socket socket;
        String hostAdd;

        public ClientClass(InetAddress hostAddress){
            hostAdd= hostAddress.getHostAddress();
            socket= new Socket();
        }

        @Override
        public void run() {

            try {
                socket.connect(new InetSocketAddress(hostAdd, 8888), 500);
                sendReceive= new SendReceive(socket);
                sendReceive.start();
                Log.d("MyP2PTest", "MainActivity sendReceive.start();");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}