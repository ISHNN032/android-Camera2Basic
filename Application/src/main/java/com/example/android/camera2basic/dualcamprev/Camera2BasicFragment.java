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
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
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
    private Semaphore mCameraOpenCloseLock_Back = new Semaphore(1);
    private Size mPreviewSize_Back;

    private String mCameraId_Front;
    private AutoFitTextureView mTextureView_Front;
    private CameraCaptureSession mCaptureSession_Front;
    private CameraDevice mCameraDevice_Front;
    private ImageReader mImageReader_Front;
    private Semaphore mCameraOpenCloseLock_Front = new Semaphore(1);
    private Size mPreviewSize_Front;

    CameraManager manager;

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
            Log.e("Camera_Callback_Error", cameraDevice.toString() + " Error");
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
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        Activity activity = getActivity();
        assert activity != null;
        manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        Handler handler = new Handler();

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
                            Log.e("DEBUG", "onSurfaceTextureUpdated 0");
                        }
                    });
                }
            }
        },0);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mTextureView_Front.isAvailable()) {
                    Log.e("DEBUG", "openCamera 1");
                    openCamera(1, mTextureView_Front.getWidth(), mTextureView_Front.getHeight());
                } else {
                    mTextureView_Front.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                        @Override
                        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
                            Log.e("DEBUG", "onSurfaceTextureAvailable 1");
                            openCamera(1, width, height);
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
                            Log.e("DEBUG", "onSurfaceTextureUpdated 1");
                        }
                    });
                }
            }
        }, 0);
    }

    @Override
    public void onPause() {
        closeCamera();
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

    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int camera_num, int width, int height) {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        switch (camera_num){
            case 0:{
                try {
                    for (String cameraId : manager.getCameraIdList()) {
                        CameraCharacteristics characteristics
                                = manager.getCameraCharacteristics(cameraId);

                        StreamConfigurationMap map = characteristics.get(
                                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                        if (map == null) {
                            continue;
                        }

                        // For still image captures, we use the largest available size.
                        Size largest = Collections.max(
                                Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                                new CompareSizesByArea());
                        mImageReader_Back = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
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

                        // We don't use a front facing camera in this sample.
                        Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                        if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                            mCameraId_Back = cameraId;
                            mPreviewSize_Back = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                                    width, height, maxPreviewWidth,
                                    maxPreviewHeight, largest);
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
            case 1:{
                try {
                    for (String cameraId : manager.getCameraIdList()) {
                        CameraCharacteristics characteristics
                                = manager.getCameraCharacteristics(cameraId);

                        StreamConfigurationMap map = characteristics.get(
                                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                        if (map == null) {
                            continue;
                        }

                        // For still image captures, we use the largest available size.
                        Size largest = Collections.max(
                                Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                                new CompareSizesByArea());
                        mImageReader_Front = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
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

                        // We don't use a front facing camera in this sample.
                        Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                            mCameraId_Front = cameraId;
                            mPreviewSize_Front = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                                    width, height, maxPreviewWidth,
                                    maxPreviewHeight, largest);
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
        switch (camera_num){
            case 0:{
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestCameraPermission();
                    return;
                }
                setUpCameraOutputs(0, width, height);
                configureTransform(0, width, height);
                try {
                    if (!mCameraOpenCloseLock_Back.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                        throw new RuntimeException("Time out waiting to lock camera opening.");
                    }
                    manager.openCamera(mCameraId_Back, mStateCallback_Back, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
                }
                break;
            }
            case 1:{
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestCameraPermission();
                    return;
                }
                setUpCameraOutputs(1, width, height);
                configureTransform(1, width, height);
                try {
                    if (!mCameraOpenCloseLock_Front.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                        throw new RuntimeException("Time out waiting to lock camera opening.");
                    }
                    manager.openCamera(mCameraId_Front, mStateCallback_Front, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
                }
                break;
            }
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock_Back.acquire();
            if (null != mCaptureSession_Back) {
                mCaptureSession_Back.close();
                mCaptureSession_Back = null;
            }
            mCameraOpenCloseLock_Front.acquire();
            if (null != mCaptureSession_Front) {
                mCaptureSession_Front.close();
                mCaptureSession_Front = null;
            }

            if (null != mCameraDevice_Back) {
                mCameraDevice_Back.close();
                mCameraDevice_Back = null;
            }
            if (null != mCameraDevice_Front) {
                mCameraDevice_Front.close();
                mCameraDevice_Front = null;
            }


            if (null != mImageReader_Back) {
                mImageReader_Back.close();
                mImageReader_Back = null;
            }
            if (null != mImageReader_Front) {
                mImageReader_Front.close();
                mImageReader_Front = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock_Back.release();
            mCameraOpenCloseLock_Front.release();
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
                                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

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
                                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

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

        switch (camera_num){
            case 0:{
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
            case 1:{
                RectF bufferRect = new RectF(0, 0, mPreviewSize_Front.getHeight(), mPreviewSize_Front.getWidth());
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
