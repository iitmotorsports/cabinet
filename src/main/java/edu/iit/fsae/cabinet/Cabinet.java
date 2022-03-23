package edu.iit.fsae.cabinet;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

/**
 * @author Noah Husby
 */
public class Cabinet {

    private final Javalin app;

    protected Cabinet() {
        app = configure();
    }

    public void start() {
        app.start(7000);
    }

    private Javalin configure() {
        return Javalin.create(config -> config.addStaticFiles(staticFiles -> {
            staticFiles.directory = "/public";
            staticFiles.location = Location.CLASSPATH;
        }));
    }
}
