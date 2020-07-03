//
//  QRDRTCViewController.m
//  QNRTCKitDemo
//
//  Created by 冯文秀 on 2018/1/18.
//  Copyright © 2018年 PILI. All rights reserved.
//
#import "QRDPublicHeader.h"

#import "QRDRTCViewController.h"
#import <ReplayKit/ReplayKit.h>
#import "UIView+Alert.h"
#import <QNRTCKit/QNRTCKit.h>
#import "QRDMergeInfo.h"


@interface QRDRTCViewController ()

@property (nonatomic, strong) NSMutableArray *mergeUserArray;
@property (nonatomic, strong) NSMutableArray *mergeInfoArray;
@property (nonatomic, assign) CGSize mergeStreamSize;
@property (nonatomic, strong) NSString *mergeJobId;

@end

@implementation QRDRTCViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    
    self.view.backgroundColor = QRD_COLOR_RGBA(20, 20, 20, 1);
        
    self.mergeStreamSize = CGSizeMake(480, 848);
    self.mergeInfoArray = [[NSMutableArray alloc] init];
    self.mergeUserArray = [[NSMutableArray alloc] init];

    self.videoEncodeSize = CGSizeFromString(_configDic[@"VideoSize"]);
    self.bitrate = [_configDic[@"Bitrate"] integerValue];
    [self setupEngine];
    [self setupBottomButtons];
    [self requestToken];
    
    self.logButton = [[UIButton alloc] init];
    [self.logButton setImage:[UIImage imageNamed:@"log-btn"] forState:UIControlStateNormal];
    [self.logButton addTarget:self action:@selector(logAction:) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:self.logButton];
    [self.view bringSubviewToFront:self.tableView];
    
    [self.logButton mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.view).offset(0);
        make.top.equalTo(self.mas_topLayoutGuide);
        make.size.equalTo(CGSizeMake(50, 50));
    }];
    
    [self.tableView mas_remakeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.logButton);
        make.top.equalTo(self.logButton.mas_bottom);
        make.width.height.equalTo(self.view).multipliedBy(0.6);
    }];
    self.tableView.hidden = YES;
    
    if ([self.roomToken length] > 0) {
        [self joinRTCRoom];
    }
}

- (void)conferenceAction:(UIButton *)conferenceButton {
    if ([self.navigationController isKindOfClass:[UINavigationController class]]) {
        if (self.navigationController.childViewControllers.count > 1) {
            UINavigationController* nav = (UINavigationController*)self.navigationController;
            [nav popViewControllerAnimated:YES];
        } else {
            UINavigationController* nav = (UINavigationController*)self.navigationController;
            [nav dismissViewControllerAnimated:YES completion:nil];
        }
    } else {
        [self dismissViewControllerAnimated:YES completion:nil];
    }
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)viewDidDisappear:(BOOL)animated {
    [self stoptimer];
    [self.engine leaveRoom];
    
    [super viewDidDisappear:animated];
}

- (void)setTitle:(NSString *)title {
    if (nil == self.titleLabel) {
        self.titleLabel = [[UILabel alloc] init];
        if (@available(iOS 9.0, *)) {
            self.titleLabel.font = [UIFont monospacedDigitSystemFontOfSize:14 weight:(UIFontWeightRegular)];
        } else {
            self.titleLabel.font = [UIFont systemFontOfSize:14];
        }
        self.titleLabel.textAlignment = NSTextAlignmentCenter;
        self.titleLabel.textColor = [UIColor whiteColor];
        [self.view addSubview:self.titleLabel];
    }
    self.titleLabel.text = title;
    [self.titleLabel sizeToFit];
    self.titleLabel.center = CGPointMake(self.view.center.x, self.logButton.center.y);
    [self.view bringSubviewToFront:self.titleLabel];
}

- (void)joinRTCRoom {
    [self.view showNormalLoadingWithTip:@"加入房间中..."];
    [self.engine joinRoomWithToken:self.roomToken];
}

- (void)requestToken {
    [self.view showFullLoadingWithTip:@"请求 token..."];
    //__weak typeof(self) wself = self;
    __weak QRDRTCViewController* wself = self;
    [QRDNetworkUtil requestTokenWithRoomName:self.roomName appId:self.appId userId:self.userId completionHandler:^(NSError *error, NSString *token) {
        
        [wself.view hideFullLoading];
        
        if (error) {
            [wself addLogString:error.description];
            [wself.view showFailTip:error.description];
            wself.title = @"请求 token 出错，请检查网络";
        } else {
            NSString *str = [NSString stringWithFormat:@"获取到 token: %@", token];
            [wself addLogString:str];
            
            wself.roomToken = token;
            [wself joinRTCRoom];
        }
    }];
}

