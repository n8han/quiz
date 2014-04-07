package us.technically.quiz.app;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by nathan on 4/7/14.
 */
public class HostServer extends AsyncTask<Void, Void, String> {

    private TextView winner;
    public HostServer(TextView winner) {
        this.winner = winner;
    }
    static final int PORT = 8988;
    static final String TAG = "HostServer";

    @Override
    protected String doInBackground(Void... params) {
        try {
            while (true) {
                ServerSocket serverSocket = new ServerSocket(PORT);
                Log.d(TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(TAG, "Server: connection done");

                InputStream inputstream = client.getInputStream();
                Log.d(TAG, "got input stream");
                byte[] bytes = new byte[1024];
                inputstream.read(bytes);
                String str = new String(bytes, "utf-8");
                winner.setText(str);
                Log.d(TAG, "got winner " + str);
                Thread.sleep(5000);
                serverSocket.close();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

}
