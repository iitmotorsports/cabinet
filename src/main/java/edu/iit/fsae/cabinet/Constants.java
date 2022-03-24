package edu.iit.fsae.cabinet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author Noah Husby
 */
public class Constants {
    public static final Gson GSON;

    static {
        GSON = new GsonBuilder().setPrettyPrinting().create();
    }
}
