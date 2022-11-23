#import "CordovaBambuserBroadcaster.h"

@implementation CordovaBambuserBroadcaster {
    UIColor *originalBackgroundColor;
    NSString *onConnectionErrorCallbackId;
    NSString *onConnectionStatusChangeCallbackId;
    NSString *onBroadcastIdAvailableCallbackId;
}

- (id) init {
    if (self = [super init]) {
        originalBackgroundColor = self.webView.backgroundColor;
        onConnectionErrorCallbackId = nil;
        onConnectionStatusChangeCallbackId = nil;
    }
    return self;
}

- (void) ensureLibbambuserIsBootstrapped {
    if (bambuserView == nil) {
        bambuserView = [[BambuserView alloc] initWithPreset: kSessionPresetAuto];
        bambuserView.delegate = self;
    }
}

- (void) setApplicationId: (CDVInvokedUrlCommand*) command {
    CDVPluginResult* result = nil;
    NSString* applicationId = command.arguments[0];
    if (applicationId != nil && [applicationId length] > 0) {
        [self ensureLibbambuserIsBootstrapped];
        bambuserView.applicationId = applicationId;
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void) showViewfinderBehindWebView: (CDVInvokedUrlCommand*) command {
    [self.webView setOpaque: NO];
    self.webView.backgroundColor = UIColor.clearColor;
    self.webView.scrollView.backgroundColor = UIColor.clearColor;
    [self ensureLibbambuserIsBootstrapped];
    [bambuserView setOrientation: [UIApplication sharedApplication].statusBarOrientation];
    [self.webView.superview insertSubview: bambuserView.view belowSubview:self.webView];
    [bambuserView setPreviewFrame: CGRectMake(0, 0, self.webView.bounds.size.width, self.webView.bounds.size.height)];
    [self.viewController.view sendSubviewToBack:bambuserView.view];
    [self.commandDelegate sendPluginResult: [CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId: command.callbackId];
}

- (void) hideViewfinder: (CDVInvokedUrlCommand*) command {
    [self.webView setOpaque: YES];
    self.webView.backgroundColor = originalBackgroundColor;
    if (bambuserView != nil) {
        [bambuserView.view removeFromSuperview];
    }
    [self.commandDelegate sendPluginResult: [CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId: command.callbackId];
}

- (void) setCustomData: (CDVInvokedUrlCommand*) command {
    CDVPluginResult* result = nil;
    NSString* customData = command.arguments[0];
    if (customData != nil) {
        [self ensureLibbambuserIsBootstrapped];
        bambuserView.customData = customData;
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void) setSendPosition: (CDVInvokedUrlCommand*) command {
    [self ensureLibbambuserIsBootstrapped];
    bambuserView.sendPosition = (BOOL)[command.arguments[0] boolValue];
    [self.commandDelegate sendPluginResult: [CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId: command.callbackId];
}

- (void) setSaveOnServer: (CDVInvokedUrlCommand*) command {
    [self ensureLibbambuserIsBootstrapped];
    bambuserView.SaveOnServer = (BOOL)[command.arguments[0] boolValue];
    [self.commandDelegate sendPluginResult: [CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId: command.callbackId];
}

- (void) setTitle: (CDVInvokedUrlCommand*) command {
    CDVPluginResult* result = nil;
    NSString* title = command.arguments[0];
    if (title != nil) {
        [self ensureLibbambuserIsBootstrapped];
        bambuserView.broadcastTitle = title;
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void) setAuthor: (CDVInvokedUrlCommand*) command {
    CDVPluginResult* result = nil;
    NSString* author = command.arguments[0];
    if (author != nil) {
        [self ensureLibbambuserIsBootstrapped];
        bambuserView.author = author;
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void) startBroadcast: (CDVInvokedUrlCommand*) command {
    [self ensureLibbambuserIsBootstrapped];
    [bambuserView startBroadcasting];
    [self.commandDelegate sendPluginResult: [CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId: command.callbackId];
    if (onConnectionStatusChangeCallbackId != nil) {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:(CDVCommandStatus)CDVCommandStatus_OK messageAsString:@"starting"];
        [result setKeepCallbackAsBool:true];
        [self.commandDelegate sendPluginResult: result callbackId: onConnectionStatusChangeCallbackId];
    }
}

- (void) toggleTorchLight: (CDVInvokedUrlCommand*) command {
    bambuserView.torch = !bambuserView.torch;
    [self.commandDelegate sendPluginResult: [CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId: command.callbackId];
}

- (void) stopBroadcast: (CDVInvokedUrlCommand*) command {
    [self ensureLibbambuserIsBootstrapped];
    [bambuserView stopBroadcasting];
    [self.commandDelegate sendPluginResult: [CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId: command.callbackId];
    if (onConnectionStatusChangeCallbackId != nil) {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:(CDVCommandStatus)CDVCommandStatus_OK messageAsString:@"finishing"];
        [result setKeepCallbackAsBool:true];
        [self.commandDelegate sendPluginResult: result callbackId: onConnectionStatusChangeCallbackId];
    }
}

- (void) switchCamera: (CDVInvokedUrlCommand*) command {
    [self ensureLibbambuserIsBootstrapped];
    [bambuserView swapCamera];
    [self.commandDelegate sendPluginResult: [CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId: command.callbackId];
}

- (void) onConnectionError: (CDVInvokedUrlCommand*) command {
    onConnectionErrorCallbackId = command.callbackId;
}

- (void) onConnectionStatusChange: (CDVInvokedUrlCommand*) command {
    onConnectionStatusChangeCallbackId = command.callbackId;
}

- (void) onBroadcastIdAvailable: (CDVInvokedUrlCommand*) command {
    onBroadcastIdAvailableCallbackId = command.callbackId;
}

- (void) onOrientationChange: (CDVInvokedUrlCommand*) command {
    // Web view orientation changed - update viewfinder
    [bambuserView setOrientation: [UIApplication sharedApplication].statusBarOrientation];
    [bambuserView setPreviewFrame: CGRectMake(0, 0, self.webView.bounds.size.width, self.webView.bounds.size.height)];
}

- (void) bambuserError: (enum BambuserError)errorCode message:(NSString*)errorMessage {
    NSLog(@"bambuserError %d %@", errorCode, errorMessage);
    if (onConnectionErrorCallbackId != nil) {
        // TODO: define a high-level vocabulary that works with both SDK:s
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:(CDVCommandStatus)CDVCommandStatus_OK messageAsString:@"undefined-error"];
        [result setKeepCallbackAsBool:true];
        [self.commandDelegate sendPluginResult: result callbackId: onConnectionErrorCallbackId];
    }
}

- (void) broadcastStarted {
    NSLog(@"Received broadcastStarted signal");
    if (onConnectionStatusChangeCallbackId != nil) {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:(CDVCommandStatus)CDVCommandStatus_OK messageAsString:@"capturing"];
        [result setKeepCallbackAsBool:true];
        [self.commandDelegate sendPluginResult: result callbackId: onConnectionStatusChangeCallbackId];
    }
}

- (void) broadcastStopped {
    NSLog(@"Received broadcastStopped signal");
    if (onConnectionStatusChangeCallbackId != nil) {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:(CDVCommandStatus)CDVCommandStatus_OK messageAsString:@"idle"];
        [result setKeepCallbackAsBool:true];
        [self.commandDelegate sendPluginResult: result callbackId: onConnectionStatusChangeCallbackId];
    }
}

- (void) broadcastIdReceived: (NSString *) broadcastId {
  NSLog(@"Received broadcastId: %@", broadcastId);
  if (onBroadcastIdAvailableCallbackId != nil) {
      CDVPluginResult *result = [CDVPluginResult resultWithStatus:(CDVCommandStatus)CDVCommandStatus_OK messageAsString:broadcastId];
      [result setKeepCallbackAsBool:true];
      [self.commandDelegate sendPluginResult: result callbackId: onBroadcastIdAvailableCallbackId];
  }
}

@end
