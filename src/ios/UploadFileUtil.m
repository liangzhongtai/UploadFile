//
//  UploadFileUtil.m
//  HelloCordova
//
//  Created by 梁仲太 on 2018/6/11.
//

#import "UploadFileUtil.h"
#import "UploadFile.h"
#import "AFManager.h"
#import "NetUtil.h"

@interface UploadFileUtil()

@property(nonatomic,strong)NSURLSessionDownloadTask *downLoadTask;

@end

@implementation UploadFileUtil

+(NSMutableDictionary *)httpDic:(NSArray *)keys andValues:(NSArray *)values andPlugin:(UploadFile *)plugin andUploadType:(NSInteger)uploadType andCallBackId:(NSString *)callback{
    NSMutableDictionary *dic = [[NSMutableDictionary alloc] init];
    NSMutableArray<NSMutableArray<UIImage *> *> *images = [NSMutableArray array];
    NSMutableArray<NSMutableArray<NSString *> *> *fileNames = [NSMutableArray array];
    NSMutableArray<NSString *> *fileKeys = [NSMutableArray array];
    BOOL error = NO;
    NSString *errorPath = @"";
    for (NSInteger i=0,len = (keys==nil||values==nil)?0:keys.count; i<len; i++) {
        if(error){
            [plugin faileWithMessage:@[[NSNumber numberWithInteger:uploadType],[NSNumber numberWithInteger:UPLOAD_FAILE],[NSString stringWithFormat:@"图片:%@已丢失，请检查",errorPath]] andCallBackId:callback];
            break;
        }
        //为array类型
        if([values[i] isKindOfClass:NSArray.class]){
            NSLog(@"key为array类型=%@",keys[i]);
            NSArray *valueArray = values[i];
            NSLog(@"valueArray为array类型=%@",valueArray);
            NSMutableArray<UIImage *> *imgArr = [NSMutableArray array];
            NSMutableArray<NSString *> *fileArr = [NSMutableArray array];
              NSLog(@"valueArray.count=%ld",valueArray.count);
            for (NSInteger j=0,jLen = valueArray == nil?0:valueArray.count; j<jLen; j++) {
                
                UIImage *image = nil;
                NSString * fileName = valueArray[j];
                
                if([fileName hasPrefix:@"/"]){
                    image = [UIImage imageWithContentsOfFile:fileName];
                }else{
                    image = [UIImage imageNamed:fileName];
                }
                NSLog(@"image=%@",image);
                NSLog(@"fileName=%@",fileName);
                if(image==nil){
                    error = YES;
                    errorPath = valueArray[j];
                    break;
                }
                
                [imgArr addObject:image];
                
                if([fileName hasPrefix:@"/"]){
                    NSArray *arr = [fileName componentsSeparatedByString:@"/"];
                    [fileArr addObject:arr[arr.count-1]];
                }else{
                    [fileArr addObject:fileName];
                }
            }
            NSLog(@"imgArr=%@",imgArr);
            NSLog(@"fileArr=%@",fileArr);
            [images addObject:imgArr];
            [fileNames addObject:fileArr];
            [fileKeys addObject:keys[i]];
        }else{
             [dic setValue:values[i] forKey:keys[i]];
        }
    }
    NSLog(@"images=%@",images);
    NSLog(@"fileKeys=%@",fileKeys);
    NSLog(@"fileNames=%@",fileNames);
    [dic setValue:[NSNumber numberWithInteger:uploadType] forKey:UPLOAD_TYPE];
    return [UploadFileUtil httpDicImge:dic andImages:images andServicveName:fileKeys andFileNames:fileNames andCallBackId:callback];
}

+(NSMutableDictionary *)httpDicImge:(NSMutableDictionary *)dic andImages:(NSMutableArray<NSMutableArray<UIImage *> *> *)images andServicveName:(NSArray<NSString *> *)filekeys andFileNames:(NSMutableArray<NSMutableArray<NSString *> *> *)fileNames andCallBackId:(NSString *)callback {
    [dic setValue:images forKey:KEY_IMAGES];
    [dic setValue:filekeys forKey:KEY_FILE_KEYS];
    [dic setValue:fileNames forKey:KEY_FILE_NAMES];
    return dic;
}

