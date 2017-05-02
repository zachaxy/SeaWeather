package com.fenghuo.seaweather;

import android.util.Log;

import com.fenghuo.seaweather.bean.IMsg;
import com.fenghuo.seaweather.bean.Msg;
import com.fenghuo.seaweather.bean.TyphoonBean;
import com.fenghuo.seaweather.bean.TyphoonTrack;
import com.fenghuo.seaweather.bean.WeatherBean;
import com.fenghuo.seaweather.utils.Params;
import com.fenghuo.seaweather.utils.ParseUtil;
import com.fenghuo.seaweather.utils.StrUtil;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import static android.R.attr.breadCrumbShortTitle;
import static android.R.attr.data;
import static android.R.attr.switchMinWidth;
import static android.R.attr.visible;
import static android.R.attr.wallpaperCloseEnterAnimation;
import static android.os.Build.VERSION_CODES.M;
import static com.fenghuo.seaweather.utils.StrUtil.getIntergerLength;

/**
 * Created by zhangxin on 2017/4/19 0019.
 * <p>
 * Description :
 */

public class Help {

    static final String TAG = "###";
    //NOTE:这里为了编译通过,先用null代替了;
    private FHApplication fhApplication1 = null;

    private final int MSG = 1;                  //短消息
    private final int ORDINARY_WEATHER = 2;     //一般气象消息
    private final int SPECIAL_WEATHER = 3;      //紧急气象消息
    private final int BUSINESS_WEATHER = 4;     //商务信息
    private final int PAYMENT = 5;              //用户缴费
    private final int SIGNOUT = 8;              //注销用户
    private final int LOGIN = 9;                //注册用户(补发)

    //哪个单位用,分为山东,舟山,茂名,NOTE:具体数值还没有确定;
    private final int SHANDONG = 1;
    private final int MAOMING = 2;
    private final int ZHOUSHAN = 3;

    //哪种类型,分为海区/渔区/边缘海区/台风
    private final int SEA_AREA = 0;
    private final int FISH_AREA = 1;
    private final int EDGE_SEA_AREA = 2;
    private final int TYPHOON = 0xFF;

    /***
     * 解析应用数据
     *
     * @param timeStamp 时间戳并不在应用数据中包含,而是在外层中得到的
     * @param b         应用数据字节码
     */
    public void parseAppData(String timeStamp, byte[] b) {
        int infoType = b[0];
        switch (infoType) {
            case MSG:
                parseMsg(timeStamp, b);
                break;
            case ORDINARY_WEATHER:
                parseWeather(timeStamp, b);
                break;
            case SPECIAL_WEATHER:
                parseWeather(timeStamp, b);
                break;
            case BUSINESS_WEATHER:
                break;
            case PAYMENT:
                break;
            case SIGNOUT:
                break;
            case LOGIN:
                break;
            default:
                break;
        }
    }


