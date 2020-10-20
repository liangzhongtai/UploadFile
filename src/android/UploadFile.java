package com.chinamobile.upload;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;


import com.chinamobile.gdwy.LogUtil;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by liangzhongtai on 2018/5/17.
 */

public class UploadFile extends CordovaPlugin{
    public final static String TAG              = "Upload_Plugin";
    public final static String URL              = "url";
    public final static String FILE_NAME        = "fileName";
    public final static String DIR_NAME         = "dirName";
    public final static String USE_BREAKPOINT   = "useBreakPoint";
    public final static String USE_STREAM       = "useStream";
    public final static String TICKETS          = "tickets";
    public final static String VERSION          = "version";
    public final static String CLOSE           = "close";
    public final static String REPLACE_TOKEN    = "replace_token";

    public final static int RESULTCODE_PERMISSION       = 20440;
    public final static int RESULTCODE_OVERLAY_WINDOW   = 20441;

    // 上传
    public final static int UPLOAD_DEFAULT              = 0;
    // 取消上传
    public final static int CANCEL                      = 1;
    // 取消所有上传
    public final static int CANCEL_ALL                  = 2;
    // 上传不需回传进度
    public final static int UPLOAD_NO_PROGRESS          = 3;
    // GET请求
    public final static int GET                         = 4;
    // 下载文件
    public final static int DOWNLOAD_FILE               = 5;
    // 预览附件
    public final static int PREVIEW_FILE                = 6;
    // 断点续传
    public final static int DOWNLOAD_BREAKPOINT_RESUME  = 7;
    // 暂停断点续传
    public final static int DOWNLOAD_BREAKPOINT_PAUSE   = 8;
    // 取消断点续传
    public final static int DOWNLOAD_BREAKPOINT_CANCEL  = 9;
    // 文件删除
    public final static int DELETE_FILES                = 10;
    // 生成上传文件
    public final static int FILE_CREATE                 = 11;
    // 检查文件是否存在
    public final static int FILE_CHECK_EXIT             = 12;
    // 启动websocket
    public final static int START_WEB_SOCKET            = 13;
    // 关闭websocket
    public final static int CLOSE_WEB_SOCKET            = 14;
    // 关闭前台服务
    public final static int CLOSE_FORE_GROUND           = 15;
    // 断点续传更新
    public final static int DOWNLOAD_BREAKPOINT_RESUME_UPDATE = 16;
    // 检查是否存在更新下载任务
    public final static int TASK_UPDATE                 = 17;
    // 关闭9宫格
    public final static int CLOSE_UPDATE                = 18;
    // 删除更新记录
    public final static int DELETE_TASK_RECORD          = 19;


    // 网络不可用
    public final static int NET_NOT_AVAILABLE       = 1;
    // 文件上传失败
    public final static int UPLOAD_FAILE            = 2;
    // 文件上传成功
    public final static int UPLOAD_SUCCESS          = 3;
    // 文件路径不存在
    public final static int FILE_NO_EXIST           = 4;
    // 上传进度
    public final static int UPLOAD_PROGRESS         = 5;
    // 文件下载失败
    public final static int DOWNLOAD_FAILE          = 6;
    // 文件下载成功
    public final static int DOWNLOAD_SUCCESS        = 7;
    // 文件保存失败
    public final static int SAVE_FAILE              = 8;
    // 文件断点续传初始化失败
    public final static int BREAK_RESUME_INIT_FAILE = 9;
    // 文件删除成功
    public final static int DELETE_FINISH           = 10;
    // 文件创建失败
    public final static int CREATE_FAILE            = 11;
    // 文件创建成功
    public final static int CREATE_SUCCESS          = 12;
    // 开始上传
    public final static int UPLOAD_START            = 13;
    // 开始下载
    public final static int DOWNLOAD_START          = 14;
    // 下载进度
    public final static int DOWNLOAD_PROGRESS       = 15;

    // 文件的状态
    // 未下载
    public final static int STATUS_NO_START = 0;
    // 开始
    public final static int STATUS_START    = 1;
    // 下载中
    public final static int STATUS_LOADING  = 2;
    // 停止
    public final static int STATUS_STOP     = 3;
    // 丢失
    public final static int STATUS_LOST     = 4;
    // 完成
    public final static int STATUS_FINISH   = 5;
    // 异常
    public final static int STATUS_ERROR    = 6;

