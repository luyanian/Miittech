package com.miittech.you.net.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Administrator on 2017/11/6.
 */

public class SoundListResponse extends BaseResponse {

    private List<SourndlistBean> sourndlist;

    public List<SourndlistBean> getSourndlist() {
        return sourndlist;
    }

    public void setSourndlist(List<SourndlistBean> sourndlist) {
        this.sourndlist = sourndlist;
    }

    public static class SourndlistBean {
        /**
         * id : 1
         * name : Don't Cry
         * url : native://donotcry.mp3
         */

        private int id;
        private String name;
        @SerializedName("url")
        private String urlX;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrlX() {
            return urlX;
        }

        public void setUrlX(String urlX) {
            this.urlX = urlX;
        }
    }
}
