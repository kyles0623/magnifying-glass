package com.davidparry.magnifying;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
	public final static  String TAG ="CameraPreview";
	private  List<Size> mSupportedPreviewSizes = null;
    private Size mPreviewSize;
	
	private void stopPreviewAndFreeCamera() {

	    if (mCamera != null) {
	        /*
	          Call stopPreview() to stop updating the preview surface.
	        */
	        mCamera.stopPreview();
	    
	        /*
	          Important: Call release() to release the camera for use by other applications. 
	          Applications should release the camera immediately in onPause() (and re-open() it in
	          onResume()).
	        */
	        mCamera.release();
	    
	        mCamera = null;
	    }
	}
	
	public void setCamera(Camera camera) {
	    if (mCamera == camera) { return; }
	    stopPreviewAndFreeCamera();
	    mCamera = camera;
	    
	    if (mCamera != null) {
	        List<Size> localSizes = mCamera.getParameters().getSupportedPreviewSizes();
	        mSupportedPreviewSizes = localSizes;
	        if(mSupportedPreviewSizes != null && mSupportedPreviewSizes.size() >0){
	        	mPreviewSize = mSupportedPreviewSizes.get(0);
	        }
	        requestLayout();
	        try {
	            mCamera.setPreviewDisplay(mHolder);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	      
	        /*
	          Important: Call startPreview() to start updating the preview surface. Preview must 
	          be started before you can take a picture.
	          */
	        mCamera.startPreview();
	    }
	}
	
    public CameraPreview(Context context, Camera camera) {
        super(context);
        setCamera(camera);
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
          // preview surface does not exist
          return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        
        
        
        // start preview with new settings
        try {
        
        	Camera.Parameters parameters = mCamera.getParameters();
     	    parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
     	    requestLayout();
     	    mCamera.setParameters(parameters);
        	mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            
        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    
    
}
