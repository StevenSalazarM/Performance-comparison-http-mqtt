package com.example.u__ca.examplegooglemap;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;


import java.util.List;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    // attributi necessari per usare GoogleAPI
    // parte di codice generata in automatico
    // da AndroidStudio
    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    // questa variabile è quella più interessante per il nostro caso
    // permette di ricevere la posizione del dispositivo ogni tot secondi
    LocationRequest mLocationRequest;
    FusedLocationProviderClient mFusedLocationClient;

    Location mLastLocation;

    // riferimento ad un client in modo da inviare messaggi
    // quando l'oggetto viene creato, questo in automatico
    // inizia a ricevere i messaggi dal Server
    MyClient client;
    // Quando l'applicazione viene minimizzata è inutile continuare
    // a essere notificati della posizione, per  cui firstTime
    // ci permette di non ricevere più questo messaggio che contiene la posizione
    boolean firstTime=false;
    // quando si riprende l'app è utile poter ricevere i messaggi nuovamente
    boolean removedLocationUpdated=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Bundle extras = getIntent().getExtras();
        if(extras !=null) {
            // riprendo i messaggi inviati dal MainActivity
            String which = extras.getString("protocol");
            String client_id = extras.getString("client_id");
            String host = extras.getString("host");
            int port = extras.getInt("port");

            if(which.equals("HTTP")){
                client = new MyHttpClient(client_id, host, port, mGoogleMap);
            }else if (which.equals("MQTT")){
                client = new MyMqttClient(client_id, host, port, mGoogleMap);
            }

        }
        getSupportActionBar().setTitle("Map Location Activity");

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);
        // quando l'applicazione entra in funzione è utile che lo schermo
        // rimanga accesso anche quando non lo si tocca
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    @Override
    protected void onDestroy() {
        try {
            // quando questo activity viene chiuso
            // la connessione deve essere pulita
            client.cleanConnection();
        }catch(Exception e){
            Log.d("EXCEPTION DISCONNECTING", e.getMessage());

        }
        // e si aggiorna l'info sulla batteria
        MainActivity.main.updateCurrentBatteryInfo();
        super.onDestroy();
        }

    @Override
    public void onPause() {
        super.onPause();

        // non vogliamo ricevere più aggiornamenti sulla posizione
        // quando l'activity viene messo in pausa
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            removedLocationUpdated=true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // dopo che l'app è stata messa in pausa
        // vogliamo ricontinuare a ricevere aggiornamenti
        // sulla posizione
        if(removedLocationUpdated && firstTime){
            removedLocationUpdated=false;
            // è bello chiedere le autorizzazioni durante l'utilizzo dell'app
            // se non ce l'hanno già date prima
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    //Location Permission already granted
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    mGoogleMap.setMyLocationEnabled(true);
                } else {
                    //Request Location Permission
                    checkLocationPermission();
                }
            }
            else {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mGoogleMap.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        client.setGoogleMap(mGoogleMap);
        mLocationRequest = new LocationRequest();
        // si richiede la posizione ogni 2 secondi
        mLocationRequest.setInterval(2000);
        // in alcuni casi il sistema operativi potrebbe darci la posizione
        // più frequentemente se altre app la richiedono
        // per cui è necessario impostare un massimo di frequenza
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mGoogleMap.getUiSettings().setMapToolbarEnabled(false);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mGoogleMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        }
        else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mGoogleMap.setMyLocationEnabled(true);
        }
    }

    LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                // Quando la posizione è pronta per essere utilizzata la si prende
                // Si trova nell'ultima posizione disponibile
                Location location = locationList.get(locationList.size() - 1);
                Log.d("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;

                // si crea un oggetto latLng in modo da permettere di muovere la vista dello schermo
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                // si prepara la stringa json da inviare
                String string_json = "{ \"client_id\": \""+client.getClientId()+"\", \"latitude\": "+latLng.latitude+", \"longitude\": "+latLng.longitude +" }";
                client.sendPosition(string_json);
                if(!firstTime) {
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
                    firstTime=true;
                }
            }
        }
    };

    // codice di autorizzazione per la localizzazione
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    // metodo che permette di verificare se un permesso è stato dato
    // se non è così lo si richiede
    private void checkLocationPermission() {
        // preso da stack overflow
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mGoogleMap.setMyLocationEnabled(true);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}