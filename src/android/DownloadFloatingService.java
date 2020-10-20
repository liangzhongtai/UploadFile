package com.chinamobile.upload;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chinamobile.gdwy.MainActivity;
import com.chinamobile.gdwy.R;

import org.apache.cordova.CallbackContext;
import org.json.JSONObject;

import java.io.File;


/**
 * Created by liangzhongtai on 2020/6/17.
 * 下载更新悬浮窗
 */
public class DownloadFloatingService extends Service {
   public final static String FILE_NAME_TASK_UPDATE = "update_app";
   public final static String DIR_NAME_TASK_UPDATE = "rfworker";
   private static int HANDLER_UPDATE = 1;
   private static int HANDLER_DELAY_CHECK = 2;
   private static long DELAY_TIME = 10000;
   private WindowManager windowManager;
   private WindowManager.LayoutParams wmParams;
   private LinearLayout floatingLL;
   private TextView progress1TV;
   private TextView progress2TV;
   private TextView stateTV;
   private TextView versionTV;
   private TextView sizeTV;
   private float mTouchStartX;
   private float mTouchStartY;
   private boolean isTouch;
   private float x;
   private float y;
   private float xMiddel;
   private float xMax;
   private float widthFloatting;
   private static boolean viewAdded;
   private static long lastTime;
   private static int intervalClick = 3000;
   private static String url;
   private static String fileName;
   private static String dirName;
   private static boolean useBreakPoint;
   private static boolean useStream;
   private static String tickets;
   private static long maxNow;
   private static String maxSize;
   private static String sizeNow;
   private static float progressNow;
   private static volatile String state;
   private static String version;
   public static boolean isLoading;
   public static boolean hidde;
   public boolean isClick;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(UploadFile.TAG, "启动悬浮窗onCreate");
        viewAdded = false;
        lastTime = 0;
        url = "";
        fileName = "";
        dirName = "";
        useBreakPoint = true;
        useStream = false;
        tickets = "";
        maxNow = 0;
        maxSize = "";
        sizeNow = "";
        progressNow = 0;
        state = "";
        isLoading = false;
        initWindow();
        Message msg = new Message();
        msg.what = HANDLER_DELAY_CHECK;
        handler.sendMessageDelayed(msg, DELAY_TIME);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(UploadFile.TAG, "启动悬浮窗onStartCommand");
        if (windowManager == null ||
           wmParams == null ||
           floatingLL == null ||
           !viewAdded) {
           initWindow();
        }
        if (intent != null && intent.getExtras() != null) {
            Bundle extras = intent.getExtras();
            Log.d(UploadFile.TAG, "服务器获取extras=" + extras);
            String urlOri = extras.getString(UploadFile.URL, url);
            hidde = extras.getBoolean(UploadFile.CLOSE, false);
            if (hidde) {
                windowManager.removeView(floatingLL);
                viewAdded = false;
            } else {
                fileName = extras.getString(UploadFile.FILE_NAME, fileName);
                dirName = extras.getString(UploadFile.DIR_NAME, dirName);
                useBreakPoint = extras.getBoolean(UploadFile.USE_BREAKPOINT, useBreakPoint);
                useStream = extras.getBoolean(UploadFile.USE_STREAM, useStream);
                tickets = extras.getString(UploadFile.TICKETS, tickets);
                version = extras.getString(UploadFile.VERSION, version);
                url = urlOri.replace(UploadFile.REPLACE_TOKEN, tickets);
                maxNow = 0;
                maxSize = "-/-";
                progressNow = 0;
                // 保存下载记录
                JSONObject obj = new JSONObject();
                try {
                    obj.put(UploadFile.URL, urlOri);
                    obj.put(UploadFile.FILE_NAME, fileName);
                    obj.put(UploadFile.DIR_NAME, dirName);
                    obj.put(UploadFile.USE_BREAKPOINT, useBreakPoint);
                    obj.put(UploadFile.USE_STREAM, useStream);
                    obj.put(UploadFile.TICKETS, tickets);
                    obj.put(UploadFile.VERSION, version);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                DownloadFileUtil.writeObject(FILE_NAME_TASK_UPDATE, "", obj);
                if (TextUtils.isEmpty(tickets) || "".equals(tickets)) {
                    windowManager.removeView(floatingLL);
                    viewAdded = false;
                    return super.onStartCommand(intent, flags, startId);
                }
            }
        }
        this.download();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        try {
            windowManager.removeViewImmediate(floatingLL);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            windowManager = null;
            floatingLL    = null;
            viewAdded     = false;
            super.onDestroy();
        }
    }

