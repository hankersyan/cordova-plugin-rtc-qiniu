/*
 * @Author: hankers.yan
 * @Date: 2018-11-05
 */
#import <Cordova/CDV.h>
#import <QNRTCKit/QNRTCKit.h>

@interface CDVQNRtc : CDVPlugin

@property (strong, nonatomic) UIWindow *window;

- (void)start:(CDVInvokedUrlCommand *)command;

+ (NSString*) getUserInfoUrl;

@end
