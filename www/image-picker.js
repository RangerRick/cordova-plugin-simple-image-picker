'use strict';

var exec = require('cordova/exec');

var ImagePicker = {
	pick: function(success, failure) {
		exec(success, failure, 'ImagePickerPlugin', 'pick', []);
	}
};

module.exports = ImagePicker;

if (!window.plugins) {
	window.plugins = {};
}
if (!window.plugins.ImagePicker) {
	window.plugins.ImagePicker = ImagePicker;
}
