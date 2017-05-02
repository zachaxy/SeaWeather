package com.fenghuo.seaweather.bean;

import com.fenghuo.seaweather.utils.Params;

import java.io.Serializable;

/**
 * Created by zhangxin on 2017/4/19 0019.
 * <p>
 * Description :
 */

public class WeatherBean implements Serializable, IMsg {
    public int weatherType1;
    public int weatherType2;
    /*public String windDirct;
    public String windPower;
    public String gustWind;*/

    public String desc;  //只包含 风向 风力 阵风 的转向;
    public String visibility;
    public String waveHeight;

    @Override
    public String getMsgContent() {
        return desc + "，" + visibility + "，" + waveHeight;
    }

    @Override
    public int getMsgType() {
        return Params.type_weather;
    }
}
