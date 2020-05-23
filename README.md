# cordova-plugin-rtc-qiniu

Cordova plugin for RTC/Video conference based on QiNiu Cloud. 基于七牛云实时音视频的视频会议插件.

Support Android/iOS. 支持安卓和苹果.

Please apply for APPID at https://www.qiniu.com/products/rtn . 请先申请你的APPID

![Bilby Stampede](https://www.qiniu.com/assets/sdk/img-shejiao-0f2d2d077c4f1bc0794b75dfc66e9e582446506e7bb795ebed0821b7af22ff86.png)


# Install

```bash
cordova plugin add cordova-plugin-rtc-qiniu --variable APIID=YOU_APPID
```

# 说明

1. 限制
   
   1.1 七牛云实时音视频 userId 仅允许字母、数字和下划线

2. 自定义用户信息：姓名。 
   
   2.1 提供用户信息 RESTful api 需自行开发。

   2.2 传入参数：user_info_url，该URL中的<USER_ID>字符串会被替换成实际值。

   2.3 返回的JSON格式：{ "name":"foo", "avatar":"http://your.domain.com/avatar.jpg" }

# 用法

```Javascript
if (typeof QNRtc == 'undefined') {
	alert('QNRtc plugin not found');
	return;
}
var appId = 'd8lk7l4ed';
var roomName = 'room711';
var userId = 'user007';
var bundleId = 'com.qbox.QNRTCKitDemo';
var oReq = new XMLHttpRequest();
oReq.addEventListener("load", function() {
	console.log(this.responseText);
	var para = {
		app_id: appId,
		user_id: userId,
		room_name: roomName,
		user_info_url: 'http://your.domain.com/api/users/<USER_ID>', //用户信息API，<USER_ID>会被替换成实际值
		room_token: this.responseText
	}
	QNRtc.start(para);
});
oReq.open("GET", "https://api-demo.qnsdk.com/v1/rtc/token/admin/"
	+"app/"+appId
	+"/room/"+roomName
	+"/user/"+userId
	+"?bundleId="+bundleId);
oReq.send();
```

# Example
At first you need install npm/cordova.

```bash
cd ./example
./create-demo.sh
```
