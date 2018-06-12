//
//  InstanceSessionManager.h
//  Zhikucai
//
//  Created by 梁仲太 on 2018/4/21.
//  Copyright © 2018年 魔品. All rights reserved.
//

#import "AFHTTPSessionManager.h"

@interface InstanceSessionManager : AFHTTPSessionManager
@property(nonatomic,strong)InstanceSessionManager *manager;
+(InstanceSessionManager *)instance;

@end
