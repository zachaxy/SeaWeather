package com.fenghuo.seaweather.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.fenghuo.seaweather.R;
import com.fenghuo.seaweather.bean.Msg;
import com.fenghuo.seaweather.dao.MsgDao;
import com.fenghuo.seaweather.utils.StrUtil;
import com.sdsmdg.tastytoast.TastyToast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangxin on 2016/8/26 0026.
 * <p>
 * Description :
 */
public class DBFragment extends Fragment {

    public View dbLayout;

    //下拉菜单
    private Spinner mSpinner;
    private List<String> spinner_data_list;
    private ArrayAdapter<String> spinner_adapter;

    //数据库列表
    private ListView db_lv;
    private MsgDao dao;
    private List<Msg> list;
    private DBAdapter adapter;


    //翻页按钮
    private ImageView pre;
    private ImageView next;

    //各数据表中的个数
    private int msg_count;
    private int bmsg_count;
    private int wmsg_count;

    private int currentDB = StrUtil.FH_MSG;
    private int currentPage = 1;

    private static final int FH_MSG_DB = 1;
    private static final int FH_BMSG_DB = 2;
    private static final int FH_WMSG_DB = 3;

    Handler h3 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            /*switch (msg.what) {
                case FH_MSG_DB:
                    adapter = new DBAdapter();
                    db_lv.setAdapter(adapter);
                    break;
                case FH_BMSG_DB:

                    break;
                case FH_WMSG_DB:

                    break;
            }*/
            adapter = new DBAdapter();
            db_lv.setAdapter(adapter);
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        dbLayout = inflater.inflate(R.layout.db_layout, container, false);
        initView();
        initDB();
        return dbLayout;
    }

    void initDB() {
        dao = new MsgDao(getActivity());
        initDBCount();
        //页面加载进来,首先判断总条目是否大于一万条,如大于,删除前五千条并重新初始化count
        if (msg_count + bmsg_count + wmsg_count > 10000) {
            dao.delete();
            initDBCount();
        }
        //初始化数据库时,页面加载进来先显示短信界面的消息.
        query(StrUtil.FH_MSG, msg_count);
        /*new Thread() {
            @Override
            public void run() {
                list = dao.findPartByType(currentPage, 20, StrUtil.FH_MSG, msg_count);
            }
        }.start();*/
    }

    void initDBCount() {
        msg_count = dao.getTotalItemByType(StrUtil.FH_MSG);
        bmsg_count = dao.getTotalItemByType(StrUtil.FH_BUSSNIS_MSG);
        wmsg_count = dao.getTotalItemByType(StrUtil.FH_WEATHER_MSG);
    }

    void initView() {
        db_lv = (ListView) dbLayout.findViewById(R.id.lv_db);

        pre = (ImageView) dbLayout.findViewById(R.id.pre);
        next = (ImageView) dbLayout.findViewById(R.id.next);

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (currentDB) {
                    case StrUtil.FH_MSG:
                        currentPage++;
                        if (msg_count - currentPage * 20 <= -20) {
                            TastyToast.makeText(getActivity(), "已经是最后一页", TastyToast.LENGTH_LONG, TastyToast.WARNING);
                            break;
                        } else {
                            query(StrUtil.FH_MSG, msg_count);
                        }
                        break;
                    case StrUtil.FH_BUSSNIS_MSG:
                        currentPage++;
                        if (bmsg_count - currentPage * 20 <= -20) {
                            TastyToast.makeText(getActivity(), "已经是最后一页", TastyToast.LENGTH_LONG, TastyToast.WARNING);
                            break;
                        } else {
                            query(StrUtil.FH_BUSSNIS_MSG, bmsg_count);
                        }
                        break;
                    case StrUtil.FH_WEATHER_MSG:
                        currentPage++;
                        if (wmsg_count - currentPage * 20 <= -20) {
                            TastyToast.makeText(getActivity(), "已经是最后一页", TastyToast.LENGTH_LONG, TastyToast.WARNING);
                            break;
                        } else {
                            query(StrUtil.FH_WEATHER_MSG, wmsg_count);
                        }
                        break;
                }
            }
        });


        pre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage == 1) {
                    TastyToast.makeText(getActivity(), "已经是第一页", TastyToast.LENGTH_LONG, TastyToast.WARNING);
                } else {
                    currentPage--;
                    switch (currentDB) {
                        case StrUtil.FH_MSG:
                            query(StrUtil.FH_MSG, msg_count);
                            break;
                        case StrUtil.FH_BUSSNIS_MSG:
                            query(StrUtil.FH_BUSSNIS_MSG, bmsg_count);
                            break;
                        case StrUtil.FH_WEATHER_MSG:
                            query(StrUtil.FH_WEATHER_MSG, wmsg_count);
                            break;
                    }
                }
            }

        });

        mSpinner = (Spinner) dbLayout.findViewById(R.id.sp);
        spinner_data_list = new ArrayList<>();
        spinner_data_list.add("普通短信");
        spinner_data_list.add("商务短信");
        spinner_data_list.add("天气消息");
        spinner_adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, spinner_data_list);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //加载适配器
        mSpinner.setAdapter(spinner_adapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getActivity(), "选择数据库" + position, Toast.LENGTH_SHORT).show();
                currentPage = 1;
                switch (position) {
                    case 0:
                        currentDB = StrUtil.FH_MSG;
                        new Thread() {
                            @Override
                            public void run() {
                                list = dao.findPartByType(currentPage, 20, StrUtil.FH_MSG, msg_count);
                                h3.sendEmptyMessage(FH_MSG_DB);
                            }
                        }.start();
                        break;
                    case 1:
                        currentDB = StrUtil.FH_BUSSNIS_MSG;
                        new Thread() {
                            @Override
                            public void run() {
                                list = dao.findPartByType(currentPage, 20, StrUtil.FH_BUSSNIS_MSG, bmsg_count);
                                h3.sendEmptyMessage(FH_BMSG_DB);
                            }
                        }.start();
                        break;
                    case 2:
                        currentDB = StrUtil.FH_WEATHER_MSG;
                        new Thread() {
                            @Override
                            public void run() {
                                list = dao.findPartByType(currentPage, 20, StrUtil.FH_WEATHER_MSG, wmsg_count);
                                h3.sendEmptyMessage(FH_WMSG_DB);
                            }
                        }.start();
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    class DBAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Msg msg = list.get(position);
            View view;

            MsgItemHolder viewHolder;
            if (convertView == null) {
                view = View.inflate(getActivity(), R.layout.db_item, null);
                viewHolder = new MsgItemHolder();
                viewHolder.img = (ImageView) view.findViewById(R.id.msg_img);
                viewHolder.content = (TextView) view.findViewById(R.id.msg_content);
                viewHolder.time = (TextView) view.findViewById(R.id.msg_time);
                view.setTag(viewHolder);
            } else {
                view = convertView;
                viewHolder = (MsgItemHolder) view.getTag();
            }
            //--至此:避免了多度使用inflate加载所带来的性能问题.,然而这一步仅仅是解决了单纯的view复用问题
            //接下来还要为view重新绑定新的数据.

            viewHolder.img.setImageResource(msg.mMsgImg);
            viewHolder.content.setText(msg.mMsgContent);
            viewHolder.time.setText(msg.mMsgTime);
            return view;
        }
    }

    class MsgItemHolder {
        public ImageView img;
        public TextView content;
        public TextView time;
    }

    private void query(final int type, final int count) {
        new Thread() {
            @Override
            public void run() {
                list = dao.findPartByType(currentPage, 20, type, count);
                h3.sendEmptyMessage(0);
            }
        }.start();
    }
}
