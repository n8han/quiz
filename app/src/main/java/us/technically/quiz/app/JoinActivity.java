package us.technically.quiz.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;


public class JoinActivity extends WifiActivity {
    private static final String TAG = "JoinActivity";

    private static final int SOCKET_TIMEOUT = 5000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                    // Determine if Wifi P2P mode is enabled or not, whatever
                } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                    Log.d(TAG, "P2P peers changed");
                    mManager.requestPeers(mChannel, joinPeerListListener);
                } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                    Log.d(TAG, "Connection changed action");
                    NetworkInfo networkInfo = (NetworkInfo) intent
                            .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                    if (networkInfo.isConnected()) {
                        Log.d(TAG, "Connected to another device");
                        // we are connected with the other device, request connection
                        // info to find group owner IP
                        mManager.requestConnectionInfo(mChannel, joinInfoListener);
                    }
                }
            }
        };
        registerReceiver(receiver, intentFilter);
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "discovering peers");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Failing to discover peers: " + reasonCode);
            }
        });
    }


    private WifiP2pManager.ConnectionInfoListener joinInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(final WifiP2pInfo wifiP2pInfo) {
            Log.d(TAG, "groupOwnerAddress " + wifiP2pInfo.groupOwnerAddress);
            ((TextView) findViewById(R.id.address)).append(wifiP2pInfo.groupOwnerAddress.toString());

            new AsyncTask<Void,Void,String>() {
                @Override
                protected String doInBackground(Void... voids) {
                    Socket socket = new Socket();
                    int port = HostServer.PORT;

                    try {
                        Log.d(TAG, "Opening client socket - ");
                        socket.bind(null);
                        socket.connect((new InetSocketAddress("192.168.49.1"/*wifiP2pInfo.groupOwnerAddress*/, port)), SOCKET_TIMEOUT);
                        Log.d(TAG, "Opened socket");
                        socket.getOutputStream().write("hello\n".getBytes("utf-8"));
                        socket.getOutputStream().close();
                        socket.close();
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                    }
                    return "";
                }
            }.execute();
        }
    };

    private WifiP2pManager.PeerListListener joinPeerListListener = new WifiP2pManager.PeerListListener() {
        private Set<String> availableDevices = new HashSet<String>();

        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

            for (final WifiP2pDevice device : peerList.getDeviceList()) {
                Log.d(TAG, "device available " + device.deviceName);
                if (availableDevices.add(device.deviceAddress)) {
                    Log.d(TAG, "connecting to " + device.deviceAddress);
                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = device.deviceAddress;
                    config.wps.setup = WpsInfo.PBC;
                    config.groupOwnerIntent = 15;

                    mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.d(TAG, "connection failed to device.deviceName");
                            Toast.makeText(JoinActivity.this, "Connect failed to " + device.deviceName, Toast.LENGTH_LONG).show();
                            availableDevices.remove(device.deviceAddress);
                        }
                    });
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.join, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
