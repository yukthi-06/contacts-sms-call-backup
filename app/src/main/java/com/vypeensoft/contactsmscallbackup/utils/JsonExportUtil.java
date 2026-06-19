package com.vypeensoft.contactsmscallbackup.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.vypeensoft.contactsmscallbackup.models.CallLogModel;
import com.vypeensoft.contactsmscallbackup.models.SmsModel;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JsonExportUtil {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(SmsModel.class, new JsonSerializer<SmsModel>() {
                @Override
                public JsonElement serialize(SmsModel src, Type typeOfSrc, JsonSerializationContext context) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("address", src.getAddress());
                    obj.addProperty("body", src.getBody());
                    obj.addProperty("date", dateFormat.format(new Date(src.getDate())));
                    obj.addProperty("type", src.getType());
                    obj.addProperty("threadId", src.getThreadId());
                    return obj;
                }
            })
            .registerTypeAdapter(CallLogModel.class, new JsonSerializer<CallLogModel>() {
                @Override
                public JsonElement serialize(CallLogModel src, Type typeOfSrc, JsonSerializationContext context) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("name", src.getName());
                    obj.addProperty("number", src.getNumber());
                    obj.addProperty("type", src.getType());
                    obj.addProperty("duration", src.getDuration());
                    obj.addProperty("date", dateFormat.format(new Date(src.getDate())));
                    return obj;
                }
            })
            .create();

    public static <T> String exportToJson(List<T> dataList) {
        return gson.toJson(dataList);
    }
}
