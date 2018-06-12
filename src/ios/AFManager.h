//
//  NetWorkManager.h
//  AFNetWorking再封装
//
//  Created by 戴文婷 on 16/5/20.
//  Copyright © 2016年 戴文婷. All rights reserved.
//
//#import <AFNetworking/AFNetworking.h>

//#import <AFNetworking.h>
#import <Foundation/Foundation.h>
#import <Photos/Photos.h>
//#import "AFNetworking.h"

/**定义请求成功的block*/
typedef void(^requestSuccess)( id object);

/**定义请求失败的block*/
typedef void(^requestFailure)( NSError *error);

/**定义上传进度block*/
typedef void(^uploadProgress)(float progress);

/**定义下载进度block*/
typedef void(^downloadProgress)(float progress);

@interface AFManager : NSObject

//请求任务队列
@property(nonatomic,strong)NSMutableDictionary *taskQueue;
-(void)addTask:(NSURLSessionDataTask *)task andKey:(NSString *)key;
-(void)removeTask:(NSString *)key;
-(void)cancelTask:(NSString *)key;
-(void)cancelTasks:(NSString *)vcKey;
-(BOOL)isRequesting;


/**
 *  单例方法
 *
 *  @return 实例对象
 */
+(instancetype)shareManager;

/**
 *  网络请求的实例方法 get
 *
 *  @param urlString    请求的地址
 *  @param paraments    请求的参数
 *  @param successBlock 请求成功的回调
 *  @param failureBlock 请求失败的回调
 *  @param progress 进度
 */
-(void)GET:(NSString *)urlString withParaments:(id)paraments withSuccessBlock:(requestSuccess)successBlock withFailureBlock:(requestFailure)failureBlock progress:(downloadProgress)progress  taskKey:(NSString *)key;

/**
 *  上传文件 post
 *
 *  @param urlString    请求的地址
 *  @param paraments    请求的参数
 *  @param successBlock 请求成功的回调
 *  @param failureBlock 请求失败的回调
 */
-(void)POST:(NSString *)urlString parameters:(id)paraments success:(requestSuccess)successBlock failure:(requestFailure)failureBlock;

/**
 *  网络请求的实例方法 post
 *
 *  @param urlString    请求的地址
 *  @param paraments    请求的参数
 *  @param successBlock 请求成功的回调
 *  @param failureBlock 请求失败的回调
 *  @param progress 进度
 */

-(void)POST:(NSString *)urlString withParaments:(id)paraments withSuccessBlock:(requestSuccess)successBlock withFailureBlock:(requestFailure)failureBlock progress:(downloadProgress)progress taskKey:(NSString *)key;

/**
 *  上传图片
 *
 *  @param operations   上传图片预留参数---视具体情况而定 可移除
 *  @param imageArray   上传的图片数组
 *  @parm width      图片要被压缩到的宽度
 *  @param urlString    上传的url
 *  @param successBlock 上传成功的回调
 *  @param failureBlock 上传失败的回调
 *  @param progress     上传进度
 */
/*-(void)uploadImageWithUrlString:(NSString *)urlString withOperations:(NSDictionary *)operations withImageArray:(NSArray *)imageArray withServiceName:(NSString *)serviceName withSuccessBlock:(requestSuccess)successBlock withFailurBlock:(requestFailure)failureBlock withUpLoadProgress:(uploadProgress)progress  taskKey:(NSString *)key;*/

-(void)uploadImageWithUrlString:(NSString *)urlString withOperations:(NSDictionary *)operations withImageArray:(NSArray *)imageArray withServiceName:(NSString *)serviceName withFileName:(NSArray *)fileNameArray withSuccessBlock:(requestSuccess)successBlock withFailurBlock:(requestFailure)failureBlock withUpLoadProgress:(uploadProgress)progress  taskKey:(NSString *)key;

//上传多个数组
/*-(void)uploadMoreImageWithUrlString:(NSString *)urlString withOperations:(NSDictionary *)operations withImageArray:(NSArray *)imageArray withServiceName:(NSArray *)serviceNames withSuccessBlock:(requestSuccess)successBlock withFailurBlock:(requestFailure)failureBlock withUpLoadProgress:(uploadProgress)progress  taskKey:(NSString *)key;*/

-(void)uploadMoreImageWithUrlString:(NSString *)urlString withOperations:(NSDictionary *)operations withImageArray:(NSMutableArray<NSMutableArray<UIImage *> *> *)imageArray withServiceName:(NSMutableArray<NSString *> *)serviceNames withFileName:(NSMutableArray<NSMutableArray<NSString *> *> *)fileNameArray withSuccessBlock:(requestSuccess)successBlock withFailurBlock:(requestFailure)failureBlock withUpLoadProgress:(uploadProgress)progress  taskKey:(NSString *)key;

/**
 *  视频上传
 *
 *  @param operations   上传视频预留参数---视具体情况而定 可移除
 *  @param videoPath    上传视频的本地沙河路径
 *  @param urlString     上传的url
 *  @param successBlock 成功的回调
 *  @param failureBlock 失败的回调
 *  @param progress     上传的进度
 */
-(void)uploadVideoWithOperaitons:(NSDictionary *)operations withVideoPath:(NSString *)videoPath withUrlString:(NSString *)urlString withSuccessBlock:(requestSuccess)successBlock withFailureBlock:(requestFailure)failureBlock withUploadProgress:(uploadProgress)progress;


/**
 *  文件下载
 *
 *  @param operations   文件下载预留参数---视具体情况而定 可移除
 *  @param savePath     下载文件保存路径
 *  @param urlString        请求的url
 *  @param successBlock 下载文件成功的回调
 *  @param failureBlock 下载文件失败的回调
 *  @param progress     下载文件的进度显示
 */


-(void)downLoadFileWithOperations:(NSDictionary *)operations withSavaPath:(NSString *)savePath withUrlString:(NSString *)urlString withSuccessBlock:(requestSuccess)successBlock withFailureBlock:(requestFailure)failureBlock withDownLoadProgress:(downloadProgress)progress;

/**
 *  取消所有的网络请求
 */


-(void)cancelAllRequest;
/**
 *  取消指定的url请求
 *
 *  @param requestType 该请求的请求类型
 *  @param string      该请求的url
 */

-(void)cancelHttpRequestWithRequestType:(NSString *)requestType requestUrlString:(NSString *)string;

//非代理管理的网络请求方法
+(void)requstGET:(NSString *)URLString parameters:(id)parameters withSuccessBlock:(requestSuccess)successBlock withFailureBlock:(requestFailure)failureBlock;

/**
 *  网络请求的实例方法 get
 *
 *  @param urlString    请求的地址
 *  @param paraments    请求的参数
 *  @param successBlock 请求成功的回调
 *  @param failureBlock 请求失败的回调
 *  @param progress 进度
 */
+(void)GET:(NSString *)urlString withParaments:(id)paraments withSuccessBlock:(requestSuccess)successBlock withFailureBlock:(requestFailure)failureBlock progress:(downloadProgress)progress;

/**
 *  网络请求的实例方法 get(请求的是二进制数据)
 *
 *  @param urlString    请求的地址
 *  @param paraments    请求的参数
 *  @param successBlock 请求成功的回调
 *  @param failureBlock 请求失败的回调
 *  @param progress 进度
 */
-(void)GET:(NSString *)urlString dataWithParaments:(id)paraments withSuccessBlock:(requestSuccess)successBlock withFailureBlock:(requestFailure)failureBlock progress:(downloadProgress)progress;

@end

