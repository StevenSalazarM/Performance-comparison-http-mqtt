package com.example.u__ca.examplegooglemap;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    // in alcuni casi è utile avere un riferimento al main activity
    // in questo caso l'activity deve modificare l'informazione
    // della batteria quando la mappa viene chiusa
    public static MainActivity main;

    Button btnStartMap;
    RadioGroup radio_group;
    String client;
    String host;
    int port;

    // Questo RelativeLayout verrà usato per mostrare
    // l'informazione sulla batteria (cioè dopo il click su Star Map)
    RelativeLayout layoutInfo;

    // Metodo usato per visualizzare la percentuale della batteria
    public static int getBatteryPercentage(Context context) {
        // preso da StackOverFlow
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

        float batteryPct = level / (float) scale;

        return (int) (batteryPct * 100);
    }

    // Metodo usato per mostrare il tempo corrente
    private static String getReminingTime() {
        // preso da StackOverFlow
        String delegate = "hh.mm.ss aaa";
        return (String) DateFormat.format(delegate,Calendar.getInstance().getTime());
    }

    // Questo metodo viene chiamato da MapsActivity in modo
    // da aggiornare lo stato della batteria e il tempo in cui
    // la Mappa è stata chiusa
    public void updateCurrentBatteryInfo(){
        TextView batteryLevel = (TextView) findViewById(R.id.BatteryLevelView2);
        TextView battertTime = (TextView) findViewById(R.id.BatteryTimeView2);

        batteryLevel.setText("Battery Level: "+getBatteryPercentage(MainActivity.this)+"%");

        battertTime.setText("Time:   "+getReminingTime());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnStartMap = (Button) findViewById(R.id.btnStartMap);
        radio_group = (RadioGroup) findViewById(R.id.radioGroup);
        prepareForMapActivity();
    }

    // questo metodo prepara l'activity per la Mappa
    // prende l'input del utente e assegna le variabili
    public void prepareForMapActivity(){

        btnStartMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // begin reading input
                String protocol;
                int selectedId = radio_group.getCheckedRadioButtonId();

                if(selectedId == R.id.radioMQTT){
                    protocol = "MQTT";
                }else{
                    protocol = "HTTP";
                }

                String l_client = ((EditText)findViewById(R.id.clientId)).getText().toString().trim();
                if(!l_client.equals(""))
                    client= l_client;
                else {
                    Toast.makeText(MainActivity.this, "Please insert a valid client id", Toast.LENGTH_LONG).show();
                    return;
                }

                String l_host  = ((EditText)findViewById(R.id.serverId)).getText().toString().trim();
                if(!l_host.equals(""))
                    host = l_host;
                else{
                    Toast.makeText(MainActivity.this, "Please insert a valid host", Toast.LENGTH_LONG).show();
                    return;
                }

                String l_port = ((EditText)findViewById(R.id.port)).getText().toString().trim();
                if(!l_port.equals(""))
                    port = Integer.valueOf(l_port);
                else{
                    Toast.makeText(MainActivity.this, "Please insert a valid port", Toast.LENGTH_LONG).show();
                    return;
                }
                // end reading input

                Intent i = new Intent(MainActivity.this, MapsActivity.class);
                i.putExtra("protocol", protocol);
                i.putExtra("client_id",client);
                i.putExtra("host", host);
                i.putExtra("port",port);

                // we want to show information about Battery
                layoutInfo = (RelativeLayout) findViewById(R.id.RelativeInfoView);
                layoutInfo.setVisibility(View.VISIBLE);
                TextView batteryLevel = (TextView) findViewById(R.id.BatteryLevelView);
                TextView battertTime = (TextView) findViewById(R.id.BatteryTimeView);

                batteryLevel.setText("Battery Level: "+getBatteryPercentage(MainActivity.this)+"%");
                battertTime.setText("Time:   "+getReminingTime());


                startActivity(i);
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        main=this;
    }
}
