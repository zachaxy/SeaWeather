package com.fenghuo.seaweather.bean;

import com.fenghuo.seaweather.utils.StrUtil;

/**
 * Created by zhangxin on 2016/8/30 0030.
 * <p>
 * Description :
 */
public class Msg {
    public boolean isRead = false;
    public int mMsgImg; //资源id
    public String mMsgContent; //内容
    public String mMsgTime;  //时间戳,已经被格式化过了...

    public Msg(int mMsgImg, String mMsgContent, String mMsgTime) {
        this.mMsgImg = mMsgImg;
        this.mMsgContent = mMsgContent;
        this.mMsgTime = StrUtil.formatTime(mMsgTime.toCharArray());
    }

    public String showMsg() {
        return mMsgContent + "\n" + "接收时间是: " + mMsgTime;
    }
}
