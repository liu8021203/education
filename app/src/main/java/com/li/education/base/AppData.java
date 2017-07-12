package com.li.education.base;

import android.os.Environment;

import com.li.education.base.bean.vo.PlayUploadVO;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liu on 2017/6/13.
 */

public class AppData {
    public static String PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/education/";

    public static String token = "";

    public static String cycle_code = "1";
    //考试题组别
    public static String paperid = "";
    /**
     * 是否有效
     */
    public static boolean isValid = false;

    public static List<PlayUploadVO> data = new ArrayList<>();
}
