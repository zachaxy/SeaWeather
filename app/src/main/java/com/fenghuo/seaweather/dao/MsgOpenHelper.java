package com.fenghuo.seaweather.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by zhangxin on 2016/9/11 0011.
 * <p>
 * Description :
 */
public class MsgOpenHelper extends SQLiteOpenHelper {

    //id:自增长;type:存储消息类型(短信,商务信息,天气,台风 4种);time:接收时间按;content:接收内容;help:辅助信息(类型不同,辅助内容不同)
    public static final String CREATE_MSGLIST = "create table msgList ( "
            + "id integer primary key autoincrement,"
            + "msgtype integer,"
            + "receivetime text,"
            + "msgcontent text,"
            + "msghelp text )";


    public MsgOpenHelper(Context context) {
        super(context, "fh.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_MSGLIST);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
