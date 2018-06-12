//
//  UploadFile.m
//  HelloCordova
//
//  Created by 梁仲太 on 2018/6/11.
//

#import "UploadFile.h"
#import "UploadFileUtil.h"

@interface UploadFile()

@property(nonatomic,copy)NSString *callbackId;
@property(nonatomic,assign)NSInteger uploadType;
@property(nonatomic,assign)BOOL base64;
@property(nonatomic,strong)NSString *url;
@property(nonatomic,strong)NSMutableArray<NSString *> *keys;
@property(nonatomic,strong)NSMutableArray *values;
@property(nonatomic,strong)UploadFileUtil *uploadUtil;

@end

@implementation UploadFile

-(void)coolMethod:(CDVInvokedUrlCommand *)command{
    NSLog(@"UploadFile");
    self.callbackId = command.callbackId;
    self.uploadType = [command.arguments[0] integerValue] ;
    if(command.arguments.count>1){
        self.url = command.arguments[1];
        self.keys = [NSMutableArray arrayWithArray:command.arguments[2]];
        self.values = [NSMutableArray arrayWithArray:command.arguments[3]];
    }
    self.url = command.arguments[0];
    if(self.uploadUtil == nil){
        self.uploadUtil = [[UploadFileUtil alloc] init];
    }
    //上传文件
    [self.uploadUtil httpPost:self.url andParams:[UploadFileUtil httpDic:self.keys andValues:self.values andPlugin:self andUploadType:self.uploadType]];
}

-(void)test:(NSString *)url andKeys:(NSArray *)keys andValues:(NSArray *)values{
    if(self.uploadUtil == nil){
        self.uploadUtil = [[UploadFileUtil alloc] init];
    }
    //上传文件
    [self.uploadUtil httpPost:url andParams:[UploadFileUtil httpDic:keys andValues:values andPlugin:self andUploadType:self.uploadType]];
}

-(void)successWithMessage:(NSArray *)messages{
    if(self.callbackId==nil)return;
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:messages];
    [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
}

-(void)faileWithMessage:(NSArray *)message{
    if(self.callbackId==nil)return;
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsArray:message];
    [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
}

@end
