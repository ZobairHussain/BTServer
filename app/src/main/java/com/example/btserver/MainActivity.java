package com.example.btserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    Button send;
    TextView msg_box, connectingStatus;
    EditText writeMsg;

    BluetoothAdapter bluetoothAdapter;

    ServerClass serverClass;
    BluetoothServerSocket serverSocket;
    String DISCONNECTING_REQUEST = "-1";

    static SendReceive sendReceive;

    static final int CONNECTING_STATE_LISTENING = 1;
    static final int CONNECTING_STATE_CONNECTING =2;
    static final int CONNECTING_STATE_CONNECTED =3;
    static final int CONNECTING_STATE_CONNECTION_FAILED =4;
    static final int CONNECTING_STATE_MESSAGE_RECEIVED =5;
    static int CONNECTING_STATE = -1;

    static final int CALL_STATE_RINGING = 1;
    static final int CALL_STATE_RECEIVED = 2;
    static final int CALL_STATE_IDLE = 3;
    static int CALL_STATE = -1;


    int REQUEST_ENABLE_BLUETOOTH=1;

    private static final String APP_NAME = "BTChat";
    private static final UUID MY_UUID=UUID.fromString("8ce255c0-223a-11e0-ac64-0803450c9a66");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewByIdes();

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
        }

        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();

        if(!bluetoothAdapter.isEnabled())
        {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BLUETOOTH);
        }

        serverClass=new ServerClass();
        serverClass.start();
        implementListeners();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this, "READ_PHONE_STATE granted", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(this, "READ_PHONE_STATE permission not granted", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
        }
    }

    private void implementListeners() {
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String string= String.valueOf(writeMsg.getText());
                sendReceive.write(string.getBytes());
            }
        });
    }

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what)
            {
                case CONNECTING_STATE_LISTENING:
                    connectingStatus.setText("Listening");
                    CONNECTING_STATE = CONNECTING_STATE_LISTENING;
                    break;
                case CONNECTING_STATE_CONNECTING:
                    connectingStatus.setText("Connecting");
                    CONNECTING_STATE = CONNECTING_STATE_CONNECTING;
                    break;
                case CONNECTING_STATE_CONNECTED:
                    connectingStatus.setText("Connected");
                    CONNECTING_STATE = CONNECTING_STATE_CONNECTED;
                    if(CALL_STATE == CALL_STATE_RINGING){
                        sendReceive.write("RINGING".getBytes());
                    }
                    else if(CALL_STATE == CALL_STATE_RECEIVED){
                        sendReceive.write("RECEIVED".getBytes());
                    }
                    break;
                case CONNECTING_STATE_CONNECTION_FAILED:
                    connectingStatus.setText("Connection Failed");
                    CONNECTING_STATE = CONNECTING_STATE_CONNECTION_FAILED;
                    break;
                case CONNECTING_STATE_MESSAGE_RECEIVED:
                    byte[] readBuff= (byte[]) msg.obj;
                    String tempMsg=new String(readBuff,0,msg.arg1);
                    if(tempMsg.equals(DISCONNECTING_REQUEST)){
                        serverClass=new ServerClass();
                        serverClass.start();
                    }
                    else{
                        msg_box.setText(tempMsg);
                    }
                    break;
            }
            return true;
        }
    });

    private void findViewByIdes() {
        connectingStatus=(TextView) findViewById(R.id.status);

        writeMsg=(EditText) findViewById(R.id.writemsg);
        send=(Button) findViewById(R.id.send);
        msg_box =(TextView) findViewById(R.id.msg);
    }

    private class ServerClass extends Thread
    {
        public ServerClass(){
            try {
                serverSocket=bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run()
        {
            BluetoothSocket socket=null;

            while (socket==null)
            {
                try {
                    Message message=Message.obtain();
                    message.what= CONNECTING_STATE_CONNECTING;
                    handler.sendMessage(message);

                    socket=serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message=Message.obtain();
                    message.what= CONNECTING_STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                if(socket!=null)
                {
                    Message message=Message.obtain();
                    message.what= CONNECTING_STATE_CONNECTED;
                    handler.sendMessage(message);

                    sendReceive=new SendReceive(socket);
                    sendReceive.start();
                    break;
                }
            }
        }
    }

    public class SendReceive extends Thread
    {
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive (BluetoothSocket socket)
        {
            InputStream tempIn=null;
            OutputStream tempOut=null;

            try {
                tempIn= socket.getInputStream();
                tempOut= socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream=tempIn;
            outputStream=tempOut;
        }

        public void run()
        {
            byte[] buffer=new byte[1024];

            while (true)
            {
                try {
                    int bytes = inputStream.read(buffer);
                    handler.obtainMessage(CONNECTING_STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes)
        {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void setWrite(String s) {
            write(s.getBytes());
        }
    }
}