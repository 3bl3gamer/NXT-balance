package com.example.bt_test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


public class MainActivity extends Activity implements SensorEventListener
{
	private BluetoothAdapter btAbapter = null;
	private BluetoothSocket sock = null;
	private TextView tv;
	private SensorManager sensorManager = null;
	private BroadcastReceiver receiver = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        final Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(3);
        tv = new TextView(this);
        setContentView(tv);
        
        btAbapter = BluetoothAdapter.getDefaultAdapter();
        if (!findInPaired()) scanForNew();
    }
    
    @Override
    protected void onDestroy()
    {
    	super.onDestroy();
    	/*if (btAbapter != null) {
    		btAbapter.cancelDiscovery();
    		btAbapter = null;
    	}*/
    	
    	//if socket won't be closed and app won't be completely restarted,
    	//sock.connect() will throw "java.io.IOException: read failed, socket might closed, read ret: -1"
		try {
			if (sock != null) {
				sock.close();
				sock = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (sensorManager != null) {
			sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
			sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
			sensorManager = null;
		}
		
		if (receiver != null) {
			this.unregisterReceiver(receiver);
			receiver = null;
		}
    }
    
    private boolean findInPaired()
    {
    	tv.setText(tv.getText() + "searching in paired devices\n");
    	
    	Set<BluetoothDevice> pairedDevices = btAbapter.getBondedDevices();
    	for (BluetoothDevice device : pairedDevices)
    	{
    		boolean matching = (device.getBluetoothClass() != null) && (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.TOY_ROBOT);
    		tv.setText(tv.getText() + device.getName() + " --- " + device.getAddress() + " matching: " + matching + "\n");
            if (matching)
            {
            	gotDevice(device);
                return true;
            }
        }
    	
    	tv.setText(tv.getText() + "no appropriate paired devices found\n");
    	return false;
    }
    
    private void scanForNew()
    {
    	tv.setText(tv.getText() + "scanning for new devices...\n");
        
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action))
                {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    boolean matching = (device.getBondState() != BluetoothDevice.BOND_BONDED) && (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.TOY_ROBOT);
                    tv.setText(tv.getText() + device.getName() + " --- " + device.getAddress() + " matching: " + matching + "\n");
                    if (matching)
                    {
                        btAbapter.cancelDiscovery();
                        gotDevice(device);
                    }
                }
                else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
                {
                	tv.setText(tv.getText() + "done scanning\n");
                }
            }
        };
        
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(receiver, filter);
        
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(receiver, filter);
        
        btAbapter.startDiscovery();
    }
    
    private void gotDevice(BluetoothDevice device)
    {
    	tv.setText(tv.getText() + "sending to " + device.getName() + "\n");
    	try {
        	sock = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
    		//Method m = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
    		//BluetoothSocket sock = (BluetoothSocket) m.invoke(device, 1);
        	tv.setText(tv.getText() + "got socket, connecting...\n");
			sock.connect();
			//OutputStream os = sock.getOutputStream();
			//InputStream is = sock.getInputStream();
			//tv.setText(tv.getText() + "sending...\n");
			//byte[] buf = {0x07, 0x00, (byte)0x00, 0x09, 0x00, 0x03, 0x01, 0x41, 0x00};//{ 0x0C, 0x00, 0x00, 0x04, (byte)0xFF, 100, 0x01, 0x00, 0x00, 0x20, (byte)0x80, 0x00, 0x00, 0x00 };//
			//os.write(buf);
			//os.flush();
			//send("Hello World!");
			//tv.setText(tv.getText() + "sended\n");
			//is.read(buf);
			//tv.setText(tv.getText() + "readed, "+buf[0]+","+buf[1]+","+buf[2]+","+buf[3]+","+buf[4]+"\n");
			/*new Thread(){
				@Override
				public void run()
				{
					while(true)
					{
						float a = (System.currentTimeMillis()%1000)/1000.0f;
						float b = a*2;
						float c = a/2;
						send(""+a+" "+b+" "+c);
						try { Thread.sleep(20); } catch (InterruptedException e) {}
					}
				}
			}.start();*/
			sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
			sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
			sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
		} catch (IOException e) {
			tv.setText(tv.getText() + e.toString());
		}/* catch (NoSuchMethodException e) {
			tv.setText("no such method");
		} catch (IllegalAccessException e) {
			tv.setText("illegal access");
		} catch (InvocationTargetException e) {
			tv.setText("InvocationTargetException");
		}*/
    }
    
    private void send(String str)
    {
		try {
			byte[] strBytes = str.getBytes();
			int msgLength = 4 + strBytes.length + 1; // header(4) + string(strBytes.length) + zero_termination(1)
			byte[] buf = new byte[2+msgLength]; // message_length(2) + message(msgLength)
			
			buf[0] = (byte)(msgLength&0xFF);
			buf[1] = (byte)(msgLength>>8);
			buf[2] = 0x00; // no reply telegram
			buf[3] = 0x09; // MessageWrite Direct Command
			buf[4] = 0x00; // Mailbox number
			buf[5] = (byte)(strBytes.length+1);
			for (int i=0; i<strBytes.length; i++) buf[i+6] = strBytes[i]; 
			buf[msgLength+1] = 0x00; // zero termination
			
			OutputStream os = sock.getOutputStream();
			os.write(buf);
			os.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    
    @Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    
    float accel_summ_angle = 0;
    float gyro_summ_angle_speed = 0;
    int events_slowdown_rate = 4;
    int event_index = 0;
    @Override
    public void onSensorChanged(SensorEvent event) {
    	float ax, ay, az;
    	if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
    	{
			ax = event.values[0];
			ay = event.values[1];
			az = event.values[2];
			accel_summ_angle += (float)Math.atan2(az, ax);
		}
    	else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
		{
			ax = event.values[0];
			ay = event.values[1];
			az = event.values[2];
			gyro_summ_angle_speed += ay;
			
			if ((++event_index)%events_slowdown_rate == 0)
			{
				float gyro_avg_angle_speed = gyro_summ_angle_speed/event_index;
				float accel_avg_angle      = accel_summ_angle     /event_index;
				
				send(String.format(Locale.ENGLISH, "%.05f %.05f", gyro_avg_angle_speed, accel_avg_angle));
				
				gyro_summ_angle_speed = 0;
				accel_summ_angle = 0;
				event_index = 0;
			}
		}
    }

	
}
