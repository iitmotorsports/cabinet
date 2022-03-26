package edu.iit.fsae.cabinet;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import edu.iit.fsae.cabinet.entities.Log;
import edu.iit.fsae.cabinet.util.Util;
import lombok.Getter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * @author Noah Husby
 */
public class LogHandler {
    @Getter
    private static final LogHandler instance = new LogHandler();

    private final Map<Integer, Log> logs = new TreeMap<>();

    protected LogHandler() {
    }

    public Map<Integer, Log> getSortedLogs() {
        Map<Integer, Log> temp = new TreeMap<>(Collections.reverseOrder());
        temp.putAll(logs);
        return temp;
    }

    public JsonArray getSortedLogsAsJson() {
        JsonArray array = new JsonArray();
        getSortedLogs().values().forEach(log -> array.add(Constants.GSON.toJsonTree(log)));
        return array;
    }

    /**
     * Loads logs from set working directory
     */
    public void load() {
        Cabinet.getLogger().info("Loading logs...");
        File logDir = Cabinet.getInstance().getFolder();
        for (File file : Objects.requireNonNull(logDir.listFiles())) {
            if(file.isFile()) {
                Cabinet.getLogger().warn("Unknown file in log directory: " + file.getName());
            } else {
                String directoryName = file.getName();
                if(!Util.isInteger(directoryName)) {
                    Cabinet.getLogger().warn("Non-indexed folder in directory: " + file.getName());
                    continue;
                }
                File manifest = new File(file, "manifest.json");
                if(!manifest.exists()) {
                    Cabinet.getLogger().warn("No manifest for log in directory: " + file.getName());
                    continue;
                }
                loadLogFromManifest(file, manifest);
            }
        }
        Cabinet.getLogger().info(String.format("Loaded %d logs", logs.size()));
    }

    private void loadLogFromManifest(File parent, File manifestFile) {
        Log log;
        try {
            log = Constants.GSON.fromJson(new FileReader(manifestFile), Log.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        if(!Util.doesChildFileExist(parent, log.getId() + ".txt")) {
            Cabinet.getLogger().warn("Log file missing. Not loading log: " + parent.getName());
            return;
        }
        if(!Util.doesChildFileExist(parent, log.getId() + ".zip")) {
            // TODO: Run worker
            // TODO: Set log file size
        }
        if(Util.doesChildFileExist(parent, log.getId() + ".xlsx")) {
            log.setDoesSheetExist(true);
        }
        log.setSize("0kb");
        logs.put(log.getId(), log);
    }
}
