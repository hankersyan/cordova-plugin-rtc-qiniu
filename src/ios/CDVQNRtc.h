/*
 * @Author: hankers.yan
 * @Date: 2018-11-05
 */
#import <Cordova/CDV.h>
#import <QNRTCKit/QNRTCKit.h>

@interface CDVQNRtc : CDVPlugin

@property (strong, nonatomic) UIWindow *window;

- (void)init:(CDVInvokedUrlCommand *)command;
- (void)start:(CDVInvokedUrlCommand *)command;
- (void)startWithWeb:(CDVInvokedUrlCommand *)command;

@end
