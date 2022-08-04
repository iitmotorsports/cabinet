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

import com.google.gson.reflect.TypeToken;
import edu.iit.fsae.cabinet.Constants;
import edu.iit.fsae.cabinet.entities.Log;
import edu.iit.fsae.cabinet.util.tracking.LastTrackingPolicy;
import edu.iit.fsae.cabinet.util.tracking.MaxTrackingPolicy;
import edu.iit.fsae.cabinet.util.tracking.TrackingPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.MarkerStyle;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFLineChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Noah Husby
 */
@RequiredArgsConstructor
@Slf4j
public class StatisticsSheetWriter {

    private final Log logMetadata;
    private final File statisticsFile;
    private final File statisticsMapFile;
    private Map<String, String> statisticsMap;
    private Map<Long, Map<String, Integer>> statistics;

    private static final Type statMapType = new TypeToken<Map<String, String>>() {}.getType();
    private final Map<String, TrackingPolicy> trackedStatistics = new HashMap<>();
    private static final String OVERVIEW_PAGE = "Overview";

    private static final String MOTOR_SPEED = "mtr_spd";
    private static final String STATE_OF_CHARGE = "bms_soc";
    private static final String MOTOR_CONTROLLER_0_CURRENT = "mc0_dc_i";
    private static final String MOTOR_CONTROLLER_1_CURRENT = "mc1_dc_i";
    private static final String MOTOR_CONTROLLER_0_TEMP = "mc0_mtr_tmp";
    private static final String MOTOR_CONTROLLER_1_TEMP = "mc1_mtr_tmp";

    private void setupTrackers() {
        trackedStatistics.put(MOTOR_SPEED, new MaxTrackingPolicy());
        trackedStatistics.put(STATE_OF_CHARGE, new LastTrackingPolicy());
        trackedStatistics.put(MOTOR_CONTROLLER_0_CURRENT, new MaxTrackingPolicy());
        trackedStatistics.put(MOTOR_CONTROLLER_1_CURRENT, new MaxTrackingPolicy());
        trackedStatistics.put(MOTOR_CONTROLLER_0_TEMP, new MaxTrackingPolicy());
        trackedStatistics.put(MOTOR_CONTROLLER_1_TEMP, new MaxTrackingPolicy());
    }

