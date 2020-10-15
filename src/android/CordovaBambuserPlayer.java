package com.bambuser.cordova;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import com.bambuser.broadcaster.BroadcastPlayer;
import com.bambuser.broadcaster.PlayerState;
import com.bambuser.broadcaster.SurfaceViewWithAutoAR;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class CordovaBambuserPlayer extends CordovaPlugin implements BroadcastPlayer.Observer {
    /**
     * CordovaPlugin methods
     */
    @Override
    public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
        log("initialization");
        super.initialize(cordova, webView);
        self = this;
        self.initSurfaceView();
    }

    @Override
    public boolean execute(final String action, final CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        log("Executing Cordova plugin action: " + action);

        if ("setApplicationId".equals(action)) {
            final String id = args.getString(0);
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (applicationId != null) {
                        callbackContext.error("applicationId is already set");
                        return;
                    }
                    applicationId = id;
                    callbackContext.success("Application id set");
                }
            });
            return true;
        }

        if ("showPlayerBehindWebView".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (playbackSurfaceView == null) {
                        callbackContext.error("Player view not initialized.");
                        return;
                    }
                    webView.getView().setBackgroundColor(android.R.color.transparent);
                    ViewGroup parentView = (ViewGroup) webView.getView().getParent();
                    RelativeLayout.LayoutParams previewLayoutParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                    parentView.addView(playbackSurfaceView, 0, previewLayoutParams);
                    callbackContext.success("Playback view added");
                }
            });
            return true;
        }

        if ("hidePlayer".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (playbackSurfaceView  == null) {
                        callbackContext.error("Playback view not initialized.");
                        return;
                    }
                    ViewGroup parentView = (ViewGroup) webView.getView().getParent();
                    parentView.removeView(playbackSurfaceView);
                    callbackContext.success("Viewfinder view removed");
                }
            });
            return true;
        }

        if ("setAudioVolume".equals(action)) {
            final double volume = args.getDouble(0);
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (volume < 0.0) {
                        callbackContext.error("Volume cannot be less than 0.0");
                        return;
                    };
                    if (volume > 1.0) {
                        callbackContext.error("Volume cannot be larger than 1.0");
                        return;
                    };
                    // Let volume persist between plays
                    self.audioVolume = (float) volume;
                    if (mBroadcastPlayer != null) {
                        // Update current player
                        mBroadcastPlayer.setAudioVolume((float) volume);
                    };
                    callbackContext.success("Playback volume set");
                }
            });
            return true;
        }

        if ("loadBroadcast".equals(action)) {
            final String resourceUri = args.getString(0);
            final String requiredBroadcastState = args.getString(1);
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (applicationId == null) {
                        callbackContext.error("applicationId is not set");
                        return;
                    }
                    loadContent(applicationId, resourceUri, requiredBroadcastState);
                    callbackContext.success("Broadcast is loading");
                }
            });
            return true;
        }

        if ("pause".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcastPlayer == null) {
                        callbackContext.error("Player is not initialized.");
                        return;
                    };
                    if (!mBroadcastPlayer.canPause()) {
                        // TODO: should we automatically stop here like on iOS?
                        callbackContext.error("Player not pausable");
                        return;
                    }
                    mBroadcastPlayer.pause();
                    callbackContext.success("Player paused");
                }
            });
            return true;
        }

        if ("resume".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcastPlayer == null) {
                        callbackContext.error("Player is not initialized.");
                        return;
                    };
                    if (!mBroadcastPlayer.canPause()) {
                        // TODO: should we automatically stop here like on iOS?
                        callbackContext.error("Player not resumable");
                        return;
                    }
                    if (mBroadcastPlayer.getState() != PlayerState.PAUSED) {
                        // TODO: should we support restarting a finished broadcast
                        // like the underlaying player's start() does?
                        callbackContext.error("Player is not paused");
                        return;
                    }
                    mBroadcastPlayer.start();
                    callbackContext.success("Player resumed");
                }
            });
            return true;
        }

        if ("close".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBroadcastPlayer == null) {
                        callbackContext.error("Player is not initialized.");
                        return;
                    };
                    mBroadcastPlayer.close();
                    callbackContext.success("Player closed");
                }
            });
            return true;
        }

        if ("onBroadcastLoaded".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    self.onBroadcastLoadedCallbackContext = callbackContext;
                }
            });
            return true;
        }

        if ("onStateChange".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    self.onStateChangeCallbackContext = callbackContext;
                }
            });
            return true;
        }

        return false;
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     */
    @Override
    public void onPause(boolean multitasking) {
        if (mBroadcastPlayer != null) {
            mBroadcastPlayer.close();
            mBroadcastPlayer = null;
        }
        playbackSurfaceView = null;
    }

    /**
     * Called when the activity will start interacting with the user.
     */
    @Override
    public void onResume(boolean multitasking) {
        self.initSurfaceView();
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
        if (mBroadcastPlayer != null) {
            mBroadcastPlayer.close();
            mBroadcastPlayer = null;
        }
        playbackSurfaceView = null;
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
     * Broadcaster.BroadcastPlayer protocol
     * https://bambuser.com/docs/reference/android-sdk-0.9.14/com/bambuser/broadcaster/BroadcastPlayer.Observer.html
     */

    @Override
    public void onBroadcastLoaded(final boolean live, final int width, final int height) {
        log("broadcastLoaded:" + (live ? "live" : "archived") + " " + width + "x" + height);
      Point size = getScreenSize();
      float screenAR = size.x / (float) size.y;
      float videoAR = width / (float) height;
      float arDiff = screenAR - videoAR;

      boolean shouldCrop = Math.abs(arDiff) < 0.2;
      playbackSurfaceView.setCropToParent(shouldCrop);
    
      if (!shouldCrop) {
        // calcalute the margin top based on the aspect ratio 
        // might be better to do it with the layout option to vertically centre it.
        int offsetTop = Math.round((size.y / 2) - (size.x /  videoAR / 2));
        ViewGroup parentView = (ViewGroup) webView.getView().getParent();
        RelativeLayout.LayoutParams previewLayoutParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        previewLayoutParams.setMargins(0, offsetTop , 0 , 0);
        parentView.removeView(playbackSurfaceView);
        parentView.addView(playbackSurfaceView, 0, previewLayoutParams);
      }

      mBroadcastPlayer.setSurfaceView(playbackSurfaceView);
        if (this.onBroadcastLoadedCallbackContext != null) {
          JSONObject event = new JSONObject();
            try {
                event.put("isLive", live);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            // Not available via the iOS SDK - should they be added?
            // event.put("width", width);
            // event.put("height", height);
            PluginResult result = new PluginResult(PluginResult.Status.OK, event);
            result.setKeepCallback(true);
            this.onBroadcastLoadedCallbackContext.sendPluginResult(result);
        }
    }

    @Override
    public void onStateChange(final PlayerState state) {
        log("stateChange: " + state);
        if (this.onBroadcastLoadedCallbackContext != null) {
            String res = null;
            if (state == PlayerState.PLAYING) {
                res = "playing";
            /*
            // Not available on iOS (yet)
            // Disabling on Android too for the time being
            } else if (state == PlayerState.CONSTRUCTION) {
                res = "construction";
            } else if (state == PlayerState.LOADING) {
                res = "loading";
            } else if (state == PlayerState.BUFFERING) {
                res = "buffering";
            */
            } else if (state == PlayerState.PAUSED) {
                res = "paused";
            } else if (state == PlayerState.COMPLETED) {
                res = "completed";
            } else if (state == PlayerState.ERROR) {
                res = "error";
            } else if (state == PlayerState.CLOSED) {
                // Using stopped instead of (perma)close,
                // since this implementation is closer to the iOS SDK
                // in terms of reuse: player.js is a singleton.
                res = "stopped";
            }
            if (res != null) {
                PluginResult result = new PluginResult(PluginResult.Status.OK, res);
                result.setKeepCallback(true);
                this.onBroadcastLoadedCallbackContext.sendPluginResult(result);
            }
        }
    }

    /**
     * Private methods
     */
    private void initSurfaceView() {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewGroup parentView = (ViewGroup) webView.getView().getParent();
                playbackSurfaceView = new SurfaceViewWithAutoAR(parentView.getContext());
                playbackSurfaceView.setCropToParent(true);
            }
        });
     }

    private void loadContent(String applicationId, String resourceUri, String requiredBroadcastState) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mBroadcastPlayer != null) {
                    mBroadcastPlayer.close();
                }
                Context context = self.cordova.getActivity().getApplicationContext();
                mBroadcastPlayer = new BroadcastPlayer(context, resourceUri, applicationId, self);
                mBroadcastPlayer.setSurfaceView(playbackSurfaceView);
                if (audioVolume >= 0) {
                    // Let volume persist between plays
                    mBroadcastPlayer.setAudioVolume((float) audioVolume);
                }
                switch (requiredBroadcastState) {
                    case "live":
                        mBroadcastPlayer.setAcceptType(BroadcastPlayer.AcceptType.LIVE);
                        break;
                    case "archived":
                        mBroadcastPlayer.setAcceptType(BroadcastPlayer.AcceptType.ARCHIVED);
                        break;
                    case "any":
                        break;
                    default:
                        // TODO: reject?
                }
                mBroadcastPlayer.load();
            }
        });
    }

    private void displayToast(final String text) {
        Toast.makeText(this.cordova.getActivity().getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }

    private Point getScreenSize() {
        if (mDefaultDisplay == null)
        mDefaultDisplay = cordova.getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        try {
        // this is officially supported since SDK 17 and said to work down to SDK 14 through reflection,
        // so it might be everything we need.
        mDefaultDisplay.getClass().getMethod("getRealSize", Point.class).invoke(mDefaultDisplay, size);
        } catch (Exception e) {
        // fallback to approximate size.
        mDefaultDisplay.getSize(size);
        }
        return size;
    }

    private void log(final String text) {
        log(text, false);
    }

    private void log(final String text, boolean toast) {
        Log.v(LOG_PREFIX, text);
        if (toast) displayToast(text);
    }

    private static final String LOG_PREFIX = "CordovaBambuserPlayer";
    private String applicationId;
    private double audioVolume = -1.0;
    private CordovaBambuserPlayer self;
    private SurfaceViewWithAutoAR playbackSurfaceView;
    private CallbackContext onBroadcastLoadedCallbackContext;
    private CallbackContext onStateChangeCallbackContext;
    private Display mDefaultDisplay;
    private OrientationEventListener mOrientationListener;
    private BroadcastPlayer mBroadcastPlayer;
}
