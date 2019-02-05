package com.example.u__ca.examplegooglemap;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;

public abstract class MyClient {

    // Variabili utilizzate per connettersi al server
    protected int port;
    protected String client_id;
    protected String host;

    // Un riferimento per interagire con GoogleMap API
    private GoogleMap mGoogleMap;

    // Gli altri client verrano mostrati come markers
    private HashMap<String, Marker> clients;


    MyClient(final String client_id, final String host, final int port, GoogleMap mGoogleMap){
        this.clients = new HashMap<>();
        this.host=host;
        this.client_id=client_id;
        this.port=port;
        this.mGoogleMap=mGoogleMap;
    }

    // Dal momento che mostrare un client sulla mappa è una cosa che entrambi i client (http e mqtt)
    // faranno, torna comodo inserire il metodo qui
    public void updateMap(String r_client_id, LatLng latLng) {
        if(clients.containsKey(r_client_id) && clients.get(r_client_id)!=null) {
            clients.get(r_client_id).remove();
        }
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(r_client_id + " Position");
        markerOptions.icon(BitmapDescriptorFactory.fromAsset("m3.png"));
        Marker m = mGoogleMap.addMarker(markerOptions);
        clients.put(r_client_id,m);
    }

    public void setGoogleMap(GoogleMap mGoogleMap){
        this.mGoogleMap=mGoogleMap;
    }

    public int getPort() {
        return port;
    }

    public String getClientId() {
        return client_id;
    }

    public String getHost() {
        return host;
    }

    // Tutti i client devono pulire la connessione
    abstract public void cleanConnection()throws Exception;

    // Tutti i client devono inviare la propria posizione
    abstract public void sendPosition(String position);

}
