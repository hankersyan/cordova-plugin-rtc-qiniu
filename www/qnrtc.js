/*
 * @Author: hankers.yan
 * @Date: 2018-11-05
 */
var exec = require('cordova/exec');

function isFunction(fn) {
    return Object.prototype.toString.call(fn) === '[object Function]';
}

module.exports = {
    init: function(param, success, error) {
        exec(success, error, "QNRtc", "init", [param]);
    },
    start: function(param, success, error) {
        if (isFunction(param)) {
            error = success;
            success = param;
            param = null;
        }
        exec(success, error, "QNRtc", "start", [param]);
    },
    startWithWeb: function(param, success, error) {
        if (isFunction(param)) {
            error = success;
            success = param;
            param = null;
        }
        exec(success, error, "QNRtc", "startWithWeb", [param]);
    }
};