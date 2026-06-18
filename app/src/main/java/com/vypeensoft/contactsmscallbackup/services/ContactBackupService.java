package com.vypeensoft.contactsmscallbackup.services;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;

import com.vypeensoft.contactsmscallbackup.models.ContactModel;
import com.vypeensoft.contactsmscallbackup.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactBackupService {

    public List<ContactModel> readContacts(Context context) {
        List<ContactModel> contactsList = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        
        // Define projection for ContactsContract.Data
        String[] projection = new String[] {
            Data.CONTACT_ID,
            Data.DISPLAY_NAME,
            Data.MIMETYPE,
            CommonDataKinds.Phone.NUMBER,
            CommonDataKinds.Email.ADDRESS,
            CommonDataKinds.Organization.COMPANY,
            CommonDataKinds.Note.NOTE
        };

        // We only care about specific mimetypes
        String selection = Data.MIMETYPE + "=? OR " +
                           Data.MIMETYPE + "=? OR " +
                           Data.MIMETYPE + "=? OR " +
                           Data.MIMETYPE + "=?";
                           
        String[] selectionArgs = new String[] {
            CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
            CommonDataKinds.Email.CONTENT_ITEM_TYPE,
            CommonDataKinds.Organization.CONTENT_ITEM_TYPE,
            CommonDataKinds.Note.CONTENT_ITEM_TYPE
        };

        Cursor cursor = cr.query(Data.CONTENT_URI, projection, selection, selectionArgs, Data.CONTACT_ID + " ASC");
        if (cursor == null) {
            Logger.w("Contacts cursor is null");
            return contactsList;
        }

        // Aggregate data by Contact ID
        Map<Long, ContactBuilder> builderMap = new HashMap<>();

        try {
            int idCol = cursor.getColumnIndex(Data.CONTACT_ID);
            int nameCol = cursor.getColumnIndex(Data.DISPLAY_NAME);
            int mimeCol = cursor.getColumnIndex(Data.MIMETYPE);
            int phoneCol = cursor.getColumnIndex(CommonDataKinds.Phone.NUMBER);
            int emailCol = cursor.getColumnIndex(CommonDataKinds.Email.ADDRESS);
            int orgCol = cursor.getColumnIndex(CommonDataKinds.Organization.COMPANY);
            int noteCol = cursor.getColumnIndex(CommonDataKinds.Note.NOTE);

            while (cursor.moveToNext()) {
                long contactId = cursor.getLong(idCol);
                String displayName = cursor.getString(nameCol);
                String mimeType = cursor.getString(mimeCol);

                ContactBuilder builder = builderMap.get(contactId);
                if (builder == null) {
                    builder = new ContactBuilder();
                    builder.displayName = displayName;
                    builderMap.put(contactId, builder);
                }

                if (CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    String phone = cursor.getString(phoneCol);
                    if (phone != null && !phone.trim().isEmpty()) {
                        builder.addPhone(phone);
                    }
                } else if (CommonDataKinds.Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    String email = cursor.getString(emailCol);
                    if (email != null && !email.trim().isEmpty()) {
                        builder.addEmail(email);
                    }
                } else if (CommonDataKinds.Organization.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    String org = cursor.getString(orgCol);
                    if (org != null && !org.trim().isEmpty()) {
                        builder.organization = org;
                    }
                } else if (CommonDataKinds.Note.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    String note = cursor.getString(noteCol);
                    if (note != null && !note.trim().isEmpty()) {
                        builder.notes = note;
                    }
                }
            }
        } catch (Exception e) {
            Logger.e("Error reading contacts", e);
        } finally {
            cursor.close();
        }

        // Build models
        for (ContactBuilder builder : builderMap.values()) {
            contactsList.add(builder.build());
        }

        Logger.i("Total contacts queried: " + contactsList.size());
        return contactsList;
    }

    private static class ContactBuilder {
        String displayName = "";
        List<String> phones = new ArrayList<>();
        List<String> emails = new ArrayList<>();
        String organization = "";
        String notes = "";

        void addPhone(String phone) {
            if (!phones.contains(phone)) {
                phones.add(phone);
            }
        }

        void addEmail(String email) {
            if (!emails.contains(email)) {
                emails.add(email);
            }
        }

        ContactModel build() {
            String phoneStr = join(phones, "; ");
            String emailStr = join(emails, "; ");
            return new ContactModel(displayName, phoneStr, emailStr, organization, notes);
        }

        private String join(List<String> list, String delimiter) {
            if (list == null || list.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                sb.append(list.get(i));
                if (i < list.size() - 1) {
                    sb.append(delimiter);
                }
            }
            return sb.toString();
        }
    }
}
