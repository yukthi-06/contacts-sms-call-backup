package com.vypeensoft.contactsmscallbackup.services;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;

import com.vypeensoft.contactsmscallbackup.models.CallLogModel;
import com.vypeensoft.contactsmscallbackup.utils.Logger;

import java.util.ArrayList;
import java.util.List;

public class CallLogBackupService {

    public List<CallLogModel> readCallLogs(Context context) {
        List<CallLogModel> callLogsList = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();

        String[] projection = new String[] {
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION,
            CallLog.Calls.DATE
        };

        Cursor cursor = cr.query(CallLog.Calls.CONTENT_URI, projection, null, null, CallLog.Calls.DATE + " DESC");
        if (cursor == null) {
            Logger.w("CallLog cursor is null");
            return callLogsList;
        }

        try {
            int nameCol = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
            int numCol = cursor.getColumnIndex(CallLog.Calls.NUMBER);
            int typeCol = cursor.getColumnIndex(CallLog.Calls.TYPE);
            int durCol = cursor.getColumnIndex(CallLog.Calls.DURATION);
            int dateCol = cursor.getColumnIndex(CallLog.Calls.DATE);

            while (cursor.moveToNext()) {
                String name = cursor.getString(nameCol);
                String number = cursor.getString(numCol);
                int typeInt = cursor.getInt(typeCol);
                String duration = cursor.getString(durCol);
                long dateVal = cursor.getLong(dateCol);

                if (name == null || name.trim().isEmpty()) {
                    name = "Unknown";
                }

                String typeStr = "Unknown";
                switch (typeInt) {
                    case CallLog.Calls.INCOMING_TYPE:
                        typeStr = "Incoming";
                        break;
                    case CallLog.Calls.OUTGOING_TYPE:
                        typeStr = "Outgoing";
                        break;
                    case CallLog.Calls.MISSED_TYPE:
                        typeStr = "Missed";
                        break;
                    case CallLog.Calls.REJECTED_TYPE:
                        typeStr = "Rejected";
                        break;
                    case CallLog.Calls.BLOCKED_TYPE:
                        typeStr = "Blocked";
                        break;
                    case CallLog.Calls.VOICEMAIL_TYPE:
                        typeStr = "Voicemail";
                        break;
                }

                callLogsList.add(new CallLogModel(name, number, typeStr, duration, dateVal));
            }
        } catch (Exception e) {
            Logger.e("Error reading call log list", e);
        } finally {
            cursor.close();
        }

        Logger.i("Total call logs queried: " + callLogsList.size());
        return callLogsList;
    }
}
