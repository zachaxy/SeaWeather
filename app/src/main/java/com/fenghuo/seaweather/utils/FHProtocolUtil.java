package com.fenghuo.seaweather.utils;

import android.util.Log;

/**
 * Created by zhangxin on 2016/9/11 0011.
 * <p>
 * Description :
 */
public class FHProtocolUtil {
    private static String parseChannel2Hex(String s) {
        String hexChannel = Integer.toHexString(s.charAt(0))
                + Integer.toHexString(s.charAt(1))
                + Integer.toHexString(s.charAt(3))
                + Integer.toHexString(s.charAt(4))
                + Integer.toHexString(s.charAt(5))
                + Integer.toHexString(s.charAt(6));
        return hexChannel;
    }

    public static byte[] parseChannels(String s, int index) {
        String hexChannel;
        hexChannel = parseChannel2Hex(s);
        String ss = "02534B30303" + index + hexChannel + "30" + hexChannel
                + "303003";
        Log.d("信道频率参数", "第" + index + "信道发送的内容: " + ss);
        return StrUtil.hexStringToBytes(ss);
    }


    /**
     * 格式化频率值字符串
     *
     * @param left  整数部分
     * @param right 小数部分
     * @return
     */
    public static String formatChannel(String left, String right) {
        if (left.length() == 1) {
            left = "0" + left;
        }

        if (right.length() == 1) {
            right = right + "000";
        } else if (right.length() == 2) {
            right = right + "00";
        } else if (right.length() == 3) {
            right = right + "0";
        }

        return left + "." + right;

    }

    public static byte[] parseOffset(String s) {
        String hexOffset = Integer.toHexString(s.charAt(0))
                + Integer.toHexString(s.charAt(1))
                + Integer.toHexString(s.charAt(2));
        String ss = "025336" + hexOffset + "03";
        return StrUtil.hexStringToBytes(ss);
    }


    /***
     * 解析要发送的静噪参数
     *
     * @param isOn 声音开/关. 静噪开:是静音!!!!!!!
     */
    public static byte[] parseSound(boolean isOn) {
        String hexSound;
        if (isOn) {
            hexSound = "30";
        } else {
            hexSound = "31";
        }
        String ss = "025344" + hexSound + "3003";
        return StrUtil.hexStringToBytes(ss);
    }


    /***
     * 解析要发送的音量参数
     *
     * @param i 音量
     * @return
     */
    public static byte[] parseSounds(int i) {
        String s;
        if (i < 10) {
            s = "0" + i;
        } else {
            s = "" + i;
        }
        String hexSounds = Integer.toHexString(s.charAt(0)) + Integer.toHexString(s.charAt(1));
        String ss = "025335" + hexSounds + "03";
        return StrUtil.hexStringToBytes(ss);
    }
}
