/*
 * @Author: hankers.yan
 * @Date: 2018-11-05
 */

#import "CDVQNRtc.h"
#import "QRDPublicHeader.h"
#import "QRDRTCViewController.h"
#import "QRDWebVC.h"
#import "CDVQNSettings.h"

@interface CDVQNRtc ()


@end

@implementation CDVQNRtc

#pragma mark "API"

- (void)pluginInitialize {
    [self retrieveMainWindow];
    
    [self initCompleteBlock];
}

- (void)init:(CDVInvokedUrlCommand *)command {
    NSDictionary *param = [command.arguments objectAtIndex:0];
 
    [CDVQNSettings setAppId:[param objectForKey:@"app_id"]];
    [CDVQNSettings setUserInfoUrl:[param objectForKey:@"user_info_url"]];
}

- (void)start:(CDVInvokedUrlCommand *)command {
    [self.commandDelegate runInBackground:^{
        NSDictionary *param = [command.arguments objectAtIndex:0];

        NSDictionary *configDic = [[NSUserDefaults standardUserDefaults] objectForKey:QN_SET_CONFIG_KEY];
        if (!configDic) {
            configDic = @{@"VideoSize":NSStringFromCGSize(CGSizeMake(480, 640)), @"FrameRate":@20};
//            configDic = @{@"VideoSize":NSStringFromCGSize(CGSizeMake(720, 1280)), @"FrameRate":@20};
        }

        QRDRTCViewController *rtcVC = [[QRDRTCViewController alloc] init];
        rtcVC.roomName = [param objectForKey:@"room_name"];
        rtcVC.userId = [param objectForKey:@"user_id"];
        rtcVC.roomToken = [param objectForKey:@"room_token"];
        rtcVC.appId = [param objectForKey:@"app_id"];
        rtcVC.configDic = configDic;
        rtcVC.enableMergeStream = [param objectForKey:@"enable_merge_stream"];
//        rtcVC.videoEnabled = YES;
        
        NSLog(@"%@, %@, enableMergeStream=%d", rtcVC.roomName, rtcVC.userId, rtcVC.enableMergeStream);
        
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
            [self presentVC:rtcVC animated:YES completion:nil];
        }];
    }];
}

- (void)startWithWeb:(CDVInvokedUrlCommand *)command {
    [self.commandDelegate runInBackground:^{
        NSDictionary *param = [command.arguments objectAtIndex:0];

        NSDictionary *configDic = [[NSUserDefaults standardUserDefaults] objectForKey:QN_SET_CONFIG_KEY];
        if (!configDic) {
            configDic = @{@"VideoSize":NSStringFromCGSize(CGSizeMake(480, 640)), @"FrameRate":@20};
//            configDic = @{@"VideoSize":NSStringFromCGSize(CGSizeMake(720, 1280)), @"FrameRate":@20};
        }

        QRDWebVC* webvc = [[QRDWebVC alloc] init];
        webvc.roomName = [param objectForKey:@"room_name"];
        webvc.userId = [param objectForKey:@"user_id"];
        webvc.roomToken = [param objectForKey:@"room_token"];
        webvc.appId = [param objectForKey:@"app_id"];
        webvc.configDic = configDic;
        webvc.enableMergeStream = [param objectForKey:@"enable_merge_stream"];
        webvc.url = [param objectForKey:@"url"];

        NSLog(@"%@, %@, enableMergeStream=%d", webvc.roomName, webvc.userId, webvc.enableMergeStream);
        
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
            [self presentVC:webvc animated:YES completion:nil];
        }];
    }];
}

- (void)presentVC:(UIViewController *)vc animated:(BOOL)animated completion:(void (^)(void))completion
{
    if ([_window.rootViewController isKindOfClass:[UINavigationController class]]) {
        UINavigationController *nav = (UINavigationController*)_window.rootViewController;
        [nav pushViewController:vc animated:YES];
    } else {
        UIViewController* topVC = [CDVQNRtc topMostController];
        UINavigationController *nav2 = [[UINavigationController alloc] initWithRootViewController:vc];
        nav2.navigationBarHidden = YES;
        nav2.modalPresentationStyle = UIModalPresentationFullScreen;
        [topVC presentViewController:nav2 animated:YES completion:nil];
    }
}

- (void)retrieveMainWindow {
    self.window = [[[UIApplication sharedApplication] delegate] window];
    if (!self.window) {
        // for iOS13
        for (UIWindow *window in [UIApplication sharedApplication].windows) {
            if (window.isKeyWindow) {
                self.window = window;
                break;
            }
        }
    }
}

+ (UIViewController*)topMostController
{
    UIViewController *topController = [UIApplication sharedApplication].keyWindow.rootViewController;

    while (topController.presentedViewController) {
        topController = topController.presentedViewController;
    }

    return topController;
}

- (void)initCompleteBlock {
    //__weak CDVQNRtc *weakSelf = self;
}

#pragma mark "Private methods"


@end
