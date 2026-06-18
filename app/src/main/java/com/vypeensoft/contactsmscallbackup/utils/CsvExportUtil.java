package com.vypeensoft.contactsmscallbackup.utils;

import com.vypeensoft.contactsmscallbackup.models.CallLogModel;
import com.vypeensoft.contactsmscallbackup.models.ContactModel;
import com.vypeensoft.contactsmscallbackup.models.SmsModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CsvExportUtil {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public static String exportContacts(List<ContactModel> contacts) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name,Phone,Email,Organization\n");
        for (ContactModel contact : contacts) {
            sb.append(escapeCsv(contact.getDisplayName())).append(",")
              .append(escapeCsv(contact.getPhoneNumbers())).append(",")
              .append(escapeCsv(contact.getEmailAddresses())).append(",")
              .append(escapeCsv(contact.getOrganization())).append("\n");
        }
        return sb.toString();
    }

    public static String exportSms(List<SmsModel> smsList) {
        StringBuilder sb = new StringBuilder();
        sb.append("Address,Body,Date,Type\n");
        for (SmsModel sms : smsList) {
            String dateStr = dateFormat.format(new Date(sms.getDate()));
            sb.append(escapeCsv(sms.getAddress())).append(",")
              .append(escapeCsv(sms.getBody())).append(",")
              .append(escapeCsv(dateStr)).append(",")
              .append(escapeCsv(sms.getType())).append("\n");
        }
        return sb.toString();
    }

    public static String exportCallLogs(List<CallLogModel> callLogs) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name,Number,Type,Duration,Date\n");
        for (CallLogModel log : callLogs) {
            String dateStr = dateFormat.format(new Date(log.getDate()));
            sb.append(escapeCsv(log.getName())).append(",")
              .append(escapeCsv(log.getNumber())).append(",")
              .append(escapeCsv(log.getType())).append(",")
              .append(escapeCsv(log.getDuration())).append(",")
              .append(escapeCsv(dateStr)).append("\n");
        }
        return sb.toString();
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
