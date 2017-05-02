package com.fenghuo.seaweather.bean;

/**
 * Created by zhangxin on 2017/5/2 0002.
 * <p>
 * Description :
 */

public interface IMsg {
    //获取天气的文字描述,在右侧显示;
    String getMsgContent();
    //获取天气的类型是台风还是天气;
    int getMsgType();
}
