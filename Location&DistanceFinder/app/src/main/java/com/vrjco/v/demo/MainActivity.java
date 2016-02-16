package com.vrjco.v.demo;
/*
 * Copyright 2016 Vrushabh Jambhulkar
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements View.OnClickListener{

    private static final double DISTANCE_THRESHOLD = 7;

    private TextView tvlat, tvlongi, tvtestlat, tvtestlongi, tvSameLocation;
    private Button bStart, bTest;
    private double lat, longi, test_lat, test_longi;
    private boolean test_is_set, gpsok, networkok, checking = false;
    private LocationManager lm;
    private LogLocationListener logLocationListener;
    private TestLocationListener testLocationListener;
    private Location location;
    private Handler handler;
    private SoundPool sp;
    private int beep_sound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize layout contents.
        initialize_Layout();

        //click listener.
        bTest.setOnClickListener(this);
        bStart.setOnClickListener(this);
    }


    //initialize layout contents
    private void initialize_Layout() {
        tvlat = (TextView) findViewById(R.id.tv_lat);
        tvlongi = (TextView) findViewById(R.id.tv_longi);
        tvtestlat = (TextView) findViewById(R.id.test_lat);
        tvtestlongi = (TextView) findViewById(R.id.test_longi);
        tvSameLocation = (TextView) findViewById(R.id.check_loc);
        bStart = (Button) findViewById(R.id.bstart);
        bTest = (Button) findViewById(R.id.btest);
        test_is_set = false;
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        logLocationListener = new LogLocationListener();
        testLocationListener = new TestLocationListener();
        handler = new Handler(getMainLooper());
        //noinspection deprecation
        sp = new SoundPool(1, AudioManager.STREAM_ALARM, 0);
        beep_sound = sp.load(getApplicationContext(), R.raw.beep, 1);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.bstart){
            if(test_is_set) {
                if(!checking) {
                    startLogging(true);
                    bStart.setText("Stop");
                }else {
                    startLogging(false);
                    bStart.setText("Start");
                }
            }else{
                Toast.makeText(this, "Please Set Test Coordinates!", Toast.LENGTH_SHORT).show();
            }
        }else if(v.getId() == R.id.btest){
            setTestCoordinate();
        }
    }

    // starts searching current location.
    private void startLogging(boolean check) {
        if(check) {
            //checking if gps and network is ON.
            gpsok = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            networkok = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "LOCATION PROVIDER PERMISSIONS NOT GRANTED", Toast.LENGTH_LONG).show();
                return;
            }
            checking = true;
            //checking every 100 miliSec and minDistance change 0
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, logLocationListener);
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, logLocationListener);
            if (gpsok && networkok) {
                Toast.makeText(this, "GPS and NETWORK PROVIDER Found!!", Toast.LENGTH_SHORT).show();
                location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    lat = location.getLatitude();
                    longi = location.getLongitude();
                    tvlat.setText(Double.toString(lat));
                    tvlongi.setText(Double.toString(longi));
                }

            } else {
                Toast.makeText(this, "LOCATION PROVIDER NOT AVAILABLE", Toast.LENGTH_LONG).show();
            }
        }else {
            lm.removeUpdates(logLocationListener);
            checking = false;
        }
    }


    private class LogLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location p1) {
            if (p1 != null) {
                lat = p1.getLatitude();
                longi = p1.getLongitude();
                tvlat.setText(Double.toString(lat));
                tvlongi.setText(Double.toString(longi));
                double disDiff = (CalculateDistanceBetween(test_lat,test_longi,lat,longi)*1000);
                if(disDiff < DISTANCE_THRESHOLD){
                    tvSameLocation.setText("Reached Location");
                    float level = (float) (0.4f + (DISTANCE_THRESHOLD - (int)disDiff)/10);
                    if(level != 0) {
                        sp.play(beep_sound, level, level, 0, 0, 1);
                    }else {
                        sp.play(beep_sound, 1, 1, 0, 0, 1);
                    }
                }else {
                    tvSameLocation.setText("Checking...");
                }
            } else {
                tvlongi.setText("Not available");
                tvlat.setText("Not available");
                tvSameLocation.setText("Check Location");
            }

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {
            Toast.makeText(MainActivity.this, provider.toUpperCase() + " is ENABLED!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(MainActivity.this, provider.toUpperCase() + " is DISABLED!", Toast.LENGTH_SHORT).show();
            tvlat.setText("Not available");
            tvlongi.setText("Not available");
            tvSameLocation.setText("Check Location");

        }

        //using Haversine algorithm for distance calculation
		// referred from StackOverflow.com
        private double CalculateDistanceBetween(double initlat, double initlongi, double lat, double longi) {
            double theta = initlongi - longi;
            double dist = Math.sin(deg2rad(initlat)) * Math.sin(deg2rad(lat)) + Math.cos(deg2rad(initlat)) * Math.cos(deg2rad(lat)) * Math.cos(deg2rad(theta));
            dist = Math.acos(dist);
            dist = rad2deg(dist);
            dist = dist * 60 * 1.1515;
            return (dist);
        }

        private double deg2rad(double deg) {
            return (deg * Math.PI / 180.0);
        }
        private double rad2deg(double rad) {
            return (rad * 180.0 / Math.PI);
        }
    }



// just for testing not part of main app
    private void setTestCoordinate() {
        //checking if gps and network is ON.
        gpsok = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        networkok = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "LOCATION PROVIDER PERMISSIONS NOT GRANTED", Toast.LENGTH_LONG).show();
            return;
        }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1000 * 60 , testLocationListener);
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 1000 * 60 , testLocationListener);
        if (gpsok && networkok) {
            Toast.makeText(this, "GPS and NETWORK PROVIDER Found!!", Toast.LENGTH_SHORT).show();
            location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                test_lat = location.getLatitude();
                test_longi = location.getLongitude();
                tvtestlat.setText(Double.toString(test_lat));
                tvtestlongi.setText(Double.toString(test_longi));
                test_is_set = true;
            }

        } else {
            Toast.makeText(this, "LOCATION PROVIDER NOT AVAILABLE", Toast.LENGTH_LONG).show();
            test_is_set = false;
        }

        lm.removeUpdates(testLocationListener);
    }

    private class TestLocationListener implements LocationListener{
        @Override
        public void onLocationChanged(Location p1) {
            if (p1 != null) {
                test_lat = p1.getLatitude();
                test_longi = p1.getLongitude();
                tvtestlat.setText(Double.toString(test_lat));
                tvtestlongi.setText(Double.toString(test_longi));
            } else {
                tvtestlat.setText("Not available");
                tvtestlongi.setText("Not available");
            }

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {
            Toast.makeText(MainActivity.this, provider.toUpperCase() + " is ENABLED!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(MainActivity.this, provider.toUpperCase() + " is DISABLED!", Toast.LENGTH_SHORT).show();
            tvtestlongi.setText("Not available");
            tvtestlongi.setText("Not available");
        }
    }
}
