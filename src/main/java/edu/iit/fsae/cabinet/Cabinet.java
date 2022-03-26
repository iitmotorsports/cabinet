package edu.iit.fsae.cabinet;

import io.javalin.Javalin;
import io.javalin.core.validation.ValidationError;
import io.javalin.http.staticfiles.Location;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * @author Noah Husby
 */
public class Cabinet {

    private static Cabinet instance = null;

    public static Cabinet getInstance() {
        return instance == null ? instance = new Cabinet() : instance;
    }

    @Getter
    public static Logger logger = LoggerFactory.getLogger(Cabinet.class);

    private final Javalin app;

    @Getter
    private final File folder;

    protected Cabinet() {
        String filesDirectory = System.getenv("CABINET_DIR");
        if(filesDirectory == null) {
            filesDirectory = System.getProperty("user.dir");
        }
        folder = new File(filesDirectory, "logs");
        if(folder.mkdirs()) {
            logger.info("Storage directory does not exist. Creating at: " + filesDirectory);
        }
        app = configure();
    }

    private void start() {
        LogHandler.getInstance().load();
        logger.info("Starting server...");
        app.start(7000);
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
    }

    private Javalin configure() {
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.addStaticFiles("/public", Location.CLASSPATH);
            config.addStaticFiles(s -> {
                s.directory = folder.getAbsolutePath();
                s.location = Location.EXTERNAL;
                s.hostedPath = "/files";
            });
        });
        app.post("/api/post", ctx -> {
            long epoch = ctx.queryParamAsClass("time", Long.class).get();
            LocalDateTime time = Instant.ofEpochSecond(epoch).atOffset(ZoneOffset.UTC).toLocalDateTime();
            System.out.println(time.toString());
            System.out.println("Test");
            for(Map.Entry<String, List<String>> e : ctx.queryParamMap().entrySet()) {
                System.out.println(e.getKey() + ", " + e.getValue().get(0));
            }
        });
        app.get("/api/list_logs", ctx -> ctx.json(Constants.GSON.toJson(LogHandler.getInstance().getSortedLogsAsJson())));
        return app;
    }

    public static void main(String[] args) {
        Cabinet.getInstance().start();
    }
}
