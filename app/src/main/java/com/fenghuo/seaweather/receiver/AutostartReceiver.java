package com.fenghuo.seaweather.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.fenghuo.seaweather.ui.FirstActivity;

/**
 * Created by zhangxin on 2016/8/25 0025.
 * <p/>
 * Description :
 * 监听开机自启动广播,如果收到系统开启广播,自动打开FirstActivity
 */
public class AutostartReceiver extends BroadcastReceiver {

    private static final String AUTO_START_ACTION = "android.intent.action.BOOT_COMPLETED";

    public AutostartReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(AUTO_START_ACTION)){
            Intent autoStartIntent = new Intent(context, FirstActivity.class);
            context.startActivity(autoStartIntent);
        }
    }
}
