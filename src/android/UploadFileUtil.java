package com.chinamobile.upload;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v4.BuildConfig;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.cookie.CookieJarImpl;
import com.lzy.okgo.cookie.store.MemoryCookieStore;
import com.lzy.okgo.interceptor.HttpLoggingInterceptor;
import com.lzy.okgo.model.HttpHeaders;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.request.PostRequest;
import com.lzy.okgo.request.base.Request;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okio.Buffer;


/**
 * Created by liangzhongtai on 2018/6/7.
 */

public class UploadFileUtil {
    private volatile static UploadFileUtil uniqueInstance;
    private Context mContext;
    public UploadFile.UploadListener listener;
    public int quality = 100;
    public boolean hasStart = false;
    //执行上传请求
    private long distance;
    public static int interval = 100;

    private UploadFileUtil(Context context) {
        mContext = context;
        initOkHttp();
    }

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

    /**
     * 初始化okhttp
     * */
    private void initOkHttp() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.cookieJar(new CookieJarImpl(new MemoryCookieStore()));
        builder.addInterceptor(new HttpLoggingInterceptor("OkGo"));
        //设置SSL证书
        /*try {
            builder.sslSocketFactory(setCard(mContext.getAssets().open("xxxx.crt")).getSocketFactory())
                    .hostnameVerifier((hostname, session) -> true)
            ;
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        //必须调用初始化
        OkGo.getInstance()
            .init(((Activity)mContext).getApplication())
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
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            return sslContext;
        }
    }


    public void upload(final int uploadType, final String url, final String[] keys, final List values, final boolean base64){
        Log.d(UploadFile.TAG,"url="+url);
        Log.d(UploadFile.TAG,"keys.count="+keys.length);
        Log.d(UploadFile.TAG,"values.count="+values.size());
        new Thread(new Runnable() {
            @Override
            public void run() {
                Request request;
                Response response;
                if(uploadType < UploadFile.GET) {
                    request = OkGo.post(url);
                } else {
                    request = OkGo.get(url);
                }
                boolean hasFile = false;
                distance = System.currentTimeMillis();
                for (int i = 0, len = keys.length; i < len; i++) {
                    String key = keys[i];
                    Object value = values.get(i);
                    Log.d(UploadFile.TAG, "key= " + key + getHolder(keys[i]
                            .length())+ getHolder(15) + "type= " + formatType(value)+"-----value= " + value);
                    if (file(value)) {
                        Log.d(UploadFile.TAG,"单file");
                        hasFile = true;
                        if (base64) {
                            Log.d(UploadFile.TAG, "Base64");
                            if (isNetFile(value)) {
                                request.params(key, "");
                            } else {
                                String litpic = encodeBitmapForBase64(value + "", quality);
                                request.params(key, litpic);
                            }
                        } else {
                            try {
                                Log.d(UploadFile.TAG, "二进制");
                                // 网络地址，生成空文件
                                if (isNetFile(value)) {
                                    String[] nArr = ((String)value).split("/");
                                    String fileName = nArr[nArr.length-1];
                                    File file = new File(Environment.getExternalStorageDirectory() + "/DCIM/", fileName);
                                    if (!file.exists()) {
                                       file.createNewFile();
                                    }
                                    ((PostRequest) request).params(key, file);
                                } else {
                                    ((PostRequest) request).params(key, new File((String) value));
                                }
                            } catch (IOException e) {
                                Log.d(UploadFile.TAG,e.toString());
                            }
                        }
                    } else if (fileList(value)) {
                        Log.d(UploadFile.TAG,"多files");
                        try {
                            hasFile = true;
                            boolean error = false;
                            List<File> files = new ArrayList<File>();
                            List<String> paths = (List<String>)value;
                            for (int j = 0,jLen = paths.size();j<jLen;j++){
                                String path = paths.get(j);
                                //网络地址，生成空文件
                                if (isNetFile(path)) {
                                    String[] nArr = path.split("/");
                                    String fileName = nArr[nArr.length-1];
                                    File file = new File(Environment.getExternalStorageDirectory() + "/DCIM/", fileName);
                                    if (!file.exists()) {
                                        try {
                                            file.createNewFile();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    ((PostRequest) request).params(key, file);
                                }else {
                                    File file = new File(path);
                                    if (file.exists()) {
                                        files.add(file);
                                    } else {
                                        error = true;
                                        Message message = new Message();
                                        message.what = HANDLER_FILE_NO_EXIT;
                                        message.obj = new Object[]{uploadType, "第" + j + "个文件:" + path + "未能在本地存储找到"};
                                        mHandler.sendMessage(message);
                                        break;
                                    }
                                }
                            }
                            if (error) {
                                break;
                            }
                            ((PostRequest)request).addFileParams(keys[i], files);
                        } catch (Exception e) {
                            Log.d(UploadFile.TAG,e.toString());
                        }
                    } else {
                        request.params(keys[i],values.get(i)+"");
                    }
                }
                try {
                    if (hasFile) {
                        long startTime = System.currentTimeMillis();
                        hasStart = true;
                        HttpHeaders headers = new HttpHeaders();
                        headers.put("Content-Type", "multipart/form-data");
                        request.headers(headers);
                        Log.d(UploadFile.TAG, "requestbody=" + requestBodyToString((PostRequest) request));
                        request.tag(url).execute(new StringCallback() {
                            @Override
                            public void onSuccess(com.lzy.okgo.model.Response<String> response) {
                                String body = "";
                                if(response!=null&&response.body()!=null) {
                                    body = response.body().toString();
                                    Log.d(UploadFile.TAG, "上传成功_=" + body);
                                }
                                sendMessage(HANDLER_FINISH,new Object[]{uploadType,(response!=null&&response.body()!=null)?body:""});
                            }

                            @Override
                            public void uploadProgress(Progress progress) {
                                super.uploadProgress(progress);
                                Log.d(UploadFile.TAG,"size/byte="+progress.currentSize);
                                long nowTime = System.currentTimeMillis();
                                Log.d(UploadFile.TAG,"nowTime-distance=" + (nowTime-distance));
                                if (uploadType == UploadFile.UPLOAD_NO_PROGRESS) {
                                    return;
                                }
                                if (hasStart) {
                                    hasStart = false;
                                    sendMessage(HANDLER_DELAY, new Object[]{uploadType, nowTime - startTime, progress});
                                }
                                if (nowTime - distance < interval) {
                                    return;
                                }
                                distance = nowTime;
                                sendMessage(HANDLER_PROGRESS, new Object[]{uploadType, progress});
                            }

                            @Override
                            public void onError(com.lzy.okgo.model.Response<String> response) {
                                super.onError(response);
                                String body = "";
                                if (response.body() != null) {
                                    body = response.body().toString();
                                    Log.d(UploadFile.TAG, "上传失败_=" + body);
                                }
                                sendMessage(HANDLER_ERROR,new Object[]{uploadType,response.body()!=null?body:"上传失败"});
                            }
                        });
                    } else {
                        Log.d(UploadFile.TAG,"没有文件,使用同步请求");
                        response = request.tag(url).execute();
                        String body = "";
                        if(response.code() == 200){
                            if (response.body() != null) {
                                body = response.body().string();
                                Log.d(UploadFile.TAG, "上传成功_=" + body);
                            }
                            sendMessage(HANDLER_FINISH,new Object[]{uploadType, body});
                        } else {
                            if (response.body() != null) {
                                body = response.body().string();
                                Log.d(UploadFile.TAG,"上传失败_="+body);
                            }
                            sendMessage(HANDLER_ERROR,new Object[]{uploadType, response.body()!=null? body : "上传失败"});
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(UploadFile.TAG,"异常e="+e.toString());
                    sendMessage(HANDLER_ERROR,new Object[]{uploadType,"POST参数格式化异常"});
                }
            }
        }).start();
    }

    private String formatType(Object o) {
        if(o == null) {
            return "Object";
        } else if (o instanceof String) {
            return "String";
        } else if (o instanceof Number) {
            return "Number";
        } else if (o instanceof Boolean) {
            return "Boolea";
        }
        return "Object";
    }

    /**
     * 取消上传请求
     * */
    public void cancelUpload(String url) {
        OkGo.getInstance().cancelTag(url);
    }

