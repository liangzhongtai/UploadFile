##UploadFile插件使用说明
* 版本:2.2.0

##环境配置
* npm 4.4.1 +
* node 9.8.0 +


##使用流程
####注意:
######Mac平台,如果以下的控制台命令遇到权限问题，可以在命令前加sudo

######1.进入项目的根目录，添加相机插件::com.chinamobile.upload.uploadfile
* 为项目添加UploadFile插件，执行:`cordova plugin add com.chinamobile.upload.uploadfile`
* 如果要删除插件,执行:`cordova plugin add com.chinamobile.upload.UploadFile`
* 为项目添加对应的platform平台,已添加过，此步忽略，执行:
* 安卓平台: `cordova platform add android`
* ios 平台:`cordova platform add ios`
* 将插件添加到对应平台后,执行: `cordova build`

######2.在js文件中,通过以下js方法调用插件，可以执行app更新功能
*
```javascript
    camera: function(){
        //向native发出文件上传请求
        //android端
        //参数元素1：0：上传， 1：取消url对应的上传，2：取消所有上传
        //参数元素2：接口url
        //参数元素3：post-keys
        //参数元素4：post-values
        cordova.exec(null,null,"UploadFile","coolMethod",[0,"http://",["file","taskId"],[["/storage/emulated/0/DCIM/71_1528608551054.jpg","/storage/emulated/0/DCIM/70_1528608610614.jpg","/storage/emulated/0/DCIM/70_1528608636480.jpg"],"4596673]]);
    }
    
    //目前只有android端会回调更新状态
    success: function(var result){
        //动作:0:上传，1：取消url对应的上传，2：取消所有上传
        var uploadType = result[0];
        //status=3:上传完成
        //status=4:进度
        var status     = result[1];
        //提示信息，如果status=4 ，则message = 进度格式： 0.54323245。
        var message    = result[2];
    }

    error: function(var result){
        //上传失败提示
       //动作:0:上传，1：取消url对应的上传，2：取消所有上传
        var uploadType = result[0];
        //status=1:网络不可用
        //status=2:上传失败
        var status     = result[1];
        //提示信息
        var message    = result[2];
    }
```

*注意问题:
###### cordova.exec函数中,JSONArray参数的第三个元素固定为文件参数
###### IOS平台，需要在项目的 info.list 添加以下权限 App Transport Security Settings 添加: Allow Arbitrary Loads 设置为YES 


##问题反馈
  在使用中有任何问题，可以用以下联系方式.
  
  * 邮件:18520660170@139.com
  * 时间:2018-5-24 16:00:00





