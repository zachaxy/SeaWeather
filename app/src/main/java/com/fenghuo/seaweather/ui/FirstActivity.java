package com.fenghuo.seaweather.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.fenghuo.seaweather.FHApplication;
import com.fenghuo.seaweather.R;
import com.fenghuo.seaweather.utils.FHProtocolUtil;
import com.fenghuo.seaweather.utils.StrUtil;
import com.sdsmdg.tastytoast.TastyToast;
import com.zhl.cbdialog.CBDialogBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhangxin on 2016/8/25 0025.
 * <p>
 * Description :
 * 应用的开启界面,在这个界面初始化usb连接,设置接收机的一些参数以及本应用程序所需的一些初始化操作
 * <p>
 * TODO:这里信道有10个,程序中是从0~9,但是界面显示上是1-10,注意对应关系!!!
 */
public class FirstActivity extends Activity {

    final String TAG = FirstActivity.class.getSimpleName();

    //自定义广播的action,标识用户是否点击了连接usb选项
    final String ACTION_USB_PERMISSION = "com.android.fh.USB_PERMISSION";

    //为handler设置的选项
    final int FH_USB_OK = 1;
    final int FH_USB_NOK = 2;

    final int FH_SDR_ACK = 3;
    final int FH_SDR_NAK = 4;
    final int FH_SDR_NO_RESPONSE = 5;

    final int FH_FREQ_ACK = 6;
    final int FH_FREQ_NAK = 7;
    final int FH_FREQ_NO_RESPONSE = 8;

    final int FH_UNKNOWN_MSG = 9;


    private TextView tv2, tv3, tv4, tv5;
    private ImageView img2, img3, img4;

    private int currentFreqIndex = 0;

    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;


    public FHApplication mFHApplication;

    private SharedPreferences mPref;

    //用来接收usb发来的消息.
    private StringBuilder receiveParamAckBuilder = new StringBuilder();


