package com.example.u__ca.examplegooglemap;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class MyHttpClient extends MyClient {

    private Timer timer;
    private TimerTask task;

    public MyHttpClient(final String client_id, final String host, final int port, GoogleMap mGoogleMap) {
        super(client_id, host, port, mGoogleMap);

        // Una volta creato il Client, bisogna fare il polling al Server in modo da
        // richiedere la posizione degli altri Client.
        final Handler handler = new Handler();
         timer = new Timer();
         task = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        new RequestPositionsTask(MyHttpClient.this).execute("http://"+MyHttpClient.this.host+":"+MyHttpClient.this.port+"/get-all-position");
                    }
                });
            }
        };
         // si richiede la posizione ogni 1000 ms
        timer.schedule(task, 0, 1000);
    }


    // Una volta che la si sta per chiudere l'applicazione
    // bisogna fermare il polling
    @Override
    public void cleanConnection() throws Exception {
        if(timer!=null){
            if(task!=null)
                task.cancel();
            timer.cancel();
        }
    }

    // Nello stesso modo della ricezione della posizione degli altri client
    // quando si invia una richiesta HTTP si utilizza un Asyntask
    @Override
    public void sendPosition(String position) {
        Log.d("*** FROM HTTP**", "SENDING POSITION TO "+"http://"+host+":"+port+"/clients/positions");
        new SendPositionTask().execute("http://"+host+":"+port+"/clients/positions",position);
    }




    // Dal momento che non si deve bloccare l'interfaccia grafica
    // mandando richieste HTTP, è obbligatorio usare una delle alternative
    // in questo caso la ricezione delle posizioni viene fatta tramite un AsyncTask
    private static class RequestPositionsTask extends  AsyncTask<String, Void, String>{

        // Una volta ricevuto il messaggio dal Server bisogna mostrali sulla mappa
        // per cui, è necessario un riferimento al Client
        private WeakReference<MyHttpClient> activity_reference;


        RequestPositionsTask(MyHttpClient client){
            activity_reference = new WeakReference<>(client);
        }

        private String requestHttp(String url_to_download){
            String result="";
            HttpURLConnection httpURLConnection = null;
            try {
                // si crea l'url a cui si connetterà
                URL url = new URL(url_to_download);
                // si crea la connessione
                httpURLConnection = (HttpURLConnection) url.openConnection();
                // si abilita lo streaming http senza buffering quando la lunghezza del messaggio non è nota
                // 0 in modo da usare un valore di default
                httpURLConnection.setChunkedStreamingMode(0);
                httpURLConnection.setReadTimeout(10000);
                httpURLConnection.setConnectTimeout(15000);
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setDoInput(true);
                InputStream in = httpURLConnection.getInputStream();
                // si legge l'input ricevuto dal Server (la stringa json con le posizioni)
                result = readStream(in);
            }catch(MalformedURLException mal){
                Log.d("*** FROM HTTP**", "MAL FORMED" + mal.getMessage());
            }catch(IOException io){
                Log.d("*** FROM HTTP**", "IO-" + io.getMessage());
            }
            finally{
                if(httpURLConnection!=null)
                    httpURLConnection.disconnect();
            }
                Log.d("*** FROM HTTP**", "RECIVED-GET:"+ result);
            return result;
        }
        @Override
        protected String doInBackground(String... strings) {
            return requestHttp(strings[0]);
        }
        @Override
        protected void onPostExecute(final String s) {
            // una volta che la risposta è già pronta
            // bisogna aggiornare la mappa
            MyHttpClient client = activity_reference.get();
            JSONArray json;
            try{
                json = new JSONArray(s);
                for (int i = 0; i < json.length(); i++) {
                    JSONObject json_from_get = new JSONObject(json.getString(i));
                    String id = json_from_get.getString("id");
                    // ci interessa di far visualizzare solo la posizione degli altri
                    if(!id.equals(client.client_id)){
                        JSONObject json_lat_long = new JSONObject(json_from_get.getString("name"));
                        Log.d("** HTTP RECEIVE *** ", " JSON OBJECT FOR ELEMENT "+ i+ " is :" +json_lat_long.toString() );
                        // bisogna modificare leggeramente la posizione dato che l'icona dei marker non è centrata
                        // ha come riferimento la parte sinistra superiore
                        LatLng latLng = new LatLng(json_lat_long.getDouble("latitude")-0.00000534, json_lat_long.getDouble("longitude")+0.0000008);
                        // aggiorno il client con id 'id' e la posizione 'latLng'
                        client.updateMap(id, latLng);
                    }
                }
             }catch(JSONException mal){
                Log.d("*** ASYNC RECEIVE", "JSON EXCEPTION "+mal.getMessage());
            }
            client=null;
            super.onPostExecute(s);
        }

        // metodo utilizzato per leggere la risposta di un server HTTP
        private String readStream(InputStream in) {

            BufferedReader reader = null;
            StringBuffer response = new StringBuffer();
            InputStreamReader input2 = null;
            try {
                input2 = new InputStreamReader(in);
                reader = new BufferedReader(input2);
                String line = "";
                // si legge riga per riga e le inseriamo nello StringBuffer
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                        input2.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            // si ritorna la riposta pronta per essere utilizzata
            return response.toString();
        }

    }


    // Analogamente a come abbiamo fatto per ricevere messaggi
    // bisogna fare lo stesso per l'invio dei dati (tutte e due sono richieste HTTP)
    private static class SendPositionTask extends AsyncTask<String, Void, Void> {

        // @param strings:  string[0] contiene l'URL a cui fare le richieste HTTP
        //                  string[1] contiene il messaggio che si vuole trasmettere
        @Override
        protected Void doInBackground(String... strings) {
            HttpURLConnection urlConnection = null;
            try {
                OutputStream out = null;
                URL url = new URL(strings[0]);
                urlConnection= (HttpURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                // in questo caso è desiderabile avere un minimo di sicurezza non mostrando direttamente
                // ciò che si trasmette
                urlConnection.setRequestMethod("POST");
                // è utile sapere anche che cosa ha risposto il server, ovvero il codice di risposta
                urlConnection.setDoInput(true);

                out = new BufferedOutputStream(urlConnection.getOutputStream());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                // invio del messaggio (stringa json che contiene la posizione) da trasmettere
                writer.write("json="+strings[1]);
                writer.flush();
                writer.close();
                out.close();
                urlConnection.connect();

                Log.d("*** FROM TASK HTTP POST" ,"SENT WITH RESPONSE**"+ urlConnection.getResponseCode());
            }catch (MalformedURLException mal){
                Log.d("*** FROM HTTP CLIENT**", "MalFormed "+mal.getMessage() );
            }catch (IOException io){
                Log.d("*** FROM HTTP CLIENT**",  "IO_ " +io.getMessage()  );
            }finally{

                if(urlConnection != null)
                    urlConnection.disconnect();
            }

            return null;
        }
    }
}
