package com.bambuser.cordova;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.Manifest.permission;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.bambuser.broadcaster.BroadcastStatus;
import com.bambuser.broadcaster.Broadcaster;
import com.bambuser.broadcaster.CameraError;
import com.bambuser.broadcaster.ConnectionError;
import com.bambuser.broadcaster.SurfaceViewWithAutoAR;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.json.JSONException;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import android.view.OrientationEventListener;
import org.json.JSONArray;

public class CordovaBambuserBroadcaster extends CordovaPlugin implements Broadcaster.Observer {
    /**
     * CordovaPlugin methods
     */

    @Override
    public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
        log("initialization");
        super.initialize(cordova, webView);
        self = this;
    }

    @Override
    public boolean execute(final String action, final CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        log("Executing Cordova plugin action: " + action);

        if ("showViewfinderBehindWebView".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (previewSurfaceView == null) {
                        callbackContext.error("Viewfinder view not initialized. Set applicationId first.");
                        return;
                    }
                    webView.getView().setBackgroundColor(android.R.color.transparent);
                    ViewGroup parentView = (ViewGroup) webView.getView().getParent();
                    RelativeLayout.LayoutParams previewLayoutParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                    parentView.addView(previewSurfaceView, 0, previewLayoutParams);

                    callbackContext.success("Viewfinder view added");
                }
            });
            return true;
        }

        if ("hideViewfinder".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (previewSurfaceView == null) {
                        callbackContext.error("Viewfinder view not initialized. Set applicationId first.");
                        return;
                    }

                    ViewGroup parentView = (ViewGroup) webView.getView().getParent();
                    parentView.removeView(previewSurfaceView);
                    callbackContext.success("Viewfinder view removed");
                }
            });
            return true;
        }

        if ("setApplicationId".equals(action)) {
            final String applicationId = args.getString(0);
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initializeBroadcaster(applicationId);
                    callbackContext.success("Application id set");
                }
            });
            return true;
        }

        if ("setCustomData".equals(action)) {
            final String customData = args.getString(0);
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcaster == null) {
                        callbackContext.error("Broadcaster is not initialized. Set applicationId first.");
                        return;
                    };
                    mBroadcaster.setCustomData(customData);
                    callbackContext.success("Custom data updated");
                }
            });
            return true;
        }

        if ("setSendPosition".equals(action)) {
            final boolean value = args.getBoolean(0);
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcaster == null) {
                        callbackContext.error("Broadcaster is not initialized. Set applicationId first.");
                        return;
                    };
                    mBroadcaster.setSendPosition(value);
                    callbackContext.success(value ? "Positioning enabled" : "Positioning disabled");
                }
            });
            return true;
        }

        if ("setSaveOnServer".equals(action)) {
            final boolean value = args.getBoolean(0);
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcaster == null) {
                        callbackContext.error("Broadcaster is not initialized. Set applicationId first.");
                        return;
                    }
                    ;
                    mBroadcaster.setSaveOnServer(value);
                    callbackContext.success(value ? "SaveOnServer enabled" : "SaveOnServer disabled");
                }
            });
            return true;
        }

        if ("setTitle".equals(action)) {
            final String title = args.getString(0);
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcaster == null) {
                        callbackContext.error("Broadcaster is not initialized. Set applicationId first.");
                        return;
                    };
                    mBroadcaster.setTitle(title);
                    callbackContext.success("Broadcast title updated");
                }
            });
            return true;
        }

        if ("setAuthor".equals(action)) {
            final String author = args.getString(0);
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcaster == null) {
                        callbackContext.error("Broadcaster is not initialized. Set applicationId first.");
                        return;
                    };
                    mBroadcaster.setAuthor(author);
                    callbackContext.success("Broadcast author updated");
                }
            });
            return true;
        }

        if ("startBroadcast".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcaster == null) {
                        callbackContext.error("applicationId must be set first");
                        return;
                    };
                    if (!mBroadcaster.canStartBroadcasting()) {
                        log("Cannot start broadcasting, ignoring start request");
                        callbackContext.error("Cannot start broadcasting, ignoring start request");
                        return;
                    }
                    if (hasPermission(permission.CAMERA) && hasPermission(permission.RECORD_AUDIO)) {
                        lockCurrentOrientation();
                        // initLocalRecording();
                        log("Starting broadcast");
                        mBroadcaster.startBroadcast();
                        callbackContext.success("Broadcast starting");
                    } else {
                        log("Insufficient permissions to broadcast, requesting permissions");
                        List<String> permissions = new ArrayList<String>();
                        if (!hasPermission(permission.CAMERA)) {
                            permissions.add(permission.CAMERA);
                        }
                        if (!hasPermission(permission.RECORD_AUDIO)) {
                            permissions.add(permission.RECORD_AUDIO);
                        }
                        if (!hasPermission(permission.WRITE_EXTERNAL_STORAGE)) {
                            permissions.add(permission.WRITE_EXTERNAL_STORAGE);
                        }
                        requestPermissions(permissions, BROADCAST_PERMISSIONS_CODE);
                        callbackContext.error("Requesting permissions");
                    }
                }
            });
            return true;
        }


        if ("getCameraId".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcaster == null) {
                        callbackContext.error("Broadcaster is not initialized. Set applicationId first.");
                        return;
                    };
                    String id = mBroadcaster.getCameraId();
                    log("getCameraId id " + id );
                    callbackContext.success(id);
                }
            });
            return true;
        }

        if ("stopBroadcast".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcaster == null) {
                        callbackContext.error("Broadcaster is not initialized. Set applicationId first.");
                        return;
                    };
                    mBroadcaster.stopBroadcast();
                    callbackContext.success("Broadcast stopped");
                }
            });
            return true;
        }

        if ("switchCamera".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcaster == null) {
                        callbackContext.error("Broadcaster is not initialized. Set applicationId first.");
                        return;
                    };
                    mBroadcaster.switchCamera();
                    callbackContext.success("Camera switch requested");
                }
            });
            return true;
        }

        if ("getSupportedCameras".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcaster == null) {
                        callbackContext.error("Broadcaster is not initialized. Set applicationId first.");
                        return;
                    };
                    List<String> result = new ArrayList<String>();
                    for (Broadcaster.Camera element : mBroadcaster.getSupportedCameras()) {
                        result.add("{ \"id\": " + element.id + ",  \"facing\":\""+element.facing+"\"}");
                    }
                    String finalResult = result.stream()
                    .map(n -> String.valueOf(n))
                    .collect(Collectors.joining(",", "[", "]"));
                    callbackContext.success(finalResult);
                }
            });
            return true;
        }

        if ("getSupportedResolutions".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcaster == null) {
                        callbackContext.error("Broadcaster is not initialized. Set applicationId first.");
                        return;
                    };

                    List<String> result = new ArrayList<String>();
                    for (Object element : mBroadcaster.getSupportedResolutions()) {
                        result.add(element.toString());
                    }

                    String finalResult = result.stream()
                    .map(n -> String.valueOf(n))
                    .collect(Collectors.joining(",", "", ""));
 
                    callbackContext.success(finalResult);
                }
            });
            return true;
        }

        if ("canSwitchCameraWithoutResolutionChangeById".equals(action)) {
           String cameraId = args.getString(0);
           this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcaster == null) {
                        callbackContext.error("Broadcaster is not initialized. Set applicationId first.");
                        return;
                    }; 
                    int intResult = mBroadcaster.canSwitchCameraWithoutResolutionChange(cameraId) ? 1 : 0;
                    callbackContext.success(intResult);
                }
            });
            return true;
        }

        if ("setCameraId".equals(action)) {
           String cameraId = args.getString(0);
           this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcaster == null) {
                        callbackContext.error("Broadcaster is not initialized. Set applicationId first.");
                        return;
                    }; 
                    mBroadcaster.setCameraId(cameraId);
                    callbackContext.success("SET OK cameraId " + cameraId );
                }
            });
            return true;
        }

        if ("setResolution".equals(action)) {
           int maxWidth = args.getInt(0);
           int maxHeight = args.getInt(1);
           this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcaster == null) {
                        callbackContext.error("Broadcaster is not initialized. Set applicationId first.");
                        return;
                    }; 
                    mBroadcaster.setResolution(maxWidth,maxHeight);
                    callbackContext.success("SET RESOLUTION");
                }
            });
            return true;
        }

        if ("canSwitchCameraWithoutResolutionChange".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcaster == null) {
                        callbackContext.error("Broadcaster is not initialized. Set applicationId first.");
                        return;
                    }; 
                    int intResult = mBroadcaster.canSwitchCameraWithoutResolutionChange() ? 1 : 0;
                    callbackContext.success(intResult);
                }
            });
            return true;
        }

        if ("toggleTorchLight".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcaster == null) {
                        callbackContext.error("Broadcaster is not initialized. Set applicationId first.");
                        return;
                    };
                    mBroadcaster.toggleTorch();
                    callbackContext.success("torch light toggled");
                }
            });
            return true;
        }

        if ("onConnectionError".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    self.onConnectionErrorCallbackContext = callbackContext;
                }
            });
            return true;
        }

        if ("onConnectionStatusChange".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    self.onConnectionStatusChangeCallbackContext = callbackContext;
                }
            });
            return true;
        }

        if ("onBroadcastIdAvailable".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    self.onBroadcastIdAvailableCallbackContext = callbackContext;
                }
            });
            return true;
        }

        if ("onOrientationChange".equals(action)) {
            // Web view orientation changed - handled by OrientationEventListener - do nothing
            return true;
        }

        return false;
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     */
    @Override
    public void onPause(boolean multitasking) {
        // mOrientationListener.disable();
        if (mBroadcaster == null) return;
        mOrientationListener.disable();
        mBroadcaster.onActivityPause();
    }

    /**
     * Called when the activity will start interacting with the user.
     */
    @Override
    public void onResume(boolean multitasking) {
        // mOrientationListener.enable();
        if (mBroadcaster == null) return;
        mOrientationListener.enable();
        mBroadcaster.setRotation(mDefaultDisplay.getRotation());
        mBroadcaster.onActivityResume();
    }

    /**
     * Called when the activity is becoming visible to the user.
     */
    @Override
    public void onStart() {
    }

    /**
     * Called when the activity is no longer visible to the user.
     */
    @Override
    public void onStop() {
    }

    /**
     * The final call you receive before your activity is destroyed.
     */
    @Override
    public void onDestroy() {
        if (mBroadcaster == null) return;
        mBroadcaster.onActivityDestroy();
    }

    /**
     * Called when the WebView does a top-level navigation or refreshes.
     *
     * Plugins should stop any long-running processes and clean up internal state.
     *
     * Does nothing by default.
     */
    @Override
    public void onReset() {
    }



    /**
     * Broadcaster.Observer protocol
     */

    @Override
    public void onConnectionStatusChange(final BroadcastStatus status) {
        if (status == BroadcastStatus.STARTING) {
            log("BroadcastStatus.STARTING");
            this.cordova.getActivity().getWindow().addFlags(FLAG_KEEP_SCREEN_ON);
        }
        if (status == BroadcastStatus.IDLE) {
            log("BroadcastStatus.IDLE");
            this.cordova.getActivity().getWindow().clearFlags(FLAG_KEEP_SCREEN_ON);
            this.cordova.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
        // if (mBroadcaster != null) {
            // mBroadcastButton.setText(status == BroadcastStatus.IDLE ? "Broadcast" : "Disconnect");
            // mSwitchButton.setEnabled(status == BroadcastStatus.IDLE);
        // }

        if (this.onConnectionStatusChangeCallbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, status.name().toLowerCase());
            result.setKeepCallback(true);
            this.onConnectionStatusChangeCallbackContext.sendPluginResult(result);
        }
    }

    @Override
    public void onStreamHealthUpdate(final int health) {
    }

    @Override
    public void onConnectionError(final ConnectionError type, final String message) {
        if (this.onConnectionErrorCallbackContext != null) {
            // TODO: define a high-level vocabulary that works with both SDK:s
            PluginResult result = new PluginResult(PluginResult.Status.OK, "undefined-error");
            result.setKeepCallback(true);
            this.onConnectionErrorCallbackContext.sendPluginResult(result);
        }
        String str = type.toString();
        if (message != null) {
            str += " " + message;
        }
        displayToast(str);
    }

    @Override
    public void onCameraError(final CameraError error) {
        displayToast(error.toString());
    }

    @Override
    public void onChatMessage(final String message) {
    }

    @Override
    public void onResolutionsScanned() {
    }

    @Override
    public void onCameraPreviewStateChanged() {
    }

    @Override
    public void onBroadcastInfoAvailable(String videoId, String url) {
        log("Broadcast with id " + videoId + " published");
    }

    @Override
    public void onBroadcastIdAvailable(String broadcastId) {
        log("Broadcast with broadcastId " + broadcastId + " published");
        if (this.onBroadcastIdAvailableCallbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, broadcastId);
            result.setKeepCallback(true);
            this.onBroadcastIdAvailableCallbackContext.sendPluginResult(result);
        }
    }

    /**
     * Private methods
     */
     private void initializeBroadcaster(String applicationId) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewGroup parentView = (ViewGroup) webView.getView().getParent();
                previewSurfaceView = new SurfaceViewWithAutoAR(parentView.getContext());
                previewSurfaceView.setCropToParent(true);
                final Activity activity = cordova.getActivity();

                mDefaultDisplay = activity.getWindowManager().getDefaultDisplay();
                mBroadcaster = new Broadcaster(activity, applicationId, self);
                mBroadcaster.setRotation(mDefaultDisplay.getRotation());
                mBroadcaster.setCameraSurface(previewSurfaceView);

                mOrientationListener = new OrientationEventListener(parentView.getContext()) {
                    @Override
                    public void onOrientationChanged(int orientation) {
                        if (mBroadcaster != null && mBroadcaster.canStartBroadcasting()) {
                            mBroadcaster.setRotation(mDefaultDisplay.getRotation());
                        }
                    }
                };
                mOrientationListener.enable();

                if (!mInPermissionRequest) {
                    final List<String> missingPermissions = new ArrayList<String>();
                    if (!hasPermission(permission.CAMERA)) {
                        missingPermissions.add(permission.CAMERA);
                    }
                    if (!hasPermission(permission.RECORD_AUDIO)) {
                        missingPermissions.add(permission.RECORD_AUDIO);
                    }
                    if (!hasPermission(permission.WRITE_EXTERNAL_STORAGE)) {
                        missingPermissions.add(permission.WRITE_EXTERNAL_STORAGE);
                    }
                    if (missingPermissions.size() > 0) {
                        requestPermissions(missingPermissions, START_PERMISSIONS_CODE);
                    }
                }
            }
        });
    }

    private boolean hasPermission(String permission) {
        // https://cordova.apache.org/docs/en/latest/guide/platforms/android/plugin.html#android-permissions
        return cordova.hasPermission(permission);
    }

    private void requestPermissions(List<String> missingPermissions, int code) {
        mInPermissionRequest = true;
        String[] permissions = missingPermissions.toArray(new String[missingPermissions.size()]);
        // https://cordova.apache.org/docs/en/latest/guide/platforms/android/plugin.html#android-permissions
        this.cordova.requestPermissions(this, code, permissions);
    }

    private static int getScreenOrientation(int displayRotation, int configOrientation) {
        if (configOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (displayRotation == Surface.ROTATION_0 || displayRotation == Surface.ROTATION_90)
                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            else
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
        } else {
            if (displayRotation == Surface.ROTATION_0 || displayRotation == Surface.ROTATION_270)
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            else
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        }
    }

    private void lockCurrentOrientation() {
        int displayRotation = cordova.getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int configOrientation = cordova.getActivity().getResources().getConfiguration().orientation;
        int screenOrientation = getScreenOrientation(displayRotation, configOrientation);
        cordova.getActivity().setRequestedOrientation(screenOrientation);
    }

    private void displayToast(final String text) {
        Toast.makeText(this.cordova.getActivity().getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }

    private void log(final String text) {
        log(text, false);
    }

    private void log(final String text, boolean toast) {
        Log.v(LOG_PREFIX, text);
        if (toast) displayToast(text);
    }

    private static final String LOG_PREFIX = "CordovaBambuserBroadcaster";
    private static final int START_PERMISSIONS_CODE = 2;
    private static final int BROADCAST_PERMISSIONS_CODE = 3;
    private CordovaBambuserBroadcaster self;
    private SurfaceViewWithAutoAR previewSurfaceView;
    private CallbackContext onConnectionErrorCallbackContext;
    private CallbackContext onConnectionStatusChangeCallbackContext;
    private CallbackContext onBroadcastIdAvailableCallbackContext;
    private boolean mInPermissionRequest = false;
    private Display mDefaultDisplay;
    private OrientationEventListener mOrientationListener;
    private Broadcaster mBroadcaster;
}
