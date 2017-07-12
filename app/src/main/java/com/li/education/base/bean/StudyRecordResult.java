package com.li.education.base.bean;

import com.li.education.base.bean.vo.StudyRecordVO;

import java.util.List;

/**
 * Created by liu on 2017/7/11.
 */

public class StudyRecordResult{
    private List<StudyRecordVO> list;
    private int pagesize;
    private int count;
    private int lastpagenum;
    private int pagenum;

    public List<StudyRecordVO> getList() {
        return list;
    }

    public void setList(List<StudyRecordVO> list) {
        this.list = list;
    }

    public int getPagesize() {
        return pagesize;
    }

    public void setPagesize(int pagesize) {
        this.pagesize = pagesize;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getLastpagenum() {
        return lastpagenum;
    }

    public void setLastpagenum(int lastpagenum) {
        this.lastpagenum = lastpagenum;
    }

    public int getPagenum() {
        return pagenum;
    }

    public void setPagenum(int pagenum) {
        this.pagenum = pagenum;
    }
}
