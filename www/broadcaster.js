var Broadcaster = {}

var exec = require('cordova/exec');
var utils = require('cordova/utils');

var queue = Promise.resolve();

var execQueue = function() {
    var execArgs = Array.prototype.slice.call(arguments);
    queue = queue.then(function() {
        return new Promise(function(resolve) {
            var userCb = execArgs.shift();
            var userEb = execArgs.shift();
            execArgs.unshift(function() {
                // Custom errback that resolves the queue promise before triggering actual errback
                resolve();
                if (userEb) userEb.apply(null, arguments);
            });
            execArgs.unshift(function() {
                // Custom callback that resolves the queue promise before triggering actual callback
                resolve();
                if (userCb) userCb.apply(null, arguments);
            });
            exec.apply(null, execArgs);
        });
    });
};

Broadcaster._applicationIdSet = false;

Broadcaster.setApplicationId = function(applicationId, successCallback, errorCallback) {
    var res;
    if (!successCallback) {
        res = new Promise(function (resolve, reject) { successCallback = resolve; errorCallback = reject; });
    }
    if (Broadcaster._applicationIdSet) {
        errorCallback('applicationId is already set');
        return res;
    }
    if (!applicationId) {
        errorCallback('An applicationId is required');
        return res;
    }
    Broadcaster._applicationIdSet = true;
    execQueue(successCallback, errorCallback, 'CordovaBambuserBroadcaster', 'setApplicationId', [applicationId]);
    return res;
}


Broadcaster.showViewfinderBehindWebView = function(successCallback, errorCallback) {
    var res;
    if (!successCallback) {
        res = new Promise(function (resolve, reject) { successCallback = resolve; errorCallback = reject; });
    }
    execQueue(function() {
        if (successCallback) {
          successCallback.call(this, arguments);
        }
    }, function() {
        console.log('showViewfinderBehindWebView failure');
        togglingViewfinder = false;
        if (errorCallback) {
          errorCallback.call(this, arguments);
        }
    }, 'CordovaBambuserBroadcaster', 'showViewfinderBehindWebView', []);
    return res;
}

Broadcaster.hideViewfinder = function(successCallback, errorCallback) {
    var res;
    if (!successCallback) {
        res = new Promise(function (resolve, reject) { successCallback = resolve; errorCallback = reject; });
    }
    execQueue(function() {
        if (successCallback) {
          successCallback.call(this, arguments);
        }
    }, function() {
        console.log('hideViewfinder failure');
        if (errorCallback) {
          errorCallback.call(this, arguments);
        }
    }, 'CordovaBambuserBroadcaster', 'hideViewfinder', []);
    return res;
}

Broadcaster.setCustomData = function(customData, successCallback, errorCallback) {
    var res;
    if (!successCallback) {
        res = new Promise(function (resolve, reject) { successCallback = resolve; errorCallback = reject; });
    }
    if (!Broadcaster._applicationIdSet) {
        errorCallback('applicationId must be set first');
        return res;
    }
    execQueue(successCallback, errorCallback, 'CordovaBambuserBroadcaster', 'setCustomData', [customData]);
    return res;
}

Broadcaster.setSendPosition = function(value, successCallback, errorCallback) {
    var res;
    if (!successCallback) {
        res = new Promise(function (resolve, reject) { successCallback = resolve; errorCallback = reject; });
    }
    if (!Broadcaster._applicationIdSet) {
        errorCallback('applicationId must be set first');
        return res;
    }
    execQueue(successCallback, errorCallback, 'CordovaBambuserBroadcaster', 'setSendPosition', [value]);
    return res;
}

Broadcaster.setTitle = function(title, successCallback, errorCallback) {
    var res;
    if (!successCallback) {
        res = new Promise(function (resolve, reject) { successCallback = resolve; errorCallback = reject; });
    }
    if (!Broadcaster._applicationIdSet) {
        errorCallback('applicationId must be set first');
        return res;
    }
    execQueue(successCallback, errorCallback, 'CordovaBambuserBroadcaster', 'setTitle', [title]);
    return res;
}

Broadcaster.setAuthor = function(author, successCallback, errorCallback) {
    var res;
    if (!successCallback) {
        res = new Promise(function (resolve, reject) { successCallback = resolve; errorCallback = reject; });
    }
    if (!Broadcaster._applicationIdSet) {
        errorCallback('applicationId must be set first');
        return res;
    }
    execQueue(successCallback, errorCallback, 'CordovaBambuserBroadcaster', 'setAuthor', [author]);
    return res;
}

Broadcaster.setSaveOnServer = function (saveOnServer, successCallback, errorCallback) {
    var res;
    if (!successCallback) {
        res = new Promise(function (resolve, reject) { successCallback = resolve; errorCallback = reject; });
    }
    if (!Broadcaster._applicationIdSet) {
        errorCallback('applicationId must be set first');
        return res;
    }
    execQueue(successCallback, errorCallback, 'CordovaBambuserBroadcaster', 'setSaveOnServer', [saveOnServer]);
    return res;
}

/**
 * Deprecated: auto resolution is always used
 */
Broadcaster.setVideoQualityPreset = function(preset, successCallback, errorCallback) {
    var res;
    if (!successCallback) {
        res = new Promise(function (resolve, reject) { successCallback = resolve; errorCallback = reject; });
    }
    successCallback();
    return res;
}

