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

package edu.iit.fsae.cabinet.util;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A utility class
 *
 * @author Noah Husby
 */
@UtilityClass
public class Util {
    /**
     * Checks if a string can be parsed as an integer
     *
     * @param s String to check
     * @return True if an integer, false if not
     */
    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /**
     * Checks if a child file exists under a parent folder
     *
     * @param parent {@link File}
     * @param child  The name of the child
     * @return True if the child exists, false otherwise
     */
    public static boolean doesChildFileExist(File parent, String child) {
        return new File(parent, child).exists();
    }

    /**
     * Zips entire folder into zip file
     *
     * @param folder  The folder to be zipped
     * @param zipFile The destination zip folder
     * @throws IOException If the file cannot be created
     */
    public static void zipFolder(File folder, File zipFile) throws IOException {
        Path p = Files.createFile(Paths.get(zipFile.getAbsolutePath()));
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
            Path pp = Paths.get(folder.getAbsolutePath());
            Files.walk(pp)
                    .filter(path -> !Files.isDirectory(path) && !path.toString().equals(p.toString()))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    /**
     * Converts amount of bytes to human-readable format
     *
     * @param bytes Size of file
     * @return Size of file formatted as a String
     */
    public static String humanReadableBytes(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }

    /**
     * Creates a new entry.
     *
     * @param key   Key of entry.
     * @param value Value of entry.
     * @param <K>   The type of the key.
     * @param <V>   The type of the value.
     * @return {@link Map.Entry}
     */
    public static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new Map.Entry<>() {
            @Override
            public K getKey() {
                return key;
            }

            @Override
            public V getValue() {
                return value;
            }

            @Override
            public V setValue(V value) {
                return null;
            }
        };
    }

    /**
     * Converts LocalDateTime to date string.
     *
     * @param ldt {@link LocalDateTime} to be formatted.
     * @return LocalDateTime formatted as a string.
     */
    public static String of(LocalDateTime ldt) {
        ldt = ldt.atZone(ZoneId.systemDefault()).toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss a");
        return ldt.format(formatter);
    }
}
