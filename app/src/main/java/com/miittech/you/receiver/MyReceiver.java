package com.miittech.you.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.miittech.you.activity.user.LoginRegisteActivity;
import com.miittech.you.utils.Common;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.dialog.MsgTipDialog;
import com.miittech.you.entity.JpushMsg;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.global.SPConst;
import com.miittech.you.impl.OnMsgTipOptions;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.BaseResponse;
import com.ryon.mutils.ActivityPools;
import com.ryon.mutils.AppUtils;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.SPUtils;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import cn.jpush.android.api.JPushInterface;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * 自定义接收器
 * 
 * 如果不定义这个 Receiver，则：
 * 1) 默认用户会打开主界面
 * 2) 接收不到自定义消息
 */
public class MyReceiver extends BroadcastReceiver {
	private static final String TAG = "JIGUANG-Example";

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			Bundle bundle = intent.getExtras();
			LogUtils.d(TAG, "[MyReceiver] onReceive - " + intent.getAction() + ", extras: " + printBundle(bundle));

			if (JPushInterface.ACTION_REGISTRATION_ID.equals(intent.getAction())) {
				String regId = bundle.getString(JPushInterface.EXTRA_REGISTRATION_ID);
				LogUtils.d(TAG, "[MyReceiver] 接收Registration Id : " + regId);
				//send the Registration Id to your server...
				sendRegistationIdToServer(context,regId);

			} else if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {
				LogUtils.d(TAG, "[MyReceiver] 接收到推送下来的自定义消息: " + bundle.getString(JPushInterface.EXTRA_MESSAGE));
				processCustomMessage(context, bundle);

			} else if (JPushInterface.ACTION_NOTIFICATION_RECEIVED.equals(intent.getAction())) {
				LogUtils.d(TAG, "[MyReceiver] 接收到推送下来的通知");
				int notifactionId = bundle.getInt(JPushInterface.EXTRA_NOTIFICATION_ID);
				LogUtils.d(TAG, "[MyReceiver] 接收到推送下来的通知的ID: " + notifactionId);

			} else if (JPushInterface.ACTION_NOTIFICATION_OPENED.equals(intent.getAction())) {
				LogUtils.d(TAG, "[MyReceiver] 用户点击打开了通知");

				//打开自定义的Activity
//				Intent i = new Intent(context, TestActivity.class);
//				i.putExtras(bundle);
//				//i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP );
//				context.startActivity(i);

			} else if (JPushInterface.ACTION_RICHPUSH_CALLBACK.equals(intent.getAction())) {
				LogUtils.d(TAG, "[MyReceiver] 用户收到到RICH PUSH CALLBACK: " + bundle.getString(JPushInterface.EXTRA_EXTRA));
				//在这里根据 JPushInterface.EXTRA_EXTRA 的内容处理代码，比如打开新的Activity， 打开一个网页等..

			} else if(JPushInterface.ACTION_CONNECTION_CHANGE.equals(intent.getAction())) {
				boolean connected = intent.getBooleanExtra(JPushInterface.EXTRA_CONNECTION_CHANGE, false);
				LogUtils.w(TAG, "[MyReceiver]" + intent.getAction() +" connected state change to "+connected);
			} else {
				LogUtils.d(TAG, "[MyReceiver] Unhandled intent - " + intent.getAction());
			}
		} catch (Exception e){

		}

	}

	private void sendRegistationIdToServer(final Context context, String regId) {
		if(!NetworkUtils.isConnected()){
			return;
		}
		Map param = new HashMap();
		param.put("regid", regId);
		param.put("ostype", "android");
		param.put("ver", AppUtils.getAppVersionName());
		String json = new Gson().toJson(param);
		PubParam pubParam = new PubParam(Common.getUserId());
		String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
		LogUtils.d("sign_unsha1", sign_unSha1);
		String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
		LogUtils.d("sign_sha1", sign);
		String path = HttpUrl.Api + "jpushid/" + pubParam.toUrlParam(sign);
		final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);
		ApiServiceManager.getInstance().buildApiService(context).postNetRequest(path, requestBody)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Consumer<BaseResponse>() {
					@Override
					public void accept(BaseResponse response) throws Exception {
						if (response.isSuccessful()) {

						}
					}
				}, new Consumer<Throwable>() {
					@Override
					public void accept(Throwable throwable) throws Exception {
						throwable.printStackTrace();
					}
				});
	}

	// 打印所有的 intent extra 数据
	private static String printBundle(Bundle bundle) {
		StringBuilder sb = new StringBuilder();
		for (String key : bundle.keySet()) {
			if (key.equals(JPushInterface.EXTRA_NOTIFICATION_ID)) {
				sb.append("\nkey:" + key + ", value:" + bundle.getInt(key));
			}else if(key.equals(JPushInterface.EXTRA_CONNECTION_CHANGE)){
				sb.append("\nkey:" + key + ", value:" + bundle.getBoolean(key));
			} else if (key.equals(JPushInterface.EXTRA_EXTRA)) {
				if (TextUtils.isEmpty(bundle.getString(JPushInterface.EXTRA_EXTRA))) {
					LogUtils.i(TAG, "This message has no Extra data");
					continue;
				}

				try {
					JSONObject json = new JSONObject(bundle.getString(JPushInterface.EXTRA_EXTRA));
					Iterator<String> it =  json.keys();

					while (it.hasNext()) {
						String myKey = it.next();
						sb.append("\nkey:" + key + ", value: [" +
								myKey + " - " +json.optString(myKey) + "]");
					}
				} catch (JSONException e) {
					LogUtils.e(TAG, "Get message extra JSON error!");
				}

			} else {
				sb.append("\nkey:" + key + ", value:" + bundle.getString(key));
			}
		}
		return sb.toString();
	}
	
	//send msg to MainActivity
	private void processCustomMessage(final Context context, Bundle bundle) {
		final Activity activity = ActivityPools.getTopActivity();
		if(activity!=null){
			String message = bundle.getString(JPushInterface.EXTRA_MESSAGE);
			Gson gson = new Gson();
			final JpushMsg jpushMsg = gson.fromJson(message,JpushMsg.class);
			if("addfriend".equals(jpushMsg.getContent_type())) {
				MsgTipDialog msgTipDialog = DialogUtils.getInstance().createMsgTipDialog(activity);
				msgTipDialog.setTitle(jpushMsg.getTitle());
				msgTipDialog.setMsg(jpushMsg.getMsg_content());
				int state = jpushMsg.getExtras().getState();
				if(state==1) {
					msgTipDialog.setLeftBtnText("拒绝");
					msgTipDialog.setRightBtnText("同意");
					msgTipDialog.setOnMsgTipOptions(new OnMsgTipOptions() {
						@Override
						public void onSure() {
							super.onSure();
							Common.AddFriendConfirm(context, jpushMsg.getExtras().getSourceid(), Params.METHOD.FRIEND_CONFIRM);
						}

						@Override
						public void onCancel() {
							super.onCancel();
							Common.AddFriendConfirm(context, jpushMsg.getExtras().getSourceid(), Params.METHOD.FRIEND_REFUSE);
						}
					});
				}else{
					msgTipDialog.hideLeftBtn();
					msgTipDialog.setRightBtnText("知道了");
				}
				msgTipDialog.show();
			}else if("shared".equals(jpushMsg.getContent_type())){
				int state = jpushMsg.getExtras().getState();
				MsgTipDialog msgTipDialog = DialogUtils.getInstance().createMsgTipDialog(activity);
				msgTipDialog.setTitle(jpushMsg.getTitle());
				msgTipDialog.setMsg(jpushMsg.getMsg_content());
				if(state==0) {
					msgTipDialog.setLeftBtnText("拒绝");
					msgTipDialog.setRightBtnText("同意");
					msgTipDialog.setOnMsgTipOptions(new OnMsgTipOptions() {
						@Override
						public void onSure() {
							super.onSure();
							Common.eventConfirm(activity, jpushMsg.getExtras().getEventid(), Params.METHOD.CONFIRM_YES);
						}

						@Override
						public void onCancel() {
							super.onCancel();
							Common.eventConfirm(activity, jpushMsg.getExtras().getEventid(), Params.METHOD.CONFIRM_NO);
						}
					});
				}else {
					msgTipDialog.hideLeftBtn();
					msgTipDialog.setRightBtnText("知道了");
				}
				msgTipDialog.show();
			}else if("login".equals(jpushMsg.getContent_type())){
				Intent cmd= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
				cmd.putExtra("cmd",IntentExtras.CMD.CMD_DEVICE_LIST_CLEAR);
				activity.sendBroadcast(cmd);
				SPUtils.getInstance(SPConst.USER.SP_NAME).clear();

				DialogUtils.getInstance().createMsgTipDialog(activity)
						.setTitle(jpushMsg.getTitle())
						.setMsg(jpushMsg.getMsg_content())
						.hideLeftBtn()
						.setRightBtnText("知道了")
						.setOnMsgTipOptions(new OnMsgTipOptions() {
							@Override
							public void onSure() {
								super.onSure();
								Intent intent = new Intent(activity,LoginRegisteActivity.class);
								activity.startActivity(intent);
								ActivityPools.finishAllExcept(LoginRegisteActivity.class);
							}
						})
						.show();

			}else if("normal".equals(jpushMsg.getContent_type())){
				MsgTipDialog msgTipDialog = DialogUtils.getInstance().createMsgTipDialog(activity);
				msgTipDialog.setTitle(jpushMsg.getTitle());
				msgTipDialog.setMsg(jpushMsg.getMsg_content());msgTipDialog.hideLeftBtn();
				msgTipDialog.setRightBtnText("知道了");
				msgTipDialog.show();
			}
		}
	}
}
