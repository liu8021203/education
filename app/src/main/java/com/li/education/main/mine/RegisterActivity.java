package com.li.education.main.mine;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.google.gson.Gson;
import com.li.education.R;
import com.li.education.base.AppData;
import com.li.education.base.BaseActivity;
import com.li.education.base.bean.AreaResult;
import com.li.education.base.bean.BaseResult;
import com.li.education.base.bean.CityResult;
import com.li.education.base.bean.HomeResult;
import com.li.education.base.bean.vo.AreaVO;
import com.li.education.base.bean.vo.CityVO;
import com.li.education.base.bean.vo.IdResult;
import com.li.education.base.bean.vo.IdVO;
import com.li.education.base.http.HttpService;
import com.li.education.base.http.RetrofitUtil;
import com.li.education.main.mine.adapter.AdapterVO;
import com.li.education.main.mine.adapter.AreaAdapter;
import com.li.education.main.mine.adapter.CityAdapter;
import com.li.education.main.mine.adapter.CycleAdapter;
import com.li.education.main.mine.adapter.TypeAdapter;
import com.li.education.util.UtilBitmap;
import com.li.education.util.UtilDisplay;
import com.li.education.util.UtilGson;
import com.li.education.util.UtilMD5Encryption;
import com.li.education.util.UtilPixelTransfrom;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import rx.Subscriber;

import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.io;

/**
 * Created by liu on 2017/6/12.
 */

