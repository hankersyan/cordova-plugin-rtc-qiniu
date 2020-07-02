//
//  QRDWebVC.h
//  QNRtcDemo
//
//  Created by hankers on 2020/7/2.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface QRDWebVC : UIViewController

@property (nonatomic, readwrite) NSString *userId;
@property (nonatomic, readwrite) NSString *appId;
@property (nonatomic, readwrite) NSString *roomName;
@property (nonatomic, readwrite) NSString *roomToken;
@property (nonatomic, readwrite) NSString *userInfoUrl;
@property (nonatomic, strong) NSDictionary *configDic;
@property (nonatomic, assign) BOOL enableMergeStream;
@property (nonatomic, readwrite) NSString *url;

@end

NS_ASSUME_NONNULL_END
