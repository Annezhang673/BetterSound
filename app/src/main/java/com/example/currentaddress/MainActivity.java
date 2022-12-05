package com.example.currentaddress;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    FirebaseFirestore firestore;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    static ProgressBar progressBar;
    static TextView textLatLong, address, postcode, locaity, state, district, country;
    ResultReceiver resultReceiver;
    private MediaRecorder recorder;
    private int i = 0;
    private static int MICROPHONE_PERMISSION_CODE = 200;
    audioThread t1; // This thread will read from Mic
    private Handler mHandler;
    static Vibrator v;
    public static Context main;
    private Boolean checked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultReceiver = new AddressResultReceiver(new Handler());

        main = getApplicationContext();

        progressBar = findViewById(R.id.progress_circular);
        textLatLong = findViewById(R.id.textLatLong);
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        address = findViewById(R.id.textaddress);
        locaity = findViewById(R.id.textlocality);
        postcode = findViewById(R.id.textcode);
        country = findViewById(R.id.textcountry);
        district = findViewById(R.id.textdistrict);
        state = findViewById(R.id.textstate);

        if (isMicrophonePresent()) {
            getMicrophonePermission();
        }

        mHandler = new Handler();

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checked && ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
                    checked = true;
                } else {
                    getCurrentLocation(-1);
                }
            }
        });
    }

    public void listen(View view) {
        if (i == 0) {
            // Start audio Thread
            TextView audio_level = (TextView) findViewById(R.id.audio_level);
            t1 = new audioThread(getApplicationContext(), audio_level, mHandler);
            t1.start();
            i=1;
        }
        else {
            i=0;
            t1.interrupt();
        }
    }

    private boolean isMicrophonePresent() {
        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
    }

    private void getMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.RECORD_AUDIO}, MICROPHONE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation(-1);
            } else {
                Toast.makeText(this, "Permission is denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

//    private void requestPermissions() {
//        ActivityCompat.requestPermissions(this,
//                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//    }

    public void getCurrentLocation(double noise) {
        //progressBar.setVisibility(View.VISIBLE);
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


//        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
        if (!checked && ContextCompat.checkSelfPermission(main,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            checked = true;
        }

        LocationServices.getFusedLocationProviderClient(main)
                .requestLocationUpdates(locationRequest, new LocationCallback() {

                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        LocationServices.getFusedLocationProviderClient(main)
                                .removeLocationUpdates(this);
                        if (locationResult != null && locationResult.getLocations().size() > 0) {

                            int latestlocIndex = locationResult.getLocations().size() - 1;
                            double lati = locationResult.getLocations().get(latestlocIndex).getLatitude();
                            double longi = locationResult.getLocations().get(latestlocIndex).getLongitude();
                            textLatLong.setText(String.format("Latitude : %s\n Longitude: %s", lati, longi));

                            if (noise >= 0) {

                                v.vibrate(400);

                                firestore = FirebaseFirestore.getInstance();
                                Map<String, Object> coordinate = new HashMap<>();
                                coordinate.put("latitude", lati);
                                coordinate.put("longitude", longi);
                                coordinate.put("noise", noise);

                                firestore.collection("test").add(coordinate).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {

                                        Toast.makeText(main, "Success", Toast.LENGTH_LONG).show();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                        Toast.makeText(main, "Fail", Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else {
                                firestore = FirebaseFirestore.getInstance();
                                firestore.collection("test")
//                                        .document("JA74xzagNxEnJfsEwZG4")
//                                        .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//                                            @Override
//                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                                                if (task.isSuccessful()) {
//                                                    DocumentSnapshot doc = task.getResult();
//                                                    if (doc.exists()) {
//                                                        double x = (double) doc.get("longitude") + 3.345;
//                                                        Log.d("hello", "The value is: " + x);
//                                                    } else {
//                                                        Log.d("Document", "No data");
//                                                    }
//                                                }
//                                            }
//                                        });
                                        .whereGreaterThanOrEqualTo("longitude", longi-10)
                                        .whereLessThanOrEqualTo("longitude", longi+10)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()) {

                                                    Log.d("TAG", "TESTPOINT A");
                                                    if (task.getResult().size() == 0) {
                                                        Log.d("TAG", "TESTPOINT B");
                                                        Toast.makeText(main, "Area does NOT have history of noise.", Toast.LENGTH_LONG).show();
                                                    }
                                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                                        double la = (double) document.get("latitude");
                                                        if (la >= lati-10 && la <= lati+10) {
                                                            v.vibrate(400);
                                                            Toast.makeText(main, "Avoid This Area!! Associated with high noise levels.", Toast.LENGTH_LONG).show();
                                                            break;
                                                        }
                                                        else {
                                                            Log.d("TAG", "TESTPOINT C");
                                                            Toast.makeText(main, "Area does not have history of noise.", Toast.LENGTH_SHORT).show();
                                                        }
//                                                        Log.d("hello", " => " + document.getData());
                                                    }
                                                } else {
                                                    Log.d("Document", "Error getting documents: ", task.getException());
                                                }
                                            }
                                        });
                            }
                            
                            Location location = new Location("providerNA");
                            location.setLongitude(longi);
                            location.setLatitude(lati);
                            //fetchaddressfromlocation(location);

                            //progressBar.setVisibility(View.GONE);
                        } else {
                            //progressBar.setVisibility(View.GONE);
                        }
                    }
                }, Looper.getMainLooper());
    }

    private class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            if (resultCode == Constants.SUCCESS_RESULT) {
                address.setText(resultData.getString(Constants.ADDRESS));
                locaity.setText(resultData.getString(Constants.LOCAITY));
                state.setText(resultData.getString(Constants.STATE));
                district.setText(resultData.getString(Constants.DISTRICT));
                country.setText(resultData.getString(Constants.COUNTRY));
                postcode.setText(resultData.getString(Constants.POST_CODE));
            } else {
                Toast.makeText(MainActivity.this, resultData.getString(Constants.RESULT_DATA_KEY), Toast.LENGTH_SHORT).show();
            }
            progressBar.setVisibility(View.GONE);
        }
    }

    private void fetchaddressfromlocation(Location location) {
        Intent intent = new Intent(main, FetchAddressIntentServices.class);
        intent.putExtra(Constants.RECEVIER, resultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, location);
        main.startService(intent);
    }

//
//    public static void updateBackend() {
//        //Log.i("TAG", "Testing");
//        /* Update the backend here */
//    }


}