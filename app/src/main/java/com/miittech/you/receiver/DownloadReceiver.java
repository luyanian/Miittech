package com.miittech.you.receiver;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.FileProvider;

import com.luck.picture.lib.permissions.RxPermissions;
import com.miittech.you.App;
import com.ryon.mutils.AppUtils;
import com.ryon.mutils.CrashUtils;
import com.ryon.mutils.LogUtils;

import java.io.FileNotFoundException;
import java.time.Instant;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

public class DownloadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            installApk(context, id);
        } else if (intent.getAction().equals(DownloadManager.ACTION_NOTIFICATION_CLICKED)) {
            // DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);  
            //获取所有下载任务Ids组  
            //long[] ids = intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS);  
            ////点击通知栏取消所有下载  
            //manager.remove(ids);  
            //Toast.makeText(context, "下载任务已取消", Toast.LENGTH_SHORT).show();  
            //处理 如果还未完成下载，用户点击Notification ，跳转到下载中心  
            Intent viewDownloadIntent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
            viewDownloadIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(viewDownloadIntent);
        }
    }

    public synchronized void installApk(Context context, long downloadApkId) {
        DownloadManager dManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadApkId);
        Cursor c = dManager.query(query);
        if(c.moveToFirst()) {
            //获取文件下载路径
            String filename = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
            //如果文件名不为空，说明已经存在了，拿到文件名想干嘛都好
            if(filename != null){
                LogUtils.d("=====", "下载完成的文件名为："+filename);
                //执行安装
                Intent install = new Intent(Intent.ACTION_VIEW);
                install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                install.addFlags(Intent.FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS);
                install.setDataAndType(Uri.parse("file://" + filename),"application/vnd.android.package-archive");
                context.getApplicationContext().startActivity(install);
            }
        }

//        Intent install = new Intent(Intent.ACTION_VIEW);
//        Uri downloadFileUri = dManager.getUriForDownloadedFile(downloadApkId);
//        if (downloadFileUri != null) {
//            LogUtils.d("DownloadManager", downloadFileUri.toString());
//            install.setDataAndType(downloadFileUri, "application/vnd.android.package-archive");
//            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            install.addCategory(Intent.CATEGORY_DEFAULT);
//            install.addFlags(Intent.FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS);
//            context.startActivity(install);
//        } else {
//            LogUtils.e("DownloadManager", "download error");
//        }
    }
}