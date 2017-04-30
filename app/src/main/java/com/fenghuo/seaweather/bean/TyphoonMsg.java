package com.fenghuo.seaweather.bean;

/**
 * Created by zhangxin on 2016/8/30 0030.
 * <p>
 * Description :
 */
public class TyphoonMsg extends Msg {
    public int number;
    public String name;

    //设置进来的(x,y)都是相对与ImageView中心点的位置
    public int x;
    public int y;

    public int is7;
    public int rang7;

    public int is10;
    public int rang10;

    public int is12;
    public int rang12;


    public TyphoonMsg(int mMsgImg, String mMsgContent, String mMsgTime) {
        super(mMsgImg, mMsgContent, mMsgTime);
    }

    public void setLocate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setRang(int is7, int rang7, int is10, int rang10, int is12, int rang12) {
        this.is7 = is7;
        this.rang7 = rang7;
        this.is10 = is10;
        this.rang10 = rang10;
        this.is12 = is12;
        this.rang12 = rang12;
    }

    public void setTyphoonDetail(String help) {
        //help格式:[name;number;x,y;is7,range7;is10,range10;is12,range12]
        String[] details = help.split(";");
        name = details[0];
        int index = 0;
        number = Integer.valueOf(details[++index]);
        x = Integer.valueOf(details[++index]);
        y = Integer.valueOf(details[++index]);
        is7 = Integer.valueOf(details[++index]);
        rang7 = Integer.valueOf(details[++index]);
        is10 = Integer.valueOf(details[++index]);
        rang10 = Integer.valueOf(details[++index]);
        is12 = Integer.valueOf(details[++index]);
        rang12 = Integer.valueOf(details[++index]);
    }

}
