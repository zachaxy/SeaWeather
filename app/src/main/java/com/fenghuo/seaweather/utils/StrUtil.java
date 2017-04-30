package com.fenghuo.seaweather.utils;

import android.os.Handler;
import android.util.Log;

import com.fenghuo.seaweather.bean.Information;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhangxin on 2016/8/25 0025.
 * <p>
 * Description :字符串相关的操作工具
 * FirstString:从usb串口数到的数据
 * SecondString:从FS中将外层slip包取掉后的数据
 * ThirdString:从SS中将需要解析的数据拿出来.
 */
public class StrUtil {

    public static final int FH_MSG = 1;
    public static final int FH_BUSSNIS_MSG = 2;
    public static final int FH_WEATHER_MSG = 3;
    public static final int FH_TYPHOON = 4;

    private static final int PARAM_LEFT_GRP = 14;
    private static final int PARAM_RIGHT_GRP = 6;


    //根据不同的Activity设置不同的Handler
    public static Handler handler;

    /**
     * @param src 待转换的字节数组,eg:{0x1,0x56,0x0a}或者{0x01,0x56,0x0a}
     * @return 转换成的字符串, 上述传入的字节数组返回"01560a"
     */
    public static String bytesToHexString(byte[] src) {
        return bytesToHexString(src, src.length);
    }


    public static String[] freqNameInPref = {"FREQ0", "FREQ1", "FREQ2", "FREQ3", "FREQ4", "FREQ5", "FREQ6", "FREQ7",
            "FREQ8", "FREQ9"};

