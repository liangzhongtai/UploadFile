//
//  InstanceSessionManager.m
//  Zhikucai
//
//  Created by 梁仲太 on 2018/4/21.
//  Copyright © 2018年 魔品. All rights reserved.
//

#import "InstanceSessionManager.h"

@implementation InstanceSessionManager

+(InstanceSessionManager *)instance{
    static InstanceSessionManager *_manager = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken,^{
        _manager = [[InstanceSessionManager alloc] init];
        _manager.requestSerializer.timeoutInterval = 10;
        _manager.securityPolicy = [AFSecurityPolicy policyWithPinningMode:AFSSLPinningModeNone];
    });
    return _manager;
}

-(InstanceSessionManager *)manager{
    if(!_manager){
        _manager = [InstanceSessionManager instance];
    }
    return _manager;
}

@end
