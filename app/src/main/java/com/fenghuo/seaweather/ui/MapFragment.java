package com.fenghuo.seaweather.ui;

import android.app.Dialog;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.felhr.usbserial.UsbSerialInterface;
import com.fenghuo.seaweather.FHApplication;
import com.fenghuo.seaweather.R;
import com.fenghuo.seaweather.bean.Information;
import com.fenghuo.seaweather.bean.Msg;
import com.fenghuo.seaweather.bean.WeatherMsg;
import com.fenghuo.seaweather.dao.MsgDao;
import com.fenghuo.seaweather.utils.AppDataInterface;
import com.fenghuo.seaweather.utils.EncryptUtil;
import com.fenghuo.seaweather.utils.FHProtocolUtil;
import com.fenghuo.seaweather.utils.ParseAppDataThread;
import com.fenghuo.seaweather.utils.ParseUtil;
import com.fenghuo.seaweather.utils.StrUtil;
import com.fenghuo.seaweather.widget.FreqDialog;
import com.fenghuo.seaweather.widget.MarqueenTextView;
import com.fenghuo.seaweather.widget.ZoomImageView;
import com.sdsmdg.tastytoast.TastyToast;
import com.zhl.cbdialog.CBDialogBuilder;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhangxin on 2016/8/26 0026.
 * <p>
 * Description :
 * 应用程序的第一屏,主要显示地图界面
 */
public class MapFragment extends Fragment implements AppDataInterface {

    //给Handler设置的消息类型.
    private final int FH_CHANNEL_NO = 1;  //信道号
    private final int FH_CHANNEL_BI = 2;  //信噪比
    private final int FH_CHANNEL_RATE = 3;  //信道速率
    private final int FH_CHANNEL_SCAN = 4;   //显示扫描中...

    private final int FH_SOUND_ACK = 5;     //静噪设置
    private final int FH_SOUND_NACK = 6;

    private final int FH_SOUNDS_ACK = 7;   //音量设置
    private final int FH_SOUNDS_NACK = 8;

    private final int FH_MAP_DRAW = 9;    //刷新地图;

    private final int FH_MSG_READ = 10;    //右边栏信息已读
    private final int FH_MSG_READ_FAILED = 11;    //向右边栏信息已读状态更新失败

    private final int FH_MSG_DELETE = 12;  //删除消息,必须有个延时,否则侧滑菜单不能完全收缩
    private final int FH_MSG_DELETE_FAILED = 13;  //删除失败...

    private final int FH_MSG_ADD = 14;  //新消息添加进数据库
    private final int FH_MSG_ADD_FAILED = 15;  //新消息添加数据库失败...

    private final int FH_UPDATE_DATE = 16;  //用户注册或者续费;
    private final int FH_UPDATE_DATE_FAILD = 17;  //用户注册或者续费;

    private final int FH_UNKNOWN_ACK = 21;
    private final int FH_NO_RESPONSE = 22;

    //********view相关
    private View mapLayout;

    private ZoomImageView zoomImageView;

    //四个按钮,分别显示24小时天气,48小时天气,台风,删除台风轨迹;
    private ImageView weather24, weather48, typhoon_show, typhoon_delete;

    private ImageButton sound;
    boolean isSoundOpen = true;  //默认值

    private ImageButton sounds;
    int soundsCount = 18;   //默认值

    private TextView date;

    private TextView cRate;
    private TextView cNo;
    private TextView cBi;

    private MarqueenTextView mNewMsg;

    private SwipeMenuListView mMsgLv;
    private List<Msg> msgList; //其初始化在进来加载了数据库后
    private MsgAdapter adapter;

    private TextToSpeech tts;


    //******配置相关
    private SharedPreferences mPref;
    private FHApplication fhApplication1;
    private ParseAppDataThread t1;  //一个专门用来解析应用协议的线程;
    private MsgDao dao; //用于操作数据库


    //*************工具相关
    public static List<WeatherMsg> weather_msg_list;
    public static List<String> typhoon_msg_list;

    // TODO: 2016/9/13 0013 天气图标是弄一个还是弄两个?一个吧

    private LruCache<String, Bitmap> mMemoryCache;//缓存图标类,避免内存溢出
    private static final int cacheSize = 4 * 1024 * 1024;//缓存设置为4M吧
    //public static List<Bitmap> weather_imgs = new ArrayList<Bitmap>();
    public static Bitmap[] weather_imgs = new Bitmap[18 * 2];   //当前可以显示的图标集合

