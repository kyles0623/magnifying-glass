package com.davidparry.magnifying;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

public class Main extends Activity implements OnZoomChangeListener,
		AutoFocusCallback {
	public final static String tag = "Main";
	private Preview mPreview;
	Camera mCamera;
	int numberOfCameras;
	int cameraCurrentlyLocked;
	int max_zoom;
	public static int zoomed = 0;
	boolean zoomSupported = false;
	String flashMode;
	List<String> flashModes;
	public boolean smoothZoomSupported = false;
	// The first rear facing camera
	int defaultCameraId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Hide the window title.
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		// getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// Create a RelativeLayout container that will hold a SurfaceView,
		// and set it as the content of our activity.
		mPreview = new Preview(this);
		setContentView(mPreview);

		// Find the total number of cameras available
		numberOfCameras = Camera.getNumberOfCameras();

		// Find the ID of the default camera
		CameraInfo cameraInfo = new CameraInfo();
		for (int i = 0; i < numberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
				defaultCameraId = i;
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Open the default i.e. the first rear facing camera.
		mCamera = Camera.open();
		initCameraValues(mCamera.getParameters());
		cameraCurrentlyLocked = defaultCameraId;
		//mCamera.setZoomChangeListener(this);
		mPreview.setCamera(mCamera);
	}

	public void initCameraValues(Camera.Parameters parameters) {
		max_zoom = parameters.getMaxZoom();
		zoomSupported = parameters.isZoomSupported();
		flashModes = parameters.getSupportedFlashModes();
		smoothZoomSupported = parameters.isSmoothZoomSupported();
		
	
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Because the Camera object is a shared resource, it's very
		// important to release it when the activity is paused.
		if (mCamera != null) {
			mPreview.setCamera(null);
			mCamera.release();
			mCamera = null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate our menu which can gather user input for switching camera
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.actions, menu);
		boolean flag = true;
		for(String mode : flashModes) {
	      if(mode.equalsIgnoreCase(Camera.Parameters.FLASH_MODE_TORCH)){
	    	  flag = false;
	      }
	    }
		if(flag){
			MenuItem mi = menu.findItem(R.id.action_flash);
			mi.setVisible(false);
		}
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.action_flash: 
			if (mCamera != null){
		        if(item.isChecked()){
		        	Camera.Parameters parameters = mCamera.getParameters();
					parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			        mCamera.setParameters(parameters);
		        	item.setChecked(false);
		        	item.setIcon(R.drawable.bulb);
			    } else {
		        	Camera.Parameters parameters = mCamera.getParameters();
					parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			        mCamera.setParameters(parameters);
		        	item.setChecked(true);
		        	item.setIcon(R.drawable.bulbon);
		        }
			}
			break;
			case R.id.action_freeze:
				if (mCamera != null) {
						try {
							if(item.isChecked()){
								item.setChecked(false);
								item.setIcon(android.R.drawable.ic_media_pause);
								item.setTitle(R.string.action_bar_freeze);
								if (mCamera != null) {
					                mCamera.stopPreview();
					                mPreview.setCamera(null);
					                mCamera.release();
					                mCamera = null;
					            }
					            // Acquire the next camera and request Preview to reconfigure
					            // parameters.
					            mCamera = Camera.open(cameraCurrentlyLocked);
					            mPreview.switchCamera(mCamera);
					            // Start the preview
					            mCamera.startPreview();
					            Camera.Parameters parameters = mCamera.getParameters();
								parameters.setZoom(zoomed);
								mCamera.setParameters(parameters);
								mCamera.autoFocus(this);
							} else {
								item.setChecked(true);
								mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
								item.setIcon(android.R.drawable.ic_media_play);
								item.setTitle(R.string.action_bar_freeze_off);
							}
						} catch (Exception e) {
							Log.e(tag, "Error taking the picture ", e);
						}
				}
				break;
			case R.id.action_focus:
				if (mCamera != null) {
					mCamera.autoFocus(this);
				}
				break;
			case R.id.action_in:
				if (mCamera != null) {
					Camera.Parameters parameters = mCamera.getParameters();
					zoomed = zoomed + 3;
					int nzoom = zoomed;
					if (nzoom <= max_zoom) {
						parameters.setZoom(nzoom);
						mCamera.setParameters(parameters);
					}
				}
				break;
			case R.id.action_out:
				if (mCamera != null) {
					Camera.Parameters parameters = mCamera.getParameters();
					zoomed = zoomed - 3;
					if (zoomed >= 3) {
						if(zoomed >30){
							zoomed = 30;
						}
						parameters.setZoom(zoomed);
						mCamera.setParameters(parameters);
					} else {
						zoomed = 0;
						parameters.setZoom(zoomed);
						mCamera.setParameters(parameters);
					}
				}
				
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}
	
	ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
			Log.d(tag, "onShutter'd");
		}
	};

	/** Handles data for raw picture */
	PictureCallback rawCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			Log.d(tag, "onPictureTaken - raw");
		}
	};

	/** Handles data for jpeg picture */
	PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			Log.d(tag, "onPictureTaken - jpeg");
		}
	};
	

	@Override
	public void onZoomChange(int zoomValue, boolean stopped, Camera camera) {
		zoomed = zoomValue;
		Log.d(tag, "Stopped Zoom " + stopped);

		if (stopped) {
			camera.autoFocus(this);
		}

		if (zoomValue == max_zoom) {
			Log.d(tag, "Maxed out");
			// camera.autoFocus(this);
		} else if (zoomValue == 0) {
			// camera.autoFocus(this);
			Log.d(tag, "Zoomed out");
		}
	}

	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		if(!success){
			Log.d(tag, "Did it work " + success);
		}

	}

}

