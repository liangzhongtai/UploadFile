//
//  NetUtil.m
//  Zhikucai
//
//  Created by 梁仲太 on 2017/12/20.
//  Copyright © 2017年 魔品. All rights reserved.
//

#import "NetUtil.h"
#import <SystemConfiguration/SystemConfiguration.h>
#import <netinet/in.h>
#import "Reachability.h"

@implementation NetUtil
//网络是否可用
+(BOOL) netAvailable{
    //TODO
    BOOL isExistenceNetwork = YES;
    Reachability *reach = [Reachability reachabilityWithHostName:@"www.apple.com"];
    switch ([reach currentReachabilityStatus]) {
        case NotReachable:
            isExistenceNetwork = NO;
            //NSLog(@"notReachable");
            break;
        case ReachableViaWiFi:
            isExistenceNetwork = YES;
            //NSLog(@"WIFI");
            break;
        case ReachableViaWWAN:
            isExistenceNetwork = YES;
            //NSLog(@"3G");
            break;
    }
    return YES;
}
@end
