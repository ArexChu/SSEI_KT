package com.ecust.equsys.base.impl.vesseldetail;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ecust.equsys.R;
import com.ecust.equsys.base.MenuDetailBasePager;
import com.ecust.equsys.utils.CacheUtils;
import com.ecust.equsys.utils.DbHelper;
import com.ecust.equsys.utils.HttpHelper;
import com.ecust.equsys.utils.RFIDReader;
import com.android.uhflibs.as3992_native;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ViewInject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class VesselTagLoadPager extends MenuDetailBasePager{

	public VesselTagLoadPager(Activity activity) {
		super(activity);
	}
	public static final String VESSEL_TAG_ID = "vessel_tag_id" ;
	public static final String VESSEL_MISSION_ID = "vessel_mission_id" ;

	private DbHelper mDbHelper;

	private String vesselID;//管道号

	private String missionID;//任务号

	//传入平台
	private as3992_native native_method;

	@ViewInject(R.id.et_tag_load_equ_id)
	private EditText et_tagID;//标签号

	@ViewInject(R.id.et_mission_load_man_id)
	private EditText et_missionID;//业务号

	@ViewInject(R.id.bt_tag_search_equ_id)
	private Button button_searchop;//搜索按钮

	@ViewInject(R.id.bt_tag_read_equ_id)
	private Button button_readop;//读取按钮

	@ViewInject(R.id.bt_tag_write_equ_id)
	private Button button_writeop;//写入按钮

	@ViewInject(R.id.bt_tag_download_Info)
	private Button loadTagInfo;//从网络加载数据

	@ViewInject(R.id.bt_tag_load_equ_id)
	private Button saveTagButton;//提交按钮

	@ViewInject(R.id.bt_mission_load_man_id)
	private Button loadMissionButton;//提交按钮

	@Override
	public View initView() {
		View view = View.inflate(mActivity, R.layout.load_vessel_tag_info, null);
		ViewUtils.inject(this,view);
		return view;
	}

	@Override
	public void initData() {
		mDbHelper = DbHelper.getInstance(mActivity);

		//传入平台类型
		native_method = RFIDReader.getNative();

		//为搜索按钮添加按键监听
		button_searchop.setOnClickListener(new SearchClickListener());
		//为读取按钮添加按键监听
		button_readop.setOnClickListener(new ReadClickListener());
		//为写入按钮添加按键监听
		button_writeop.setOnClickListener(new WriteClickListener());

		//先判断该标签是否已经保存，如果有值，将其显示
		boolean isSaved = judgeIsSavedTagID();
		if (isSaved) {
			et_tagID.setText(vesselID);
		}

		//判断是否输入业务号，如果有，将其显示
		boolean isInputMissionID = judgeisInputMissionID();
		if (isInputMissionID) {
			et_missionID.setText(missionID);
		}
		//为从网络获取数据添加监听事件
		loadTagInfo.setOnClickListener(new LoadInfoFromInternet());

		//为提交按钮添加按键监听
		saveTagButton.setOnClickListener(new TagSubmitClickListener());
		//为提交按钮添加按键监听
		loadMissionButton.setOnClickListener(new MissionSubmitClickListener());
	}

	//判断是否输入标签号
	private boolean judgeIsSavedTagID() {
		vesselID = CacheUtils.getString(mActivity, VESSEL_TAG_ID, null);
		return !(vesselID == null);
	}

	/**
	 * 判断是否输入业务号
	 * @return
	 */
	private boolean judgeisInputMissionID() {
		//从内存中取值，如果有值，则说明之前存过。
		missionID = CacheUtils.getString(mActivity, VESSEL_MISSION_ID, null);
		if (missionID == null) {
			return false;
		}
		return true;
	}

	//从网络获取数据按钮的监听事件
	class LoadInfoFromInternet implements OnClickListener{

		@Override
		public void onClick(View v) {
			vesselID = et_tagID.getText().toString();
			if (vesselID == null) {
				Toast.makeText(mActivity, "请先读取或提交标签信息！", Toast.LENGTH_SHORT).show();
			}
			else {
				HttpHelper httpHelper = new HttpHelper(mActivity);
				httpHelper.loadVesselBasicInfo(vesselID);
			}
		}
	}

	//提交按钮的监听事件
	class TagSubmitClickListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			String tagString = et_tagID.getText().toString();
			if (tagString == null) {
				Toast.makeText(mActivity, "请先读取标签或手工填写标签信息", Toast.LENGTH_SHORT).show();
			}
			CacheUtils.putString(mActivity, VESSEL_TAG_ID, tagString);
			Toast.makeText(mActivity, "标签信息提交成功", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * 提交按钮的监听事件
	 * @author smxiang
	 *
	 */
	class MissionSubmitClickListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			String missionString = et_missionID.getText().toString();
			if (missionString == null) {
				Toast.makeText(mActivity, "业务号不能为空！", Toast.LENGTH_SHORT).show();
			}
			else {
				CacheUtils.putString(mActivity, VESSEL_MISSION_ID, missionString);
				et_missionID.setText(missionString);
				Toast.makeText(mActivity, "业务号提交成功", Toast.LENGTH_SHORT).show();
			}
		}
	}

	class SearchClickListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			VesselTagLoadPager.SearchTagDialog searchTag = new VesselTagLoadPager.SearchTagDialog(mActivity);
			searchTag.setTitle(R.string.tag_choose);
			searchTag.show();
		}
	}

	//写入标签信息监听
	class WriteClickListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {

			vesselID = et_tagID.getText().toString();
			if (vesselID == null) {
				Toast.makeText(mActivity, "请填写标签写入信息", Toast.LENGTH_SHORT).show();
			} else {
				CacheUtils.putString(mActivity, VESSEL_TAG_ID, vesselID);
			}

			byte[] pasw = new byte[4];

			byte[] wsize = new byte[2];
			wsize[0] = (byte) (vesselID.length() >> 8);
			wsize[1] = (byte) (vesselID.length());

			byte[] wdata = null;

			int retry = 0, writed = 0, reval = -1;
			// while(write_area(area, addr, count, pasw, cont) != 0)
			// while(native_method.write_area(area, addr, count, pasw, cont) != 0)
			do {
				if ((++retry) >= 5) {
					break;
				}

				reval = native_method.write_area(3, 0, 1, pasw, wsize);

				if (reval > 0) {
					if (reval == (1 - writed)) {
						break;
					}
					writed += reval;
					byte[] tp = new byte[(1 - writed) * 2];
					System.arraycopy(wsize, reval * 2, tp, 0, tp.length);
					wsize = tp;
				}

			} while (writed < 1);

			if (reval > 0) {
				if ((vesselID.length() % 2) != 0) {
					vesselID += "0";
				}
				try {
					wdata = vesselID.getBytes("gbk");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				//Log.v(TAG, "wdata：" + Arrays.toString(wdata));
			}

			retry = 0;
			writed = 0;
			reval = -1;
			if (wdata != null) {
				do {
					if ((++retry) >= 5) {
						break;
					}

					reval = native_method.write_area(3, 1, wdata.length/2, pasw, wdata);

					if (reval > 0) {
						if (reval == (wdata.length/2 - writed)) {
							break;
						}
						writed += reval;
						byte[] tp = new byte[(wdata.length/2 - writed) * 2];
						System.arraycopy(wdata, reval * 2, tp, 0, tp.length);
						wdata = tp;
					}

				} while (writed < wdata.length/2);
			}

			if (reval > 0) {
				Toast.makeText(mActivity, "写入标签成功",
						Toast.LENGTH_SHORT).show();
			} else
				Toast.makeText(mActivity, "写入标签失败",
						Toast.LENGTH_SHORT).show();

		}
	}

	//读取标签信息监听
	class ReadClickListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			int rDataSize = 0;
			int rBlockSize = 0;

			byte[] res = null;
			int retry = 0;// , reval = 0;
			do {
				if ((++retry) >= 10) {
					break;
				}
				// res = read_area(area, addr, count);
				res = native_method.read_area(3, 0, 1);
			} while (res == null);

			if (res != null) {
				rDataSize = (res[0] << 8) & 0xff00 | (res[1]) & 0x00ff;
				int rAddSize = rDataSize;
				//Log.v(TAG, "rDataSize：" + rDataSize);
				if ((rAddSize % 2) != 0) {
					rAddSize += 1;
					rBlockSize = rAddSize / 2;
				} else {
					rBlockSize = rAddSize / 2;
				}
				//Log.v(TAG, "rBlockSize：" + rBlockSize);
			}

			res = null;
			retry = 0;
			do {
				if ((++retry) >= 10) {
					break;
				}
				// res = read_area(area, addr, count);
				res = native_method.read_area(3, 1, rBlockSize);
			} while (res == null);

			if (res != null) {
				//Log.v(TAG, "rdata：" + Arrays.toString(rdata));
				String val = "";
				try {
					val = new String(res, "gbk");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				//Log.v(TAG, "val:" + val);
				if ((rDataSize % 2) != 0) {
					val = val.substring(0, rDataSize);
				}
				//读取标签内容
				et_tagID.setText(val);
				CacheUtils.putString(mActivity, VESSEL_TAG_ID, val);

				Toast.makeText(mActivity, "读取标签成功",
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(mActivity, "读取标签失败",
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	class SearchTagDialog extends Dialog implements
			android.view.View.OnClickListener, AdapterView.OnItemClickListener {

		private Button Cancle;
		private Button Action;
		private TextView Status;
		private ListView EpcList;
		private boolean inSearch = false;
		private String epcs[] = null;
		private List<VesselTagLoadPager.SearchTagDialog.EpcDataBase> firm = new ArrayList<VesselTagLoadPager.SearchTagDialog.EpcDataBase>();
		private VesselTagLoadPager.SearchTagDialog.SearchThread st;
		private Handler handler = null;
		private ArrayAdapter<VesselTagLoadPager.SearchTagDialog.EpcDataBase> adapter;
		private Context cont;
		private SoundPool soundPool;
		private int soundId;

		public SearchTagDialog(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
			cont = context;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.setreader);

			Cancle = (Button) findViewById(R.id.btn_search_cancle);
			Cancle.setOnClickListener(this);
			Action = (Button) findViewById(R.id.btn_search_action);
			Action.setOnClickListener(this);

			Status = (TextView) findViewById(R.id.textView_search_status);
			EpcList = (ListView) findViewById(R.id.listView_search_epclist);
			EpcList.setOnItemClickListener(this);

			soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
			if (soundPool == null) {
				Log.e("as3992", "Open sound failed");
			}
			soundId = soundPool.load("/system/media/audio/ui/VideoRecord.ogg", 0);
			Log.w("as3992_6C", "id is " + soundId);

			handler = new Handler() {
				@Override
/*				public void handleMessage(Message msg)
				{
					super.handleMessage(msg);
					if(msg.what == 1)
					{
						soundPool.play(soundId, 1, 1, 0, 0, 1);
						String tmp[] = epcs;
						int i, j;
						for(i = 0; i < tmp.length; i++)
						{
							for(j = 0; j < firm.size(); j++)
							{
								if(tmp[i].equals(firm.get(j).epc))
								{
									break;
								}
							}
							if(j == firm.size())
							{
								firm.add(new EpcDataBase(tmp[i], 1));
								Log.i("as3992_search", "now we have " + firm.size() + " cards");
							}
						}
					}
					adapter = new ArrayAdapter<As3992demoISO6C.SearchTagDialog.EpcDataBase>(
							cont, android.R.layout.simple_list_item_1, firm);
					EpcList.setAdapter(adapter);
				}*/
				public void handleMessage(Message msg)
				{
					super.handleMessage(msg);
					if (msg.what == 1) {
						soundPool.play(soundId, 1, 1, 0, 0, 1);
						String tmp[] = epcs;
						int i, j;
						for (i = 0; i < tmp.length; i++) {
							for (j = 0; j < firm.size(); j++) {
								if (tmp[i].equals(firm.get(j).epc)) {
									firm.get(j).valid = 5;
									break;
								}
							}
							if (j == firm.size()) {
								firm.add(new VesselTagLoadPager.SearchTagDialog.EpcDataBase(tmp[i], 5));
							}
						}
						for (i = 0; i < firm.size(); i++) {
							for (j = 0; j < tmp.length; j++) {
								if (firm.get(i).epc.equals(tmp[j])) {
									break;
								}
							}
							if (j == tmp.length) {
								if (--firm.get(i).valid == 0) {
									firm.remove(i);
								}
							}
						}
					} else {
						for (int i = 0; i < firm.size(); i++) {
							if (--firm.get(i).valid == 0) {
								firm.remove(i);
							}
						}
					}
					adapter = new ArrayAdapter<>(
							cont, android.R.layout.simple_list_item_1, firm);
					EpcList.setAdapter(adapter);
				}
			};
		}

		/*
		 * @Override public void onBackPressed() { // TODO Auto-generated method
		 * stub if(inSearch) { inSearch = false; } super.onBackPressed(); }
		 */

		@Override
		protected void onStop() {
			// TODO Auto-generated method stub
			Log.w("stop", "im stopping");
			if (inSearch) {
				inSearch = false;
			}
			soundPool.release();
			super.onStop();
		}

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (v == Cancle) {
				soundPool.release();
				power_antenna(false);
				dismiss();
			} else if (v == Action) {
				if (inSearch) {
					// st.interrupt();
					inSearch = false;
					power_antenna(false);
					Action.setText(R.string.Start_Search_Btn);
					Cancle.setEnabled(true);
				} else {
					st = new VesselTagLoadPager.SearchTagDialog.SearchThread();
					inSearch = true;
					power_antenna(true);
					st.start();
					Action.setText(R.string.Stop_Search_Btn);
					Cancle.setEnabled(false);
				}
			}

		}

		class EpcDataBase {
			String epc;
			int valid;

			public EpcDataBase(String e, int v) {
				// TODO Auto-generated constructor stub
				epc = e;
				valid = v;
			}

			@Override
			public String toString() {
				return epc;
			}
		}

		class SearchThread extends Thread {
			@Override
			public void run() {
				super.run();
				// while(!isInterrupted()) {
				while (inSearch) {
					Message msg = new Message();
					// epcs = native_method.search_UHF();
					epcs = search_UHF();
					if (epcs != null) {
						msg.what = 1;
						handler.sendMessage(msg);
					} else {
						msg.what = 0;
						handler.sendMessage(msg);
					}
					try {
						sleep(200);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						this.interrupt();
//						Log.i("as3992",
//								"********************************************************");
//						Log.w("as3992", "test w color");
//						Log.v("as3992", "test v color");
					}
				}
				Log.d("as3992", "search thread is interrupted");
			}
		}

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
								long arg3) {
			// TODO Auto-generated method stub
			if (inSearch) {
				return;
			}
			// int res = native_method.select_UHF(firm.get(arg2).epc);
			int res = select_UHF(firm.get(arg2).epc);
			if (res == 0) {
				dismiss();
			} else {
				Toast.makeText(mActivity, "选取标签失败",
						Toast.LENGTH_SHORT).show();
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

	String[] search_UHF() {
		List<as3992_native.Tag_Data_Rssi> cnt;
		int retry = 0;
		do {
//			Log.e("as3992", "search retry");
			cnt = native_method.search_card_rssi();
			if ((++retry) >= 10) {
				return null;
			}
		} while (cnt == null);
		int noc = cnt.size();
		String[] out = new String[noc];
		for (int i = 0; i < noc; i++) {
			out[i] = new String();
			int loe = cnt.get(i).tdata.epc.length;
			for (int j = 0; j < loe; j++) {
				out[i] += String.format("%02x ", cnt.get(i).tdata.epc[j]);
			}
		}
		return out;
	}

	int select_UHF(String epc) {
		StringTokenizer sepc = new StringTokenizer(epc);
		byte[] eepc = new byte[sepc.countTokens()];
		int index = 0;
		while (sepc.hasMoreTokens()) {
			try {
				eepc[index++] = (byte) Integer.parseInt(sepc.nextToken(), 16);
			} catch (NumberFormatException p) {
				return -1;
			}
		}
		int retry = 0;
		do {
			Log.e("as3992", "select retry");
			if ((++retry) >= 10) {
				return -1;
			}
		} while (native_method.select_card(eepc) != 0);
		return 0;
	}
}
