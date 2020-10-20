package com.chinamobile.upload;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.chinamobile.cache.Caches;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.cookie.CookieJarImpl;
import com.lzy.okgo.cookie.store.MemoryCookieStore;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.request.base.Request;

import org.apache.commons.io.FileUtils;
import org.apache.cordova.CallbackContext;
import org.json.JSONObject;

import java.io.File;
import okhttp3.OkHttpClient;

/**
 * Created by liangzhongtai on 2019/9/27.
 */

public class DownloadFileUtil {
    private volatile static DownloadFileUtil uniqueInstance;
    private Context mContext;

    private DownloadFileUtil(Context context) {
        mContext = context;
        initOkHttp();
    }

    public static DownloadFileUtil getInstance(Context context) {
        if (uniqueInstance == null) {
            synchronized (DownloadFileUtil.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new DownloadFileUtil(context);
                }
            }
        }
        return uniqueInstance;
    }

    //初始化okhttp
    private void initOkHttp() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.cookieJar(new CookieJarImpl(new MemoryCookieStore()));
        //设置SSL证书
        /*try {
            builder.sslSocketFactory(setCard(mContext.getAssets().open("xxxx.crt")).getSocketFactory())
                    .hostnameVerifier((hostname, session) -> true)
            ;
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        //必须调用初始化
        OkGo.getInstance().init(((Activity)mContext).getApplication())
                //建议设置OkHttpClient，不设置将使用默认的
                .setOkHttpClient(builder.build())
                //全局统一缓存模式，默认不使用缓存，可以不传
                .setCacheMode(CacheMode.NO_CACHE)
                //全局统一缓存时间，默认永不过期，可以不传
                //.setCacheTime(CacheEntity.CACHE_NEVER_EXPIRE)
                //全局统一超时重连次数，默认为三次，那么最差的情况会请求4次(一次原始请求，三次重连请求)，不需要可以设置为0
                .setRetryCount(3);
    }

    /**
     * 下载
     * */
    public void download(CallbackContext callbackContext, UploadFile plugin, final int uploadType,
                         final String url, final String fileName, final  String dirName) {
        String dir = Environment.getExternalStorageDirectory()
                + (TextUtils.isEmpty(dirName) ? "" : ("/" + dirName));
        long startTime = System.currentTimeMillis();
        File file = new File(dir, fileName);
        if (file.exists()) {
            file.delete();
        }
        Log.d(UploadFile.TAG, url);
        OkGo.<File>get(url).tag(url).execute(new FileCallback(dir, fileName) {
            @Override
            public void onStart(Request<File, ? extends Request> request) {
                super.onStart(request);
                Log.d(UploadFile.TAG, "开始下载文件");
                plugin.sendUpdateResult(callbackContext, uploadType, UploadFile.DOWNLOAD_START, System.currentTimeMillis() - startTime + "");
            }

            @Override
            public void onSuccess(com.lzy.okgo.model.Response<File> response) {
                Log.d(UploadFile.TAG, "下载文件成功length="+response.body().length());
                // mBasePath=response.body().getAbsolutePath();
                plugin.sendUpdateResult(callbackContext, uploadType, UploadFile.DOWNLOAD_SUCCESS, "下载成功");
            }

            @Override
            public void onFinish() {
                super.onFinish();
                Log.d(UploadFile.TAG, "下载文件完成");
                // SPUtils.getInstance().put("localPath", mBasePath);
            }

            @Override
            public void onError(com.lzy.okgo.model.Response<File> response) {
                super.onError(response);
                Log.d(UploadFile.TAG, "下载文件出错error="+response.message());
                plugin.sendUpdateResult(callbackContext, uploadType, UploadFile.DOWNLOAD_FAILE, "下载失败");
            }

            @Override
            public void downloadProgress(Progress progress) {
                super.downloadProgress(progress);
                float dLProgress = progress.fraction;
                Log.d(UploadFile.TAG, "文件下载的进度="+dLProgress);
                plugin.sendUpdateResult(callbackContext, uploadType, UploadFile.DOWNLOAD_PROGRESS, dLProgress + "");
            }
        });
        Log.d(UploadFile.TAG, "启动请求");
    }


    /**
     * 保存JSONObject
     * */
    public static void writeObject(String fileName, String dirName, JSONObject obj) {
        String path = Environment.getExternalStorageDirectory() + (TextUtils.isEmpty(dirName) ? "/" : ("/" + dirName + "/")) + fileName;
        File dir = new File(Environment.getExternalStorageDirectory() +
                (TextUtils.isEmpty(dirName) ? "" : ("/" + dirName)));
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        } else if(!dir.exists()) {
            dir.mkdirs();
        }
        Log.d(Caches.TAG,"path="+path);
        Log.d(Caches.TAG,"data=" + obj.toString());
        try {
            FileUtils.writeStringToFile(file, obj.toString(), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读JSONObject
     * */
    public static JSONObject readObject(String fileName, String dirName) {
        String path = Environment.getExternalStorageDirectory() + (TextUtils.isEmpty(dirName) ? "/" : ("/" + dirName + "/")) + fileName;
        File file = new File(path);
        JSONObject obj = new JSONObject();
        try {
            String string = FileUtils.readFileToString(file);
            obj = new JSONObject(string);
            Log.d(Caches.TAG,"path=" + path);
            Log.d(Caches.TAG,"data=" + string);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }
}
