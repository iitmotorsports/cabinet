package edu.iit.fsae.cabinet;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import lombok.Getter;

import java.io.File;

/**
 * @author Noah Husby
 */
public class Cabinet {

    private static Cabinet instance = null;

    public static Cabinet getInstance() {
        return instance == null ? instance = new Cabinet() : instance;
    }

    private final Javalin app;

    @Getter
    private final File folder = new File(System.getProperty("user.dir"));

    protected Cabinet() {
        app = configure();
        app.post("/api/post", ctx -> {

        });
        app.get("/api/list_logs", ctx -> ctx.json(Constants.GSON.toJson(LogHandler.getInstance().getSortedLogsAsJson())));
    }

    private void start() {
        app.start(7000);
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
    }

    private Javalin configure() {
        return Javalin.create(config -> config.addStaticFiles(staticFiles -> {
            staticFiles.directory = "/public";
            staticFiles.location = Location.CLASSPATH;
        }));
    }

    public static void main(String[] args) {
        Cabinet.getInstance().start();
    }
}
