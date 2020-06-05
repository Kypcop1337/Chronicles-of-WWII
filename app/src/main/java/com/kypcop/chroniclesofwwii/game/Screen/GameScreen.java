package com.kypcop.chroniclesofwwii.game.Screen;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.kypcop.chroniclesofwwii.R;
import com.kypcop.chroniclesofwwii.game.Engine;
import com.kypcop.chroniclesofwwii.game.Logic.Missions.AllMissions;
import com.kypcop.chroniclesofwwii.game.Logic.Missions.Mission;
import com.kypcop.chroniclesofwwii.game.Logic.Player.Player;
import com.kypcop.chroniclesofwwii.game.Network.WiFiNetwork;

import java.net.InetAddress;

public class GameScreen extends Activity {

    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
            (LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    LinearLayout layout1;
    LinearLayout layout2;
    HUD hud;
    Mission mission;
    Player player1;
    Player player2;
    Engine engine;
    TableLayout tableLayout;
    WifiManager wifiManager;
    WiFiNetwork network = Engine.NETWORK;
    private boolean isServer = false;
    private boolean running = true;


    public void goToMainScreen(){
        Intent intent = new Intent(this, LaunchMenuScreen.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        System.gc();
    }

    public void receiveMissionInfo(Mission mission){
        this.mission = mission;
    }


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_game);



        if(getIntent().getExtras() != null) {
            isServer = getIntent().getExtras().getBoolean(WiFiNetwork.SERVER);
        }
        tableLayout = findViewById(R.id.table);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if(wifiManager != null && !wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        if(isServer){
            WifiP2pManager p2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
            if(p2pManager != null){
                WifiP2pManager.Channel channel = p2pManager.initialize(this, getMainLooper(), null);
                p2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(GameScreen.this, "Discover success", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(GameScreen.this, "Discover failure", Toast.LENGTH_SHORT).show();
                    }
                });
            } else{
                Toast.makeText(this, "Упс, видимо ваш телефон не поддерживает Wi-Fi Direct :(", Toast.LENGTH_SHORT).show();
                goToMainScreen();
            }

            int missionId = getIntent().getExtras().getInt("mission");
            mission = AllMissions.missionList.get(missionId);
            network.initializeServer(mission, buildMessageWaitingForConnected());
            Toast.makeText(this, "Server Initialized", Toast.LENGTH_SHORT).show();
            player1 = mission.getPlayer1();
            player2 = mission.getPlayer2();
        } else {
            if (getIntent().getExtras() != null) {
                InetAddress inetAddress = WiFiNetwork.GSON.fromJson
                        (getIntent().getExtras().getString(WiFiNetwork.HOST_ADDRESS), InetAddress.class);
                network.initializeClient(inetAddress);
            } else {
                Toast.makeText(this, "Extras == null", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            synchronized (mission){
                while(mission == null){
                    try {
                        mission.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (mission == null) {
                Toast.makeText(this, "mission == null", Toast.LENGTH_SHORT).show();
                finish();
                return;
            } else {
                player1 = mission.getPlayer2();
                player2 = mission.getPlayer1();
            }
        }
        if(mission == null){
            Toast.makeText(this, "mission == null", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        layout1 = findViewById(R.id.hud1);
        layout2 = findViewById(R.id.hud2);
        hud = new HUD(this, mission, layout1, layout2);
        hud.setButtonParams(params);
        hud.setTextParams(params);
        hud.initializeHud();
        engine = new Engine(this, hud);
        engine.initializeRows(tableLayout);
    }

    private Handler buildMessageWaitingForConnected() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Ждем подключения...").setCancelable(false).
                setNeutralButton("Выйти", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        final AlertDialog dialog = builder.create();
        dialog.show();
        Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                if(msg.what == 1){
                    dialog.cancel();
                }
                return false;
            }
        });
        return handler;
    }

    public void buildAlertMessageEndOfTheGame(boolean win) {
        String result;
        if(win){
            result = "Вы победили!\nЮХУУУУУ!!!";
        } else{
            result = "Вы проиграли!\nАХАХАХ, ЛОХ!!!";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(result).setCancelable(false).
                setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        goToMainScreen();
                    }
                });
        builder.create().show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wifiManager.setWifiEnabled(false);
    }
}
