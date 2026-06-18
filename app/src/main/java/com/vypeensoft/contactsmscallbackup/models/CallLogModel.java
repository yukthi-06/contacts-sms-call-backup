package com.vypeensoft.contactsmscallbackup.models;

public class CallLogModel {
    private String name; // Contact Name
    private String number; // Phone Number
    private String type; // Incoming, Outgoing, Missed, Rejected, Blocked
    private String duration; // Call duration in seconds
    private long date; // Date/Time timestamp

    public CallLogModel() {}

    public CallLogModel(String name, String number, String type, String duration, long date) {
        this.name = name;
        this.number = number;
        this.type = type;
        this.duration = duration;
        this.date = date;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number != null ? number : "";
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getType() {
        return type != null ? type : "";
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDuration() {
        return duration != null ? duration : "";
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }
}
