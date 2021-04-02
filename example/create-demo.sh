cd ../..
cordova create qnrtcdemo com.qbox.QNRTCKitDemo QNRtcDemo
cd qnrtcdemo
cordova platform add ios android
cordova plugin add cordova-plugin-rtc-qiniu --variable APPID=dmqotunph --searchpath ../
cp -r ../cordova-plugin-rtc-qiniu/example/www .
cp ../cordova-plugin-rtc-qiniu/example/config.xml ./
cp ../cordova-plugin-rtc-qiniu/example/config_webview.xml ./platforms/android/app/src/main/res/xml/
cp ../cordova-plugin-rtc-qiniu/example/config_webview.xml ./platforms/ios/QNRtcDemo/
cordova prepare
