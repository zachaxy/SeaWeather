package com.fenghuo.seaweather.utils;

import android.util.Log;

import com.fenghuo.seaweather.R;
import com.fenghuo.seaweather.bean.Msg;
import com.fenghuo.seaweather.bean.TyphoonMsg;
import com.fenghuo.seaweather.bean.WeatherMsg;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import static com.fenghuo.seaweather.utils.Params.weatherImg;
import static com.fenghuo.seaweather.utils.Params.weatherName;
import static com.fenghuo.seaweather.utils.Params.windDirection;

/**
 * Created by zhangxin on 2016/8/30 0030.
 * <p>
 * Description :该工具主要用来解析应用层协议.
 */
public class ParseUtil {



    //build系列的方法的作用是将接收到的消息内容进行格式化的构建,具体时间的格式化会在
    // TODO: 2016/10/18 0018 发现bug:传入的time是未格式化过的,但是这里返回的msg,在其构造函数中已经对msg的time进行了格式化 
    public static Msg buildMsg(byte[] b, String time) {
        byte[] bpNo = {b[4], b[5], b[6], b[7], b[8], b[9], b[10]};
        String phoneNo = StrUtil.bytesToHexString(bpNo).substring(0, 13);
        String ordinaryMSG = "来电(" + phoneNo + ")";
        try {
            ordinaryMSG += new String(b, 4 + 7, b.length - 4 - 7, "gbk");
        } catch (UnsupportedEncodingException e) {
            Log.e("###", "短消息中的类型不能解析为中文");
            return null;
        }

        return new Msg(R.drawable.msg_unread, ordinaryMSG, time);
    }

    public static Msg buildBussnisMsg(byte[] b, String time) {
        String ordinaryMSG = "";
        try {
            ordinaryMSG = new String(b, 4, b.length - 4, "gbk");
        } catch (UnsupportedEncodingException e) {
            Log.e("###", "短消息中的类型不能解析为中文");
            return null;
        }

        return new Msg(R.drawable.business_msg_unread, ordinaryMSG, time);
    }

    public static WeatherMsg buildWeather(byte[] data, String time) {

        String text = "";
        int startIndex = 4;

        int weatherType1 = data[startIndex++]; // 天气的标识码;
        int weatherType2 = data[startIndex++]; // 天气的标识码;
        text += Params.weatherName[weatherType1] + "转" + weatherName[weatherType2];

        byte[] bArea = {data[startIndex++], data[startIndex++], data[startIndex++], data[startIndex++]};
        String area = Integer.toBinaryString(Integer.valueOf(StrUtil.bytesToHexString(bArea), 16));

        int period = data[startIndex++];

        //风向
        int windDire = data[startIndex++];
        int windDire1 = (windDire >> 4) & 0x0f;
        int windDire2 = windDire & 0x0f;

        //风力
        int windPower1 = data[startIndex++];
        int windPower1_1 = (windPower1 >> 4) & 0x0f;
        int windPower1_2 = windPower1 & 0x0f;

        int windPower2 = data[startIndex++];
        int windPower2_1 = (windPower2 >> 4) & 0x0f;
        int windPower2_2 = windPower2 & 0x0f;

        //阵风
        int gustWind1 = data[startIndex++];
        int gustWind1_1 = (gustWind1 >> 4) & 0x0f;
        int gustWind1_2 = gustWind1 & 0x0f;
        int gustWind2 = data[startIndex++];
        int gustWind2_1 = (gustWind2 >> 4) & 0x0f;
        int gustWind2_2 = gustWind2 & 0x0f;

        text += Params.windDirection[windDire1] + windPower1_1 + "-" + windPower1_2 + "级,阵风" + gustWind1_1 + "-" + gustWind1_2 + "级 转 ";
        text += windDirection[windDire2] + windPower2_1 + "-" + windPower2_2 + "级,阵风" + gustWind2_1 + "-" + gustWind2_2 + "级 能见度";

        //TODO:能见度/浪高.
        int visibility1 = data[startIndex++];
        int visibility2 = data[startIndex++];
        text += visibility1 + "-" + visibility2;

        int waveHeight1 = data[startIndex++];
        int waveHeight2 = data[startIndex++];
        text += "浪高" + waveHeight1 * 1.0 / 10 + "-" + waveHeight2 * 1.0 / 10 + "\n";

        try {
            text += new String(data, startIndex, data.length - startIndex, "gbk");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            Log.e("###", "天气类型中的内容不能转化为中文!!!");
            return null;
        }
        Log.d("###", "天气中的内容是:" + text);
        WeatherMsg msg = new WeatherMsg(weatherImg[weatherType1], text, time);
        msg.mMsgImg2 = weatherImg[weatherType2];
        msg.area = area;
        msg.period = period;
        return msg;
    }