    /**
     * Parses the statistics file.
     */
    public void parse() throws IOException {
        setupTrackers();
        FileReader statMapReader = new FileReader(statisticsMapFile);
        statisticsMap = Constants.GSON.fromJson(statMapReader, statMapType);
        statMapReader.close();
        statistics = new TreeMap<>();
        try (
                BufferedReader reader = new BufferedReader(new FileReader(statisticsFile));
        ) {
            String line = reader.readLine();
            while (line != null) {
                String[] statArray = line.split(" ");
                long timestamp = Long.parseLong(statArray[0]);
                int value = Integer.parseInt(statArray[2]);
                statistics.putIfAbsent(timestamp, new HashMap<>());
                statistics.get(timestamp).put(statArray[1], value);
                TrackingPolicy policy = trackedStatistics.get(statisticsMap.get(statArray[1]));
                if (policy != null) {
                    policy.post(value);
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            log.error("Failed to parse log file for statistics.", e);
        }


        // Normalize data
        Map<String, Integer> lastRowStat = null;
        for (Map.Entry<Long, Map<String, Integer>> e : new TreeMap<>(statistics).entrySet()) {
            for (Map.Entry<String, String> e2 : statisticsMap.entrySet()) {
                if (!e.getValue().containsKey(e2.getKey())) {
                    if (lastRowStat == null) {
                        e.getValue().put(e2.getKey(), 0);
                        statistics.get(e.getKey()).put(e2.getKey(), 0);
                    } else {
                        statistics.get(e.getKey()).put(e2.getKey(), lastRowStat.get(e2.getKey()));
                    }
                }
            }
            lastRowStat = e.getValue();
        }
    }

    /**
     * Writes the final workbook to the specified file.
     *
     * @param file The file to written to.
     */
    public void write(File file) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        overview(workbook);
        XSSFSheet raw = workbook.createSheet("Raw");
        // Create Header
        Row headerRow = raw.createRow(0);
        headerRow.createCell(0).setCellValue("Timestamp");
        headerRow.createCell(1).setCellValue("Time (Seconds)");
        int currentHeaderColumn = 2;
        // Stat ID as String | Header column
        Map<String, Integer> headerMap = new HashMap<>();
        for (Map.Entry<String, String> e : statisticsMap.entrySet()) {
            headerRow.createCell(currentHeaderColumn).setCellValue(e.getValue());
            headerMap.put(e.getKey(), currentHeaderColumn);
            currentHeaderColumn++;
        }

        long offset = statistics.keySet().iterator().next();

        int currentRow = 1;
        for (Map.Entry<Long, Map<String, Integer>> e : statistics.entrySet()) {
            Row row = raw.createRow(currentRow);
            row.createCell(0).setCellValue(e.getKey());
            row.createCell(1).setCellValue(Math.ceil((e.getKey() - offset) / 1000.00));
            for (Map.Entry<String, Integer> e2 : e.getValue().entrySet()) {
                try {
                    row.createCell(headerMap.get(e2.getKey())).setCellValue(e2.getValue());
                } catch (NullPointerException ignored) {
                    // Failed to create row. Ignoring and moving on.
                }
            }
            currentRow++;
        }

        visual(workbook, headerMap);

        FileOutputStream outputStream = new FileOutputStream(file);
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
    }

    /*
     * Overview
     */

    /**
     * Creates the overview tab and elements.
     *
     * @param workbook The current session's workbook.
     */
    private void overview(XSSFWorkbook workbook) {
        XSSFSheet overview = workbook.createSheet(OVERVIEW_PAGE);
        overviewTitleBlock(workbook, overview);
        overviewValues(workbook, overview);
        overviewImageBlock(workbook, overview);
        overviewMenu(workbook, overview);
    }

    /**
     * Generates the title block and information for the session.
     *
     * @param workbook The current session's workbook.
     * @param sheet    The overview tab.
     */
    private void overviewTitleBlock(XSSFWorkbook workbook, Sheet sheet) {
        sheet.addMergedRegion(CellRangeAddress.valueOf("B2:E2"));
        sheet.addMergedRegion(CellRangeAddress.valueOf("C3:E3"));
        sheet.addMergedRegion(CellRangeAddress.valueOf("C4:E4"));
        sheet.addMergedRegion(CellRangeAddress.valueOf("B5:E5"));

        // Header Row
        Row r2 = sheet.createRow(1);
        Cell header = r2.createCell(1);
        header.setCellValue("Log #" + logMetadata.getId());

        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        header.setCellStyle(style);

        // Creation Date Row
        Row r3 = sheet.createRow(2);
        Cell created = r3.createCell(1);
        created.setCellValue("Created:");
        Cell createdValue = r3.createCell(2);
        createdValue.setCellValue(Util.of(logMetadata.getDate()));

        // Uploaded Date Row
        Row r4 = sheet.createRow(3);
        Cell uploaded = r4.createCell(1);
        uploaded.setCellValue("Uploaded:");
        Cell uploadedValue = r4.createCell(2);
        uploadedValue.setCellValue(Util.of(logMetadata.getUploadDate()));

        // Open Link Row
        Row r5 = sheet.createRow(4);
        CreationHelper helper = workbook.getCreationHelper();
        XSSFHyperlink link = (XSSFHyperlink) helper.createHyperlink(HyperlinkType.URL);
        link.setAddress(String.format("https://logs.iitmotorsports.org/files/%d/%d.txt", logMetadata.getId(), logMetadata.getId()));

        Cell openLog = r5.createCell(1);
        openLog.setCellValue("Open Log");
        openLog.setHyperlink(link);
        formatAsLink(workbook, openLog);

        // Configure borders
        setBorder(CellRangeAddress.valueOf("B3:B3"), sheet);
        setBorder(CellRangeAddress.valueOf("B4:B4"), sheet);
        setBorder(CellRangeAddress.valueOf("C3:E3"), sheet);
        setBorder(CellRangeAddress.valueOf("C4:E4"), sheet);
        setBorder(CellRangeAddress.valueOf("B2:E5"), sheet);
    }

    /**
     * Generates the quick-view statistics on the overview page.
     *
     * @param workbook The current session's workbook.
     * @param sheet    The overview tab.
     */
    private void overviewValues(XSSFWorkbook workbook, Sheet sheet) {
        Row r6 = sheet.createRow(6);
        Cell header = r6.createCell(1);
        header.setCellValue(OVERVIEW_PAGE);

        sheet.addMergedRegion(CellRangeAddress.valueOf("B7:E7"));
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        header.setCellStyle(style);

        int i = 8;
        addOverviewStatistic(sheet, i++, "Top Speed", " MPH", MOTOR_SPEED);
        addOverviewStatistic(sheet, i++, "Remaining Battery", "%", STATE_OF_CHARGE);
        addOverviewStatistic(sheet, i++, "MC0 Top Current", " A", MOTOR_CONTROLLER_0_CURRENT);
        addOverviewStatistic(sheet, i++, "MC1 Top Current", " A", MOTOR_CONTROLLER_1_CURRENT);
        addOverviewStatistic(sheet, i++, "M0 Top Temp", " \u02DAC", MOTOR_CONTROLLER_0_TEMP);
        addOverviewStatistic(sheet, i++, "M1 Top Temp", " \u02DAC", MOTOR_CONTROLLER_1_TEMP);

        setBorder(CellRangeAddress.valueOf("B7:E" + (i - 1)), sheet);
    }

    /**
     * Adds a specified statistic to the chart using a {@link TrackingPolicy}
     *
     * @param sheet  The overview tab.
     * @param rowNo  The row number of where to print the data.
     * @param title  A descriptive title of the statistic.
     * @param suffix The suffix of the value.
     * @param stat   The statistic id.
     */
    private void addOverviewStatistic(Sheet sheet, int rowNo, String title, String suffix, String stat) {
        sheet.addMergedRegion(CellRangeAddress.valueOf(String.format("B%s:C%s", rowNo, rowNo)));
        sheet.addMergedRegion(CellRangeAddress.valueOf(String.format("D%s:E%s", rowNo, rowNo)));
        Row row = sheet.createRow(rowNo - 1);
        Cell titleCell = row.createCell(1);
        titleCell.setCellValue(title + ":");

        TrackingPolicy policy = trackedStatistics.get(stat);
        Cell valueCell = row.createCell(3);
        valueCell.setCellValue(policy.get() + suffix);

        setBorder(CellRangeAddress.valueOf(String.format("B%s:C%s", rowNo, rowNo)), sheet);
        setBorder(CellRangeAddress.valueOf(String.format("D%s:E%s", rowNo, rowNo)), sheet);
    }

    /**
     * Generates image block for the overview.
     *
     * @param workbook The current session's workbook.
     * @param sheet    The overview tab.
     */
    private void overviewImageBlock(XSSFWorkbook workbook, Sheet sheet) {
        sheet.addMergedRegion(CellRangeAddress.valueOf("G2:J8"));
        Row row = sheet.getRow(1);
        Cell imageDescription = row.createCell(6);
        imageDescription.setCellValue("Cabinet Logging System by Noah Husby");

        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 9);
        byte[] rgb = { (byte) 187, 2, 0 };
        font.setColor(new XSSFColor(rgb, new DefaultIndexedColorMap()));
        style.setFont(font);
        // TODO: Remove vertical alignment once logo is fixed.
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.CENTER);
        imageDescription.setCellStyle(style);