    //注意,有可能会返回null;
    private Msg parseMsg(String timeStamp, byte[] data) {
        int msgIndex = 1;//从1开始,0代表的是消息类型,之前已经解析过了;
        int phoneLen = 7;
        //+86 13812345678 ==> 0x86 0x13 0x81 0x23 0x45 0x67 0x8F
        byte[] bPhone = new byte[phoneLen];
        for (int i = 0; i < phoneLen; i++) {
            bPhone[i] = data[msgIndex++];
        }
        //原本是14位的,这里要删掉最后一位,最后一位是F;
        String phoneNo = StrUtil.bytesToHexString(bPhone).substring(0, 13);
        Log.d(TAG, "parseMsg: 解析到的电话号码:" + phoneNo);

        int groupCount = data[msgIndex++];//如果为0就代表是单发,后面的ID就是接受的ID;否则就是群发的组数

        //单发的情况:
        if (groupCount == 0) {
            /*
            第一个字节表示当前接收机所属的地区，1为山东、2为茂名、3为舟山、4为山东和茂名通用、
            5为山东舟山通用、6为茂名舟山通用、7为山东舟山茂名通用；
            后2个字节表示具体的接收机编号，范围为：0~65535。
            NOTE:areaNo怎么处理???以及本地ID到底什么时候初始化???  是上岸使用之前就会设置好;
             */
            int areaNo = data[msgIndex++];
            //单发的话首先要判断是不是自己的组,如果不是那么直接抛弃吧

            if (isMyArea(Params.my_area, areaNo)) {
                int receiveID = (data[msgIndex++] + 256) % 256 * 256 + (data[msgIndex++] + 256) % 256;
                if ((receiveID != 0) && (receiveID != fhApplication1.ID)) {
                    Log.d(TAG, "不是群发,不是本机ID,舍弃");
                    return null;
                }
                if ((receiveID == fhApplication1.ID) && (Long.valueOf(timeStamp) > Long.valueOf(fhApplication1.date))) {
                    Log.d(TAG, "是本机ID,但是已过期,舍弃");
                    return null;
                }
            } else {
                return null;
            }
        } else {
            boolean isMyGroup = false;
            for (int i = 0; i < groupCount; i++) {
                //是自己的group,时间也没有过期,才证明是自己所在的group;
                if (Params.my_group == data[msgIndex++]
                        && (Long.valueOf(timeStamp) < Long.valueOf(fhApplication1.date))) {
                    isMyGroup = true;
                    break;
                }
            }
            if (!isMyGroup) {  //如果是自己的组,在判断时间戳信息;也有可能返回null;
                return null;
            }
        }
        String ordinaryMSG = "来电(" + phoneNo + ")";
        try {
            ordinaryMSG += new String(data, msgIndex, data.length - msgIndex, "gbk");
        } catch (UnsupportedEncodingException e) {
            Log.e("###", "短消息中的类型不能解析为中文");
        }
        return new Msg(R.drawable.msg_unread, ordinaryMSG, timeStamp);
    }

    //台风和天气混合在一起,是在没办法统一返回一个bean;只能;
    IMsg parseWeather(String timeStamp, byte[] data) {
        int weatherIndex = 1;//从1开始,0代表的是消息类型,之前已经解析过了;
        int company = data[weatherIndex++];  //代表是哪个公司,山东气象局、舟山气象局和茂名气象局
        //根据公司类型来确定分为几个区;
        int areaCount = 0;
        switch (company) {
            case SHANDONG:
                areaCount = Params.SHANDONG_AREA_COUNT;
                break;
            case ZHOUSHAN:
                areaCount = Params.ZHOUSHAN_AREA_COUNT;
                break;
            case MAOMING:
                areaCount = Params.MAOMING_AREA_COUNT;
                break;
        }

        int whatMsg = data[weatherIndex++]; //代表是哪个类型; 海区/渔区/台风

        //台风单独处理
        if (whatMsg == TYPHOON) {
            return parseTyphoon(data, weatherIndex);
        } else {
            //预报的时效; 高四位 + 低四位
            int forecastLength = data[weatherIndex++];

            //高四位每个单位多少小时:时效,24小时*7天或者12小时*2半天; 0:24小时  1:12小时;
            int forecastTimeInterval = forecastLength >> 4;

            //低4位表示预报多少个单位时间,七天或者半天*2;
            int forecastCount = forecastLength & 0xf;

            int fishAreaType = data[weatherIndex++];  //渔区/海区类别,0表示大渔区；1表示小渔区。海区则不判断此位。
            int groupCount = data[weatherIndex++];    //分组数量,后面的渔区/海区有多少分组;

            //分了几组,每个组中在接下来预报的天气都是相同的;
            //考虑用什么数据结构?二维数组???NOTE:需要设置为全局的吗?
            //行表示:哪个海区,从1开始    列表示:哪一天(有可能是七天,有可能是两个半天)
            //NOTE:设置为全局吧...
            WeatherBean[][] weathers = new WeatherBean[areaCount + 1][forecastCount];

            for (int i = 0; i < groupCount; i++) { //外层按组分
                for (int j = 0; j < forecastCount; j++) { //内层按照预报时间长度分
                    int count = data[weatherIndex++];  //第i组第j天的预报里共有几个组?
                    int tempIndex = weatherIndex;  //此时的tempIndex代表的是组号的起始字节;
                    weatherIndex += count; //先跳过count自己,因为要先解析接下来几组的相同的天气,然后在回填;
                    WeatherBean bean;
                    if (company == SHANDONG) {
                        bean = parseWeatherInShanDong(data, weatherIndex);
                        weatherIndex += 14;
                    } else if (company == ZHOUSHAN) {
                        bean = parseWeatherInZhouShan(data, weatherIndex);
                        weatherIndex += 18;
                    } else {
                        bean = parseWeatherInMaoMing(data, weatherIndex);
                        weatherIndex += 18;
                    }
                    //天气数据回填;
                    for (int k = tempIndex; k < tempIndex + count; k++) {
                        weathers[data[k]][j] = bean;
                    }
                }
            }


            String content = "";
            try {
                content += new String(data, weatherIndex, data.length - weatherIndex, "gbk");
            } catch (UnsupportedEncodingException e) {
                Log.e("###", "天气中的类型不能解析为中文");
            }
            // TODO: 2017/5/2 0002 单独弄一个 overall weather bean 来实现 IMsg接口返回行不行? 可以吧; 
            final String finalContent = content;
            return new IMsg() {
                @Override
                public String getMsgContent() {
                    return finalContent;
                }

                @Override
                public int getMsgType() {
                    return Params.type_weather;
                }
            };
        }
    }


