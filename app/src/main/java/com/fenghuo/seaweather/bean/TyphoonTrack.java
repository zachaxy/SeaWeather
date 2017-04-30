package com.fenghuo.seaweather.bean;

/**
 * Created by zhangxin on 2017/4/19 0019.
 * <p>
 * Description :台风轨迹点
 * 收到一次台风预报时,会附带接下来几天的轨迹点,作为记录
 */

public class TyphoonTrack {
    double typhoonX;
    double typhoonY;

    String windPower;
    String windDirect;

    String other;

    public TyphoonTrack(double typhoonX, double typhoonY, String windPower, String windDirect) {
        this.typhoonX = typhoonX;
        this.typhoonY = typhoonY;
        this.windPower = windPower;
        this.windDirect = windDirect;
    }
}
