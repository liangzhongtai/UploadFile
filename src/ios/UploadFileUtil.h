//
//  UploadFileUtil.h
//  HelloCordova
//
//  Created by 梁仲太 on 2018/6/11.
//

#import <Foundation/Foundation.h>
#import "UploadFile.h"

static NSString *const UPLOAD_TYPE     = @"upload_type";
static NSString *const KEY_FILES             = @"key_files";
static NSString *const KEY_IMAGES        = @"key_images";
static NSString *const KEY_FILE_NAMES = @"key_file_names";
static NSString *const KEY_FILE_KEYS     = @"key_file_keys";

@interface UploadFileUtil : NSObject

@property(nonatomic,strong)UploadFile *plugin;

+(NSMutableDictionary *)httpDic:(NSArray *)keys andValues:(NSArray *)values andPlugin:(UploadFile *)plugin  andUploadType:(NSInteger)uploadType andCallBackId:(NSString *)callback;

+(NSMutableDictionary *)httpDicImge:(NSMutableDictionary *)dic andImages:(NSMutableArray<NSMutableArray<UIImage *> *> *)images andServicveName:(NSArray<NSString *> *)filekeys andFileNames:(NSMutableArray<NSMutableArray<NSString *> *> *)fileNames andCallBackId:(NSString *)callback;

-(void)httpCancel:(NSString *)url andCallBackId:(NSString *)callback;

-(void)httpCancelAll:(NSString *)callback;

-(void)httpDownload:(NSString *)url andFileName:(NSString *)fileName andDirName:(NSString *)dirName andUploadType:(NSInteger)uploadType andCallBackId:(NSString *)callback;

-(void)httpPost:(NSString *)url andParams:(NSMutableDictionary *)parms andCallBackId:(NSString *)callback;

-(void)sendUploadResult:(NSArray *)message andSuccess:(BOOL)success andKeep:(BOOL)keep andCallBackId:(NSString *)callback;

@end
