package edu.iit.fsae.cabinet.entities;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Noah Husby
 */
@Data
public class Log {
    @Expose
    private final int id;
    @Expose
    private final LocalDateTime time;
    private String size = "0kb";
    private boolean doesSheetExist = false;
}
