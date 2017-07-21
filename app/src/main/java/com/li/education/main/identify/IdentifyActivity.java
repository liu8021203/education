package com.li.education.main.identify;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.camera2.*;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.FaceDetector;
import android.media.Image;
import android.media.ImageReader;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.li.education.R;
import com.li.education.base.AppData;
import com.li.education.base.BaseActivity;
import com.li.education.base.bean.BaseResult;
import com.li.education.base.bean.FaceResult;
import com.li.education.base.bean.QuestionResult;
import com.li.education.base.bean.vo.Face2FaceVO;
import com.li.education.base.bean.vo.FaceActionResult;
import com.li.education.base.http.HttpService;
import com.li.education.base.http.RetrofitUtil;
import com.li.education.main.identify.handler.PicCaptureActivityHandler;
import com.li.education.util.UtilBitmap;
import com.li.education.util.UtilData;
import com.li.education.util.UtilGson;
import com.li.education.util.UtilMD5Encryption;
import com.li.education.util.UtilSPutil;
import com.li.education.view.AutoFitTextureView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import rx.Subscriber;

import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.io;

/**
 * Created by liu on 2017/6/3.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class IdentifyActivity extends BaseActivity {
    private int face_close_eye = 5;
    private int face_open_month = 5;
    private int face_up_head_min = 8;
    private int face_up_head_max = 25;
    private float face_similarity = 0.8f;
    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    private String mCameraId;
    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);


    /**
     * 转换屏幕预览和JPEG的朝向一致
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;
    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;

    private File mFile;

    /**
     * {@link CaptureRequest} generated by {@link #previewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;
    private AutoFitTextureView mTextureView;
    private CaptureRequest.Builder previewRequestBuilder;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;
    private Handler childHandler;
    private ImageReader mImageReader;
    private Surface mSurface;
    private HandlerThread handlerThread;

    private ImageView mIvAction;
    //是否停止检测人脸位置
    private boolean isStop = false;
    private boolean isChecking = false;
    private boolean isCheckAction = true;
    //是否离开人脸框
    private boolean isExit = false;

    private Bitmap mBitmap;

    private FaceDialog mFaceDialog;
    //是否首次启动
    private boolean isFirst = true;
    //当前动作：1张嘴，2闭眼，3抬头
    private int currAction = -1;
    //是否是从home来的
    private boolean isBackground = false;

    private int n = 0;


    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;

    @android.support.annotation.RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mTextureView = (AutoFitTextureView) findViewById(R.id.preview_view);
        mIvAction = (ImageView) findViewById(R.id.iv_action);
        handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        childHandler = new Handler(handlerThread.getLooper());
        init();
    }

    private void init() {
        face_close_eye = UtilSPutil.getInstance(this).getInt("eye", 5);
        face_open_month = UtilSPutil.getInstance(this).getInt("month",5);
        face_up_head_min = UtilSPutil.getInstance(this).getInt("head_min", 8);
        face_up_head_max = UtilSPutil.getInstance(this).getInt("head_max", 25);
        face_similarity = UtilSPutil.getInstance(this).getFloat("face", 0.8f);
    }

    private Handler actionHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    startAction();
                    actionHandler.sendEmptyMessageDelayed(1,2000);
                    break;
                case 1:
                    checkAction();
                    break;
            }
        }
    };

    private Handler faceHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            checkFace();
        }
    };

    private Handler errorHander = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:{
                    showDialog("人脸不能离开人脸框", 0);
                }
                    break;
            }

        }
    };

    private void checkAction(){
        String base64Str = UtilBitmap.bitmapToBase64(mBitmap);
        RetrofitUtil.getFaceInstance().create(HttpService.class).faceangle(base64Str).subscribeOn(io()).observeOn(mainThread()).subscribe(new Subscriber<FaceActionResult>() {

            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                if (!isExit) {
                    showDialog("人脸动作验证失败", 2);
                }
            }

            @Override
            public void onNext(FaceActionResult result) {
                if(!isExit) {
                    if (result.isResult()) {
                        switch (currAction) {
                            case 1: {
                                int x = result.getPoints().get(47).getY() - result.getPoints().get(44).getY();
                                if (x > face_open_month) {
                                    setResult(1);
                                    finish();
                                } else {
                                    if(n < 10) {
                                        n++;
                                        showDialog("人脸动作验证失败", 2);
                                    }else{
                                        showDialog("人脸动作验证失败", 1);
                                    }
                                }
                            }
                            break;
                            case 2: {
                                int x = result.getPoints().get(24).getY() - result.getPoints().get(20).getY();
                                if (x < face_close_eye) {
                                    setResult(1);
                                    finish();
                                } else {
                                    if(n < 10) {
                                        n++;
                                        showDialog("人脸动作验证失败", 2);
                                    }else{
                                        showDialog("人脸动作验证失败", 1);
                                    }
                                }
                            }
                            break;
                            case 3: {
                                if (result.getPitch() >= face_up_head_min && result.getPitch() <= face_up_head_max) {
                                    setResult(1);
                                    finish();
                                } else {
                                    if(n < 10) {
                                        n++;
                                        showDialog("人脸动作验证失败", 2);
                                    }else{
                                        showDialog("人脸动作验证失败", 1);
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
        });
    }

    private void checkFace(){
        if(isExit){
            return;
        }
        File file = UtilBitmap.saveFile(mBitmap, AppData.PATH, "face.jpg");
        Map<String, RequestBody> map = new HashMap<>();
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), file);
        map.put("facefirsturl\"; filename=\"face.jpg", requestBody);
        RequestBody bodyToken = RequestBody.create(MediaType.parse("text/plain"), AppData.token);
        map.put("token", bodyToken);
        RetrofitUtil.getInstance().create(HttpService.class).url2url(map).subscribeOn(io()).observeOn(mainThread()).subscribe(new Subscriber<FaceResult>() {

            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                if(!isExit) {
                    showDialog("人脸识别不通过", 1);
                    isChecking = false;
                }
            }

            @Override
            public void onNext(FaceResult result) {
                if(!isExit) {
                    if (result.isStatus()) {
                        Face2FaceVO faceVO = (Face2FaceVO) UtilGson.fromJson(result.getData().getFace2faceResult(), Face2FaceVO.class);
                        if(faceVO.getSimilar() > face_similarity){
                            isChecking = true;
                            isCheckAction = false;
                        }else{
                            showDialog("人脸识别不通过", 1);
                            isChecking = false;
                        }
                    } else {
                        showDialog("人脸识别不通过", 1);
                        isChecking = false;
                    }
                }
            }
        });
    }



    private void initSurface() {
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        assert texture != null;
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        mSurface = new Surface(texture);
    }

    private TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };


    /**
     * 摄像头创建监听
     */
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {//打开摄像头
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;
            //开启预览
            takePreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {//关闭摄像头
            mCameraOpenCloseLock.release();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {//发生错误
            mCameraOpenCloseLock.release();
            mCameraDevice.close();
            mCameraDevice = null;
            finish();
            Toast.makeText(IdentifyActivity.this, "摄像头开启失败", Toast.LENGTH_SHORT).show();
        }
    };


    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            process(result);
        }

        private void process(CaptureResult result) {
            if(isFirst){
                isFirst = false;
                new FaceThread().start();
            }
            if(!isExit) {
                if(!isChecking && mBitmap != null){
                    isChecking = true;
                    faceHandler.sendEmptyMessageDelayed(0,2000);
                }
                if (!isCheckAction && mBitmap != null) {
                    isCheckAction = true;
                    actionHandler.sendEmptyMessageDelayed(0,1000);
                }
            }
        }
    };


    private void startAction(){
        int random = UtilData.getRandom(1, 3);
        switch (random){
            //张嘴
            case 1: {
                Uri notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.face_open_mouth);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
                mIvAction.setImageResource(R.drawable.face_open_month);
                AnimationDrawable animationDrawable = (AnimationDrawable) mIvAction.getDrawable();
                animationDrawable.start();
                currAction = 1;
            }
                break;
            //闭眼
            case 2:{
                Uri notification = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.face_close_eye);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
                mIvAction.setImageResource(R.drawable.face_close_eye);
                AnimationDrawable animationDrawable = (AnimationDrawable) mIvAction.getDrawable();
                animationDrawable.start();
                currAction = 2;
            }
                break;
            //向上抬头
            case 3:{
                Uri notification = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.face_up_head);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
                mIvAction.setImageResource(R.drawable.face_up_head);
                AnimationDrawable animationDrawable = (AnimationDrawable) mIvAction.getDrawable();
                animationDrawable.start();
                currAction = 3;
            }
                break;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isBackground){
            showDialog("检测失败", 0);
        }
        //设置人脸位置检测是否停止
        isStop = false;
        //设置人脸对比是否在检测中
        isChecking = false;
        //设置人脸动作是否在检测中
        isCheckAction = true;
        startThread();
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mTextureListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //设置人脸检测中
        isChecking = true;
        //设置人脸动作在检测中
        isCheckAction = true;
        //设置人脸位置检测停止
        isStop = true;
        isBackground = true;
        closeCamera();
        stopThread();
    }

    private void takePicture() {
        lockFocus();
    }

    private void lockFocus() {
        try {
            mState = STATE_WAITING_LOCK;
            mCameraCaptureSession.capture(previewRequestBuilder.build(), mCaptureCallback,
                    childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始预览
     */
    private void takePreview() {
        try {
            initSurface();
            // 创建预览需要的CaptureRequest.Builder
            previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 将SurfaceView的surface作为CaptureRequest.Builder的目标
            previewRequestBuilder.addTarget(mSurface);
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() // ③
            {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    if (null == mCameraDevice) return;
                    // 当摄像头已经准备好时，开始显示预览
                    mCameraCaptureSession = cameraCaptureSession;
                    try {
                        // 显示预览
                        mPreviewRequest = previewRequestBuilder.build();
                        mCameraCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, childHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(IdentifyActivity.this, "配置失败", Toast.LENGTH_SHORT).show();
                }
            }, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera(int width, int height) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
//            requestCameraPermission();
            return;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, stateCallback, childHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private void setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    continue;
                }
                Log.d("camera", cameraId);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }
                Size size = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(size.getWidth(), size.getHeight(), ImageFormat.JPEG, 7);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, childHandler);
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, size);
                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }
                mCameraId = cameraId;
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }


    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size o1, Size o2) {
            return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
        }
    }


    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d("aaa", "有回调");
