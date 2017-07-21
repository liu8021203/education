package com.li.education.main.identify;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Camera;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;

import com.li.education.R;
import com.li.education.base.AppData;
import com.li.education.base.BaseActivity;
import com.li.education.base.bean.FaceResult;
import com.li.education.base.bean.vo.Face2FaceVO;
import com.li.education.base.bean.vo.FaceActionResult;
import com.li.education.base.http.HttpService;
import com.li.education.base.http.RetrofitUtil;
import com.li.education.util.UtilBitmap;
import com.li.education.util.UtilData;
import com.li.education.util.UtilGson;
import com.li.education.util.UtilSPutil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import rx.Subscriber;

import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.io;

/**
 * Created by liu on 2017/7/17.
 */

public class ScanActivity extends BaseActivity implements SurfaceHolder.Callback {
    private int face_close_eye = 5;
    private int face_open_month = 5;
    private int face_up_head_min = 8;
    private int face_up_head_max = 25;
    private float face_similarity = 0.8f;

    private File mFile;

    private SurfaceView mSurfaceView;

    private ImageView mIvAction;
    //是否停止检测人脸位置
    private boolean isStop = false;
    private boolean isChecking = false;
    private boolean isCheckAction = true;
    //是否离开人脸框
    private boolean isExit = false;
    private boolean isFirstExit = false;
    private Camera camera;//声明相机
    private Bitmap mBitmap;

    private FaceDialog mFaceDialog;
    //提示人脸是否离开相框
    private FaceTipDialog mFaceTipDialog;
    //是否首次启动
    private boolean isFirst = true;
    //当前动作：1张嘴，2闭眼，3抬头
    private int currAction = -1;
    //是否是从home来的
    private boolean isBackground = false;
    private SurfaceHolder mSurfaceHolder;
    private int n = 0;
    private int cameraPosition = 1;//0代表前置摄像头，1代表后置摄像头