- (void)setupEngine {
    
    self.engine = [[QNRTCEngine alloc] init];
    self.engine.delegate = self;
    self.engine.videoFrameRate = [_configDic[@"FrameRate"] integerValue];;
    self.engine.statisticInterval = 5;
    [self.engine setBeautifyModeOn:YES];
    
    [self.colorView addSubview:self.engine.previewView];
    [self.renderBackgroundView addSubview:self.colorView];
    
    [self.engine.previewView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.edges.equalTo(self.colorView);
    }];
    
    [self.colorView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.edges.equalTo(self.renderBackgroundView);
    }];
    
    [self.engine startCapture];
}

- (void)setupBottomButtons {
    
    self.bottomButtonView = [[UIView alloc] init];
    [self.view addSubview:self.bottomButtonView];
    
    UIButton* buttons[6];
    NSString *selectedImage[] = {
        @"microphone",
        @"loudspeaker",
        @"video-open",
        @"face-beauty-open",
        @"close-phone",
        @"camera-switch-front",
    };
    NSString *normalImage[] = {
        @"microphone-disable",
        @"loudspeaker-disable",
        @"video-close",
        @"face-beauty-close",
        @"close-phone",
        @"camera-switch-end",
    };
    SEL selectors[] = {
        @selector(microphoneAction:),
        @selector(loudspeakerAction:),
        @selector(videoAction:),
        @selector(beautyButtonClick:),
        @selector(conferenceAction:),
        @selector(toggleButtonClick:)
    };
    
    UIView *preView = nil;
    for (int i = 0; i < ARRAY_SIZE(normalImage); i ++) {
        buttons[i] = [[UIButton alloc] init];
        [buttons[i] setImage:[UIImage imageNamed:selectedImage[i]] forState:(UIControlStateSelected)];
        [buttons[i] setImage:[UIImage imageNamed:normalImage[i]] forState:(UIControlStateNormal)];
        [buttons[i] addTarget:self action:selectors[i] forControlEvents:(UIControlEventTouchUpInside)];
        [self.bottomButtonView addSubview:buttons[i]];
    }
    int index = 0;
    _microphoneButton = buttons[index ++];
    _speakerButton = buttons[index ++];
    _speakerButton.selected = YES;
    _videoButton = buttons[index ++];
    _beautyButton = buttons[index ++];
    _conferenceButton = buttons[index ++];
    _togCameraButton = buttons[index ++];
    _beautyButton.selected = YES;//默认打开美颜
    
    CGFloat buttonWidth = 54;
    NSInteger space = (UIScreen.mainScreen.bounds.size.width * (self.splitMode ? 0.5 : 1) - buttonWidth * 3)/4;
    
    NSArray *array = [NSArray arrayWithObjects:&buttons[3] count:3];
    [array mas_distributeViewsAlongAxis:(MASAxisTypeHorizontal) withFixedItemLength:buttonWidth leadSpacing:space tailSpacing:space];
    [array mas_makeConstraints:^(MASConstraintMaker *make) {
        make.height.equalTo(buttonWidth);
        make.bottom.equalTo(self.bottomButtonView).offset(-space * 0.8);
    }];
    
    preView = buttons[3];
    array = [NSArray arrayWithObjects:buttons count:3];
    [array mas_distributeViewsAlongAxis:(MASAxisTypeHorizontal) withFixedItemLength:buttonWidth leadSpacing:space tailSpacing:space];
    [array mas_makeConstraints:^(MASConstraintMaker *make) {
        make.height.equalTo(buttonWidth);
        make.bottom.equalTo(preView.mas_top).offset(-space * 0.8);
    }];
    
    preView = buttons[0];
    [self.bottomButtonView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.right.equalTo(self.view);
        make.bottom.equalTo(self.mas_bottomLayoutGuide);
        make.top.equalTo(preView.mas_top);
    }];
}

- (void)viewWillLayoutSubviews {
    [super viewWillLayoutSubviews];
    NSLog(@"bounds = %@", NSStringFromCGRect(self.view.bounds));
}

