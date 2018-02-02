package com.miittech.you.net.response;

/**
 * Created by Administrator on 2018/2/2.
 */

public class BleVersionResponse extends BaseResponse {

    /**
     * firmware : {"devtype":1,"firmware":"v_3.0.6.1","software":"v_3.50.1.54","dl_url":"https://www.yoowoo.cn/apps/yoowoo/firmware/yoowoo_fw02.img","availtime":"20180130","status":1}
     */

    private FirmwareBean firmware;

    public FirmwareBean getFirmware() {
        return firmware;
    }

    public void setFirmware(FirmwareBean firmware) {
        this.firmware = firmware;
    }

    public static class FirmwareBean {
        /**
         * devtype : 1
         * firmware : v_3.0.6.1
         * software : v_3.50.1.54
         * dl_url : https://www.yoowoo.cn/apps/yoowoo/firmware/yoowoo_fw02.img
         * availtime : 20180130
         * status : 1
         */

        private int devtype;
        private String firmware;
        private String software;
        private String dl_url;
        private String availtime;
        private int status;

        public int getDevtype() {
            return devtype;
        }

        public void setDevtype(int devtype) {
            this.devtype = devtype;
        }

        public String getFirmware() {
            return firmware;
        }

        public void setFirmware(String firmware) {
            this.firmware = firmware;
        }

        public String getSoftware() {
            return software;
        }

        public void setSoftware(String software) {
            this.software = software;
        }

        public String getDl_url() {
            return dl_url;
        }

        public void setDl_url(String dl_url) {
            this.dl_url = dl_url;
        }

        public String getAvailtime() {
            return availtime;
        }

        public void setAvailtime(String availtime) {
            this.availtime = availtime;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }
    }
}
