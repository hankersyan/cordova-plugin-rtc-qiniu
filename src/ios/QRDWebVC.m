//
//  QRDWebVC.m
//  QNRtcDemo
//
//  Created by hankers on 2020/7/2.
//

#import "QRDWebVC.h"
#import <Cordova/CDVViewController.h>
#import "QRDRTCViewController.h"

@interface QRDWebVC ()
@end

float firstChildWidthPercent = 0.5f;

@implementation QRDWebVC

- (void)viewDidLoad {
    [super viewDidLoad];
    
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(OrientationDidChange:) name:UIDeviceOrientationDidChangeNotification object:nil];

    // Do any additional setup after loading the view.
    CGFloat w2 = self.view.bounds.size.width * firstChildWidthPercent;

    CDVViewController* web = [[CDVViewController alloc] init];
    NSString *configFile = [[NSBundle mainBundle] pathForResource:@"config_webview" ofType:@"xml"];
    BOOL fileExists = [[NSFileManager defaultManager] fileExistsAtPath:configFile];
    if (fileExists)
        [web setConfigFile:configFile];
    //add as a childviewcontroller
    [self addChildViewController:web];
    // Add the child's View as a subview
    [self.view addSubview:web.view];
    web.view.frame = CGRectMake(0, 0, w2, self.view.bounds.size.height);
    // tell the childviewcontroller it's contained in it's parent
    [web didMoveToParentViewController:self];
    
    double delayInSeconds = 0.5;
    dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delayInSeconds * NSEC_PER_SEC));
    dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
        [web.webViewEngine loadRequest:[NSURLRequest requestWithURL:[NSURL URLWithString:self.url]]];
    });
    
    QRDRTCViewController* rtc = [[QRDRTCViewController alloc] init];
    rtc.roomName = self.roomName;
    rtc.userId = self.userId;
    rtc.roomToken = self.roomToken;
    rtc.appId = self.appId;
    rtc.configDic = self.configDic;
    rtc.enableMergeStream = self.enableMergeStream;
    rtc.splitMode = true;
    //add as a childviewcontroller
    [self addChildViewController:rtc];
    // Add the child's View as a subview
    [self.view addSubview:rtc.view];
    rtc.view.frame = CGRectMake(w2, 0, self.view.bounds.size.width - w2, self.view.bounds.size.height);
    // tell the childviewcontroller it's contained in it's parent
    [rtc didMoveToParentViewController:self];
}

-(void)OrientationDidChange:(NSNotification*)notification {
    if (self.childViewControllers.count < 2)
        return;
    
    CGFloat w2 = self.view.bounds.size.width * firstChildWidthPercent;
    self.childViewControllers[0].view.frame = CGRectMake(0, 0, w2, self.view.bounds.size.height);
    self.childViewControllers[1].view.frame = CGRectMake(w2, 0, self.view.bounds.size.width - w2, self.view.bounds.size.height);
}

-(void) dealloc {
      [[NSNotificationCenter defaultCenter] removeObserver:self];
}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end