#pragma mark - 连麦时长计算

- (void)startTimer {
    [self stoptimer];
    self.durationTimer = [NSTimer timerWithTimeInterval:1
                                                 target:self
                                               selector:@selector(timerAction)
                                               userInfo:nil repeats:YES];
    [[NSRunLoop mainRunLoop] addTimer:self.durationTimer forMode:NSRunLoopCommonModes];
}

- (void)timerAction {
    self.duration ++;
    NSString *str = [NSString stringWithFormat:@"%02ld:%02ld", self.duration / 60, self.duration % 60];
    self.title = str;
}

- (void)stoptimer {
    if (self.durationTimer) {
        [self.durationTimer invalidate];
        self.durationTimer = nil;
    }
}

- (void)beautyButtonClick:(UIButton *)beautyButton {
    beautyButton.selected = !beautyButton.selected;
    [self.engine setBeautifyModeOn:beautyButton.selected];
}

- (void)toggleButtonClick:(UIButton *)button {
    [self.engine toggleCamera];
}

- (void)microphoneAction:(UIButton *)microphoneButton {
    self.microphoneButton.selected = !self.microphoneButton.isSelected;
    [self.engine muteAudio:!self.microphoneButton.isSelected];
}

- (void)loudspeakerAction:(UIButton *)loudspeakerButton {
    self.engine.muteSpeaker = !self.engine.isMuteSpeaker;
    loudspeakerButton.selected = !self.engine.isMuteSpeaker;
}

- (void)videoAction:(UIButton *)videoButton {
    videoButton.selected = !videoButton.isSelected;
    NSMutableArray *videoTracks = [[NSMutableArray alloc] init];
    if (self.screenTrackInfo) {
        self.screenTrackInfo.muted = !videoButton.isSelected;
        [videoTracks addObject:self.screenTrackInfo];
    }
    if (self.cameraTrackInfo) {
        [videoTracks addObject:self.cameraTrackInfo];
        self.cameraTrackInfo.muted = !videoButton.isSelected;
    }
    [self.engine muteTracks:videoTracks];
    
    self.engine.previewView.hidden = !videoButton.isSelected;
    [self checkSelfPreviewGesture];
}

- (void)logAction:(UIButton *)button {
    button.selected = !button.isSelected;
    if (button.selected) {
        if ([self.tableView numberOfRowsInSection:0] != self.logStringArray.count) {
            [self.tableView reloadData];
        }
    }
    self.tableView.hidden = !button.selected;
}

- (void)publish {
    
    QNTrackInfo *audioTrack = [[QNTrackInfo alloc] initWithSourceType:QNRTCSourceTypeAudio master:YES];
    QNTrackInfo *cameraTrack =  [[QNTrackInfo alloc] initWithSourceType:(QNRTCSourceTypeCamera)
                                                                    tag:cameraTag
                                                                 master:YES
                                                             bitrateBps:self.bitrate
                                                        videoEncodeSize:self.videoEncodeSize];
    
    [self.engine publishTracks:@[audioTrack, cameraTrack]];
}

- (void)showAlertWithMessage:(NSString *)message completionHandler:(void (^)(void))handler
{
    UIAlertController *controller = [UIAlertController alertControllerWithTitle:@"错误" message:message preferredStyle:UIAlertControllerStyleAlert];
    [controller addAction:[UIAlertAction actionWithTitle:@"确定" style:UIAlertActionStyleCancel handler:^(UIAlertAction * _Nonnull action) {
        if (handler) {
            handler();
        }
    }]];
    [self presentViewController:controller animated:YES completion:nil];
}

#pragma mark - QNRTCEngineDelegate

/**
 * SDK 运行过程中发生错误会通过该方法回调，具体错误码的含义可以见 QNTypeDefines.h 文件
 */
- (void)RTCEngine:(QNRTCEngine *)engine didFailWithError:(NSError *)error {
    [super RTCEngine:engine didFailWithError:error];
    
    dispatch_async(dispatch_get_main_queue(), ^{
        [self hideLoading];

        NSString *errorMessage = error.localizedDescription;
        if (error.code == QNRTCErrorReconnectTokenError) {
            errorMessage = @"重新进入房间超时";
        }
        [self showAlertWithMessage:errorMessage completionHandler:^{
            [self dismissViewControllerAnimated:YES completion:nil];
        }];
    });
}