    WeatherBean parseWeatherInMaoMing(byte[] data, int weatherIndex) {
        WeatherBean bean = new WeatherBean();
        bean.weatherType1 = data[weatherIndex++];
        bean.weatherType2 = data[weatherIndex++];

        boolean flag = false; //表明需不需要转,默认是需要转的;如果后和前都相同,flag=true,那么不需要转;

        int windDirect1 = data[weatherIndex++];

        int windPower1_1 = data[weatherIndex++];
        int windPower1_2 = data[weatherIndex++];
        String windPower1 = "";
        if (windPower1_1 == windPower1_2 || windPower1_2 == 0) {
            windPower1 = windPower1_1 + "级,";
        } else {
            windPower1 = windPower1_1 + "-" + windPower1_2 + "级,";
        }

        int gustWind1_1 = data[weatherIndex++];
        int gustWind1_2 = data[weatherIndex++];

        String gustWind1 = "阵风";
        if (gustWind1_1 == gustWind1_2 || gustWind1_2 == 0) {
            gustWind1 += gustWind1_1 + "级";
        } else {
            gustWind1 += gustWind1_1 + "-" + gustWind1_2 + "级";
        }

        //-------------------------------------------------
        int windDirect2 = data[weatherIndex++];

        int windPower2_1 = data[weatherIndex++];
        int windPower2_2 = data[weatherIndex++];
        String windPower2 = "";
        if (windPower2_1 == windPower2_2 || windPower2_2 == 0) {
            windPower2 = windPower2_1 + "级,";
        } else {
            windPower2 = windPower2_1 + "-" + windPower1_2 + "级,";
        }

        int gustWind2_1 = data[weatherIndex++];
        int gustWind2_2 = data[weatherIndex++];
        String gustWind2 = "阵风";
        if (gustWind2_1 == gustWind2_2 || gustWind2_2 == 0) {
            gustWind2 += gustWind2_1 + "级";
        } else {
            gustWind2 += gustWind2_1 + "-" + gustWind2_2 + "级";
        }


        if (windDirect2 == 0 && windPower2_1 == 0 && windPower2_2 == 0 && gustWind2_1 == 0 && gustWind2_2 == 0) {
            flag = true;
        }

        String desc = Params.windDirection[windDirect1] + windPower1 + gustWind1;
        if (!flag) {
            desc += "转" + Params.windDirection[windDirect2] + windPower2 + gustWind2;
        }

        int visible1 = data[weatherIndex++];
        int visible2 = data[weatherIndex++];
        String visible = "能见度" + visible1 + "-" + visible2 + "公里";

        int waveHeight1_1 = data[weatherIndex++];
        int waveHeight1_2 = data[weatherIndex++];
        int waveHeight2_1 = data[weatherIndex++];
        int waveHeight2_2 = data[weatherIndex++];

        String waveHeight = "浪高 " + waveHeight1_1 + "." + waveHeight1_2 + " - "
                + waveHeight2_1 + "." + waveHeight2_2 + " 米";


        bean.desc = desc;
        bean.visibility = visible;
        bean.waveHeight = waveHeight;

        return bean;
    }

