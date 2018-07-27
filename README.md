## UploadFile插件使用说明
* 版本:3.7.0

## 环境配置
* npm 4.4.1 +
* node 9.8.0 +


## 使用流程
#### 注意:
###### ios平台,在Mac系统下，如果以下的控制台命令遇到权限问题，可以在命令前加sudo
##### 安卓平台，插件加入第三方jar包后，如果cordova build 命令后，报以下异常:
##### ‘*\项目名\platforms\android\Androidmanifest.xml’
##### ‘*\项目名\platfroms\android\res\xml\config.xml’
##### 请通过以下方法解决问题
##### 方法一:项目根目录\platforms\android\cordova\Api.js文件作以下修改，否则执行cordova build命令会报异常：UnhandledPromiseRejectionWarning: Error: ENOENT: no such file or directory,......；

```javascript
this.locations = {
    root: self.root,
    www: path.join(self.root, 'assets/www'),
    res: path.join(self.root, 'res'),
    platformWww: path.join(self.root, 'platform_www'),
    configXml: path.join(self.root, 'app/src/main/res/xml/config.xml'),
    defaultConfigXml: path.join(self.root, 'cordova/defaults.xml'),
    strings: path.join(self.root, 'app/src/main/res/values/strings.xml'),
    manifest: path.join(self.root, 'app/src/main/AndroidManifest.xml'),
    build: path.join(self.root, 'build'),
    javaSrc: path.join(self.root, 'app/src/main/java/'),
    // NOTE: Due to platformApi spec we need to return relative paths here
    cordovaJs: 'bin/templates/project/assets/www/cordova.js',
    cordovaJsSrc: 'cordova-js-src'
};
```
##### 然后手动将没有成功自动导入的jar包，手动放置到libs目录下
##### 方法二:使用的cordova-android的版本小于7.0.0,如cordova platform add android@6.4.0


###### 1.进入项目的根目录，添加相机插件::com.chinamobile.upload.uploadfile
* 为项目添加UploadFile插件，执行:`cordova plugin add com.chinamobile.upload.uploadfile`
* 如果要删除插件,执行:`cordova plugin add com.chinamobile.upload.uploadfile`
* 为项目添加对应的platform平台,已添加过，此步忽略，执行:
* 安卓平台: `cordova platform add android`
* ios 平台:`cordova platform add ios`
* 将插件添加到对应平台后,执行: `cordova build`

###### 2.在js文件中,通过以下js方法调用插件，可以执行app更新功能
*
```javascript
    camera: function(){
        //向native发出文件上传请求
        //success:成功的回调函数
        //error:失败的回调函数
        //UploadFile:插件名,固定值
        //coolMethod:插件方法，固定值
        //[0,"http://",["file","taskId"],[["/storage/emulated/0/DCIM/71_1528608551054.jpg","/storage/emulated/0/DCIM/70_1528608610614.jpg","/storage/emulated/0/DCIM/70_1528608636480.jpg"],"4596673]:插件方法参数，具体对应以下
        //参数1：0：上传， 1：取消url对应的上传，2：取消所有上传
        //参数2：接口url
        //参数3：post-keys
        //参数4：post-values
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


## 问题反馈
  在使用中有任何问题，可以用以下联系方式.
  
  * 邮件:18520660170@139.com
  * 时间:2018-5-24 16:00:00




