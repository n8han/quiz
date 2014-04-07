package us.technically.quiz.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class MainActivity extends Activity {

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private static final String TAG = "MyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //  Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        final Button host = (Button) findViewById(R.id.host);
        host.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                registerReceiver(joinBroadcastReceiver, intentFilter);
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
        });
        final Button join = (Button) findViewById(R.id.join);
        join.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                registerReceiver(joinBroadcastReceiver, intentFilter);
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
        });
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
    }

    private WifiP2pManager.PeerListListener hostPeerListListener = new WifiP2pManager.PeerListListener() {
        private Set<String> availableDevices = new HashSet<String>();

        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            for (final WifiP2pDevice device : peerList.getDeviceList()) {
                Log.d(TAG, "device available " + device.deviceName);
                if (availableDevices.add(device.deviceAddress)) {
                    Log.d(TAG, "device connecting " + device.deviceAddress);
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
                            Toast.makeText(MainActivity.this, "Connect failed to " + device.deviceName, Toast.LENGTH_SHORT).show();
                            availableDevices.remove(device.deviceAddress);
                        }
                    });
                }
            }
        }
    };
    private BroadcastReceiver hostBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // Determine if Wifi P2P mode is enabled or not, whatever
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "P2P peers changed");
                mManager.requestPeers(mChannel, hostPeerListListener);
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "Connection changed action");
                NetworkInfo networkInfo = (NetworkInfo) intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            }
        }
    };
    private BroadcastReceiver joinBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // Determine if Wifi P2P mode is enabled or not, whatever
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "P2P peers changed");
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "Connection changed action");
                NetworkInfo networkInfo = (NetworkInfo) intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    // we are connected with the other device, request connection
                    // info to find group owner IP
                    mManager.requestConnectionInfo(mChannel, joinInfoListener);
                }
            }
        }
    };

    private WifiP2pManager.ConnectionInfoListener joinInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            Log.d(TAG, "groupOwnerAddress" + wifiP2pInfo.groupOwnerAddress);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
/*        receiver = new WiFiDirectBroadcastReceiver();
        registerReceiver(receiver, intentFilter);*/
    }

    @Override
    public void onPause() {
        super.onPause();
//        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
