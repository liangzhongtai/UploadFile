package com.chinamobile.upload;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

/**
 * Created by liangzhongtai on 2020/5/19.
 */
public class CWebSocket {
    private volatile static CWebSocket uniqueInstance;
    private CWebSocketClient client;
    private CWebSocketClientService.CSTWebSocketClientBinder binder;
    private CWebSocketClientService cstWebSClientService;
    private CSTWebSocketReceiver receiver;
    private CallbackContext callback;
    private Activity activity;
    private boolean hasNoSend;
    private String noSendMessage;
    public boolean isFrontDesk;

    private CWebSocket() {
    }

    public static synchronized CWebSocket getInstance() {
        if (uniqueInstance == null) {
            uniqueInstance = new CWebSocket();
        }
        return uniqueInstance;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // 服务与活动成功绑定
            Log.d("JWebSocket", "服务与活动成功绑定");
            binder = (CWebSocketClientService.CSTWebSocketClientBinder) iBinder;
            cstWebSClientService = binder.getService();
            CWebSocket.this.client = cstWebSClientService.client;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //服务与活动断开
            Log.d("JWebSocket", "服务与活动成功断开");
        }
    };

    /**
     * 关闭websocket链接
     * */
    public void closeWebSocket() {
        try {
            if (null != client) {
                client.close();
            }
            if (null != cstWebSClientService) {
                cstWebSClientService.closeConnect();
            }
            destoryWebSocket(activity);
            Log.d("JWebSocket", "关闭长链接");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client = null;
        }
    }

    /**
     * Websocket的广播接收者
     * */
    private class CSTWebSocketReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CWebSocketClientService.WEBSOCKET.equals(intent.getAction())) {
                String message = intent.getStringExtra("message");
                Log.d("JWebSocket", "返回Plugin_message=" + message);
                if (callback != null) {
                    // 如果应用在前台，记录标记
                    if (isFrontDesk) {
                        hasNoSend = false;
                        PluginResult result = new PluginResult(PluginResult.Status.OK, message);
                        result.setKeepCallback(true);
                        callback.sendPluginResult(result);
                        return;
                    }
                    // 如果应用不在前台，记录标记
                    hasNoSend = true;
                    noSendMessage = message;
                }
            }
        }
    }

    /**
     * 应用返回到前台
     * */
    public void onResume() {
        isFrontDesk = true;
        // 检查是否有没有送达的消息
        if (hasNoSend && callback != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, noSendMessage);
            result.setKeepCallback(true);
            callback.sendPluginResult(result);
        }
    }

    /**
     * 应用切到后台
     * */
    public void onStop() {
        isFrontDesk = false;
    }

    /**
     * 初始化websocket
     * @param activity Mainactivity的引用
     * @param callback Plugin插件native回调js层的引用
     * @param ws webscoket的ws地址
     * */
    public void initWebSocket(Activity activity, CallbackContext callback, String ws) {
        CWebSocketClientService.WS = ws;
        hasNoSend = false;
        isFrontDesk = true;
        Log.d("JWebSocket", ws);
        //启动服务
        startService(activity);
        //绑定服务
        bindService(activity, callback);
        //注册广播
        doRegisterReceiver(activity);
    }

    /**
     * 销毁websocket
     * */
    public void destoryWebSocket(Activity activity) {
        if (activity == null) {
            return;
        }
        if (serviceConnection!=null) {
            activity.unbindService(serviceConnection);
        }
        unRegisterReceiver(activity);
    }

    /**
     * 注销广播
     * */
    public void unRegisterReceiver(Activity activity) {
        if (receiver != null) {
            try {
                activity.unregisterReceiver(receiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        receiver = null;
    }

    /**
     * 启动服务（websocket客户端服务）
     */
    private void startService(Activity activity) {
        Intent intent = new Intent(activity, CWebSocketClientService.class);
        activity.startService(intent);
    }

    /**
     * 动态注册广播
     */
    private void doRegisterReceiver(Activity activity) {
        if (receiver != null) {
            activity.unregisterReceiver(receiver);
        }
        receiver = new CSTWebSocketReceiver();
        IntentFilter filter = new IntentFilter(CWebSocketClientService.WEBSOCKET);
        activity.registerReceiver(receiver, filter);
    }

    /**
     * 绑定服务
     * @param activity Mainactivity的引用
     * @param callback Plugin插件native回调js层的引用
     */
    private void bindService(Activity activity, CallbackContext callback) {
        this.callback = callback;
        this.activity = activity;
        Intent bindIntent = new Intent(activity, CWebSocketClientService.class);
        activity.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
}
