package com.example.perffer;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.perffer.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    //String appFileDirectory = getFilesDir().getPath();
    //String executableFilePath = appFileDirectory + "/iperf39";

    public void copy(InputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while ((length = source.read(buf)) > 0) {
            target.write(buf, 0, length);
        }
    }

    public void runClient() {

        File file = new File(getApplicationInfo().nativeLibraryDir, "libiperf39");
        if(!file.exists()){
            Log.d("Client", "Cannot find libiperf39");
        }

        try {
            Os.setenv("TEMP", getFilesDir().getPath(), true);
            Os.setenv("TMPDIR", getFilesDir().getPath(), true);
            Os.setenv("TMP", getFilesDir().getPath(), true);
            Log.d("Client", "TEMP set done");
        }
        catch (ErrnoException e) {
            e.printStackTrace();
        }

        String executableFilePath = getApplicationInfo().nativeLibraryDir + "/libiperf39";
        String parameters = " -c 192.168.1.2 -p 5201 -Z -R -P 4 -J -t 5 -l [128KB] -b [1GB] ";
        // -J = json output
        // -R = reverse: server sends to client
        // -n, --num n[KM] The number of buffers to transmit. Normally, iPerf sends for 10 seconds. The -n option overrides this and sends an array of len bytes num times,
        //           no matter how long that takes. See also the -l, -k and -t options.
        // -Z, --zerocopy : use a 'zero copy' sendfile() method of sending data. This uses much less CPU.
        // -k, --blockcount The number of blocks (packets) to transmit. (instead of -t or -n) See also the -t, -l and -n options.
        // -b, --bandwidth Set target bandwidth to n bits/sec (default 1 Mbit/sec for UDP, unlimited for TCP).
        //                 If there are multiple streams (-P flag),
        // -t, --time n The time in seconds to transmit for. iPerf normally works by repeatedly sending
        //              an array of len bytes for time seconds. Default is 10 seconds. See also the -l, -k and -n options.
        // -P, --parallel n The number of simultaneous connections to make to the server. Default is 1.
        Log.d("Client", executableFilePath + parameters);

        try {
            Process process = Runtime.getRuntime().exec(executableFilePath + parameters);
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errs = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line = "";
            String data = "";
            while ((line = in.readLine()) != null) {
                Log.d("Client", line);
                data += line;
            }

            while ((line = errs.readLine()) != null) {
                Log.d("Client errors:", line);
            }

            JSONObject myjson = null;
            try {
                myjson = new JSONObject(data);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                JSONArray value = myjson.getJSONArray("intervals");
                for (int i = 0; i < value.length(); i++) {
                    JSONObject item = value.getJSONObject(i);
                    double bitspersec = item.getJSONObject("sum").getDouble("bits_per_second");
                    bitspersec /= 1024 * 1024;
                    Log.d("speed", String.valueOf(bitspersec));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                new Thread(new Runnable() {
                    public void run() {
                        // a potentially time consuming task
                        runClient();
                    }
                }).start();
            }
        });
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}