//封装的文件下载请求
-(void)httpDownload:(NSString *)url andFileName:(NSString *)fileName andDirName:(NSString *)dirName andUploadType:(NSInteger)uploadType andCallBackId:(NSString *)callback {
    /* NSString *savePath = [[NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) lastObject] stringByAppendingPathComponent: fileName];
     
     NSLog(@"下载的url------%@",url);
     NSLog(@"保存的本地路径------%@",savePath);
     [[AFManager shareManager] downLoadFileWithOperations: nil withSavaPath:savePath withUrlString:url withSuccessBlock:^(id result) {
     NSLog(@"下载成功");
     } withFailureBlock:^(NSError *error) {
     NSLog(@"下载异常-----%@", error);
     } withDownLoadProgress:^(float progress) {
     NSLog(@"下载进度-----%f", progress);
     }];*/
    //创建请求
    //1、url有中文，需要转码
    url = [url stringByAddingPercentEncodingWithAllowedCharacters:[NSCharacterSet URLQueryAllowedCharacterSet]];
    NSURL *urlNS  = [NSURL URLWithString: url];
    
    /*
     如果在回调方法中，不做任何处理，下载的文件会被删除
     下载文件是默认下载到tmp文件夹，系统会自动回收这个区域
     */
    NSMutableURLRequest *request=[NSMutableURLRequest requestWithURL:urlNS];
    
    request.timeoutInterval=90.0;//设置请求超时为5秒
    request.HTTPMethod=@"GET";//设置请求方法
    
    // 下载文件
    NSURLSession *session = [NSURLSession sharedSession];
    
    self.downLoadTask = [session downloadTaskWithRequest:request completionHandler:^(NSURL * _Nullable location, NSURLResponse * _Nullable response, NSError * _Nullable error) {
        
        if (!error){
            // 下载成功
            NSLog(@"下载成功");
            // 注意 location是下载后的临时保存路径, 需要将它移动到需要保存的位置
            NSError *saveError;
            // 创建一个自定义存储路径
            NSString *cachePath = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) lastObject];
            NSString *dirPath = dirName == nil ? nil : [cachePath stringByAppendingPathComponent: dirName];
            // 创建文件夹，把下载文件存到文件夹里
            NSFileManager *fileManager = [NSFileManager defaultManager];
            NSLog(@"文件夹路径：%@",dirPath);
            if(dirPath != nil && ![fileManager fileExistsAtPath:dirPath]){
                NSLog(@"创建目录");
                [fileManager createDirectoryAtPath:dirPath withIntermediateDirectories:YES attributes:nil error:nil];
            }else{
                // 删除原文件夹，创建新文件夹
                // NSLog(@"删除目录");
                // [fileManager removeItemAtPath:dirPath error:nil];
                // [fileManager createDirectoryAtPath:dirPath withIntermediateDirectories:YES attributes:nil error:nil];
            }
            // 保存下载的文件
            NSLog(@"保存的文件名=%@", fileName);
            NSString *savePath;
            if (dirPath == nil) {
                savePath = [cachePath stringByAppendingPathComponent: fileName];
            } else {
                savePath = [dirPath stringByAppendingPathComponent: fileName];
            }
            NSLog(@"保存的路径=%@", savePath);
            // 判断文件是否存在,存在，先删除
            if([fileManager fileExistsAtPath:savePath]){
                [fileManager removeItemAtPath:savePath error:nil];
            }
            
            NSURL *saveURL = [NSURL fileURLWithPath:savePath];
            
            // 文件移动到cache路径中
            [[NSFileManager defaultManager] moveItemAtURL:location toURL:saveURL error:&saveError];
            if (!saveError){
                NSLog(@"保存成功");
                [self sendUploadResult:@[[NSNumber numberWithInteger:uploadType],[NSNumber numberWithInteger:DOWNLOAD_SUCCESS],@"保存成功"] andSuccess:YES andKeep:NO andCallBackId:callback];
            } else {
                NSLog(@"保存失败%@", saveError.localizedDescription);
                [self sendUploadResult:@[[NSNumber numberWithInteger:uploadType],[NSNumber numberWithInteger:SAVE_FAILE],@"保存失败"] andSuccess:NO andKeep:NO andCallBackId:callback];
            }
        }else{
            // 下载失败
            NSLog(@"下载失败: %@", error.localizedDescription);
            [self sendUploadResult:@[[NSNumber numberWithInteger:uploadType],[NSNumber numberWithInteger:DOWNLOAD_FAILE],@"下载失败"] andSuccess:NO andKeep:NO andCallBackId:callback];
        }}];
    // 恢复线程, 启动任务
    [self.downLoadTask resume];
}

