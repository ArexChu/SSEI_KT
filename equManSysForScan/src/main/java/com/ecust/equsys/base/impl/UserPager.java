package com.ecust.equsys.base.impl;

import android.app.Activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.ecust.equsys.R;
import com.ecust.equsys.base.BasePager;
import com.ecust.equsys.utils.DeviceControl;
import com.ecust.equsys.utils.RFIDReader;
import com.ecust.equsys.utils.CacheUtils;
import com.android.uhflibs.as3992_native;
import com.ecust.equsys.utils.ReadThread;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ViewInject;

import java.io.IOException;

import static android.content.Context.POWER_SERVICE;


public class UserPager extends BasePager {

	public UserPager(Activity activity) {
		super(activity);
	}

	public static final String USERNAME = "userName";
	public static final String PASSWORD = "password";
	public static final String SAVEPASSWORD = "savePassword";

	//传入平台
    // private int which_choose;// = 0;
    private as3992_native native_method;
    private DeviceControl DevCtrl;
    private ReadThread rthread;

    private PowerManager pM = null;
    private PowerManager.WakeLock wK = null;

    private int init_progress = 0;

	@ViewInject(R.id.login_edit_account)
	private EditText et_userName;//用户名

	@ViewInject(R.id.login_edit_pwd)
	private EditText et_password;//密码

	@ViewInject(R.id.login_btn_login)
	private Button loginButton;//提交按钮

	@ViewInject(R.id.login_cb_savepwd)
	private CheckBox savedBox;//是否保存密码

	@ViewInject(R.id.button_connect)
	private Button button_connect;//连接

	@ViewInject(R.id.button_disconnect)
	private Button button_disconnect;//断开

	@Override
	public void initData() {
		tvTitle.setText(R.string.content_user);
		ibMenu.setVisibility(View.GONE);

		View view = View.inflate(mActivity, R.layout.user_login, null);
		ViewUtils.inject(this, view);

		//连接监听
		button_connect.setOnClickListener(new UserPager.ConnectClickListener());
		//断开监听
		button_disconnect.setOnClickListener(new UserPager.DisconnectClickListener());

		/**
		 * 1：从SharedPreferences中取出用户名，如果不为空，为用户名赋初值
		 * 2：从SharedPreferences中取出密码，如果不为空，为密码赋初值
		 * * 2：从SharedPreferences中取出是否保存
		 * 3：为提交按钮添加点击事件：（1）保存用户名 （2）保存密码 （3）保存是否需要保存密码
		 */
		String userName = CacheUtils.getString(mActivity, USERNAME, "0");
		String passWord = CacheUtils.getString(mActivity, PASSWORD, "0");
		Boolean savePass = CacheUtils.getBoolean(mActivity, SAVEPASSWORD, false);
		if (!userName.equals("0")) {
			et_userName.setText(userName);
		}
		if (!passWord.equals("0")) {
			et_password.setText(passWord);
		}
		savedBox.setChecked(savePass);
		loginButton.setOnClickListener(new LoginClickListener());

		flContent.addView(view);
	}

	private class LoginClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			//禁用按钮的点击事件，以免多次点击
			loginButton.setClickable(false);
			if (et_userName.length() == 0 || et_password.length() == 0) {
				Toast.makeText(mActivity, "用户名或密码不能为空", Toast.LENGTH_SHORT).show();
			} else {
				//记录各种的状态
				CacheUtils.putString(mActivity, USERNAME, et_userName.getText().toString());
				CacheUtils.putString(mActivity, PASSWORD, et_password.getText().toString());
				CacheUtils.putBoolean(mActivity, SAVEPASSWORD, savedBox.isChecked());
				Toast.makeText(mActivity, "提交成功", Toast.LENGTH_SHORT).show();
			}
			//重新启用按钮
			loginButton.setClickable(true);

		}

	}

	//窗体事件
	class ConnectClickListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {
            native_method = RFIDReader.getNative();
            if (native_method.OpenComPort("/dev/ttyMT2") != 0) {
                return;
            }
            init_progress++;
            try {
                DevCtrl = RFIDReader.getDevCtrl();

            } catch (SecurityException e) {
                e.printStackTrace();
            }
            try {
                DevCtrl.PowerOnDevice();
                Log.i("as3992_destroy", "poweron");
            } catch (IOException e) {
            }
            init_progress++;

            rthread = RFIDReader.getReadThread();
            rthread.start();

            pM = (PowerManager) mActivity.getSystemService(POWER_SERVICE);
            if (pM != null) {
                wK = pM.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                        | PowerManager.ON_AFTER_RELEASE, "lock3992");
                if (wK != null) {
                    wK.acquire();
                    init_progress++;
                }
            }

            if (init_progress == 2) {
                Log.w("3992_6C", "wake lock init failed");
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            byte[] soft_ver = native_method.get_version(0);
            if (soft_ver == null) {
                Toast.makeText(mActivity, "Communication Error",
                        Toast.LENGTH_SHORT).show();
            } else {
                if (power_antenna(true) == 0) {
                    Toast.makeText(mActivity, "连接成功",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mActivity, "连接失败",
                            Toast.LENGTH_SHORT).show();
                }
                set_hopping_freq(920625, 924375, 750, -40);
                native_method.set_alloc_param(1, 10000, 0);
            }

            }

		}


    class DisconnectClickListener implements OnClickListener {

        @Override
        public void onClick(View arg0) {

            switch (init_progress) {
                case 3:
                    wK.release();
                case 2:
                    rthread.interrupt();
                    try {
                        DevCtrl.PowerOffDevice();
                        Log.i("as3992_destroy", "write poweroff cmd to file");
                        Toast.makeText(mActivity, "断开读写器",
                                Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                case 1:
                    native_method.CloseComPort();
                case 0:
                default:
                    init_progress = 0;
            }

        }

    }

    int power_antenna(boolean power) {
        int retry = 0;
        while (native_method.set_antenna_power(power) != 0) {
            if ((++retry) >= 5) {
                return -1;
            }
        }
        return 0;
    }

    int set_hopping_freq(int start, int stop, int increment, int rssi)// , int
    // id)
    // //in
    // my
    // gui
    // program
    // don't
    // use
    // profile
    // id.
    // Just
    // set
    // it to
    // zero.
    {
        int retry = 0, reval;
        while ((reval = native_method.set_freq(start, stop, increment, rssi, 0)) != 0) {
            if (reval == -2) {
                return -2;
            }
            Log.e("as9932", "setfreq retry");
            if ((++retry) >= 5) {
                return -1;
            }
        }
        return 0;
    }


}
