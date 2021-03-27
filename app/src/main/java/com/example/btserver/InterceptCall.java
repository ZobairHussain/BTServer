package com.example.btserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class InterceptCall extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            showPopup(state, context);
            if(MainActivity.CONNECTING_STATE == MainActivity.CONNECTING_STATE_CONNECTED){
                sendDataToClient(state);
            }

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void showPopup(String state, Context context){
        if(state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING)) {
            MainActivity.CALL_STATE = MainActivity.CALL_STATE_RINGING;
            Toast.makeText(context, "RINGING!", Toast.LENGTH_SHORT).show();
        }
        if(state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
            MainActivity.CALL_STATE = MainActivity.CALL_STATE_RECEIVED;
            Toast.makeText(context, "RECEIVED!", Toast.LENGTH_SHORT).show();
        }
        if(state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE)) {
            MainActivity.CALL_STATE = MainActivity.CALL_STATE_IDLE;
            Toast.makeText(context, "IDLE!", Toast.LENGTH_SHORT).show();
        }
    }

    public void sendDataToClient(String state){
        if(state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING)) {
            MainActivity.sendReceive.setWrite("RINGING");
        }
        if(state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
            MainActivity.sendReceive.setWrite("RECEIVED");
        }
        if(state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE)) {
            MainActivity.sendReceive.setWrite("IDLE");
        }
    }
}