//            mFile = new File(getExternalFilesDir(null), System.currentTimeMillis() + ".jpg");
//            childHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
//            mHandler.sendEmptyMessageDelayed(0, 2000);
            ByteBuffer buffer = reader.acquireNextImage().getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
//            StoreByteImage(bytes);

        }

    };

    private Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                   int textureViewHeight, Size aspectRatio) {
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                }
            }
        }
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            return choices[0];
        }
    }

    /**
     * 关闭相机
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCameraCaptureSession) {
                mCameraCaptureSession.close();
                mCameraCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (Exception e) {

        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void stopThread() {
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            childHandler = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startThread() {
        handlerThread = new HandlerThread("CameraBackground");
        handlerThread.start();
        childHandler = new Handler(handlerThread.getLooper());
    }


    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            mCameraCaptureSession.capture(previewRequestBuilder.build(), mCaptureCallback,
                    childHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCameraCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCameraCaptureSession.capture(previewRequestBuilder.build(), mCaptureCallback,
                    childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        try {
            if (null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    unlockFocus();
                }
            };

            mCameraCaptureSession.stopRepeating();
            mCameraCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }





    private class FaceThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isStop) {
                mBitmap = mTextureView.getBitmap();
                if (mBitmap == null) {
                    continue;
                }
                int i = mBitmap.getWidth();
                int j = mBitmap.getHeight();
                Bitmap localBitmap2 = null;
                Matrix localMatrix = new Matrix();
                FaceDetector localFaceDetector = null;
                localFaceDetector = new FaceDetector(i, j, 1);
                localMatrix.postRotate(0.0F, i / 2, j / 2);
                localBitmap2 = Bitmap.createBitmap(i, j, Bitmap.Config.RGB_565);
                FaceDetector.Face[] arrayOfFace = new FaceDetector.Face[1];
                Paint localPaint1 = new Paint();
                Paint localPaint2 = new Paint();
                localPaint1.setDither(true);
                localPaint2.setColor(0xffff3000);
                localPaint2.setStyle(Paint.Style.STROKE);
                localPaint2.setStrokeWidth(2.0F);
                Canvas localCanvas = new Canvas();
                localCanvas.setBitmap(localBitmap2);
                localCanvas.setMatrix(localMatrix);
                localCanvas.drawBitmap(mBitmap, 0.0F, 0.0F, localPaint1); //该处将localBitmap1和localBitmap2关联（可不要？）

                int numberOfFaceDetected = localFaceDetector.findFaces(localBitmap2, arrayOfFace); //返回识脸的结果
                localBitmap2.recycle();
                Log.d("abc", numberOfFaceDetected + "");
                if(numberOfFaceDetected <= 0){
                    isStop = true;
                    errorHander.sendEmptyMessage(0);
                }
            }
        }
    }

    /**
     * 弹出提示框
     * @param message
     */
    private void showDialog(String message, int type){
        mIvAction.setImageResource(R.mipmap.scan_front);
        //设置人脸离开人脸框
        isExit = true;
        //设置人脸对比在检测中
        isChecking = true;
        //设置人脸动作在检测中
        isCheckAction = true;
        //设置人脸检测停止
        isStop = true;
        if(mFaceDialog == null){
            mFaceDialog = new FaceDialog(IdentifyActivity.this);
        }
        mFaceDialog.setType(type);
        mFaceDialog.setListener(new FaceDialog.FaceCallBackListener() {
            @Override
            public void cancle() {
                setResult(0);
                finish();
            }

            @Override
            public void face() {
                //开启人脸位置检测线程
                new FaceThread().start();
                //设置人脸位置在检测中
                isStop = false;
                //设置人脸对比没有检测
                isChecking = false;
                //设置人脸动作在检测中
                isCheckAction = true;
                isExit = false;
            }

            @Override
            public void action() {
                new FaceThread().start();
                isStop = false;
                isChecking = true;
                isCheckAction = false;
                isExit = false;
            }
        });
        mFaceDialog.setContent(message);
        if(!mFaceDialog.isShowing()) {
            mFaceDialog.show();
        }
    }





}
