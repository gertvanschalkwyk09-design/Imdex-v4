package com.imdex.fluidmonitor;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import org.eclipse.paho.client.mqttv3.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MqttTcpClient {
    private final Context context;
    private final WebView webView;
    private final SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MqttTcpClient(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        this.prefs = context.getSharedPreferences("imdex_mqtt", Context.MODE_PRIVATE);
    }

    @JavascriptInterface public void saveCredentials(String username, String password) {
        SharedPreferences.Editor e = prefs.edit();
        e.putString("username", username == null ? "" : username);
        if (password != null && !password.isEmpty()) e.putString("password", password);
        e.apply();
    }

    @JavascriptInterface public String getSavedUsername() { return prefs.getString("username", ""); }
    @JavascriptInterface public String getSavedPassword() { return prefs.getString("password", ""); }

    @JavascriptInterface public void publish(String host, int port, String username, String password, String topic, String payload) {
        executor.execute(() -> {
            MqttClient client = null;
            try {
                String broker = "tcp://" + host + ":" + port;
                client = new MqttClient(broker, "IMDEX-FluidMonitor-" + UUID.randomUUID(), null);
                MqttConnectOptions options = new MqttConnectOptions();
                options.setAutomaticReconnect(false);
                options.setCleanSession(true);
                options.setConnectionTimeout(12);
                options.setKeepAliveInterval(30);
                if (username != null && !username.isEmpty()) options.setUserName(username);
                if (password != null && !password.isEmpty()) options.setPassword(password.toCharArray());
                client.connect(options);
                MqttMessage msg = new MqttMessage((payload == null ? "" : payload).getBytes(StandardCharsets.UTF_8));
                msg.setQos(1);
                client.publish(topic, msg);
                client.disconnect();
            } catch (Exception ignored) {
            } finally {
                try { if (client != null) client.close(); } catch (Exception ignored) {}
            }
        });
    }
}
