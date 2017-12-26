package com.miittech.you.net;
import com.google.gson.JsonObject;
import com.miittech.you.net.response.AppVersionResponse;
import com.miittech.you.net.response.BaseResponse;
import com.miittech.you.net.response.DeviceDetailResponse;
import com.miittech.you.net.response.DeviceListResponse;
import com.miittech.you.net.response.FriendLocInfoResponse;
import com.miittech.you.net.response.FriendTraceResponse;
import com.miittech.you.net.response.FriendsResponse;
import com.miittech.you.net.response.LoginResponse;
import com.miittech.you.net.response.SoundListResponse;
import com.miittech.you.net.response.UserInfoResponse;

import io.reactivex.Observable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
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
    Observable<LoginResponse> postToLogin(@Url String url, @Body RequestBody body);

    @POST()
    Observable<FriendsResponse> postToGetFriendList(@Url String url, @Body RequestBody body);

    @POST()
    Observable<FriendLocInfoResponse> postToGetFriendLocList(@Url String url, @Body RequestBody body);

    @POST()
    Observable<DeviceListResponse> postDeviceOption(@Url String url, @Body RequestBody body);

    @POST()
    Observable<DeviceDetailResponse> postDeviceInfoOption(@Url String url, @Body RequestBody body);

    @POST()
    Observable<AppVersionResponse> postGetAppVersion(@Url String url, @Body RequestBody body);

    @POST()
    Observable<FriendTraceResponse> postGetFriendTraceList(@Url String url, @Body RequestBody body);

    @POST()
    Observable<JsonObject> postNetRequestObject(@Url String url, @Body RequestBody body);

    @POST()
    Observable<SoundListResponse> postToGetSoundList(@Url String url, @Body RequestBody body);

    @Multipart
    @POST()
    Observable<BaseResponse> uploadImage(@Url String url, @Part MultipartBody.Part file);
}