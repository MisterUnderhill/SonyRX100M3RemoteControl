package uk.co.lost_boyproductions.sonyrx100m3remotecontrol;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.StrictMode;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import static uk.co.lost_boyproductions.sonyrx100m3remotecontrol.R.id.action_settings;
import static uk.co.lost_boyproductions.sonyrx100m3remotecontrol.R.menu.actionbar;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {

    SeekBar lensZoom;
    int lensZoomPosition = 50;
    int lensZoomOldPosition = 50; // set to the same value as android.progress in layout definition


    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        final TextView mTextView = (TextView) findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        lensZoom=(SeekBar)findViewById(R.id.lensZoom);
//        lensZoom.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener) this);
        lensZoom.setOnTouchListener(mDelayHideTouchListener);

        /* SIMON
         * Start of code extract from http://www.milosev.com/83-android/485-simple-service-discovery-protocol.html
         */

        final String DISCOVER_MESSAGE = "M-SEARCH * HTTP/1.1\r\n"
                + "HOST: 239.255.255.250:1900\r\n" + "MAN: \"ssdp:discover\"\r\n"
                + "MX: 3\r\n" + "ST: urn:schemas-sony-com:service:ScalarWebAPI:1\r\n"
                + "USER-AGENT: OS/version product/version";

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            MulticastSocket s = new MulticastSocket(1900);
            s.joinGroup(InetAddress.getByName("239.255.255.250") );
            DatagramPacket packet = new DatagramPacket(DISCOVER_MESSAGE.getBytes(), DISCOVER_MESSAGE.length(), getBroadcastAddress(), 1900);
            s.setBroadcast(true);
            s.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            MulticastSocket s = new MulticastSocket(1900);
            s.joinGroup(InetAddress.getByName("239.255.255.250") );
            DatagramPacket packet = new DatagramPacket(DISCOVER_MESSAGE.getBytes(), DISCOVER_MESSAGE.length(), getBroadcastAddress(), 1900);
            s.setBroadcast(true);
            s.setSoTimeout(10000); // Wait 10 seconds for a response

            while(true) {
                byte[] buf = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

                s.receive(receivePacket);
                String msg = new String(receivePacket.getData(), 0, receivePacket.getLength());
                mTextView.setText("SSDP Response : " + msg.substring(0,receivePacket.getLength()) + "\n\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* SIMON
         * End of code extract from http://www.milosev.com/83-android/485-simple-service-discovery-protocol.html
         */

        /* SIMON
         * Start of code extract from https://developer.android.com/training/volley/simple.html
         */

        RequestQueue queue = Volley.newRequestQueue(this);  // Instantiate the RequestQueue.

        // Request a string response from the provided URL.
        String url ="http://www.google.com";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // Display the first 500 characters of the response string.
                    mTextView.append("\n\nVolley is : "+ response.substring(0,500));
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mTextView.setText("That didn't work!");
                }
            }
        );

        queue.add(stringRequest);   // Add the request to the RequestQueue.

        /* SIMON
         * End of code extract from https://developer.android.com/training/volley/simple.html
         */
    }

    InetAddress getBroadcastAddress() throws IOException {

        Context mContext = getApplicationContext();
        WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(actionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case action_settings:
                // User chose the "Settings" item, show the app settings UI...
        /* SIMON
         * Start of code extract from https://stackoverflow.com/questions/32411898/how-to-set-up-settings-activity-in-android-studio
         */
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
        /* SIMON
         * End of code extract from https://stackoverflow.com/questions/32411898/how-to-set-up-settings-activity-in-android-studio
         */

                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
/*        if (progress > lensZoomPosition) { // this would be a zoom out action
            Toast.makeText(getApplicationContext(), "Increased", Toast.LENGTH_SHORT).show();
        } else { // this would be a zoom in action
            Toast.makeText(getApplicationContext(), "Decreased", Toast.LENGTH_SHORT).show();
        }
*/    }


    public void onStartTrackingTouch(SeekBar seekBar) {
//        Toast.makeText(getApplicationContext(),"seekbar touch started!", Toast.LENGTH_SHORT).show();
    }


    public void onStopTrackingTouch(SeekBar seekBar) {
        lensZoomPosition = lensZoom.getProgress(); //
        if (lensZoomOldPosition < lensZoomPosition) {
            Toast.makeText(getApplicationContext(),"seekbar final position is higher: " + lensZoomPosition, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(),"seekbar final position is lower: " + lensZoomPosition, Toast.LENGTH_SHORT).show();
        }
        lensZoomOldPosition = lensZoomPosition;
//        Toast.makeText(getApplicationContext(),"seekbar touch stopped!", Toast.LENGTH_SHORT).show();
    }

}
