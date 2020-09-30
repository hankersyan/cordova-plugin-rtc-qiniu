# cordova-plugin-rtc-qiniu

Cordova plugin for RTC/Video conference based on QiNiu Cloud. 基于七牛云实时音视频的视频会议插件.

Support Android/iOS. 支持安卓和苹果.

Please apply for APPID at https://www.qiniu.com/products/rtn . 请先申请你的APPID

![Bilby Stampede](https://www.qiniu.com/assets/sdk/img-shejiao-0f2d2d077c4f1bc0794b75dfc66e9e582446506e7bb795ebed0821b7af22ff86.png)

分屏+四/九宫格

![分屏](https://i-smart.oss-cn-shanghai.aliyuncs.com/static/split-screen.jpeg)

# Install

```bash
cordova plugin add cordova-plugin-rtc-qiniu --variable APPID=YOU_APPID
```

# 功能

- 视频会议
- 带web分屏的视频会议 (推荐pad)
- 布局最多九宫格

# 说明

1. 限制
   
   - 七牛云实时音视频 userId 仅允许字母、数字和下划线

2. 自定义用户信息：姓名。 
   
   - 提供用户信息 RESTful api 需自行开发。
   - 传入参数：user_info_url，该URL中的<USER_ID>字符串会被替换成实际值。
   - 返回的JSON格式：{ "name":"foo", "avatar":"http://your.domain.com/avatar.jpg" }

3. 带web分屏的视频会议

	- 需要在 config.xml 中加入 &lt;allow-navigation href="https://foo.bar" /&gt;
	- 可以通过 config_webview.xml 自定义分屏界面webview的配置，该文件跟 config.xml 同目录


#### config_webview.xml 示例

```xml
<?xml version='1.0' encoding='utf-8'?>
<widget id="com.qbox.QNRTCKitDemo" version="1.0.0" xmlns="http://www.w3.org/ns/widgets" xmlns:cdv="http://cordova.apache.org/ns/1.0">
    <feature name="Whitelist">
        <param name="android-package" value="org.apache.cordova.whitelist.WhitelistPlugin" />
        <param name="onload" value="true" />
    </feature>
	<!-- 指定更多插件 -->
    <content src="embedded.html" />
    <access origin="*" />
    <allow-intent href="*" />
    <allow-navigation href="*" />
    <preference name="loglevel" value="DEBUG" />
</widget>
```

# 用法

```Javascript
if (typeof QNRtc == 'undefined') {
	alert('QNRtc plugin not found');
	return;
}
var appId = 'd8lk7l4ed'; // 七牛云APPID
QNRtc.init({
	app_id: appId,
	user_info_url: 'http://your.domain.com/api/users/<USER_ID>', //用户信息api，<USER_ID>会被替换成实际值
});
var roomName = document.getElementById('room').value;
var userId = document.getElementById('name').value;
var bundleId = 'com.qbox.QNRTCKitDemo';
var oReq = new XMLHttpRequest();
oReq.addEventListener("load", function() {
	console.log(this.responseText);
	var isWithWeb = false; // 是否打开带web分屏的视频会议
	var para = {
		user_id: userId,
		room_name: roomName,
		enable_merge_stream: true, // 合流
		room_token: this.responseText,
		url: isWithWeb?"https://qq.com":undefined
	}
	if (isWithWeb) {
		QNRtc.startWithWeb(para); // 带web分屏
	} else {
		QNRtc.start(para);
	}
});
oReq.open("GET", "https://api-demo.qnsdk.com/v1/rtc/token/admin/" +
	"app/" + appId +
	"/room/" + roomName +
	"/user/" + userId +
	"?bundleId=" + bundleId);
oReq.send();
```

# Example
At first you need install npm/cordova.

```bash
cd ./example
./create-demo.sh
```