    // 上传参数
    public static JSONArray args;
    //文件上传格式
    public boolean base64;
    public CordovaInterface cordova;
    public CordovaWebView webView;
    public boolean first = true;
    public boolean openOver = false;
    public long delay = 0;
    public static CallbackContext callbackContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.cordova = cordova;
        this.webView = webView;
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext)
            throws JSONException {
        Log.d(TAG,"执行方法uploadfile");
        Log.d(TAG,"length="+args.length());
        if ("coolMethod".equals(action)) {
            UploadFile.args = args;
            //权限
            try {
                if(!PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    ||!PermissionHelper.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ||!PermissionHelper.hasPermission(this, Manifest.permission.ACCESS_NETWORK_STATE)) {
                    UploadFile.callbackContext = callbackContext;
                    PermissionHelper.requestPermissions(this,RESULTCODE_PERMISSION,new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.ACCESS_NETWORK_STATE});
                } else {
                    startWork(args, callbackContext);
                }
            } catch (Exception e){
                //权限异常
                callbackContext.error("文件上传功能异常");
                return true;
            }
            return true;
        }
        return super.execute(action, args, callbackContext);
    }

    @Override
    public Bundle onSaveInstanceState() {
        return super.onSaveInstanceState();
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                callbackContext.error("缺少内部存储权限, 无法使用app下载功能");
                return;
            }
        }
        switch (requestCode) {
            case RESULTCODE_PERMISSION:
                startWork(args, callbackContext);
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        LogUtil.d(TAG, "权限检查返回_onActivityResult=" + requestCode);
        switch (requestCode) {
            case RESULTCODE_OVERLAY_WINDOW:
                startWork(args, callbackContext);
                break;
            default:
                break;
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        CWebSocket.getInstance().onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        CWebSocket.getInstance().onStop();
    }

    @Override
    public void onDestroy() {
        UploadFileUtil.getInstance(cordova.getActivity()).cancelAll();
        DownloadBreakPointManager.getInstance().cancelAll();
        CWebSocket.getInstance().unRegisterReceiver(cordova.getActivity());
        super.onDestroy();
    }

    private void startWork(JSONArray args, CallbackContext callBack) {
        try {
            int uploadType = args.getInt(0);
            Log.d(TAG,"uploadType="+uploadType);
            // 开启websocket链接
            if (uploadType == START_WEB_SOCKET) {
                String ws = args.getString(1);
                CWebSocket.getInstance().initWebSocket(cordova.getActivity(), callBack, ws);
                return;
            // 关闭websocket链接
            } else if (uploadType == CLOSE_WEB_SOCKET) {
                CWebSocket.getInstance().closeWebSocket();
                return;
            // 关闭前台服务
            } else if (uploadType == CLOSE_FORE_GROUND) {
                Intent intentWeb = new Intent(cordova.getContext(), CWebSocketClientService.class);
                cordova.getActivity().stopService(intentWeb);
                Intent intentWebInner = new Intent(cordova.getContext(), CWebSocketClientService.GrayInnerService.class);
                cordova.getActivity().stopService(intentWebInner);
                return;
            // 取消接口
            } else if (uploadType == CANCEL) {
                String url = args.getString(1);
                UploadFileUtil.getInstance(cordova.getActivity()).cancelUpload(url);
                return;
            // 取消全部接口
            } else if (uploadType == CANCEL_ALL) {
                UploadFileUtil.getInstance(cordova.getActivity()).cancelAll();
                return;
            // 下载文件
            } else if (uploadType == DOWNLOAD_FILE) {
                Log.d(TAG, "启动下载");
                String url = args.getString(1);
                String fileName = args.getString(2);
                String dirName = args.length() > 3 ? args.getString(3) : "";
                try {
                    DownloadFileUtil.getInstance(cordova.getActivity()).
                            download(callBack ,this, uploadType, url, fileName, dirName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            // 预览文件
            } else if(uploadType == PREVIEW_FILE) {
                Log.d(TAG, "启动预览");
                String url = args.getString(1);
                String fileName = args.getString(2);
                String dirName =  args.length() > 3 ? args.getString(3) : "";
                String dir = Environment.getExternalStorageDirectory()
                        + (TextUtils.isEmpty(dirName) ? "" : ("/" + dirName));
                File file = new File(dir, fileName);
                if (!file.exists()) {
                    Log.d(UploadFile.TAG,"预览的文件不存在");
                    sendUpdateResult(callBack, uploadType, UploadFile.FILE_NO_EXIST, "文件不存在");
                    return;
                }
                UploadFileUtil.openFile(this.cordova.getContext(), file);
                return;

            // 断点续传
            } else if (uploadType == DOWNLOAD_BREAKPOINT_RESUME ||
                       uploadType == DOWNLOAD_BREAKPOINT_RESUME_UPDATE) {
                Log.d(TAG, "启动断点下载");
                String url = args.getString(1);
                String fileName = args.getString(2);
                String dirName = args.length() > 3 ? args.getString(3) : "";
                boolean useBreakPoint = args.length() > 4 ? args.getBoolean(4) : true;
                boolean useStream = args.length() > 5 ? args.getBoolean(5) : false;
                String tickets = args.length() > 6 ? args.getString(6) : "";
                Log.d(TAG, "url=" + url);
                Log.d(TAG, "fileName=" + fileName);
                Log.d(TAG, "dirName=" + dirName);


                // 启动下载悬浮
                if (uploadType == DOWNLOAD_BREAKPOINT_RESUME_UPDATE) {
                    String version = args.length() > 7 ? args.getString(7) : "";
                    boolean pass = Build.MODEL.contains("PIC-AL00") && openOver;
                    // Toast.makeText(cordova.getContext(),"下载_MODEL=" + Build.MODEL + "_openOver=" + openOver, Toast.LENGTH_LONG).show();
                    // 有悬浮窗权限开启服务绑定
                    if (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(cordova.getActivity()) || pass) {
                        Log.d(TAG, "启动断点下载更新");
                        // 先删除旧文件
                        File file = DownloadFloatingService.getFile(dirName, fileName);
                        if (file.exists()) {
                            file.delete();
                        }
                        // 启动悬浮窗服务
                        Intent intent = new Intent(cordova.getActivity(), DownloadFloatingService.class);
                        intent.putExtra(URL, url);
                        intent.putExtra(FILE_NAME, fileName);
                        intent.putExtra(DIR_NAME, dirName);
                        intent.putExtra(USE_BREAKPOINT, useBreakPoint);
                        intent.putExtra(USE_STREAM, useStream);
                        intent.putExtra(TICKETS, tickets);
                        intent.putExtra(VERSION, version);
                        cordova.getActivity().startService(intent);
                        // 没有悬浮窗权限，提示悬浮窗权限
                    } else {
                        Log.d(TAG, "启动弹窗权限页面");
                        try {
                            openOver = true;
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                            Uri packageURI = Uri.parse("package:" + cordova.getActivity().getPackageName());
                            intent.setData(packageURI);
                            cordova.setActivityResultCallback(this);
                            cordova.getActivity().startActivityForResult(intent, RESULTCODE_OVERLAY_WINDOW);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                        }
                    }
                    return;
                }
                try {
                    // 启动断点续传
                    delay = System.currentTimeMillis();
                    DownloadBreakPointManager.getInstance().
                        downLoader(uploadType, url, fileName, dirName, useBreakPoint, useStream, tickets, callBack,
                        new DownloadBreakPointListener() {
                            @Override
                            public void start(int uploadType, long max, long maxService, CallbackContext callBack) {
                                // 文件总大小
                                Log.d(UploadFile.TAG, fileName + "_文件总大小max=" + max);
                                delay = System.currentTimeMillis() - delay;
                                UploadFile.this.sendDownloadBreakPointResult(
                                    uploadType, STATUS_START,
                                    maxService == -1 ? "-1" : max + "", url, true, callBack);
                            }

                            @Override
                            public void loading(int uploadType, float progress, CallbackContext callBack) {
                                // 下载进度
                                Log.d(UploadFile.TAG, fileName + "_下载进度progress=" + progress);
                                UploadFile.this.sendDownloadBreakPointResult(
                                    uploadType, STATUS_LOADING,
                                    progress + "", url, true, callBack);

                            }

                            @Override
                            public void complete(int uploadType, String path, CallbackContext callBack) {
                                // 移除任务
                                DownloadBreakPointManager.getInstance().remove(url, fileName, callBack);
                                long max = 0;
                                File file = new File(path);
                                if (file.exists()) {
                                    max = file.length();
                                }
                                // 下载完成
                                Log.d(UploadFile.TAG, fileName + "_下载完成complete=" + path + "_max=" + max);
                                UploadFile.this.sendDownloadBreakPointResult(
                                    uploadType, STATUS_FINISH,
                                    path + "", url, max == 0 ? "" : (max + ""), false, callBack);
                            }

                            @Override
                            public void fail(int uploadType, int code, String message, CallbackContext callBack) {
                                // 请求失败
                                Log.d(UploadFile.TAG, fileName + "_请求失败fail=" + message);
                                UploadFile.this.sendDownloadBreakPointResult(
                                    uploadType, STATUS_ERROR,
                                    message + "", url, false, callBack);
                            }

                            @Override
                            public void loadfail(int uploadType, String message, CallbackContext callBack) {
                                // 下载异常
                                Log.d(UploadFile.TAG, fileName + "_下载异常loadfail=" + message);
                                UploadFile.this.sendDownloadBreakPointResult(
                                    uploadType, STATUS_STOP,
                                    message + "", url, false, callBack);
                            }
                        });
                } catch (Exception e) {
                    e.printStackTrace();
                    sendUpdateResult(callBack, uploadType,
                            UploadFile.BREAK_RESUME_INIT_FAILE, "断点续传初始化失败");
                }
                return;
            // 检查更新下载任务
            } else if (uploadType == TASK_UPDATE) {
                Log.d(TAG, "检查更新下载任务");
                String tickets = args.getString(1);
                // 已启动下载，不再检查
                if (DownloadFloatingService.isLoading) {
                    return;
                }
                JSONObject obj = DownloadFileUtil.readObject(DownloadFloatingService.FILE_NAME_TASK_UPDATE, "");
                if (obj == null) {
                    return;
                }
                String url = obj.getString(UploadFile.URL);
                String fileName = obj.getString(UploadFile.FILE_NAME);
                String dirName = obj.getString(UploadFile.DIR_NAME);
                boolean useBreakPoint = obj.getBoolean(UploadFile.USE_BREAKPOINT);
                boolean useStream = obj.getBoolean(UploadFile.USE_STREAM);
                if (TextUtils.isEmpty(tickets)) {
                    tickets = obj.getString(UploadFile.TICKETS);
                }
                // 格式化更新版本
                String versionUpdate = obj.getString(UploadFile.VERSION);
                // 获取本地版本
                try {
                    String pkName = cordova.getContext().getPackageName();
                    String versionName = cordova.getContext().getPackageManager().
                            getPackageInfo(pkName, 0).versionName;
                    Log.d(TAG, "检查更新记录versionUpdate=" + versionUpdate);
                    Log.d(TAG, "检查更新记录versionName=" + versionName);
                    // 比较更新版本和本地版本
                    if (TextUtils.isEmpty(versionUpdate) ||
                            versionUpdate.compareTo(versionName) <= 0) {
                        return;
                    }
                    Log.d(TAG, "检查更新记录url=" + url);
                    boolean pass = Build.MODEL.contains("PIC-AL00") && openOver;
                    // Toast.makeText(cordova.getContext(),"任务_MODEL=" + Build.MODEL + "_openOver=" + openOver, Toast.LENGTH_LONG).show();
                    // 启动断点下载更新
                    // 有悬浮窗权限开启服务绑定
                    if (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(cordova.getActivity()) || pass) {
                        Log.d(TAG, "启动断点下载更新");
                        // 启动悬浮窗服务
                        Intent intent = new Intent(cordova.getActivity(), DownloadFloatingService.class);
                        intent.putExtra(URL, url);
                        intent.putExtra(FILE_NAME, fileName);
                        intent.putExtra(DIR_NAME, dirName);
                        intent.putExtra(USE_BREAKPOINT, useBreakPoint);
                        intent.putExtra(USE_STREAM, useStream);
                        intent.putExtra(TICKETS, tickets);
                        intent.putExtra(VERSION, versionUpdate);
                        cordova.getActivity().startService(intent);
                        // 没有悬浮窗权限，提示悬浮窗权限
                    } else {
                        Log.d(TAG, "启动弹窗权限页面");
                        try {
                            openOver = true;
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                            Uri packageURI = Uri.parse("package:" + cordova.getActivity().getPackageName());
                            intent.setData(packageURI);
                            cordova.setActivityResultCallback(this);
                            cordova.getActivity().startActivityForResult(intent, RESULTCODE_OVERLAY_WINDOW);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            } else if (uploadType == CLOSE_UPDATE) {
                if (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(cordova.getActivity())) {
                    Log.d(TAG, "启动断点下载更新");
                    // 启动悬浮窗服务
                    Intent intent = new Intent(cordova.getActivity(), DownloadFloatingService.class);
                    intent.putExtra(CLOSE, true);
                    cordova.getActivity().startService(intent);
                    // 没有悬浮窗权限，提示悬浮窗权限
                } else {
                    Log.d(TAG, "启动弹窗权限页面");
                    try {
                        openOver = true;
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        Uri packageURI = Uri.parse("package:" + cordova.getActivity().getPackageName());
                        intent.setData(packageURI);
                        cordova.setActivityResultCallback(this);
                        cordova.getActivity().startActivityForResult(intent, RESULTCODE_OVERLAY_WINDOW);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                    }
                }
                return;
            // 删除任务记录
            } else if (uploadType == DELETE_TASK_RECORD) {
                JSONObject obj = new JSONObject();
                DownloadFileUtil.writeObject(DownloadFloatingService.FILE_NAME_TASK_UPDATE, "", obj);
                return;
            // 取消/暂停断点续传
            } else if (uploadType == DOWNLOAD_BREAKPOINT_PAUSE ||
                       uploadType == DOWNLOAD_BREAKPOINT_CANCEL) {
                String url = args.getString(1);
                DownloadBreakPointManager.getInstance().cancel(url, callBack);
                return;
            // 删除文件
            } else if (uploadType == DELETE_FILES) {
                String urlStrs = args.getString(1);
                String files = args.getString(2);
                String dirName =  args.length() > 3 ? args.getString(3) : "";
                String[] urls = urlStrs.contains(",") ? urlStrs.split(",") : new String[]{ urlStrs };
                String[] fileNames = files.contains(",") ? files.split(",") : new String[]{ files };
                Log.d(UploadFile.TAG, "要删除的urlStrs:" + urlStrs);
                Log.d(UploadFile.TAG, "要删除的files:" + files);
                Log.d(UploadFile.TAG, "要删除的urls:" + Arrays.toString(urls));
                Log.d(UploadFile.TAG, "要删除的fileNames:" + Arrays.toString(fileNames));
                // 先取消下载
                for (int i = 0; i < urls.length; i++) {
                    DownloadBreakPointManager.getInstance().cancel(urls[i], callBack);
                }
                Log.d(UploadFile.TAG, "要删除的文件");
                // 再移除本地文件
                String dir = Environment.getExternalStorageDirectory()
                        + (TextUtils.isEmpty(dirName) ? "" : ("/" + dirName));
                for (int i = 0; i < fileNames.length; i++) {
                    File file = new File(dir, fileNames[i]);
                    Log.d(UploadFile.TAG, "要删除的文件fileName_i="+i+":" + fileNames[i]);
                    if (file.exists()) {
                        file.delete();
                    }
                }
                sendUpdateResult(callBack, uploadType, DELETE_FINISH,"文件删除成功!");
                return;
            // 生成文件
            } else if (uploadType == FILE_CREATE) {
                String fileName = args.length() > 2 ? args.getString(2) : "";
                Log.d(TAG, "生成文件1_ftpAbsoPath=" + fileName);
                String dirPath = Environment.getExternalStorageDirectory() + "/" ;
                String name = TextUtils.isEmpty(fileName) ? "upload500.zip" : fileName;
                String ftpAbsoPath = dirPath + name;
                long size = args.length() > 3 ? args.getInt(3) : 100;
                File file = new File(ftpAbsoPath);
                Log.d(TAG, "生成文件1_ftpAbsoPath=" + ftpAbsoPath);
                // 文件存在，直接返回
                if (file.exists() && file.length() == (size * 1024 * 1024)) {
                    sendUpdateResult(callBack, uploadType, CREATE_SUCCESS, ftpAbsoPath);
                    Log.d(TAG, "生成文件2");
                    return;
                } else {
                    file.delete();
                }
                file.createNewFile();
                callbackContext = callBack;
                Log.d(TAG, "生成文件3");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // 循环，创建文件，直到文件大小大于设定值
                        RandomAccessFile raf = null;
                        try {
                            raf = new RandomAccessFile(ftpAbsoPath, "rw");
                            // 1 Mb
                            byte[] b = new byte[1024 * 1024];
                            b[0] = '0';
                            b[b.length - 1] = '1';
                            for (int i = 0; i < size; i++) {
                                raf.write(b);
                                raf.seek(raf.length());
                            }
                            raf.close();
                            Log.d(TAG, "生成文件4");
                            Bundle bundle = new Bundle();
                            bundle.putInt("uploadType", uploadType);
                            bundle.putInt("status", CREATE_SUCCESS);
                            bundle.putString("message", ftpAbsoPath);
                            Message msg = new Message();
                            msg.what = HANDLER_CREATE_FILE;
                            msg.setData(bundle);
                            mHandler.sendMessage(msg);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d(TAG, "生成文件5");
                            Bundle bundle = new Bundle();
                            bundle.putInt("uploadType", uploadType);
                            bundle.putInt("status", CREATE_FAILE);
                            bundle.putString("message", "文件创建失败!");
                            Message msg = new Message();
                            msg.what = HANDLER_CREATE_FILE;
                            msg.setData(bundle);
                            mHandler.sendMessage(msg);
                        }
                    }
                }).start();
                return;
            } else if (uploadType == FILE_CHECK_EXIT) {

            } else {
                if (args.length() > 1 && args.length() % 2 == 0) {
                    String url = args.getString(1);
                    JSONArray keysJA = args.getJSONArray(2);
                    JSONArray valuesJA = args.getJSONArray(3);
                    String[] keys = new String[keysJA.length()];
                    List values = new ArrayList();
                    for (int i=0,len = keysJA.length();i<len;i++ ){
                        keys[i] = keysJA.getString(i);
                    }
                    Log.d(TAG,"keys="+ Arrays.toString(keys));
                    for (int i=0,len = valuesJA.length();i<len;i++){
                        Object value = valuesJA.get(i);
                        if(value instanceof JSONArray){
                            JSONArray fpJArray = valuesJA.getJSONArray(i);
                            if (fpJArray==null) {
                                break;
                            }
                            List<String> filePaths = new ArrayList<String>();
                            for (int j=0,jLen = fpJArray.length();j<jLen;j++){
                                filePaths.add(fpJArray.getString(j));
                            }
                            values.add(filePaths);
                        } else if(value instanceof String){
                            values.add(valuesJA.getString(i));
                        } else  if (value instanceof Integer) {
                            values.add(valuesJA.getInt(i));
                        } else if(value instanceof Boolean){
                            values.add(valuesJA.getBoolean(i));
                        } else if(value instanceof Float || value instanceof Double){
                            values.add(valuesJA.getDouble(i));
                        } else if (value instanceof Long){
                            values.add(valuesJA.getLong(i));
                        } else if(value instanceof JSONObject){
                            values.add(valuesJA.getJSONObject(i));
                        } else {
                            values.add(valuesJA.get(i));
                        }
                    }
                    Log.d(TAG,"values="+values);
                    if (!UploadFileUtil.isNetworkAvailable(cordova.getActivity())) {
                        sendUpdateResult(callBack, uploadType,NET_NOT_AVAILABLE,"网络不可用,请检查网络");
                        return;
                    }
                    UploadFileUtil.getInstance(cordova.getActivity()).listener = new UploadListener() {
                        @Override
                        public void sendUpdateResult(int uploadType, int status, String message) {
                            UploadFile.this.sendUpdateResult(callBack, uploadType, status, message);
                        }

                        @Override
                        public void sendUpdateResult(int uploadType, int status, String message, long nowSize, long totalSize) {
                            UploadFile.this.sendUpdateResult(callBack, uploadType, status, message, nowSize, totalSize);
                        }
                    };
                    UploadFileUtil.getInstance(cordova.getActivity()).upload(uploadType, url, keys, values, base64);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendUpdateResult(CallbackContext callbackContext,int uploadType, int status,
                                 String message){
        PluginResult pluginResult;
        JSONArray array = new JSONArray();
        try {
            array.put(0, uploadType);
            array.put(1, status);
            array.put(2, message);
            Log.d(TAG,"返回的uploadType="+ uploadType + "_status="+ status + "_message=" + message);
        } catch (Exception e){
            e.printStackTrace();
        }
        if (status == NET_NOT_AVAILABLE||
            status == UPLOAD_FAILE||
            status == FILE_NO_EXIST||
            status == DOWNLOAD_FAILE||
            status == CREATE_FAILE) {
            pluginResult = new PluginResult(PluginResult.Status.ERROR, array);
            pluginResult.setKeepCallback(false);
        } else {
            pluginResult = new PluginResult(PluginResult.Status.OK, array);
            pluginResult.setKeepCallback(
            status == DELETE_FINISH ||
            status == UPLOAD_SUCCESS ||
            status == DOWNLOAD_SUCCESS ||
            status == CREATE_SUCCESS ? false : true);
        }

        callbackContext.sendPluginResult(pluginResult);
    }

    public void sendUpdateResult(CallbackContext callbackContext,int uploadType, int status,
                                 String message, long nowSize, long totalSize){
        PluginResult pluginResult;
        JSONArray array = new JSONArray();
        try {
            array.put(0, uploadType);
            array.put(1, status);
            array.put(2, message);
            array.put(3, nowSize);
            array.put(4, totalSize);
            Log.d(TAG,"返回的uploadType="+ uploadType + "_status="+ status + "_message=" + message);
        } catch (Exception e){
            e.printStackTrace();
        }
        if (status == NET_NOT_AVAILABLE||
                status == UPLOAD_FAILE||
                status == FILE_NO_EXIST||
                status == DOWNLOAD_FAILE||
                status == CREATE_FAILE) {
            pluginResult = new PluginResult(PluginResult.Status.ERROR, array);
            pluginResult.setKeepCallback(false);
        } else {
            pluginResult = new PluginResult(PluginResult.Status.OK, array);
            pluginResult.setKeepCallback(
                status == DELETE_FINISH ||
                status == UPLOAD_SUCCESS ||
                status == DOWNLOAD_SUCCESS ||
                status == CREATE_SUCCESS ? false : true);
        }

        callbackContext.sendPluginResult(pluginResult);
    }

    public void sendDownloadBreakPointResult(int uploadType, int status, String message, String url,
                                             boolean keep, CallbackContext callBack) {
        sendDownloadBreakPointResult(uploadType, status, message, url, "", keep, callBack);
    }

    public void sendDownloadBreakPointResult(int uploadType, int status, String message, String url,
                                             String length, boolean keep, CallbackContext callBack) {
        PluginResult pluginResult;
        JSONArray array = new JSONArray();
        try {
            array.put(0, uploadType);
            array.put(1, status);
            array.put(2, message);
            array.put(3, url);
            array.put(4, length);
            if (status == STATUS_START) {
                array.put(5, delay);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        PluginResult.Status st =
            status == STATUS_NO_START ||
            status == STATUS_START ||
            status == STATUS_LOADING ||
            status == STATUS_FINISH ||
            status == STATUS_STOP? PluginResult.Status.OK : PluginResult.Status.ERROR;
        pluginResult = new PluginResult(st, array);
        pluginResult.setKeepCallback(keep);

        callBack.sendPluginResult(pluginResult);
    }

    private final static int HANDLER_CREATE_FILE = 0;
    private Handler mHandler = new Handler(){
        @Override
        public void dispatchMessage(Message msg) {
            try {
                if (msg.what == HANDLER_CREATE_FILE) {
                    Bundle bundle = msg.getData();
                    sendUpdateResult(callbackContext,
                        bundle.getInt("uploadType"),
                        bundle.getInt("status"),
                        bundle.getString("message"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public interface UploadListener{
        void sendUpdateResult(int uploadType, int status, String message);
        void sendUpdateResult(int uploadType, int status, String message, long nowSize, long totalSize);
    }
}
