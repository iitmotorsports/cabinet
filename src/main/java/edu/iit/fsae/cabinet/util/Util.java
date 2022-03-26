package edu.iit.fsae.cabinet.util;

import java.io.File;

/**
 * @author Noah Husby
 */
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
     * @param child The name of the child
     * @return True if the child exists, false otherwise
     */
    public static boolean doesChildFileExist(File parent, String child) {
        return new File(parent, child).exists();
    }
}
