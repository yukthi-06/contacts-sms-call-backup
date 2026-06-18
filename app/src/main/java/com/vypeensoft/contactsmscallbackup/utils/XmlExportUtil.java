package com.vypeensoft.contactsmscallbackup.utils;

import com.vypeensoft.contactsmscallbackup.models.CallLogModel;
import com.vypeensoft.contactsmscallbackup.models.ContactModel;
import com.vypeensoft.contactsmscallbackup.models.SmsModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class XmlExportUtil {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public static String exportContacts(List<ContactModel> contacts) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<contacts>\n");
        for (ContactModel contact : contacts) {
            sb.append("    <contact>\n")
              .append("        <name>").append(escapeXml(contact.getDisplayName())).append("</name>\n")
              .append("        <phone>").append(escapeXml(contact.getPhoneNumbers())).append("</phone>\n")
              .append("        <email>").append(escapeXml(contact.getEmailAddresses())).append("</email>\n")
              .append("        <organization>").append(escapeXml(contact.getOrganization())).append("</organization>\n")
              .append("        <notes>").append(escapeXml(contact.getNotes())).append("</notes>\n")
              .append("    </contact>\n");
        }
        sb.append("</contacts>\n");
        return sb.toString();
    }

    public static String exportSms(List<SmsModel> smsList) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<sms_messages>\n");
        for (SmsModel sms : smsList) {
            String dateStr = dateFormat.format(new Date(sms.getDate()));
            sb.append("    <sms>\n")
              .append("        <address>").append(escapeXml(sms.getAddress())).append("</address>\n")
              .append("        <body>").append(escapeXml(sms.getBody())).append("</body>\n")
              .append("        <date>").append(escapeXml(dateStr)).append("</date>\n")
              .append("        <type>").append(escapeXml(sms.getType())).append("</type>\n")
              .append("        <thread_id>").append(escapeXml(sms.getThreadId())).append("</thread_id>\n")
              .append("    </sms>\n");
        }
        sb.append("</sms_messages>\n");
        return sb.toString();
    }

    public static String exportCallLogs(List<CallLogModel> callLogs) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<call_logs>\n");
        for (CallLogModel log : callLogs) {
            String dateStr = dateFormat.format(new Date(log.getDate()));
            sb.append("    <call>\n")
              .append("        <name>").append(escapeXml(log.getName())).append("</name>\n")
              .append("        <number>").append(escapeXml(log.getNumber())).append("</number>\n")
              .append("        <type>").append(escapeXml(log.getType())).append("</type>\n")
              .append("        <duration>").append(escapeXml(log.getDuration())).append("</duration>\n")
              .append("        <date>").append(escapeXml(dateStr)).append("</date>\n")
              .append("    </call>\n");
        }
        sb.append("</call_logs>\n");
        return sb.toString();
    }

    private static String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }
}
