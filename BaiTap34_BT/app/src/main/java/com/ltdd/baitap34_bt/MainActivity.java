package com.ltdd.baitap34_bt;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;


import com.ltdd.baitap34_bt.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding; // View Binding

    // Constants
    public static String EXTRA_ADDRESS = "device_address";

    // Bluetooth
    private BluetoothAdapter myBluetooth = null;
    private ArrayAdapter<String> pairedDevicesArrayAdapter;
    private ArrayList<String> deviceListInfo = new ArrayList<>(); // To store "Name\nAddress"
    private ArrayList<String> deviceAddresses = new ArrayList<>(); // To store just addresses

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> enableBtLauncher;
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;
    private final List<String> neededPermissions = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d(TAG, "onCreate started");

        setupActivityResultLaunchers();

        // Setup ArrayAdapter for paired devices ListView
        pairedDevicesArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceListInfo);
        binding.listTb.setAdapter(pairedDevicesArrayAdapter);
        binding.listTb.setOnItemClickListener(myListClickListener); // Set click listener

        // Setup Button Click
        binding.btnTimthietbi.setOnClickListener(v -> {
            Log.d(TAG, "Find Devices button clicked");
            checkAndRequestPermissions(); // Check permissions first
            // Listing devices will be triggered after permissions are granted
        });

        // Initial Bluetooth Check
        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        if (myBluetooth == null) {
            Toast.makeText(getApplicationContext(), "Thiết bị không hỗ trợ Bluetooth", Toast.LENGTH_LONG).show();
            finish(); // Close app if no Bluetooth support
            return;
        }

        // Check permissions on startup as well
        checkAndRequestPermissions();
    }

    private void setupActivityResultLaunchers() {
        // Launcher for enabling Bluetooth
        enableBtLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Toast.makeText(this, "Bluetooth đã được bật", Toast.LENGTH_SHORT).show();
                        listPairedDevices(); // List devices after enabling
                    } else {
                        Toast.makeText(this, "Bạn cần bật Bluetooth để tiếp tục", Toast.LENGTH_SHORT).show();
                        // Optionally finish() or disable functionality
                    }
                });

        // Launcher for requesting multiple permissions
        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allGranted = true;
                    for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                        if (!entry.getValue()) {
                            Log.w(TAG, "Permission denied: " + entry.getKey());
                            allGranted = false;
                        }
                    }

                    if (allGranted) {
                        Log.d(TAG, "All necessary permissions granted.");
                        checkBluetoothStateAndListDevices(); // Proceed after getting permissions
                    } else {
                        Toast.makeText(this, "Bạn cần cấp đủ quyền Bluetooth và Vị trí", Toast.LENGTH_LONG).show();
                        // Handle the case where permissions are denied (e.g., disable functionality)
                    }
                });
    }

    private void checkAndRequestPermissions() {
        neededPermissions.clear();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        } else { // Below Android 12
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
        }
        // Location permission is often needed regardless of SDK for scanning
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }


        if (!neededPermissions.isEmpty()) {
            Log.d(TAG, "Requesting permissions: " + neededPermissions);
            requestPermissionsLauncher.launch(neededPermissions.toArray(new String[0]));
        } else {
            Log.d(TAG, "All permissions already granted.");
            // Permissions are already granted, proceed
            checkBluetoothStateAndListDevices();
        }
    }

    private void checkBluetoothStateAndListDevices() {
        // Double check adapter after potential permission delays
        if (myBluetooth == null) {
            myBluetooth = BluetoothAdapter.getDefaultAdapter();
            if (myBluetooth == null) { return; } // Should not happen if checked in onCreate
        }

        // Check BLUETOOTH_CONNECT permission before calling isEnabled() on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "BLUETOOTH_CONNECT permission needed for isEnabled check");
                // Permissions should have been requested, maybe show a message or wait
                Toast.makeText(this, "Quyền Bluetooth Connect chưa được cấp", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        // Now safe to check isEnabled()

        if (!myBluetooth.isEnabled()) {
            Log.d(TAG, "Bluetooth is not enabled. Requesting enable.");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtLauncher.launch(enableBtIntent);
        } else {
            Log.d(TAG, "Bluetooth is enabled. Listing paired devices.");
            listPairedDevices(); // List devices if Bluetooth is already on
        }
    }


    //SuppressLint needed because permissions checked dynamically via launchers
    @SuppressLint("MissingPermission")
    private void listPairedDevices() {
        Log.d(TAG, "Listing paired devices...");

        // Check BLUETOOTH_CONNECT permission before getBondedDevices on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "BLUETOOTH_CONNECT permission not granted for getBondedDevices");
                Toast.makeText(this, "Quyền Bluetooth Connect chưa được cấp", Toast.LENGTH_SHORT).show();
                return; // Cannot proceed without permission
            }
        }
        // Now safe to call getBondedDevices()

        Set<BluetoothDevice> pairedDevices = myBluetooth.getBondedDevices();
        deviceListInfo.clear(); // Clear previous list
        deviceAddresses.clear();

        if (pairedDevices != null && !pairedDevices.isEmpty()) {
            Log.d(TAG, "Found " + pairedDevices.size() + " paired devices.");
            for (BluetoothDevice device : pairedDevices) {
                // Check permission again before accessing name/address, though redundant if checked above
                // if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                deviceListInfo.add(deviceName + "\n" + deviceHardwareAddress);
                deviceAddresses.add(deviceHardwareAddress); // Store address separately
                Log.i(TAG, "Device: " + deviceName + " [" + deviceHardwareAddress + "]");
                // } else { Log.e(TAG,"Permission denied inside loop"); } // Should not happen
            }
        } else {
            Log.d(TAG, "No paired devices found.");
            Toast.makeText(getApplicationContext(), "Không tìm thấy thiết bị Bluetooth nào đã ghép đôi.", Toast.LENGTH_LONG).show();
            deviceListInfo.add("Không có thiết bị nào"); // Add placeholder
            deviceAddresses.add("");
        }
        pairedDevicesArrayAdapter.notifyDataSetChanged(); // Update the ListView
    }

    // Click listener for the ListView
    private final android.widget.AdapterView.OnItemClickListener myListClickListener = (parent, view, position, id) -> {
        if (position < deviceAddresses.size() && !deviceAddresses.get(position).isEmpty()) {
            // Get the device MAC address, which is stored separately now
            String address = deviceAddresses.get(position);
            String info = ((TextView) view).getText().toString(); // For logging/display if needed
            Log.d(TAG, "ListView item clicked: Position=" + position + ", Info=" + info + ", Address=" + address);


            // Make an intent to start next activity.
            Intent i = new Intent(MainActivity.this, BlueControl.class);
            // Change the activity.
            i.putExtra(EXTRA_ADDRESS, address); //this will be received at BlueControl (class) Activity
            startActivity(i);
        } else {
            Log.w(TAG, "Clicked on placeholder or invalid item at position: " + position);
        }
    };

}