    private ArrayList<String> hasReceivedBefore = new ArrayList<>();//标志之前是否收到相同的时间戳,应对发三遍的机制;
    //private ArrayList<Msg> MSGList = new ArrayList<>();
    private ConcurrentHashMap<String, Information> infoMap = new ConcurrentHashMap<>(); // 同步;用来收集解析的数据map

    private StringBuilder receiveFirstBuilder = new StringBuilder();

    //代表右侧接收的一个新的msg;只要该msg一实例化,那么其时间就是格式化好的时间,往数据库中存储也是格式化好的
    private Msg msg;

    private Handler h1 = new Handler() {
        @Override
        public void handleMessage(Message message) {
            //TODO:处理各种事件.
            switch (message.what) {
                case FH_CHANNEL_SCAN:
                    cRate.setText("");
                    cBi.setText("");
                    cNo.setText("扫描中");
                    break;
                case FH_CHANNEL_NO:
                    cNo.setText((String) message.obj);
                    break;
                case FH_CHANNEL_BI:
                    cBi.setText((String) message.obj);
                    break;
                case FH_CHANNEL_RATE:
                    cRate.setText((String) message.obj);
                    break;
                case FH_SOUND_ACK:
                    if (isSoundOpen) {
                        sound.setImageResource(R.drawable.sound_on);
                    } else {
                        sound.setImageResource(R.drawable.sound_off);
                    }
                    break;
                case FH_SOUND_NACK:
                    TastyToast.makeText(getActivity(), "静噪设置不成功,请稍后设置", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    break;
                case FH_SOUNDS_ACK:
                    TastyToast.makeText(getActivity(), "音量设置成功^_^", TastyToast.LENGTH_LONG, TastyToast.SUCCESS);
                    break;
                case FH_SOUNDS_NACK:
                    TastyToast.makeText(getActivity(), "音量设置不成功,请稍后设置", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    break;
                case FH_MSG_READ:
                    adapter.updateSingleView(message.arg1);
                    break;
                case FH_MSG_READ_FAILED:
                    TastyToast.makeText(getActivity(), "消息已读状态更新失败,但不影响正常使用,请稍后再试", TastyToast.LENGTH_LONG, TastyToast.INFO);
                    break;
                case FH_MSG_DELETE:
                    msgList.remove(message.arg1);
                    adapter.notifyDataSetChanged();
                    break;
                case FH_MSG_DELETE_FAILED:
                    TastyToast.makeText(getActivity(), "删除失败,请稍后尝试^_^", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    break;
                case FH_MSG_ADD:
                    addToMSGList(msg);
                    String text2Speak = msg.mMsgContent;
                    if (text2Speak.length() >= 200) {
                        text2Speak = text2Speak.substring(0, 200);
                    }
                    mNewMsg.setText(text2Speak);
                    tts.speak(text2Speak, TextToSpeech.QUEUE_FLUSH, null);
                    break;
                case FH_MSG_ADD_FAILED:
                    TastyToast.makeText(getActivity(), "新消息添加数据库发生错误,请联系管理员!", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    break;
                case FH_MAP_DRAW:
                    zoomImageView.currentDisplayType = 0;
                    zoomImageView.invalidate();
                    break;
                case FH_UPDATE_DATE:
                    date.setText(StrUtil.formatTime(fhApplication1.date.toCharArray()));
                    break;
                case FH_UPDATE_DATE_FAILD:
                    TastyToast.makeText(getActivity(), "注册消息写入失败,请联系管理员!", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                case FH_NO_RESPONSE:
                    TastyToast.makeText(getActivity(), (String) message.obj + "设置无响应,请稍后设置", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    break;
                case FH_UNKNOWN_ACK:
                    TastyToast.makeText(getActivity(), "未知的ack回复,协议解析错误", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    break;

            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mapLayout = inflater.inflate(R.layout.map_layout, container, false);

        fhApplication1 = (FHApplication) getActivity().getApplication();
        mPref = getActivity().getSharedPreferences("fenghuo", Context.MODE_PRIVATE);

        initView();
        initConf();
        //单独的一个线程,专门用来解析协议.
        t1 = new ParseAppDataThread(hasReceivedBefore, infoMap);
        t1.setAppDataInterface(this);
        t1.start();
        return mapLayout;
    }

    //初始化界面部件
    void initView() {
        //从左到右;
        zoomImageView = (ZoomImageView) mapLayout.findViewById(R.id.zoomImg);

        weather24 = (ImageView) mapLayout.findViewById(R.id.weather24);
        weather48 = (ImageView) mapLayout.findViewById(R.id.weather48);
        typhoon_show = (ImageView) mapLayout.findViewById(R.id.typhoon);
        typhoon_delete = (ImageView) mapLayout.findViewById(R.id.delete_typhoon);


        //从上到下
        sound = (ImageButton) mapLayout.findViewById(R.id.sound_switch);
        sound.setImageResource(R.drawable.sound_on);

        sounds = (ImageButton) mapLayout.findViewById(R.id.sounds);
        sounds.setImageResource(R.drawable.sounds);

        date = (TextView) mapLayout.findViewById(R.id.date);
        date.setText(StrUtil.formatTime(fhApplication1.date.toCharArray()));

        mNewMsg = (MarqueenTextView) mapLayout.findViewById(R.id.new_msg);

        cRate = (TextView) mapLayout.findViewById(R.id.xindaosulv);
        cNo = (TextView) mapLayout.findViewById(R.id.xindaohao);
        cBi = (TextView) mapLayout.findViewById(R.id.xinzaobi);

        mMsgLv = (SwipeMenuListView) mapLayout.findViewById(R.id.lv_msg);

        //绑定相应点击事件
        weather24.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                typhoon_delete.setVisibility(View.INVISIBLE);
                zoomImageView.currentDisplayType = 0;
                zoomImageView.invalidate();
            }
        });

        weather48.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                typhoon_delete.setVisibility(View.INVISIBLE);
                zoomImageView.currentDisplayType = 1;
                zoomImageView.invalidate();
            }
        });

        typhoon_show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                typhoon_delete.setVisibility(View.VISIBLE);
                zoomImageView.currentDisplayType = 2;
                zoomImageView.invalidate();
            }
        });

        typhoon_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new CBDialogBuilder(getActivity())
                        .setCancelable(false)
                        .showCancelButton(true)
                        .setTitle("清空台风轨迹")
                        .setMessage("执行该操作将清空数据库中所有历史台风信息,删除后不可恢复,请慎重操作!")
                        .setConfirmButtonText("确认删除")
                        .setCancelButtonText("取消操作")
                        .setDialogAnimation(CBDialogBuilder.DIALOG_ANIM_SLID_BOTTOM)
                        .setButtonClickListener(true, new CBDialogBuilder.onDialogbtnClickListener() {
                            @Override
                            public void onDialogbtnClick(Context context, Dialog dialog, int whichBtn) {
                                switch (whichBtn) {
                                    case BUTTON_CONFIRM:
                                        dao.delete(StrUtil.FH_TYPHOON);
                                        typhoon_msg_list.clear();
                                        zoomImageView.currentDisplayType = -1;
                                        zoomImageView.invalidate();
                                        break;
                                    case BUTTON_CANCEL:
                                        break;
                                    default:
                                        break;
                                }
                            }
                        })
                        .create().show();
            }
        });

        sound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = "";
                final boolean tmpState = !isSoundOpen;
                if (isSoundOpen) {
                    text = "确认关闭声音吗?";
                } else {
                    text = "确认打开声音吗?";
                }

                new CBDialogBuilder(getActivity())
                        .setCancelable(false)
                        .showCancelButton(true)
                        .setTitle("音量调节")
                        .setMessage(text)
                        .setConfirmButtonText("确定")
                        .setCancelButtonText("取消")
                        .setDialogAnimation(CBDialogBuilder.DIALOG_ANIM_SLID_BOTTOM)
                        .setButtonClickListener(true, new CBDialogBuilder.onDialogbtnClickListener() {
                            @Override
                            public void onDialogbtnClick(Context context, Dialog dialog, int whichBtn) {
                                switch (whichBtn) {
                                    case BUTTON_CONFIRM:
                                        fhApplication1.serialPort.write(FHProtocolUtil.parseSound(tmpState));
                                        fhApplication1.soundAck = 0;
                                        final Timer timer = new Timer();
                                        timer.schedule(new TimerTask() {
                                            @Override
                                            public void run() {
                                                if (fhApplication1.soundAck == 1) {
                                                    isSoundOpen = tmpState;
                                                    h1.sendEmptyMessage(FH_SOUND_ACK);
                                                } else if (fhApplication1.soundAck == 2) {
                                                    h1.sendEmptyMessage(FH_SOUND_NACK);
                                                } else if (fhApplication1.soundAck == 0) {
                                                    h1.obtainMessage(FH_NO_RESPONSE, "静噪").sendToTarget();
                                                }
                                                fhApplication1.soundAck = -1;
                                                timer.cancel();
                                            }
                                        }, 800);
                                        break;
                                    default:
                                        break;
                                }
                            }
                        })
                        .create().show();
            }
        });

        sounds.setOnClickListener(new View.OnClickListener() {
            int tmpSounds = 0;

            @Override
            public void onClick(View v) {
                LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
                View soundsView = layoutInflater.inflate(R.layout.setsounds, null);

                final TextView tv = (TextView) soundsView.findViewById(R.id.tv_sounds);
                final SeekBar sb = (SeekBar) soundsView.findViewById(R.id.sb_sounds);
                sb.setMax(31);
                sb.setProgress(soundsCount);
                sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        tv.setText("当前的音量为:" + progress);
                        tmpSounds = progress;
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });

                if (isSoundOpen) {
                    sb.setEnabled(true);
                    //sb.setProgress(soundsCount); 暂时没有用吗?
                    tv.setText("当前音量:" + soundsCount);
                } else {
                    sb.setEnabled(false);
                    tv.setText("当前为静音状态,若想调节音量,请打开声音");
                }

                new FreqDialog(getActivity())
                        .setCancelable(false)
                        .showCancelButton(true)
                        .setTitle("音量设置")
                        .setView(soundsView)
                        .setConfirmButtonText("确认")
                        .setCancelButtonText("取消")
                        .setDialogAnimation(CBDialogBuilder.DIALOG_ANIM_NORMAL)
                        .setButtonClickListener(true, new CBDialogBuilder.onDialogbtnClickListener() {
                            @Override
                            public void onDialogbtnClick(Context context, Dialog dialog, int whichBtn) {
                                switch (whichBtn) {
                                    case BUTTON_CONFIRM:
                                        fhApplication1.serialPort.write(FHProtocolUtil.parseSounds(tmpSounds));
                                        fhApplication1.soundsAck = 0;
                                        final Timer timer = new Timer();
                                        timer.schedule(new TimerTask() {
                                            @Override
                                            public void run() {
                                                if (fhApplication1.soundsAck == 1) {
                                                    soundsCount = tmpSounds;
                                                    h1.sendEmptyMessage(FH_SOUNDS_ACK);
                                                } else if (fhApplication1.soundsAck == 2) {
                                                    h1.sendEmptyMessage(FH_SOUNDS_NACK);
                                                } else if (fhApplication1.soundsAck == 0) {
                                                    h1.obtainMessage(FH_NO_RESPONSE, "音量").sendToTarget();
                                                }
                                                fhApplication1.soundsAck = -1;
                                                timer.cancel();
                                            }
                                        }, 800);
                                        break;
                                    case BUTTON_CANCEL:
                                        //sb.setProgress();
                                        break;
                                    default:
                                        break;
                                }
                            }
                        })
                        .create().show();

            }
        });

        tts = new TextToSpeech(getActivity(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.CHINESE);
                    if (result != TextToSpeech.LANG_COUNTRY_AVAILABLE && result != TextToSpeech.LANG_AVAILABLE)
                        TastyToast.makeText(getActivity(), "不支持的语音格式", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                }
            }
        });
    }

    //重新配置usb的读取函数,跳转到HomeActivity前,已经将串口关闭了
    void initConf() {
        //初始化LruCache
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
        //初始化数据库dao
        dao = new MsgDao(getActivity());
        //初始化侧边栏list
        msgList = dao.findRecent(20);
        adapter = new MsgAdapter();
        mMsgLv.setAdapter(adapter);

        SwipeMenuCreator creator = new SwipeMenuCreator() {
            @Override
            public void create(SwipeMenu menu) {
                SwipeMenuItem item1 = new SwipeMenuItem(getActivity());
                item1.setBackground(new ColorDrawable(Color.rgb(0xE5, 0x18, 0x5E)));
                item1.setWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90, getResources().getDisplayMetrics()));
                item1.setIcon(R.drawable.delete_msg);
                menu.addMenuItem(item1);
            }
        };

        mMsgLv.setMenuCreator(creator);
        //点击效果;
        mMsgLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Msg msg = msgList.get(position);
                if (msg.mMsgImg == R.drawable.business_msg_unread || msg.mMsgImg == R.drawable.msg_unread) {
                    updateReadState(msg.mMsgTime, position);
                }
                new CBDialogBuilder(getActivity())
                        .setTitle("详细信息")
                        .setMessage(msg.mMsgContent)
                        .setConfirmButtonText("确定")
                        .setDialogAnimation(CBDialogBuilder.DIALOG_ANIM_SLID_BOTTOM)
                        .setButtonClickListener(true, null)
                        .create().show();
            }
        });

        //侧滑效果;
        mMsgLv.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                Msg msg = msgList.get(position);
                if (msg.mMsgImg == R.drawable.w38) {
                    TastyToast.makeText(getActivity(), "台风信息不可在此处进行删除", TastyToast.LENGTH_LONG, TastyToast.INFO);
                } else if (dao.delete(msg.mMsgTime)) {
                    Message m = h1.obtainMessage();
                    m.what = FH_MSG_DELETE;
                    m.arg1 = position;
                    //必须要延时处理,否则侧滑栏收缩不回去.
                    h1.sendMessageDelayed(m, 800);
                } else {
                    h1.sendEmptyMessage(FH_MSG_DELETE_FAILED);
                }
                return false;
            }
        });

        if (fhApplication1.serialPort != null) {
            if (fhApplication1.serialPort.open()) {
                fhApplication1.serialPort.setBaudRate(fhApplication1.BAUD_RATE);
                fhApplication1.serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                fhApplication1.serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                fhApplication1.serialPort.setParity(UsbSerialInterface.PARITY_NONE);

                fhApplication1.serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                fhApplication1.serialPort.read(mCallback);
                //TODO:这两个函数不设置行不行?
                //mFHApplication.serialPort.getCTS(ctsCallback);
                //mFHApplication.serialPort.getDSR(dsrCallback);
                //h1.obtainMessage(FH_USB_OK).sendToTarget();
                Toast.makeText(getActivity(), "usb再次打开", Toast.LENGTH_SHORT).show();
            } else {
                //串口无法打开,可能发生了IO错误或者被认为是CDC,但是并不适合本应用,不做处理.
                //h1.obtainMessage(FH_USB_NOK, "USB设备IO错误,请重新连接!!!").sendToTarget();
                Toast.makeText(getActivity(), "usb无法打开", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("###", "initConf:此时usb串口已不可用");
            TastyToast.makeText(getActivity(), "严重错误", TastyToast.LENGTH_LONG, TastyToast.ERROR);
        }

    }

    private void updateReadState(final String msgTime, final int position) {
        new Thread() {
            @Override
            public void run() {
                if (dao.update(msgTime)) {
                    //更新成功
                    h1.obtainMessage(FH_MSG_READ, position, 0).sendToTarget();
                } else {
                    //更新失败,咋办...
                    h1.sendEmptyMessage(FH_MSG_READ_FAILED);
                }
            }
        }.start();
    }

    /**
     * 重新设置一个回调解析函数,该函数,以及其调用的所有函数都在子线程中!!!
     */
    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            //TODO:解析接收机的参数.
            String data = StrUtil.bytesToHexString(arg0);
            receiveFirstBuilder.append(data);
            int begin8383Index = 0;
            int end8383Index = 0;
            while ((begin8383Index = receiveFirstBuilder.indexOf("c0b0b1b2")) > 0) {
                // TODO: 2016/9/11 0011 这一步直接截取0000c0c0a0可能会出问题,可以尝试替换0000c0并且校验偶数位,但是这样也可能截取不准确...
                end8383Index = receiveFirstBuilder.indexOf("c0", begin8383Index);
                if (end8383Index % 2 == 1) {
                    continue;
                }
                String paramStr = receiveFirstBuilder.substring(begin8383Index + FHApplication.PARAM_PREFIX, end8383Index);
                parseParamData(paramStr);
                receiveFirstBuilder.delete(begin8383Index, end8383Index + 2);
            }

            int begin8303Index = 0; // 8303包对应的起始位置
            int end8304Index = 0; // 8304包对应的起始位置
            while ((end8304Index = receiveFirstBuilder.indexOf("c0a008828183040004c0")) > 0) {
                begin8303Index = receiveFirstBuilder.indexOf("c0a008828183030003c0");
                if (begin8303Index < 0) {
                    Log.e("###", "已检测到8304包,但是没有检测到8303包,严重错误,那么丢弃当前8304包以及之前的所有数据!!!");
                    receiveFirstBuilder.delete(0, end8304Index + 20);
                    break;
                }

                StringBuilder realString = StrUtil.getRealDataFromSecondString(receiveFirstBuilder.substring(begin8303Index + 20, end8304Index));
                StrUtil.parseAppDataFromThirdString(realString, hasReceivedBefore, infoMap);
                receiveFirstBuilder.delete(0, end8304Index + 20);
            }
        }
    };

    /**
     * 专门用来解析ack
     *
     * @param s 传进来的参数.这里并没有sdr的回复,所有回复类型均为ack或者nak,或者为主动发送的带有参数的相应,首先需要判断是哪个设置发起了请求
     */
    void parseParamData(String s) {
        if (s.startsWith("02") && s.endsWith("03")) {
            if (s.equals("020603")) { //ack
                whichAck(true);
            } else if (s.startsWith("02733330")) {
                h1.sendEmptyMessage(FH_CHANNEL_SCAN);
            } else if (s.startsWith("02733331")) {
                //收到信道号,表示建链,那么就开启拆链计时
                //需要在l1中,如果接收到数据,就停止计时
                //dislinkThread.onStart();
                // TODO: 2016/9/11 0011 在这里进行拆连设置
                //Param.unlinkCount = 0;
                h1.obtainMessage(FH_CHANNEL_NO, new String(StrUtil.hexStringToBytes(s.substring(8, 12)))).sendToTarget();
            } else if (s.startsWith("027334")) {
                h1.obtainMessage(FH_CHANNEL_RATE, new String(StrUtil.hexStringToBytes(s.substring(6, 16)))).sendToTarget();
            } else if (s.startsWith("027336")) {
                h1.obtainMessage(FH_CHANNEL_BI, new String(StrUtil.hexStringToBytes(s.substring(6, 16)))).sendToTarget();
            } else if (s.equals("021503")) {
                whichAck(false);
            }
        } else {
            //handler 发送坏消息.协议解析失败!!!
            h1.sendEmptyMessage(FH_UNKNOWN_ACK);
        }
    }

    /***
     * 用来判断是那个参数进行了设置,根据不同的结果进行相应的提示.
     *
     * @param ack 是否是ack,false表示nack
     */
    void whichAck(boolean ack) {

        //在设置界面继续的设置,只需要将对应的ack置为相应的值即可,后续有判断修改的.
        if (fhApplication1.offsetAck == 0) {
            if (ack) {
                fhApplication1.offsetAck = 1;
            } else {
                fhApplication1.offsetAck = 2;
            }
        }

        if (fhApplication1.channelAck == 0) {
            if (ack) {
                fhApplication1.channelAck = 1;
            } else {
                fhApplication1.channelAck = 2;
            }
        }

        //设置静噪
        if (fhApplication1.soundAck == 0) {
            if (ack) {
                fhApplication1.soundAck = 1;
            } else {
                fhApplication1.soundAck = 2;
            }
        }

        if (fhApplication1.soundsAck == 0) {
            if (ack) {
                fhApplication1.soundsAck = 1;
            } else {
                fhApplication1.soundsAck = 2;
            }
        }
    }

    //TODO:该方法不在主线程中.需要使用handler
    @Override
    public void onParseAppData(String timeStamp, byte[] b) {
        int infoType = b[0];
        Log.d("###", "消息类型是:" + infoType);
        int userID = (b[1] + 256) % 256 * 256 + (b[2] + 256) % 256;
         /* int isClearTyphoon = b[3];
        if (isClearTyphoon == 1) {
            Param.IsTyphonClear = true;
            // 将台风map中在内存中的数据全部清除
            Param.typhoonMap.clear();
        }*/

        int myID = fhApplication1.ID;
        String myDate = fhApplication1.date;

        switch (infoType) {
            //TODO:都差一个写入数据库的功能还未实现
            case 1: //短消息
                if ((userID != 0) && (userID != myID)) {
                    Log.d("###", "不是群发,不是本机ID,舍弃");
                    break;
                }

                if ((myID == userID) && (Long.valueOf(timeStamp) > Long.valueOf(myDate))) {
                    Log.d("###", "是本机ID,但是已过期,舍弃");
                    break;
                }
                msg = ParseUtil.buildMsg(b, timeStamp);
                add2DB(StrUtil.FH_MSG, msg);
                break;
            case 2://一般气象
                if ((userID != 0)
                        && (Long.valueOf(timeStamp) > Long.valueOf(myDate))) {
                    Log.d("###", "超时舍弃");
                    break;
                }
                msg = ParseUtil.buildWeather(b, timeStamp);
                String area = ((WeatherMsg) msg).area;
                // TODO: 2016/9/11 0011 拿到单独发来的天气,根据area,将其更新到数据中
                break;
            case 3://紧急气象
                msg = ParseUtil.buildWeather(b, timeStamp);

                break;
            case 4://商务气象
                if ((userID != 0)
                        && (Long.valueOf(timeStamp) > Long.valueOf(myDate))) {
                    Log.d("###", "超时舍弃");
                    break;
                }
                msg = ParseUtil.buildBussnisMsg(b, timeStamp);
                add2DB(StrUtil.FH_BUSSNIS_MSG, msg);
                break;
            case 5://用户缴费
            case 9://注册用户
                // TODO: 2016/9/14 0014 时间格式需要处理!!!
                if ((userID != 0) && (myID != userID)) {
                    break;
                }
                byte[] d = {b[4], b[5], b[6], b[7], b[8], b[9]};
                fhApplication1.date = StrUtil.parseTimeInMSG(d);
                String privateData = EncryptUtil.encrypt(fhApplication1.date);
                if (mPref.edit().putString("DATE", privateData).commit()) {
                    h1.sendEmptyMessage(FH_UPDATE_DATE);
                } else {
                    h1.sendEmptyMessage(FH_UPDATE_DATE_FAILD);
                }
                break;
            case 6:

                break;
            case 7:

                break;
            case 8://注销用户

                break;
            case 10://台风
                msg = ParseUtil.buildTyphoon(b, timeStamp);
                add2DB(StrUtil.FH_TYPHOON, msg);
                break;
            case 11://一键发送  烦!!!
                // TODO: 2016/9/13 0013 全发,就这这里!!!
                weather_msg_list = ParseUtil.builWeatherAll(b, timeStamp);
                makeImgs();
                String allContent = "";
                for (int i = 0; i < weather_msg_list.size(); i++) {
                    allContent += "海区" + i + ":" + weather_msg_list.get(i).mMsgContent + ";\n";
                }
                msg = new Msg(R.drawable.w1, allContent, timeStamp);
                if (dao.add(StrUtil.FH_WEATHER_MSG, msg)) {
                    h1.sendEmptyMessage(FH_MSG_ADD);
                    h1.sendEmptyMessage(FH_MAP_DRAW);
                } else {
                    h1.sendEmptyMessage(FH_MSG_ADD_FAILED);
                }
                break;
            default:
                break;
        }

    }

    //该函数的调用是在子线程中
    private void add2DB(int type, Msg msg) {
        if (dao.add(type, msg)) {
            h1.sendEmptyMessage(FH_MSG_ADD);
        } else {
            h1.sendEmptyMessage(FH_MSG_ADD_FAILED);
        }
    }

    private void addToMSGList(Msg msg) {
        if (msgList.size() == 20) {
            msgList.remove(19);
        }
        msgList.add(0, msg);
        adapter.notifyDataSetChanged();
    }

    /***
     * 当接收天气消息时,为其制作各个海区的天气图标,这里要绘制36个
     */
    private void makeImgs() {
        //每次都清除之前保存的内容.
        //weather_imgs.clear();
        for (int i = 0; i < weather_msg_list.size(); i++) {
            WeatherMsg msg = weather_msg_list.get(i);
            String key1 = String.valueOf(msg.mMsgImg);
            String key2 = String.valueOf(msg.mMsgImg2);
            String key3 = key1 + key2;
            Bitmap bitmap1, bitmap2, bitmap3;
            bitmap3 = mMemoryCache.get(key3);
            if (bitmap3 == null) {
                if (mMemoryCache.get(key1) == null) {
                    bitmap1 = BitmapFactory.decodeResource(getResources(), msg.mMsgImg);
                    mMemoryCache.put(key1, bitmap1);
                } else {
                    bitmap1 = mMemoryCache.get(key1);
                }

                if (mMemoryCache.get(key2) == null) {
                    bitmap2 = BitmapFactory.decodeResource(getResources(), msg.mMsgImg);
                    mMemoryCache.put(key2, bitmap2);
                } else {
                    bitmap2 = mMemoryCache.get(key2);
                }

                bitmap3 = mergeBitmap_LR(bitmap1, bitmap2, true);
                mMemoryCache.put(key3, bitmap3);
            }
            weather_imgs[i] = bitmap3;
        }
    }

    /***
     * @param leftBitmap
     * @param rightBitmap
     * @param isBaseMax   是否取二者的最大高度最为拼接后的高度
     * @return
     */
    public Bitmap mergeBitmap_LR(Bitmap leftBitmap, Bitmap rightBitmap, boolean isBaseMax) {

        if (leftBitmap == null || leftBitmap.isRecycled()
                || rightBitmap == null || rightBitmap.isRecycled()) {
            return null;
        }
        int height = 0; // 拼接后的高度，按照参数取大或取小
        if (isBaseMax) {
            height = leftBitmap.getHeight() > rightBitmap.getHeight() ? leftBitmap.getHeight() : rightBitmap.getHeight();
        } else {
            height = leftBitmap.getHeight() < rightBitmap.getHeight() ? leftBitmap.getHeight() : rightBitmap.getHeight();
        }

        // 缩放之后的bitmap
        Bitmap tempBitmapL = leftBitmap;
        Bitmap tempBitmapR = rightBitmap;

        if (leftBitmap.getHeight() != height) {
            tempBitmapL = Bitmap.createScaledBitmap(leftBitmap, (int) (leftBitmap.getWidth() * 1f / leftBitmap.getHeight() * height), height, false);
        } else if (rightBitmap.getHeight() != height) {
            tempBitmapR = Bitmap.createScaledBitmap(rightBitmap, (int) (rightBitmap.getWidth() * 1f / rightBitmap.getHeight() * height), height, false);
        }

        // 拼接后的宽度
        int width = tempBitmapL.getWidth() + tempBitmapR.getWidth();

        // 定义输出的bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);

        // 缩放后两个bitmap需要绘制的参数
        Rect leftRect = new Rect(0, 0, tempBitmapL.getWidth(), tempBitmapL.getHeight());
        Rect rightRect = new Rect(0, 0, tempBitmapR.getWidth(), tempBitmapR.getHeight());

        // 右边图需要绘制的位置，往右边偏移左边图的宽度，高度是相同的
        Rect rightRectT = new Rect(tempBitmapL.getWidth(), 0, width, height);

        canvas.drawBitmap(tempBitmapL, leftRect, leftRect, null);
        canvas.drawBitmap(tempBitmapR, rightRect, rightRectT, null);
        return bitmap;
    }


    //为右侧最新消息设置的适配器
    private class MsgAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return msgList.size();
        }

        @Override
        public Object getItem(int position) {
            return msgList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ViewHolder viewHolder;
            if (convertView == null) {
                view = View.inflate(getActivity(), R.layout.recent_msg_item, null);
                viewHolder = new ViewHolder(view);
                view.setTag(viewHolder);
            } else {
                view = convertView;
                viewHolder = (ViewHolder) convertView.getTag();
            }
            Msg msg = msgList.get(position);
            viewHolder.imageView.setImageResource(msg.mMsgImg);
            viewHolder.content.setText(msg.mMsgContent);
            viewHolder.time.setText(msg.mMsgTime);
            return view;
        }


        class ViewHolder {
            public ImageView imageView;
            public TextView content;
            public TextView time;

            public ViewHolder(View view) {
                imageView = (ImageView) view.findViewById(R.id.recent_msg_img);
                content = (TextView) view.findViewById(R.id.recent_msg_content);
                time = (TextView) view.findViewById(R.id.recent_msg_time);
                view.setTag(this);
            }
        }


        public void updateSingleView(int position) {
            int visibleFirstPosi = mMsgLv.getFirstVisiblePosition();
            int visibleLastPosi = mMsgLv.getLastVisiblePosition();
            if (position >= visibleFirstPosi && position <= visibleLastPosi) {
                View view = mMsgLv.getChildAt(position - visibleFirstPosi);
                ViewHolder holder = (ViewHolder) view.getTag();
                Msg msg = msgList.get(position);
                msg.isRead = true;
                if (msg.mMsgImg == R.drawable.msg_unread) {
                    msg.mMsgImg = R.drawable.msg_read;
                    holder.imageView.setImageResource(R.drawable.msg_read);
                } else if (msg.mMsgImg == R.drawable.business_msg_unread) {
                    msg.mMsgImg = R.drawable.business_msg_read;
                    holder.imageView.setImageResource(R.drawable.business_msg_read);
                }
            }
        }
    }

}