    /**
     * @param src 待转换的字节数组
     * @param len 只转换字节数组中的前len个字节
     * @return 转换成的字符串
     */
    public static String bytesToHexString(byte[] src, int len) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || len <= 0) {
            return null;
        }
        for (int i = 0; i < len; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }


    /**
     * TODO:该函数似乎没有被调用.
     * 把为字符串转化为字节数组,注:不是随意的字符串都可以转换的,必须是0~9,A~F组成的字符串
     *
     * @param hexString eg:"0a56a8"
     * @return byte[] {0x0a,0x56,0xa8}
     */
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        //TODO:收到的数据都是小写的,转换可能没有必要.可是一换全换.
        hexString = hexString.toUpperCase();
        if (hexString.length() % 2 == 1) {
            hexString = "0" + hexString;
        }
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }


    /** TODO:depresd
     * @param hexString
     * @param n
     * @return
     */
    /*public static byte[] hexStringToBytes(String hexString, int n) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        if (hexString.length() % 2 == 1) {
            hexString = "0" + hexString;
        }
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d1 = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d1[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        byte[] d2 = new byte[n - length];
        byte[] d = new byte[n];
        System.arraycopy(d2, 0, d, 0, d2.length);
        System.arraycopy(d1, 0, d, d2.length, d1.length);
        return d;
    }*/


    /**
     * 将字符转换为对应的字节,支持的字符仅仅包括"0123456789ABCDEF"
     *
     * @param c
     * @return
     */
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    /**
     * 将字符数组中的内容格式化为时间,注:收到的时间可能是包含秒的,但是本函数中不处理秒的部分,只将字符数组的前10位格式化为
     * yy-mm-dd hh:mm 格式
     *
     * @param c eg:char[] {'1','6','0','8','2','5','1','8','0','6','3','0'}
     * @return 时间的字符串 "16-08-25 18:06"
     */
    public static String formatTime(char[] c) {
        String date_text = "";
        for (int i = 0; i < c.length - 2; i++) {
            date_text = date_text + c[i];
            if (i % 2 == 1 && i < 5) {
                date_text = date_text + "-";
            }

            if (i == 5) {
                date_text = date_text + " ";
            }

            if (i == 7) {
                date_text = date_text + ":";
            }
        }
        return date_text;
    }


    /**
     * 弃用!!! 耦合性太强,无法与参数解析业务分解出来;
     * 从混乱不堪的数据中获取有效的数据
     *
     * @param firstString 混乱不堪的数据
     */
    public static void getValidDataFromFirstString(StringBuilder firstString) {
        int begin8383Index = 0;
        int end8383Index = 0;
        //TODO:需要确认--c0a00082818383
        //使用while,可能会出现里面包含多个回复消息的情况.
        while ((begin8383Index = firstString.indexOf("c0a00082818383")) > 0) {
            end8383Index = firstString.indexOf("0000c0c0a0", begin8383Index);
            // TODO: 2016/9/11 0011 硬编码,这里设置46是否合理? 先检测头在检测尾是否合理?
            if (end8383Index < 0 || end8383Index - begin8383Index > 46) {
                Log.e("###", "getValidDataFromFirstString: 未知的错误,first--end不正确");
                //TODO:暂时不解决不了.
                break;
            }
            String paramStr = firstString.substring(begin8383Index + PARAM_LEFT_GRP, end8383Index);
            parseParamDataOnly(paramStr);
            firstString.delete(begin8383Index, end8383Index + PARAM_RIGHT_GRP);
        }


        int begin8303Index = 0; // 8303包对应的起始位置
        int end8304Index = 0; // 8304包对应的起始位置
        while ((end8304Index = firstString.indexOf("c0a008828183040004c0")) > 0) {
            begin8303Index = firstString.indexOf("c0a008828183030003c0");
            if (begin8303Index < 0) {
                Log.e("###",
                        "已检测到8304包,但是没有检测到8303包,严重错误,那么丢弃当前8304包以及之前的所有数据!!!");
                firstString.delete(0, end8304Index + 20);
                break;
            }

            getRealDataFromSecondString(firstString.substring(begin8303Index + 20, end8304Index));
            firstString.delete(0, end8304Index + 20);
        }
    }

    /**
     * 从乱七八糟的数据中获取真实数据
     *
     * @param text 乱七八糟的数据
     * @return 真实的数据, 每一帧c0-c0,需要从中截取应用协议数据
     */
    public static StringBuilder getRealDataFromSecondString(String text) {
        StringBuilder data = new StringBuilder();
        if (text.startsWith("c0") && text.endsWith("c0")) {
            text = transferReplace(text, "dbdc", "c0");
            text = transferReplace(text, "dbdd", "db");

            while (text.length() > 0) {
                if (text.startsWith("c0a0")) {
                    if (text.indexOf("828184") == 6) {
                        // 判断是不是是真正的数据包!!!
                        int len = Integer.valueOf(text.substring(4, 6), 16);
                        if (text.charAt(len * 2 + 4 - 2) != 'c' && text.charAt(len * 2 + 4 - 1) != '0') {
                            Log.e("###", "错误的包格式,可能会导致死循环,丢弃该包");
                            text = "";
                            break;
                        }
                        data = data.append(text.substring(12, 12 + (len - 7) * 2));
                        text = text.substring((len + 2) * 2);
                    } else {
                        int len = Integer.valueOf(text.substring(4, 6), 16);
                        text = text.substring((len + 2) * 2);
                    }
                } else {
                    Log.e("###", "居然有数据不是以c0a0开头,严重错误!!!!");
                    Log.e("###", "data的已有长度" + data.length());
                    Log.e("###", "text的已有内容长度" + data.toString());
                    Log.e("###", "text的剩余长度" + text.length());
                    Log.e("###", "text的剩余内容" + text);
                    text = "";
                    break;
                }
            }

            if (data.length() > 0) {
                //parseAppDataFromThirdString(data);
                return data;
            }
        }
        return null;
    }

    /**
     * 从真实数据中解析接收机数据
     */
    public static void parseAppDataFromThirdString(StringBuilder data, ArrayList<String> hasReceivedBefore,
                                                   ConcurrentHashMap<String, Information> infoMap) {
        String time = "";
        String frameString = data.toString();
        // count代表总包数,index代表帧序号
        int count = -1;
        int index = -1;
        while (frameString.length() > 0) {
            int i, k;
            i = findSilpBagHead(frameString);
            k = findSilpBagTail(i, frameString);
            Log.d("###", i + "<-->" + k);
            if (i >= k) {
                Log.d("###", "i>=k");
                break;
            }
            if (i % 2 != 0) {
                Log.d("###", "i%2!=0");
                break;
            }
            if (k % 2 != 0) {
                Log.d("###", "k%2!=0");
                break;
            }

            // ss是一个数据帧.但是还未进行替换.
            String ss = frameString.substring(i, k + 2);
            ss = transferReplace(ss, "dbdc", "c0");
            ss = transferReplace(ss, "dbdd", "db");
            // ss这时是一个真正的数据帧.接下来处理这一帧.

            byte[] infoi = hexStringToBytes(ss);
            //类型信息(1),接收机ID(2),时间戳(6),总帧数(2),帧序号(2)
            byte[] tmp1 = new byte[13];
            try {
                System.arraycopy(infoi, 1, tmp1, 0, 13);
            } catch (Exception e) {
                Log.e("###", "3丢包了...");
                break;
            }

            // -17,把tmp2的crc也删去了,但是把校验和放在了ocrc中
            //只包含纯正的数据
            byte[] tmp2 = new byte[infoi.length - 17];
            try {
                System.arraycopy(infoi, 14, tmp2, 0, infoi.length - 17);
            } catch (Exception e) {
                Log.e("###", "4丢包了...");
                break;
            }

            byte[] oCrc = null;
            if (infoi.length >= 3) {
                oCrc = new byte[2];
                oCrc[0] = infoi[infoi.length - 3];
                oCrc[1] = infoi[infoi.length - 2];
            } else {
                Log.e("###", "4丢包了...");
                break;
            }

            if (bytesToHexString(tmp2) == null || bytesToHexString(tmp1) == null) {
                Log.d("###", "tmp2或者tmp1是null,直接丢弃");
                break;
            }

            // Log.d("w23",BytesUtil.bytesToHexString(tmp2));
            // 只有crc1和crc2的校验均正确,那么这一帧可用
            if (checkCRC(tmp1) && checkCRC(tmp2, oCrc)) {
                byte[] bTime = new byte[6];
                try {
                    System.arraycopy(infoi, 1, bTime, 0, 6);
                } catch (Exception e) {
                    Log.e("###", "5丢包了");
                    break;
                }

                time = parseTimeInMSG(bTime);
                count = Integer.valueOf(bytesToHexString(new byte[]{tmp1[6], tmp1[7]}), 16);
                index = Integer.valueOf(bytesToHexString(new byte[]{tmp1[8], tmp1[9]}), 16) - 1;


                if (hasReceivedBefore.contains(time)) {
                    return;
                }
                if (infoMap.containsKey(time)) {
                    if (infoMap.get(time).bflag[index] == '0') {
                        infoMap.get(time).list.set(index, bytesToHexString(tmp2));
                        infoMap.get(time).start = System.currentTimeMillis();
                        infoMap.get(time).setFlag(index);
                        Log.e("tmp", "未收集的包:" + index);
                    }
                } else {
                    Log.d("tmp", "新的消息,需要加入到map中,新创建一个info对象,time和count是" + time + "--" + count);
                    Information info = new Information(time, count);
                    info.start = System.currentTimeMillis();
                    info.list.set(index, bytesToHexString(tmp2));
                    if (count <= 2) {
                        info.waitSeconds = 3 * count * 8 + 20;
                    } else {
                        info.waitSeconds = 3 * count * 2 + 20;
                    }

                    info.setFlag(index);
                    infoMap.put(time, info);
                }
            }
            frameString = frameString.substring(k + 2);
        }
    }

    /**
     * 从真实数据中解析数传数据
     */
   /* public static void parseAppData(String timeStamp, byte[] b) {

    }*/


    /**
     * 用于开机启动应用时与接收机之间的交互
     *
     * @param src 接收机发来的响应码
     */
    public static void parseParamDataOnly(String src) {
        if (src.startsWith("02") && src.endsWith("03")) {
            switch (src) {
                case "0273313103": //sdr正常

                    break;
                case "0273313003": //sdr异常

                    break;
                case "020603":  //ack

                    break;
                case "021503": //nak

                    break;

            }
        } else {
            //handler 发送坏消息.
        }
    }


    /**
     * 字符串替换功能
     *
     * @param src     字符串
     * @param oldChar
     * @param newChar
     * @return 替换后的字符串
     */
    public static String transferReplace(String src, String oldChar,
                                         String newChar) {
        StringBuilder src0 = new StringBuilder();
        src0.append(src);
        return transferReplace(src0, oldChar, newChar);
    }

    /**
     * @param src
     * @param oldChar
     * @param newChar
     * @return
     */
    public static String transferReplace(StringBuilder src, String oldChar,
                                         String newChar) {
        int index = src.indexOf(oldChar);
        while (index >= 0) {
            if (index % 2 == 0) {
                src = src.replace(index, index + 4, newChar);
            }
            index = src.indexOf(oldChar, index + 2);
        }
        return src.toString();
    }


    /**
     * 找到slip包的头
     *
     * @param s slip包
     * @return 头索引
     */
    public static int findSilpBagHead(String s) {
        int begin = s.indexOf("c0");
        while (begin > 0 && begin % 2 != 0) {
            begin = s.indexOf("c0", begin + 2);
        }
        if (begin % 2 == 0 && s.length() - begin >= 2) {
            if (s.charAt(begin + 2) == 'c' && s.charAt(begin + 3) == '0') {
                begin = begin + 2;
            }
        }
        return begin;
    }


    /**
     * 找到slip包的索引
     *
     * @param begin 开始位置
     * @param s     slip包
     * @return 尾索引
     */
    public static int findSilpBagTail(int begin, String s) {
        int tail = s.indexOf("c0", begin + 2);
        while (tail > 0 && tail % 2 != 0) {
            tail = s.indexOf("c0", tail + 2);
        }
        return tail;
    }

    /**
     * crc校验
     *
     * @param bs
     * @param oCrc
     * @return
     */
    private static boolean checkCRC(byte[] bs, byte[] oCrc) {
        int crc = 0;
        char c;
        int len = bs.length;
        int j = 0;
        while (len-- != 0) {
            for (c = 0x80; c != 0; c /= 2) {
                if ((crc & 0x8000) != 0) {
                    crc *= 2;
                    crc ^= 0x1021;
                } else {
                    crc *= 2;
                }

                if ((bs[j] & c) != 0) {
                    crc ^= 0x1021;
                }
            }
            j++;
        }

        // String oCrc1 = BytesUtil.bytesToHexString(oCrc);
        // int oCrc1 = oCrc[0]*256+oCrc[1];
        int oCrc1 = ((oCrc[0] + 256) % 256) * 256 + (oCrc[1] + 256) % 256;
        if (crc < 0) {
            crc = (crc + Integer.MAX_VALUE + 1) % (Integer.MAX_VALUE + 1);
        }
        return (crc % 65536) == (oCrc1);
    }

    /**
     * @param bs
     * @return
     */
    private static boolean checkCRC(byte[] bs) {
        int crc = 0;
        char c;
        int len = bs.length - 2;
        int j = 0;
        while (len-- != 0) {
            for (c = 0x80; c != 0; c /= 2) {
                if ((crc & 0x8000) != 0) {
                    crc *= 2;
                    crc ^= 0x1021;
                } else {
                    crc *= 2;
                }

                if ((bs[j] & c) != 0) {
                    crc ^= 0x1021;
                }
            }
            j++;
        }

        byte[] bb = new byte[2];// 用来存放传入的字节数组的原始校验和
        bb[0] = bs[bs.length - 2];
        bb[1] = bs[bs.length - 1];
        // int oCrc = bb[0]*256+bb[1];
        int oCrc = ((bb[0] + 256) % 256) * 256 + (bb[1] + 256) % 256;
        if (crc < 0) {
            crc = (crc + Integer.MAX_VALUE + 1) % (Integer.MAX_VALUE + 1);
        }
        return (crc % 65536) == (oCrc);
    }

    public static String parseTimeInMSG(byte[] b) {
        int year = b[0];
        int month = b[1];
        int day = b[2];
        int hour = b[3];
        int minus = b[4];
        int second = b[5];

        return String.format("%02d%02d%02d%02d%02d%02d", year, month, day, hour, minus, second);
    }

    public static int getIntergerLength(int i) {
        int res = 1;
        while (i != 0) {
            i /= 10;
            res *= 10;
        }
        return res;
    }
}