    private int a = 0;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mSurfaceView = (SurfaceView) findViewById(R.id.preview_view);
        mIvAction = (ImageView) findViewById(R.id.iv_action);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        init();
    }

    private void init() {
        face_close_eye = UtilSPutil.getInstance(this).getInt("eye", 5);
        face_open_month = UtilSPutil.getInstance(this).getInt("month", 5);
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

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("aaa", "surfaceCreated");
        if (camera == null) {
            //切换前后摄像头
            int cameraCount = 0;
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            cameraCount = Camera.getNumberOfCameras();//得到摄像头的个数
            for (int i = 0; i < cameraCount; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    cameraPosition = i;
                }
            }
            camera = Camera.open(cameraPosition);
            if(Build.BRAND.equals("Xiaomi")){
                camera.setDisplayOrientation(270);
            }else {
                camera.setDisplayOrientation(90);
            }
            camera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
                @Override
                public void onFaceDetection(Camera.Face[] faces, final Camera camera) {
                    if (null == faces || faces.length == 0) {
                        if(mFaceTipDialog == null){
                            mFaceTipDialog = new FaceTipDialog(ScanActivity.this);
                            mFaceTipDialog.setListener(new FaceTipDialog.FaceCallBackListener() {
                                @Override
                                public void cancle() {
                                    setResult(0);
                                    finish();
                                }
                            });
                        }
                        mFaceTipDialog.setType(0);
                        mFaceTipDialog.setContent("人脸不能离开相框，检测已中断");
                        if (!mFaceTipDialog.isShowing()) {
                            mFaceTipDialog.show();
                        }
                        //设置人脸对比在检测中
                        isChecking = true;
                        //设置人脸动作在检测中
                        isCheckAction = true;
                        isExit = true;
                    } else {
                        isExit = false;
                        if(mFaceTipDialog != null && mFaceTipDialog.isShowing()){
                            mFaceTipDialog.dismiss();
                            isChecking = false;
                            isCheckAction = true;
                        }
                    }
                }


            });
            camera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (camera != null && (!isChecking || !isCheckAction)) {
                        Camera.Size size = camera.getParameters().getPreviewSize(); //获取预览大小
                        final int w = size.width;  //宽度
                        final int h = size.height;
                        final YuvImage image = new YuvImage(data, ImageFormat.NV21, w, h, null);
                        ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
                        if (!image.compressToJpeg(new Rect(0, 0, w, h), 100, os)) {
                            return;
                        }
                        byte[] tmp = os.toByteArray();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);
                        if (bitmap == null) {
                            return;
                        }
                        Matrix matrixs = new Matrix();
                        if(Build.BRAND.equals("Xiaomi")){
                            matrixs.setRotate(90);
                        }else {
                            matrixs.setRotate(270);
                        }
                        mBitmap = Bitmap.createBitmap(bitmap, 0,0,bitmap.getWidth(), bitmap.getHeight(), matrixs, true);
                        bitmap.recycle();
                        Log.d("bbb", isChecking + "");
                        if (!isChecking && mBitmap != null) {
                            isChecking = true;
                            faceHandler.sendEmptyMessageDelayed(0, 2000);
                        }
                        if (!isCheckAction && mBitmap != null) {
                            isCheckAction = true;
                            actionHandler.sendEmptyMessageDelayed(0, 1000);
                        }
                    }
                }
            });
            try {
                camera.setPreviewDisplay(holder);//通过surfaceview显示取景画面
                camera.startPreview();//开始预览
                camera.startFaceDetection();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("aaa", "surfaceDestroyed");
        //当surfaceview关闭时，关闭预览并释放资源
        camera.setPreviewCallback(null);
        camera.stopFaceDetection();
        camera.stopPreview();
        camera.release();
        camera = null;
        mSurfaceView = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isBackground) {
            isBackground = false;
            showDialog("检测失败", 0);
        }
        //设置人脸对比是否在检测中
        isChecking = false;
        //设置人脸动作是否在检测中
        isCheckAction = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isBackground = true;
    }

    /**
     * 弹出提示框
     *
     * @param message
     */
    private void showDialog(String message, int type) {
        mIvAction.setImageResource(R.mipmap.scan_front);
        //设置人脸离开人脸框
        isExit = true;
        //设置人脸对比在检测中
        isChecking = true;
        //设置人脸动作在检测中
        isCheckAction = true;
        if (mFaceDialog == null) {
            mFaceDialog = new FaceDialog(ScanActivity.this);
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
                //设置人脸对比没有检测
                isChecking = false;
                //设置人脸动作在检测中
                isCheckAction = true;
            }

            @Override
            public void action() {
                //设置人脸对比检查完成
                isChecking = true;
                //动作对比没有检测
                isCheckAction = false;
            }
        });
        mFaceDialog.setContent(message);
        if (!mFaceDialog.isShowing()) {
            mFaceDialog.show();
        }
    }


    private void startAction() {
        int random = UtilData.getRandom(1, 3);
        switch (random) {
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
            case 2: {
                Uri notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.face_close_eye);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
                mIvAction.setImageResource(R.drawable.face_close_eye);
                AnimationDrawable animationDrawable = (AnimationDrawable) mIvAction.getDrawable();
                animationDrawable.start();
                currAction = 2;
            }
            break;
            //向上抬头
            case 3: {
                Uri notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.face_up_head);
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

    /**
     * 人脸验证
     */
    private void checkFace() {
        if (isExit) {
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
                Log.d("aaa", "人脸比对开始");
            }

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Log.d("aaa", "人脸比对报错" + e.getMessage());
                if (!isExit) {
                    showDialog("人脸识别不通过", 1);
                    isChecking = true;
                    isCheckAction = true;
                }
            }

            @Override
            public void onNext(FaceResult result) {
                if (!isExit) {
                    if (result.isStatus()) {
                        Face2FaceVO faceVO = (Face2FaceVO) UtilGson.fromJson(result.getData().getFace2faceResult(), Face2FaceVO.class);
                        Log.d("aaa", "人脸比对完成" + faceVO.getSimilar());
                        setResult(1);
                        finish();
//                        if (faceVO.getSimilar() > face_similarity) {
//                            isChecking = true;
//                            isCheckAction = false;
//                        } else {
//                            showDialog("人脸识别不通过", 1);
//                            isChecking = true;
//                            isCheckAction = true;
//                        }
                    } else {
                        showDialog("人脸识别不通过", 1);
                        isChecking = true;
                        isCheckAction = true;
                    }
                }
            }
        });
    }

    /**
     * 动作比对
     */
    private void checkAction() {
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
                if (!isExit) {
                    if (result.isResult()) {
                        switch (currAction) {
                            case 1: {
                                int x = result.getPoints().get(47).getY() - result.getPoints().get(44).getY();
                                if (x > face_open_month) {
                                    setResult(1);
                                    finish();
                                } else {
                                    if (n < 10) {
                                        n++;
                                        showDialog("人脸动作验证失败", 2);
                                    } else {
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
                                    if (n < 10) {
                                        n++;
                                        showDialog("人脸动作验证失败", 2);
                                    } else {
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
                                    if (n < 10) {
                                        n++;
                                        showDialog("人脸动作验证失败", 2);
                                    } else {
                                        showDialog("人脸动作验证失败", 1);
                                    }
                                }
                            }
                            break;
                        }
                    }else{
                        if (n < 10) {
                            n++;
                            showDialog("人脸动作验证失败", 2);
                        } else {
                            showDialog("人脸动作验证失败", 1);
                        }
                    }
                }
            }
        });
    }


}
