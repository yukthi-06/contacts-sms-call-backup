package com.vypeensoft.contactsmscallbackup.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;

public class JsonExportUtil {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static <T> String exportToJson(List<T> dataList) {
        return gson.toJson(dataList);
    }
}
