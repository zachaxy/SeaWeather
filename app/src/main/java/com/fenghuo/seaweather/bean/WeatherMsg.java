package com.fenghuo.seaweather.bean;

/**
 * Created by zhangxin on 2016/8/30 0030.
 * <p>
 * Description :
 */
public class WeatherMsg extends Msg {
    public int mMsgImg2; //辅助资源id,用于天气.
    public String area; //判断其在哪一个区域
    public int period;  //0/1---判断其是24小时还是48小时


    public WeatherMsg(int mMsgImg, String mMsgContent, String mMsgTime) {
        super(mMsgImg, mMsgContent, mMsgTime);
    }

    @Override
    public String showMsg() {
        return super.showMsg();
    }
}
