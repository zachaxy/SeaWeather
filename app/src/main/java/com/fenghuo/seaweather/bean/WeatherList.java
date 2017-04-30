package com.fenghuo.seaweather.bean;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by zhangxin on 2017/4/19 0019.
 * <p>
 * Description : 引入ACache,不用数据库存储了;
 */

public class WeatherList implements Serializable {
    int areaNo;  //区号
    int time;   //预报时长
    int count;  //存了几项
    ArrayList<WeatherBean> list;

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public ArrayList<WeatherBean> getList() {
        return list;
    }

    public void setList(ArrayList<WeatherBean> list) {
        this.list = list;
    }
}
