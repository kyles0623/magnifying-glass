package com.davidparry.magnifying;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
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

public class Main extends Activity implements AutoFocusCallback {
	public final static String tag = "Main";
	private Preview mPreview;
	Camera mCamera;
	int numberOfCameras;
	int cameraCurrentlyLocked;
	public static int zoomed = 0;
	int defaultCameraId;
	static public boolean focusing = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPreview = new Preview(this);
		setContentView(mPreview);
		numberOfCameras = Camera.getNumberOfCameras();
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
		try {
			mCamera = Camera.open();
			cameraCurrentlyLocked = defaultCameraId;
			if(mPreview == null){
				mPreview = new Preview(this);
			}
			mPreview.setCamera(mCamera);
		} catch(Exception er) {
			Log.e(tag, "Error on onResume ",er);
		}
	}


	@Override
	protected void onPause() {
		super.onPause();
		if (mCamera != null) {
			mPreview.setCamera(null);
			mCamera.release();
			mCamera = null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.actions, menu);
		if(mCamera != null){
			List<String> flashModes = mCamera.getParameters().getSupportedFlashModes();
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
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
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
				if(!focusing){
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
						            mCamera = Camera.open(cameraCurrentlyLocked);
						            mPreview.switchCamera(mCamera);
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
				}
				break;
			case R.id.action_focus:
				if(!focusing){
					if (mCamera != null) {
						focusing = true;
						mCamera.autoFocus(this);
					}
				}
				break;
			case R.id.action_in:
				if(!focusing){
					if (mCamera != null) {
						Camera.Parameters parameters = mCamera.getParameters();
						zoomed = zoomed + 3;
						int nzoom = zoomed;
						if (nzoom <= mCamera.getParameters().getMaxZoom()) {
							parameters.setZoom(nzoom);
							mCamera.setParameters(parameters);
						}
					}
				}
				break;
			case R.id.action_out:
				if(!focusing){
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
	public void onAutoFocus(boolean success, Camera camera) {
		if(!success){
			Log.d(tag, "Did it work " + success);
		}
		focusing = false;
	}

}


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
		try {
			if (mCamera != null) {
				mCamera.setPreviewDisplay(holder);
			}
		} catch (IOException exception) {
			Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
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

		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}
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
		try{
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
				if(!Main.focusing){
					Main.focusing = true;
					mCamera.autoFocus(this);
				}
			} catch (Exception e) {
				Log.e("Preview", "Error starting zoom", e);
			}
			
		}
		} catch(Exception er) {
			Log.e("Preview", "Error in surfaceChanged ", er);
		}
	}
	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		Log.d("Preview", "Did it work " + success);
		Main.focusing = false;
	}
}
