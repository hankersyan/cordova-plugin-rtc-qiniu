/*
 * @Author: hankers.yan
 * @Date: 2018-11-05
 */

#import "CDVQNRtc.h"
#import "QRDPublicHeader.h"
#import "QRDRTCViewController.h"

@interface CDVQNRtc ()


@end

@implementation CDVQNRtc

#pragma mark "API"

- (void)pluginInitialize {
    [self retrieveMainWindow];
    
    [self initCompleteBlock];
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
//        rtcVC.roomName = @"mh008";
//        rtcVC.userId = @"iPhone-for-03";
//        rtcVC.appId = @"d8lk7l4ed"; // dmqotunph
        rtcVC.configDic = configDic;
        rtcVC.videoEnabled = YES;
//        [self.viewController.navigationController pushViewController:rtcVC animated:YES];
        
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
            [self presentVC:rtcVC animated:YES completion:nil];
        }];
    }];
}

- (void)presentVC:(UIViewController *)vc animated:(BOOL)animated completion:(void (^)(void))completion
{
    if ([_window.rootViewController isKindOfClass:[UINavigationController class]]) {
        UINavigationController *nav = (UINavigationController*)_window.rootViewController;
        [nav pushViewController:vc animated:YES];
    } else {
        UINavigationController *nav2 = [[UINavigationController alloc] initWithRootViewController:vc];
        nav2.modalPresentationStyle = UIModalPresentationFullScreen;
        [_window.rootViewController presentViewController:nav2 animated:YES completion:nil];// presentModalViewController
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

- (void)initCompleteBlock {
    //__weak CDVQNRtc *weakSelf = self;
}

#pragma mark "Private methods"


@end
