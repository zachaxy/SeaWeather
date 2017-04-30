package com.fenghuo.seaweather.bean;

import java.io.Serializable;

/**
 * Created by zhangxin on 2017/4/19 0019.
 * <p>
 * Description :
 */

public class WeatherBean implements Serializable {
    public int weatherType1;
    public int weatherType2;
    /*public String windDirct;
    public String windPower;
    public String gustWind;*/

    public String desc;
    public String visibility;
    public String waveHeight;
}