/**
 * 房间状态变更的回调。当状态变为 QNRoomStateReconnecting 时，SDK 会为您自动重连，如果希望退出，直接调用 leaveRoom 即可
 */
- (void)RTCEngine:(QNRTCEngine *)engine roomStateDidChange:(QNRoomState)roomState {
    [super RTCEngine:engine roomStateDidChange:roomState];
    
    dispatch_async(dispatch_get_main_queue(), ^{
        [self hideLoading];

        if (QNRoomStateConnected == roomState || QNRoomStateReconnected == roomState) {
            [self startTimer];
        } else {
            [self stoptimer];
        }
        
        if (QNRoomStateConnected == roomState) {
            [self.view showSuccessTip:@"加入房间成功"];
            self.videoButton.selected = YES;
            self.microphoneButton.selected = YES;
            [self publish];
        } else if (QNRoomStateIdle == roomState) {
            self.videoButton.enabled = NO;
            self.videoButton.selected = NO;
        } else if (QNRoomStateReconnecting == roomState) {
            [self.view showNormalLoadingWithTip:@"正在重连..."];
            self.title = @"正在重连...";
            self.videoButton.enabled = NO;
            self.microphoneButton.enabled = NO;
        } else if (QNRoomStateReconnected == roomState) {
            [self.view showSuccessTip:@"重新加入房间成功"];
            self.videoButton.enabled = YES;
            self.microphoneButton.enabled = YES;
        }
    });
}

- (void)RTCEngine:(QNRTCEngine *)engine didPublishLocalTracks:(NSArray<QNTrackInfo *> *)tracks {
    [super RTCEngine:engine didPublishLocalTracks:tracks];
    
    dispatch_main_async_safe(^{
        [self hideLoading];
        [self.view showSuccessTip:@"发布成功了"];
        
        for (QNTrackInfo *trackInfo in tracks) {
            if (trackInfo.kind == QNTrackKindAudio) {
                self.microphoneButton.enabled = YES;
                self.isAudioPublished = YES;
                self.audioTrackInfo = trackInfo;
                continue;
            }
            if (trackInfo.kind == QNTrackKindVideo) {
                if ([trackInfo.tag isEqualToString:screenTag]) {
                    self.screenTrackInfo = trackInfo;
                    self.isScreenPublished = YES;
                } else {
                    self.videoButton.enabled = YES;
                    self.isVideoPublished = YES;
                    self.cameraTrackInfo = trackInfo;
                }
                continue;
            }
        }
    });
}

/**
 * 远端用户发布音/视频的回调
 */
- (void)RTCEngine:(QNRTCEngine *)engine didPublishTracks:(NSArray<QNTrackInfo *> *)tracks ofRemoteUserId:(NSString *)userId {
    
    NSString *str = [NSString stringWithFormat:@"远端用户: %@ 发布成功的回调:\nTracks: %@",  userId, tracks];
    [self addLogString:str];
    
    dispatch_main_async_safe(^{
        [self addMergeInfoWithTracks:tracks userId:userId];
        if ([self enableMergeStream] && [self isFirstUser]) {
            [self resetMergeFrame];
        }
        //[self resetUserList];
    });
}

/**
 * 远端用户取消发布音/视频的回调
 */
- (void)RTCEngine:(QNRTCEngine *)engine didUnPublishTracks:(NSArray<QNTrackInfo *> *)tracks ofRemoteUserId:(NSString *)userId {
    [super RTCEngine:engine didUnPublishTracks:tracks ofRemoteUserId:userId];
        
    NSString *str = [NSString stringWithFormat:@"远端用户: %@ 取消发布的回调:\nTracks: %@",  userId, tracks];
    [self addLogString:str];

    dispatch_main_async_safe(^{
        [self removeMergeInfoWithTracks:tracks userId:userId];
        if ([self enableMergeStream] && [self isFirstUser]) {
            [self resetMergeFrame];
        }
//        [self resetUserList];
    })

    dispatch_main_async_safe(^{
        for (QNTrackInfo *trackInfo in tracks) {
            QRDUserView *userView = [self userViewWithUserId:userId];
            QNTrackInfo *tempInfo = [userView trackInfoWithTrackId:trackInfo.trackId];
            if (tempInfo) {
                [userView.traks removeObject:tempInfo];
                
                if (trackInfo.kind == QNTrackKindVideo) {
                    if ([trackInfo.tag isEqualToString:screenTag]) {
                        [userView hideScreenView];
                    } else {
                        [userView hideCameraView];
                    }
                } else {
                    [userView setMuteViewHidden:YES];
                }
                
                if (0 == userView.traks.count) {
                    [self removeRenderViewFromSuperView:userView];
                }
            }
        }
    });
}

