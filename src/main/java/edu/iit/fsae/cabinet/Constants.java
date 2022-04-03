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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.iit.fsae.cabinet.util.LocalDateTimeSerializer;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

/**
 * @author Noah Husby
 */
@UtilityClass
public class Constants {
    public static final Gson GSON;
    public static final Gson EXPOSED_GSON;

    public static final String API_V1_PATH = "/api/v1";

    static {
        GSON = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer()).create();
        EXPOSED_GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer()).create();
    }
}
