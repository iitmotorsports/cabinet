package edu.iit.fsae.cabinet;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Noah Husby
 */
@Data
@AllArgsConstructor
public class Log {
    private final int id;
    private final String date;
    private String size;
    private String sheet;
    private String zip;
    private String log;
}