    //舟山和茂名的气相区别在于浪高的处理上,茂名为小数,而这个为整数
    WeatherBean parseWeatherInZhouShan(byte[] data, int weatherIndex) {
        WeatherBean bean = new WeatherBean();
        bean.weatherType1 = data[weatherIndex++];
        bean.weatherType2 = data[weatherIndex++];

        boolean flag = false; //表明需不需要转,默认是需要转的;如果后和前都相同,flag=true,那么不需要转;

        int windDirect1 = data[weatherIndex++];

        int windPower1_1 = data[weatherIndex++];
        int windPower1_2 = data[weatherIndex++];
        String windPower1 = "";
        if (windPower1_1 == windPower1_2 || windPower1_2 == 0) {
            windPower1 = windPower1_1 + "级,";
        } else {
            windPower1 = windPower1_1 + "-" + windPower1_2 + "级,";
        }

        int gustWind1_1 = data[weatherIndex++];
        int gustWind1_2 = data[weatherIndex++];

        String gustWind1 = "阵风";
        if (gustWind1_1 == gustWind1_2 || gustWind1_2 == 0) {
            gustWind1 += gustWind1_1 + "级";
        } else {
            gustWind1 += gustWind1_1 + "-" + gustWind1_2 + "级";
        }

        //-------------------------------------------------
        int windDirect2 = data[weatherIndex++];

        int windPower2_1 = data[weatherIndex++];
        int windPower2_2 = data[weatherIndex++];
        String windPower2 = "";
        if (windPower2_1 == windPower2_2 || windPower2_2 == 0) {
            windPower2 = windPower2_1 + "级,";
        } else {
            windPower2 = windPower2_1 + "-" + windPower1_2 + "级,";
        }

        int gustWind2_1 = data[weatherIndex++];
        int gustWind2_2 = data[weatherIndex++];
        String gustWind2 = "阵风";
        if (gustWind2_1 == gustWind2_2 || gustWind2_2 == 0) {
            gustWind2 += gustWind2_1 + "级";
        } else {
            gustWind2 += gustWind2_1 + "-" + gustWind2_2 + "级";
        }


        if (windDirect2 == 0 && windPower2_1 == 0 && windPower2_2 == 0 && gustWind2_1 == 0 && gustWind2_2 == 0) {
            flag = true;
        }

        String desc = Params.windDirection[windDirect1] + windPower1 + gustWind1;
        if (!flag) {
            desc += "转" + Params.windDirection[windDirect2] + windPower2 + gustWind2;
        }

        int visible1 = data[weatherIndex++];
        int visible2 = data[weatherIndex++];
        String visible = "能见度" + visible1 + "-" + visible2 + "公里";

        int waveHeight1_1 = data[weatherIndex++];
        int waveHeight1_2 = data[weatherIndex++];
        int waveHeight2_1 = data[weatherIndex++];
        int waveHeight2_2 = data[weatherIndex++];

        String waveHeight = "浪高";

        if (waveHeight1_1 == waveHeight1_2 || waveHeight1_2 == 0) {
            waveHeight += waveHeight1_1 + "米";
        } else {
            waveHeight += waveHeight1_1 + " - " + waveHeight1_2 + "米";
        }

        if (waveHeight2_1 == waveHeight2_2 || waveHeight2_2 == 0) {
            waveHeight += "到" + waveHeight2_1 + "米";
        } else {
            waveHeight += "到" + waveHeight2_1 + " - " + waveHeight2_2 + "米";
        }


        bean.desc = desc;
        bean.visibility = visible;
        bean.waveHeight = waveHeight;

        return bean;
    }

