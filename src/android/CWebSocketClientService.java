package com.chinamobile.upload;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.chinamobile.gdwy.MainActivity;
import com.chinamobile.gdwy.R;

import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;

public class CWebSocketClientService extends Service {
    public static String WS                            = "ws://";
    public final static String WEBSOCKET               = "websocket";
    public final static String CHANNEL_ID_WEBSOCKET    = "956722";
    public static boolean start                        = false;
    public CWebSocketClient client;
    private CSTWebSocketClientBinder mBinder           = new CSTWebSocketClientBinder();
    private final static int NOTIFYCATION_ID_WEBSOCKET = 93412;
    private final static int NOTIFYCATION_ID_SERVICE   = 97108;
    private final static int NOTIFYCATION_ID_MSG       = 94523;
    //灰色保活
    public static class GrayInnerService extends Service {

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            // 适配8.0service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager notificationManager = (NotificationManager) getApplication().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID_WEBSOCKET, "消息推送",
                        NotificationManager.IMPORTANCE_LOW);
                notificationManager.createNotificationChannel(mChannel);
                Notification notification = new Notification.Builder(getApplicationContext(), CHANNEL_ID_WEBSOCKET).build();
                startForeground(NOTIFYCATION_ID_WEBSOCKET, notification);
            } else {
                startForeground(NOTIFYCATION_ID_WEBSOCKET, new Notification());
            }
            stopForeground(true);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }
    // 锁屏唤醒
    private PowerManager.WakeLock wakeLock;
    // 获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
    @SuppressLint("InvalidWakeLockTag")
    private void acquireWakeLock() {
        if (null == wakeLock) {
            PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE, "PostLocationService");
            if (null != wakeLock) {
                wakeLock.acquire();
            }
        }
    }

    //用于Activity和service通讯
    public class CSTWebSocketClientBinder extends Binder {
        public CWebSocketClientService getService() {
            return CWebSocketClientService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 标记为启动
        start = true;

        // 初始化websocket
        initSocketClient();

        // 开启心跳检测
        mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);

        //设置service为前台服务，提高优先级
        if (Build.VERSION.SDK_INT < 18) {
            //Android4.3以下 ，隐藏Notification上的图标
            startForeground(NOTIFYCATION_ID_SERVICE, new Notification());
        } else if(Build.VERSION.SDK_INT > 18 && Build.VERSION.SDK_INT < 25){
            //Android4.3 - Android7.0，隐藏Notification上的图标
            Intent innerIntent = new Intent(this, GrayInnerService.class);
            startService(innerIntent);
            startForeground(NOTIFYCATION_ID_SERVICE, new Notification());
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getApplication().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID_WEBSOCKET, "消息推送",
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(mChannel);
            Notification notification = new Notification.Builder(getApplicationContext(), CHANNEL_ID_WEBSOCKET).build();
            startForeground(NOTIFYCATION_ID_WEBSOCKET, notification);
        } else {
            startForeground(NOTIFYCATION_ID_WEBSOCKET, new Notification());
        }

        acquireWakeLock();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        closeConnect();
        super.onDestroy();
    }

    public CWebSocketClientService() {
    }


    /**
     * 初始化websocket连接
     */
    private synchronized void  initSocketClient() {
        if (TextUtils.isEmpty(WS) || "ws://".equalsIgnoreCase(WS)) {
            return;
        }
        URI uri = URI.create(WS);
        client = new CWebSocketClient(uri) {
            @Override
            public void onMessage(String message) {
                Log.d("JWebSocketClientService", "Service收到的消息：" + message);
                Intent intent = new Intent();
                intent.setAction(WEBSOCKET);
                intent.putExtra("message", message);
                sendBroadcast(intent);
                checkLockAndShowNotification(message);
            }

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                super.onOpen(handshakedata);
                // checkLockAndShowNotification("websocket连接成功");
                Log.d("JWebSocketClientService", "websocket连接成功");
            }
        };
        Log.d("JWebSocketClientService", "initSocketClient");
        connect();
    }

    /**
     * 连接websocket
     */
    private void connect() {
        new Thread() {
            @Override
            public void run() {
                try {
                    Log.d("JWebSocketClientService", "connect");
                    // connectBlocking多出一个等待操作，会先连接再发送，否则未连接发送会报错
                    client.connectBlocking();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("JWebSocketClientService", "connect_e=" + e.toString());
                }
            }
        }.start();
    }

    /**
     * 发送消息
     *
     * @param msg
     */
    public void sendMsg(String msg) {
        if (null != client) {
            Log.d("JWebSocketClientService", "发送的消息：" + msg);
            if (client.isClosed()) {
               this.connect();
            } else {
                client.send(msg);
            }
        }
    }

    /**
     * 断开连接
     */
    public void closeConnect() {
        // 标记为关闭
        start = false;
        try {
            if (null != client) {
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client = null;
        }
    }


    //-----------------------------------消息通知--------------------------------------------------------
    /**
     * 检查锁屏状态，如果锁屏先点亮屏幕
     *
     * @param content
     */
    private void checkLockAndShowNotification(String content) {
        Log.d("JWebSocketClientService", "checkLockAndShowNotification");
        //管理锁屏的一个服务
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        // 锁屏
        if (km.inKeyguardRestrictedInputMode()) {
            //获取电源管理器对象
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            if (!pm.isScreenOn()) {
                @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
                // 点亮屏幕
                wl.acquire();
                // 任务结束后释放
                wl.release();
            }
            sendNotification(content);
        } else {
            sendNotification(content);
        }
    }

    /**
     * 发送通知
     *
     * @param msg
     */
    private void sendNotification(String msg) {
        Log.d("JWebSocketClientService", "sendNotification");

        Intent intent = new Intent();
        intent.setClass(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // 适配安卓8.0及以上版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_WEBSOCKET,
                    "消息推送", NotificationManager.IMPORTANCE_HIGH);
            notifyManager.createNotificationChannel(channel);
            Notification notification = new NotificationCompat.Builder(this)
                    .setAutoCancel(true)
                    // 设置该通知优先级
                    .setPriority(Notification.FLAG_NO_CLEAR)
                    .setSmallIcon(R.mipmap.icon)
                    // .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),R.mipmap.icon))
                    .setContentTitle("网优助手")
                    .setContentText(msg)
                    .setVisibility(VISIBILITY_PUBLIC)
                    .setWhen(System.currentTimeMillis())
                    // 向通知添加声音、闪灯和振动效果
                    .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_ALL | Notification.DEFAULT_SOUND)
                    .setContentIntent(pendingIntent)
                    .setChannelId(CHANNEL_ID_WEBSOCKET)
                    .build();
            // id要保证唯一
            notifyManager.notify(NOTIFYCATION_ID_MSG, notification);
            Log.d("JWebSocketClientService", "发送");
        } else {
            Notification notification = new NotificationCompat.Builder(this)
                    .setAutoCancel(true)
                    // 设置该通知优先级
                    .setPriority(Notification.FLAG_NO_CLEAR)
                    .setSmallIcon(R.mipmap.icon)
                    // .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),R.mipmap.icon))
                    .setContentTitle("网优助手")
                    .setContentText(msg)
                    .setVisibility(VISIBILITY_PUBLIC)
                    .setWhen(System.currentTimeMillis())
                    // 向通知添加声音、闪灯和振动效果
                    .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_ALL | Notification.DEFAULT_SOUND)
                    .setContentIntent(pendingIntent)
                    .build();
            // id要保证唯一
            notifyManager.notify(NOTIFYCATION_ID_MSG, notification);
            Log.d("JWebSocketClientService", "发送");
        }
    }


    //  -------------------------------------websocket心跳检测------------------------------------------------
    // 每隔10秒进行一次对长连接的心跳检测
    private static final long HEART_BEAT_RATE = 10 * 1000;
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (!start) {
                return;
            }
            Log.d("JWebSocketClientService", "心跳包检测websocket连接状态");
            if (client != null) {
                // if (client.isClosed()) {
                    reconnectWs();
                // } else {
                    //client.sendPing();
                // }
            } else {
                //如果client已为空，重新初始化连接
                client = null;
                initSocketClient();
            }
            //每隔一定的时间，对长连接进行一次心跳检测
            mHandler.postDelayed(this, HEART_BEAT_RATE);
        }
    };

    /**
     * 开启重连
     */
    private void reconnectWs() {
        mHandler.removeCallbacks(heartBeatRunnable);
        new Thread() {
            @Override
            public void run() {
                try {
                    Log.d("JWebSocketClientService", "开启重连");
                    client.reconnectBlocking();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("JWebSocketClientService", "重连失败——e=" + e.toString());
                }
            }
        }.start();
    }
}
