package com.example.u__ca.examplegooglemap;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

public class MyMqttClient extends MyClient {


    // cliente mqttAndroid preso dalla libreria paho-eclipse
    private MqttAndroidClient mqttAndroidClient;
    // topic a cui ci si sottoscrive
    private final String subscriptionTopic;
    // topic in cui si pubblicano i messaggi
    private final String publishTopic;

    Context context;

    public MyMqttClient(final String client_id, final String host, int port, GoogleMap mGoogleMap) {
        super(client_id, host, port, mGoogleMap);
        // quando ci si sottoscrive ad un topic/# si riceve tutti i messaggi
        // sotto topic/
        subscriptionTopic = "test_posizione/#";
        // i messaggi invece vengono pubblicati sotto i propri topic test_posizione/client_id
        publishTopic = "test_posizione/"+client_id;
        // context torna utile per interagire con l'activity e mostrare dei toast
        this.context=MainActivity.main.getBaseContext();

        mqttAndroidClient = new MqttAndroidClient(context, "tcp://" + host + ":" + port, client_id);
        try {
            // per gestire come reagire quando ci si connette è necessario
            // utilizzare una callBack
            mqttAndroidClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    if(reconnect){
                        addToHistory("Reconnected to : " + serverURI);
                        subscribeToTopic();
                    }else{
                        addToHistory("Connected to: " + serverURI);
                    }
                }
                // quando la connessione viene persa si mostra semplicemente un messaggio
                @Override
                public void connectionLost(Throwable cause) {
                    addToHistory("The Connection was lost.");
                    if(cause!=null)
                        Log.d("connection lost", cause.getMessage());
                }



                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {

                    // dal momento che ci si sottoscrive solo ad un topic, il parametro topic è inutile
                    String str_json = new String(message.getPayload());
                    try {
                        JSONObject json = new JSONObject(str_json);
                        String r_client_id = json.getString("client_id");
                        if(r_client_id.trim().equals(""))
                            throw new JSONException("Attribute client_id not found");
                        if(!r_client_id.equals(client_id)){
                            // dato che gli altri client sono dei marker
                            // è necessario modificare leggeramente la posizione
                            // perché il centro del marker parte dalla parte sinistra superiore
                            double lat = json.getDouble("latitude")-0.00000534;
                            double lon = json.getDouble("longitude")+0.0000008;
                            // addToHistory("Updating map for " +r_client_id+ " with lat:" +lat + " and long:" + lon);
                            updateMap(r_client_id, new LatLng(lat,lon));
                        }
                    }catch(JSONException e){
                        addToHistory("received not a valid json :" +str_json);
                    }

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });

            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            // si prova la riconnessione
            mqttConnectOptions.setAutomaticReconnect(true);
            // non si pulisce la sessione una volta chiusa la connessione
            // ovvero siamo interessati ad essere sottoscritti ancora
            // dopo la disconessione
            mqttConnectOptions.setCleanSession(false);
            // ci si prova a connettere
            Toast.makeText(context,"Connecting to "+host, Toast.LENGTH_SHORT).show();
            mqttAndroidClient.connect(mqttConnectOptions, context, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    // dato che l'invio dei messaggi sono tanti non si dovrebbe avere un
                    // buffer persistente
                    disconnectedBufferOptions.setPersistBuffer(false);
                    // non siamo interessati ai messaggi vecchi
                    disconnectedBufferOptions.setDeleteOldestMessages(false);

                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    // ci si sottoscrive al topic
                    subscribeToTopic();
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d("prova2","exception:"+ exception.getMessage());
                    addToHistory("Failed to connect to: " + host);
                }
            });
        } catch (MqttException ex){
            Log.d("Exception:", ex.getMessage());
        }
          catch (Exception e){
            Log.d("Exception2",e.getMessage());
          }
        catch (Error e3){
            Log.d("Exception4", e3.getMessage());
        }
          catch (Throwable e2){
            Log.d("Exception3", e2.getMessage());
          }

    }


    // l'invio della posizione è semplicemente una pubblicazione
    @Override
    public void sendPosition(String position) {
        publishMessage(position);
    }


    public void addToHistory(String message) {
        Log.d("*** from MQTT ***", message);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
    @Override
    public void cleanConnection() throws MqttException {


        mqttAndroidClient.disconnect(context, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d("*** FROM MQTT ***", "SUCCESS ON DISCONNECTING");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
              Log.d("*** FROM MQTT ***", "FAILED ON DISCONNECTING - "+ exception.getMessage());
            }
        });
          }

    public void publishMessage(String publishMessage){
        try {
            // viene instanziato un oggetto MqttMessage in modo da pubblicarlo
            // con semplicità
            MqttMessage message = new MqttMessage();
            // il payload viene caricato (messaggio con la posizione)
            message.setPayload(publishMessage.getBytes());
            // si imposta il flag retained come true
            // dato che potrebbe essere utile che il broker inoltri
            // l'ultima posizione del client anche ai nuovo arrivati
            message.setRetained(true);
            mqttAndroidClient.publish(publishTopic, message);
       } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void subscribeToTopic(){
        try {
            // la qualità del servizio viene scelta 0
            // dato che è inutile essere sicuri della corretta consegna del messaggio
            // dal momento che la frequenza di invio è alta
            mqttAndroidClient.subscribe(subscriptionTopic, 0, context, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    addToHistory("Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    addToHistory("Failed to subscribe");
                }
            });
        } catch (MqttException ex){
            System.err.println("Exception while subscribing");
            ex.printStackTrace();
        }
    }
}