    public void cancelAll() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
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
        String holder= "";
        for(int i = 0; i < (20 - length); i++) {
            holder += "_";
        }
        return holder;
    }

    public boolean file(Object requValue) {
        if (requValue!=null&&requValue instanceof String) {
            if (isNetFile(requValue)){
                return true;
            } else {
                File file = new File((String) requValue);
                if (file.exists()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isNetFile(Object requValue) {
        if (((String) requValue).startsWith("http") &&
            (((String) requValue).endsWith(".png") ||
            ((String) requValue).endsWith(".jpg") ||
            ((String) requValue).endsWith(".jpeg") ||
            ((String) requValue).endsWith(".gif") ||
            ((String) requValue).endsWith(".txt") ||
            ((String) requValue).endsWith(".zip") ||
            ((String) requValue).endsWith(".rar") ||
            ((String) requValue).endsWith(".pdf") ||
            ((String) requValue).endsWith(".doc") ||
            ((String) requValue).endsWith(".docx") ||
            ((String) requValue).endsWith(".xlxs") ||
            ((String) requValue).endsWith(".lte") ||
            ((String) requValue).endsWith(".json") ||
            ((String) requValue).endsWith(".xml") ||
            ((String) requValue).endsWith(".excel"))) {
            return true;
        } else {
            return false;
        }
    }

    public boolean fileList(Object requValue) {
        if (requValue != null && requValue instanceof List) {
            List list = (List) requValue;
            if (list.size() > 0 && file(list.get(0))) {
                return true;
            }
        }
        return false;
    }

    public static String encodeBitmapForBase64(String path,int quality) {
        if (TextUtils.isEmpty(path)) {
            return "";
        }
        ByteArrayOutputStream baos = null;
        byte[] buffer = null;
        try {
            baos = new ByteArrayOutputStream();
            Bitmap bitmap = decodeSampleBitmap(path);
            if (path.endsWith("jpeg") || path.endsWith("jpg")) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality,baos);
            } else if (path.endsWith("webp")) {
                bitmap.compress(Bitmap.CompressFormat.WEBP, quality,baos);
            } else {
                bitmap.compress(Bitmap.CompressFormat.PNG,  quality,baos);
            }
            baos.close();
            buffer = baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (baos!=null) {
                try {
                    baos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (buffer==null) {
                return "";
            }
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
    private final static int HANDLER_DELAY = 4;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try{
                Object[] objs = (Object[]) msg.obj;
                if (msg.what == HANDLER_ERROR) {
                    if(listener!=null){
                        listener.sendUpdateResult((Integer) objs[0],UploadFile.UPLOAD_FAILE,(String) objs[1]);
                    }
                } else if (msg.what == HANDLER_FINISH){
                    if(listener!=null){
                        Log.d(UploadFile.TAG,"成功body="+objs[1]);
                        if((Integer)objs[0]==UploadFile.UPLOAD_DEFAULT) {
                            listener.sendUpdateResult((Integer) objs[0], UploadFile.UPLOAD_PROGRESS,
                                    "1.00", 0, 0);
                        }
                        listener.sendUpdateResult((Integer)objs[0],UploadFile.UPLOAD_SUCCESS,(String) objs[1]);
                    }
                } else if (msg.what == HANDLER_FILE_NO_EXIT) {
                    if(listener!=null){
                        listener.sendUpdateResult((Integer)objs[0],UploadFile.FILE_NO_EXIST,(String) objs[1]);
                    }
                } else if (msg.what == HANDLER_PROGRESS){
                    Progress progress = (Progress) objs[1];
                    Log.d(UploadFile.TAG,"已上传:" + progress.currentSize/1024 + "kb, 共" + progress.totalSize/1024 + "kb;");
                    if (listener!=null) {
                        listener.sendUpdateResult((Integer)objs[0],UploadFile.UPLOAD_PROGRESS,
                                ((float)progress.currentSize/progress.totalSize) +"",
                                progress.currentSize/1024, progress.totalSize/1024);
                    }
                } else if (msg.what == HANDLER_DELAY) {
                    if (listener!=null) {
                        long delay = (long) objs[1];
                        Progress progress = (Progress) objs[2];
                        listener.sendUpdateResult((Integer)objs[0], UploadFile.UPLOAD_START, delay +"");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                UploadFileUtil.this.sendMessage(HANDLER_ERROR,new Object[]{UploadFile.UPLOAD_DEFAULT,"POST参数格式化异常"});
            }
        }
    };

    public static String getMIMETypeString(File file) {
        String type = "*/*";
        String strings[] = file.getPath().split("/");
        String fName = strings[strings.length - 1];
        // 获取后缀名前的分隔符"."在fName中的位置。
        int dotIndex = fName.lastIndexOf(".");
        if (dotIndex < 0) {
            return type;
        }
        dotIndex++;
        /* 获取文件的后缀名 */
        String end = fName.substring(dotIndex, fName.length()).toLowerCase();
        Log.d(UploadFile.TAG, "fName = " + fName + " dotIndex = " + dotIndex + " end = " + end);
        if (TextUtils.isEmpty(end)) {
            return type;
        }
        // 在MIME和文件类型的匹配表中找到对应的MIME类型。
        for (int i = 0; i < MIME_MapTable.length; i++) {
            if (end.equals(MIME_MapTable[i][0])) {
                type = MIME_MapTable[i][1];
            }
        }
        return type;
    }

    /**
     * 打开文件
     *
     * @param file
     */
    public static void openFile(Context context, File file) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // 判断版本大于等于7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }
        String type = getMIMETypeString(file);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            //设置intent的Action属性
            intent.setAction(Intent.ACTION_VIEW);
            uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            Log.d(UploadFile.TAG, "跳转到预览页面uri=" + uri.toString() + "_type=" + type);
            //设置intent的data和Type属性。
            intent.setDataAndType(uri, type);
        } else {
            uri = Uri.fromFile(file);
            intent.setDataAndType(uri, type);
        }

        // 8.0安装解析包出错
        // 查询所有符合 intent 跳转目标应用类型的应用，注意此方法必须放置在 setDataAndType 方法之后
        List<ResolveInfo> resolveLists = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        // 然后全部授权
        for (ResolveInfo resolveInfo : resolveLists) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }

        //跳转
        context.startActivity(intent);
    }

    public static final String[][] MIME_MapTable = {
            //{后缀名，MIME类型}
            {"pbm", "image/x-portable-bitmap"},
            {"pcx", "image/x-pcx"},
            {"nbmp", "image/nbmp"},
            {"pda", "image/x-pda"},
            {"pgm", "image/x-portable-graymap"},
            {"pict", "image/x-pict"},
            {"png", "image/png"},
            {"pnm", "image/x-portable-anymap"},
            {"pnz", "image/png"},
            {"ppm", "image/x-portable-pixmap"},
            {"nokia-op-logo", "image/vnd.nok-oplogo-color"},
            {"qti", "image/x-quicktime"},
            {"qtif", "image/x-quicktime"},
            {"ras", "image/x-cmu-raster"},
            {"rf", "image/vnd.rn-realflash"},
            {"rp", "image/vnd.rn-realpix"},
            {"rgb", "image/x-rgb"},
            {"si9", "image/vnd.lgtwap.sis"},
            {"si7", "image/vnd.stiwap.sis"},
            {"svf", "image/vnd"},
            {"svg", "image/svg-xml"},
            {"svh", "image/svh"},
            {"si6", "image/si6"},
            {"tif", "image/tiff"},
            {"tiff", "image/tiff"},
            {"toy", "image/toy"},
            {"wbmp", "image/vnd.wap.wbmp"},
            {"wi", "image/wavelet"},
            {"wpng", "image/x-up-wpng"},
            {"xbm", "image/x-xbitmap"},
            {"xpm", "image/x-xpixmap"},
            {"xwd", "image/x-xwindowdump"},
            {"fh4", "image/x-freehand"},
            {"fh5", "image/x-freehand"},
            {"fhc", "image/x-freehand"},
            {"fif", "image/fif"},
            {"bmp", "image/bmp"},
            {"cal", "image/x-cals"},
            {"cod", "image/cis-cod"},
            {"fpx", "image/x-fpx"},
            {"dcx", "image/x-dcx"},
            {"eri", "image/x-eri"},
            {"gif", "image/gif"},
            {"ief", "image/ief"},
            {"ifm", "image/gif"},
            {"ifs", "image/ifs"},
            {"j2k", "image/j2k"},
            {"jpe", "image/jpeg"},
            {"jpeg", "image/jpeg"},
            {"jpg", "image/jpeg"},
            {"jpz", "image/jpeg"},
            {"mil", "image/x-cals"},

            {"3gp", "video/3gpp"},
            {"asf", "video/x-ms-asf"},
            {"asx", "video/x-ms-asf"},
            {"avi", "video/x-msvideo"},
            {"fvi", "video/isivideo"},
            {"lsf", "video/x-ms-asf"},
            {"lsx", "video/x-ms-asf"},
            {"m4u", "video/vnd.mpegurl"},
            {"m4v", "video/x-m4v"},
            {"pvx", "video/x-pv-pvx"},
            {"qt", "video/quicktime"},
            {"rv", "video/vnd.rn-realvideo"},
            {"viv", "video/vivo"},
            {"vivo", "video/vivo"},
            {"vdo", "video/vdo"},
            {"wm", "video/x-ms-wm"},
            {"wmx", "video/x-ms-wmx"},
            {"wv", "video/wavelet"},
            {"wvx", "video/x-ms-wvx"},
            {"mov", "video/quicktime"},
            {"movie", "video/x-sgi-movie"},
            {"mp4", "video/mp4"},
            {"mng", "video/x-mng"},
            {"mpe", "video/mpeg"},
            {"mpeg", "video/mpeg"},
            {"mpg video/mpeg"},
            {"mpg4", "video/mp4"},

            {"aif", "audio/x-aiff"},
            {"aifc", "audio/x-aiff"},
            {"aiff", "audio/x-aiff"},
            {"als", "audio/X-Alpha5"},
            {"au", "audio/basic"},
            {"es", "audio/echospeech"},
            {"esl", "audio/echospeech"},
            {"awb", "audio/amr-wb"},
            {"imy", "audio/melody"},
            {"it", "audio/x-mod"},
            {"itz", "audio/x-mod"},
            {"tsi", "audio/tsplayer"},
            {"ult", "audio/x-mod"},
            {"vib", "audio/vib"},
            {"vox", "audio/voxware"},
            {"vqe", "audio/x-twinvq-plugin"},
            {"vqf", "audio/x-twinvq"},
            {"vql", "audio/x-twinvq"},
            {"wav", "audio/x-wav"},
            {"wax", "audio/x-ms-wax"},
            {"wmv", "audio/x-ms-wmv"},
            {"wma", "audio/x-ms-wma"},
            {"xmz", "audio/x-mod"},
            {"m15", "audio/x-mod"},
            {"m3u", "audio/x-mpegurl"},
            {"m3url", "audio/x-mpegurl"},
            {"m4a", "audio/mp4a-latm"},
            {"m4b", "audio/mp4a-latm"},
            {"m4p", "audio/mp4a-latm"},
            {"ma1", "audio/ma1"},
            {"ma2", "audio/ma2"},
            {"ma3", "audio/ma3"},
            {"ma5", "audio/ma5"},
            {"mdz", "audio/x-mod"},
            {"mid", "audio/midi"},
            {"midi", "audio/midi"},
            {"mio", "audio/x-mio"},
            {"mod", "audio/x-mod"},
            {"mp2", "audio/x-mpeg"},
            {"mp3", "audio/x-mpeg"},
            {"mpga", "audio/mpeg"},
            {"ogg", "audio/ogg"},
            {"nsnd", "audio/nsnd"},
            {"pae", "audio/x-epac"},
            {"pac", "audio/x-pac"},
            {"qcp", "audio/vnd.qcelp"},
            {"ra", "audio/x-pn-realaudio"},
            {"ram", "audio/x-pn-realaudio"},
            {"rm", "audio/x-pn-realaudio"},
            {"rmf", "audio/x-rmf"},
            {"rmm", "audio/x-pn-realaudio"},
            {"rmvb", "audio/x-pn-realaudio"},
            {"rpm", "audio/x-pn-realaudio-plugin"},
            {"s3m", "audio/x-mod"},
            {"s3z", "audio/x-mod"},
            {"stm", "audio/x-mod"},
            {"smz", "audio/x-smd"},
            {"snd", "audio/basic"},
            {"smd", "audio/x-smd"},
            {"xm", "audio/x-mod"},

            {"c", "text/plain"},
            {"asc", "text/plain"},
            {"conf", "text/plain"},
            {"cpp", "text/plain"},
            {"css", "text/css"},
            {"dhtml", "text/html"},
            {"etx", "text/x-setext"},
            {"h", "text/plain"},
            {"hdm", "text/x-hdml"},
            {"hdml", "text/x-hdml"},
            {"htm", "text/html"},
            {"html", "text/html"},
            {"hts", "text/html"},
            {"jad", "text/vnd.sun.j2me.app-descriptor"},
            {"java", "text/plain"},
            {"log", "text/plain"},
            {"mel", "text/x-vmel"},
            {"mrl", "text/x-mrml"},
            {"prop", "text/plain"},
            {"r3t", "text/vnd.rn-realtext3d"},
            {"sgm", "text/x-sgml"},
            {"rc", "text/plain"},
            {"rtx", "text/richtext"},
            {"rt", "text/vnd.rn-realtext"},
            {"sgml", "text/x-sgml"},
            {"spc", "text/x-speech"},
            {"txt", "text/plain"},
            {"tsv", "text/tab-separated-values"},
            {"tsv", "text/tab-separated-values"},
            {"talk", "text/x-speech"},
            {"vcf", "text/x-vcard"},
            {"wml", "text/vnd.wap.wml"},
            {"wmls", "text/vnd.wap.wmlscript"},
            {"wmlscript", "text/vnd.wap.wmlscript"},
            {"ws", "text/vnd.wap.wmlscript"},
            {"xml", "text/xml"},
            {"xsit", "text/xml"},
            {"xsl", "text/xml"},
            {"xul", "text/xul"},

            {"apk", "application/vnd.android.package-archive"},

            {"aab", "application/x-authoware-bin"},
            {"aam", "application/x-authoware-map"},
            {"aas", "application/x-authoware-seg"},
            {"ai", "application/postscript"},
            {"amc", "application/x-mpeg"},
            {"ani", "application/octet-stream"},
            {"asd", "application/astound"},
            {"asn", "application/astound"},
            {"asp", "application/x-asap"},
            {"avb", "application/octet-stream"},
            {"bcpio", "application/x-bcpio"},
            {"bin", "application/octet-stream"},
            {"bld", "application/bld"},
            {"bld2", "application/bld2"},
            {"bpk", "application/octet-stream"},
            {"bz2", "application/x-bzip2"},
            {"ccn", "application/x-cnc"},
            {"cco", "application/x-cocoa"},
            {"cdf", "application/x-netcdf"},
            {"chat", "application/x-chat"},
            {"class", "application/octet-stream"},
            {"clp", "application/x-msclip"},
            {"cmx", "application/x-cmx"},
            {"co", "application/x-cult3d-object"},
            {"cpio", "application/x-cpio"},
            {"cpt", "application/mac-compactpro"},
            {"crd", "application/x-mscardfile"},
            {"csh", "application/x-csh"},
            {"cur", "application/octet-stream"},
            {"dcr", "application/x-director"},
            {"dir", "application/x-director"},
            {"dll", "application/octet-stream"},
            {"dmg", "application/octet-stream"},
            {"dms", "application/octet-stream"},
            {"doc", "application/msword"},
            {"docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
            {"dot", "application/x-dot"},
            {"dvi", "application/x-dvi"},
            {"dwg", "application/x-autocad"},
            {"dxf", "application/x-autocad"},
            {"dxr", "application/x-director"},
            {"ebk", "application/x-expandedbook"},
            {"eps", "application/postscript"},
            {"evy", "application/x-envoy"},
            {"exe", "application/octet-stream"},
            {"etc", "application/x-earthtime"},
            {"fm", "application/x-maker"},
            {"gps", "application/x-gps"},
            {"gtar", "application/x-gtar"},
            {"gz", "application/x-gzip"},
            {"gca", "application/x-gca-compressed"},
            {"hdf", "application/x-hdf"},
            {"hlp", "application/winhlp"},
            {"hqx", "application/mac-binhex40"},
            {"ico", "application/octet-stream"},
            {"ins", "application/x-NET-Install"},
            {"ips", "application/x-ipscript"},
            {"ipx", "application/x-ipix"},
            {"jam", "application/x-jam"},
            {"jar", "application/java-archive"},
            {"jnlp", "application/x-java-jnlp-file"},
            {"latex", "application/x-latex"},
            {"lcc", "application/fastman"},
            {"lcl", "application/x-digitalloca"},
            {"lcr", "application/x-digitalloca"},
            {"lgh", "application/lgh"},
            {"lha", "application/octet-stream"},
            {"js", "application/x-javascript"},
            {"jwc", "application/jwc"},
            {"kjx", "application/x-kjx"},
            {"lzh", "application/x-lzh"},
            {"m13", "application/x-msmediaview"},
            {"m14", "application/x-msmediaview"},
            {"man", "application/x-troff-man"},
            {"mbd", "application/mbedlet"},
            {"mct", "application/x-mascot"},
            {"mdb", "application/x-msaccess"},
            {"me", "application/x-troff-me"},
            {"mi", "application/x-mif"},
            {"mif", "application/x-mif"},
            {"mmf", "application/x-skt-lbs"},
            {"mny", "application/x-msmoney"},
            {"moc", "application/x-mocha"},
            {"mocha", "application/x-mocha"},
            {"mpn", "application/vnd.mophun.application"},
            {"mpc", "application/vnd.mpohun.certificate"},
            {"mof", "application/x-yumekara"},
            {"mpp", "application/vnd.ms-project"},
            {"mps", "application/x-mapserver"},
            {"mrm", "application/x-mrm"},
            {"ms", "application/x-troff-ms"},
            {"msg", "application/vnd.ms-outlook"},
            {"mts", "application/metastream"},
            {"mtx", "application/metastream"},
            {"mtz", "application/metastream"},
            {"mzv", "application/metastream"},
            {"nar", "application/zip"},
            {"nc", "application/x-netcdf"},
            {"ndwn", "application/ndwn"},
            {"nif", "application/x-nif"},
            {"nmz", "application/x-scream"},
            {"npx", "application/x-netfpx"},
            {"nva", "application/x-neva1"},
            {"oda", "application/oda"},
            {"oom", "application/x-AtlasMate-Plugin"},
            {"pan", "application/x-pan"},
            {"pdf", "application/pdf"},
            {"pfr", "application/font-tdpfr"},
            {"pm", "application/x-perl"},
            {"pmd", "application/x-pmd"},
            {"pot", "application/vnd.ms-powerpoint"},
            {"pps", "application/vnd.ms-powerpoint"},
            {"ppt", "application/vnd.ms-powerpoint"},
            {"pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"},
            {"pqf", "application/x-cprplayer"},
            {"pqi", "application/cprplayer"},
            {"proxy", "application/x-ns-proxy-autoconfig"},
            {"ps", "application/postscript"},
            {"ptlk", "application/listenup"},
            {"pub", "application/x-mspublisher"},
            {"prc", "application/x-prc"},
            {"rar", "application/x-rar-compressed"},
            {"rdf", "application/rdf+xml"},
            {"rlf", "application/x-richlink"},
            {"rnx", "application/vnd.rn-realplayer"},
            {"roff", "application/x-troff"},
            {"rtf", "application/rtf"},
            {"rtg", "application/metastream"},
            {"rwc", "application/x-rogerwilco"},
            {"sca", "application/x-supercard"},
            {"scd", "application/x-msschedule"},
            {"sdf", "application/e-score"},
            {"sea", "application/x-stuffit"},
            {"sh", "application/x-sh"},
            {"shw", "application/presentations"},
            {"shar", "application/x-shar"},
            {"sis", "application/vnd.symbian.install"},
            {"sit", "application/x-stuffit"},
            {"skd", "application/x-Koan"},
            {"skm", "application/x-Koan"},
            {"skp", "application/x-Koan"},
            {"skt", "application/x-Koan"},
            {"slc", "application/x-salsa"},
            {"smi", "application/smil"},
            {"smil", "application/smil"},
            {"smp", "application/studiom"},
            {"spl", "application/futuresplash"},
            {"spr", "application/x-sprite"},
            {"sprite", "application/x-sprite"},
            {"spt", "application/x-spt"},
            {"src", "application/x-wais-source"},
            {"stk", "application/hyperstudio"},
            {"sv4cpio", "application/x-sv4cpio"},
            {"sv4crc", "application/x-sv4crc"},
            {"swf", "application/x-shockwave-flash"},
            {"swfl", "application/x-shockwave-flash"},
            {"t", "application/x-troff"},
            {"tad", "application/octet-stream"},
            {"tar", "application/x-tar"},
            {"taz", "application/x-tar"},
            {"tbp", "application/x-timbuktu"},
            {"tbt", "application/x-timbuktu"},
            {"tcl", "application/x-tcl"},
            {"tex", "application/x-tex"},
            {"texi", "application/x-texinfo"},
            {"texinfo", "application/x-texinfo"},
            {"tgz", "application/x-tar"},
            {"thm", "application/vnd.eri.thm"},
            {"tki", "application/x-tkined"},
            {"tkined", "application/x-tkined"},
            {"toc", "application/toc"},
            {"tr", "application/x-troff"},
            {"trm", "application/x-msterminal"},
            {"tsp", "application/dsptype"},
            {"ttf", "application/octet-stream"},
            {"ttz", "application/t-time"},
            {"ustar", "application/x-ustar"},
            {"uu", "application/x-uuencode"},
            {"uue", "application/x-uuencode"},
            {"vcd", "application/x-cdlink"},
            {"vmd", "application/vocaltec-media-desc"},
            {"vmf", "application/vocaltec-media-file"},
            {"vmi", "application/x-dreamcast-vms-info"},
            {"vms", "application/x-dreamcast-vms"},
            {"wis", "application/x-InstallShield"},
            {"wmd", "application/x-ms-wmd"},
            {"wmf", "application/x-msmetafile"},
            {"wmlc", "application/vnd.wap.wmlc"},
            {"wmlsc", "application/vnd.wap.wmlscriptc"},
            {"wps", "application/vnd.ms-works"},
            {"wmz", "application/x-ms-wmz"},
            {"wri", "application/x-mswrite"},
            {"web", "application/vnd.xara"},
            {"wsc", "application/vnd.wap.wmlscriptc"},
            {"wxl", "application/x-wxl"},
            {"x-gzip", "application/x-gzip"},
            {"xar", "application/vnd.xara"},
            {"xdm", "application/x-xdma"},
            {"xdma", "application/x-xdma"},
            {"xdw", "application/vnd.fujixerox.docuworks"},
            {"xht", "application/xhtml+xml"},
            {"xhtm", "application/xhtml+xml"},
            {"xhtml", "application/xhtml+xml"},
            {"xla", "application/vnd.ms-excel"},
            {"xlc", "application/vnd.ms-excel"},
            {"xll", "application/x-excel"},
            {"xlm", "application/vnd.ms-excel"},
            {"xls", "application/vnd.ms-excel"},
            {"xlt", "application/vnd.ms-excel"},
            {"xlw", "application/vnd.ms-excel"},
            {"xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
            {"csv", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
            {"xpi", "application/x-xpinstall"},
            {"yz1", "application/x-yz1"},
            {"z", "application/x-compress"},
            {"zac", "application/x-zaurus-zac"},
            {"zip", "application/zip"},

            {"gau", "chemical/x-gaussian-input"},
            {"csm", "chemical/x-csml"},
            {"csml", "chemical/x-csml"},
            {"emb", "chemical/x-embl-dl-nucleotide"},
            {"embl", "chemical/x-embl-dl-nucleotide"},
            {"mol", "chemical/x-mdl-molfile"},
            {"pdb", "chemical/x-pdb"},
            {"xyz", "chemical/x-pdb"},
            {"mop", "chemical/x-mopac-input"},

            {"dcm", "x-lml/x-evm"},
            {"evm", "x-lml/x-evm"},
            {"gdb", "x-lml/x-gdb"},
            {"lak", "x-lml/x-lak"},
            {"lml", "x-lml/x-lml"},
            {"lmlpack", "x-lml/x-lmlpack"},
            {"ndb", "x-lml/x-ndb"},
            {"rte", "x-lml/x-gps"},
            {"wpt", "x-lml/x-gps"},
            {"trk", "x-lml/x-gps"},

            {"svr", "x-world/x-svr"},
            {"ivr", "i-world/i-vrml"},
            {"vre", "x-world/x-vream"},
            {"vrml", "x-world/x-vrml"},
            {"vrt", "x-world/x-vrt"},
            {"vrw", "x-world/x-vream"},
            {"vts", "workbook/formulaone"},
            {"wrl", "x-world/x-vrml"},
            {"wrz", "x-world/x-vrml"},

            {"dwf", "drawing/x-dwf"},
            {"ice", "x-conference/x-cooltalk"},
            {"map", "magnus-internal/imagemap"},
            {"shtml", "magnus-internal/parsed-html"},
            {"cgi", "magnus-internal/cgi"},

            {"", "*/*"}
    };

    public static void enterSetting(Context context) {
        // 手机型号
        String model = android.os.Build.MODEL;
        // android系统版本号
        String release = android.os.Build.VERSION.RELEASE;
        //手机厂商
        String brand = Build.BRAND;
        if (TextUtils.equals(brand.toLowerCase(), "redmi") || TextUtils.equals(brand.toLowerCase(), "xiaomi")) {
            gotoMiuiPermission(context);
        } else if (TextUtils.equals(brand.toLowerCase(), "meizu")) {
            gotoMeizuPermission(context);
        } else if (TextUtils.equals(brand.toLowerCase(), "huawei") || TextUtils.equals(brand.toLowerCase(), "honor")) {
            gotoHuaweiPermission(context);
        } else {
            context.startActivity(getAppDetailSettingIntent(context));
        }
    }

    /**
     * 跳转到miui的权限管理页面
     */
    public static void gotoMiuiPermission(Context context) {
        try {
            // MIUI 8
            Intent localIntent = new Intent("miui.intent.action.APP_PERM_EDITOR");
            localIntent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity");
            localIntent.putExtra("extra_pkgname", context.getPackageName());
            context.startActivity(localIntent);
        } catch (Exception e) {
            try {
                // MIUI 5/6/7
                Intent localIntent = new Intent("miui.intent.action.APP_PERM_EDITOR");
                localIntent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity");
                localIntent.putExtra("extra_pkgname", context.getPackageName());
                context.startActivity(localIntent);
            } catch (Exception e1) {
                // 否则跳转到应用详情
                context.startActivity(getAppDetailSettingIntent(context));
            }
        }
    }

    /**
     * 跳转到魅族的权限管理系统
     */
    public static void gotoMeizuPermission(Context context) {
        try {
            Intent intent = new Intent("com.meizu.safe.security.SHOW_APPSEC");
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.putExtra("packageName", BuildConfig.APPLICATION_ID);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            context.startActivity(getAppDetailSettingIntent(context));
        }
    }

    /**
     * 华为的权限管理页面
     */
    public static void gotoHuaweiPermission(Context context) {
        try {
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // 华为权限管理
            ComponentName comp = new ComponentName("com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity");
            intent.setComponent(comp);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            context.startActivity(getAppDetailSettingIntent(context));
        }

    }

    /**
     * 获取应用详情页面intent（如果找不到要跳转的界面，也可以先把用户引导到系统设置页面）
     *
     * @return
     */
    public static Intent getAppDetailSettingIntent(Context context) {
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 9) {
            localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            localIntent.setData(Uri.fromParts("package", context.getPackageName(), null));
        } else if (Build.VERSION.SDK_INT <= 8) {
            localIntent.setAction(Intent.ACTION_VIEW);
            localIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            localIntent.putExtra("com.android.settings.ApplicationPkgName", context.getPackageName());
        }
        return localIntent;
    }


    /**
     * 将requestbody转成字符串
     * */
    private String requestBodyToString(Request request) {
        if (!(request instanceof PostRequest)) {
            return "";
        }

        Buffer buffer = new Buffer();
        okhttp3.RequestBody requestBody = ((PostRequest)request).generateRequestBody();
        try {
            requestBody.writeTo(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

        MediaType mediaType = requestBody.contentType();
        Charset charset = Charset.forName("UTF-8");
        charset = requestBody.contentType() != null ? mediaType.charset(charset) : charset;
        return buffer.readString(charset);
    }
}