    // 启动断点续传
    private void download() {
        try {
            if (TextUtils.isEmpty(url) || 
                TextUtils.isEmpty(fileName)) {
                return;
            }
            DownloadBreakPointManager.getInstance().downLoader(
                UploadFile.DOWNLOAD_BREAKPOINT_RESUME_UPDATE, url, fileName, dirName,
                useBreakPoint, useStream, tickets, null, new DownloadBreakPointListener() {
                    @Override
                    public void start(int uploadType, long max, long maxService, CallbackContext callBack) {
                        String size = "0/";
                        if (max > 1024 * 1024) {
                            maxSize = String.format("%.1f", (float) (max / (1024 * 1024))) + "mb";
                        } else {
                            maxSize = String.format("%.1f", (float) (max / 1024)) + "kb";
                        }
                        size = size + maxSize;
                        sizeNow = size;
                        maxNow = max;
                        Log.d(UploadFile.TAG, "文件总长度-------*******************8-----------max=" + max);
                        Log.d(UploadFile.TAG, "文件总长度-------*******************8-----------maxService=" + maxService);
                        state = "已就绪";
                        DownloadFloatingService.this.sendMsg(0, "已就绪", size);
                    }

                    @Override
                    public void loading(int uploadType, float progress, CallbackContext callBack) {
                        isLoading = true;
                        float nowSize = progress * maxNow;
                        String size;
                        if (maxNow > 1024 * 1024) {
                            size = String.format("%.1f", (float) (nowSize / (1024 * 1024))) + "/" + maxSize;
                        } else {
                            size = String.format("%.1f", (float) (nowSize / 1024)) + "/" + maxSize;
                        }
                        sizeNow = size;
                        progressNow = progress;
                        state = "下载中";
                        DownloadFloatingService.this.sendMsg(progress, "下载中", size);
                    }

                    @Override
                    public void complete(int uploadType, String path, CallbackContext callBack) {
                        isLoading = false;
                        File file = getFile(dirName, fileName);
                        if (file.length() <  maxNow || file.length() < DownloadBreakPoint.NORMAL_LENGTH) {
                            state = "已中断";
                            DownloadFloatingService.this.sendMsg(progressNow, "已中断", sizeNow);
                            return;
                        }
                        DownloadBreakPointManager.getInstance().remove(url, fileName, null);
                        progressNow = 1.0f;
                        state = "点击安装";
                        DownloadFloatingService.this.sendMsg(1.0f, "点击安装", maxSize + " 完成");
                    }

                    @Override
                    public void fail(int uploadType, int code, String message, CallbackContext callBack) {
                        isLoading = false;
                        state = "下载失败";
                        DownloadFloatingService.this.sendMsg(progressNow,"下载失败", sizeNow);
                    }

                    @Override
                    public void loadfail(int uploadType, String message, CallbackContext callBack) {
                        isLoading  = false;
                        state = "已中断";
                        DownloadFloatingService.this.sendMsg(progressNow, "已中断", sizeNow);
                    }
                });
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    // 更新悬浮窗口位置参数
    private void updateViewPosition(boolean isMove){
        wmParams.x = (int)(x-mTouchStartX);
        if (!isMove && wmParams.x > xMiddel) {
            wmParams.x = (int)(xMax - widthFloatting);
        } else if (!isMove) {
            wmParams.x = 0;
        }
        wmParams.y = (int)(y-mTouchStartY);
        if (viewAdded) {
            windowManager.updateViewLayout(floatingLL, wmParams);
        } else {
            viewAdded = true;
            windowManager.addView(floatingLL, wmParams);
        }
    }

    // 发送消息
    public void sendMsg(float progress, String state, String size) {
        Message msg = new Message();
        msg.obj = new Object[] {progress, state, size};
        msg.what = HANDLER_UPDATE;
        handler.sendMessage(msg);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == HANDLER_UPDATE) {
                Object[] objs = (Object[]) msg.obj;
                try {
                    updateView((Float) objs[0], (String) objs[1], (String) objs[2]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (msg.what == HANDLER_DELAY_CHECK) {
                if ("已中断".equals(state) || "下载失败".equals(state)) {
                    DownloadFloatingService.this.download();
                } else if ("点击安装".equals(state)) {
                    // 检查文件是否存在
                    File file = getFile(dirName, fileName);
                    long size = file.length();
                    if (!file.exists() && version.compareTo(getNowVersion()) > 0) {
                        // 不存在，重新下载
                        DownloadFloatingService.this.download();
                        return;
                    }

                    if (size < DownloadBreakPoint.NORMAL_LENGTH ||
                        (size > DownloadBreakPoint.NORMAL_LENGTH &&
                        maxNow > DownloadBreakPoint.NORMAL_LENGTH &&
                        size > maxNow + 1)) {
                        try {
                            file.delete();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        // 文件下载大小错误，重新下载
                        DownloadFloatingService.this.download();
                        return;
                    }
                }
                Message message = new Message();
                message.what = HANDLER_DELAY_CHECK;
                handler.sendMessageDelayed(message, DELAY_TIME);
            }
        }
    };

    /**
     * 获取当前客户端版本
     * */
    private String getNowVersion() {
        String versionName = "";
        try {
            String pkName = getApplication().getPackageName();
            versionName = getApplicationContext().getPackageManager().
                    getPackageInfo(pkName, 0).versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionName;
    }

    // 更新窗口显示
    private void updateView(float progress, String state, String size) {
        if (hidde) {
            return;
        }
        if (progress >= 0) {
            float pro1 = (progress + 0.1f) > 1 ? 1 : (progress + 0.1f);
            FrameLayout.LayoutParams params1 = (FrameLayout.LayoutParams) progress1TV.getLayoutParams();
            params1.width = (int) (pro1 * 80 * this.getResources().getDisplayMetrics().density);
            progress1TV.setLayoutParams(params1);

            FrameLayout.LayoutParams params2 = (FrameLayout.LayoutParams) progress2TV.getLayoutParams();
            params2.width = (int) (progress * 80 * this.getResources().getDisplayMetrics().density);
            progress2TV.setLayoutParams(params2);
        }
        if (progress >= 0.5) {
            sizeTV.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            sizeTV.setTextColor(getResources().getColor(android.R.color.black));
        }
        if (progress >= 0.7) {
            stateTV.setTextColor(getResources().getColor(android.R.color.white));
            versionTV.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            stateTV.setTextColor(getResources().getColor(android.R.color.black));
            versionTV.setTextColor(getResources().getColor(android.R.color.black));
        }
        stateTV.setText(state);
        versionTV.setText(version);
        if (!TextUtils.isEmpty(size)) {
            sizeTV.setText(size);
        }
        if (viewAdded) {
            windowManager.updateViewLayout(floatingLL, wmParams);
        } else {
            viewAdded = true;
            windowManager.addView(floatingLL, wmParams);
        }
    }

    // 获取文件
    public static File getFile(String dirName, String fileName) {
        String root = Environment.getExternalStorageDirectory() + "/";
        File file = new File(TextUtils.isEmpty(dirName) ? root : (root + "/" + dirName), fileName);
        return file;
    }

    // 初始化下载弹窗
    private void initWindow() {
        startForeground(1, new Notification());
        // 初始化布局
        floatingLL =  (LinearLayout) LayoutInflater.from(this).inflate(R.layout.download_layout, null);
        progress1TV = floatingLL.findViewById(R.id.tv_progress1);
        progress2TV = floatingLL.findViewById(R.id.tv_progress2);
        stateTV = floatingLL.findViewById(R.id.tv_state);
        versionTV = floatingLL.findViewById(R.id.tv_version);
        sizeTV = floatingLL.findViewById(R.id.tv_size);
        float density = getResources().getDisplayMetrics().density;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            clipViewCornerByDp(floatingLL, (int) density * 13);
        }

        windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        wmParams = new WindowManager.LayoutParams();

        // 至于手机最顶层
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            wmParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        } else if (Build.VERSION.SDK_INT >= 26) {
            //wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG;
            wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else if (Build.VERSION.SDK_INT == 25) {
            wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        } else {
            wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        // 不接受按键事件
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        // 背景色，透明为：PixelFormat.TRANSPARENT
        wmParams.format = PixelFormat.TRANSPARENT;

        // 悬浮窗相对屏幕的位置
        wmParams.gravity = Gravity.LEFT|Gravity.TOP;
        // 以屏幕左上角为原点，设置x，y初始值
        wmParams.x = 0;
        wmParams.y = 0;

        // 设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        // 屏幕宽度
        xMax = getResources().getDisplayMetrics().widthPixels;
        xMiddel = xMax / 2;
        widthFloatting = density * 80;

        // 添加View
        viewAdded = true;
        windowManager.addView(floatingLL, wmParams);

        // 触摸监听
        floatingLL.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // if(viewAdded)
                //获取相对屏幕的坐标，即以屏幕左上角为原点
                x = event.getRawX();
                // 25是系统状态栏的高度，也可以通过方法得到准确的值，自己微调就是了
                y = event.getRawY();
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        //获取相对View的坐标，即以此View左上角为原点
                        mTouchStartX = event.getX();
                        mTouchStartY = event.getY() + floatingLL.getHeight()/2;
                        isTouch = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        isTouch = true;
                        updateViewPosition(true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        updateViewPosition(false);
                        Log.d(UploadFile.TAG, "15 * density =" + 15 * density );
                        Log.d(UploadFile.TAG, "Math.abs(x - mTouchStartX)=" + Math.abs(event.getX() - mTouchStartX));
                        Log.d(UploadFile.TAG, "Math.abs(y - mTouchStartY)=" + Math.abs(event.getY() - mTouchStartY + floatingLL.getHeight()/2));
                        // 判断是否是点击
                        if (Math.abs(event.getX() - mTouchStartX) < 15 * density &&
                            Math.abs(event.getY() - mTouchStartY + floatingLL.getHeight()/2) < 15 * density) {
                            checkState();
                        }
                        mTouchStartX = mTouchStartY = 0;
                        break;
                }
                return isTouch;
        }});

        // 点击监听
        floatingLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DownloadFloatingService.this.checkState();
            }
        });
    }

    private void checkState() {
        Log.d(UploadFile.TAG, "点击");
        long nowTime = System.currentTimeMillis();
        if (nowTime - lastTime < intervalClick) {
            return;
        }
        lastTime = nowTime;
        // 下载完成，点击启动安装
        if ("点击安装".equals(state) &&
            maxNow > DownloadBreakPoint.NORMAL_LENGTH &&
            !TextUtils.isEmpty(fileName)) {
            Log.d(UploadFile.TAG, "下载完成，点击安装");
            Toast.makeText(getApplicationContext(), "安装包检测中...", Toast.LENGTH_LONG).show();
            // 检查文件是否存在
            File file = getFile(dirName, fileName);
            if (!file.exists() && version.compareTo(getNowVersion()) > 0) {
                Log.d(UploadFile.TAG, "没有文件");
                // 不存在，重新下载
                DownloadFloatingService.this.download();
                return;
            }
            long size = file.length();
            if (size < DownloadBreakPoint.NORMAL_LENGTH ||
                (size > DownloadBreakPoint.NORMAL_LENGTH &&
                maxNow > DownloadBreakPoint.NORMAL_LENGTH &&
                size > maxNow + 1)) {
                Toast.makeText(getApplicationContext(), "安装包已损坏，重新下载中...", Toast.LENGTH_LONG).show();
                try {
                    file.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // 文件下载大小错误，重新下载
                DownloadFloatingService.this.download();
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                boolean installAllowed = getApplicationContext().getPackageManager().canRequestPackageInstalls();
                if (!installAllowed) {
                    Log.d(UploadFile.TAG, "安装未知应用");
                    // 将用户引导至安装未知应用界面。
                    Intent intent = new Intent();
                    //获取当前apk包URI，并设置到intent中（这一步设置，可让“未知应用权限设置界面”只显示当前应用的设置项）
                    Uri packageURI = Uri.parse("package:" + getApplicationContext().getPackageName());
                    intent.setData(packageURI);
                    //设置不同版本跳转未知应用的动作
                    if (Build.VERSION.SDK_INT >= 26) {
                        intent.setAction(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    }else {
                        intent.setAction(android.provider.Settings.ACTION_SECURITY_SETTINGS);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    DownloadFloatingService.this.startActivity(intent);
                    return;
                }
            }
            UploadFileUtil.openFile(DownloadFloatingService.this, file);
            return;
        }

        // 下载中断，点击恢复下载
        if ("已中断".equals(state) || "下载失败".equals(state)) {
            Log.d(UploadFile.TAG, "已中断/下载失败，点击继续下载");
            DownloadFloatingService.this.download();
        }

        // 点击启动应用
        Log.d(UploadFile.TAG, "下载中，点击启动应用");
        Intent intent = new Intent(DownloadFloatingService.this, MainActivity.class);
        DownloadFloatingService.this.startActivity(intent);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void clipViewCircle(View view) {
        view.setClipToOutline(true);
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void clipViewCornerByDp(View view, final int pixel) {
        view.setClipToOutline(true);
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), pixel);
            }
        });
    }
}
