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
static NSInteger const UPLOAD_DEFAULT = 0;
//取消上传
static NSInteger const CANCEL = 1;
//取消所有上传
static NSInteger const CANCEL_ALL = 2;

//网络不可用
static NSInteger const NET_NOT_AVAILABLE = 1;
//文件上传失败
static NSInteger const UPLOAD_FAILE      = 2;
//文件上传成功
static NSInteger const UPLOAD_SUCCESS    = 3;
//文件路径不存在
static NSInteger const FILE_NO_EXIST     = 4;
//上传进度
static NSInteger const UPLOAD_PROGRESS   = 5;

@interface UploadFile : CDVPlugin

-(void)test:(NSString *)url andKeys:(NSArray *)keys andValues:(NSArray *)values;
-(void)successWithMessage:(NSArray *)messages;
-(void)faileWithMessage:(NSArray *)message;

@end
