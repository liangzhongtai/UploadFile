##UploadFile���ʹ��˵��
* �汾:2.2.0

##��������
* npm 4.4.1 +
* node 9.8.0 +


##ʹ������
####ע��:
######Macƽ̨,������µĿ���̨��������Ȩ�����⣬����������ǰ��sudo

######1.������Ŀ�ĸ�Ŀ¼�����������::com.chinamobile.upload.uploadfile
* Ϊ��Ŀ���UploadFile�����ִ��:`cordova plugin add com.chinamobile.upload.uploadfile`
* ���Ҫɾ�����,ִ��:`cordova plugin add com.chinamobile.upload.UploadFile`
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





