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
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
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
public class StatisticsSheetWriter {

    private final Log log;
    private final File statisticsFile;
    private final File statisticsMapFile;
    private Map<String, String> statisticsMap;
    private Map<Long, Map<String, Integer>> statistics;

    private final static Type statMapType = new TypeToken<Map<String, String>>() {}.getType();
    private final Map<String, TrackingPolicy> trackedStatistics = new HashMap<>();

    private void setupTrackers() {
        trackedStatistics.put("mtr_spd", new MaxTrackingPolicy());
        trackedStatistics.put("bms_soc", new LastTrackingPolicy());
        trackedStatistics.put("mc0_dc_i", new MaxTrackingPolicy());
        trackedStatistics.put("mc1_dc_i", new MaxTrackingPolicy());
        trackedStatistics.put("mc0_mtr_tmp", new MaxTrackingPolicy());
        trackedStatistics.put("mc1_mtr_tmp", new MaxTrackingPolicy());
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
        BufferedReader reader = new BufferedReader(new FileReader(statisticsFile));
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
        int currentHeaderColumn = 1;
        // Stat ID as String | Header column
        Map<String, Integer> headerMap = new HashMap<>();
        for (Map.Entry<String, String> e : statisticsMap.entrySet()) {
            headerRow.createCell(currentHeaderColumn).setCellValue(e.getValue());
            headerMap.put(e.getKey(), currentHeaderColumn);
            currentHeaderColumn++;
        }

        int currentRow = 1;
        for (Map.Entry<Long, Map<String, Integer>> e : statistics.entrySet()) {
            Row row = raw.createRow(currentRow);
            row.createCell(0).setCellValue(e.getKey());
            for (Map.Entry<String, Integer> e2 : e.getValue().entrySet()) {
                try {
                    row.createCell(headerMap.get(e2.getKey())).setCellValue(e2.getValue());
                } catch (NullPointerException ignored) {
                }
            }
            currentRow++;
        }

        /*
        // Generate chart
        XSSFDrawing drawing = breakdown.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 7, 15, 26);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("Statistics");
        chart.setTitleOverlay(false);

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.BOTTOM);

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Time");
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Value");

        XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
        int rows = raw.getPhysicalNumberOfRows();
        XDDFNumericalDataSource<Double> timestamps = XDDFDataSourcesFactory.fromNumericCellRange(raw, new CellRangeAddress(1, rows - 1, 0, 0));

        for (Map.Entry<String, Integer> e : headerMap.entrySet()) {
            XDDFNumericalDataSource<Double> dataSource = XDDFDataSourcesFactory.fromNumericCellRange(raw, new CellRangeAddress(1, rows - 1, e.getValue(), e.getValue()));
            XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(timestamps, dataSource);
            series.setTitle(statisticsMap.get(e.getKey()), null);
            series.setSmooth(false);
            series.setMarkerStyle(MarkerStyle.NONE);
        }

        chart.plot(data);

         */
        FileOutputStream outputStream = new FileOutputStream(file);
        workbook.write(outputStream);
        workbook.close();
    }

    private void overview(XSSFWorkbook workbook) {
        XSSFSheet breakdown = workbook.createSheet("Breakdown");
        overviewTitleBlock(workbook, breakdown);
        overviewValues(workbook, breakdown);
    }

    private void overviewTitleBlock(XSSFWorkbook workbook, Sheet sheet) {
        sheet.addMergedRegion(CellRangeAddress.valueOf("B2:E2"));
        sheet.addMergedRegion(CellRangeAddress.valueOf("C3:E3"));
        sheet.addMergedRegion(CellRangeAddress.valueOf("C4:E4"));
        sheet.addMergedRegion(CellRangeAddress.valueOf("B5:E5"));

        // Header Row
        Row r2 = sheet.createRow(1);
        Cell header = r2.createCell(1);
        header.setCellValue("Log #" + log.getId());
        {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);
            header.setCellStyle(style);
        }

        // Creation Date Row
        Row r3 = sheet.createRow(2);
        Cell created = r3.createCell(1);
        created.setCellValue("Created:");
        Cell createdValue = r3.createCell(2);
        createdValue.setCellValue(Util.of(log.getDate()));

        // Uploaded Date Row
        Row r4 = sheet.createRow(3);
        Cell uploaded = r4.createCell(1);
        uploaded.setCellValue("Uploaded:");
        Cell uploadedValue = r4.createCell(2);
        uploadedValue.setCellValue(Util.of(log.getUploadDate()));

        // Open Link Row
        Row r5 = sheet.createRow(4);
        CreationHelper helper = workbook.getCreationHelper();
        XSSFHyperlink link = (XSSFHyperlink) helper.createHyperlink(HyperlinkType.URL);
        link.setAddress(String.format("https://logs.iitmotorsports.org/files/%d/%d.txt", log.getId(), log.getId()));

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

    private void overviewValues(XSSFWorkbook workbook, Sheet sheet) {
        Row r6 = sheet.createRow(6);
        Cell header = r6.createCell(1);
        header.setCellValue("Overview");
        {
            sheet.addMergedRegion(CellRangeAddress.valueOf("B7:E7"));
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);
            header.setCellStyle(style);
        }

        int i = 8;
        addOverviewStatistic(sheet, i++, "Max Speed", " MPH", "mtr_spd");
        addOverviewStatistic(sheet, i++, "Remaining Battery", "%", "bms_soc");
        addOverviewStatistic(sheet, i++, "MC0 Top Current", " A", "mc0_dc_i");
        addOverviewStatistic(sheet, i++, "MC1 Top Current", " A", "mc1_dc_i");
        addOverviewStatistic(sheet, i++, "M0 Top Temp", " \u02DAC", "mc0_mtr_tmp");
        addOverviewStatistic(sheet, i++, "M1 Top Temp", " \u02DAC", "mc1_mtr_tmp");

        setBorder(CellRangeAddress.valueOf("B7:E" + (i - 1)), sheet);
    }

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

    private void formatAsLink(Workbook workbook, Cell cell) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setUnderline(Font.U_SINGLE);
        font.setColor(IndexedColors.CORNFLOWER_BLUE.index);
        style.setFont(font);
        cell.setCellStyle(style);
    }

    private void setBorder(CellRangeAddress region, Sheet sheet) {
        RegionUtil.setBorderBottom(BorderStyle.MEDIUM, region, sheet);
        RegionUtil.setBorderLeft(BorderStyle.MEDIUM, region, sheet);
        RegionUtil.setBorderRight(BorderStyle.MEDIUM, region, sheet);
        RegionUtil.setBorderTop(BorderStyle.MEDIUM, region, sheet);
    }
}