//封装的Post请求
-(void)httpPost:(NSString *)url andParams:(NSMutableDictionary *)parms andCallBackId:(NSString *)callback {
    NSInteger uploadType = [[parms valueForKey:UPLOAD_TYPE] integerValue];
    NSString  *key         = url;
    NSMutableArray<NSMutableArray<UIImage *> *>  *images    = [parms valueForKey:KEY_IMAGES];
    NSMutableArray<NSString *>  *fileKeys     = [parms valueForKey:KEY_FILE_KEYS];
    NSMutableArray<NSMutableArray<NSString *> *>  *fileNames= [parms valueForKey:KEY_FILE_NAMES];
    NSLog(@"开始请求_url=%@",url);
    NSLog(@"开始请求_parms=%@",parms);
    
    [[AFManager shareManager] uploadMoreImageWithUrlString:url withOperations:parms withImageArray:images withServiceName:fileKeys  withFileName:fileNames withSuccessBlock:^(id result) {
        NSLog(@"success_result=%@",result);
        NSLog(@"开始请求_callback=%@",callback);
        /*[self sendUploadResult:@[[NSNumber numberWithInteger:uploadType],[NSNumber numberWithInteger:UPLOAD_PROGRESS],[NSNumber numberWithFloat:1.00]] andSuccess:YES andKeep:YES andCallBackId:callback];*/
        [self sendUploadResult:@[[NSNumber numberWithInteger:uploadType],[NSNumber numberWithInteger:UPLOAD_SUCCESS],[self dictToJson:result]] andSuccess:YES andKeep:NO andCallBackId:callback];
        NSLog(@"success_返回结束");
    } withFailurBlock:^(NSError *error) {
        NSLog(@"error=%@",error);
        if([NetUtil netAvailable]){
            [self sendUploadResult:@[[NSNumber numberWithInteger:uploadType],[NSNumber numberWithInteger:UPLOAD_FAILE],@"上传失败"] andSuccess:NO andKeep:NO andCallBackId:callback];
        }else{
            [self sendUploadResult:@[[NSNumber numberWithInteger:uploadType],[NSNumber numberWithInteger:NET_NOT_AVAILABLE],@"网络不可用"] andSuccess:NO andKeep:NO andCallBackId:callback];
        }
    } withUpLoadProgress:^(float progress){
        if(uploadType != UPLOAD_NO_PROGRESS){
            [self sendUploadResult:@[[NSNumber numberWithInteger:uploadType],[NSNumber numberWithInteger:UPLOAD_PROGRESS],[NSNumber numberWithFloat:progress]] andSuccess:YES andKeep:NO andCallBackId:callback];
        }
    } taskKey:key];
}


-(void)sendUploadResult:(NSArray *)message andSuccess:(BOOL)success andKeep:(BOOL)keep andCallBackId:(NSString *)callback {
    if(success){
        [self.plugin successWithMessage:message andKeep:keep andCallBackId:callback];
    }else{
        [self.plugin faileWithMessage:message andCallBackId:callback];
    }
}

-(NSString *)dictToJson:(NSDictionary *)dict{
    NSError *error;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:dict options:NSJSONWritingPrettyPrinted error:&error];
    NSString *jsonString;
    if (!jsonData) {
        NSLog(@"%@",error);
    } else {
        jsonString = [[NSString alloc]initWithData:jsonData encoding:NSUTF8StringEncoding];
    }
    NSMutableString *mutStr = [NSMutableString stringWithString:jsonString];
    NSRange range = {0,jsonString.length};
    //去掉字符串中的空格
    [mutStr replaceOccurrencesOfString:@" " withString:@"" options:NSLiteralSearch range:range];
    NSRange range2 = {0,mutStr.length};
    //去掉字符串中的换行符
    [mutStr replaceOccurrencesOfString:@"\n" withString:@"" options:NSLiteralSearch range:range2];
    return mutStr;

}

-(void)httpCancel:(NSString *)url andCallBackId:(NSString *)callback {
    if (self.downLoadTask != nil) {
        [self.downLoadTask cancel];
    }
}

-(void)httpCancelAll:(NSString *)callback {
    [[AFManager shareManager] cancelAllRequest];
}

@end
