package com.miittech.you.net.code;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonParseException;
import com.miittech.you.R;
import com.ryon.mutils.ToastUtils;

import org.json.JSONException;

import java.net.ConnectException;

import retrofit2.HttpException;

/**
 * Created by Administrator on 2017/9/12.
 */

public class ErrorHandle {

    private static final int UNAUTHORIZED = 401;
    private static final int FORBIDDEN = 403;
    private static final int NOT_FOUND = 404;
    private static final int REQUEST_TIMEOUT = 408;
    private static final int INTERNAL_SERVER_ERROR = 500;
    private static final int BAD_GATEWAY = 502;
    private static final int SERVICE_UNAVAILABLE = 503;
    private static final int GATEWAY_TIMEOUT = 504;

    public void onError(Context context,Throwable e){
        Log.i("tag", "e.toString = " + e.toString());
        if (e instanceof HttpException) {
            HttpException httpException = (HttpException) e;
            switch (httpException.code()) {
                case UNAUTHORIZED:
                case FORBIDDEN:
                case NOT_FOUND:
                case REQUEST_TIMEOUT:
                case GATEWAY_TIMEOUT:
                case INTERNAL_SERVER_ERROR:
                case BAD_GATEWAY:
                case SERVICE_UNAVAILABLE:
                default:
                    //ex.code = httpException.code();
                    ToastUtils.showShort(context.getResources().getString(R.string.msg_net_error));
                    break;
            }
        } else if (e instanceof RuntimeException) {
            ToastUtils.showShort(context.getResources().getString(R.string.msg_server_error));
        } else if (e instanceof JsonParseException
                || e instanceof JSONException
                /*|| e instanceof ParseException*/) {
            ToastUtils.showShort(context.getResources().getString(R.string.msg_data_parse_error));
        } else if (e instanceof ConnectException) {
            ToastUtils.showShort(context.getResources().getString(R.string.msg_server_connect_error));
        } else if (e instanceof javax.net.ssl.SSLHandshakeException) {
            ToastUtils.showShort(context.getResources().getString(R.string.msg_ssl_error));
        } else {
            ToastUtils.showShort(context.getResources().getString(R.string.msg_unknown_error));
        }
    }
}
