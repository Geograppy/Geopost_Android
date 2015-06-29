package com.geograppy.geopost.classes;

import java.util.UUID;

public class Geopost {

    public String Title;

    public String Text;

    public String UserName;

    public double Lon;

    public double Lat;

    public UUID ConversationId;

    public String TimeAgo;

    public void setTimeAgo(String value){
        if (value.contains(".") && (value.indexOf(".") < value.indexOf(":"))){
            int index = value.indexOf(".");
            int endIndex = index--;
            setTimeAgoFromDays(value.substring(0, endIndex));
        }
        else if (!value.substring(0,2).equals("00")) setTimeAgoHours(value.substring(0,2));
        else if (!value.substring(3,5).equals("00")) setTimeAgoMinutes(value.substring(3,5));
        else TimeAgo = "just now";
    }

    private void setTimeAgoFromDays(String value){
        int days = Integer.parseInt(value);
        if (Math.floor(days/365) > 1) {
            int years = (int) Math.floor(days/365);
            TimeAgo = Integer.toString(years) + " years ago";
        }
        else if ((int) Math.floor(days/365) == 1) {
            int years = (int) Math.floor(days/365);
            TimeAgo = Integer.toString(years) + " year ago";
        }
        else if (Math.floor(days/30) > 1) {
            int months = (int) Math.floor(days/30);
            TimeAgo = Integer.toString(months) + " months ago";
        }
        else if ((int) Math.floor(days/30) == 1) {
            int months = (int) Math.floor(days/30);
            TimeAgo = Integer.toString(months) + " month ago";
        }
        else if (Math.floor(days/7) > 1) {
            int weeks = (int) Math.floor(days/7);
            TimeAgo = Integer.toString(weeks) + " weeks ago";
        }
        else if ((int) Math.floor(days/7) == 1) {
            int weeks = (int) Math.floor(days/7);
            TimeAgo = Integer.toString(weeks) + " week ago";
        }
        else if (days > 1) TimeAgo = Integer.toString(days) + " days ago";
        else TimeAgo = "1 day ago";
    }

    private void setTimeAgoHours(String value){
        int hours = Integer.parseInt(value);
        if (hours > 1) TimeAgo = Integer.toString(hours) + " hours ago";
        else TimeAgo = "1 hour ago";
    }

    private void setTimeAgoMinutes(String value){
        int minutes = Integer.parseInt(value);
        if (minutes > 1) TimeAgo = Integer.toString(minutes) + " minutes ago";
        else TimeAgo = "1 minute ago";
    }
}
