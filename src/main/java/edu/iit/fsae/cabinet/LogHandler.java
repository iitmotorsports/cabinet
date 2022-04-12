/*
 * MIT License
 *
 * Copyright 2022 Illinois Tech Motorsports
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package edu.iit.fsae.cabinet;

import com.google.gson.JsonArray;
import edu.iit.fsae.cabinet.entities.Log;
import edu.iit.fsae.cabinet.util.StatisticsSheetWriter;
import edu.iit.fsae.cabinet.util.Util;
import io.javalin.core.util.FileUtil;
import io.javalin.http.UploadedFile;
import lombok.Getter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A handler for creating, storing, and fetching log files.
 *
 * @author Noah Husby
 */
public class LogHandler {
    @Getter
    private static final LogHandler instance = new LogHandler();

    private final Map<Integer, Log> logs = new TreeMap<>();
    private final ExecutorService logWorkerThreads;

    protected LogHandler() {
        logWorkerThreads = Executors.newFixedThreadPool(8);
        Runtime.getRuntime().addShutdownHook(new Thread(logWorkerThreads::shutdown));
    }

    /**
     * Gets logs reverse sorted by id.
     *
     * @return Map of Integer, Log.
     */
    public Map<Integer, Log> getSortedLogs() {
        Map<Integer, Log> temp = new TreeMap<>(Collections.reverseOrder());
        temp.putAll(logs);
        return temp;
    }

    /**
     * Gets logs reverse sorted by id as a JsonArray.
     *
     * @return {@link JsonArray} of sorted logs.
     */
    public JsonArray getSortedLogsAsJson() {
        JsonArray array = new JsonArray();
        getSortedLogs().values().forEach(log -> array.add(Constants.GSON.toJsonTree(log)));
        return array;
    }

    /**
     * Loads logs from set working directory.
     */
    public void load() {
        Cabinet.getLogger().info("Loading logs...");
        File logDir = Cabinet.getInstance().getFolder();
        for (File file : Objects.requireNonNull(logDir.listFiles())) {
            if (file.isFile()) {
                Cabinet.getLogger().warn("Unknown file in log directory: " + file.getName());
            } else {
                String directoryName = file.getName();
                if (!Util.isInteger(directoryName)) {
                    Cabinet.getLogger().warn("Non-indexed folder in directory: " + file.getName());
                    continue;
                }
                File manifest = new File(file, "manifest.json");
                if (!manifest.exists()) {
                    Cabinet.getLogger().warn("No manifest for log in directory: " + file.getName());
                    continue;
                }
                loadLogFromManifest(file, manifest);
            }
        }
        Cabinet.getLogger().info(String.format("Loaded %d logs", logs.size()));
    }

    /**
     * Posts a new log
     *
     * @param date         Date of log creation.
     * @param logFile      {@link UploadedFile} representing the plain-text log file.
     * @param statsFile    {@link UploadedFile} representing the binary statistics file.
     * @param statsMapFile {@link UploadedFile} representing the json statistics mapping file.
     * @return A new {@link Log} representation of the upload.
     */
    public Log postNewLog(LocalDateTime date, UploadedFile logFile, UploadedFile statsFile, UploadedFile statsMapFile) {
        int i = logs.size();
        while (logs.containsKey(i)) {
            i++;
        }
        Log log = new Log(i, date, LocalDateTime.now());
        Cabinet.getLogger().info("Uploaded new log: " + log.getId() + " (w/ log" + (statsFile != null ? " & stats)" : ")"));
        logWorkerThreads.submit(() -> {
            saveLogToManifest(log);
            saveLogFiles(log, logFile, statsFile, statsMapFile);
            logs.put(log.getId(), log);
            handleLogStatistics(log);
            handleLogArchive(log);
        });
        return log;
    }

