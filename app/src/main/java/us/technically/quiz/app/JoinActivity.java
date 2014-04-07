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
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;


public class JoinActivity extends WifiActivity {
    private static final String TAG = "JoinActivity";

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
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            Log.d(TAG, "groupOwnerAddress" + wifiP2pInfo.groupOwnerAddress);
            ((TextView)findViewById(R.id.host)).append(wifiP2pInfo.groupOwnerAddress.toString());
        }
    };

    private WifiP2pManager.PeerListListener joinPeerListListener = new WifiP2pManager.PeerListListener() {
        private Set<String> availableDevices = new HashSet<String>();

        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

            for (final WifiP2pDevice device : peerList.getDeviceList()) {
                Log.d(TAG, "device available " + device.deviceName);
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
