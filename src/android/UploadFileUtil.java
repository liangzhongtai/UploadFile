package com.chinamobile.upload;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.cookie.CookieJarImpl;
import com.lzy.okgo.cookie.store.MemoryCookieStore;
import com.lzy.okgo.model.HttpHeaders;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.request.PostRequest;
import com.lzy.okgo.request.base.Request;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * Created by liangzhongtai on 2018/6/7.
 */

public class UploadFileUtil {
    private volatile static UploadFileUtil uniqueInstance;
    private Context mContext;
    public static UploadFile.UploadListener listener;
    public int quality = 100;

    private UploadFileUtil(Context context) {
        mContext = context;
        initOkHttp();
    }


    //采用Double CheckLock(DCL)实现单例
    public static UploadFileUtil getInstance(Context context) {
        if (uniqueInstance == null) {
            synchronized (UploadFileUtil.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new UploadFileUtil(context);
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


    public static SSLContext setCard(InputStream certificate) {
        SSLContext sslContext = null;
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            String certificateAlias = Integer.toString(0);
            keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate));
            sslContext = SSLContext.getInstance("TLS");
            final TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            sslContext.init
                    (
                            null,
                            trustManagerFactory.getTrustManagers(),
                            new SecureRandom()
                    );
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } finally {
            return sslContext;
        }
    }

