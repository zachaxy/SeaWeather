package com.fenghuo.seaweather.utils;

import android.util.Log;

import com.fenghuo.seaweather.bean.Information;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by zhangxin on 2016/8/30 0030.
 * <p>
 * Description :
 * 这个类要在主Activity中单独开启,需要对该线程对象设置解析函数.
 */
public class ParseAppDataThread extends Thread {
    private ArrayList<String> hasReceivedBefore;
    private ConcurrentHashMap<String, Information> infoMap;
    private AppDataInterface mAppDataInterface;
    private AtomicBoolean working;

    public ParseAppDataThread(ArrayList<String> hasReceivedBefore, ConcurrentHashMap<String, Information> infoMap) {
        this.hasReceivedBefore = hasReceivedBefore;
        this.infoMap = infoMap;
        working = new AtomicBoolean(true);
    }

    public void stopParseThread() {
        working.set(false);
    }

    public void setAppDataInterface(AppDataInterface appDataInterface) {
        mAppDataInterface = appDataInterface;
    }

    private void onAppDataParseCallback(String timeStamp, byte[] b) {
        if (mAppDataInterface != null) {
            mAppDataInterface.onParseAppData(timeStamp, b);
        }
    }


    @Override
    public void run() {
        String key = "";
        Information out;
        while (working.get()) {
            if (!infoMap.isEmpty()) {
                // Log.d("###","进入线程同步的map检测");
                for (Map.Entry<String, Information> entry : infoMap
                        .entrySet()) {
                    String k = entry.getKey();
                    // Log.d("get", "获取到当前的key是:  " + k);
                    Information info = entry.getValue();
                    if (info.flag) {
                        Log.d("get", "flag里不包含0,表明已经信息已收集完整");
                        key = k;
                    } else if (info.n == 3) {
                        if (info.bflag[0] == '1') {
                            Log.d("get", "已经明确的发了三遍了,可是信息不完整,还好头消息还在");
                            key = k;
                        } else {
                            Log.d("get", "已经明确发了三遍了,但是头消息任然不完整,直接舍弃");
                            infoMap.remove(k);
                        }
                    } else if ((System.currentTimeMillis() - info.start) / 1000 >= info.waitSeconds) {
                        Log.d("get", "第三遍还未收到,但是已经超时"
                                + info.waitSeconds + "s");
                        Log.d("get", "此时info的消息是: count->" + info.count
                                + "  发送次数->" + info.n + "  消息列表中的数量->"
                                + info.list.size() + "  发送的标志->"
                                + new String(info.bflag));
                        if (info.bflag[0] == '1') {
                            Log.d("get", "已经明确的发了三遍了,可是信息不完整,还好头消息还在");
                            key = k;
                        } else {
                            Log.d("get", "已经明确发了三遍了,但是头消息任然不完整,直接舍弃");
                            infoMap.remove(k);
                        }
                    }
                    // TODO:每次解决完一个消息就处理一下.
                    if (key != "") {
                        out = infoMap.remove(key);
                        Log.d("get获取到的key是", "run: " + key);
                        StringBuilder sb = new StringBuilder();
                                /*
                                 * for (String s : out.list) { sb.append(s); }
								 */
                        for (int i = 0; i < out.bflag.length; i++) {
                            if (out.bflag[i] == '1') {
                                sb.append(out.list.get(i));
                            } else {
                                sb.append("28cffbcfa2d2d1c6c6cbf029");// (信息已破损)
                                break;
                            }
                        }
                        Log.d("get获取到的内容是", sb.toString());
                        byte[] realData = StrUtil.hexStringToBytes(sb.toString());
                        Log.e("###", "添加进已接收列表的时间戳是:" + key);
                        hasReceivedBefore.add(key);
                        if (hasReceivedBefore.size() >= 10) {
                            hasReceivedBefore.remove(0);
                        }
                        onAppDataParseCallback(key, realData);
                        key = "";
                    }// if(key!="")
                }// for(infoMap)
            }// if(infoMap!=null)
        }
    }
}

