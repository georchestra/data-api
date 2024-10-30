package com.camptocamp.opendata.ogc.features.http.codec.xls;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.geotools.util.Converters;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

class StreamingWorkbookWriter {

    private ZipOutputStream zout;

    private StreamingRow lastRow;

    private XMLStreamWriter sheetWriter;

    @SneakyThrows
    StreamingWorkbookWriter(OutputStream out) {
        this.zout = new ZipOutputStream(out, StandardCharsets.UTF_8);
        ZipEntry sheet1 = new ZipEntry("xl/worksheets/sheet1.xml");
        this.zout.putNextEntry(sheet1);
        sheetWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(zout, "UTF-8");
        startSheet(sheetWriter);
    }

    /**
     * Outputs a worksheet document header like
     * 
     * <pre>
     * {@code
     * <?xml version="1.0" encoding="utf-8" standalone="yes"?>
     * <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
     * xmlns:r=
     * "http://schemas.openxmlformats.org/officeDocument/2006/relationships">
     *  <dimension ref="A1" />
     *  <sheetViews>
     *   <sheetView workbookViewId="0"/>
     *  </sheetViews>
     *  <sheetFormatPr defaultRowHeight="15.0"/>
     * <sheetData>
     * }
     * </pre>
     */

    private @SneakyThrows void startSheet(XMLStreamWriter sheet) {
        sheet.writeStartDocument();
        sheet.writeStartElement("worksheet");
        sheet.writeDefaultNamespace("http://schemas.openxmlformats.org/spreadsheetml/2006/main");
        sheet.writeNamespace("r", "http://schemas.openxmlformats.org/officeDocument/2006/relationships");

        sheet.writeStartElement("dimension");
        sheet.writeAttribute("ref", "A1");
        sheet.writeEndElement();

        sheet.writeStartElement("sheetViews");
        sheet.writeStartElement("sheetView");
        sheet.writeAttribute("workbookViewId", "0");
        sheet.writeEndElement();
        sheet.writeEndElement();

        sheet.writeStartElement("sheetFormatPr");
        sheet.writeAttribute("defaultRowHeight", "15.0");
        sheet.writeEndElement();

        sheet.writeStartElement("sheetData");
    }

    @SneakyThrows
    public void finish() {
        sheetWriter.writeEndElement();// sheetData
        sheetWriter.writeEndDocument();
        sheetWriter.close();

        addTemplate(zout, "_rels/.rels");
        addTemplate(zout, "docProps/app.xml");
        addTemplate(zout, "docProps/core.xml");
        addTemplate(zout, "xl/_rels/workbook.xml.rels");
        addTemplate(zout, "xl/sharedStrings.xml");
        addTemplate(zout, "xl/styles.xml");
        addTemplate(zout, "xl/workbook.xml");
        addTemplate(zout, "[Content_Types].xml");

        zout.finish();
        sheetWriter = null;
        zout = null;
    }

    private @SneakyThrows void addTemplate(ZipOutputStream zout, String fileName) {
        ZipEntry entry = new ZipEntry(fileName);
        zout.putNextEntry(entry);
        String resourceName = "workbook_template/" + fileName;
        try (InputStream in = getClass().getResourceAsStream(resourceName)) {
            Objects.requireNonNull(in, () -> "Resource not found" + getClass().getResource(resourceName));
            in.transferTo(zout);
        }
    }

    public StreamingRow newRow() {
        if (null != lastRow && !lastRow.finished) {
            lastRow.end();
        }
        if (null == lastRow) {
            lastRow = new StreamingRow(1, sheetWriter, this);
        } else {
            lastRow = lastRow.nextRow();
        }
        return lastRow;
    }

    @RequiredArgsConstructor
    public static class StreamingRow {

        private final int rowNum;
        private final XMLStreamWriter writer;
        private final StreamingWorkbookWriter workbook;

        private boolean started;
        private boolean finished;

        private ColumnNames colNames;

        public StreamingRow nextRow() {
            end();
            return new StreamingRow(rowNum + 1, writer, workbook);
        }

        public @SneakyThrows StreamingRow start() {
            if (!started) {
                colNames = new ColumnNames(rowNum);
                writer.writeStartElement("row");
                writer.writeAttribute("r", String.valueOf(rowNum));
                started = true;
            }
            return this;
        }

        public @SneakyThrows StreamingRow addColumnValue(Object value) {
            start();
            final String nextColumn = colNames.nextColumn();
            if (null != value) {
                writer.writeStartElement("c");
                writer.writeAttribute("r", nextColumn);

                if (value instanceof Number) {
                    writer.writeStartElement("v");
                    writer.writeCharacters(value.toString());
                    writer.writeEndElement();
                } else {
                    writer.writeAttribute("t", "inlineStr");
                    writer.writeStartElement("is");
                    writer.writeStartElement("t");
                    writeValue(value);
                    writer.writeEndElement();// t
                    writer.writeEndElement();// is
                }
                writer.writeEndElement();// c
            }
            return this;
        }

        private @SneakyThrows void writeValue(Object nextCellValue) {
            writer.writeCharacters(Converters.convert(nextCellValue, String.class));
        }

        public @SneakyThrows StreamingWorkbookWriter end() {
            if (!finished) {
                writer.writeEndElement();
                finished = true;
            }
            return workbook;
        }
    }

    @RequiredArgsConstructor
    private static class ColumnNames {

        static final List<String> letters = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
                "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z");

        private final int rowNum;

        private int index = 0;

        public String nextColumn() {
            String colName = "";
            int n = ++index;

            while (n > 0) {
                int i = (n - 1) % letters.size();
                colName = letters.get(i) + colName;
                n = (n - 1) / letters.size();
            }
            return colName + rowNum;
        }
    }
}
