#import <Cordova/CDVPlugin.h>
#import "libbambuser.h"

@interface CordovaBambuserBroadcaster : CDVPlugin <BambuserViewDelegate> {
    BambuserView *bambuserView;
}

- (void) setApplicationId: (CDVInvokedUrlCommand*) command;
- (void) showViewfinderBehindWebView: (CDVInvokedUrlCommand*) command;
- (void) hideViewfinder: (CDVInvokedUrlCommand*) command;
- (void) setCustomData: (CDVInvokedUrlCommand*) command;
- (void) setSendPosition: (CDVInvokedUrlCommand*) command;
- (void) setSaveOnServer:(CDVInvokedUrlCommand *)command;
- (void) setTitle: (CDVInvokedUrlCommand*) command;
- (void) setAuthor: (CDVInvokedUrlCommand*) command;
- (void) startBroadcast: (CDVInvokedUrlCommand*) command;
- (void) stopBroadcast: (CDVInvokedUrlCommand*) command;
- (void) switchCamera: (CDVInvokedUrlCommand*) command;
- (void) onConnectionError: (CDVInvokedUrlCommand*) command;
- (void) onConnectionStatusChange: (CDVInvokedUrlCommand*) command;
- (void) onOrientationChange: (CDVInvokedUrlCommand*) command;
- (void) toggleTorchLight: (CDVInvokedUrlCommand*) command;

// BambuserViewDelegate
- (void) bambuserError: (enum BambuserError)errorCode message:(NSString*)errorMessage;
- (void) broadcastStarted;
- (void) broadcastStopped;

@end
