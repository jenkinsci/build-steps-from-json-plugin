package com.spcow.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Created by suresh on 8/2/2016.
 */
public class Util {

    @SuppressFBWarnings("EC_UNRELATED_TYPES_USING_POINTER_EQUALITY")
    public static Map<String, Object> jsonToMap(JSONObject json) throws JSONException {
        Map<String, Object> retMap = new HashMap<String, Object>();

        if (json != JSONObject.NULL) {
            retMap = toMap(json);
        }
        return retMap;
    }

    public static Map<String, Object> toMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new HashMap<String, Object>();

        Iterator<String> keysItr = object.keys();
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    public static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

    public static String displayJSONMAP(Map<String, Object> allKeys) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(allKeys);
        return json;
    }

    public static String removeStaplerClass(net.sf.json.JSONObject jsonObject) {
        removeStaplerClassFromObject(jsonObject);
        return jsonObject.toString();
    }

    static void removeStaplerClassFromObject(net.sf.json.JSONObject jsonObject) {
        Iterator<String> keysItr = jsonObject.keys();
        ArrayList deleteKeys = new ArrayList();
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = jsonObject.get(key);
            if (key.equals("stapler-class")) {
                deleteKeys.add(key);
            } else if (key.equals("")) {
                deleteKeys.add(key);
            } else if ((value instanceof net.sf.json.JSONObject ? (((net.sf.json.JSONObject) value).size() == 2) : false)
                    && (value instanceof net.sf.json.JSONObject ? (((net.sf.json.JSONObject) value).has("stapler-class")) : false)
                    && (value instanceof net.sf.json.JSONObject ? (((net.sf.json.JSONObject) value).has("$class")) : false)) {
                deleteKeys.add(key);
            } else {
                if (value instanceof net.sf.json.JSONArray) {
                    Iterator arrayIterator = ((net.sf.json.JSONArray) value).iterator();
                    while (arrayIterator.hasNext()) {
                        net.sf.json.JSONObject subElementObject = (net.sf.json.JSONObject) arrayIterator.next();
                        removeStaplerClassFromObject(subElementObject);
                    }
                } else if (value instanceof net.sf.json.JSONObject) {
                    removeStaplerClassFromObject((net.sf.json.JSONObject) value);
                }
            }
        }
        for (Object key : deleteKeys) {
            jsonObject.remove(key);
        }
    }
}
