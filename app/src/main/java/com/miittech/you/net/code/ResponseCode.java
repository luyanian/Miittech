package com.miittech.you.net.code;

/**
 * Created by Administrator on 2017/9/22.
 */

public class ResponseCode {
    public static final int successful = 0;
    public static final int invalid_request=-999998;//  请求报文重复
    public static final int invalid_sign=-70001; //签名错误
    public static final int invalid_postdata=-70002; //上传参数有误
    public static final int invalid_repost_or_timestate_error=-999998; //重复请求或timestamp问题
    public static final int invalid_token=-70100; //token无效
    public static final int invalid_sign_data=-70101; //sign计算问题

    public static final int server_error=-20001;//服务器错误
    public static final int user_notregister_userid=-20002;
    public static final int invalid_method=-20003;
    public static final int sms_senderror=-20004;
    public static final int server_inittoken=-20005;
    public static final int server_notfound=-20006; //没有绑定蓝牙贴片

    public static final int upload_filetoolarge=-20020;
    public static final int upload_notmultipart=-20021;
    public static final int upload_wrongfilesize=-20022;
    public static final int upload_wrongsha=-20023;
    public static final int upload_canotwritefile=-20024;
    public static final int upload_wrongfiletype=-20025;

    public static final int user_notexist=-10001;//用户不存在
    public static final int user_invalidpasswd=-10002;//密码错误
    public static final int user_weakpassword=-10003;
    public static final int user_dupregister=-10004;
    public static final int user_notsetpasswd=-10005;

    public static final int user_account_already_bind=-10010;//账号已被绑定
    public static final int device_invalidowner=-10100;
    public static final int device_binderr=-10101;
    public static final int device_alreadybind=-10102;
    public static final int device_notexist=-10103;
}
