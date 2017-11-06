package com.miittech.you.net;
import com.google.gson.JsonObject;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.net.response.FriendsResponse;
import com.miittech.you.net.response.RegisteCodeResponse;
import com.miittech.you.net.response.SoundListResponse;
import com.miittech.you.net.response.UserInfoResponse;

import io.reactivex.Observable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Url;

public interface ApiService {
    @POST()
    Observable<BaseResponse> postNetRequest(@Url String url, @Body RequestBody body);

    @POST()
    Observable<UserInfoResponse> postToGetUserInfo(@Url String url, @Body RequestBody body);

    @POST()
    Observable<FriendsResponse> postToGetFriendList(@Url String url, @Body RequestBody body);

    @POST()
    Observable<DeviceResponse> postDeviceOption(@Url String url, @Body RequestBody body);

    @POST()
    Observable<JsonObject> postNetRequestObject(@Url String url, @Body RequestBody body);

    @POST()
    Observable<SoundListResponse> postToGetSoundList(@Url String url, @Body RequestBody body);

    @Multipart
    @POST()
    Observable<BaseResponse> uploadImage(@Url String url, @Part MultipartBody.Part file);
}