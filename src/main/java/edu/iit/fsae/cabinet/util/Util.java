package edu.iit.fsae.cabinet.util;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
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
}