    //执行上传请求
    private long distance;
    public static int interval = 100;
    public void upload(final int uploadType, final String url, final String[] keys, final List values, final boolean base64){
        Log.d(UploadFile.TAG,"url="+url);
        Log.d(UploadFile.TAG,"keys.count="+keys.length);
        Log.d(UploadFile.TAG,"values.count="+values.size());
        new Thread(new Runnable() {
            @Override
            public void run() {
                Request request;
                Response response;
                request = OkGo.post(url);
                boolean hasFile = false;
                distance = System.currentTimeMillis();
                for (int i = 0,len = keys.length; i < len; i++) {
                    Log.d(UploadFile.TAG, "key= " + keys[i] + getHolder(keys[i]
                            .length())+ "value= " + values.get(i));
                    if(file(values.get(i))){
                        Log.d(UploadFile.TAG,"单file");
                        hasFile = true;
                        if(base64){
                            String litpic = encodeBitmapForBase64(values.get(i)+"",quality);
                            request.params(keys[i], litpic);
                        }else{
                            ((PostRequest)request).params(keys[i],new File((String) values.get(i)));
                        }
                    }else if(fileList(values.get(i))){
                        Log.d(UploadFile.TAG,"多files");
                        hasFile = true;
                        boolean error = false;
                        List<File> files = new ArrayList<File>();
                        List<String> paths = (List<String>) values.get(i);
                        for (int j = 0,jLen = paths.size();j<jLen;j++){
                            File file = new File(paths.get(j));
                            if(file.exists()) {
                                files.add(file);
                            }else {
                                error = true;
                                Message message = new Message();
                                message.what = HANDLER_FILE_NO_EXIT;
                                message.obj = new Object[]{uploadType,"第"+j+"个文件:"+paths.get(j)+"未能在本地存储找到"};
                                mHandler.sendMessage(message);
                                break;
                            }
                        }
                        if(error){
                            break;
                        }
                        ((PostRequest)request).addFileParams(keys[i], files);
                    }else {
                        request.params(keys[i],values.get(i)+"");
                    }
                }
                try {
                    if(hasFile) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.put("Content-Type", "multipart/form-data");
                        request.headers(headers);
                        //response = request.tag(url).execute();
                        request.tag(url).execute(new StringCallback() {
                            @Override
                            public void onSuccess(com.lzy.okgo.model.Response<String> response) {
                                Log.d(UploadFile.TAG,"上传成功_="+response.body().toString());
                                sendMessage(HANDLER_FINISH,new Object[]{uploadType,response.body().toString()});
                            }

                            @Override
                            public void uploadProgress(Progress progress) {
                                super.uploadProgress(progress);
                                Log.d(UploadFile.TAG,"sice="+progress.currentSize);
                                long nowTime = System.currentTimeMillis();
                                Log.d(UploadFile.TAG,"nowTime-distance="+(nowTime-distance));
                                if(nowTime-distance<interval){
                                    return;
                                }
                                distance = nowTime;
                                sendMessage(HANDLER_PROGRESS,new Object[]{uploadType,progress});
                            }

                            @Override
                            public void onError(com.lzy.okgo.model.Response<String> response) {
                                super.onError(response);
                                sendMessage(HANDLER_ERROR,new Object[]{uploadType,"上传失败"});
                            }
                        });
                    }else {
                        response = request.tag(url).execute();
                        if(response.code()==200){
                            Log.d(UploadFile.TAG,"上传成功_="+response.body().string());
                            sendMessage(HANDLER_FINISH,new Object[]{uploadType,response.body().string()});
                        }else{
                            Log.d(UploadFile.TAG,"上传失败_="+response.body().string());
                            sendMessage(HANDLER_ERROR,new Object[]{uploadType,"上传失败"});
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    sendMessage(HANDLER_ERROR,new Object[]{uploadType,"POST参数格式化异常"});
                }
            }
        }).start();
    }


    //取消上传请求
    public void cancelUpload(String url) {
        OkGo.getInstance().cancelTag(url);
    }

    public void cancelAll(){
        OkGo.getInstance().cancelAll();
    }


    /**
     * 网络是否可用
     * @return
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static String getHolder(int length) {
        String  holder= "";
        for(int i=0;i<(20-length);i++){
            holder += "_";
        }
        return holder;
    }


    public boolean file(Object requValue) {
        if(requValue!=null&&requValue instanceof String){
            File file = new File((String) requValue);
            if (file.exists()) {
                return true;
            }
        }
        return false;
    }
    public boolean fileList(Object requValue) {
        if(requValue!=null&&requValue instanceof List){
            List list = (List) requValue;
            if(list.size()>0&&file(list.get(0))){
                return true;
            }
        }
        return false;
    }

    public static String encodeBitmapForBase64(String path,int quality) {
        if(TextUtils.isEmpty(path))return "";
        ByteArrayOutputStream baos = null;
        byte[] buffer = null;
        try {
            baos = new ByteArrayOutputStream();
            Bitmap bitmap = decodeSampleBitmap(path);
            if(path.endsWith("jpeg")||path.endsWith("jpg")){
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality,baos);
            }else if(path.endsWith("webp")){
                bitmap.compress(Bitmap.CompressFormat.WEBP, quality,baos);
            }else{
                bitmap.compress(Bitmap.CompressFormat.PNG,  quality,baos);
            }
            baos.close();
            buffer = baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(baos!=null) try {
                baos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(buffer==null)return "";
            return Base64.encodeToString(buffer,0,buffer.length,Base64.DEFAULT);
        }
    }

    public static Bitmap decodeSampleBitmap(String path){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path,options);
        options.inSampleSize = 1;
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path,options);
    }

    private void sendMessage(int what,Object obj){
        Message message = new Message();
        message.what = what;
        message.obj = obj;
        mHandler.sendMessage(message);
    }

    private final static int HANDLER_FINISH = 0;
    private final static int HANDLER_ERROR  = 1;
    private final static int HANDLER_FILE_NO_EXIT = 2;
    private final static int HANDLER_PROGRESS = 3;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Object[] objs = (Object[]) msg.obj;
            if(msg.what == HANDLER_ERROR){
                if(listener!=null)listener.sendUpdateResult((Integer) objs[0],UploadFile.UPLOAD_FAILE,(String) objs[1]);
            }else if(msg.what == HANDLER_FINISH){
                if(listener!=null){
                    listener.sendUpdateResult((Integer)objs[0],UploadFile.UPLOAD_PROGRESS,"1.00");
                    listener.sendUpdateResult((Integer)objs[0],UploadFile.UPLOAD_SUCCESS,(String) objs[1]);
                }
            }else if(msg.what == HANDLER_FILE_NO_EXIT){
                if(listener!=null)listener.sendUpdateResult((Integer)objs[0],UploadFile.FILE_NO_EXIST,(String) objs[1]);
            }else if(msg.what == HANDLER_PROGRESS){
                Progress progress = (Progress) objs[1];
                //Log.d(UploadFile.TAG,"progress="+((float)progress.currentSize/progress.totalSize));
                //Toast.makeText(mContext,"已上传:" + progress.currentSize/1024/1024 + "MB, 共" + progress.totalSize/1024/1024 + "MB;",Toast.LENGTH_SHORT);
                if(listener!=null)listener.sendUpdateResult((Integer)objs[0],UploadFile.UPLOAD_PROGRESS,((float)progress.currentSize/progress.totalSize) +"");
            }
        }
    };

}
