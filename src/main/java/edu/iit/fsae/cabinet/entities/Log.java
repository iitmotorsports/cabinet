package edu.iit.fsae.cabinet.entities;

import com.google.gson.annotations.Expose;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents the metadata of a log
 *
 * @author Noah Husby
 */
@Data
public class Log {
    @Expose
    private final int id;
    @Expose
    private final LocalDateTime date;
    @Expose
    private final LocalDateTime uploadDate;
    private String size = "0kb";
    private boolean doesSheetExist = false;
}
