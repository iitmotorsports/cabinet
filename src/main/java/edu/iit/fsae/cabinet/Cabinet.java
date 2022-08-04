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

import edu.iit.fsae.cabinet.entities.Log;
import edu.iit.fsae.cabinet.util.Util;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UploadedFile;
import io.javalin.http.staticfiles.Location;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @author Noah Husby
 */
@Slf4j
public class Cabinet {

    @Getter
    private static final Cabinet instance = new Cabinet();

    private final Javalin app;

    @Getter
    private final File folder;

    protected Cabinet() {
        String filesDirectory = System.getenv("CABINET_DIR");
        if (filesDirectory == null) {
            filesDirectory = System.getProperty("user.dir");
        }
        folder = new File(filesDirectory, "logs");
        if (folder.mkdirs()) {
            log.info("Storage directory does not exist. Creating at: {}", filesDirectory);
        }
        app = configure(folder);
    }

    /**
     * Starts the Cabinet server.
     */
    private void start() {
        LogHandler.getInstance().load();
        log.info("Starting server...");
        app.start(80);
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
    }

    /**
     * Creates a new Javalin instance with set endpoints.
     *
     * @return {@link Javalin}
     */
    private static Javalin configure(File folder) {
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.addStaticFiles("/public", Location.CLASSPATH);
            config.addStaticFiles(s -> {
                s.directory = folder.getAbsolutePath();
                s.location = Location.EXTERNAL;
                s.hostedPath = "/files";
            });
        });
        app.post(Constants.API_V1_PATH + "/logs", ctx -> {
            if (!ctx.queryParamMap().containsKey("date")) {
                throw new BadRequestResponse("The 'date' parameter has not been set.");
            }
            long epoch = ctx.queryParamAsClass("date", Long.class).get();
            LocalDateTime date = Instant.ofEpochSecond(epoch).atOffset(ZoneOffset.UTC).toLocalDateTime();
            UploadedFile logFile = ctx.uploadedFile("log");
            if (logFile == null) {
                throw new BadRequestResponse("The 'log' file has not been attached.");
            }
            UploadedFile statsFile = ctx.uploadedFile("stats");
            UploadedFile statsMapFile = ctx.uploadedFile("stats_map");
            if (statsFile != null && statsMapFile == null) {
                throw new BadRequestResponse("The 'stats' file was attached, but the 'stats_map' file is missing.");
            }
            Log log = LogHandler.getInstance().postNewLog(date, logFile, statsFile, statsMapFile);
            ctx.json(Constants.EXPOSED_GSON.toJson(log));
        });
        app.get(Constants.API_V1_PATH + "/logs/{log}", ctx -> {
            String id = ctx.pathParam("log");
            if (!Util.isInteger(id)) {
                throw new BadRequestResponse();
            }
            Log log = LogHandler.getInstance().getLog(Integer.parseInt(id));
            if (log == null) {
                throw new NotFoundResponse();
            }
            ctx.json(Constants.GSON.toJson(log));
        });
        app.get(Constants.API_V1_PATH + "/logs", ctx -> ctx.json(Constants.GSON.toJson(LogHandler.getInstance().getSortedLogsAsJson())));
        return app;
    }

    public static void main(String[] args) {
        Cabinet.getInstance().start();
    }
}
