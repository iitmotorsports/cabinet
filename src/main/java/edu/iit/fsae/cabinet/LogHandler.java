package edu.iit.fsae.cabinet;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Noah Husby
 */
public class LogHandler {
    @Getter
    private static final LogHandler instance = new LogHandler();

    private final Map<Integer, Log> logs = new TreeMap<>();

    protected LogHandler() {
        logs.put(1, new Log(1, "1/04/2022 11:03AM", "312KB", "", "data.json", "log.txt"));
        logs.put(2, new Log(2, "3/23/2022 8:24PM", "4MB", "data.json", "data.json", "log.txt"));
    }

    public Map<Integer, Log> getSortedLogs() {
        Map<Integer, Log> temp = new TreeMap<>(Collections.reverseOrder());
        temp.putAll(logs);
        return temp;
    }

    public JsonArray getSortedLogsAsJson() {
        JsonArray array = new JsonArray();
        for (Log log : getSortedLogs().values()) {
            array.add(new Gson().toJsonTree(log));
        }
        return array;
    }
}