// ----------------------------------------------------------------------

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered
 * preview of the Camera to the surface. We need to center the SurfaceView
 * because not all devices have cameras that support preview sizes at the same
 * aspect ratio as the device's display.
 */
class Preview extends ViewGroup implements SurfaceHolder.Callback,AutoFocusCallback {
	private final String TAG = "Preview";

	SurfaceView mSurfaceView;
	SurfaceHolder mHolder;
	Size mPreviewSize;
	List<Size> mSupportedPreviewSizes;
	Camera mCamera;

	Preview(Context context) {
		super(context);
		mSurfaceView = new SurfaceView(context);
		addView(mSurfaceView);
		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = mSurfaceView.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void setCamera(Camera camera) {
		mCamera = camera;
		if (mCamera != null) {
			mSupportedPreviewSizes = mCamera.getParameters()
					.getSupportedPreviewSizes();
			requestLayout();
		}
	}

	public void switchCamera(Camera camera) {
		setCamera(camera);
		try {
			camera.setPreviewDisplay(mHolder);
		} catch (IOException exception) {
			Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
		}
		Camera.Parameters parameters = camera.getParameters();
		parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
		requestLayout();

		camera.setParameters(parameters);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// We purposely disregard child measurements because act as a
		// wrapper to a SurfaceView that centers the camera preview instead
		// of stretching it.
		final int width = resolveSize(getSuggestedMinimumWidth(),
				widthMeasureSpec);
		final int height = resolveSize(getSuggestedMinimumHeight(),
				heightMeasureSpec);
		setMeasuredDimension(width, height);

		if (mSupportedPreviewSizes != null) {
			mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width,
					height);
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (changed && getChildCount() > 0) {
			final View child = getChildAt(0);

			final int width = r - l;
			final int height = b - t;

			int previewWidth = width;
			int previewHeight = height;
			if (mPreviewSize != null) {
				previewWidth = mPreviewSize.width;
				previewHeight = mPreviewSize.height;
			}

			// Center the child SurfaceView within the parent.
			if (width * previewHeight > height * previewWidth) {
				final int scaledChildWidth = previewWidth * height
						/ previewHeight;
				child.layout((width - scaledChildWidth) / 2, 0,
						(width + scaledChildWidth) / 2, height);
			} else {
				final int scaledChildHeight = previewHeight * width
						/ previewWidth;
				child.layout(0, (height - scaledChildHeight) / 2, width,
						(height + scaledChildHeight) / 2);
			}
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		try {
			if (mCamera != null) {
				mCamera.setPreviewDisplay(holder);
			}
		} catch (IOException exception) {
			Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the preview.
		if (mCamera != null) {
			mCamera.stopPreview();
		}
	}

	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double) w / h;
		if (sizes == null)
			return null;

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		// Try to find an size match aspect ratio and size
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		// Cannot find the one match the aspect ratio, ignore the requirement
		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
			// set it to the first view dp
			optimalSize = sizes.get(0);
		}
		return optimalSize;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// Now that the size is known, set up the camera parameters and begin
		// the preview.
		if (mCamera != null) {
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
			requestLayout();
			mCamera.setParameters(parameters);
			mCamera.startPreview();
			
			try {
				Main.zoomed = parameters.getMaxZoom();
				parameters.setZoom(Main.zoomed);
				mCamera.setParameters(parameters);
				mCamera.autoFocus(this);
			} catch (Exception e) {
				Log.e("Previe", "Error starting zoom", e);
			}
			
		}
	}
	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		Log.d("Preview", "Did it work " + success);

	}
}
