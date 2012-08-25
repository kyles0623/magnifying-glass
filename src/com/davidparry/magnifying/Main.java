package com.davidparry.magnifying;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class Main extends Activity implements AutoFocusCallback {
	public final static String tag = "Main";
	private Preview mPreview;
	Camera mCamera;
	int numberOfCameras;
	public static int cameraCurrentlyLocked;
	public static int zoomed = 0;
	public static int defaultCameraId;
	static public boolean focusing = false;
	public boolean kidplaypresent = false;
	int frontCameraId;	
	boolean frontCameraPresent = false;
	Menu _menu;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPreview = new Preview(this);
		setContentView(mPreview);
		numberOfCameras = Camera.getNumberOfCameras();
		CameraInfo cameraInfo = new CameraInfo();
		for (int i = 0; i < numberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if(cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
				defaultCameraId = i;
				cameraCurrentlyLocked = defaultCameraId;
			} else if(cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT){
				frontCameraId = i;
				frontCameraPresent = true;
			}
		}
		kidplaypresent = checkForKidPlay();
		
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
		return true;
	}
	
	public void setMenuItems(Menu menu) {
		if(kidplaypresent){
			MenuItem mi = menu.findItem(R.id.action_help_me);
			mi.setVisible(false);
		}
		if(mCamera != null){
			MenuItem mif = menu.findItem(R.id.action_flash);
			MenuItem mifr = menu.findItem(R.id.action_freeze);
			MenuItem mi = menu.findItem(R.id.action_mirror);
			CameraInfo cameraInfo = new CameraInfo();
			Camera.getCameraInfo(Main.cameraCurrentlyLocked, cameraInfo);
			if(cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT){
				mif.setVisible(false);
				mif.setChecked(false);
				mif.setTitle(R.string.flash);
				mifr.setChecked(false);
				mifr.setVisible(false);
				mifr.setTitle(R.string.action_bar_freeze);
				mi.setTitle(R.string.action_bar_magnify);
			} else {
				if(canFlash()){
					mif.setVisible(true);
				} else {
					mif.setVisible(false);
				}
				mifr.setVisible(true);
			}
		}
		if(!frontCameraPresent){
			MenuItem mi = menu.findItem(R.id.action_mirror);
			mi.setVisible(false);
		}
	
	}
	
	

	@Override 
	public boolean onPrepareOptionsMenu(Menu menu) {
		_menu = menu;
		setMenuItems(menu);
		return true;
	};
	
	public boolean canFlash(){
		boolean flag = false;
		if(mCamera != null){
			List<String> flashModes = mCamera.getParameters().getSupportedFlashModes();
			if(flashModes != null && flashModes.size() >0){
				for(String mode : flashModes) {
			      if(mode.equalsIgnoreCase(Camera.Parameters.FLASH_MODE_TORCH)){
			    	  flag = true;
			      }
			    }
			}
		}
		return flag;
	}
	
	public void reloadCamera() {
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
		try {
			mCamera.autoFocus(this);
		} catch (Exception e) {
			Log.e(tag, "Error auto focusing",e);
		}
	}
	
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_flash: 
			if (mCamera != null){
				if(canFlash()){
					CameraInfo cameraInfo = new CameraInfo();
					Camera.getCameraInfo(Main.cameraCurrentlyLocked, cameraInfo);
					if(cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK){
						if(item.isChecked()){
				        	Camera.Parameters parameters = mCamera.getParameters();
				        	if (Build.MANUFACTURER.equalsIgnoreCase("Samsung"))
				            {
				        		reloadCamera();
				            }
				        	parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
					        mCamera.setParameters(parameters);
				        	item.setChecked(false);
				        	item.setTitle(R.string.flash);
				        	item.setIcon(R.drawable.bulb);
					    } else {
				        	Camera.Parameters parameters = mCamera.getParameters();
							parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
					        mCamera.setParameters(parameters);
				        	item.setChecked(true);
				        	item.setIcon(R.drawable.bulbon);
				        	item.setTitle(R.string.flash_off);
				        }
					}
				} else {
					Toast.makeText(this, "No flash option", Toast.LENGTH_SHORT).show();
				}
			}
			break;
			case R.id.action_help_me:
				 new AlertDialog.Builder(Main.this)
	                .setIconAttribute(android.R.attr.alertDialogIcon)
	                .setTitle(R.string.alert_dialog_msg_header)
	                .setMessage(R.string.alert_dialog_msg)
	                .setPositiveButton(R.string.alert_dialog_support, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                    	Intent goToMarket = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=com.davidparry.kidplay"));
	                    	goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
	                    	startActivity(goToMarket);
	                    	Toast.makeText(((AlertDialog)dialog).getContext(), "Thank you for your support!", Toast.LENGTH_LONG).show();
	                    }
	                })
	                .setNegativeButton(R.string.alert_dialog_no, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {

	                    }
	                })
	                .create().show();
				break;
			case R.id.action_freeze:
				if(!focusing){
					if (mCamera != null) {
							try {
								CameraInfo cameraInfo = new CameraInfo();
								Camera.getCameraInfo(Main.cameraCurrentlyLocked, cameraInfo);
								if(cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK){
									if(item.isChecked()){
										item.setChecked(false);
										item.setIcon(android.R.drawable.ic_media_pause);
										item.setTitle(R.string.action_bar_freeze);
										reloadCamera();
									} else {
										item.setChecked(true);
										mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
										item.setIcon(android.R.drawable.ic_media_play);
										item.setTitle(R.string.action_bar_freeze_off);
							        	MenuItem flash = _menu.findItem(R.id.action_flash);
							        	flash.setChecked(false);
							        	flash.setTitle(R.string.flash);
							        	flash.setIcon(R.drawable.bulb);
							        	if (mCamera != null) {
							                mCamera.stopPreview();
							                mPreview.setCamera(null);
							                mCamera.release();
							                mCamera = null;
							            }       	
									}
								}
							} catch (Exception e) {
								Log.e(tag, "Error taking the picture ", e);
							}
					} else {
						if(item.isChecked()){
							item.setChecked(false);
							item.setIcon(android.R.drawable.ic_media_pause);
							item.setTitle(R.string.action_bar_freeze);
							reloadCamera();
						}
					}
				} else {
					focusing = false;
					zoomed = 0;
				}
				break;
			case R.id.action_focus:
				if(!focusing){
					if (mCamera != null) {
						focusing = true;
						mCamera.autoFocus(this);
					} else {
						focusing = false;
					}
				}
				break;
			case R.id.action_in:
				if(!focusing){
					if (mCamera != null) {
						Log.d(tag, "MaxZoom "+mCamera.getParameters().getMaxZoom() + "  zoomed = "+zoomed);
						Camera.Parameters parameters = mCamera.getParameters();
						if(parameters.getMaxZoom() <= 0){
							Toast.makeText(this, "No Zoom for this camera!", Toast.LENGTH_SHORT).show();
						} else {
							zoomed = zoomed + 3;
							if (zoomed <= parameters.getMaxZoom()) {
								parameters.setZoom(zoomed);
								mCamera.setParameters(parameters);
							}else{
								zoomed = parameters.getMaxZoom();
							}
						}
					}
				} else {
					focusing = false;
				}
				break;
			case R.id.action_out:
				if(!focusing){
					if (mCamera != null) {
						Camera.Parameters parameters = mCamera.getParameters();
						if(parameters.getMaxZoom() <= 0){
							Toast.makeText(this, "No Zoom for this camera!", Toast.LENGTH_SHORT).show();
						} else {
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
				} else {
					focusing = false;
				}
				break;
			case R.id.action_mirror:
				if (mCamera != null) {
	                mCamera.stopPreview();
	                mPreview.setCamera(null);
	                mCamera.release();
	                mCamera = null;
	                focusing = false;
				}
			    if(item.isChecked()){
			    	item.setChecked(false);
			    	item.setTitle(R.string.action_bar_mirror);
		        	item.setIcon(R.drawable.mirror);
			     	mCamera = Camera.open(defaultCameraId);
			    	cameraCurrentlyLocked = defaultCameraId;
			    } else {
			    	try{
				    	mCamera = Camera.open(frontCameraId); 
				      	item.setTitle(R.string.action_bar_magnify);
				      	item.setIcon(R.drawable.magnify);
				    	cameraCurrentlyLocked = frontCameraId;
				    	item.setChecked(true);
			    	} catch(Exception e) {
			    		Toast.makeText(this, "Your front camera is locked by another application sorry try again soon", Toast.LENGTH_SHORT).show();
			    	}
			    }
	            mPreview.switchCamera(mCamera);
	            mCamera.startPreview();
	            zoomed = 0;
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
			/**
			try{
				Camera.Parameters parameters = mCamera.getParameters();
				parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
		        mCamera.setParameters(parameters);
			} catch (Exception er){
				Log.e(tag, "Flash is not turning off", er);
			}
			**/
		}
	};
	

	
	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		if(!success){
			Log.d(tag, "Did it work " + success);
		}
		focusing = false;
	}

	 private boolean checkForKidPlay() {
	        boolean flag = false;
		 	Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
	        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
	        List<ResolveInfo> mApps = getPackageManager().queryIntentActivities(mainIntent, 0);
	       for(ResolveInfo info : mApps){
	    	   CharSequence label = info.activityInfo.loadLabel(getPackageManager());
	    	   try{
		    	   if("Kid Play".equalsIgnoreCase((String)label)){
		    		   flag = true;
		    	   }
	    	   } catch(Exception er) {
	    		   try{
	    			   android.text.SpannedString st = (android.text.SpannedString)label;
	    			   String sp = st.toString();
	    			   if("Kid Play".equalsIgnoreCase(sp)){
			    		   flag = true;
			    	   }
	    		   } catch(Exception e){
	    			   
	    		   }
	    	   }
	       }
	       return flag;
	 }

	
}


class Preview extends ViewGroup implements SurfaceHolder.Callback,AutoFocusCallback {
	private final String TAG = "Preview";

	SurfaceView mSurfaceView;
	SurfaceHolder mHolder;
	Size mPreviewSize;
	List<Size> mSupportedPreviewSizes;
	Camera mCamera;
	Size mFrontCameraPreviewSize;
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
		CameraInfo cameraInfo = new CameraInfo();
		Camera.getCameraInfo(Main.cameraCurrentlyLocked, cameraInfo);
		if(cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT){
		//	parameters.setPreviewSize(mFrontCameraPreviewSize.width, mFrontCameraPreviewSize.height);
		//	parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
		} else {
		//	parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
		}
		try{
			camera.setParameters(parameters);
		} catch(Exception er){
			throw new DebugException("SwitchCamera is "+ camera + " parameters "+ parameters.flatten(),er);
		}
		requestLayout();
		
	
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
		}
		mFrontCameraPreviewSize = optimalSize;
		// set it to the first view dp
		optimalSize = sizes.get(0);

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
