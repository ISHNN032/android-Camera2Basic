/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.camera2basic.dualcamprev;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DisplayDetectManager;
import android.app.IDisplayDetectService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Camera2BasicFragment extends Fragment
        implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "Camera2BasicFragment";

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;


    private String mCameraId_Back;
    private AutoFitTextureView mTextureView_Back;
    private CameraCaptureSession mCaptureSession_Back;
    private CameraDevice mCameraDevice_Back;
    private ImageReader mImageReader_Back;
    private final Semaphore mCameraOpenCloseLock_Back = new Semaphore(1);
    private Size mPreviewSize_Back = new Size(0, 0);

    private TextView mTextCameraMode_Back;
    private TextView mTextResolution_Back;
    private boolean mCameraMode_isHDMI;
    private boolean mCameraBack_Toggle = false;


    private String mCameraId_Front;
    private AutoFitTextureView mTextureView_Front;
    private CameraCaptureSession mCaptureSession_Front;
    private CameraDevice mCameraDevice_Front;
    private ImageReader mImageReader_Front;
    private final Semaphore mCameraOpenCloseLock_Front = new Semaphore(1);
    private Size mPreviewSize_Front = new Size(0, 0);

    private boolean mCameraFront_Toggle = false;


    private String mCameraId_Extern;
    private AutoFitTextureView mTextureView_Extern;
    private CameraCaptureSession mCaptureSession_Extern;
    private CameraDevice mCameraDevice_Extern;
    private ImageReader mImageReader_Extern;
    private final Semaphore mCameraOpenCloseLock_Extern = new Semaphore(1);
    private Size mPreviewSize_Extern = new Size(0, 0);

    private boolean mCameraExtern_Toggle = true;


    CameraManager manager;
    DisplayDetectManager displayManager;

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback_Back = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock_Back.release();
            mCameraDevice_Back = cameraDevice;
            createCameraPreviewSession(0);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock_Back.release();
            cameraDevice.close();
            mCameraDevice_Back = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock_Back.release();
            cameraDevice.close();
            mCameraDevice_Back = null;
            Log.e("Camera_Callback_Error", cameraDevice.toString() + " Error :" + error);
        }

    };

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback_Front = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock_Front.release();
            mCameraDevice_Front = cameraDevice;
            createCameraPreviewSession(1);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock_Front.release();
            cameraDevice.close();
            mCameraDevice_Front = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock_Front.release();
            cameraDevice.close();
            mCameraDevice_Front = null;
            Log.e("Camera_Callback_Error", cameraDevice.toString() + " Error");
        }

    };

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback_Extern = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock_Extern.release();
            mCameraDevice_Extern = cameraDevice;
            createCameraPreviewSession(2);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock_Extern.release();
            cameraDevice.close();
            mCameraDevice_Extern = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock_Extern.release();
            cameraDevice.close();
            mCameraDevice_Extern = null;
            Log.e("Camera_Callback_Error", cameraDevice.toString() + " Error");
        }

    };


    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static Camera2BasicFragment newInstance() {
        return new Camera2BasicFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera2_basic, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mTextureView_Back = (AutoFitTextureView) view.findViewById(R.id.texture_back);
        mTextureView_Front = (AutoFitTextureView) view.findViewById(R.id.texture_front);
        mTextureView_Extern = (AutoFitTextureView) view.findViewById(R.id.texture_extern);

        mTextCameraMode_Back = (TextView) view.findViewById(R.id.text_camera_back);
        mTextResolution_Back = (TextView) view.findViewById(R.id.text_resolution_back);
        Button mBtnBack = (Button) view.findViewById(R.id.btn_cam_back);
        Button mBtnFront = (Button) view.findViewById(R.id.btn_cam_front);
        Button mBtnExtern = (Button) view.findViewById(R.id.btn_cam_extern);

        mBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCameraBack_Toggle) {
                    Log.e("Clicked", "close");
                    closeCamera(0);
                } else {
                    Log.e("Clicked", "open");
                    openCamera(0, mPreviewSize_Back.getWidth(), mPreviewSize_Back.getHeight());
                }
            }
        });

        mBtnFront.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCameraFront_Toggle) {
                    Log.e("Clicked", "close");
                    closeCamera(1);
                } else {
                    Log.e("Clicked", "open");
                    openCamera(1, mPreviewSize_Front.getWidth(), mPreviewSize_Front.getHeight());
                }
            }
        });

        mBtnExtern.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCameraExtern_Toggle) {
                    Log.e("Clicked", "close");
                    closeCamera(2);
                } else {
                    Log.e("Clicked", "open");
                    openCamera(2, mPreviewSize_Extern.getWidth(), mPreviewSize_Extern.getHeight());
                }
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        assert activity != null;
        manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        displayManager = (DisplayDetectManager) activity.getSystemService("display_detect");
        if(displayManager != null){
            //displayManager.setDisplayValue(2);
            Log.d(TAG, "display value : " + displayManager.getDisplayValue());
            Log.d(TAG, "display status : " + displayManager.getDisplayState());
        }
        checkCameraMode(false);

        Handler handler = new Handler();

        if(!mCameraBack_Toggle){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mTextureView_Back.isAvailable()) {
                        Log.e("DEBUG", "openCamera 0");
                        openCamera(0, mTextureView_Back.getWidth(), mTextureView_Back.getHeight());
                    } else {
                        mTextureView_Back.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                            @Override
                            public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
                                Log.e("DEBUG", "onSurfaceTextureAvailable 0");
                                openCamera(0, width, height);
                            }

                            @Override
                            public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
                                Log.e("DEBUG", "onSurfaceTextureSizeChanged 0");
                                configureTransform(0, width, height);
                            }

                            @Override
                            public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
                                Log.e("DEBUG", "onSurfaceTextureDestroyed 0");
                                return true;
                            }

                            @Override
                            public void onSurfaceTextureUpdated(SurfaceTexture texture) {
                            }
                        });
                    }
                }
            }, 0);
        }

        if(!mCameraFront_Toggle){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mTextureView_Front.isAvailable()) {
                        Log.e("DEBUG", "openCamera 1");
//                        openCamera(1, mTextureView_Front.getWidth(), mTextureView_Front.getHeight());
                    } else {
                        mTextureView_Front.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                            @Override
                            public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
                                Log.e("DEBUG", "onSurfaceTextureAvailable 1");
//                                openCamera(1, width, height);
                            }

                            @Override
                            public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
                                Log.e("DEBUG", "onSurfaceTextureSizeChanged 1");
                                configureTransform(1, width, height);
                            }

                            @Override
                            public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
                                Log.e("DEBUG", "onSurfaceTextureDestroyed 1");
                                return true;
                            }

                            @Override
                            public void onSurfaceTextureUpdated(SurfaceTexture texture) {
                            }
                        });
                    }
                }
            }, 0);
        }
        if(!mCameraExtern_Toggle){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mTextureView_Extern.isAvailable()) {
                        Log.e("DEBUG", "openCamera 2");
//                        openCamera(1, mTextureView_Front.getWidth(), mTextureView_Front.getHeight());
                    } else {
                        mTextureView_Extern.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                            @Override
                            public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
                                Log.e("DEBUG", "onSurfaceTextureAvailable 2");
//                                openCamera(1, width, height);
                            }

                            @Override
                            public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
                                Log.e("DEBUG", "onSurfaceTextureSizeChanged 2");
                                configureTransform(2, width, height);
                            }

                            @Override
                            public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
                                Log.e("DEBUG", "onSurfaceTextureDestroyed 2");
                                return true;
                            }

                            @Override
                            public void onSurfaceTextureUpdated(SurfaceTexture texture) {
                            }
                        });
                    }
                }
            }, 0);
        }
    }

    public void resumeCamera(){}

    public void refreshCamera(){
        Handler handler = new Handler();
        if(mCameraBack_Toggle){
            closeCamera(0);
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                openCamera(0, mPreviewSize_Back.getWidth(), mPreviewSize_Back.getHeight());
            }
        }, 1000);
    }

    public void checkCameraMode(boolean withSwitching){
        Handler handler = new Handler();

        if(displayManager != null){
            if(withSwitching) {
                displayManager.setDisplayValue(2);
            }
            handler.postDelayed(() -> {
                boolean mode = ( displayManager.getDisplayValue() == 0) ;
                mCameraMode_isHDMI = mode;
                String modeText = mode ? "HDMI":"DSUB";
                mTextCameraMode_Back.setText(modeText);
                android.view.ViewGroup.LayoutParams layoutParams = mTextureView_Back.getLayoutParams();
                if(mode){
                    layoutParams.width = 1280;
                    layoutParams.height = 720;
                }else {
                    layoutParams.width = 800;
                    layoutParams.height = 600;
                }
                mTextureView_Back.setLayoutParams(layoutParams);
            }, 1000);
        }

        if(mCameraBack_Toggle){
            closeCamera(0);
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                openCamera(0, mPreviewSize_Back.getWidth(), mPreviewSize_Back.getHeight());
            }
        }, 1000);
    }

    @Override
    public void onPause() {
        closeCamera(0);
        closeCamera(1);
        super.onPause();
    }

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.request_permission))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @SuppressLint("SetTextI18n")
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int camera_num, int width, int height) {
        Log.e("setUpCameraOutputs", "setUpCameraOutputs" +camera_num);

        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        switch (camera_num) {
            case 0: {
                try {
                    for (String cameraId : manager.getCameraIdList()) {
                        CameraCharacteristics characteristics
                                = manager.getCameraCharacteristics(cameraId);

                        StreamConfigurationMap map = characteristics.get(
                                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                        if (map == null) {
                            continue;
                        }

                        List<Size> coSizes = Arrays.asList(map.getOutputSizes(ImageFormat.JPEG));

                        Size resolution;

                        if(mCameraMode_isHDMI){
                            if(coSizes.stream().anyMatch(size -> size.getWidth() == 1280)){
                                resolution = coSizes.stream().filter(size -> size.getWidth() == 1280).findFirst().get();
                            }else{
                                resolution = Collections.max( coSizes, new CompareSizesByArea());
                            }
                        }
                        else{
                            if(coSizes.stream().anyMatch(size -> size.getWidth() == 800)){
                                resolution = coSizes.stream().filter(size -> size.getWidth() == 800).findFirst().get();
                            }else{
                                resolution = Collections.max( coSizes, new CompareSizesByArea());
                            }
                        }

                        mImageReader_Back = ImageReader.newInstance(resolution.getWidth(), resolution.getHeight(),
                                ImageFormat.JPEG, /*maxImages*/2);

                        Point displaySize = new Point();
                        activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                        int maxPreviewWidth = displaySize.x;
                        int maxPreviewHeight = displaySize.y;

                        if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                            maxPreviewWidth = MAX_PREVIEW_WIDTH;
                        }
                        if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                            maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                        }

                        Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                        if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                            mCameraId_Back = cameraId;
                            mPreviewSize_Back = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                                    width, height, maxPreviewWidth,
                                    maxPreviewHeight, resolution);
                            Log.e("Camera OutputSizes Back", coSizes.toString());
                            Log.e("Camera resolution Back", resolution.toString());
                            mTextResolution_Back.setText(resolution.toString());
                        }
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    ErrorDialog.newInstance(getString(R.string.camera_error))
                            .show(getChildFragmentManager(), FRAGMENT_DIALOG);
                }
                break;
            }
            case 1: {
                try {
                    for (String cameraId : manager.getCameraIdList()) {
                        CameraCharacteristics characteristics
                                = manager.getCameraCharacteristics(cameraId);

                        StreamConfigurationMap map = characteristics.get(
                                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                        if (map == null) {
                            continue;
                        }

                        List<Size> coSizes = Arrays.asList(map.getOutputSizes(ImageFormat.JPEG));
                        Log.e("Camera OutputSizes Front", coSizes.toString());

                        Size resolution;

                        if(coSizes.stream().anyMatch(size -> size.equals(new Size(1280, 720)))){
                            resolution = coSizes.stream().filter(size -> size.equals(new Size(1280, 720))).findFirst().get();
                        }else{
                            resolution = Collections.max( coSizes, new CompareSizesByArea());
                        }
                        mImageReader_Front = ImageReader.newInstance(resolution.getWidth(), resolution.getHeight(),
                                ImageFormat.JPEG, /*maxImages*/2);

                        Log.e("Camera resolution Front", resolution.toString());


                        Point displaySize = new Point();
                        activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                        int maxPreviewWidth = displaySize.x;
                        int maxPreviewHeight = displaySize.y;
                        if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                            maxPreviewWidth = MAX_PREVIEW_WIDTH;
                        }
                        if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                            maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                        }

                        Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                            mCameraId_Front = cameraId;
                            mPreviewSize_Front = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                                    width, height, maxPreviewWidth,
                                    maxPreviewHeight, resolution);
                        }
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    ErrorDialog.newInstance(getString(R.string.camera_error))
                            .show(getChildFragmentManager(), FRAGMENT_DIALOG);
                }
                break;
            }
            case 2: {
                try {
                    for (String cameraId : manager.getCameraIdList()) {
                        CameraCharacteristics characteristics
                                = manager.getCameraCharacteristics(cameraId);

                        //region Camera Preview Setting
                        StreamConfigurationMap map = characteristics.get(
                                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                        if (map == null) {
                            continue;
                        }

                        List<Size> coSizes = Arrays.asList(map.getOutputSizes(ImageFormat.JPEG));
                        Log.e("Camera OutputSizes Extern", coSizes.toString());

                        Size resolution;

                        if(coSizes.stream().anyMatch(size -> size.equals(new Size(1280, 720)))){
                            resolution = coSizes.stream().filter(size -> size.equals(new Size(1280, 720))).findFirst().get();
                        }else{
                            resolution = Collections.max( coSizes, new CompareSizesByArea());
                        }
                        mImageReader_Extern = ImageReader.newInstance(resolution.getWidth(), resolution.getHeight(),
                                ImageFormat.JPEG, /*maxImages*/2);

                        Log.e("Camera resolution Extern", resolution.toString());


                        Point displaySize = new Point();
                        activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                        int maxPreviewWidth = displaySize.x;
                        int maxPreviewHeight = displaySize.y;
                        if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                            maxPreviewWidth = MAX_PREVIEW_WIDTH;
                        }
                        if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                            maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                        }
                        //endregion

                        Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                        if (facing != null && facing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                            mCameraId_Extern = cameraId;
                            mPreviewSize_Extern = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                                    width, height, maxPreviewWidth,
                                    maxPreviewHeight, resolution);
                        }
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    ErrorDialog.newInstance(getString(R.string.camera_error))
                            .show(getChildFragmentManager(), FRAGMENT_DIALOG);
                }
                break;
            }
        }
    }

    private void openCamera(int camera_num, int width, int height) {
        Log.e("openCamera", "openCamera" +camera_num);
        switch (camera_num) {
            case 0: {
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestCameraPermission();
                    return;
                }
                setUpCameraOutputs(0, width, height);
                configureTransform(0, width, height);
                try {
                    if (!mCameraOpenCloseLock_Back.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                        showToast("Camera0 Open Timeout");
                    }
                    mCameraBack_Toggle = true;
                    manager.openCamera(mCameraId_Back, mStateCallback_Back, null);
                    mCameraOpenCloseLock_Back.release();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
                }
                break;
            }
            case 1: {
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestCameraPermission();
                    return;
                }
                setUpCameraOutputs(1, width, height);
                configureTransform(1, width, height);
                try {
                    if (!mCameraOpenCloseLock_Front.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                        showToast("Camera1 Open Timeout");
                    }
                    mCameraFront_Toggle = true;
                    if(mCameraId_Front != null){
                        manager.openCamera(mCameraId_Front, mStateCallback_Front, null);
                    }
                    mCameraOpenCloseLock_Front.release();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
                }
                break;
            }
            case 2: {
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestCameraPermission();
                    return;
                }
                setUpCameraOutputs(2, width, height);
                configureTransform(2, width, height);
                try {
                    if (!mCameraOpenCloseLock_Extern.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                        showToast("Camera2 Open Timeout");
                    }
                    mCameraExtern_Toggle = true;
                    if(mCameraId_Extern != null){
                        manager.openCamera(mCameraId_Extern, mStateCallback_Extern, null);
                    }
                    mCameraOpenCloseLock_Extern.release();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
                }
                break;
            }
        }
    }

    private void closeCamera(int num) {
        try {
            switch (num) {
                case 0: {
                    mCameraOpenCloseLock_Back.acquire();
                    mCameraBack_Toggle = false;
                    if (null != mCaptureSession_Back) {
                        mCaptureSession_Back.close();
                        mCaptureSession_Back = null;
                    }
                    if (null != mCameraDevice_Back) {
                        mCameraDevice_Back.close();
                        mCameraDevice_Back = null;
                    }
                    if (null != mImageReader_Back) {
                        mImageReader_Back.close();
                        mImageReader_Back = null;
                    }
                    mCameraOpenCloseLock_Back.release();
                    break;
                }
                case 1: {
                    mCameraOpenCloseLock_Front.acquire();
                    mCameraFront_Toggle = false;
                    if (null != mCaptureSession_Front) {
                        mCaptureSession_Front.close();
                        mCaptureSession_Front = null;
                    }
                    if (null != mCameraDevice_Front) {
                        mCameraDevice_Front.close();
                        mCameraDevice_Front = null;
                    }
                    if (null != mImageReader_Front) {
                        mImageReader_Front.close();
                        mImageReader_Front = null;
                    }
                    mCameraOpenCloseLock_Front.release();
                    break;
                }
                case 2: {
                    mCameraOpenCloseLock_Extern.acquire();
                    mCameraExtern_Toggle = false;
                    if (null != mCaptureSession_Extern) {
                        mCaptureSession_Extern.close();
                        mCaptureSession_Extern = null;
                    }
                    if (null != mCameraDevice_Extern) {
                        mCameraDevice_Extern.close();
                        mCameraDevice_Extern = null;
                    }
                    if (null != mImageReader_Extern) {
                        mImageReader_Extern.close();
                        mImageReader_Extern = null;
                    }
                    mCameraOpenCloseLock_Extern.release();
                    break;
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession(int camera_num) {
        switch (camera_num) {
            case 0: {
                try {
                    SurfaceTexture texture = mTextureView_Back.getSurfaceTexture();
                    assert texture != null;

                    // We configure the size of default buffer to be the size of camera preview we want.
                    texture.setDefaultBufferSize(mPreviewSize_Back.getWidth(), mPreviewSize_Back.getHeight());

                    // This is the output Surface we need to start preview.
                    Surface surface = new Surface(texture);

                    // We set up a CaptureRequest.Builder with the output Surface.
                    final CaptureRequest.Builder previewRequestBuilder
                            = mCameraDevice_Back.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    previewRequestBuilder.addTarget(surface);

                    // Here, we create a CameraCaptureSession for camera preview.
                    mCameraDevice_Back.createCaptureSession(Arrays.asList(surface, mImageReader_Back.getSurface()),
                            new CameraCaptureSession.StateCallback() {
                                @Override
                                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                    // The camera is already closed
                                    if (null == mCameraDevice_Back) {
                                        return;
                                    }

                                    // When the session is ready, we start displaying the preview.
                                    mCaptureSession_Back = cameraCaptureSession;
                                    try {
                                        // Auto focus should be continuous for camera preview.
                                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                                CaptureRequest.CONTROL_AF_MODE_AUTO);

                                        // Finally, we start displaying the camera preview.
                                        CaptureRequest previewRequest = previewRequestBuilder.build();
                                        mCaptureSession_Back.setRepeatingRequest(previewRequest,
                                                null, null);
                                    } catch (CameraAccessException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onConfigureFailed(
                                        @NonNull CameraCaptureSession cameraCaptureSession) {
                                    showToast("Failed");
                                }
                            }, null
                    );
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
            case 1: {
                try {
                    SurfaceTexture texture = mTextureView_Front.getSurfaceTexture();
                    assert texture != null;

                    // We configure the size of default buffer to be the size of camera preview we want.
                    texture.setDefaultBufferSize(mPreviewSize_Front.getWidth(), mPreviewSize_Front.getHeight());

                    // This is the output Surface we need to start preview.
                    Surface surface = new Surface(texture);

                    // We set up a CaptureRequest.Builder with the output Surface.
                    final CaptureRequest.Builder previewRequestBuilder
                            = mCameraDevice_Front.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    previewRequestBuilder.addTarget(surface);

                    // Here, we create a CameraCaptureSession for camera preview.
                    mCameraDevice_Front.createCaptureSession(Arrays.asList(surface, mImageReader_Front.getSurface()),
                            new CameraCaptureSession.StateCallback() {
                                @Override
                                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                    // The camera is already closed
                                    if (null == mCameraDevice_Front) {
                                        return;
                                    }

                                    // When the session is ready, we start displaying the preview.
                                    mCaptureSession_Front = cameraCaptureSession;
                                    try {
                                        // Auto focus should be continuous for camera preview.
                                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                                CaptureRequest.CONTROL_AF_MODE_AUTO);

                                        // Finally, we start displaying the camera preview.
                                        CaptureRequest previewRequest = previewRequestBuilder.build();
                                        mCaptureSession_Front.setRepeatingRequest(previewRequest,
                                                null, null);
                                    } catch (CameraAccessException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onConfigureFailed(
                                        @NonNull CameraCaptureSession cameraCaptureSession) {
                                    showToast("Failed");
                                }
                            }, null
                    );
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
            case 2: {
                try {
                    SurfaceTexture texture = mTextureView_Extern.getSurfaceTexture();
                    assert texture != null;

                    // We configure the size of default buffer to be the size of camera preview we want.
                    texture.setDefaultBufferSize(mPreviewSize_Extern.getWidth(), mPreviewSize_Extern.getHeight());

                    // This is the output Surface we need to start preview.
                    Surface surface = new Surface(texture);

                    // We set up a CaptureRequest.Builder with the output Surface.
                    final CaptureRequest.Builder previewRequestBuilder
                            = mCameraDevice_Extern.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    previewRequestBuilder.addTarget(surface);

                    // Here, we create a CameraCaptureSession for camera preview.
                    mCameraDevice_Extern.createCaptureSession(Arrays.asList(surface, mImageReader_Extern.getSurface()),
                            new CameraCaptureSession.StateCallback() {
                                @Override
                                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                    // The camera is already closed
                                    if (null == mCameraDevice_Extern) {
                                        return;
                                    }

                                    // When the session is ready, we start displaying the preview.
                                    mCaptureSession_Extern = cameraCaptureSession;
                                    try {
                                        // Auto focus should be continuous for camera preview.
                                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                                CaptureRequest.CONTROL_AF_MODE_AUTO);

                                        // Finally, we start displaying the camera preview.
                                        CaptureRequest previewRequest = previewRequestBuilder.build();
                                        mCaptureSession_Extern.setRepeatingRequest(previewRequest,
                                                null, null);
                                    } catch (CameraAccessException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onConfigureFailed(
                                        @NonNull CameraCaptureSession cameraCaptureSession) {
                                    showToast("Failed");
                                }
                            }, null
                    );
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int camera_num, int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        switch (camera_num) {
            case 0: {
                RectF bufferRect = new RectF(0, 0, mPreviewSize_Back.getHeight(), mPreviewSize_Back.getWidth());
                if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                    bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                    matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                    float scale = Math.max(
                            (float) viewHeight / mPreviewSize_Back.getHeight(),
                            (float) viewWidth / mPreviewSize_Back.getWidth());
                    matrix.postScale(scale, scale, centerX, centerY);
                    matrix.postRotate(90 * (rotation - 2), centerX, centerY);
                } else if (Surface.ROTATION_180 == rotation) {
                    matrix.postRotate(180, centerX, centerY);
                }
                if (null == mTextureView_Back || null == mPreviewSize_Back) {
                    return;
                }
                mTextureView_Back.setTransform(matrix);
                break;
            }
            case 1: {
                RectF bufferRect = new RectF(0, 0, mPreviewSize_Extern.getHeight(), mPreviewSize_Front.getWidth());
                if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                    bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                    matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                    float scale = Math.max(
                            (float) viewHeight / mPreviewSize_Front.getHeight(),
                            (float) viewWidth / mPreviewSize_Front.getWidth());
                    matrix.postScale(scale, scale, centerX, centerY);
                    matrix.postRotate(90 * (rotation - 2), centerX, centerY);
                } else if (Surface.ROTATION_180 == rotation) {
                    matrix.postRotate(180, centerX, centerY);
                }
                if (null == mTextureView_Front || null == mPreviewSize_Front) {
                    return;
                }
                mTextureView_Front.setTransform(matrix);
                break;
            }
            case 2: {
                RectF bufferRect = new RectF(0, 0, mPreviewSize_Extern.getHeight(), mPreviewSize_Extern.getWidth());
                if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                    bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                    matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                    float scale = Math.max(
                            (float) viewHeight / mPreviewSize_Extern.getHeight(),
                            (float) viewWidth / mPreviewSize_Extern.getWidth());
                    matrix.postScale(scale, scale, centerX, centerY);
                    matrix.postRotate(90 * (rotation - 2), centerX, centerY);
                } else if (Surface.ROTATION_180 == rotation) {
                    matrix.postRotate(180, centerX, centerY);
                }
                if (null == mTextureView_Extern || null == mPreviewSize_Extern) {
                    return;
                }
                mTextureView_Extern.setTransform(matrix);
                break;
            }
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }

}
