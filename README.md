##UploadFile���ʹ��˵��
* �汾:2.7.0

##��������
* npm 4.4.1 +
* node 9.8.0 +


##ʹ������
####ע��:
######iosƽ̨,��Macϵͳ�£�������µĿ���̨��������Ȩ�����⣬����������ǰ��sudo
#####��׿ƽ̨��������������jar�������cordova build ����󣬱������쳣:
#####��*\��Ŀ��\platforms\android\Androidmanifest.xml��
#####��*\��Ŀ��\platfroms\android\res\xml\config.xml��
#####��ͨ�����·����������
#####����һ:��Ŀ��Ŀ¼\platforms\android\cordova\Api.js�ļ��������޸ģ�����ִ��cordova build����ᱨ�쳣��UnhandledPromiseRejectionWarning: Error: ENOENT: no such file or directory,......��

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
#####Ȼ���ֶ���û�гɹ��Զ������jar�����ֶ����õ�libsĿ¼��
#####������:ʹ�õ�cordova-android�İ汾С��7.0.0,��cordova platform add android@6.4.0


######1.������Ŀ�ĸ�Ŀ¼�����������::com.chinamobile.upload.uploadfile
* Ϊ��Ŀ���UploadFile�����ִ��:`cordova plugin add com.chinamobile.upload.uploadfile`
* ���Ҫɾ�����,ִ��:`cordova plugin add com.chinamobile.upload.uploadfile`
* Ϊ��Ŀ��Ӷ�Ӧ��platformƽ̨,����ӹ����˲����ԣ�ִ��:
* ��׿ƽ̨: `cordova platform add android`
* ios ƽ̨:`cordova platform add ios`
* �������ӵ���Ӧƽ̨��,ִ��: `cordova build`

######2.��js�ļ���,ͨ������js�������ò��������ִ��app���¹���
*
```javascript
    camera: function(){
        //��native�����ļ��ϴ�����
        //android��
        //����Ԫ��1��0���ϴ��� 1��ȡ��url��Ӧ���ϴ���2��ȡ�������ϴ�
        //����Ԫ��2���ӿ�url
        //����Ԫ��3��post-keys
        //����Ԫ��4��post-values
        cordova.exec(null,null,"UploadFile","coolMethod",[0,"http://",["file","taskId"],[["/storage/emulated/0/DCIM/71_1528608551054.jpg","/storage/emulated/0/DCIM/70_1528608610614.jpg","/storage/emulated/0/DCIM/70_1528608636480.jpg"],"4596673]]);
    }
    
    //Ŀǰֻ��android�˻�ص�����״̬
    success: function(var result){
        //����:0:�ϴ���1��ȡ��url��Ӧ���ϴ���2��ȡ�������ϴ�
        var uploadType = result[0];
        //status=3:�ϴ����
        //status=4:����
        var status     = result[1];
        //��ʾ��Ϣ�����status=4 ����message = ���ȸ�ʽ�� 0.54323245��
        var message    = result[2];
    }

    error: function(var result){
        //�ϴ�ʧ����ʾ
       //����:0:�ϴ���1��ȡ��url��Ӧ���ϴ���2��ȡ�������ϴ�
        var uploadType = result[0];
        //status=1:���粻����
        //status=2:�ϴ�ʧ��
        var status     = result[1];
        //��ʾ��Ϣ
        var message    = result[2];
    }
```

*ע������:
###### cordova.exec������,JSONArray�����ĵ�����Ԫ�ع̶�Ϊ�ļ�����
###### IOSƽ̨����Ҫ����Ŀ�� info.list �������Ȩ�� App Transport Security Settings ���: Allow Arbitrary Loads ����ΪYES 


##���ⷴ��
  ��ʹ�������κ����⣬������������ϵ��ʽ.
  
  * �ʼ�:18520660170@139.com
  * ʱ��:2018-5-24 16:00:00