/**
 * 被 userId 踢出的回调
 */
- (void)RTCEngine:(QNRTCEngine *)engine didKickoutByUserId:(NSString *)userId {
    //    [super RTCSession:session didKickoutByUserId:userId];
    
    NSString *str = [NSString stringWithFormat:@"你被用户 %@ 踢出房间", userId];
    
    dispatch_main_async_safe(^{
        [self.view showTip:str];
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(2 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
            if (self.presentingViewController) {
                [self dismissViewControllerAnimated:YES completion:nil];
            } else {
                [self.navigationController popViewControllerAnimated:YES];
            }
        });
    });
}

- (void)RTCEngine:(QNRTCEngine *)engine didSubscribeTracks:(NSArray<QNTrackInfo *> *)tracks ofRemoteUserId:(NSString *)userId {
    [super RTCEngine:engine didSubscribeTracks:tracks ofRemoteUserId:userId];
    
    dispatch_async(dispatch_get_main_queue(), ^{
        
        for (QNTrackInfo *trackInfo in tracks) {
            QRDUserView *userView = [self userViewWithUserId:userId];
            if (!userView) {
                userView = [self createUserViewWithTrackId:trackInfo.trackId userId:userId];
                [self.userViewArray addObject:userView];
                NSLog(@"createRenderViewWithTrackId: %@", trackInfo.trackId);
            }
            if (nil == userView.superview) {
                [self addRenderViewToSuperView:userView];
            }
            
            QNTrackInfo *tempInfo = [userView trackInfoWithTrackId:trackInfo.trackId];
            if (tempInfo) {
                [userView.traks removeObject:tempInfo];
            }
            [userView.traks addObject:trackInfo];
            
            if (trackInfo.kind == QNTrackKindVideo) {
                if ([trackInfo.tag isEqualToString:screenTag]) {
                    if (trackInfo.muted) {
                        [userView hideScreenView];
                    } else {
                        [userView showScreenView];
                    }
                } else {
                    if (trackInfo.muted) {
                        [userView hideCameraView];
                    } else {
                        [userView showCameraView];
                    }
                }
            } else if (trackInfo.kind == QNTrackKindAudio) {
                [userView setMuteViewHidden:NO];
                [userView setAudioMute:trackInfo.muted];
            }
        }
    });
}

/**
 * 远端用户视频首帧解码后的回调，如果需要渲染，则需要返回一个带 renderView 的 QNVideoRender 对象
 */
- (QNVideoRender *)RTCEngine:(QNRTCEngine *)engine firstVideoDidDecodeOfTrackId:(NSString *)trackId remoteUserId:(NSString *)userId {
    [super RTCEngine:engine firstVideoDidDecodeOfTrackId:trackId remoteUserId:userId];
    
    QRDUserView *userView = [self userViewWithUserId:userId];
    if (!userView) {
        [self.view showFailTip:@"逻辑错误了 firstVideoDidDecodeOfRemoteUserId 中没有获取到 VideoView"];
    }
    
    userView.contentMode = UIViewContentModeScaleAspectFit;
    QNVideoRender *render = [[QNVideoRender alloc] init];
    
    QNTrackInfo *trackInfo = [userView trackInfoWithTrackId:trackId];
    render.renderView =   [trackInfo.tag isEqualToString:screenTag] ? userView.screenView : userView.cameraView;
    return render;
}

/**
 * 远端用户视频取消渲染到 renderView 上的回调
 */
- (void)RTCEngine:(QNRTCEngine *)engine didDetachRenderView:(UIView *)renderView ofTrackId:(NSString *)trackId remoteUserId:(NSString *)userId {
    [super RTCEngine:engine didDetachRenderView:renderView ofTrackId:trackId remoteUserId:userId];
    
    QRDUserView *userView = [self userViewWithUserId:userId];
    if (userView) {
        QNTrackInfo *trackInfo = [userView trackInfoWithTrackId:trackId];
        if ([trackInfo.tag isEqualToString:screenTag]) {
            [userView hideScreenView];
        } else {
            [userView hideCameraView];
        }
        //        [self removeRenderViewFromSuperView:userView];
    }
}

