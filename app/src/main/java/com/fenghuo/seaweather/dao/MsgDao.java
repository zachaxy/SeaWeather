package com.fenghuo.seaweather.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.fenghuo.seaweather.R;
import com.fenghuo.seaweather.bean.Msg;
import com.fenghuo.seaweather.bean.TyphoonMsg;
import com.fenghuo.seaweather.utils.StrUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangxin on 2016/9/11 0011.
 * <p>
 * Description :
 */
public class MsgDao {


    private final MsgOpenHelper helper;

    public MsgDao(Context context) {
        helper = new MsgOpenHelper(context);
    }


    public boolean add(int mode, Msg msg) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        switch (mode) {
            case StrUtil.FH_MSG:  //短信
                values.put("msgtype", StrUtil.FH_MSG);
                values.put("receivetime", msg.mMsgTime);
                values.put("msgcontent", msg.mMsgContent);
                values.put("msghelp", msg.isRead ? "read" : "unread");
                break;
            case StrUtil.FH_BUSSNIS_MSG:  //商务信息
                values.put("msgtype", StrUtil.FH_BUSSNIS_MSG);
                values.put("receivetime", msg.mMsgTime);
                values.put("msgcontent", msg.mMsgContent);
                values.put("msghelp", msg.isRead ? "read" : "unread");
                break;
            case StrUtil.FH_WEATHER_MSG:  //全部天气
                values.put("msgtype", StrUtil.FH_WEATHER_MSG);
                values.put("receivetime", msg.mMsgTime);
                values.put("msgcontent", msg.mMsgContent);
                values.put("msghelp", "read");
                break;
            case StrUtil.FH_TYPHOON:  //台风;添加时要注意格式化help消息!!!
                values.put("msgtype", StrUtil.FH_TYPHOON);
                values.put("receivetime", msg.mMsgTime);
                values.put("msgcontent", msg.mMsgContent);
                values.put("msghelp", formatTyphoonHelp((TyphoonMsg) msg));
                break;
            default:
                break;
        }
        long rawID = db.insert("msgList", null, values);
        db.close();
        if (rawID == -1) {
            return false;
        }
        return true;
    }

    public boolean delete(String time) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int rowNumber = db.delete("msgList", "receivetime=?", new String[]{time});
        db.close();
        if (rowNumber == 0) {
            return false;
        } else {
            return true;
        }
    }

    /***
     * 数据库删除操作,当数据库中的总量超过1w条时,删除前5k条
     * 特别注意:删除结束后更新内存中的数据总量!!!否则删除无效
     */
    public void delete() {
        SQLiteDatabase db = helper.getWritableDatabase();
        //直接delete from table limit 10 不行,都是坑...
        db.execSQL("delete from msgList where id in (select id from msgList order by id limit 0,5000)");
        db.close();
    }

    public boolean delete(int type) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int rowNumber = db.delete("msgList", "msgtype=?", new String[]{String.valueOf(type)});
        db.close();
        if (rowNumber == 0) {
            return false;
        } else {
            return true;
        }
    }


    public boolean update(String time) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues value = new ContentValues();
        value.put("msghelp", "read");
        int rowNUmber = db.update("msgList", value, "receivetime=?", new String[]{time});
        db.close();
        if (rowNUmber == 0) {
            return false;
        } else {
            return true;
        }
    }

    /***
     * 弃用!!!改用findPartByType()
     *
     * @param currentPage
     * @param pageSize
     * @return
     */
    public List<Msg> findPart(int currentPage, int pageSize) {
        ArrayList<Msg> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        //limit表示查询多少条数据;offset表示从多少条后开始查询
        Cursor cursor = db.rawQuery("select msgtype,receivetime,msgcontent,msghelp from msgList limit ? offset ?",
                new String[]{String.valueOf(pageSize), String.valueOf(pageSize * currentPage)});
        //到着读
        if (cursor.moveToLast()) {
            do {
                int type = cursor.getInt(0);
                String time = cursor.getString(1);
                String content = cursor.getString(2);
                int imgID = R.drawable.w1;
                switch (type) {
                    case StrUtil.FH_MSG:
                        imgID = cursor.getString(3).equals("read") ? R.drawable.msg_read : R.drawable.msg_unread;
                        break;
                    case StrUtil.FH_BUSSNIS_MSG:
                        imgID = cursor.getString(3).equals("read") ? R.drawable.business_msg_read : R.drawable.business_msg_unread;
                        break;
                    case StrUtil.FH_WEATHER_MSG:
                        imgID = Integer.valueOf(cursor.getString(3)); //天气消息:将其资源id所代表的整数填写到help中
                        break;
                }
                Msg msg = new Msg(imgID, content, time);
                list.add(msg);
            } while (cursor.moveToPrevious());
        }
        cursor.close();
        db.close();
        return list;
    }

    //数据库页面分别查询:短信;商务短信;天气
    public List<Msg> findPartByType(int currentPage, int pageSize, int type, int totalCount) {
        ArrayList<Msg> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        int offset = totalCount - pageSize * currentPage;
        if (offset < 0) {
            offset = 0;
        }
        //limit表示查询多少条数据;offset表示从多少条后开始查询
        Cursor cursor = db.rawQuery("select receivetime,msgcontent,msghelp from msgList  where msgtype = ? limit ? offset ?",
                new String[]{String.valueOf(type), String.valueOf(pageSize), String.valueOf(offset)});
        //倒着读
        if (cursor.moveToLast()) {
            do {
                String time = cursor.getString(0);
                String content = cursor.getString(1);
                int imgID = R.drawable.w1;
                Msg msg = null;
                switch (type) {
                    case StrUtil.FH_MSG:
                        imgID = cursor.getString(2).equals("read") ? R.drawable.msg_read : R.drawable.msg_unread;
                        break;
                    case StrUtil.FH_BUSSNIS_MSG:
                        imgID = cursor.getString(2).equals("read") ? R.drawable.business_msg_read : R.drawable.business_msg_unread;
                        break;
                    case StrUtil.FH_WEATHER_MSG: //得到天气图标
                        imgID = Integer.valueOf(cursor.getString(2));
                        break;
                }
                msg = new Msg(imgID, content, time);
                list.add(msg);
            } while (cursor.moveToPrevious());
        }
        cursor.close();
        db.close();
        return list;
    }

    /***
     * 用来查询台风消息,调用时机点击切换台风视图时?
     *
     * @return
     */
    public List<TyphoonMsg> findTyphoon() {
        ArrayList<TyphoonMsg> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        //limit表示查询多少条数据;offset表示从多少条后开始查询
        /*Cursor cursor = db.rawQuery("select receivetime,msgcontent,msghelp from msgList  where msgtype = ?",
                new String[]{String.valueOf(StrUtil.FH_TYPHOON)});*/
        Cursor cursor = db.rawQuery("select receivetime,msgcontent,msghelp from msgList  where msgtype = 4", null);
        //倒着读
        if (cursor.moveToLast()) {
            do {
                String time = cursor.getString(0);
                String content = cursor.getString(1);
                String help = cursor.getString(2);
                int imgID = R.drawable.w38;
                TyphoonMsg msg = new TyphoonMsg(imgID, content, time);
                msg.setTyphoonDetail(help);
                list.add(msg);
            } while (cursor.moveToPrevious());
        }
        cursor.close();
        db.close();
        return list;
    }


    /***
     * 获取最新的limit条数据,展示在map页面
     *
     * @param limit
     * @return
     */
    public List<Msg> findRecent(int limit) {
        int total = getTotalItem();
        int offset = total - limit;
        if (offset < 0) {
            offset = 0;
        }
        ArrayList<Msg> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        //limit表示查询多少条数据;offset表示从多少条后开始查询
        Cursor cursor = db.rawQuery("select msgtype,receivetime,msgcontent,msghelp from msgList limit ? offset ?",
                new String[]{String.valueOf(limit), String.valueOf(offset)});
        //到着读
        if (cursor.moveToLast()) {
            do {
                int type = cursor.getInt(0);
                String time = cursor.getString(1);
                String content = cursor.getString(2);
                int imgID = R.drawable.w1;
                switch (type) {
                    case StrUtil.FH_MSG:
                        imgID = cursor.getString(3).equals("read") ? R.drawable.msg_read : R.drawable.msg_unread;
                        break;
                    case StrUtil.FH_BUSSNIS_MSG:
                        imgID = cursor.getString(3).equals("read") ? R.drawable.business_msg_read : R.drawable.business_msg_unread;
                        break;
                    case StrUtil.FH_WEATHER_MSG:
                        imgID = Integer.valueOf(cursor.getString(3)); //天气消息:将其资源id所代表的整数填写到help中
                        break;
                    case StrUtil.FH_TYPHOON:
                        //TODO:台风存储格式
                        imgID = R.drawable.w38;
                        break;
                }
                Msg msg = new Msg(imgID, content, time);
                list.add(msg);
            } while (cursor.moveToPrevious());
        }
        cursor.close();
        db.close();
        return list;
    }

    public int getTotalItem() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select count(*) from msgList", null);
        cursor.moveToNext();
        int count = cursor.getInt(0);
        cursor.close();
        db.close();
        return count;
    }

    public int getTotalItemByType(int type) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select count(*) from msgList where msgtype = ?", new String[]{String.valueOf(type)});
        cursor.moveToNext();
        int count = cursor.getInt(0);
        cursor.close();
        db.close();
        return count;
    }

    private String formatTyphoonHelp(TyphoonMsg msg) {
        return msg.name + ";" + msg.number + ";" + msg.is7 + ";" + msg.rang7 + ";" + msg.is10 + ";" + msg.rang10 + ";" + msg.is12 + ";" + msg.rang12;
    }

}