    public static TyphoonMsg buildTyphoon(byte[] data, String time) {
        String text = "";
        int startIndex = 4;
        int typhoonNo = data[startIndex++]; // 台风代号
        text += typhoonNo + "号台风\n";
        byte[] bx = new byte[]{data[startIndex++], data[startIndex++]};
        byte[] by = new byte[]{data[startIndex++], data[startIndex++]};
        int x = convertLocator(Integer.valueOf(StrUtil.bytesToHexString(bx), 16));
        int y = convertLocator(Integer.valueOf(StrUtil.bytesToHexString(by), 16));

        //风力
        int windPower1 = data[startIndex++];
        int windPower1_1 = (windPower1 >> 4) & 0x0f;
        int windPower1_2 = windPower1 & 0x0f;
        //风向
        int windDire1 = data[startIndex++];

        text += windDirection[windDire1] + windPower1_1 + "-" + windPower1_2 + "级";

        int is7 = data[startIndex++];
        byte[] r7 = new byte[]{data[startIndex++], data[startIndex++]};
        int rang7 = Integer.valueOf(StrUtil.bytesToHexString(r7), 16);
        if (is7 == 1) {
            text += "\n七级风圈,风圈影响范围:" + rang7;
        }


        int is10 = data[startIndex++];
        byte[] r10 = new byte[]{data[startIndex++], data[startIndex++]};
        int rang10 = Integer.valueOf(StrUtil.bytesToHexString(r10), 16);
        if (is10 == 1) {
            text += "\n十级风圈,风圈影响范围:" + rang10;
        }


        int is12 = data[startIndex++];
        byte[] r12 = new byte[]{data[startIndex++], data[startIndex++]};
        int rang12 = Integer.valueOf(StrUtil.bytesToHexString(r12), 16);
        if (is12 == 1) {
            text += "\n十二级风圈,风圈影响范围:" + rang12;
        }


        String name = "";
        String content = "";
        try {
            name = new String(data, startIndex, 6, "gbk");
            text += "\n台风名称:" + name;
            content = new String(data, startIndex + 6, data.length - startIndex - 6, "gbk");
            text += "\n" + content;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        TyphoonMsg msg = new TyphoonMsg(R.drawable.w38, text, time);
        msg.name = name;
        msg.number = typhoonNo;
        msg.setLocate(x, y);
        msg.setRang(is7, rang7, is10, rang10, is12, rang12);
        return msg;
    }

    /***
     * 返回18个海区的天气信息,共36个(前18为24小时天气,后18为48小时天气)
     *
     * @param data
     * @param time
     * @return
     */
    public static ArrayList<WeatherMsg> builWeatherAll(byte[] data, String time) {
        //TODO: 是否强制要将字节数字限定为 4 + 18 * 2 * 11
        if (data.length != 396 + 4) {
            Log.e("ParseUtil", "builWeatherAll: 字节数不对");
            return null;
        }

        int startIndex = 4;
        ArrayList<WeatherMsg> allArea = new ArrayList<>();

        for (int i = 0; i < 36; i++) {
            String text = "";
            int weatherType1 = data[startIndex++]; // 天气的标识码;
            int weatherType2 = data[startIndex++]; // 天气的标识码;

            //风向
            int windDire = data[startIndex++];
            int windDire1 = (windDire >> 4) & 0x0f;
            int windDire2 = windDire & 0x0f;

            //风力
            int windPower1 = data[startIndex++];
            int windPower1_1 = (windPower1 >> 4) & 0x0f;
            int windPower1_2 = windPower1 & 0x0f;

            int windPower2 = data[startIndex++];
            int windPower2_1 = (windPower2 >> 4) & 0x0f;
            int windPower2_2 = windPower2 & 0x0f;

            //阵风
            int gustWind1 = data[startIndex++];
            int gustWind1_1 = (gustWind1 >> 4) & 0x0f;
            int gustWind1_2 = gustWind1 & 0x0f;

            int gustWind2 = data[startIndex++];
            int gustWind2_1 = (gustWind2 >> 4) & 0x0f;
            int gustWind2_2 = gustWind2 & 0x0f;

            //能见度
            int visibility1 = data[startIndex++];
            int visibility2 = data[startIndex++];

            //浪高
            int waveHeight1 = data[startIndex++];
            int waveHeight2 = data[startIndex++];


            text += weatherName[weatherType1] + "转" + weatherName[weatherType2];
            text += windDirection[windDire1] + windPower1_1 + "-" + windPower1_2 + "级,阵风" + gustWind1_1 + "-" + gustWind1_2 + "级 转 ";
            text += windDirection[windDire2] + windPower2_1 + "-" + windPower2_2 + "级,阵风" + gustWind2_1 + "-" + gustWind2_2 + "级 \n能见度";
            text += visibility1 + "-" + visibility2;
            text += "  浪高" + waveHeight1 * 1.0 / 10 + "-" + waveHeight2 * 1.0 / 10;
            WeatherMsg msg = new WeatherMsg(weatherImg[weatherType1], text, time);
            msg.mMsgImg2 = weatherImg[weatherType2];
            allArea.add(msg);
        }
        return allArea;
    }


    /***
     * 将接收到的坐标转换为移动端ImageView相对于中心点的坐标
     * todo:目前使用的是int,存在的问题是精度不够,误差范围在大约6像素,也就是在相距6像素的两个点将无法区分
     *
     * @param x
     * @return
     */
    private static int convertLocator(int x) {
        //首先转换成当前像素:  4677*4677是PC端图片的像素,1579*1579是移动端图片的像素.727*727是ImageView的大小;
       /* int x1 = x * 1579 / 4677;
        return x1 * 727 / 1579 - 363;*/
        return x * 727 / 4677 - 363;
    }
}
