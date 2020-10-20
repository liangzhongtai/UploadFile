package com.chinamobile.upload;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

/**
 * Created by liangzhongtai on 2020/5/19.
 */

public class CWebSocketClient extends WebSocketClient{
    public CWebSocketClient(URI serverUri) {
        super(serverUri, new Draft_6455());
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.d("JWebSocketClient", "onOpen");
    }

    @Override
    public void onMessage(String message) {
        Log.d("JWebSocketClient", "onMessage:" + message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d("JWebSocketClient", "onClose:code=" + code + "_reason=" + reason + "_remote=" + remote);
    }

    @Override
    public void onError(Exception ex) {
        Log.d("JWebSocketClient", "onError:" + ex.toString());
    }
}
