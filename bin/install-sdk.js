var https = require('https');
var request = require('request');
var path = require('path');
var unzipper = require('unzipper');

var sdks = {
  android: {
    url: 'https://dist.bambuser.com/android_sdk/libbambuser-android-0.9.21-200424.zip',
  },
  ios: {
    url: 'https://dist.bambuser.com/ios_sdk/libbambuser-ios-0.9.28-200428.zip',
  },
};

if (process.argv.length < 3) {
  throw new Error('Please specify SDK');
}

var sdk = sdks[process.argv[2]];

if (!sdk) {
  throw new Error('Unknown SDK specified, valid options: ' + Object.keys(sdks).join(', '));
}

request = require('request');

var stream = request({url: sdk.url, pool: new https.Agent({keepAlive: false})});
stream.pipe(unzipper.Extract({path: path.join(__dirname, '..')}));
