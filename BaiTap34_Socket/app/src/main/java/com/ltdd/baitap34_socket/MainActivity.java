package com.ltdd.baitap34_socket;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;


import com.ltdd.baitap34_socket.databinding.ActivityMainBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SocketIO";
    // !!! REPLACE WITH YOUR SERVER URL !!!
    private static final String SERVER_URL = "YOUR_SOCKET_SERVER_URL"; // e.g., "http://10.0.2.2:3000" for emulator

    private ActivityMainBinding binding;
    private Socket mSocket;
    private MessageAdapter messageAdapter;
    private List<Message> messageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();
        initializeSocket();

        binding.buttonSend.setOnClickListener(v -> attemptSend());
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(messageList);
        binding.recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewMessages.setAdapter(messageAdapter);
    }

    private void initializeSocket() {
        try {
            // IO.Options options = new IO.Options(); // Add options if needed (e.g., auth)
            mSocket = IO.socket(SERVER_URL); // Use the constant
            Log.d(TAG, "Socket instance created for URL: " + SERVER_URL);
        } catch (URISyntaxException e) {
            Log.e(TAG, "URISyntaxException: " + e.getMessage());
            showToast("Invalid Server URL");
            // Handle error appropriately, maybe finish activity
            return;
        }

        // Register listeners for events FROM the server
        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on("new message", onNewMessage); // Listen for the chat message event

        mSocket.connect(); // Initiate connection
        Log.d(TAG, "Attempting to connect socket...");
    }

    // --- Socket Event Listeners ---

    private Emitter.Listener onConnect = args -> runOnUiThread(() -> {
        Log.i(TAG, "Socket Connected!");
        showToast("Connected");
        // You might want to join a room or send identification here
        // mSocket.emit("add user", "YourUsername");
    });

    private Emitter.Listener onDisconnect = args -> runOnUiThread(() -> {
        Log.i(TAG, "Socket Disconnected!");
        showToast("Disconnected");
    });

    private Emitter.Listener onConnectError = args -> runOnUiThread(() -> {
        Log.e(TAG, "Socket Connection Error: " + (args.length > 0 ? args[0] : "Unknown"));
        showToast("Connection Error");
        if (args.length > 0 && args[0] instanceof Exception) {
            ((Exception) args[0]).printStackTrace(); // Print stack trace for detailed debugging
        }
    });

    // Listener for incoming messages (Slide 6 structure)
    private Emitter.Listener onNewMessage = args -> runOnUiThread(() -> {
        Log.d(TAG, "New message event received");
        JSONObject data;
        String username = "Server"; // Default username
        String message = "Error parsing message"; // Default message

        if (args.length > 0 && args[0] instanceof JSONObject) {
            data = (JSONObject) args[0];
            Log.d(TAG,"Received Data: " + data.toString());
            try {
                // These keys MUST match what the server sends
                if (data.has("username")) {
                    username = data.getString("username");
                } else {
                    Log.w(TAG, "Received JSON does not contain 'username' key.");
                }
                if (data.has("message")) {
                    message = data.getString("message");
                } else {
                    Log.w(TAG, "Received JSON does not contain 'message' key.");
                    // Maybe the raw data is the message?
                    message = data.toString(); // Fallback: show the whole JSON if 'message' key is missing
                }

            } catch (JSONException e) {
                Log.e(TAG, "JSONException parsing message: " + e.getMessage());
                // Fallback: If JSON parsing fails, maybe the arg IS the message string?
                if (args[0] instanceof String) {
                    message = (String) args[0];
                    username = "Unknown Sender"; // No username if it's just a string
                } else {
                    message = "Received non-JSON/String data";
                }
            }
        } else if (args.length > 0 && args[0] instanceof String){
            // Handle case where server might just send a plain string message
            message = (String) args[0];
            username = "Server/Unknown";
            Log.d(TAG,"Received String Data: " + message);
        } else {
            Log.w(TAG,"Received data is not a JSONObject or String.");
            message = "Received unexpected data format";
        }


        addMessageToList(username, message, Message.TYPE_RECEIVED);
    });

    // --- Sending Messages ---

    // Method to send message (Slide 5 structure)
    private void attemptSend() {
        String message = binding.editTextMessage.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            binding.editTextMessage.requestFocus();
            return; // Don't send empty messages
        }

        if (mSocket != null && mSocket.connected()) {
            binding.editTextMessage.setText(""); // Clear input
            mSocket.emit("new message", message); // Use the same event name as listener
            Log.d(TAG, "Emitted 'new message': " + message);

            // Add the sent message to our list locally
            addMessageToList("Me", message, Message.TYPE_SENT);
        } else {
            showToast("Not connected");
            Log.w(TAG,"Attempted to send message while not connected.");
        }
    }

    // --- Helper Methods ---

    private void addMessageToList(String username, String message, int type) {
        messageList.add(new Message(username, message, type));
        // Notify adapter on the UI thread
        runOnUiThread(() -> {
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            scrollToBottom(); // Scroll to show the new message
        });
    }

    private void scrollToBottom() {
        binding.recyclerViewMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // --- Lifecycle Management ---

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        if (mSocket != null) {
            // Unregister listeners (Slide 6)
            mSocket.off(Socket.EVENT_CONNECT, onConnect);
            mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
            mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
            mSocket.off("new message", onNewMessage);

            mSocket.disconnect(); // Disconnect socket (Slide 6)
            Log.d(TAG, "Socket disconnected and listeners removed.");
        }
    }
}