Broadcaster.startBroadcast = function(successCallback, errorCallback) {
    var res;
    if (!successCallback) {
        res = new Promise(function (resolve, reject) { successCallback = resolve; errorCallback = reject; });
    }
    if (!Broadcaster._applicationIdSet) {
        errorCallback('applicationId must be set first');
        return res;
    }
    var startBroadcastResponseSent = false;
    var id = Broadcaster.addEventListener('connectionStatusChange', function(status) {
        if (status === 'capturing') {
            Broadcaster.removeEventListener(id);
            if (!startBroadcastResponseSent) {
                startBroadcastResponseSent = true;
                successCallback(status);
            }
            return;
        }
        if (status === 'finishing' || status === 'idle') {
            Broadcaster.removeEventListener(id);
            if (!startBroadcastResponseSent) {
                console.log('Broadcaster.startBroadcast: Broadcasting stopped before reaching capture state');
                startBroadcastResponseSent = true;
                errorCallback(status);
            }
            return;
        }
    });
    var errListenerId = Broadcaster.addEventListener('connectionError', function(status) {
        Broadcaster.removeEventListener(errListenerId);
        if (!startBroadcastResponseSent) {
            startBroadcastResponseSent = true;
            errorCallback(status);
        }
        return;
    });
    execQueue(function() {}, function() {
      if (!startBroadcastResponseSent) {
        startBroadcastResponseSent = true;
        if (errorCallback) errorCallback.call(this, arguments);
      }
    }, 'CordovaBambuserBroadcaster', 'startBroadcast', []);
    return res;
};

Broadcaster.stopBroadcast = function(successCallback, errorCallback) {
    var res;
    if (!successCallback) {
        res = new Promise(function (resolve, reject) { successCallback = resolve; errorCallback = reject; });
    }
    if (!Broadcaster._applicationIdSet) {
        errorCallback('applicationId must be set first');
        return res;
    }
    var id = Broadcaster.addEventListener('connectionStatusChange', function(status) {
        if (status === 'idle') {
            console.log('Broadcaster.stopBroadcast: Broadcasting stopped');
            Broadcaster.removeEventListener(id);
            successCallback();
            return;
        }
    });
    execQueue(function() {}, errorCallback, 'CordovaBambuserBroadcaster', 'stopBroadcast', []);
    return res;
};

Broadcaster.switchCamera = function(successCallback, errorCallback) {
    var res;
    if (!successCallback) {
        res = new Promise(function (resolve, reject) { successCallback = resolve; errorCallback = reject; });
    }
    if (!Broadcaster._applicationIdSet) {
        errorCallback('applicationId must be set first');
        return res;
    }
    execQueue(successCallback, errorCallback, 'CordovaBambuserBroadcaster', 'switchCamera', []);
    return res;
};

Broadcaster.toggleTorchLight = function(successCallback, errorCallback) {
    var res;
    if (!successCallback) {
        res = new Promise(function (resolve, reject) { successCallback = resolve; errorCallback = reject; });
    }
    execQueue(function() {
        if (successCallback) {
          successCallback.call(this, arguments);
        }
    }, function() {
        if (errorCallback) {
          errorCallback.call(this, arguments);
        }
    }, 'CordovaBambuserBroadcaster', 'toggleTorchLight', []);
    return res;
}

Broadcaster._eventListeners = {};

Broadcaster.addEventListener = function(event, successCallback, errorCallback) {
    var id = utils.createUUID();
    Broadcaster._eventListeners[id] = {
        event: event,
        callback: successCallback,
    };
    Broadcaster._ensureSubscribed();
    return id;
};

Broadcaster.removeEventListener = function(id) {
    delete Broadcaster._eventListeners[id];
}

Broadcaster._emitEvent = function(eventName, payload) {
    Object.keys(Broadcaster._eventListeners).forEach(function(id) {
        if (typeof Broadcaster._eventListeners[id] === 'object') {
            var listener = Broadcaster._eventListeners[id];
            if (listener.event === eventName) {
                listener.callback(payload);
            }
        }
    });
}

Broadcaster._isSubscribed = false;

Broadcaster._ensureSubscribed = function() {
    if (Broadcaster._isSubscribed) return;
    Broadcaster._isSubscribed = true;

    exec(function(status) {
        // TODO: define a high-level vocabulary that works with both SDK:s
        console.log('connectionError: ' + status);
        Broadcaster._emitEvent('connectionError', status);
    }, function(e) {
        console.log('BambuserBroadcaster: failed to subscribe to onConnectionError', e);
    }, 'CordovaBambuserBroadcaster', 'onConnectionError', []);

    exec(function(status) {
        console.log('connectionStatusChange: ' + status);
        Broadcaster._emitEvent('connectionStatusChange', status);
    }, function(e) {
        console.log('BambuserBroadcaster: failed to subscribe to onConnectionStatusChange', e);
    }, 'CordovaBambuserBroadcaster', 'onConnectionStatusChange', []);
};

window.addEventListener("orientationchange", function() {
    exec(function(status) {
        console.log('orientationChange', screen.orientation.type);
        Broadcaster._emitEvent('orientationChange', screen.orientation.type);
    }, function(e) {
    }, 'CordovaBambuserBroadcaster', 'onOrientationChange', []);
});

module.exports = Broadcaster;