    //山东和前两个的区别是少了阵风的描述;
    WeatherBean parseWeatherInShanDong(byte[] data, int weatherIndex) {
        WeatherBean bean = new WeatherBean();

        bean.weatherType1 = data[weatherIndex++];
        bean.weatherType2 = data[weatherIndex++];


        int windDirect1 = data[weatherIndex++];
        int windDirect2 = data[weatherIndex++];

        String windDirect;
        if (windDirect1 == windDirect2 || windDirect2 == 0) {
            windDirect = Params.windDirection[windDirect1];
        } else {
            windDirect = Params.windDirection[windDirect1] + "转" + Params.windDirection[windDirect2];
        }

        int windPower1_1 = data[weatherIndex++];
        int windPower1_2 = data[weatherIndex++];
        String windPower1;
        if (windPower1_1 == windPower1_2 || windPower1_2 == 0) {
            windPower1 = windPower1_1 + "级,";
        } else {
            windPower1 = windPower1_1 + "-" + windPower1_2 + "级,";
        }

        int windPower2_1 = data[weatherIndex++];
        int windPower2_2 = data[weatherIndex++];
        String windPower2;
        if (windPower2_1 == windPower2_2 || windPower2_2 == 0) {
            windPower2 = windPower2_1 + "级,";
        } else {
            windPower2 = windPower2_1 + "-" + windPower1_2 + "级,";
        }


        String desc = windDirect + windPower1 + "转" + windPower2;

        int visible1 = data[weatherIndex++];
        int visible2 = data[weatherIndex++];
        String visible = "能见度" + visible1 + "-" + visible2 + "公里";

        int waveHeight1_1 = data[weatherIndex++];
        int waveHeight1_2 = data[weatherIndex++];
        int waveHeight2_1 = data[weatherIndex++];
        int waveHeight2_2 = data[weatherIndex++];

        String waveHeight = "浪高 " + waveHeight1_1 + "." + waveHeight1_2 + " - "
                + waveHeight2_1 + "." + waveHeight2_2 + "米";

        bean.desc = desc;
        bean.visibility = visible;
        bean.waveHeight = waveHeight;

        return bean;
    }


