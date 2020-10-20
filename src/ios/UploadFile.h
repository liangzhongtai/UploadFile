//
//  UploadFile.h
//  HelloCordova
//
//  Created by 梁仲太 on 2018/6/11.
//

#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>
#import <Cordova/CDV.h>

//上传
static NSInteger const UPLOAD_DEFAULT               = 0;
//取消上传
static NSInteger const CANCEL                       = 1;
//取消所有上传
static NSInteger const CANCEL_ALL                   = 2;
//上传不需回传进度
static NSInteger const UPLOAD_NO_PROGRESS           = 3;
//GET请求
static NSInteger const GET                          = 4;
// 下载文件
static NSInteger const DOWNLOAD_FILE                = 5;
// 预览附件
static NSInteger const PREVIEW_FILE                 = 6;
// 断点续传
static NSInteger const DOWNLOAD_BREAKPOINT_RESUME   = 7;
// 暂停断点续传
static NSInteger const DOWNLOAD_BREAKPOINT_PAUSE    = 8;
// 取消断点续传
static NSInteger const DOWNLOAD_BREAKPOINT_CANCEL   = 9;
// 文件删除
static NSInteger const DELETE_FILES                 = 10;
// 生成上传文件
static NSInteger const FILE_CREATE                  = 11;
// 启动websocket
static NSInteger const START_WEB_SOCKET             = 13;
// 结束websocket
static NSInteger const CLOSE_WEB_SOCKET             = 14;
// 断点续传更新
static NSInteger const DOWNLOAD_BREAKPOINT_RESUME_UPDATE = 16;


//网络不可用
static NSInteger const NET_NOT_AVAILABLE            = 1;
//文件上传失败
static NSInteger const UPLOAD_FAILE                 = 2;
//文件上传成功
static NSInteger const UPLOAD_SUCCESS               = 3;
//文件路径不存在
static NSInteger const FILE_NO_EXIST                = 4;
//上传进度
static NSInteger const UPLOAD_PROGRESS              = 5;
// 文件下载失败
static NSInteger const DOWNLOAD_FAILE               = 6;
// 文件下载成功
static NSInteger const DOWNLOAD_SUCCESS             = 7;
// 文件保存失败
static NSInteger const SAVE_FAILE                   = 8;
// 文件断点续传初始化失败
static NSInteger const BREAK_RESUME_INIT_FAILE      = 9;
// 文件删除成功
static NSInteger const DELETE_FINISH                = 10;
// 文件创建失败
static NSInteger const CREATE_FAILE                 = 11;
// 文件创建成功
static NSInteger const CREATE_SUCCESS               = 12;
// 开始上传
static NSInteger const UPLOAD_START                 = 13;



// 文件的状态
// 未下载
static NSInteger const STATUS_NO_START = 0;
// 开始
static NSInteger const STATUS_START    = 1;
// 下载中
static NSInteger const STATUS_LOADING  = 2;
// 停止
static NSInteger const STATUS_STOP     = 3;
// 丢失
static NSInteger const STATUS_LOST     = 4;
// 完成
static NSInteger const STATUS_FINISH   = 5;
// 异常
static NSInteger const STATUS_ERROR    = 6;



@interface UploadFile : CDVPlugin

-(void)successWithMessage:(NSArray *)messages andKeep:(BOOL)keep andCallBackId:(id)callback;
-(void)faileWithMessage:(NSArray *)message andCallBackId:(id)callback;



@end
