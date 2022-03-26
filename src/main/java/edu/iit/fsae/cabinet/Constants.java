package edu.iit.fsae.cabinet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.iit.fsae.cabinet.util.LocalDateTimeSerializer;

import java.time.LocalDateTime;

/**
 * @author Noah Husby
 */
public class Constants {
    public static final Gson GSON;
    public static final Gson EXPOSED_GSON;

    static {
        GSON = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer()).create();
        EXPOSED_GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer()).create();
    }
}
