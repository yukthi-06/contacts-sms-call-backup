package com.vypeensoft.contactsmscallbackup.models;

public class SmsModel {
    private String address; // Sender/Receiver
    private String body;
    private long date;
    private String type; // Inbox, Sent, Draft, etc.
    private String threadId;

    public SmsModel() {}

    public SmsModel(String address, String body, long date, String type, String threadId) {
        this.address = address;
        this.body = body;
        this.date = date;
        this.type = type;
        this.threadId = threadId;
    }

    public String getAddress() {
        return address != null ? address : "";
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBody() {
        return body != null ? body : "";
    }

    public void setBody(String body) {
        this.body = body;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getType() {
        return type != null ? type : "";
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getThreadId() {
        return threadId != null ? threadId : "";
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }
}
