package us.forkloop.walktimer;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

    private final static long DISTANCE = 50;
    private final static int TOTAL_ADJUST = 10;
    private final static double THRESHOLD = 1.0;
    private static boolean isUpdate = true;

    private TextView distanceView;
    private EditText editText;
    private Chronometer chronometer;
    private double aimingDistance;
    private Location previousLocation;
    private int adjustCount;
    private Location startLocation;
    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = (EditText) findViewById(R.id.distance);
        distanceView = (TextView) findViewById(R.id.walk_distance);
        chronometer = (Chronometer) findViewById(R.id.chronometer);

        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        locationListener = new XLocationListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    protected void onPause() {
        locationManager.removeUpdates(locationListener);
        distanceView.setText(getString(R.string.initial_distance));
        super.onPause();
    }

    public void onToggleClicked(View view) {
        boolean isOn = ((ToggleButton) view).isChecked();
        if (isOn) {
            adjustCount = 0;
            isUpdate = true;
            try {
                aimingDistance = Double.parseDouble(editText.getText().toString());
                if (aimingDistance < 0) {
                    Toast.makeText(getApplicationContext(), "Distance must be positive!", Toast.LENGTH_SHORT).show();
                    ((ToggleButton) view).setChecked(false);
                    Log.i(this.getClass().getSimpleName(), "Invalid distance: " + aimingDistance);
                    return ;
                }
            } catch (NumberFormatException nfe) {
                Log.w(this.getClass().getSimpleName(), nfe);
                aimingDistance = DISTANCE;
            }
            Log.i(this.getClass().getSimpleName(), "Start to timing distance: " + aimingDistance);
            distanceView.setText("Waiting for GPS...");
            previousLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (previousLocation != null) {
                Log.i(this.getClass().getSimpleName(), "latitude: " + previousLocation.getLatitude() + ", longitude: " + previousLocation.getLongitude());
            } else {
                previousLocation = new Location(LocationManager.GPS_PROVIDER);
            }

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            locationManager.removeUpdates(locationListener);
            distanceView.setText(getString(R.string.initial_distance));
        }
    }

    private class XLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            Log.i(this.getClass().getSimpleName(), "latitude: " + location.getLatitude() + ", longitude: " + location.getLongitude());
            if (isUpdate) {
                double distance = location.distanceTo(previousLocation);
                Log.i(this.getClass().getSimpleName(), "adjust distance: " + distance);
                if (distance < THRESHOLD) {
                    adjustCount++;
                } else {
                    adjustCount = 0;
                }
                Log.i(this.getClass().getSimpleName(), "adjustCount: " + adjustCount);
                if (adjustCount == TOTAL_ADJUST) {
                    isUpdate = false;
                    startLocation = location;
                    Log.i(this.getClass().getSimpleName(), "Start at: " + startLocation.getLatitude() + " : " + startLocation.getLongitude());
                    distanceView.setText("Starting...");
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    chronometer.start();
                }
                previousLocation = location;
            } else {
                double distance = location.distanceTo(startLocation);
                distanceView.setText(String.format("%.1f m", distance));
                if (distance >= aimingDistance) {
                    chronometer.stop();
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    locationManager.removeUpdates(locationListener);
                }
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.w(this.getClass().getSimpleName(), provider + " is disabled...");
            Toast.makeText(getApplicationContext(), "Please enable " + provider, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.i(this.getClass().getSimpleName(), provider + " status: " + status);
            Log.i(this.getClass().getSimpleName(), "Number of satellites: " + extras.getInt("satellites"));
        }
    }
}