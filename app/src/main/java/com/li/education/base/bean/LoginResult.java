package com.li.education.base.bean;

import com.li.education.base.bean.vo.LoginVO;

/**
 * Created by liu on 2017/6/20.
 */

public class LoginResult extends BaseResult{
    private LoginVO data;

    public LoginVO getData() {
        return data;
    }

    public void setData(LoginVO data) {
        this.data = data;
    }
}
