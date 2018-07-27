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



@end

@implementation UploadFileUtil

+(NSMutableDictionary *)httpDic:(NSArray *)keys andValues:(NSArray *)values andPlugin:(UploadFile *)plugin andUploadType:(NSInteger)uploadType{
    NSMutableDictionary *dic = [[NSMutableDictionary alloc] init];
    NSMutableArray<NSMutableArray<UIImage *> *> *images = [NSMutableArray array];
    NSMutableArray<NSMutableArray<NSString *> *> *fileNames = [NSMutableArray array];
    NSMutableArray<NSString *> *fileKeys = [NSMutableArray array];
    BOOL error = NO;
    NSString *errorPath = @"";
    for (NSInteger i=0,len = (keys==nil||values==nil)?0:keys.count; i<len; i++) {
        if(error){
            [plugin faileWithMessage:@[[NSNumber numberWithInteger:uploadType],[NSNumber numberWithInteger:UPLOAD_FAILE],[NSString stringWithFormat:@"图片:%@已丢失，请检查",errorPath]]];
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
    return [UploadFileUtil httpDicImge:dic andImages:images andServicveName:fileKeys andFileNames:fileNames];
}

+(NSMutableDictionary *)httpDicImge:(NSMutableDictionary *)dic andImages:(NSMutableArray<NSMutableArray *> *)images andServicveName:(NSArray<NSString *> *)filekeys andFileNames:(NSMutableArray<NSMutableArray *> *)fileNames{
    [dic setValue:images forKey:KEY_IMAGES];
    [dic setValue:filekeys forKey:KEY_FILE_KEYS];
    [dic setValue:fileNames forKey:KEY_FILE_NAMES];
    return dic;
}



//封装的Post请求
-(void)httpPost:(NSString *)url andParams:(NSMutableDictionary *)parms{
    NSInteger uploadType = [[parms valueForKey:UPLOAD_TYPE] integerValue];
    NSString  *key         = url;
    NSMutableArray<NSMutableArray<UIImage *> *>  *images    = [parms valueForKey:KEY_IMAGES];
    NSMutableArray<NSString *>  *fileKeys     = [parms valueForKey:KEY_FILE_KEYS];
    NSMutableArray<NSMutableArray<NSString *> *>  *fileNames= [parms valueForKey:KEY_FILE_NAMES];
    NSLog(@"开始请求_url=%@",url);
    NSLog(@"开始请求_parms=%@",parms);
    
    [[AFManager shareManager] uploadMoreImageWithUrlString:url withOperations:parms withImageArray:images withServiceName:fileKeys  withFileName:fileNames withSuccessBlock:^(id result) {
        NSLog(@"success_result=%@",result);
        [self sendUploadResult:@[[NSNumber numberWithInteger:uploadType],[NSNumber numberWithInteger:UPLOAD_PROGRESS],[NSNumber numberWithFloat:1.00]] andSuccess:YES];
        [self sendUploadResult:@[[NSNumber numberWithInteger:uploadType],[NSNumber numberWithInteger:UPLOAD_SUCCESS],[self dictToJson:result]] andSuccess:YES];
    } withFailurBlock:^(NSError *error) {
        NSLog(@"error=%@",error);
        if([NetUtil netAvailable]){
            [self sendUploadResult:@[[NSNumber numberWithInteger:uploadType],[NSNumber numberWithInteger:UPLOAD_FAILE],@"上传失败"] andSuccess:NO];
        }else{
            [self sendUploadResult:@[[NSNumber numberWithInteger:uploadType],[NSNumber numberWithInteger:NET_NOT_AVAILABLE],@"网络不可用"] andSuccess:NO];
        }
    } withUpLoadProgress:^(float progress){
        [self sendUploadResult:@[[NSNumber numberWithInteger:uploadType],[NSNumber numberWithInteger:UPLOAD_PROGRESS],[NSNumber numberWithFloat:progress]] andSuccess:YES];
    } taskKey:key];
}

-(void)sendUploadResult:(NSArray *)message andSuccess:(BOOL)success{
    if(success){
        [self.plugin successWithMessage:message];
    }else{
        [self.plugin faileWithMessage:message];
    }
    
}

-(NSString *)dictToJson:(NSDictionary *)dict{
    NSError *error;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:dict options:NSJSONWritingPrettyPrinted error:&error];
    NSString *jsonString;
    if (!jsonData) {
        NSLog(@"%@",error);
    }else{
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

@end
