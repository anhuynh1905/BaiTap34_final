package com.ltdd.baitap34_bt;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;


import com.ltdd.baitap34_bt.databinding.ActivityControlBinding;

import java.io.IOException;
import java.util.UUID;

public class BlueControl extends AppCompatActivity {

    private static final String TAG = "BlueControl";
    private ActivityControlBinding binding; // View Binding

    // Bluetooth
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // SPP UUID
    private BluetoothAdapter myBluetooth = null;
    private BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    String address = null; // MAC address of the device

    // UI
    private ProgressDialog progress;

    // State flags (as per slides)
    private int flaglamp1 = 0; // 0 = off, 1 = on
    private int flaglamp2 = 0; // 0 = off, 1 = on

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityControlBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d(TAG, "onCreate started");

        // Receive the address from MainActivity
        Intent intent = getIntent();
        address = intent.getStringExtra(MainActivity.EXTRA_ADDRESS);

        if (address == null || address.isEmpty()) {
            Toast.makeText(this, "Không nhận được địa chỉ thiết bị", Toast.LENGTH_LONG).show();
            finish(); // Cannot proceed without address
            return;
        }
        Log.i(TAG, "Received address: " + address);

        // Setup UI Click Listeners
        setupButtonClickListeners();

        // Start connection process
        // Check permission before attempting connection
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Toast.makeText(this, "Quyền Bluetooth Connect chưa được cấp", Toast.LENGTH_SHORT).show();
            // Ideally, request permission here if needed, but it should have been granted in MainActivity
            finish(); // Can't connect without permission
            return;
        }
        new ConnectBT().execute(); // Call the AsyncTask to connect
    }

    private void setupButtonClickListeners() {
        binding.btnTb1.setOnClickListener(v -> sendCommand("1", "A")); // Toggle lamp 1
        binding.btnTb2.setOnClickListener(v -> sendCommand("7", "G")); // Toggle lamp 2
        binding.btnDisc.setOnClickListener(v -> disconnect());
    }

    // Function to send commands (replaces thiettbi1 and thiettbi7)
    private void sendCommand(String commandOn, String commandOff) {
        if (btSocket != null && isBtConnected) {
            try {
                String commandToSend;
                boolean turnOn;

                // Determine which command set based on commandOn value
                if (commandOn.equals("1")) { // Lamp 1 logic
                    turnOn = (flaglamp1 == 0);
                    commandToSend = turnOn ? commandOn : commandOff;
                    flaglamp1 = turnOn ? 1 : 0; // Update flag
                    binding.btnTb1.setBackgroundResource(turnOn ? R.drawable.tb1on : R.drawable.tbloff); // Update UI
                } else { // Lamp 2 logic (commandOn should be "7")
                    turnOn = (flaglamp2 == 0);
                    commandToSend = turnOn ? commandOn : commandOff;
                    flaglamp2 = turnOn ? 1 : 0; // Update flag
                    binding.btnTb2.setBackgroundResource(turnOn ? R.drawable.tb2on : R.drawable.tb2off); // Update UI
                }

                btSocket.getOutputStream().write(commandToSend.getBytes());
                Log.d(TAG, "Sent command: " + commandToSend);
                String statusText = "Thiết bị " + (commandOn.equals("1") ? "1" : "7") + (turnOn ? " đang bật" : " đang tắt");
                binding.textV1.setText(statusText);

            } catch (IOException e) {
                Log.e(TAG, "Error sending command: " + e.getMessage());
                msg("Lỗi gửi lệnh: " + e.getMessage());
                // Optionally try to disconnect or show connection lost
                disconnect();
            }
        } else {
            msg("Chưa kết nối Bluetooth.");
        }
    }


    private void disconnect() {
        if (btSocket != null) { // If the btSocket is busy
            try {
                isBtConnected = false; // Update connection status
                btSocket.close(); // Close connection
                Log.i(TAG, "Bluetooth socket closed.");
                Toast.makeText(this, "Đã ngắt kết nối", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket: " + e.getMessage());
                msg("Lỗi ngắt kết nối: " + e.getMessage());
            }
        }
        finish(); // Return to the first layout (MainActivity)
    }

    // Helper method to show Toast (from slides)
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy called");
        disconnect(); // Ensure disconnect when activity is destroyed
    }

    // AsyncTask to connect to Bluetooth device
    @SuppressLint("StaticFieldLeak") // Suppress leak warning for simple example
    private class ConnectBT extends AsyncTask<Void, Void, Boolean> {
        private boolean connectSuccess = true;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress = ProgressDialog.show(BlueControl.this, "Đang kết nối...", "Xin vui lòng đợi!!!", true); // Show progress dialog
            Log.d(TAG, "ConnectBT: onPreExecute");
        }

        @SuppressLint("MissingPermission") // Permissions checked before calling execute()
        @Override
        protected Boolean doInBackground(Void... voids) { // Connect socket in background
            Log.d(TAG, "ConnectBT: doInBackground started");
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter(); // Get mobile bluetooth device
                    if (myBluetooth == null) {
                        Log.e(TAG, "ConnectBT: BluetoothAdapter is null");
                        return false;
                    }
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address); // Connects to the device's address and checks if it's available
                    if (dispositivo == null) {
                        Log.e(TAG, "ConnectBT: Could not get remote device for address: " + address);
                        return false;
                    }
                    Log.d(TAG, "ConnectBT: Got remote device: " + dispositivo.getName());

                    // Check BLUETOOTH_SCAN permission for cancelDiscovery on Android 12+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ActivityCompat.checkSelfPermission(BlueControl.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                            Log.w(TAG, "ConnectBT: BLUETOOTH_SCAN permission not granted for cancelDiscovery");
                            // Cannot cancel discovery without permission, connection might be less reliable
                        } else {
                            myBluetooth.cancelDiscovery();
                            Log.d(TAG, "ConnectBT: Discovery cancelled");
                        }
                    } else {
                        myBluetooth.cancelDiscovery(); // Cancel discovery on older versions (needs BLUETOOTH_ADMIN)
                        Log.d(TAG, "ConnectBT: Discovery cancelled (legacy)");
                    }


                    Log.d(TAG, "ConnectBT: Creating RFCOMM socket...");
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID); // Create a RFCOMM (SPP) connection

                    Log.d(TAG, "ConnectBT: Connecting socket...");
                    btSocket.connect(); // Start connection
                    Log.d(TAG, "ConnectBT: Socket connected successfully.");
                }
            } catch (IOException e) {
                connectSuccess = false; // If the try failed, you can check the exception here
                Log.e(TAG, "ConnectBT: Socket connection error: " + e.getMessage());
                e.printStackTrace();
            } catch (SecurityException se) {
                connectSuccess = false;
                Log.e(TAG, "ConnectBT: Security Exception (Missing Permission?): " + se.getMessage());
                se.printStackTrace();
            }
            Log.d(TAG, "ConnectBT: doInBackground finished, success = " + connectSuccess);
            return connectSuccess;
        }

        @Override
        protected void onPostExecute(Boolean result) { // After the doInBackground, it checks if everything went fine
            super.onPostExecute(result);
            Log.d(TAG, "ConnectBT: onPostExecute, result = " + result);

            if (progress != null && progress.isShowing()) {
                progress.dismiss();
            }

            if (!result) { // If connection failed
                msg("Kết nối thất bại! Kiểm tra thiết bị.");
                Log.e(TAG, "ConnectBT: Connection failed.");
                finish(); // Return to MainActivity
            } else {
                msg("Kết nối thành công.");
                Log.i(TAG, "ConnectBT: Connection successful.");
                isBtConnected = true;
                displayDeviceInfo(); // Display connected device info
            }
        }
    }

    // Method to display connected device info (replaces pairedDevicesList1)
    @SuppressLint("MissingPermission") // Permissions checked before connect attempt
    private void displayDeviceInfo() {
        if (myBluetooth != null && address != null) {
            // Check BLUETOOTH_CONNECT permission before getRemoteDevice on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG,"Permission denied for getRemoteDevice");
                    binding.textViewMAC.setText("Địa chỉ MAC: " + address + " (Quyền bị từ chối)");
                    return;
                }
            }
            try {
                BluetoothDevice connectedDevice = myBluetooth.getRemoteDevice(address);
                if (connectedDevice != null) {
                    String name = connectedDevice.getName();
                    String mac = connectedDevice.getAddress();
                    binding.textViewMAC.setText((name != null ? name : "Unknown Device") + "\n" + mac);
                    Log.i(TAG, "Displaying info for: " + name + " [" + mac + "]");
                } else {
                    binding.textViewMAC.setText("Địa chỉ MAC: " + address + " (Không lấy được tên)");
                    Log.w(TAG, "Could not get BluetoothDevice object for address: " + address);
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG,"Invalid Bluetooth address: " + address, e);
                binding.textViewMAC.setText("Địa chỉ MAC không hợp lệ: " + address);
            }
        } else {
            binding.textViewMAC.setText("Không có thông tin thiết bị");
            Log.w(TAG,"Cannot display device info - adapter or address is null");
        }
    }

}