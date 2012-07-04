package com.davidparry.magnifying;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

public class CopyOfMain extends Activity {
	public final static  String tag ="Main";
	private Camera mCamera;
	private CameraPreview mPreview;
	
	
	public CopyOfMain() {
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.actions, menu);
	    return true;
	}
	
		 public void onSort(MenuItem item) {
	        int mSortMode = item.getItemId();
	        // Request a call to onPrepareOptionsMenu so we can change the sort icon
	        invalidateOptionsMenu();
	    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		

	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(tag, "pause");
		try{
			releaseCameraAndPreview();
		} catch(Exception er) {
			Log.d(tag, "Error closing camera", er);
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.d(tag, "restart");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(tag, "resume");
		if(checkCameraHardware(this)) {
			Log.d(tag, "has camera");
			 // Create an instance of Camera
	        if(safeCameraOpen(0)) {
		        // Create our Preview view and set it as the content of our activity.
		        mPreview = new CameraPreview(this, mCamera);
		        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		        preview.addView(mPreview);
	        }
		} else {
			Log.d(tag,"no camera");
		}
	}
	
	private boolean checkCameraHardware(Context context) {
	    if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
	        return true;
	    } else {
	        return false;
	    }
	}
	
	public static Camera getCameraInstance(int id){
	    Camera c = null;
	    try {
	        c = Camera.open(); 
	    }
	    catch (Exception e){
	    	Log.e(tag, "no camera returning some other application has it",e);
	    }
	    return c; 
	}
	
	private boolean safeCameraOpen(int id) {
	    boolean qOpened = false;
	  
	    try {
	        releaseCameraAndPreview();
	        mCamera = getCameraInstance(id);
	        qOpened = (mCamera != null);
	    } catch (Exception e) {
	        Log.e(getString(R.string.app_name), "failed to open Camera");
	        e.printStackTrace();
	    }

	    return qOpened;    
	}

	private void releaseCameraAndPreview() {
	    if(mPreview != null){
			mPreview.setCamera(null);
		    if (mCamera != null) {
		        mCamera.release();
		        mCamera = null;
		    }
	    }
	}
	
	
	
	
}