        setBorder(CellRangeAddress.valueOf("G2:J8"), sheet);
    }

    /**
     * Generates the menu for the overview page.
     *
     * @param workbook The current session's workbook.
     * @param sheet    The overview tab.
     */
    private void overviewMenu(XSSFWorkbook workbook, Sheet sheet) {
        Row r9 = sheet.getRow(9);
        Cell header = r9.createCell(6);
        header.setCellValue("Menu");

        sheet.addMergedRegion(CellRangeAddress.valueOf("G10:J10"));
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        header.setCellStyle(style);

        addOverviewMenu(sheet, 11, OVERVIEW_PAGE, "An overview of the session");
        addOverviewMenu(sheet, 12, "Visual", "Graphical rendering of data");
        addOverviewMenu(sheet, 13, "Raw", "Raw recorded data");

        setBorder(CellRangeAddress.valueOf("G10:J13"), sheet);
    }

    /**
     * Adds a menu element to the overview.
     *
     * @param sheet       The overview tab.
     * @param rowNo       The row number of where the menu element should be printed.
     * @param name        The name (and ID) of the specific tab.
     * @param description A description of the page.
     */
    private void addOverviewMenu(Sheet sheet, int rowNo, String name, String description) {
        sheet.addMergedRegion(CellRangeAddress.valueOf(String.format("H%s:J%s", rowNo, rowNo)));
        Row row = sheet.getRow(rowNo - 1);
        CreationHelper helper = sheet.getWorkbook().getCreationHelper();
        XSSFHyperlink link = (XSSFHyperlink) helper.createHyperlink(HyperlinkType.DOCUMENT);
        link.setAddress(String.format("'%s'!A1", name));

        Cell titleCell = row.createCell(6);
        titleCell.setCellValue(name);
        titleCell.setHyperlink(link);
        formatAsLink(sheet.getWorkbook(), titleCell);

        Cell descriptionCell = row.createCell(7);
        descriptionCell.setCellValue(description);

        setBorder(CellRangeAddress.valueOf(String.format("G%s:G%s", rowNo, rowNo)), sheet);
        setBorder(CellRangeAddress.valueOf(String.format("H%s:J%s", rowNo, rowNo)), sheet);
    }

    /*
     * Visual
     */

    /**
     * Creates the visual tab and elements.
     *
     * @param workbook  The current session's workbook.
     * @param headerMap A list of the statistics and their associated spreadsheet column.
     */
    private void visual(XSSFWorkbook workbook, Map<String, Integer> headerMap) {
        XSSFSheet visual = workbook.createSheet("Visual");
        XSSFSheet raw = workbook.getSheet("Raw");

        addVisualGraph("Speed", new String[]{ MOTOR_SPEED }, 0, 0, 15, 26, visual, raw, headerMap);
        addVisualGraph("Throttle", new String[]{ "pdl_0", "pdl_avg", "pdl_1" }, 15, 0, 30, 26, visual, raw, headerMap);
        addVisualGraph("MC Current", new String[]{ MOTOR_CONTROLLER_0_CURRENT, MOTOR_CONTROLLER_1_CURRENT }, 0, 26, 15, 52, visual, raw, headerMap);
        addVisualGraph("MC Voltage", new String[]{ "mc0_dc_v", "mc1_dc_v" }, 15, 26, 30, 52, visual, raw, headerMap);
        addVisualGraph("Steering", new String[]{ "steer" }, 0, 52, 15, 78, visual, raw, headerMap);
        addVisualGraph("State of Charge", new String[]{ STATE_OF_CHARGE }, 15, 52, 30, 78, visual, raw, headerMap);
    }

    /**
     * Adds a graph to the visual page.
     *
     * @param title      The title of the graph.
     * @param statistics An array of all statistics to graph.
     * @param col1       Upper left-hand anchor.
     * @param row1       Upper left-hand anchor.
     * @param col2       Lower right-hand anchor.
     * @param row2       Lower right-hand anchor.
     * @param visual     The visual tab.
     * @param raw        The raw tab.
     * @param headerMap  A list of the statistics and their associated spreadsheet column.
     */
    private void addVisualGraph(String title, String[] statistics, int col1, int row1, int col2, int row2, XSSFSheet visual, XSSFSheet raw, Map<String, Integer> headerMap) {
        XSSFDrawing drawing = visual.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, col1, row1, col2, row2);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(title);
        chart.setTitleOverlay(false);

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Time (seconds)");
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Value");

        XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
        data.setVaryColors(false);
        int rows = raw.getPhysicalNumberOfRows();
        int skip = rows / 12;
        XDDFNumericalDataSource<Double> timestamps = XDDFDataSourcesFactory.fromNumericCellRange(raw, new CellRangeAddress(1, rows - 1, 1, 1));

        for (String statistic : statistics) {
            int statColumn = getRowFromStatistic(statistic, headerMap);
            XDDFNumericalDataSource<Double> dataSource = XDDFDataSourcesFactory.fromNumericCellRange(raw, new CellRangeAddress(1, rows - 1, statColumn, statColumn));
            XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(timestamps, dataSource);
            series.setTitle(statistic, null);
            series.setSmooth(true);
            series.setMarkerStyle(MarkerStyle.NONE);
        }

        chart.plot(data);
        XDDFChartLegend legend = chart.getOrAddLegend();
        chart.getCTChart().getPlotArea().getCatAxArray(0).addNewTickLblSkip().setVal(skip);
        chart.getCTChart().getPlotArea().getCatAxArray(0).addNewTickMarkSkip().setVal(skip);

        legend.setPosition(LegendPosition.BOTTOM);
    }

    /**
     * Gets the column of a statistic from its ID.
     *
     * @param statistic The ID of the statistic.
     * @param headerMap A list of the statistics and their associated spreadsheet column.
     * @return The column number if the statistic exists, -1 otherwise.
     */
    private int getRowFromStatistic(String statistic, Map<String, Integer> headerMap) {
        for (Map.Entry<String, String> e : statisticsMap.entrySet()) {
            if (e.getValue().equalsIgnoreCase(statistic)) {
                return headerMap.get(e.getKey());
            }
        }
        return -1;
    }

    /**
     * Formats a cell as a link.
     *
     * @param workbook The current session's workbook.
     * @param cell     The cell to be formatted as a link.
     */
    private void formatAsLink(Workbook workbook, Cell cell) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setUnderline(Font.U_SINGLE);
        font.setColor(IndexedColors.CORNFLOWER_BLUE.index);
        style.setFont(font);
        cell.setCellStyle(style);
    }

    /**
     * Formats a cell as a link.
     *
     * @param region
     * @param sheet
     */
    private void setBorder(CellRangeAddress region, Sheet sheet) {
        RegionUtil.setBorderBottom(BorderStyle.MEDIUM, region, sheet);
        RegionUtil.setBorderLeft(BorderStyle.MEDIUM, region, sheet);
        RegionUtil.setBorderRight(BorderStyle.MEDIUM, region, sheet);
        RegionUtil.setBorderTop(BorderStyle.MEDIUM, region, sheet);
    }
}
