/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
var app = {
    // Application Constructor
    initialize: function() {
        document.addEventListener('deviceready', this.onDeviceReady.bind(this), false);
    },

    // deviceready Event Handler
    //
    // Bind any cordova events here. Common events are:
    // 'pause', 'resume', etc.
    onDeviceReady: function() {
        this.receivedEvent('deviceready');
    },

    // Update DOM on a Received Event
    receivedEvent: function(id) {
        var parentElement = document.getElementById(id);
        var listeningElement = parentElement.querySelector('.listening');
        var receivedElement = parentElement.querySelector('.received');
        var btnElem = parentElement.querySelector('.button');

        listeningElement.setAttribute('style', 'display:none;');
        receivedElement.setAttribute('style', 'display:block;');

        btnElem.addEventListener('click', this.startConference.bind(this), false);
				document.getElementById('name').value = 'u' + Math.floor((Math.random() * 1000) + 1);

        console.log('Received Event: ' + id);
    },
    
    startConference: function() {
			if (typeof QNRtc == 'undefined') {
				alert('QNRtc plugin not found');
				return;
			}
			var appId = 'd8lk7l4ed';
			var roomName = document.getElementById('room').value;
			var userId = document.getElementById('name').value;
			var bundleId = 'com.qbox.QNRTCKitDemo';
			var oReq = new XMLHttpRequest();
			oReq.addEventListener("load", function() {
				console.log(this.responseText);
				var para = {
					app_id: appId,
					user_id: userId,
					room_name: roomName,
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
    }

};

app.initialize();
