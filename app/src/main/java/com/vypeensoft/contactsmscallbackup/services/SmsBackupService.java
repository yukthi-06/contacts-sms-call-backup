package com.vypeensoft.contactsmscallbackup.services;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.vypeensoft.contactsmscallbackup.models.SmsModel;
import com.vypeensoft.contactsmscallbackup.utils.Logger;

import java.util.ArrayList;
import java.util.List;

public class SmsBackupService {

    public List<SmsModel> readSms(Context context) {
        List<SmsModel> smsList = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse("content://sms");

        String[] projection = new String[] {
            "address",
            "body",
            "date",
            "type",
            "thread_id"
        };

        // Query SMS table
        Cursor cursor = cr.query(uri, projection, null, null, "date DESC");
        if (cursor == null) {
            Logger.w("SMS cursor is null");
            return smsList;
        }

        try {
            int addrCol = cursor.getColumnIndex("address");
            int bodyCol = cursor.getColumnIndex("body");
            int dateCol = cursor.getColumnIndex("date");
            int typeCol = cursor.getColumnIndex("type");
            int threadCol = cursor.getColumnIndex("thread_id");

            while (cursor.moveToNext()) {
                String address = cursor.getString(addrCol);
                String body = cursor.getString(bodyCol);
                long dateVal = cursor.getLong(dateCol);
                int typeInt = cursor.getInt(typeCol);
                String threadId = cursor.getString(threadCol);

                String typeStr = "Other";
                switch (typeInt) {
                    case 1:
                        typeStr = "Inbox";
                        break;
                    case 2:
                        typeStr = "Sent";
                        break;
                    case 3:
                        typeStr = "Draft";
                        break;
                    case 4:
                        typeStr = "Outbox";
                        break;
                    case 5:
                        typeStr = "Failed";
                        break;
                    case 6:
                        typeStr = "Queued";
                        break;
                }

                smsList.add(new SmsModel(address, body, dateVal, typeStr, threadId));
            }
        } catch (Exception e) {
            Logger.e("Error reading SMS list", e);
        } finally {
            cursor.close();
        }

        Logger.i("Total SMS queried: " + smsList.size());
        return smsList;
    }
}
