package com.fenghuo.seaweather;

import android.app.Application;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.fenghuo.seaweather.ui.HomeActivity;
import com.fenghuo.seaweather.utils.EncryptUtil;
import com.zxy.recovery.callback.RecoveryCallback;
import com.zxy.recovery.core.Recovery;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangxin on 2016/8/25 0025.
 * <p>
 * Description :整个App的Application类,封装了该应用中的所有全局变量
 */
public class FHApplication extends Application {
    private final static String TAG = "FHApplication";

    public final static int PARAM_PREFIX = 8; //接收的参数的前缀 c0b0b1b2
    //线程的开关,当整个应用退出时,将开关设置为false,来关闭全部的
    public boolean threadFlag1;
    public boolean threadFlag2;
    public boolean threadFlag3;

    public final int BAUD_RATE = 115200;

    //设置各种ack,其取值为(-1,0,1,2)对应[未准备状态,准备状态(一直是0那么表示无响应),ack,nack]
    public int channelAck = -1;
    public int offsetAck = -1;
    public int soundAck = -1;
    public int soundsAck = -1;

    // 10个信道
    public List<String> mChannels = new ArrayList<>();
    public final int mChannelCount = 10;

    //不得不设置的全局变量
    public String date;  //当前用户的有效期,(解密&未格式化)
    public int ID;   //当前用户ID
    public int unlinkTime;
    private int weather_type = -1;  //天气类型,有24小时;48小时;台风三种类型的切换;

    public int getWeather_type() {
        return weather_type;
    }

    public void setWeather_type(int weather_type) {
        this.weather_type = weather_type;
    }

    //*串口的接口,在两个Activity中都有用到,需要设置不同的回调函数.不得不设置为全局变量;
    public UsbSerialDevice serialPort;

    @Override
    public void onCreate() {
        super.onCreate();
        // TODO: 2016/9/25 0025 crash处理机制,暂时启用;正式版本自动恢复?取消?必须修改crash框架!!!
        Recovery.getInstance()
                .debug(true)
                .recoverInBackground(false)
                .recoverStack(true)
                .mainPage(HomeActivity.class)
                .callback(new MyCrashCallback())
                .silent(false, Recovery.SilentMode.RECOVER_ACTIVITY_STACK)
                .init(this);
        
        threadFlag1 = true;
        threadFlag2 = true;
        threadFlag3 = true;
        SharedPreferences mPref = getSharedPreferences("fenghuo", MODE_PRIVATE);
        String date1 = mPref.getString("DATE", null);
        if (TextUtils.isEmpty(date1)) {
            date = "000000000000";
        } else {
            date = EncryptUtil.decrypt(date1);
        }

        // TODO: 2016/9/10 0010 读取用户ID,如果为null,弹出ID设置对话框?
        ID = mPref.getInt("ID", 100);
        unlinkTime = mPref.getInt("UNLINKTIME", 60);

        //现在此处初始化为10,在FirstActivity中设置频率的时候,再读取配置并替换
        for (int i = 0; i < mChannelCount; i++) {
            mChannels.add(i, "10.0000");
        }
    }

    //crash回调
    static final class MyCrashCallback implements RecoveryCallback {
        @Override
        public void stackTrace(String exceptionMessage) {
            Log.e(TAG, "exceptionMessage:" + exceptionMessage);
        }

        @Override
        public void cause(String cause) {
            Log.e(TAG, "cause:" + cause);
        }

        @Override
        public void exception(String exceptionType, String throwClassName, String throwMethodName, int throwLineNumber) {
            Log.e(TAG, "exceptionClassName:" + exceptionType);
            Log.e(TAG, "throwClassName:" + throwClassName);
            Log.e(TAG, "throwMethodName:" + throwMethodName);
            Log.e(TAG, "throwLineNumber:" + throwLineNumber);
        }
    }
}