/**
 * 远端用户音频状态变更为 muted 的回调
 */
- (void)RTCEngine:(QNRTCEngine *)engine didAudioMuted:(BOOL)muted ofTrackId:(NSString *)trackId byRemoteUserId:(NSString *)userId {
    [super RTCEngine:engine didAudioMuted:muted ofTrackId:trackId byRemoteUserId:userId];
    
    QRDUserView *userView = [self userViewWithUserId:userId];
    [userView setAudioMute:muted];
}

/**
 * 远端用户视频状态变更为 muted 的回调
 */
- (void)RTCEngine:(QNRTCEngine *)engine didVideoMuted:(BOOL)muted ofTrackId:(NSString *)trackId byRemoteUserId:(NSString *)userId {
    [super RTCEngine:engine didVideoMuted:muted ofTrackId:trackId byRemoteUserId:userId];
    
    QRDUserView *userView = [self userViewWithUserId:userId];
    QNTrackInfo *trackInfo = [userView trackInfoWithTrackId:trackId];
    if ([trackInfo.tag isEqualToString:screenTag]) {
        if (muted) {
            [userView hideScreenView];
        } else {
            [userView showScreenView];
        }
    } else {
        if (muted) {
            [userView hideCameraView];
        } else {
            [userView showCameraView];
        }
    }
}

-(void) hideLoading {
    [self.view hiddenLoading];
    
    __weak QRDRTCViewController* that = self;
    
    double delayInSeconds = 2.0;
    dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, delayInSeconds * NSEC_PER_SEC);
    dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
       [that.view hiddenLoading];
    });
}

- (void)addMergeInfoWithTracks:(NSArray *)tracks userId:(NSString *)userId {
    
    for (QNTrackInfo *trackInfo in tracks) {
        QRDMergeInfo *mergeInfo = [[QRDMergeInfo alloc] init];
        mergeInfo.trackId = trackInfo.trackId;
        mergeInfo.userId = userId;
        mergeInfo.kind = trackInfo.kind;
        mergeInfo.merged = YES;
        mergeInfo.trackTag = trackInfo.tag;
        
        if (trackInfo.kind == QNTrackKindVideo) {
            [self.mergeInfoArray insertObject:mergeInfo atIndex:0];
        }
        else {
            [self.mergeInfoArray addObject:mergeInfo];
        }
    }
    
    if (![self.mergeUserArray containsObject:userId]) {
        [self.mergeUserArray addObject:userId];
    }
}

- (void)removeMergeInfoWithTracks:(NSArray *)tracks userId:(NSString *)userId {
    for (QNTrackInfo *trackInfo in tracks) {
        [self removeMergeInfoWithTrackId:trackInfo.trackId];
    }
    
    BOOL deleteUser = YES;
    for (QRDMergeInfo *info in self.mergeInfoArray) {
        if ([info.userId isEqualToString:userId]) {
            deleteUser = NO;
            break;
        }
    }
    if (deleteUser) {
        [self.mergeUserArray removeObject:userId];
    }
}

- (void)removeMergeInfoWithUserId:(NSString *)userId {
    if (self.mergeInfoArray.count <= 0) {
        return;
    }
    
    for (NSInteger index = self.mergeInfoArray.count - 1; index >= 0; index--) {
        QRDMergeInfo *info = self.mergeInfoArray[index];
        if ([info.userId isEqualToString:userId]) {
            [self.mergeInfoArray removeObject:info];
        }
    }
    
    [self.mergeUserArray removeObject:userId];
}

- (void)removeMergeInfoWithTrackId:(NSString *)trackId {
    if (self.mergeInfoArray.count <= 0) {
        return;
    }
    
    for (NSInteger index = self.mergeInfoArray.count - 1; index >= 0; index--) {
        QRDMergeInfo *info = self.mergeInfoArray[index];
        if ([info.trackId isEqualToString:trackId]) {
            [self.mergeInfoArray removeObject:info];
        }
    }
}

