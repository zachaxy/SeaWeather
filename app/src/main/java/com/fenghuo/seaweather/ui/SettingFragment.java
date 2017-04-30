package com.fenghuo.seaweather.ui;

import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.fenghuo.seaweather.FHApplication;
import com.fenghuo.seaweather.R;
import com.fenghuo.seaweather.utils.FHProtocolUtil;
import com.fenghuo.seaweather.utils.StrUtil;
import com.fenghuo.seaweather.widget.FreqDialog;
import com.sdsmdg.tastytoast.TastyToast;
import com.zhl.cbdialog.CBDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by zhangxin on 2016/8/26 0026.
 * <p>
 * Description :
 */
public class SettingFragment extends Fragment {
    // 管理员密钥
    static final String ADMIN_PASSWORD = "123456";

    private final int FH_FREQ_ACK = 1;
    private final int FH_FREQ_NACK = 2;
    private final int FH_FREQ_NO_RESPONSE = 3;
    private final int FH_OFFSET_ACK = 4;
    private final int FH_OFFSET_NACK = 5;
    private final int FH_OFFSET_NO_RESPONSE = 6;
    private final int FH_UNKNOWN = 9;

    FHApplication fhApplication2;

    // --------布局中的组件------------
    EditText userID;
    Button userIDSet;
    TextView date;

    // 频道的设置
    ListView channels;
    Button channels_w;
    List<String> channelsList;
    ChannelAdapter channelsAdapter;
    //暂时用来存放频率设置量,等回复ack,在进行配置内修改
    String tmpChannel = "";


    // 频偏的设置
    private final String DEFAULT_OFFSET = "180";
    EditText offset;
    ImageButton offset_plus, offset_minus;
    Button offset_w;
    //暂时用来存放频偏设置量,等回复ack,在进行配置内修改
    String tmpOffset = "180";


    // 拆连的设置
    private final int DEFAULT_UNLINKTIME = 60;
    EditText autoUnlink;
    Button autoUnlinkSend;

    // 管理员锁
    ImageView state_img;

    // -------------------------------------------------------------------------

    // 判断参数设置是否锁定,锁定则不可以修改参数
    boolean isLocked;

    // 用于加载自定义对话框
    LayoutInflater factory;


    // 标记串口是否已经打开
    Boolean openFlag = false;

    // 当前view
    View SettingLayout;

    List<String> chanelIndexList;


    SharedPreferences mPref;

    boolean leftCheck = true;
    boolean rightCheck = true;

    int currentAck = -2;

