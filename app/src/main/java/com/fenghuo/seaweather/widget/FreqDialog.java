package com.fenghuo.seaweather.widget;

import android.content.Context;
import android.view.View;

import com.zhl.cbdialog.CBDialogBuilder;

/**
 * depresed!!!
 * Created by zhangxin on 2016/8/28 0028.
 * <p>
 * Description :作为设置频率的dialog来展示,拓展为支持自定义设置view的对话框
 */
public class FreqDialog extends CBDialogBuilder {
    public FreqDialog(Context context) {
        super(context);
    }

    @Override
    public CBDialogBuilder setView(View v) {
        getView(com.zhl.cbdialog.R.id.warning_frame).setVisibility(View.GONE);
        super.setView(v);
        return this;
    }
}
