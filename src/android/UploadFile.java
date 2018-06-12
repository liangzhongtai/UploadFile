package com.chinamobile.upload;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;


import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by liangzhongtai on 2018/5/17.
 */

public class UploadFile extends CordovaPlugin{
    public final static String TAG = "Upload_Plugin";
    public final static int RESULTCODE_PERMISSION = 20;

    //上传
    public final static int UPLOAD_DEFAULT = 0;
    //取消上传
    public final static int CANCEL         = 1;
    //取消所有上传
    public final static int CANCEL_ALL     = 2;

    //网络不可用
    public final static int NET_NOT_AVAILABLE = 1;
    //文件上传失败
    public final static int UPLOAD_FAILE      = 2;
    //文件上传成功
    public final static int UPLOAD_SUCCESS    = 3;
    //文件路径不存在
    public final static int FILE_NO_EXIST     = 4;
    //上传进度
    public final static int UPLOAD_PROGRESS   = 5;

    //文件上传类型
    public int uploadType;
    //文件上传格式
    public boolean base64;
    //文件的上传链接api
    public String url;/*
    //文件的上传路径数组
    public List<String> filePaths;*/
    public String[] keys;
    public List values;
    public CordovaInterface cordova;
    public CordovaWebView webView;
    public boolean first = true;
    private CallbackContext callbackContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.cordova = cordova;
        this.webView = webView;
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        Log.d(TAG,"执行方法updateapp");
        Log.d(TAG,"length="+args.length());
        if("coolMethod".equals(action)){
            uploadType = args.getInt(0);
            if(args.length()>1&&args.length()%2==0){
                url = args.getString(1);
                JSONArray keysJA = args.getJSONArray(2);
                JSONArray valuesJA = args.getJSONArray(3);
                keys = new String[keysJA.length()];
                values = new ArrayList();
                for (int i=0,len = keysJA.length();i<len;i++ ){
                    keys[i] = keysJA.getString(i);
                }
                for (int i=0,len = valuesJA.length();i<len;i++){
                    Object value = valuesJA.get(i);
                    if(value instanceof JSONArray){
                        JSONArray fpJArray = valuesJA.getJSONArray(i);
                        if(fpJArray==null)break;
                        List<String> filePaths = new ArrayList<String>();
                        for (int j=0,jLen = fpJArray.length();j<jLen;j++){
                            filePaths.add(fpJArray.getString(j));
                        }
                        values.add(filePaths);
                    }else if(value instanceof String){
                        values.add(valuesJA.getString(i));
                    }else  if (value instanceof Integer) {
                        values.add(valuesJA.getInt(i));
                    }else if(value instanceof Boolean){
                        values.add(valuesJA.getBoolean(i));
                    }else if(value instanceof Float || value instanceof Double){
                        values.add(valuesJA.getDouble(i));
                    }else if (value instanceof Long){
                        values.add(valuesJA.getLong(i));
                    }else if(value instanceof JSONObject){
                        values.add(valuesJA.getJSONObject(i));
                    }else {
                        values.add(valuesJA.get(i));
                    }
                }
            }
            //权限
            try {
                if(!PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    ||!PermissionHelper.hasPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ||!PermissionHelper.hasPermission(this,Manifest.permission.ACCESS_NETWORK_STATE)) {
                    PermissionHelper.requestPermissions(this,RESULTCODE_PERMISSION,new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.ACCESS_NETWORK_STATE
                    });
                }else{
                    startWork();
                }
            }catch (Exception e){
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

    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
    }


    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                callbackContext.error("缺少权限,无法使用app下载功能");
                return;
            }
        }
        switch (requestCode) {
            case RESULTCODE_PERMISSION:
                startWork();
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UploadFileUtil.getInstance(cordova.getActivity()).cancelUpload(url);
    }


    private void startWork() {
        if(uploadType == CANCEL){
            UploadFileUtil.getInstance(cordova.getActivity()).cancelUpload(url);
            return;
        }else if(uploadType == CANCEL_ALL){
            UploadFileUtil.getInstance(cordova.getActivity()).cancelAll();
            return;
        }

        if(!UploadFileUtil.isNetworkAvailable(cordova.getActivity())){
            sendUpdateResult(uploadType,NET_NOT_AVAILABLE,"网络不可用,请检查网络");
            return;
        }
        UploadFileUtil.getInstance(cordova.getActivity()).listener = new UploadListener() {
            @Override
            public void sendUpdateResult(int uploadType, int status, String message) {
                UploadFile.this.sendUpdateResult(uploadType,status,message);
            }
        };
        UploadFileUtil.getInstance(cordova.getActivity()).upload(uploadType,url,keys,values,base64);
    }

    public void sendUpdateResult(int uploadType,int status,String message){
        PluginResult pluginResult;
        JSONArray array = new JSONArray();
        try {
            array.put(0, uploadType);
            array.put(1, status);
            array.put(2, message);

        }catch (Exception e){
            e.printStackTrace();
        }
        if(status == NET_NOT_AVAILABLE||status == UPLOAD_FAILE) {
            pluginResult = new PluginResult(PluginResult.Status.ERROR, array);
        }else {
            pluginResult = new PluginResult(PluginResult.Status.OK, array);
        }
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }

    public interface UploadListener{
        void sendUpdateResult(int uploadType, int status, String message);
    }
}