    /**
     * Saves a log to a manifest file.
     *
     * @param log {@link Log}
     */
    private void saveLogToManifest(Log log) {
        File parent = new File(Cabinet.getInstance().getFolder(), String.valueOf(log.getId()));
        File manifestFile = new File(parent, "manifest.json");
        parent.mkdirs();
        FileWriter writer = null;
        try {
            writer = new FileWriter(manifestFile);
            Constants.EXPOSED_GSON.toJson(log, writer);
        } catch (IOException e) {
            Cabinet.getLogger().error("Failed to write manifest file for: " + log.getId(), e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Saves uploaded files to log directory.
     *
     * @param log               {@link Log}
     * @param uploadedLogFile   {@link UploadedFile}
     * @param uploadedStatsFile {@link UploadedFile}
     */
    private void saveLogFiles(Log log, UploadedFile uploadedLogFile, UploadedFile uploadedStatsFile, UploadedFile uploadStatsMapFile) {
        File parent = new File(Cabinet.getInstance().getFolder(), String.valueOf(log.getId()));
        File logFile = new File(parent, log.getId() + ".txt");
        FileUtil.streamToFile(uploadedLogFile.getContent(), logFile.getAbsolutePath());
        // Temporary size while sheet is generated
        log.setSize(Util.humanReadableBytes(logFile.length()));
        if (uploadedStatsFile != null) {
            File statsFile = new File(parent, log.getId() + ".stats");
            File statsMapFile = new File(parent, log.getId() + ".map.stats");
            FileUtil.streamToFile(uploadedStatsFile.getContent(), statsFile.getAbsolutePath());
            FileUtil.streamToFile(uploadStatsMapFile.getContent(), statsMapFile.getAbsolutePath());
        }
    }

    /**
     * Loads a log file from its manifest file.
     *
     * @param parent       The parent folder of the log.
     * @param manifestFile The manifest file of the log.
     */
    private void loadLogFromManifest(File parent, File manifestFile) {
        Log log;
        try {
            log = Constants.GSON.fromJson(new FileReader(manifestFile), Log.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        if (!Util.doesChildFileExist(parent, log.getId() + ".txt")) {
            Cabinet.getLogger().warn("Log file missing. Not loading log: " + parent.getName());
            return;
        }
        logWorkerThreads.submit(() -> {
            // Temporary size whilst statistics are being handled
            log.setSize(Util.humanReadableBytes(0));
            handleLogStatistics(log);
            handleLogArchive(log);
        });
        logs.put(log.getId(), log);
    }

    /**
     * Handles the checking and creation of the statistics file.
     *
     * @param log {@link Log}
     */
    private void handleLogStatistics(Log log) {
        try {
            File parent = new File(Cabinet.getInstance().getFolder(), String.valueOf(log.getId()));
            File statsMap = new File(parent, log.getId() + ".map.stats");
            File stats = new File(parent, log.getId() + ".stats");
            boolean sheetExist = Util.doesChildFileExist(parent, log.getId() + ".xlsx");
            if (sheetExist) {
                log.setDoesSheetExist(true);
                return;
            }
            if (stats.exists() && statsMap.exists()) {
                long start = System.currentTimeMillis();
                Cabinet.getLogger().info("Generating excel sheet for Log #" + log.getId() + " ...");
                StatisticsSheetWriter writer = new StatisticsSheetWriter(log, stats, statsMap);
                writer.parse();
                writer.write(new File(parent, log.getId() + ".xlsx"));
                Cabinet.getLogger().info("Finished generating excel sheet in " + (System.currentTimeMillis() - start) + " ms.");
                log.setDoesSheetExist(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the checking and creation of the log archive.
     *
     * @param log {@link Log}
     */
    private void handleLogArchive(Log log) {
        File parent = new File(Cabinet.getInstance().getFolder(), String.valueOf(log.getId()));
        File zip = new File(parent, log.getId() + ".zip");
        if (!zip.exists()) {
            try {
                Util.zipFolder(parent, zip);
            } catch (IOException e) {
                Cabinet.getLogger().warn("Failed to zip log: " + log.getId(), e);
            }
        }
        log.setSize(Util.humanReadableBytes(zip.length()));
    }

    /**
     * Gets a {@link Log} by its id number.
     *
     * @param id Id of log.
     * @return {@link Log} if exists, null otherwise.
     */
    public Log getLog(int id) {
        return logs.get(id);
    }
}
