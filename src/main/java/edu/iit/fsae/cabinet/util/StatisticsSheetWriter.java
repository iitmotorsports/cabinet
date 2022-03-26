package edu.iit.fsae.cabinet.util;

import com.google.gson.reflect.TypeToken;
import edu.iit.fsae.cabinet.Constants;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.MarkerStyle;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFLineChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
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
    private final File statisticsFile;
    private final File statisticsMapFile;

    private final static Type statMapType = new TypeToken<Map<String, String>>() {}.getType();

    private Map<String, String> statisticsMap;
    private Map<Long, Map<String, Integer>> statistics;

    /**
     * Parses the statistics file.
     */
    public void parse() throws IOException {
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
    public void write(File file) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Statistics");

        // Create Header
        Row headerRow = sheet.createRow(0);
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
            Row row = sheet.createRow(currentRow);
            row.createCell(0).setCellValue(e.getKey());
            for (Map.Entry<String, Integer> e2 : e.getValue().entrySet()) {
                row.createCell(headerMap.get(e2.getKey())).setCellValue(e2.getValue());
            }
            currentRow++;
        }

        // Generate chart
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 0, 15, 26);
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
        int rows = sheet.getPhysicalNumberOfRows();
        XDDFNumericalDataSource<Double> timestamps = XDDFDataSourcesFactory.fromNumericCellRange(sheet, new CellRangeAddress(1, rows - 1, 0, 0));

        for (Map.Entry<String, Integer> e : headerMap.entrySet()) {
            XDDFNumericalDataSource<Double> dataSource = XDDFDataSourcesFactory.fromNumericCellRange(sheet, new CellRangeAddress(1, rows - 1, e.getValue(), e.getValue()));
            XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(timestamps, dataSource);
            series.setTitle(statisticsMap.get(e.getKey()), null);
            series.setSmooth(false);
            series.setMarkerStyle(MarkerStyle.NONE);
        }

        chart.plot(data);

        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            workbook.write(outputStream);
            workbook.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
