package com.androidclient.blulock.androidclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String BYTE_MESSAGE = "byte_message" ;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothSocket mBlueSocket;
    //private ConnectThread mConnectedThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"First stuff to do!!!");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Log.d(TAG, "Starting application");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter ==  null )  {
            Toast.makeText(this,"Bluetooth is not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Log.d(TAG, "Bluetooth Adapter detected!!!");
            launchConnectThread();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void launchConnectThread() {
        int i = 0;
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size()> 0 ) {
            for(BluetoothDevice device: pairedDevices) {
                //TODO: Find proper way to select the connected devices or prompt the user to select one
                if ( i== 1) {
                    mBluetoothDevice = device;
                    Log.d(TAG, mBluetoothDevice.getAddress() + " " + mBluetoothDevice.getName());
                }
                i++;
            }
        } else {
            Log.d(TAG, "No paired devices found");
        }
        new ConnectThread(mBluetoothDevice).run();
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        private ConnectThread( BluetoothDevice mmDevice) {
            BluetoothSocket mmSocket1;
            this.mmDevice = mmDevice;
            try {
//                Log.d(TAG, "connect Thread " + mmDevice.getName());
//                Log.d(TAG, ""+mmDevice.fetchUuidsWithSdp());
//                Log.d(TAG, ""+mmDevice.getUuids().length);
//                Log.d(TAG, ""+mmDevice.getBondState());
//                Log.d(TAG, ""+mmDevice.describeContents());
//                Log.d(TAG, "Class:"+mmDevice.getBluetoothClass());
//                ParcelUuid uuidParcel[] = mmDevice.getUuids();
//                for( ParcelUuid pUuid:mmDevice.getUuids()) {
//                    Log.d(TAG, "UUid:" + pUuid.getUuid().toString());
//                }
               mmSocket1 =  mmDevice.createInsecureRfcommSocketToServiceRecord(java.util.UUID.fromString(MY_UUID));

            } catch( IOException e) {
                mmSocket1 = null;
            }

            mmSocket = mmSocket1;
        }

        public void run() {
            try {
                mmSocket.connect();
            } catch( IOException connectException ) {
                Log.d(TAG, "connectException");
                Log.d(TAG, connectException.toString());
                try {
                    mmSocket.close();
                } catch( IOException closeException ) {
                    Log.d(TAG,"closeException");
                }

                return;
            }
            manageConnectedSocket(mmSocket);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private void manageConnectedSocket(BluetoothSocket mmSocket ) {
        ///String byteRes = "Hello from Android";
        mBlueSocket = mmSocket;
        //new ConnectedThreads(mBlueSocket).write(byteRes.getBytes());
    }

    private void sendMessage(String msg) {
        new ConnectedThreads(mBlueSocket).write(msg.getBytes());
    }

    private class ConnectedThreads extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        private ConnectedThreads(BluetoothSocket mmSocket) {
            this.mmSocket = mmSocket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                Log.d(TAG,"IOException ConnectedThreads!!!");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes = 0;
            while(true) {
                try {
                    bytes = mmInStream.read(buffer);
                    Message msg = mHandler.obtainMessage(MESSAGE_READ, bytes);
                    Bundle bundle = new Bundle();
                    byte[] newbuffer = new byte[bytes];
                    for ( int i=0; i<bytes; i++ ) {
                        newbuffer[i] = buffer[i];
                    }
                    String str = new String(newbuffer, "US-ASCII");
                    bundle.putString(BYTE_MESSAGE, str);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                    //comment break this if continuous data is wanted
                    break;
                } catch (IOException e) {
                    Log.d(TAG,"run() ConnectedThreads");
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.d(TAG,"write ConnectedThreads");
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    public void sendBluetoothMessage(View v) {
        if( R.id.sleep == v.getId() ) {
            sendMessage("sleep");
        } else if ( R.id.shutdown == v.getId() ) {
            sendMessage("shutdown");
        } else if ( R.id.getfiles == v.getId() ) {
            sendMessage("getfiles");
            new ConnectedThreads(mBlueSocket).run();
        }
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "Message Received");
            switch (msg.what) {
                case MESSAGE_READ:
                    int arg1 = msg.arg1;
                    Bundle bundle = msg.getData();
                    String str = bundle.getString(BYTE_MESSAGE, "Niroj Rocks");
                    //TODO: Launch Activity with list view to append all the files received from the user and let them select file to download

                    Log.d(TAG, str);
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if( requestCode == REQUEST_ENABLE_BT ) {
            if( RESULT_OK == resultCode ) {
                //Start searching for the paired devices
                launchConnectThread();
            }
        }
    }

    private static final String MY_UUID = "5f6289aa-0acc-11e6-b512-3e1d05defe78";
    private static final String TAG = "BluLock";
    private static final int MESSAGE_READ = 1;
    private static final int REQUEST_ENABLE_BT = 2;

}
