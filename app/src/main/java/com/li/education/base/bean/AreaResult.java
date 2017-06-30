package com.li.education.base.bean;

import com.li.education.base.bean.vo.AreaListVO;

/**
 * Created by liu on 2017/6/15.
 */

public class AreaResult extends BaseResult{
    private AreaListVO data;

    public AreaListVO getData() {
        return data;
    }

    public void setData(AreaListVO data) {
        this.data = data;
    }
}