    Handler h2 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FH_FREQ_ACK:
                    channelsAdapter.notifyDataSetChanged();
                    TastyToast.makeText(fhApplication2, "频率设置成功", TastyToast.LENGTH_LONG, TastyToast.SUCCESS);
                    break;
                case FH_FREQ_NACK:
                    TastyToast.makeText(fhApplication2, "频率设置失败,请重新设置", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    break;
                case FH_FREQ_NO_RESPONSE:
                    TastyToast.makeText(fhApplication2, "频率设置无响应,请重新设置", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    break;
                case FH_OFFSET_ACK:
                    TastyToast.makeText(fhApplication2, "频偏设置成功", TastyToast.LENGTH_LONG, TastyToast.SUCCESS);
                    break;
                case FH_OFFSET_NACK:
                    TastyToast.makeText(fhApplication2, "频偏设置失败,请重新设置", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    break;
                case FH_OFFSET_NO_RESPONSE:
                    TastyToast.makeText(fhApplication2, "频偏设置无响应", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    break;
                case FH_UNKNOWN:
                    TastyToast.makeText(fhApplication2, "未知的错误,请重新设置", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    break;
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View settingLayout = inflater.inflate(R.layout.setting_layout, container, false);
        SettingLayout = settingLayout;

        fhApplication2 = ((HomeActivity) getActivity()).mFHApplication;
        mPref = getActivity().getSharedPreferences("fenghuo", Context.MODE_PRIVATE);

        initView(settingLayout);

        // TODO: 2016/9/10 0010  maybe wrong!!!
        factory = LayoutInflater.from(getActivity());

        unableEdit();

        return settingLayout;
    }


    void initView(View view) {

        //***************基础组件的初始化*******************
        isLocked = true;
        userID = (EditText) view.findViewById(R.id.usrID);
        //userID.setText(String.valueOf(Param.mUsrID));
        userIDSet = (Button) view.findViewById(R.id.uidSet);

        date = (TextView) view.findViewById(R.id.date);

        channels = (ListView) view.findViewById(R.id.channels);

        offset = (EditText) view.findViewById(R.id.offset);
        offset.setText(mPref.getString("OFFSET", DEFAULT_OFFSET));
        offset_plus = (ImageButton) view.findViewById(R.id.offset_plus);
        offset_minus = (ImageButton) view.findViewById(R.id.offset_minus);
        offset_w = (Button) view.findViewById(R.id.offset_w);

        autoUnlink = (EditText) view.findViewById(R.id.unlinkTime);
        autoUnlink.setText(String.valueOf(mPref.getInt("UNLINKTIME", DEFAULT_UNLINKTIME)));
        autoUnlinkSend = (Button) view.findViewById(R.id.unLinkSet);

        //*********************管理员锁设置*********************************
        state_img = (ImageView) view.findViewById(R.id.state_img);
        state_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLocked) {
                    // 弹出提示框,对isLoacked
                    showLoginDialog();
                } else {
                    // 弹出提示框,提示保存
                    showLayoutDialog();
                }
            }
        });


        //***********************对左侧频率列表的设置***********************
        chanelIndexList = new ArrayList<String>();
        chanelIndexList.add("信道0: ");
        chanelIndexList.add("信道1: ");
        chanelIndexList.add("信道2: ");
        chanelIndexList.add("信道3: ");
        chanelIndexList.add("信道4: ");
        chanelIndexList.add("信道5: ");
        chanelIndexList.add("信道6: ");
        chanelIndexList.add("信道7: ");
        chanelIndexList.add("信道8: ");
        chanelIndexList.add("信道9: ");

        channelsList = fhApplication2.mChannels;
        channelsAdapter = new ChannelAdapter(fhApplication2, chanelIndexList, channelsList);
        channels.setAdapter(channelsAdapter);
        // 屏蔽底层srollView的触摸事件,解决与listView的滑动冲突.
        // TODO: 2016/9/9 0009 底层如何实现? 
        channels.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ((ViewGroup) v).requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        //每个信道的点击事件,用于配置信道频率
        channels.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, final int arg2, long arg3) {
                // 如果显示是锁定状态,也可以滑动,但是点击无效.
                if (!isLocked) {
                    final View channelInput = factory.inflate(R.layout.channel_config, null);

                    final TextView warm = (TextView) channelInput.findViewById(R.id.channel_warning);

                    final EditText channelLeft = (EditText) channelInput.findViewById(R.id.channel_left);
                    channelLeft.setText(channelsList.get(arg2).substring(0, 2));

                    final EditText channelRight = (EditText) channelInput.findViewById(R.id.channel_right);
                    channelRight.setText(channelsList.get(arg2).substring(3, 7));

                    //将左右两个check的初始值设置为true,以防止不修改直接提交不成功的bug
                    leftCheck = true;
                    rightCheck = true;

                    channelLeft.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            if (s.toString().length() < 1 || s.toString().length() > 2) {
                                warm.setText("* 信道的整数部分范围在2~29之间,请重新输入");
                                warm.setTextColor(Color.RED);
                                leftCheck = false;
                            }
                        }

                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        @Override
                        public void afterTextChanged(Editable s) {

                            if (s.toString().length() < 1 || s.toString().length() > 2) {
                                warm.setText("* 信道的整数部分范围在2~29之间,请重新输入");
                                warm.setTextColor(Color.RED);
                                leftCheck = false;
                            }
                            String sValue = channelLeft.getText().toString() + "." + channelRight.getText().toString();
                            double dValue = 0;
                            try {
                                dValue = Double.valueOf(sValue);
                            } catch (NumberFormatException e) {
                                warm.setText("* 非法的字符输入,请重新输入");
                                warm.setTextColor(Color.RED);
                                leftCheck = false;
                            }

                            if (dValue < 2.0 || dValue > 29.9999) {
                                warm.setText("* 信道的整数部分范围在2.0000~29.9999之间,请重新输入");
                                warm.setTextColor(Color.RED);
                                leftCheck = false;
                            } else {
                                warm.setText("合法的输入");
                                warm.setTextColor(Color.GREEN);
                                leftCheck = true;
                            }
                        }
                    });

                    channelRight.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            if (s.toString().length() < 1
                                    || s.toString().length() > 4) {
                                warm.setText("* 信道的小数部分范围在0~9999之间,请重新输入");
                                warm.setTextColor(Color.RED);
                                rightCheck = false;
                            }
                        }

                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            if (s.toString().length() < 1 || s.toString().length() > 4) {
                                warm.setText("* 信道的小数部分范围在0~9999之间,请重新输入");
                                warm.setTextColor(Color.RED);
                                rightCheck = false;
                            }

                            String sValue = channelLeft.getText().toString() + "." + channelRight.getText().toString();
                            double dValue = 0;
                            try {
                                dValue = Double.valueOf(sValue);
                            } catch (NumberFormatException e) {
                                warm.setText("* 非法的字符输入,请重新输入");
                                warm.setTextColor(Color.RED);
                                rightCheck = false;
                            }

                            if (dValue < 2.0 || dValue > 29.9999) {
                                warm.setText("* 信道的整数部分范围在2.0000~29.9999之间,请重新输入");
                                warm.setTextColor(Color.RED);
                                rightCheck = false;
                            } else {
                                warm.setText("合法的输入");
                                warm.setTextColor(Color.GREEN);
                                rightCheck = true;
                            }

                        }
                    });
                    // TODO: 2016/9/9 0009 不用移除了吧!!!
                    /*ViewGroup p = (ViewGroup) channelInput.getParent();
                    if (p != null) {
                        p.removeView(channelInput);
                    }*/
                    FreqDialog dialog = new FreqDialog(fhApplication2);
                    dialog.setCancelable(false)
                            .showCancelButton(true)
                            .setTitle("频率设置")
                            .setView(channelInput)
                            .setConfirmButtonText("确认")
                            .setCancelButtonText("取消")
                            .setDialogAnimation(CBDialogBuilder.DIALOG_ANIM_NORMAL)
                            .setButtonClickListener(true, new CBDialogBuilder.onDialogbtnClickListener() {
                                @Override
                                public void onDialogbtnClick(Context context, Dialog dialog, int whichBtn) {
                                    switch (whichBtn) {
                                        case BUTTON_CONFIRM:
                                            if (leftCheck && rightCheck) {
                                                tmpChannel = FHProtocolUtil.formatChannel(channelLeft.getText().toString(), channelRight.getText().toString());
                                                fhApplication2.serialPort.write(FHProtocolUtil.parseChannels(tmpChannel, arg2));
                                                fhApplication2.channelAck = 0;
                                                final Timer timer = new Timer();
                                                timer.schedule(new TimerTask() {
                                                    @Override
                                                    public void run() {
                                                        if (fhApplication2.channelAck == 1) {
                                                            if (mPref.edit().putString(StrUtil.freqNameInPref[arg2], tmpChannel).commit()) {
                                                                channelsList.set(arg2, tmpChannel);
                                                                h2.sendEmptyMessage(FH_FREQ_ACK);
                                                            } else {
                                                                h2.sendEmptyMessage(FH_UNKNOWN);
                                                            }
                                                        } else if (fhApplication2.channelAck == 2) {
                                                            h2.sendEmptyMessage(FH_FREQ_NACK);
                                                        } else if (fhApplication2.channelAck == 0) {
                                                            h2.sendEmptyMessage(FH_FREQ_NO_RESPONSE);
                                                        }
                                                        fhApplication2.channelAck = -1;
                                                        timer.cancel();
                                                    }
                                                }, 800);
                                            } else {
                                                TastyToast.makeText(fhApplication2, "当前频率设置不在允许范围内,请重新设置", TastyToast.LENGTH_LONG, TastyToast.WARNING);
                                            }
                                            break;
                                        /*case BUTTON_CANCEL:
                                            break;*/
                                        default:
                                            break;
                                    }
                                }
                            })
                            .create().show();

                }
            }
        });

        //*********************用户ID设置*******************************
        userIDSet.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String s = userID.getText().toString();
                int i = -1;
                try {
                    i = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    TastyToast.makeText(fhApplication2, "不合法的字符输入", TastyToast.LENGTH_LONG, TastyToast.WARNING);
                    userID.setText(String.valueOf(fhApplication2.ID));
                }

                if (i < 1 || i > 65535) {
                    TastyToast.makeText(fhApplication2, "用户ID值超出范围(1~65535)", TastyToast.LENGTH_LONG, TastyToast.WARNING);
                    userID.setText(String.valueOf(fhApplication2.ID));
                } else if (mPref.edit().putInt("ID", i).commit()) {
                    fhApplication2.ID = i;
                } else {
                    TastyToast.makeText(fhApplication2, "未知的错误,请重新设置^_^", TastyToast.LENGTH_LONG, TastyToast.WARNING);
                    userID.setText(String.valueOf(fhApplication2.ID));
                }
            }
        });

        //********************频偏的设置***************************
        offset_plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = offset.getText().toString();
                int i = -1;
                try {
                    i = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    //程序不会执行到这一步
                    TastyToast.makeText(fhApplication2, "当前频偏值存在不合法的字符输入", TastyToast.LENGTH_LONG, TastyToast.WARNING);
                    userID.setText(DEFAULT_OFFSET);//默认值设置为180吧....
                    return;
                }

                i = i + 1;
                if (i == 256) {
                    i = 0;
                }

                s = String.valueOf(i);
                if (s.length() == 1) {
                    s = "00" + s;
                } else if (s.length() == 2) {
                    s = "0" + s;
                }
                offset.setText(s);
            }
        });

        offset_minus.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String s = offset.getText().toString();
                int i = -1;
                try {
                    i = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    TastyToast.makeText(fhApplication2, "当前频偏值存在不合法的字符输入", TastyToast.LENGTH_LONG, TastyToast.WARNING);
                    userID.setText(DEFAULT_OFFSET);
                    return;
                }

                i = i - 1;
                if (i == -1) {
                    i = 255;
                }
                s = String.valueOf(i);
                if (s.length() == 1) {
                    s = "00" + s;
                } else if (s.length() == 2) {
                    s = "0" + s;
                }
                offset.setText(s);
            }
        });

        offset_w.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String s = offset.getText().toString();
                int i = -1;
                try {
                    i = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    TastyToast.makeText(fhApplication2, "当前频偏值存在不合法的字符输入", TastyToast.LENGTH_LONG, TastyToast.WARNING);
                    offset.setText(DEFAULT_OFFSET);
                    return;
                }
                if (i < 0 || i > 255) {
                    TastyToast.makeText(fhApplication2, "频偏范围错误(有效值:0~255)", TastyToast.LENGTH_LONG, TastyToast.WARNING);
                    offset.setText(DEFAULT_OFFSET);
                } else {
                    if (s.length() == 1) {
                        s = "00" + s;
                    } else if (s.length() == 2) {
                        s = "0" + s;
                    }
                    tmpOffset = s;
                    // TODO: 2016/9/10 0010 向usb写频偏,不知是否可行.
                    fhApplication2.serialPort.write(FHProtocolUtil.parseOffset(tmpOffset));
                    fhApplication2.offsetAck = 0;
                    final Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (fhApplication2.offsetAck == 1) {
                                if (mPref.edit().putString("OFFSET", tmpOffset).commit()) {
                                    h2.sendEmptyMessage(FH_OFFSET_ACK);
                                } else {
                                    h2.sendEmptyMessage(FH_UNKNOWN);
                                }
                            } else if (fhApplication2.offsetAck == 2) {
                                h2.sendEmptyMessage(FH_OFFSET_NACK);
                            } else if (fhApplication2.offsetAck == 0) {
                                h2.sendEmptyMessage(FH_OFFSET_NO_RESPONSE);
                            }
                            //善后,还原为-1吧
                            fhApplication2.offsetAck = -1;
                            timer.cancel();
                        }
                    }, 800);
                }

            }
        });


        //*******************拆连设置****************************
        // TODO: 2016/9/10 0010 拆连要干嘛? 
        autoUnlinkSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String s = autoUnlink.getText().toString();
                fhApplication2.unlinkTime = Integer.valueOf(s);
                mPref.edit().putInt("UNLINKTIME", fhApplication2.unlinkTime).commit();
                /*Param.unlinkCount = 0;
                Perf.editor.putInt(Perf.P_UNLINKTIME, Param.unlinkTime);
                Perf.editor.commit();*/
            }
        });


        //***********************************************
    }

    private void showLoginDialog() {
        final View adminLoginView = factory.inflate(R.layout.admin_config, null);
        final EditText admin_pwd = (EditText) adminLoginView.findViewById(R.id.admin_pwd);
        final ImageView showPWD = (ImageView) adminLoginView.findViewById(R.id.show_password);

        showPWD.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        admin_pwd.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        break;
                    case MotionEvent.ACTION_UP:
                        admin_pwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        admin_pwd.setSelection(admin_pwd.getText().length());
                        break;
                    default:
                        break;
                }
                return false;
            }
        });

        FreqDialog dialog = new FreqDialog(fhApplication2);
        dialog.setCancelable(false)
                .showCancelButton(true)
                .setTitle("管理员操作!")
                .setView(adminLoginView)
                .setConfirmButtonText("确认")
                .setCancelButtonText("取消")
                .setDialogAnimation(CBDialogBuilder.DIALOG_ANIM_NORMAL)
                .setButtonClickListener(true, new CBDialogBuilder.onDialogbtnClickListener() {

                    @Override
                    public void onDialogbtnClick(Context context, Dialog dialog, int whichBtn) {
                        if (whichBtn == BUTTON_CONFIRM) {
                            String pwd = admin_pwd.getText().toString();
                            if (pwd.equals(ADMIN_PASSWORD)) {
                                isLocked = false;
                                enableEdit();
                                state_img.setImageResource(R.drawable.unlock);
                            } else {
                                TastyToast.makeText(fhApplication2, "密码错误!!!", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                            }
                        }
                    }
                }).create().show();

    }

    private void showLayoutDialog() {

        new CBDialogBuilder(fhApplication2)
                .setTouchOutSideCancelable(true)
                .setTitle("退出管理员操作")
                .setMessage("^_^")
                .setConfirmButtonText("确定")
                .setDialogAnimation(CBDialogBuilder.DIALOG_ANIM_SLID_BOTTOM)
                .create().show();

        isLocked = true;
        state_img.setImageResource(R.drawable.lock);
        unableEdit();
    }

    void enableEdit() {
        userID.setEnabled(true);

        userIDSet.setEnabled(true);
        // channels.setEnabled(true);
        //channels_w.setEnabled(true);

        offset.setEnabled(true);
        offset_plus.setEnabled(true);
        offset_minus.setEnabled(true);
        offset_w.setEnabled(true);

        autoUnlinkSend.setEnabled(true);
        autoUnlink.setEnabled(true);

    }

    void unableEdit() {
        userID.setEnabled(false);
        userIDSet.setEnabled(false);
        // channels.setEnabled(false);
        //channels_w.setEnabled(false);

        offset.setEnabled(false);
        offset_plus.setEnabled(false);
        offset_minus.setEnabled(false);
        offset_w.setEnabled(false);

        autoUnlinkSend.setEnabled(false);
        autoUnlink.setEnabled(false);

    }


    class ChannelAdapter extends BaseAdapter {

        private List<String> mNameList;
        private List<String> mFreqList;

        private LayoutInflater mInflater;

        public ChannelAdapter(Context context, List<String> nameList, List<String> freqList) {
            mInflater = LayoutInflater.from(context);
            mNameList = nameList;
            mFreqList = freqList;
        }

        @Override
        public int getCount() {
            return fhApplication2.mChannelCount;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }
    }


   /* private byte[] parseOffset(String s) {
        String hexOffset = Integer.toHexString(s.charAt(0))
                + Integer.toHexString(s.charAt(1))
                + Integer.toHexString(s.charAt(2));
        String ss = "025336" + hexOffset + "03";
        return StrUtil.hexStringToBytes(ss);
    }

    private byte[] parseChannel(String s, int index) {
        String hexChannel;
        hexChannel = parseChannel2Hex(s);
        String ss = "02534B30303" + index + hexChannel + "30" + hexChannel + "303003";
        return StrUtil.hexStringToBytes(ss);
    }

    private String parseChannel2Hex(String s) {
        String hexChannel = Integer.toHexString(s.charAt(0))
                + Integer.toHexString(s.charAt(1))
                + Integer.toHexString(s.charAt(3))
                + Integer.toHexString(s.charAt(4))
                + Integer.toHexString(s.charAt(5))
                + Integer.toHexString(s.charAt(6));
        return hexChannel;
    }*/
}