- (void)resetMergeFrame {

    //  每当有用户发布或者取消发布的时候，都重置合流参数
    NSMutableArray *videoMergeArray = [[NSMutableArray alloc] init];
    for (QRDMergeInfo *info in self.mergeInfoArray) {
        if (info.merged && QNTrackKindVideo == info.kind) {
            [videoMergeArray addObject:info];
        }
    }
    
    if (videoMergeArray.count > 0) {
        NSArray *mergeFrameArray = [self getTrackMergeFrame:(int)videoMergeArray.count];
        
        for (int i = 0; i < mergeFrameArray.count; i ++) {
            QRDMergeInfo * info = [videoMergeArray objectAtIndex:i ];
            info.mergeFrame = [[mergeFrameArray objectAtIndex:i] CGRectValue];
        }
    }
    
    NSMutableArray *array = [NSMutableArray new];
    for (QRDMergeInfo *info in self.mergeInfoArray) {
        if (info.isMerged) {
            QNMergeStreamLayout *layout = [[QNMergeStreamLayout alloc] init];
            layout.trackId = info.trackId;
            layout.frame = info.mergeFrame;
            layout.zIndex = info.zIndex;
            [array addObject:layout];
        }
    }
    
    if (array.count > 0) {
        [self.engine setMergeStreamLayouts:array jobId:self.mergeJobId];
    }
}

- (NSArray <NSValue *>*)getTrackMergeFrame:(int)count {
    
    NSMutableArray *frameArray = [[NSMutableArray alloc] init];
    if (1 == count) {
        CGRect rc = CGRectMake(0, 0, self.mergeStreamSize.width, self.mergeStreamSize.height);
        NSValue *value = [NSValue valueWithCGRect:rc];
        [frameArray addObject:value];
        return frameArray;
    }
    
    int power = log2(count);
    int bigFrameCount = pow(2, power);
    int left = count - bigFrameCount;
    
    int widthPower = power / 2;
    int heightPower = power - power / 2;
    
    CGRect *pRect = (CGRect *)malloc(sizeof(CGRect) * bigFrameCount);
    int row = pow(2, heightPower);
    int col = pow(2, widthPower);
    CGFloat width = self.mergeStreamSize.width / (pow(2, widthPower));
    CGFloat height = self.mergeStreamSize.height / (pow(2, heightPower));
    
    for (int i = 0; i < row; i ++) {
        for (int j = 0; j < col; j ++) {
            pRect[i * col + j].origin.x = j * width;
            pRect[i * col + j].origin.y = i * height;
            pRect[i * col + j].size.width = width;
            pRect[i * col + j].size.height = height;
        }
    }
    
    if (power % 2 == 0) {
        // 需要横着补刀
        for (int i = 0; i < left; i ++) {
            CGRect rc = pRect[i];
            CGRect rc1 = rc;
            rc1.size.height = rc.size.height / 2;
            CGRect rc2 = rc;
            rc2.origin.y = rc.origin.y + rc.size.height / 2;
            rc2.size.height = rc.size.height / 2;
            
            NSValue *value = [NSValue valueWithCGRect:rc1];
            [frameArray addObject:value];
            value = [NSValue valueWithCGRect:rc2];
            [frameArray addObject:value];
        }
        for (int i = left; i < bigFrameCount; i ++) {
            CGRect rc = pRect[i];
            NSValue *value = [NSValue valueWithCGRect:rc];
            [frameArray addObject:value];
        }
    } else {
        // 需要竖着补刀
        for (int i = 0; i < left; i ++) {
            CGRect rc = pRect[i];
            CGRect rc1 = rc;
            rc1.size.width = rc.size.width / 2;
            CGRect rc2 = rc;
            rc2.origin.x = rc.origin.x + rc.size.width / 2;
            rc2.size.width = rc.size.width / 2;
            
            NSValue *value = [NSValue valueWithCGRect:rc1];
            [frameArray addObject:value];
            value = [NSValue valueWithCGRect:rc2];
            [frameArray addObject:value];
        }
        
        for (int i = left; i < bigFrameCount; i ++) {
            CGRect rc = pRect[i];
            NSValue *value = [NSValue valueWithCGRect:rc];
            [frameArray addObject:value];
        }
    }
    
    free(pRect);
    
    return frameArray;
}

-(BOOL)isFirstUser {
    if ([[self.engine userList] count] > 0) {
        NSString *firstUser = [[self.engine userList] objectAtIndex:0];
        return [firstUser caseInsensitiveCompare:self.userId];
    }
    return NO;
}

@end
