//
//  FDownLoader.h
//  FDownLoadDemo
//
//  Created by allison on 2018/8/18.
//  Copyright © 2018年 allison. All rights reserved.
//

#import <Foundation/Foundation.h>

#define kCachePath NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES).firstObject
#define kTmpPath NSTemporaryDirectory()

typedef NS_ENUM(NSUInteger, FDownLoadState) {
    FDownLoadStateNoStart, // 未开始
    FDownLoadStateStart,// 开始
    FDownLoadStateDownLoading, // 正在下载
    FDownLoadStatePause, // 暂停
    FDownLoadStateLose,// 丢失
    FDownLoadStatePauseSuccess, // 成功
    FDownLoadStatePauseFailed,// 失败
};

typedef void (^DownLoadInfoBlock)(NSInteger uploadType, long long totalSize, long long serviceSize,NSString *urlTask, NSString *callBack);
typedef void (^StateChangeBlcok) (NSInteger uploadType, FDownLoadState state);
typedef void (^ProgressBlock)(NSInteger uploadType, float progress, NSString *urlTask, NSString *callBack);
typedef void (^SuccessBlock)(NSInteger uploadType, long long totalSize, NSString *filePath, NSString *urlTask, NSString *callBack);
typedef void (^FailedBlock)(NSInteger uploadType, NSString *urlTask, NSString *callBack);
typedef void (^StopBlock)(NSInteger uploadType, NSString *urlTask, NSString *callBack);

// 一个下载器,对应一个下载任务
@interface FDownLoader : NSObject

-(void)downLoader:(NSString *)url
            oriUrl:(NSString *)oriUrl
          fileName:(NSString *)fileName
           dirName:(NSString *)dirName
        callBackId:(NSString *)callBackId
        uploadType:(NSInteger)uploadType
      downLoadInfo:(DownLoadInfoBlock)downLoadInfo
          progress:(ProgressBlock)progressBlock
           success:(SuccessBlock)successBlock
            failed:(FailedBlock)failedBlock
             stop:(StopBlock)stopBlock;

-(void)downLoader:(NSString*)url andFileName:(NSString *)fileName andDirName:(NSString *)dirName;
// 取消和删除文件
-(void)cancleAndClean;
/// 暂停
-(void)pauseCurrentTask;
/// 取消
-(void)cancleCurrentTask;
/// 取消和删除
-(void)cancleAndClean;
// 继续任务
-(void)resumeCurrentTask;

#pragma mark -- <数据>
@property (nonatomic,assign,readonly)FDownLoadState state;
@property (nonatomic,copy) DownLoadInfoBlock downLoadInfo;
@property (nonatomic,copy) StateChangeBlcok stateChange;
@property (nonatomic,assign,readonly)float progress;
@property (nonatomic,copy) ProgressBlock progressChange;
@property (nonatomic,copy) SuccessBlock successBlock;
@property (nonatomic,copy) FailedBlock failedBlock;
@property (nonatomic,copy) FailedBlock stopBlock;

@end