    /***
     * 已经生成了一个TyphoonBean对象,待返回;
     * TODO:相同台风号轨迹如何保存,数据库???
     *
     * @param data
     * @param weatherIndex
     */
    TyphoonBean parseTyphoon(byte[] data, int weatherIndex) {
        int typhoonNo = data[weatherIndex++];
        int typhoonNameLen = 10;

        StringBuilder typhoonContent = new StringBuilder();
        typhoonContent.append("台风名称:");
        String typhoonName = "未知台风";
        try {
            typhoonName = new String(data, weatherIndex, typhoonNameLen, "gbk");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.e("###", "台风不支持的中文编码");
        }
        //台风内容初始化的时候前面是名字;
        typhoonContent.append(typhoonName).append(",台风级别:");

        weatherIndex += typhoonNameLen;
        int typhoonLevel = data[weatherIndex++];
        switch (typhoonLevel) {
            case 0:
                typhoonContent.append("热带低压(TD)");
                break;
            case 1:
                typhoonContent.append("热带风暴(TS)");
                break;
            case 2:
                typhoonContent.append("强热带风暴(STS)");
                break;
            case 3:
                typhoonContent.append("台风(TY)");
                break;
            case 4:
                typhoonContent.append("强台风(STY)");
                break;
            case 5:
                typhoonContent.append("超强台风(SSY)");
                break;
            case 6:
                typhoonContent.append("超强台风(SSTY)");
                break;
            default:
                typhoonContent.append("未知等级");
                break;
        }

        double typhoonX = getLocation(data, weatherIndex);
        weatherIndex += 4;

        double typhoonY = getLocation(data, weatherIndex);
        weatherIndex += 4;

        //下面处理轨迹点登陆时间,6字节;
        int typhoonTimeLen = 6;
        byte[] typhoonTime1 = new byte[typhoonNameLen];

        for (int i = 0; i < typhoonTimeLen; i++) {
            typhoonTime1[i] = data[weatherIndex++];
        }

        String typhoonTime2 = StrUtil.bytesToHexString(typhoonTime1);

        //真正需要的时间;
        String typhoonTime3 = StrUtil.formatTime(typhoonTime2.toCharArray());

        typhoonContent.append("。登陆时间：").append(typhoonTime3);

        String windPower = data[weatherIndex++] + "级";
        typhoonContent.append(".风力:").append(windPower);
        String windDirect = Params.windDirection[data[weatherIndex++]];
        typhoonContent.append(",风向:").append(windDirect);
        //气压,单位HPA
        int airPressure = (data[weatherIndex++] + 256) % 256 * 256 + (data[weatherIndex++] + 256) % 256;
        typhoonContent.append(",气压:").append(airPressure).append("百帕,");
        //目前移动方向
        String typhoonDirect = Params.windDirection[data[weatherIndex++]];
        typhoonContent.append("目前移动方向").append(typhoonDirect.substring(0, typhoonDirect.length() - 1));

        //移动速度,单位km/H
        int speed = data[weatherIndex++];
        typhoonContent.append(",移动速度:").append(speed).append("千米每小时.");
        int typhoonCircleCount = data[weatherIndex++];
        //*2,前者为风圈等级,后者为风圈范围;风圈到时候在地图上显示
        ArrayList<Integer> typhoonCircleList = new ArrayList<>(typhoonCircleCount * 2);
        for (int i = 0; i < typhoonCircleCount; i++) {
            typhoonCircleList.add(data[weatherIndex++] + 0);
            typhoonCircleList.add((data[weatherIndex++] + 256) % 256 * 256 + (data[weatherIndex++] + 256) % 256);
        }

       /*
        //预报实现协议不要了;
       int typhoonDays = data[weatherIndex++];  //预报时效,播报接下来几天的台风点;
        ArrayList<TyphoonTrack> typhoonTrackList = new ArrayList<>(typhoonDays);
        for (int i = 0; i < typhoonDays; i++) {
            double typhoonXi = getLocation(data, weatherIndex);
            weatherIndex += 4;
            double typhoonYi = getLocation(data, weatherIndex);
            weatherIndex += 4;

            int windPower1 = data[weatherIndex++];
            int windPower2 = data[weatherIndex++];
            String windPowerI;
            if (windPower2 == 0 || windPower1 == windPower2) {
                windPowerI = windPower1 + "级";
            } else {
                windPowerI = windPower1 + "-" + windPower2 + "级";
            }
            String windDirectI = Params.windDirection[data[weatherIndex++]];
            typhoonTrackList.add(new TyphoonTrack(typhoonXi, typhoonYi, windPowerI, windDirectI));

            weatherIndex += 4;//mad,还有四个自己的保留字段,直接跳过;
        }*/


        try {
            String content = new String(data, weatherIndex, data.length - weatherIndex, "gbk");
            typhoonContent.append(content);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            typhoonContent.append("未知内容");
        }
        // TODO: 2017/4/19 0019 暂时生成一个台风的bean对象;将台风对象也设置为全局的,已经没有办法返回了;
        return new TyphoonBean(typhoonNo, typhoonName, typhoonX, typhoonY,
                typhoonTime3, windPower, windDirect,
                typhoonCircleList, typhoonContent.toString());
    }

    /***
     * 拿到data中四个字节代表的坐标;
     *
     * @param data
     * @param weatherIndex
     * @return 返回经纬度坐标;
     */
    double getLocation(byte[] data, int weatherIndex) {
        int typhoonX1 = data[weatherIndex++];
        byte[] typhoonX2 = {data[weatherIndex++], data[weatherIndex++], data[weatherIndex++]};
        int typhoonX3 = Integer.valueOf(StrUtil.bytesToHexString(typhoonX2), 16);
        int res = StrUtil.getIntergerLength(typhoonX3);
        return typhoonX1 + typhoonX3 * 1.0 / res;
    }


    boolean isMyArea(int my_area, int area) {
        //my_area 的值肯定是在 1 2 3 之间;
        if (area <= 3 && my_area == area) {
            return true;
        }
        switch (area) {
            case 4:
                if (my_area == 1 || my_area == 2) {
                    return true;
                }
                break;
            case 5:
                if (my_area == 1 || my_area == 3) {
                    return true;
                }
                break;
            case 6:
                if (my_area == 2 || my_area == 3) {
                    return true;
                }
                break;
            case 7:
                return true;
        }
        return false;
    }
}
