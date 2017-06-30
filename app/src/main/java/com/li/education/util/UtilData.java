package com.li.education.util;

import java.util.Random;

/**
 * Created by liu on 2017/6/27.
 */

public class UtilData {
    /**
     * 返回min-max之间的随机数
     * @param min
     * @param max
     * @return
     */
    public static int getRandom(int min, int max){
        Random random = new Random();
        int s = random.nextInt(max) % (max - min + 1) + min;
        return s;
    }
}