public class RegisterActivity extends BaseActivity implements View.OnClickListener{
    private ImageView mIvBack;
    private EditText mEtId;
    private EditText mEtPassword;
    private EditText mEtRePassword;
    private Button mBtnGet;
    private Button mBtnRegister;
    private TextView mTvName;
    private TextView mTvPhone;
    private TextView mTvQua;
    private TextView mTvTime;
    private ImageView mIvImg;
    private Spinner mSpinnerCity;
    private Spinner mSpinnerArea;
    private Spinner mSpinnerType;
    private Spinner mSpinnerCycle;
    private IdVO vo;
    private File file;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initView();
    }

    private void initView() {
        mIvBack = (ImageView) findViewById(R.id.iv_back);
        mIvBack.setOnClickListener(this);
        mEtId = (EditText) findViewById(R.id.et_id);
        mEtPassword = (EditText) findViewById(R.id.et_password);
        mEtRePassword = (EditText) findViewById(R.id.et_repassword);
        mBtnGet = (Button) findViewById(R.id.btn_get);
        mBtnGet.setOnClickListener(this);
        mBtnRegister = (Button) findViewById(R.id.btn_register);
        mBtnRegister.setOnClickListener(this);
        mTvName = (TextView) findViewById(R.id.tv_name);
        mTvPhone = (TextView) findViewById(R.id.tv_phone);
        mTvQua = (TextView) findViewById(R.id.tv_qualification);
        mTvTime = (TextView) findViewById(R.id.tv_time);
        mIvImg = (ImageView) findViewById(R.id.iv_img);
        mIvImg.setOnClickListener(this);
        mSpinnerCity = (Spinner) findViewById(R.id.spinner_city);
        mSpinnerArea = (Spinner) findViewById(R.id.spinner_area);
        mSpinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CityVO vo = (CityVO) parent.getAdapter().getItem(position);
                getAreaData(vo.getCODE2());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mSpinnerType = (Spinner) findViewById(R.id.spinner_type);
        TypeAdapter typeAdapter = new TypeAdapter(this);
        mSpinnerType.setAdapter(typeAdapter);
        mSpinnerCycle = (Spinner) findViewById(R.id.spinner_cycle);
        CycleAdapter cycleAdapter = new CycleAdapter(this);
        mSpinnerCycle.setAdapter(cycleAdapter);
    }

    private void getIdData(String id){
        RetrofitUtil.getInstance().create(HttpService.class).getDataFrDB(id).subscribeOn(io()).observeOn(mainThread()).subscribe(new Subscriber<IdResult>() {

            @Override
            public void onStart() {
                super.onStart();
                showProgressDialog();
            }

            @Override
            public void onCompleted() {
                removeProgressDialog();
            }

            @Override
            public void onError(Throwable e) {
                removeProgressDialog();
                e.printStackTrace();
            }

            @Override
            public void onNext(IdResult result) {
                if(result.isStatus()){
                    vo = (IdVO) UtilGson.fromJson(result.getData().getData(), IdVO.class);
                    CityAdapter adapter = new CityAdapter(RegisterActivity.this);
                    adapter.setData(result.getData().getSysareList());
                    mSpinnerCity.setAdapter(adapter);
                    updateUI();
                }
            }
        });
    }

    /**
     * 获取城市数据
     */
    private void getCityData(){
        RetrofitUtil.getInstance().create(HttpService.class).getCode2ByCode1().subscribeOn(io()).observeOn(mainThread()).subscribe(new Subscriber<CityResult>() {

            @Override
            public void onStart() {
                super.onStart();
                showProgressDialog();
            }

            @Override
            public void onCompleted() {
                removeProgressDialog();
            }

            @Override
            public void onError(Throwable e) {
                removeProgressDialog();
                e.printStackTrace();
            }

            @Override
            public void onNext(CityResult result) {
                if(result.isStatus()){

                }
            }
        });
    }


    /**
     * 获取地区数据
     */
    private void getAreaData(String code){
        RetrofitUtil.getInstance().create(HttpService.class).getCode3ByCode2(code).subscribeOn(io()).observeOn(mainThread()).subscribe(new Subscriber<AreaResult>() {

            @Override
            public void onStart() {
                super.onStart();
                showProgressDialog();
            }

            @Override
            public void onCompleted() {
                removeProgressDialog();
            }

            @Override
            public void onError(Throwable e) {
                removeProgressDialog();
                e.printStackTrace();
            }

            @Override
            public void onNext(AreaResult result) {
                if(result.isStatus()){
                    AreaAdapter adapter = new AreaAdapter(RegisterActivity.this);
                    adapter.setData(result.getData().getSysareList());
                    mSpinnerArea.setAdapter(adapter);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.iv_back:
                onBackPressed();
                break;

            case R.id.btn_get:
                String id = mEtId.getText().toString();
                if(TextUtils.isEmpty(id)){
                    showToast("请输入身份证号");
                    return;
                }
                getIdData(id);
                break;

            case R.id.iv_img:
                ChoosePicDialog dialog = new ChoosePicDialog(RegisterActivity.this);
                dialog.show();
                break;

            case R.id.btn_register:
                register();
                break;
        }
    }


    private void register(){
        if(file == null){
            showToast("请上传头像");
            return;
        }
        String id = mEtId.getText().toString().trim();
        if(TextUtils.isEmpty(id)){
            showToast("请输入身份证号");
            return;
        }
        if(vo == null){
            showToast("请输入身份证获取相关数据");
            return;
        }
        CityVO cityVO = (CityVO) mSpinnerCity.getSelectedItem();
        AreaVO areaVO = (AreaVO) mSpinnerArea.getSelectedItem();
        if(cityVO.getTITLE2().equals("00") || areaVO.getTITLE3().equals("00")){
            showToast("请选择区域");
            return;
        }
        String password = mEtPassword.getText().toString().trim();
        String repassword = mEtRePassword.getText().toString().trim();
        if(TextUtils.isEmpty(password) || TextUtils.isEmpty(repassword)){
            showToast("请输入密码");
            return;
        }
        if(!password.equals(repassword)){
            showToast("两次输入的密码不一致");
            return;
        }
        if(password.length() < 6 || repassword.length() < 6){
            showToast("密码不能少于6位");
            return;
        }
        Map<String, RequestBody> map = new HashMap<>();
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), file);
        map.put("facefirsturl\"; filename=\"touxiang.jpg", requestBody);
        RequestBody bodyPassword = RequestBody.create(MediaType.parse("text/plain"), UtilMD5Encryption.getMd5Value(password));
        map.put("password", bodyPassword);
        RequestBody bodyID = RequestBody.create(MediaType.parse("text/plain"), id);
        map.put("paperscode", bodyID);
        RequestBody bodyTitle = RequestBody.create(MediaType.parse("text/plain"), vo.getXm());
        map.put("title", bodyTitle);
        RequestBody bodyTel = RequestBody.create(MediaType.parse("text/plain"), vo.getSjhm());
        map.put("tel", bodyTel);
        AdapterVO typeVO = (AdapterVO) mSpinnerType.getSelectedItem();
        RequestBody bodyType = RequestBody.create(MediaType.parse("text/plain"), typeVO.getCode());
        map.put("persontype", bodyType);

        RequestBody bodyCity = RequestBody.create(MediaType.parse("text/plain"), "14" + cityVO.getCODE2() + areaVO.getCODE3());
        map.put("area", bodyCity);
        RequestBody bodyArea = RequestBody.create(MediaType.parse("text/plain"), "山西省" + cityVO.getTITLE2() + areaVO.getTITLE3());
        map.put("tarea", bodyArea);
        RequestBody bodyObtaintime = RequestBody.create(MediaType.parse("text/plain"), vo.getCyzg_clrq());
        map.put("obtaintime", bodyObtaintime);
        AdapterVO cycleVO = (AdapterVO) mSpinnerCycle.getSelectedItem();
        RequestBody bodyCycle = RequestBody.create(MediaType.parse("text/plain"), cycleVO.getCode());
        map.put("cycle", bodyCycle);
        RequestBody bodyZyzgzh = RequestBody.create(MediaType.parse("text/plain"), vo.getCyzgzh());
        map.put("cyzgzh", bodyZyzgzh);
        RequestBody bodyCyzglb = RequestBody.create(MediaType.parse("text/plain"), vo.getCyzglb());
        map.put("cyzglb", bodyCyzglb);
        RequestBody bodyCyzg_yxqz = RequestBody.create(MediaType.parse("text/plain"), vo.getXcyzg_yxqz());
        map.put("cyzg_yxqz", bodyCyzg_yxqz);
        RequestBody bodyCyzg_fzrq = RequestBody.create(MediaType.parse("text/plain"), vo.getXcyzg_fzrq());
        map.put("cyzg_fzrq", bodyCyzg_fzrq);
        RequestBody bodyCyzg_clrq = RequestBody.create(MediaType.parse("text/plain"), vo.getCyzg_clrq());
        map.put("cyzg_clrq", bodyCyzg_clrq);
        RequestBody bodySex = RequestBody.create(MediaType.parse("text/plain"), vo.getXb());
        map.put("sex", bodySex);
        RetrofitUtil.getInstance().create(HttpService.class).register(map).subscribeOn(io()).observeOn(mainThread()).subscribe(new Subscriber<BaseResult>() {

            @Override
            public void onStart() {
                super.onStart();
                showProgressDialog();
            }

            @Override
            public void onCompleted() {
                removeProgressDialog();
            }

            @Override
            public void onError(Throwable e) {
                removeProgressDialog();
                showToast("注册失败");
                e.printStackTrace();
            }

            @Override
            public void onNext(BaseResult result) {
                if(result.isStatus()){
                    finish();
                }else{
                    showToast(result.getMessage());
                }
            }
        });
    }

    private void updateUI(){
        mTvName.setText(vo.getXm());
        mTvPhone.setText(vo.getSjhm());
        mTvQua.setText(vo.getCyzgzh());
        mTvTime.setText(vo.getCyzg_clrq());
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            switch (requestCode) {
                case 0:
                    File file = new File(AppData.PATH);
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    File fileImg = new File(AppData.PATH, "touxiang.jpg");
                    if (fileImg.exists()) {
//                    startPhotoZoom(Uri.fromFile(new File(AppData.PATH, "touxiang.jpg")));
                        setPicToView(Uri.fromFile(new File(AppData.PATH, "touxiang.jpg")));
                    }
                    break;
                case 1:
                    if (data != null) {
//                    startPhotoZoom(data.getData());
                        setPicToView(data.getData());
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void setPicToViewCamera(Uri uri){

    }


    private void setPicToView(Uri uri) {
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(uri, null, null, null, null);// 根据Uri从数据库中找
        if (cursor != null) {
            cursor.moveToFirst();// 把游标移动到首位，因为这里的Uri是包含ID的所以是唯一的不需要循环找指向第一个就是了
            String filePath = cursor.getString(cursor.getColumnIndex("_data"));// 获取图片路
            String orientation = cursor.getString(cursor
                    .getColumnIndex("orientation"));// 获取旋转的角度
            cursor.close();
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);//根据Path读取资源图片
            int angle = 0;
            if (orientation != null && !"".equals(orientation)) {
                angle = Integer.parseInt(orientation);
            }
            if (angle != 0) {
                // 下面的方法主要作用是把图片转一个角度，也可以放大缩小等
                Matrix m = new Matrix();
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                m.setRotate(angle); // 旋转angle度
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height,
                        m, true);// 从新生成图片

            }
            mIvImg.setImageBitmap(bitmap);
            file = UtilBitmap.compressBmpToFile(bitmap, AppData.PATH + "touxiang.jpg", 600);
        }else {
            int rotate = UtilBitmap.getBitmapDegree(uri.getPath());
            Bitmap bitmap = BitmapFactory.decodeFile(uri.getPath());//根据Path读取资源图片
            Bitmap bitmap1 = UtilBitmap.rotateBitmapByDegree(bitmap, rotate);
            mIvImg.setImageBitmap(bitmap1);
            file = UtilBitmap.compressBmpToFile(bitmap1, AppData.PATH + "touxiang.jpg", 600);
        }


    }


    // 获取图片信息返回 byte数组
    public static byte[] readStream(InputStream inStream) throws Exception {
        byte[] buffer = new byte[1024];
        int len = -1;
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        byte[] data = outStream.toByteArray();
        outStream.close();
        inStream.close();
        return data;
    }

    // 获取图片数组返回bitmap
    public static Bitmap getPicFromBytes(byte[] bytes,
                                         BitmapFactory.Options opts) {
        if (bytes != null) {
            if (opts != null) {
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length,
                        opts);
            } else {
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
        }
        return null;
    }

}
