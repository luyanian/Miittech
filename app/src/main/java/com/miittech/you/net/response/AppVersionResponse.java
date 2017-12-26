package com.miittech.you.net.response;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/12/26.
 */

public class AppVersionResponse extends BaseResponse {
    private Version version;

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public static class Version implements Serializable {
        private String min;
        private String last;
        private String lasturl;

        public String getMin() {
            return min;
        }

        public void setMin(String min) {
            this.min = min;
        }

        public String getLast() {
            return last;
        }

        public void setLast(String last) {
            this.last = last;
        }

        public String getLasturl() {
            return lasturl;
        }

        public void setLasturl(String lasturl) {
            this.lasturl = lasturl;
        }
    }
}
