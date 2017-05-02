package com.fenghuo.seaweather.bean;

import com.fenghuo.seaweather.utils.Params;

import java.util.ArrayList;

/**
 * Created by zhangxin on 2017/4/19 0019.
 * <p>
 * Description :台风的bean对象;
 */

public class TyphoonBean implements IMsg {
    int typhoonNo;  //台风代号
    String typhoonName;//台风名称;
    double typhoonX;//当前台风坐标x;
    double typhoonY;//当前台风坐标y;
    String typhoonTime;//台风登陆时间;
    String windPower;
    String windDirect;
    ArrayList<Integer> typhoonCircleList; //台风风圈,用来在地图上显示
    //ArrayList<TyphoonTrack> typhoonTrackList; //台风轨迹点;
    String typhoonContent;                      //台风描述内容;

/*    public TyphoonBean(int typhoonNo, String typhoonName, double typhoonX, double typhoonY, String typhoonTime, String
            windPower, String windDirect, ArrayList<Integer> typhoonCircleList, ArrayList<TyphoonTrack>
                               typhoonTrackList, String typhoonContent) {
        this.typhoonNo = typhoonNo;
        this.typhoonName = typhoonName;
        this.typhoonX = typhoonX;
        this.typhoonY = typhoonY;
        this.typhoonTime = typhoonTime;
        this.windPower = windPower;
        this.windDirect = windDirect;
        this.typhoonCircleList = typhoonCircleList;
        this.typhoonTrackList = typhoonTrackList;
        this.typhoonContent = typhoonContent;
    }*/

    public TyphoonBean(int typhoonNo, String typhoonName, double typhoonX, double typhoonY,
                       String typhoonTime, String windPower, String windDirect,
                       ArrayList<Integer> typhoonCircleList, String typhoonContent) {
        this.typhoonNo = typhoonNo;
        this.typhoonName = typhoonName;
        this.typhoonX = typhoonX;
        this.typhoonY = typhoonY;
        this.typhoonTime = typhoonTime;
        this.windPower = windPower;
        this.windDirect = windDirect;
        this.typhoonCircleList = typhoonCircleList;
        this.typhoonContent = typhoonContent;
    }

    @Override
    public String getMsgContent() {
        return typhoonContent;
    }

    @Override
    public int getMsgType() {
        return Params.type_typhooon;
    }
}
