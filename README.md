<div>
  <br/><br />
  <p align="center">
    <a href="https://bambuser.com" target="_blank" align="center">
        <img src="https://brand.bambuser.net/current/logo/bambuser-black-512.png" width="280">
    </a>
  </p>
  <br/><br />
</div>

cordova-plugin-bambuser
-----------------------

[![Build Status](https://travis-ci.org/sven-inge/cordova-plugin-bambuser.svg?branch=master)](https://travis-ci.org/sven-inge/cordova-plugin-bambuser)

## Installation

In your Cordova project, run:

`cordova plugin add cordova-plugin-bambuser`


## Broadcaster usage

broadcaster.js implements a wrapper for Bambuser's native live broadcasting SDK:s
https://bambuser.com/docs/broadcasting/

See [www/broadcaster.js](./www/broadcaster.js) for details.

Use the Cordova-style callback-based methods of `window.bambuser`
from within your Cordova application.


### Callback-based example

```javascript
// http://cordova.apache.org/docs/en/8.x/cordova/events/events.html#deviceready
document.addEventListener('deviceready', onDeviceReady, false);
function onDeviceReady() {
  // Now safe to use device APIs
  if (!window.bambuser) {
    alert('cordova-plugin-bambuser is not installed');
  } else {
    var broadcaster = window.bambuser.broadcaster;
    broadcaster.setApplicationId('YOUR-APPLICATION-ID', function() {
      broadcaster.showViewfinderBehindWebView(function() {
        // maybe also set title etc here before starting
        // maybe move startBroadcast() to a button callback instead of starting right aways
        broadcaster.startBroadcast(function() {
          // update UI to show that we're live!
        }, function(err) { alert('failed to start broadcast'); });
      }, function(err) { alert('failed to show viewfinder'); });
    }, function(err) { alert('failed to set application id'); });
  }
}
```

### Promises
For promise support, omit both callbacks:
```javascript
document.addEventListener('deviceready', () => {
  const { broadcaster } = window.bambuser;
  broadcaster.setApplicationId('YOUR-APPLICATION-ID').then(() => {
    return broadcaster.showViewfinderBehindWebView()
  }).then(() => {
    // maybe also set title etc here before starting
    // maybe move startBroadcast() to a button callback instead of starting right aways
    return broadcaster.startBroadcast();
  }).catch(e => console.log(e));
}, false);
```

### async/await
```javascript
document.addEventListener('deviceready', async () => {
  try {
    const { broadcaster } = window.bambuser;
    await broadcaster.setApplicationId('YOUR-APPLICATION-ID');
    await broadcaster.showViewfinderBehindWebView();
    // maybe also set title etc here before starting
    // maybe move startBroadcast() to a button callback instead of starting right aways
    await broadcaster.startBroadcast();
  } catch (e) {
    console.log(e);
  }
});
```

### TypeScript
Typescript definitions are not currently included. As a workaround, it is often
possible to access the plugin's window property via the array syntax and declaring
a shortcut variable of type `any`:

```javascript
document.addEventListener('deviceready', async () => {
  if (!window['bambuser']) {
    alert('cordova-plugin-bambuser is not installed');
  } else {
    const broadcaster: any = window['bambuser']['broadcaster'];
    // ...
  }
});
```

Complete usage example:
https://github.com/bambuser/bambuser-examplebroadcaster-ionic


## Player usage

player.js implements a wrapper for Bambuser's native player SDK:s
https://bambuser.com/docs/playback/android-player/
https://bambuser.com/docs/playback/ios-player/

See [www/player.js](./www/player.js) for details.

### async/await
```javascript
// https://bambuser.com/docs/key-concepts/resource-uri/
const resourceUri = 'https://cdn.bambuser.net/broadcasts/0b9860dd-359a-67c4-51d9-d87402770319?da_signature_method=HMAC-SHA256&da_id=9e1b1e83-657d-7c83-b8e7-0b782ac9543a&da_timestamp=1482921565&da_static=1&da_ttl=0&da_signature=cacf92c6737bb60beb1ee1320ad85c0ae48b91f9c1fbcb3032f54d5cfedc7afe';

document.addEventListener('deviceready', async () => {
  try {
    const { player } = window.bambuser;
    await player.setApplicationId('YOUR-APPLICATION-ID');
    await player.showPlayerBehindWebView();
    this.player.loadBroadcast(resourceUri);
  } catch (e) {
    console.log(e);
  }
});
```

Complete usage examples, both native and javascript based:
https://github.com/bambuser/bambuser-exampleplayer-ionic

The web player is another playback option in a Cordova app,
which does not require a plugin, just a few configuration changes:
https://bambuser.com/docs/playback/web-player/#javascript-api
https://github.com/bambuser/bambuser-exampleplayer-ionic/blob/0cd44e9b09e2eaf2d4ee28918ecebebbfe863935/config.xml#L9-L14

However, on some less capable web view implementations, the web player
will regularly fall back to standard HLS delivery, resulting in a bit higher latency
during live playback, which can be a case for using the native player instead.

The web player currently has a richer event vocabulary on the other hand,
and can be used in a scrollable context unlike the cordova plugin.
Depending on the app, it might be worth trying out both alternatives
and perhaps use the native SDK for live streams and the web player
for archived playback.
