package com.example.btcontroller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    String TAG = "ControllerApp";
    UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    TextView volumeText;
    Button btnSearch, btnOn, btnOff;
    SeekBar seekBarBlue;
    ListView listView;
    ImageView imageView;
    BluetoothAdapter btAdapter;
    ArrayAdapter<String> btArrayAdapter;
    ArrayList<String> deviceAddressArray;

    private final static int REQUEST_ENABLE_BT = 1;
    BluetoothSocket btSocket = null;
    ConnectedThread connectedThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get permission
        String[] permission_list = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        ActivityCompat.requestPermissions(MainActivity.this, permission_list, 1);

        // Enable bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // variables
        volumeText = (TextView) findViewById(R.id.volumeText);
        btnSearch = (Button) findViewById(R.id.btn_search);
        btnOn = (Button) findViewById(R.id.btn_on);
        btnOff = (Button) findViewById(R.id.btn_off);
        listView = (ListView) findViewById(R.id.listview);
        seekBarBlue = (SeekBar) findViewById(R.id.seekBar_blue);
        imageView = (ImageView) findViewById(R.id.imageView);

        seekBarBlue.setEnabled(false);
        btnOn.setEnabled(false);
        btnOff.setEnabled(false);
//        showImage("1-1p_Dive.png");
        showImage("1_image.png");
        // Show paired devices
        btArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceAddressArray = new ArrayList<>();
        listView.setAdapter(btArrayAdapter);

        listView.setOnItemClickListener((AdapterView.OnItemClickListener) new myOnItemClickListener());

        seekBarBlue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                connectedThread.write((byte)(seekBarBlue.getProgress()+0x50));
                volumeText.setText("Volume:" + seekBarBlue.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        btnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectedThread.write((byte) 0x61); // 'a'
                Toast.makeText(getApplicationContext(), "on!", Toast.LENGTH_SHORT).show();
            }
        });

        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectedThread.write((byte) 0x62); // 'b'
                Toast.makeText(getApplicationContext(), "off!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void showImage(String fileName){
        AssetManager am = getResources().getAssets() ;
        InputStream is = null ;

        try {
            // 애셋 폴더에 저장된 field.png 열기.
            is = am.open(fileName);
//            is = am.open("1-1p_Dive.png") ;
//            is = am.open("2-1p_Burn.png") ;
//            is = am.open("3-1p_Flow.png") ;
            // 입력스트림 is를 통해 field.png 을 Bitmap 객체로 변환.
            Bitmap bm = BitmapFactory.decodeStream(is) ;
            // 만들어진 Bitmap 객체를 이미지뷰에 표시.
            imageView.setImageBitmap(bm) ;
            is.close() ;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (is != null) {
            try {
                is.close() ;
            } catch (Exception e) {
                e.printStackTrace() ;
            }
        }
    }


    @SuppressLint("MissingPermission")
    public void onClickButtonSearch(View view) {
        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        } else {
            if (btAdapter.isEnabled()) {
                btAdapter.startDiscovery();
                btArrayAdapter.clear();
                if (deviceAddressArray != null && !deviceAddressArray.isEmpty()) {
                    deviceAddressArray.clear();
                }
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(receiver, filter);
            } else {
                Toast.makeText(getApplicationContext(), "bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                @SuppressLint("MissingPermission") String deviceName = device.getName();
                if(deviceName != null)
                {
                    if(deviceName.compareTo("PRODUCT1") == 0 || deviceName.compareTo("PRODUCT2") == 0 || deviceName.compareTo("PRODUCT3") == 0)
                    {
                        String deviceHardwareAddress = device.getAddress(); // MAC address
                        if(!deviceAddressArray.contains(deviceHardwareAddress))
                        {
                            deviceAddressArray.add(deviceHardwareAddress);
                            btArrayAdapter.add(deviceName);
                            btArrayAdapter.notifyDataSetChanged();
                        }
                    }
                }

                Log.e("change","change");
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(connectedThread != null){
            if(connectedThread.isOpen()){
                connectedThread.cancel();
                try {
                    btSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        finish();
    }

    public class myOnItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Toast.makeText(getApplicationContext(), "try to connect: " +btArrayAdapter.getItem(position), Toast.LENGTH_SHORT).show();

//            textStatus.setText("try...");

            final String name = btArrayAdapter.getItem(position); // get name
            final String address = deviceAddressArray.get(position); // get address
            boolean flag = true;

            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            // create & connect socket
            try {
                if(connectedThread != null) {
                    if (connectedThread.isOpen()) {
                        connectedThread.cancel();
//                        textStatus.setText("disconnected!");
                        showImage("1_image.png");
                    }
                }
                if (btSocket != null) {
                    if (btSocket.isConnected()) {
                        btSocket.close();
//                        textStatus.setText("closed!");
                        showImage("1_image.png");
                    }
                }

                btSocket = createBluetoothSocket(device);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                btSocket.connect();
            } catch (IOException e) {
                flag = false;
//                textStatus.setText("connection failed!");
                showImage("1_image.png");
                e.printStackTrace();
            }

            // start bluetooth communication
            if (flag) {
//                textStatus.setText("connected to " + name);
                if(name.compareTo("PRODUCT1") == 0)
                {
                    showImage("Dive_image.png");
                } else if(name.compareTo("PRODUCT2") == 0) {
                    showImage("Burn_image.png");
                } else if(name.compareTo("PRODUCT3") == 0) {
                    showImage("Flow_image.png");
                }
                connectedThread = new ConnectedThread(btSocket);
                connectedThread.start();
                seekBarBlue.setEnabled(true);
                btnOn.setEnabled(true);
                btnOff.setEnabled(true);
            }

        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }
        else return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }
}