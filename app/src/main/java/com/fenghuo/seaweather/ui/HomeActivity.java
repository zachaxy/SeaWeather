package com.fenghuo.seaweather.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import com.fenghuo.seaweather.FHApplication;
import com.fenghuo.seaweather.R;
import com.zhl.cbdialog.CBDialogBuilder;

/**
 * Created by zhangxin on 2016/8/25 0025.
 * <p>
 * Description :
 * 整个系统的主页面,包含了三个fragment
 */
public class HomeActivity extends Activity {

    public FHApplication mFHApplication;
    /*
 * 用于展示海图的Fragment
 */
    private MapFragment mapFragment;

    /*
     * 用于展示设置的Fragment
     */
    private SettingFragment settingFragment;

    /*
     * 用于展示报警数据的Fragment
     */
    private DBFragment dbFragment;

    /*
     * 在侧边栏中地图选项所在的区域
     */
    private View mapLayout;

    /*
     * 在侧边栏中参数选项所在的区域
     */
    private View settingLayout;

    /*
     * 在侧边栏中历史记录选项所占的区域
     */
    private View dbLayout;

    /*
     * 在tab界面上显示海图图标的控件,主要用于选中后的颜色变换
     */
    private ImageView mapImage;

    /*
     * 在tab界面上显示设置图标的控件,主要用于选中后的颜色变换
     */
    private ImageView settingImage;

    /*
     * 在tab界面上显示数据查询图标的控件,主要用于选中后的颜色变换
     */
    private ImageView dbImage;

    /*
     * 用于对fragment的管理
     */
    private FragmentManager fragmentManager;

    //标志fragment的index,必须初始化为-1
    private int currentIndex = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_home);
        mFHApplication = (FHApplication) getApplication();
        initViews();
    }


    private void initViews() {
        mapImage = (ImageView) findViewById(R.id.map_image);
        settingImage = (ImageView) findViewById(R.id.setting_image);
        dbImage = (ImageView) findViewById(R.id.db_image);

        fragmentManager = getFragmentManager();
        setTabSelection(mapImage);
    }


    // TODO: 2016/9/25 0025 该方法封装的并不理想;
    public void setTabSelection(View v) {
        clearSelection();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        switch (v.getId()) {
            case R.id.map_image:
                if (currentIndex == 0) {
                    mapImage.setImageResource(R.drawable.map_select);
                    break;
                }
                if (settingFragment != null) {
                    transaction.detach(settingFragment);
                }
                if (dbFragment != null) {
                    transaction.detach(dbFragment);
                }
                mapImage.setImageResource(R.drawable.map_select);
                if (mapFragment == null) {
                    mapFragment = new MapFragment();
                    transaction.add(R.id.content, mapFragment);

                } else {
                    transaction.show(mapFragment);
                }
                currentIndex = 0;
                break;
            case R.id.setting_image:
                if (currentIndex == 1) {
                    settingImage.setImageResource(R.drawable.admin_select);
                    break;
                }
                if (mapFragment != null) {
                    transaction.hide(mapFragment);
                }
                if (dbFragment != null) {
                    transaction.detach(dbFragment);
                }
                settingImage.setImageResource(R.drawable.admin_select);
                if (settingFragment == null) {
                    settingFragment = new SettingFragment();
                    transaction.add(R.id.content, settingFragment);
                } else {
                    transaction.attach(settingFragment);
                }
                currentIndex = 1;
                break;
            case R.id.db_image:
                if (currentIndex == 2) {
                    dbImage.setImageResource(R.drawable.db_select);
                    break;
                }
                if (mapFragment != null) {
                    transaction.hide(mapFragment);
                }
                if (settingFragment != null) {
                    transaction.detach(settingFragment);
                }
                dbImage.setImageResource(R.drawable.db_select);
                if (dbFragment == null) {
                    dbFragment = new DBFragment();
                    transaction.add(R.id.content, dbFragment);

                } else {
                    transaction.attach(dbFragment);
                }
                currentIndex = 2;
                break;
        }
        transaction.commit();
    }

    private void clearSelection() {
        mapImage.setImageResource(R.drawable.map_unselect);
        settingImage.setImageResource(R.drawable.admin_unselect);
        dbImage.setImageResource(R.drawable.db_unselect);
    }

    @Override
    public void onBackPressed() {
        // TODO: 2016/9/9 0009 退出时其他线程的开关是否一同关闭
        new CBDialogBuilder(HomeActivity.this)
                .setTouchOutSideCancelable(false)
                .showCancelButton(true)
                .setTitle("退出程序")
                .setMessage("退出将无法获取服务,是否退出程序?")
                .setConfirmButtonText("退出程序")
                .setCancelButtonText("暂不退出")
                .setDialogAnimation(CBDialogBuilder.DIALOG_ANIM_SLID_BOTTOM)
                .setButtonClickListener(true, new CBDialogBuilder.onDialogbtnClickListener() {
                    @Override
                    public void onDialogbtnClick(Context context, Dialog dialog, int whichBtn) {
                        switch (whichBtn) {
                            case BUTTON_CONFIRM:
                                if (mFHApplication.serialPort != null) {
                                    mFHApplication.serialPort.close();
                                }
                                finish();
                                break;
                            case BUTTON_CANCEL:
                                //null
                                break;
                            default:
                                break;
                        }
                    }
                })
                .create().show();
    }


}
