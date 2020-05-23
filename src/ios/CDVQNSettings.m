#import "CDVQNSettings.h"

@interface CDVQNSettings()

@end

@implementation CDVQNSettings

static NSString* appId;
+ (NSString*) appId {
    return appId;
}
+ (void) setAppId:(NSString*)val {
    appId = val;
}

static NSString* userInfoUrl;
+ (NSString*) userInfoUrl {
    return userInfoUrl;
}
+ (void) setUserInfoUrl:(NSString*)val {
    userInfoUrl = val;
}

@end
