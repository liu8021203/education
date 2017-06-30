package com.li.education.base.bean;

import com.li.education.base.bean.vo.HomeVO;

/**
 * Created by liu on 2017/6/8.
 */

public class HomeResult extends BaseResult{
    private HomeVO data;

    public HomeVO getData() {
        return data;
    }

    public void setData(HomeVO data) {
        this.data = data;
    }
}
