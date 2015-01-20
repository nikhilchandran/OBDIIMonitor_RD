/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.OBDIIMonitor;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * This is the main Activity that displays the current chat session.
 * @param <ImageView>
 */
@SuppressLint("HandlerLeak")
public class BluetoothChat<ImageView> extends Activity {
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private EditText mOutEditText;
    private Button mSendButton;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread

    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    ProgressDialog uploadProgress;
    File fileToUpload;
    //String serverUrlString = "http://192.168.20.64:8080/obd/upload/file";
    String serverUrlString = "http://obdinapp.azurewebsites.net/obd/upload/file";
    //String serverUrlString = "http://23.101.10.141/obd/upload/file";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.main);
        uploadProgress = new ProgressDialog(BluetoothChat.this);
        uploadProgress.setMax(100);
        uploadProgress.setProgress(0);

        File externalStorageDir = Environment.getExternalStorageDirectory();

        File folder = new File(Environment.getExternalStorageDirectory() + "/obd");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }

        fileToUpload = new File(externalStorageDir+"/obd/","obdlog.txt");

        if (!fileToUpload.exists()) {

            try {

                fileToUpload.createNewFile();

            } catch (IOException e) {

                e.printStackTrace();

            }

        }

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
    }
    
    //keep track of current PID number
    int message_number = 1;
    
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread

        // Initialize the compose field with a listener for the return key
        //mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        //mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        /*mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message + '\r');
            }
        });*/

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");

        final  Button uploadButton = (Button) findViewById(R.id.uploadButton);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        final ToggleButton getDataButton = (ToggleButton) findViewById(R.id.toggleButton1);
        uploadButton.setEnabled(false);
        getDataButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (getDataButton.isChecked()) {
                    startTransmission();


                    uploadButton.setEnabled(false);

                } else {

                    uploadButton.setEnabled(true);

                    message_number = 0;
                }
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v) {

                String fileToUploadPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/obd/obdlog.txt";


                test(fileToUploadPath,serverUrlString);
            }
        });
    }



    public void test( String uploadFile,String actionUrl)
    {

        try {
            Log.i("---------------","test");

            final String end = "\r\n";
            final String twoHyphens = "--";
            final String boundary = "*****++++++************++++++++++++";

            URL url = new URL(actionUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");

/* setRequestProperty */
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            DataOutputStream ds = new DataOutputStream(conn.getOutputStream());
            ds.writeBytes(twoHyphens + boundary + end);
            ds.writeBytes("Content-Disposition: form-data; name=\"from\"" + end + end + "auto" + end);
            ds.writeBytes(twoHyphens + boundary + end);
            ds.writeBytes("Content-Disposition: form-data; name=\"to\"" + end + end + "ja" + end);
            ds.writeBytes(twoHyphens + boundary + end);
            ds.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + uploadFile + "\"" + end);
            ds.writeBytes(end);
            Log.i("-----------","writeBytes");
            FileInputStream fStream = new FileInputStream(uploadFile);
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int length = -1;

            while ((length = fStream.read(buffer)) != -1) {
                ds.write(buffer, 0, length);
            }
            ds.writeBytes(end);
            ds.writeBytes(twoHyphens + boundary + twoHyphens + end);
/* close streams */
            fStream.close();
            ds.flush();
            ds.close();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Toast.makeText(getBaseContext(), conn.getResponseMessage(), Toast.LENGTH_LONG);
                Log.i("getResponseCode",conn.getResponseCode()+"");
            }
            else{
                Log.i("getResponseCode",conn.getResponseCode()+"");
            }

            StringBuffer b = new StringBuffer();
            InputStream is = conn.getInputStream();
            byte[] data = new byte[bufferSize];
            int leng = -1;
            while ((leng = is.read(data)) != -1) {
                b.append(new String(data, 0, leng));
            }
            String result = b.toString();
            Log.i("result",result);
        }
        catch (Exception e){
            Log.i("Exception",e.toString());
        }
    }


    public void startTransmission() {

    	sendMessage("01 00" + '\r');

    }


	public void getData(int messagenumber) {


		switch(messagenumber) {

        	case 1:
        		sendMessage("01 0C" + '\r'); //get RPM
        		messagenumber++;
        		break;

        	case 2:
        		sendMessage("01 0D" + '\r'); //get MPH
        		messagenumber++;
        		break;
        	case 3:
        		sendMessage("01 04" + '\r'); //get Engine Load
        		messagenumber++;
        		break;
        	case 4:
        		sendMessage("01 05" + '\r'); //get Coolant Temperature
        		messagenumber++;
        		break;
        	case 5:
        		sendMessage("01 0F" + '\r'); //get Intake Temperature
        		messagenumber++;
        		break;

        	case 6:
        		sendMessage("AT RV" + '\r'); //get Voltage
        		messagenumber++;
        		break;

        	default: ;
		}
    }


    public void clearCodes() {


        if(mConnectedDeviceName != null) {

        	sendMessage("04" + '\r'); //send Clear Trouble Codes Command
        	Toast.makeText(getApplicationContext(), "OBD Trouble Codes Cleared", Toast.LENGTH_SHORT).show();

        }
        else {
        	Toast.makeText(getApplicationContext(), "OBD Adapter NOT CONNECTED", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    /*private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }*/

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);


            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            if(D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };

    private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }

    //Contains previous value of parameters for gauge transition
    int prev_intake = 0;
	int prev_load = 0;
	int prev_coolant = 0;
	int prev_MPH = 0;
	int prev_RPM = 0;
	int prev_voltage = 0;


    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {


        @Override
        public void handleMessage(Message msg) {

        	//<--------- Initialize Data Display Fields ---------->//
        	final TextView RPM = (TextView) findViewById(R.id.RPMView);
        	final TextView MPH = (TextView) findViewById(R.id.MPHView);
        	final TextView engineLoad = (TextView) findViewById(R.id.LoadView);
        	final TextView coolantTemperature = (TextView) findViewById(R.id.CoolantView);
        	final TextView intakeTemperature = (TextView) findViewById(R.id.IntakeView);
        	final TextView voltage = (TextView) findViewById(R.id.voltView);


        	String dataRecieved;
        	int value = 0;
        	int value2 = 0;
        	int PID = 0;


            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    setStatus(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    setStatus(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);

                //mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);



                // ------- ADDED CODE FOR OBD -------- //
                dataRecieved = readMessage;

                if((dataRecieved != null) && (dataRecieved.matches("\\s*[0-9A-Fa-f]{2} [0-9A-Fa-f]{2}\\s*\r?\n?" ))) {

	        			dataRecieved = dataRecieved.trim();
	        			String[] bytes = dataRecieved.split(" ");

	        			if((bytes[0] != null)&&(bytes[1] != null)) {

	        				PID = Integer.parseInt(bytes[0].trim(), 16);
	        				value = Integer.parseInt(bytes[1].trim(), 16);
	        				}


	        		switch(PID) {

		            	case 15://PID(0F): Intake Temperature

		            		value = value - 40; //Formula for Intake Temperature
		                    value = ((value * 9)/ 5) + 32; //Convert from Celsius to Farenheit
		                    String displayIntakeTemp = String.valueOf(value);
		                	intakeTemperature.setText(displayIntakeTemp);

		                    int needle_value = (((value-30) * 21)/10) - 105;

                            new writetoFileAsync("Intake Temperature",displayIntakeTemp).execute();


                            break;


		            	case 4://PID(04): Engine Load

		            		value = (value * 100 ) / 255;
		            		needle_value = ((value * 21)/10) - 105;
		              	  	String displayEngineLoad = String.valueOf(value);
                            new writetoFileAsync("Engine Load",displayEngineLoad).execute();


                            engineLoad.setText(displayEngineLoad);
		            		break;

		            	case 5://PID(05): Coolant Temperature

		            		value = value - 40;
		            		value = ((value * 9)/ 5) + 32; //convert to deg F
		            		needle_value = (((value-50) * 21)/20) - 105;



                            String displayCoolantTemp = String.valueOf(value);
                            new writetoFileAsync("Coolant Temperature",displayCoolantTemp).execute();

                            coolantTemperature.setText(displayCoolantTemp);
		            		break;

		            	case 12: //PID(0C): RPM
		                		int RPM_value = (value*256)/4;
		                		needle_value = ((RPM_value * 22)/1000) - 85;


		                		String displayRPM = String.valueOf(RPM_value);
                            new writetoFileAsync("RPM",displayRPM).execute();

                            RPM.setText(displayRPM);
		            		break;


		            	case 13://PID(0D): MPH

		            		value = (value * 5) / 8; //convert KPH to MPH
		            		needle_value = ((value * 21)/20) - 85;


		             	    String displayMPH = String.valueOf(value);
                            new writetoFileAsync("Kph",displayMPH).execute();

                            MPH.setText(displayMPH);
		            		break;

		            	default: ;

	        		}

        	}
            else if((dataRecieved != null) && (dataRecieved.matches("\\s*[0-9A-Fa-f]{1,2} [0-9A-Fa-f]{2} [0-9A-Fa-f]{2}\\s*\r?\n?" ))) {

    			dataRecieved = dataRecieved.trim();
    			String[] bytes = dataRecieved.split(" ");

    			if((bytes[0] != null)&&(bytes[1] != null)&&(bytes[2] != null)) {

    				PID = Integer.parseInt(bytes[0].trim(), 16);
    				value = Integer.parseInt(bytes[1].trim(), 16);
    				value2 = Integer.parseInt(bytes[2].trim(), 16);
    			}

    			//PID(0C): RPM
            	if(PID == 12) {

            		int RPM_value = ((value*256)+value2)/4;
            		int needle_value = ((RPM_value * 22)/1000) - 85;



            		String displayRPM = String.valueOf(RPM_value);
                    new writetoFileAsync("RPM",displayRPM).execute();

                    RPM.setText(displayRPM);
            	}
            	else if((PID == 1)||(PID == 65)) {

            		switch(value) {

			            	case 15://PID(0F): Intake Temperature

			            		value2 = value2 - 40; //formula for INTAKE AIR TEMP
			                    value2 = ((value2 * 9)/ 5) + 32; //convert to deg F
			                    int needle_value = (((value2-30) * 21)/10) - 105;


			                    String displayIntakeTemp = String.valueOf(value2);
                                new writetoFileAsync("Intake Temperature",displayIntakeTemp).execute();

                                intakeTemperature.setText(displayIntakeTemp);
			            		break;

			            	case 4://PID(04): Engine Load

			            		value2 = (value2 * 100 ) / 255;
			              	  	String displayEngineLoad = String.valueOf(value2);
			              	  	needle_value = ((value2 * 21)/10) - 105;

                                new writetoFileAsync("Engine Load",displayEngineLoad).execute();

                                engineLoad.setText(displayEngineLoad);
			            		break;

			            	case 5://PID(05): Coolant Temperature

			            		value2 = value2 - 40;
			            		value2 = ((value2 * 9)/ 5) + 32; //convert to deg F
			            		needle_value = (((value2-50) * 21)/20) - 105;


			             	    String displayCoolantTemp = String.valueOf(value2);
                                new writetoFileAsync("Coolant Temperature",displayCoolantTemp).execute();

                                coolantTemperature.setText(displayCoolantTemp);
			            		break;

			            	case 13://PID(0D): MPH

			            		value2 = (value2 * 5) / 8; //convert to MPH
			            		needle_value = ((value2 * 21)/20) - 85;

			            		String displayMPH = String.valueOf(value2);
                                new writetoFileAsync("Kph",displayMPH).execute();

                                MPH.setText(displayMPH);
			            		break;

			            	default: ;
            				}
            	}

            }
            else if((dataRecieved != null) && (dataRecieved.matches("\\s*[0-9]+(\\.[0-9]?)?V\\s*\r*\n*" ))) {

            	dataRecieved = dataRecieved.trim();
            	String volt_number = dataRecieved.substring(0, dataRecieved.length()-1);
            	double needle_value = Double.parseDouble(volt_number);
            	needle_value = (((needle_value - 11)*21) /0.5) - 100;
            	int volt_value = (int)(needle_value);

                   new writetoFileAsync("Voltage",dataRecieved).execute();

                    voltage.setText(dataRecieved);

            }
            else if((dataRecieved != null) && (dataRecieved.matches("\\s*[0-9]+(\\.[0-9]?)?V\\s*V\\s*>\\s*\r*\n*" ))) {

            	dataRecieved = dataRecieved.trim();
            	String volt_number = dataRecieved.substring(0, dataRecieved.length()-1);
            	double needle_value = Double.parseDouble(volt_number);
            	needle_value = (((needle_value - 11)*21) /0.5) - 100;
            	int volt_value = (int)(needle_value);

                    new writetoFileAsync("Voltage",dataRecieved).execute();


                    voltage.setText(dataRecieved);

            }
            else if((dataRecieved != null) && (dataRecieved.matches("\\s*[ .A-Za-z0-9\\?*>\r\n]*\\s*>\\s*\r*\n*" ))) {

            	if(message_number == 7) message_number = 1;
            	getData(message_number++);
            }
            else {

        		;
        	}


                //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }


    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true);
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
        case R.id.secure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            return true;
       /* case R.id.insecure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;*/
        }
        return false;
    }

    private class writetoFileAsync extends AsyncTask<String, Integer, String> {
        private ProgressDialog pDialog;
        String filename;
        String sensorArray;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(BluetoothChat.this);
            pDialog.setMessage("Working ...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        private writetoFileAsync(String mfilename, String msensorArray) {
            filename = mfilename;
            sensorArray = msensorArray;
        }

        @Override
        protected String doInBackground(String... str) {

            //   db.addContact(new UserData(mkey, mvalue));
            //   writetoFile( filename,  sensorArray);
            writetoFile(filename, sensorArray);


            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);


            pDialog.dismiss();
        }
    }


    public Boolean writetoFile( String key,String value){
        try {
            long time  = System.currentTimeMillis();

            File readfile = new File(Environment.getExternalStorageDirectory()

                    + "/obd/obdlog.txt");

            FileWriter fw = new FileWriter(readfile.getAbsoluteFile(),true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("\n"+time+","+key+","+value);
            bw.close();
            Log.d("Suceess","Sucess");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}

	
