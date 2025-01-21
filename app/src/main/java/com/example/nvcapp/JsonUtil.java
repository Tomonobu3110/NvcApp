package com.example.nvcapp;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class  JsonUtil {
    private static final String FILE_NAME = "nvc_data.json";

    public static void saveData(Context context, List<NvcItem> items) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (NvcItem item : items) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("event", item.getEvent());
                jsonObject.put("observation", item.getObservation());
                jsonObject.put("feelings", item.getFeelings());
                jsonObject.put("needs", item.getNeeds());
                jsonObject.put("request", item.getRequest());
                jsonArray.put(jsonObject);
            }
            JSONObject root = new JSONObject();
            root.put("nvc_items", jsonArray);

            FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            fos.write(root.toString().getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<NvcItem> loadData(Context context) {
        List<NvcItem> items = new ArrayList<>();
        try {
            FileInputStream fis = context.openFileInput(FILE_NAME);
            byte[] data = new byte[fis.available()];
            fis.read(data);
            fis.close();

            String json = new String(data);
            JSONObject root = new JSONObject(json);
            JSONArray jsonArray = root.getJSONArray("nvc_items");

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                NvcItem item = new NvcItem(
                        obj.getString("event"),
                        obj.getString("observation"),
                        obj.getString("feelings"),
                        obj.getString("needs"),
                        obj.getString("request")
                );
                items.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }
}
