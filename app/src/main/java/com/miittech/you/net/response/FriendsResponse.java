package com.miittech.you.net.response;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Administrator on 2017/9/27.
 */

public class FriendsResponse extends BaseResponse {


    private List<FriendlistBean> friendlist;

    public List<FriendlistBean> getFriendlist() {
        return friendlist;
    }

    public void setFriendlist(List<FriendlistBean> friendlist) {
        this.friendlist = friendlist;
    }

    public static class FriendlistBean implements Serializable{
        /**
         * friendid : 16
         * nickname : ZmZzZA==

         * groupid : 0
         * headimg : https://openservice.wisdomsky.cn/qianji/imgs/YOU161505963172589mQ1AJBai6VhlAsYlyFt6p0lEXivkFOHt.jpg
         * state : 4
         * starttime : 20170927151344
         * note :
         */

        private String friendid;
        private String nickname;
        private int groupid;
        private String headimg;
        private int state;
        private String starttime;
        private String note;

        public String getFriendid() {
            return friendid;
        }

        public void setFriendid(String friendid) {
            this.friendid = friendid;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public int getGroupid() {
            return groupid;
        }

        public void setGroupid(int groupid) {
            this.groupid = groupid;
        }

        public String getHeadimg() {
            return headimg;
        }

        public void setHeadimg(String headimg) {
            this.headimg = headimg;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }

        public String getStarttime() {
            return starttime;
        }

        public void setStarttime(String starttime) {
            this.starttime = starttime;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }
}
