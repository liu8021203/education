package com.li.education.base.bean;

import com.li.education.base.bean.vo.UploadStudyVO;

/**
 * Created by liu on 2017/7/19.
 */

public class UploadStudyResult extends BaseResult{
    private UploadStudyVO data;

    public UploadStudyVO getData() {
        return data;
    }

    public void setData(UploadStudyVO data) {
        this.data = data;
    }
}