    Handler h1 = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case FH_USB_OK:
                    TastyToast.makeText(FirstActivity.this, "USB设备已连接!", TastyToast.LENGTH_LONG, TastyToast.SUCCESS);
                    tv2.setText("USB通信串口已连接");
                    img2.setImageResource(R.drawable.ok);
                    querySDR();
                    break;
                case FH_USB_NOK:
                    TastyToast.makeText(FirstActivity.this, msg.obj.toString(), TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    destroySource();
                    finish();
                    break;
                case FH_SDR_ACK:
                    TastyToast.makeText(FirstActivity.this, "SDR回复正常", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    tv3.setText("SDR回复正常");
                    img3.setImageResource(R.drawable.ok);
                    tv4.setText("设置接收机频率:");
                    sendFreqs(0);
                    break;
                case FH_SDR_NAK:
                    new CBDialogBuilder(FirstActivity.this)
                            .setTouchOutSideCancelable(false)
                            .showCancelButton(true)
                            .setTitle("SDR回复异常")
                            .setMessage("是否重新查询或者退出程序?")
                            .setConfirmButtonText("重新查询")
                            .setCancelButtonText("退出程序")
                            .setDialogAnimation(CBDialogBuilder.DIALOG_ANIM_SLID_BOTTOM)
                            .setButtonClickListener(true, new CBDialogBuilder.onDialogbtnClickListener() {
                                @Override
                                public void onDialogbtnClick(Context context, Dialog dialog, int whichBtn) {
                                    switch (whichBtn) {
                                        case BUTTON_CONFIRM:
                                            querySDR();
                                            break;
                                        case BUTTON_CANCEL:
                                            destroySource();
                                            finish();
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            })
                            .create().show();
                    break;
                case FH_SDR_NO_RESPONSE:
                    new CBDialogBuilder(FirstActivity.this)
                            .setTouchOutSideCancelable(false)
                            .showCancelButton(true)
                            .setTitle("SDR无响应")
                            .setMessage("是否重新查询或者退出程序?")
                            .setConfirmButtonText("重新查询")
                            .setCancelButtonText("退出程序")
                            .setDialogAnimation(CBDialogBuilder.DIALOG_ANIM_SLID_BOTTOM)
                            .setButtonClickListener(true, new CBDialogBuilder.onDialogbtnClickListener() {
                                @Override
                                public void onDialogbtnClick(Context context, Dialog dialog, int whichBtn) {
                                    switch (whichBtn) {
                                        case BUTTON_CONFIRM:
                                            querySDR();
                                            break;
                                        case BUTTON_CANCEL:
                                            destroySource();
                                            finish();
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            })
                            .create().show();
                    break;
                case FH_FREQ_ACK:
                    tv5.setText(String.valueOf((Integer) msg.obj) + "/10");
                    //img4.setImageResource(R.drawable.ok);
                    if (currentFreqIndex == 10) {
                        Intent intent = new Intent(FirstActivity.this, HomeActivity.class);
                        startActivity(intent);
                        destroySource(); //这时可以将Activity destroy
                        finish();
                    }
                    break;
                case FH_FREQ_NAK:
                    new CBDialogBuilder(FirstActivity.this)
                            .setTouchOutSideCancelable(false)
                            .showCancelButton(true)
                            .setTitle("信道" + msg.obj + "设置异常")
                            .setMessage("信道设置异常,您可以选择继续设置接收机频率,或者取消设置,直接进入程序,在程序的参数设置界面仍然可以设置频率!")
                            .setConfirmButtonText("再次设置")
                            .setCancelButtonText("取消设置")
                            .setDialogAnimation(CBDialogBuilder.DIALOG_ANIM_SLID_BOTTOM)
                            .setButtonClickListener(true, new CBDialogBuilder.onDialogbtnClickListener() {
                                @Override
                                public void onDialogbtnClick(Context context, Dialog dialog, int whichBtn) {
                                    switch (whichBtn) {
                                        case BUTTON_CONFIRM:
                                            sendFreqs((Integer) msg.obj);
                                            break;
                                        case BUTTON_CANCEL:
                                            Intent intent = new Intent(FirstActivity.this, HomeActivity.class);
                                            startActivity(intent);
                                            destroySource();
                                            finish();
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            })
                            .create().show();
                    break;
                case FH_FREQ_NO_RESPONSE:
                    new CBDialogBuilder(FirstActivity.this)
                            .setTouchOutSideCancelable(false)
                            .showCancelButton(true)
                            .setTitle("信道" + msg.obj + "设置异常")
                            .setMessage("是否继续查询或者退出程序?")
                            .setConfirmButtonText("再次设置")
                            .setCancelButtonText("退出程序")
                            .setDialogAnimation(CBDialogBuilder.DIALOG_ANIM_SLID_BOTTOM)
                            .setButtonClickListener(true, new CBDialogBuilder.onDialogbtnClickListener() {
                                @Override
                                public void onDialogbtnClick(Context context, Dialog dialog, int whichBtn) {
                                    switch (whichBtn) {
                                        case BUTTON_CONFIRM:
                                            sendFreqs((Integer) msg.obj);
                                            break;
                                        case BUTTON_CANCEL:
                                            destroySource();
                                            finish();
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            })
                            .create().show();
                    break;
                case FH_UNKNOWN_MSG:
                    TastyToast.makeText(FirstActivity.this, "未知的参数回复!", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    break;
            }
        }
    };


    BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    //用户允许连接usb;
                    connection = usbManager.openDevice(device);
                    //启动usb连接线程
                    new ConnectionThread().start();
                } else {
                    TastyToast.makeText(FirstActivity.this, "用户拒绝连接USB设备!\n请重新连接USB设备并重启本程序!", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    finish();
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);

        initViews();
        // TODO: 2016/10/18 0018,用于临时测试,直接跳转到主界面
        startActivity(new Intent(FirstActivity.this,HomeActivity.class));

        mPref = getSharedPreferences("fenghuo", Context.MODE_PRIVATE);
        mFHApplication = (FHApplication) getApplication();
        //注册usb广播
        setFilter();

        //查找设备
        findSerialPortDevice();

    }

    private void setFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);
    }

    //设置usb读取数据解析的方法.这里只解析接收机ack/nack,该函数不在主线程中,更新UI要注意!!!
    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            //TODO:解析接收机的参数.其协议两头包围的头还为确定!!!判定截取到的是否是偶数位;
            String data = StrUtil.bytesToHexString(arg0);
            receiveParamAckBuilder.append(data);
            int begin8383Index = 0;
            int end8383Index = 0;
            while ((begin8383Index = receiveParamAckBuilder.indexOf("c0b0b1b2")) > 0) {
                end8383Index = receiveParamAckBuilder.indexOf("c0", begin8383Index + FHApplication.PARAM_PREFIX);
                if (end8383Index % 2 == 1) {
                    continue;
                }
                String paramStr = receiveParamAckBuilder.substring(begin8383Index + FHApplication.PARAM_PREFIX, end8383Index);
                // TODO: 2016/9/25 0025 将来考虑把解析的函数放在另一个线程中,该线程中使用一个阻塞队列<String>;
                parseParamData(paramStr);
                receiveParamAckBuilder.delete(0, end8383Index + 2); //TODO:结尾只有个c0,容易出问题
            }

        }
    };

    private void parseParamData(String src) {

        if (src.startsWith("02") && src.endsWith("03")) {
            switch (src) {
                case "0273313103": //sdr正常
                    h1.removeMessages(FH_SDR_NO_RESPONSE);
                    h1.obtainMessage(FH_SDR_ACK).sendToTarget();
                    break;
                case "0273313003": //sdr异常
                    h1.removeMessages(FH_SDR_NO_RESPONSE);
                    h1.obtainMessage(FH_SDR_NAK).sendToTarget();
                    break;
                case "020603":  //ack
                    h1.removeMessages(FH_FREQ_NO_RESPONSE);
                    currentFreqIndex++;
                    h1.obtainMessage(FH_FREQ_ACK, currentFreqIndex).sendToTarget();
                    sendFreqs(currentFreqIndex);
                    break;
                case "021503": //nak
                    h1.removeMessages(FH_FREQ_NO_RESPONSE);
                    h1.obtainMessage(FH_FREQ_NAK, currentFreqIndex).sendToTarget();
                    break;

            }
        } else {
            //handler:未知的异常回复.
            h1.obtainMessage(FH_UNKNOWN_MSG).sendToTarget();
            Log.e(TAG, "parseParamData: " + src);
        }

    }


    void initViews() {
        TextView tv1;
        ImageView img1;
        tv1 = (TextView) findViewById(R.id.tv1);
        tv2 = (TextView) findViewById(R.id.tv2);
        tv3 = (TextView) findViewById(R.id.tv3);
        tv4 = (TextView) findViewById(R.id.tv4);
        tv5 = (TextView) findViewById(R.id.tv5);

        img1 = (ImageView) findViewById(R.id.img1);
        img2 = (ImageView) findViewById(R.id.img2);
        img3 = (ImageView) findViewById(R.id.img3);
        img4 = (ImageView) findViewById(R.id.img4);

        tv1.setText("初始化配置文件完成");
        img1.setImageResource(R.drawable.ok);
    }


    private void findSerialPortDevice() {
        //初始化usb管理器
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        //该代码将尝试打开连接遇到的第一个USB设备.但是如果连接了一个hub,那么程序目前无法处理.
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();

                //if条件中满足的情况是一个usb hub,不做处理
                if (deviceVID != 0x1d6b && (devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003)) {
                    // There is a device connected to our Android device. Try to open it as a Serial Port.
                    requestUserPermission();
                    break;
                } else {
                    TastyToast.makeText(this, "不能识别的USB设备!\n请检查设别连接并重新启动本程序!", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    finish();
                }
            }
        } else {
            //此时并没有发现usb设备
            TastyToast.makeText(this, "未检测到USB设备!\n请检查设别连接并重新启动本程序!", TastyToast.LENGTH_LONG, TastyToast.ERROR);
            finish();
        }
    }

    private void requestUserPermission() {
        //预intent,在接下来展示的是否连接usb的选项中,无论选择是/否,都会发送一个ACTION_USB_PERMISSION的广播,接下来在广播接收函数中再判断是否已连接usb;
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        //会弹出确认连接的对话框
        usbManager.requestPermission(device, mPendingIntent);
    }

    private class ConnectionThread extends Thread {

        @Override
        public void run() {
            //得到一个具体的子类 串口设备子类这里是CP2102SerialDevice的对象.
            mFHApplication.serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if (mFHApplication.serialPort != null) {
                if (mFHApplication.serialPort.open()) {
                    mFHApplication.serialPort.setBaudRate(mFHApplication.BAUD_RATE);
                    mFHApplication.serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    mFHApplication.serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    mFHApplication.serialPort.setParity(UsbSerialInterface.PARITY_NONE);

                    mFHApplication.serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    //设置USB的读回调函数
                    mFHApplication.serialPort.read(mCallback);
                    //TODO:这两个函数不设置行不行?
                    //mFHApplication.serialPort.getCTS(ctsCallback);
                    //mFHApplication.serialPort.getDSR(dsrCallback);
                    h1.obtainMessage(FH_USB_OK).sendToTarget();
                } else {
                    //串口无法打开,可能发生了IO错误或者被认为是CDC,但是并不适合本应用,不做处理.
                    h1.obtainMessage(FH_USB_NOK, "USB设备IO错误,请重新连接!!!").sendToTarget();
                }
            } else {
                //设备无法打开,即便是CDC的驱动也无法加载,不作处理.
                h1.obtainMessage(FH_USB_NOK, "USB设备无法打开,请重新连接!!!").sendToTarget();
            }
        }
    }

    private void querySDR() {
        byte[] data = StrUtil.hexStringToBytes("02533203");
        if (mFHApplication.serialPort != null) {
            mFHApplication.serialPort.write(data);
            h1.sendEmptyMessageDelayed(FH_SDR_NO_RESPONSE, 2000);
        } else {
            Log.e(TAG, "querySDR:当前mFHApplication.serialPort为null");
        }

    }

    //发送频率
    void sendFreqs(int index) {
        String freq = mPref.getString(StrUtil.freqNameInPref[index], "10.0000");
        mFHApplication.mChannels.set(index, freq);
        byte[] b = FHProtocolUtil.parseChannels(freq, index);
        if (mFHApplication.serialPort != null) {
            mFHApplication.serialPort.write(b);
            h1.sendEmptyMessageDelayed(FH_FREQ_NO_RESPONSE, 3000);
        }
    }


    // TODO: 2016/9/25 0025 典型的错误:在onDestroy中关闭接口,但是在下一个Acticity中还要复用接口,严重bug!!!
    //关闭该Activity,销毁一切打开的资源,包括USB串口连接,如果进入到第二个页面,重新打开.
   /* @Override
    protected void onDestroy() {
        //而onDestroy方法是在下一个Acticity的onResume之后才执行的,所以将该逻辑该在跳转界面或者onPause/onStop中.
        unregisterReceiver(usbReceiver);
        super.onDestroy();
    }*/


    /***
     * 跳过生命周期的管理;
     */
    void destroySource() {
        unregisterReceiver(usbReceiver);
        mFHApplication.serialPort.close();
    }

    // TODO: 2016/9/9 0009 不知道弹框后是否给正在发送消息的信道产生什么影响,所以此处直接退出程序;
    //会执行onDestroy方法;
    @Override
    public void onBackPressed() {
        destroySource();
        super.onBackPressed();
    }
}
