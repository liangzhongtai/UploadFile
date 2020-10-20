//
//  FDownLoaderManager.m
//  FDownLoadDemo
//
//  Created by allison on 2018/8/25.
//  Copyright © 2018年 allison. All rights reserved.
//

#import "FDownLoaderManager.h"
#import "NSString+MD5.h"

@interface FDownLoaderManager() <NSCopying,NSMutableCopying>
@property (nonatomic, strong) FDownLoader *downLoader;
@property (nonatomic, strong) NSMutableDictionary  *downLoaderInfo;
@end

@implementation FDownLoaderManager

static FDownLoaderManager *_shareInstance;
+(instancetype)shareInstance {
    if (_shareInstance == nil) {
        _shareInstance = [[self alloc]init];
    }
    return _shareInstance;
}
+(instancetype)allocWithZone:(struct _NSZone *)zone {
    if (!_shareInstance) {
        static dispatch_once_t onceToken;
        dispatch_once(&onceToken,^{
            _shareInstance = [super allocWithZone:zone];
        });
    }
    return _shareInstance;
}
-(id)copyWithZone:(NSZone *)zone {
    return _shareInstance;
}

-(id)mutableCopyWithZone:(NSZone *)zone {
    return _shareInstance;
}

// key:mdt(url) value:FDownLoader
-(NSMutableDictionary *)downLoaderInfo {
    if (!_downLoaderInfo) {
        _downLoaderInfo = [NSMutableDictionary dictionary];
    }
    return _downLoaderInfo;
}
-(void)downLoader:(NSString *)urlTotal
            oriUrl:(NSString *)oriUrl
          fileName:(NSString *)fileName
           dirName:(NSString *)dirName
        callBackId:(NSString *)callBackId
        uploadType:(NSInteger)uploadType
      downLoadInfo:(DownLoadInfoBlock)downLoadInfo
          progress:(ProgressBlock)progressBlock
           success:(SuccessBlock)successBlock
            failed:(FailedBlock)failedBlock
              stop:(StopBlock)stopBlock;{

    // 1. url
    // NSString *urlMD5 = [oriUrl md5];
    // NSLog(@"url.absoluteString2=%@", urlMD5);
    // 2.根据urlMD5,查找相应的下载器
    NSLog(@"self.downLoaderInfo=%@", self.downLoaderInfo);
    FDownLoader *downLoader = self.downLoaderInfo[oriUrl];
    if (downLoader == nil) {
        downLoader = [[FDownLoader alloc]init];
        self.downLoaderInfo[oriUrl] = downLoader;
    }
    NSLog(@"downLoader = %@", downLoader);
//    [downLoader downLoader:url downLoadInfo:downLoadInfo progress:progressBlock success:successBlock failed:failedBlock];
    // 下载完成之后，移除下载器
    [downLoader downLoader:urlTotal oriUrl:oriUrl fileName:fileName dirName:dirName callBackId:callBackId uploadType:uploadType downLoadInfo:downLoadInfo progress:progressBlock success:successBlock failed:failedBlock stop:stopBlock];
    NSLog(@"url.absoluteString4");
//    [downLoader downLoader:url downLoadInfo:downLoadInfo progress:progressBlock success:^(NSString *filePath) {
//        //拦截block
//        [self.downLoaderInfo removeObjectForKey:urlMD5];
//        successBlock(filePath);
//    } failed:failedBlock];
}

-(void)pauseWithURL:(NSString *)url {
    // NSString *urlMD5 = [url md5];
    FDownLoader *downLoader = self.downLoaderInfo[url];
    [downLoader pauseCurrentTask];
}

-(void)resumeWithURL:(NSString *)url {
    // NSString *urlMD5 = [url md5];
    FDownLoader *downLoader = self.downLoaderInfo[url];
    [downLoader resumeCurrentTask];
}

-(void)cancleWithURL:(NSString *)url {
    // NSString *urlMD5 = [url md5];
    FDownLoader *downLoader = self.downLoaderInfo[url];
    [downLoader cancleCurrentTask];
}

-(void)removeWithURL:(NSString *)url {
    // NSString *urlMD5 = [url md5];
    FDownLoader *downLoader = self.downLoaderInfo[url];
    [downLoader cancleCurrentTask];
    [self.downLoaderInfo removeObjectForKey:url];
}

// 暂停所有
-(void)pauseAll {
    [self.downLoaderInfo.allValues performSelector:@selector(pauseCurrentTask) withObject:nil];
}
// 恢复所有
-(void)resumeAll {
    [self.downLoaderInfo.allValues performSelector:@selector(resumeCurrentTask) withObject:nil];
}

@end
