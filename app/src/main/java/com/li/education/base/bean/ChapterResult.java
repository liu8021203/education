package com.li.education.base.bean;

import com.li.education.base.bean.vo.ChapterListVO;

/**
 * Created by liu on 2017/6/19.
 */

public class ChapterResult extends BaseResult{
    private ChapterListVO data;

    public ChapterListVO getData() {
        return data;
    }

    public void setData(ChapterListVO data) {
        this.data = data;
    }
}
