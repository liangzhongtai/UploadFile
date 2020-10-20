//
//  UploadFile.m
//  HelloCordova
//
//  Created by 梁仲太 on 2018/6/11.
//

#import "UploadFile.h"
#import "UploadFileUtil.h"
#import <QuickLook/QuickLook.h>
#import "MainViewController.h"
#import "FDownLoaderManager.h"
#import "FDownLoader.h"
#import "FFileTool.h"
#import "CWebSocketManager.h"
#import "DownloadUpdateWindow.h"

@interface UploadFile()<QLPreviewControllerDelegate,QLPreviewControllerDataSource>

@property(nonatomic,assign)BOOL base64;
@property(nonatomic,strong)UploadFileUtil *uploadUtil;
@property(nonatomic,strong)QLPreviewController *qlVC;
@property(nonatomic,strong)UINavigationController *navVC;
@property(nonatomic,strong)NSMutableArray *files;
@property(nonatomic,strong)DownloadUpdateWindow *updateWindow;

@end

@implementation UploadFile

-(void)coolMethod:(CDVInvokedUrlCommand *)command{
    NSLog(@"UploadFile**********");
    NSString *callbackId = command.callbackId;
    NSInteger uploadType = [command.arguments[0] integerValue] ;
    NSLog(@"uploadType = %ld_****************", uploadType);
    NSString *url = command.arguments[1];
    NSLog(@"uploadType = %@_****************", url);
    NSString *fileName;
    NSString *dirName;
    if (uploadType == START_WEB_SOCKET) {
        [CWebSocketManager shared].wss = url;
        [CWebSocketManager shared].plugin = self;
        [CWebSocketManager shared].callback = callbackId;
        [[CWebSocketManager shared] registerNotification];
        NSLog(@"[CWebSocketManager shared].connectType=%ld*****************", [CWebSocketManager shared].connectType);
        if ([CWebSocketManager shared].connectType != WebSocketConnect) {
            [[CWebSocketManager shared] connectServer];
        }
        return;
    } else if (uploadType == CLOSE_WEB_SOCKET) {
        NSLog(@"关闭websocket连接2********************");
        [[CWebSocketManager shared] RMWebSocketClose];
        return;
    } else if(uploadType == PREVIEW_FILE) {
        fileName = command.arguments[2];
        if (command.arguments.count > 3) {
            dirName = command.arguments[3];
        }
        self.qlVC = [[QLPreviewController alloc] init];
        self.qlVC.dataSource = self;
        self.qlVC.delegate = self;
        self.navVC = [[UINavigationController alloc]initWithRootViewController:self.qlVC] ;
        UIBarButtonItem *backButton = [[UIBarButtonItem alloc]initWithTitle:@"返回" style:UIBarButtonItemStylePlain target:self action:@selector(closeAction)];
        self.qlVC.navigationItem.leftBarButtonItem = backButton;
        NSString *cachePath = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) lastObject];
        NSString *dirPath;
        NSString *filePath;
        if (dirName == nil) {
            filePath = [cachePath stringByAppendingPathComponent: fileName];
        } else {
            dirPath = [cachePath stringByAppendingPathComponent: dirName];
            filePath = [dirPath stringByAppendingPathComponent: fileName];
        }
       
        // 检查文件是否存在
        NSFileManager *fileManager = [NSFileManager defaultManager];
        if (![fileManager fileExistsAtPath:filePath]) {
            [self faileWithMessage:@[[NSNumber numberWithInteger:uploadType],[NSNumber numberWithInteger:FILE_NO_EXIST],@"文件不存在"] andCallBackId:callbackId];
            return;
        }
        self.files = [NSMutableArray array];
        [self.files addObject:filePath];
        [self.viewController presentViewController:self.navVC animated:YES completion:nil];
        // [self.viewController presentViewController:self.qlVc animated:YES completion:nil];
        return;
    // 启动断点下载
    } else if (uploadType == DOWNLOAD_BREAKPOINT_RESUME ||
               uploadType == DOWNLOAD_BREAKPOINT_RESUME_UPDATE) {
        // 启动更新弹窗
        if (uploadType == DOWNLOAD_BREAKPOINT_RESUME_UPDATE &&
            self.updateWindow == nil) {
            CGFloat screenHeight = [[UIApplication sharedApplication] statusBarFrame].size.height;
            self.updateWindow = [[DownloadUpdateWindow alloc] initWithFrame:CGRectMake(0, screenHeight, 120, 90)];
        }
        // 启动断点续传
        fileName = command.arguments[2];
        if (command.arguments.count > 3) {
            dirName = command.arguments[3];
        }
        NSString *tickets = command.arguments.count > 6 ? command.arguments[6] : nil;
        NSLog(@"#########文件名fileName=%@", fileName);
        NSLog(@"#########文件名dirName=%@", dirName);
        NSLog(@"#########文件名url=%@", url);
        NSString *ticketUrl = url;
        NSLog(@"#########文件名ticketUrl1=%@", ticketUrl);
        if (tickets != nil) {
            if ([url containsString:@"?"]) {
                ticketUrl = [NSString stringWithFormat:@"%@&tickets=%@",ticketUrl,tickets];
            } else {
                ticketUrl = [NSString stringWithFormat:@"%@?tickets=%@",ticketUrl,tickets];
            }
        }
        ticketUrl = [ticketUrl stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
        NSLog(@"#########文件名ticketUrl2=%@", ticketUrl);
        NSLog(@"#########文件名url=%@", url);
        [[FDownLoaderManager shareInstance] downLoader:ticketUrl oriUrl:url fileName:fileName dirName:dirName callBackId:callbackId uploadType:uploadType downLoadInfo:^(NSInteger uploadType, long long totalSize, long long serviceSize,NSString *urlTask, NSString *callBack) {
            NSLog(@"下载开始_%lld", totalSize);
            NSLog(@"下载开始urlTask=%@", urlTask);
            [self successWithMessage:@[
               [NSNumber numberWithInteger:uploadType],
               [NSNumber numberWithInteger:STATUS_START],
               [NSString stringWithFormat:@"%lld", serviceSize == - 1 ? -1 : totalSize],
               urlTask,
               @"" ] andKeep:YES andCallBackId:callBack];
            if (uploadType == DOWNLOAD_BREAKPOINT_RESUME_UPDATE) {
                [self.updateWindow updateView:0 andState:@"已就绪" andSize:@""];
            }
        } progress:^(NSInteger uploadType, float progress, NSString *urlTask, NSString *callBack) {
            NSLog(@"下载中urlTask=%@", urlTask);
            [self successWithMessage:@[
               [NSNumber numberWithInteger:uploadType],
               [NSNumber numberWithInteger:STATUS_LOADING],
               [NSString stringWithFormat:@"%f", progress],
               urlTask,
               @""] andKeep:YES andCallBackId:callBack];
            if (uploadType == DOWNLOAD_BREAKPOINT_RESUME_UPDATE) {
                [self.updateWindow updateView:progress andState:@"下载中" andSize:@""];
            }
        } success:^(NSInteger uploadType, long long totalSize, NSString *filePath, NSString *urlTask, NSString *callBack) {
            NSLog(@"下载成功urlTask=%@", urlTask);
            [self successWithMessage:@[
               [NSNumber numberWithInteger:uploadType],
               [NSNumber numberWithInteger:STATUS_FINISH],
               filePath,
               urlTask,
               [NSString stringWithFormat:@"%lld", totalSize]] andKeep:NO andCallBackId:callBack];
            [[FDownLoaderManager shareInstance] removeWithURL:[urlTask stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
            if (uploadType == DOWNLOAD_BREAKPOINT_RESUME_UPDATE) {
                [self.updateWindow updateView:1.0 andState:@"已完成" andSize:@""];
            }
        } failed:^(NSInteger uploadType, NSString *urlTask, NSString *callBack){
            NSLog(@"下载失败urlTask=%@", urlTask);
            [self successWithMessage:@[
               [NSNumber numberWithInteger:uploadType],
               [NSNumber numberWithInteger:STATUS_ERROR],
               @"下载失败",
               urlTask,
               @""] andKeep:NO andCallBackId:callBack];
            if (uploadType == DOWNLOAD_BREAKPOINT_RESUME_UPDATE) {
                [self.updateWindow updateView:0 andState:@"下载失败" andSize:@""];
            }
        } stop:^(NSInteger uploadType, NSString *urlTask, NSString *callBack){
            NSLog(@"下载暂停urlTask=%@",urlTask);
            [self successWithMessage:@[
               [NSNumber numberWithInteger:uploadType],
               [NSNumber numberWithInteger:STATUS_STOP],
               @"下载暂停",
               urlTask,
               @""] andKeep:NO andCallBackId:callBack];
            if (uploadType == DOWNLOAD_BREAKPOINT_RESUME_UPDATE) {
                [self.updateWindow updateView:0 andState:@"已中断" andSize:@""];
            }
        }];
        return;
    // 暂停
    } else if (uploadType == DOWNLOAD_BREAKPOINT_PAUSE) {
        [[FDownLoaderManager shareInstance] pauseWithURL:url];
        return;
    // 取消
    } else if (uploadType == DOWNLOAD_BREAKPOINT_CANCEL) {
        [[FDownLoaderManager shareInstance] cancleWithURL:url];
        return;
    // 删除
    } else if (uploadType == DELETE_FILES) {
        NSString *urlStrs = command.arguments[1];
        NSString *files = command.arguments[2];
        NSString *dirName = command.arguments.count > 3 ? command.arguments[3] : @"";
        NSArray<NSString *> *urls = [urlStrs hasPrefix:@","] ? [urlStrs componentsSeparatedByString:@","] : @[urlStrs];
        NSArray<NSString *> *fileNames = [files hasPrefix:@","] ? [files componentsSeparatedByString:@","] : @[files];
        for (NSInteger i = 0; i < urls.count; i++) {
            [[FDownLoaderManager shareInstance] cancleWithURL:urls[i]];
        }
        for (NSInteger i = 0; i < urls.count; i++) {
            NSString *downLoadingPath = nil;
            NSString *downLoaderPath = nil;
            if (dirName == nil) {
                downLoadingPath = [kTmpPath stringByAppendingPathComponent:fileNames[i]];
                downLoaderPath = [kCachePath stringByAppendingPathComponent:fileNames[i]];
            } else {
                NSString *dirPath = [kTmpPath stringByAppendingPathComponent:dirName];
                downLoadingPath = [dirPath stringByAppendingPathComponent:fileNames[i]];
                
                NSString *dirCachePath = [kCachePath stringByAppendingPathComponent:dirName];
                downLoaderPath = [dirCachePath stringByAppendingPathComponent:fileNames[i]];
            }

            NSLog(@"downLoadingPath=------%@",downLoadingPath);
            NSLog(@"downLoaderPath=-------%@",downLoaderPath);
            ///Users/liangzhongtai/Library/Developer/CoreSimulator/Devices/81E95018-972D-45DF-AF11-973ABEED6B93/data/Containers/Data/Application/20A3BB85-9802-415A-B1E3-22B586B4805E/tmp/vpn.apk
            
            ////Users/liangzhongtai/Library/Developer/CoreSimulator/Devices/81E95018-972D-45DF-AF11-973ABEED6B93/data/Containers/Data/Application/20A3BB85-9802-415A-B1E3-22B586B4805E/tmp/vpn.apk
            
            ///Users/liangzhongtai/Library/Developer/CoreSimulator/Devices/81E95018-972D-45DF-AF11-973ABEED6B93/data/Containers/Data/Application/20A3BB85-9802-415A-B1E3-22B586B4805E/Library/Caches/vpn.apk
            
            ///Users/liangzhongtai/Library/Developer/CoreSimulator/Devices/81E95018-972D-45DF-AF11-973ABEED6B93/data/Containers/Data/Application/20A3BB85-9802-415A-B1E3-22B586B4805E/Library/Caches/vpn.apk
            [FFileTool removeFile:downLoadingPath];
            [FFileTool removeFile:downLoaderPath];
        }
        [self successWithMessage:@[[NSNumber numberWithInteger:uploadType], [NSNumber numberWithInteger:DELETE_FINISH], @"文件删除成功!"] andKeep:NO andCallBackId:callbackId];
    } else if (uploadType == CANCEL) {
        [self.uploadUtil httpCancel:url andCallBackId:callbackId];
        return;
    } else if (uploadType == CANCEL_ALL) {
        [self.uploadUtil httpCancelAll:callbackId];
        return;
    // 生成文件
    } else if (uploadType == FILE_CREATE) {
        if (command.arguments.count > 2) {
            fileName = command.arguments[2];
        }
        NSInteger size = 100;
        if (command.arguments.count > 3) {
            size = [command.arguments[3] integerValue];
        }
        if (fileName == nil) {
            fileName = @"upload500.zip";
        }
        NSString *cachePath = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) lastObject];
        NSString *filePath = [cachePath stringByAppendingPathComponent: fileName];
        NSFileManager *fileManager = [NSFileManager defaultManager];
        if ([fileManager fileExistsAtPath:filePath]) {
            if ([[fileManager attributesOfItemAtPath:filePath error:nil] fileSize] > ((size - 1) * 1024*1024)) {
                [self successWithMessage:@[[NSNumber numberWithInteger:FILE_CREATE], [NSNumber numberWithInteger:CREATE_SUCCESS], filePath] andKeep:NO andCallBackId:callbackId];
                return;
            } else {
                [fileManager removeItemAtPath:filePath error:nil];
            }
        }
        if (![fileManager fileExistsAtPath:filePath]) {
            [fileManager createFileAtPath:filePath contents:[@"0" dataUsingEncoding:NSUTF8StringEncoding] attributes:nil];
        }
        NSFileHandle * fileHandle = [NSFileHandle fileHandleForWritingAtPath:filePath];
        if(fileHandle == nil){
            return;
        }
        [fileHandle seekToEndOfFile];
        NSString *str = @"01010101";
        for (NSInteger i = 0; i < 128; i++) {
            str = [str stringByAppendingString:@"01010101"];
        }
        NSData* stringData  = [str dataUsingEncoding:NSUTF8StringEncoding];
        for (NSInteger i = 0; i < size * 1024; i++) {
            [fileHandle writeData:stringData];
        }
        [fileHandle closeFile];
        [self successWithMessage:@[[NSNumber numberWithInteger:FILE_CREATE], [NSNumber numberWithInteger:CREATE_SUCCESS], filePath] andKeep:NO andCallBackId:callbackId];
        return;
    }
    
    if (self.uploadUtil == nil) {
        self.uploadUtil = [[UploadFileUtil alloc] init];
    }
    self.uploadUtil.plugin = self;
    if (uploadType == DOWNLOAD_FILE) {
        fileName = command.arguments[2];
        if (command.arguments.count > 3) {
            dirName = command.arguments[3];
        }
        // 下载文件
        [self.uploadUtil httpDownload:url andFileName:fileName andDirName:dirName andUploadType:uploadType andCallBackId:callbackId];
    } else if (uploadType == UPLOAD_DEFAULT || uploadType == UPLOAD_NO_PROGRESS) {
        NSMutableArray<NSString *> *keys;
        NSMutableArray *values;
        if (command.arguments.count > 1) {
            keys = [NSMutableArray arrayWithArray:command.arguments[2]];
            values = [NSMutableArray arrayWithArray:command.arguments[3]];
        }
        NSLog(@"keys=------%@",keys);
        NSLog(@"values=-------%@",values);
        // 上传文件
        [self.uploadUtil httpPost:url andParams:[UploadFileUtil httpDic:keys andValues:values andPlugin:self andUploadType:uploadType andCallBackId:callbackId] andCallBackId:callbackId];
    }
}

//返回文件的个数
-(NSInteger)numberOfPreviewItemsInPreviewController:(QLPreviewController *)controller{
    return self.files.count;
}

//关闭导航文件
-(void)closeAction{
    [self.navVC dismissViewControllerAnimated:YES completion:nil];
}

//加载需要显示的文件
-(id<QLPreviewItem>)previewController:(QLPreviewController *)controller previewItemAtIndex:(NSInteger)index{
    return [NSURL fileURLWithPath:self.files[index]];
}

-(void)successWithMessage:(NSArray *)messages andKeep:(BOOL)keep andCallBackId:(id)callback {
    if (callback == nil) {
        return;
    }
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:messages];
    [result setKeepCallbackAsBool:keep];
    [self.commandDelegate sendPluginResult:result callbackId:callback];
}

-(void)faileWithMessage:(NSArray *)message andCallBackId:(id)callback {
    if (callback == nil) {
        return;
    }
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsArray:message];
    [self.commandDelegate sendPluginResult:result callbackId:callback];
}

@end
