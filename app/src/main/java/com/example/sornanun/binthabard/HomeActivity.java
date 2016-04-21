package com.example.sornanun.binthabard;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.parse.ParseUser;
import com.skyfishjy.library.RippleBackground;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;

public class HomeActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    Button logout;
    RippleBackground rippleBackground;
    RelativeLayout positionDetail;
    TextView myLat;
    TextView myLong;
    TextView myAddress;
    TextView myTime;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Location mCurrentLocation;
    String mLastUpdateTime;
    LocationRequest mLocationRequest;
    boolean switchLocation = false;

    ParseUser currentUser;
    ParseController parseController;

    private String latitude;
    private String longitude;
    private String address;

    boolean GPSenabled;
    boolean NetworkEnabled;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);

        currentUser = ParseUser.getCurrentUser();
        parseController = new ParseController();
        String struser = currentUser.getUsername().toString();
        TextView txtuser = (TextView) findViewById(R.id.txtuser);
        txtuser.setText("ยินดีต้อนรับ " + struser);
        logout = (Button) findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                ParseUser.logOut();
                finish();
            }
        });

        // Create an instance of GoogleAPIClient.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
        myLat = (TextView) findViewById(R.id.txtlatitude);
        myLong = (TextView) findViewById(R.id.txtlongitude);
        myAddress = (TextView) findViewById(R.id.txtaddress);
        myTime = (TextView) findViewById(R.id.txttime);
        positionDetail = (RelativeLayout) findViewById(R.id.positionDetail);

        rippleBackground = (RippleBackground) findViewById(R.id.content);
        ImageView imageView = (ImageView) findViewById(R.id.centerImage);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (switchLocation == false) {
                    isNetworkAvailable(getApplicationContext());
                    if (NetworkEnabled) {
                        isGPSEnable();
                        if (GPSenabled && NetworkEnabled) {
                            startLocationUpdates();
                            rippleBackground.startRippleAnimation();
                            switchLocation = true;
                        }
                    }
                } else {
                    stopLocationUpdates();
                    rippleBackground.stopRippleAnimation();
                    switchLocation = false;
                    removeLocationFromParse();
                }
            }
        });

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    public void isGPSEnable() {
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        GPSenabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!GPSenabled) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final String message = "GPS ไม่ได้เปิดอยู่ กรุณากดตกลง แล้วทำการเปิด GPS";

            builder.setMessage(message)
                    .setPositiveButton("ตกลง",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface d, int id) {
                                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                    startActivity(intent);
                                    d.dismiss();
                                }
                            })
                    .setNegativeButton("ยกเลิก",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface d, int id) {
                                    d.cancel();
                                    Toast.makeText(getApplicationContext(),
                                            "ไม่สามารถดึงตำแหน่งปัจจุบันของคุณได้ กรุณาเปิด GPS",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
            builder.create().show();
        }
        GPSenabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void isNetworkAvailable(final Context context) {
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        NetworkEnabled = connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();

        if (!NetworkEnabled) {
            new AlertDialog.Builder(this)
                    .setTitle("ไม่มีการเชื่อมต่ออินเทอร์เน็ต")
                    .setMessage("อินเทอร์เน็ตไม่ได้เปิดอยู่ กรุณาเชื่อมต่ออินเทอร์เน็ตผ่าน Wifi หรือ 3G หรือ 4G ก่อนทำรายการอีกครั้ง")
                    .setCancelable(false)
                    .setPositiveButton("เข้าใจ", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).create().show();
        }
        NetworkEnabled = connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private String getAddress(double lat, double longitude) {
        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(
                    "http://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + "," + longitude + "&sensor=true&language=th");
            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonResults.toString();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
//        if (mLastLocation != null) {
//            myLat.setText("ลติจูด " + String.valueOf(mLastLocation.getLatitude()));
//            myLong.setText("ลองจิจูด " + String.valueOf(mLastLocation.getLongitude()));
//            myTime.setText("อัพเดทเมื่อ " + DateFormat.getTimeInstance().format(new Date()).toString());
//        } else {
//            myLat.setText("ลติจูด ไม่พบ");
//            myLong.setText("ลองจิจูด ไม่พบ");
//            myAddress.setText("สถานที่ ไม่พบ");
//            myTime.setText("อัพเดทเมื่อ" + DateFormat.getTimeInstance().format(new Date()).toString());
//        }
        if (switchLocation == true) {
            startLocationUpdates();
        }
    }

    protected void startLocationUpdates() {
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        setLocation();
        updateUI();
    }

    private void setLocation() {
        latitude = String.valueOf(mCurrentLocation.getLatitude());
        longitude = String.valueOf(mCurrentLocation.getLongitude());
        try
        {
            JSONObject jsonObject = new JSONObject(getAddress(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude()));
            if (jsonObject.getString("status").equals("OK"))
            {
                JSONArray addressArray = jsonObject.getJSONArray("results");
                JSONObject address = addressArray.getJSONObject(0);
                String addressComponents = address.getString("formatted_address");
                this.address = addressComponents;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void updateUI() {
        myLat.setText("ละติจูด " + String.valueOf(mCurrentLocation.getLatitude()));
        myLong.setText("ลองจิจูด " + String.valueOf(mCurrentLocation.getLongitude()));
        myAddress.setText("สถานที่ " + address);
        myTime.setText("อัพเดทเมื่อ " + mLastUpdateTime);

        saveOrUpdateLocationToParse();
    }

    private void saveOrUpdateLocationToParse() {
        // Save Location
        if (parseController.objectId == null || parseController.objectId.equals("")) {
            parseController.saveLocation(latitude, longitude, address);
        }
        // Update Location
        else {
            parseController.updateLocation(latitude, longitude, address);
        }
    }

    private void removeLocationFromParse() {
        parseController.removeLocation();
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    protected void onPause() {
        super.onPause();
//        stopLocationUpdates();
    }

    @Override
    public void onResume() {
        super.onResume();
//        if (mGoogleApiClient.isConnected() && !switchLocation) {
//            startLocationUpdates();
//        }
    }
}
