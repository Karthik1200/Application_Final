package com.example.Application.service;

import com.example.Application.entity.AuditLog;
import com.example.Application.entity.GateLog;
import com.example.Application.entity.Visitor;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DeviceRgb HEADER_BG  = new DeviceRgb(30, 30, 60);
    private static final DeviceRgb HEADER_FG  = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb ROW_ALT    = new DeviceRgb(240, 242, 255);

    @Autowired private VisitorService visitorService;
    @Autowired private AuditService auditService;
    @Autowired private GateService gateService;


    public byte[] exportVisitorsPdf() throws IOException {
        List<Visitor> visitors = visitorService.getAllVisitors();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);

        addPdfTitle(doc, "Visitor Log Report");

        Table table = new Table(UnitValue.createPercentArray(new float[]{5, 18, 14, 14, 12, 12, 12, 13}));
        table.setWidth(UnitValue.createPercentValue(100));

        String[] headers = {"ID", "Name", "Company", "Host", "Purpose", "Status", "ID Type", "Arrived"};
        addPdfHeaders(table, headers);

        boolean alt = false;
        for (Visitor v : visitors) {
            DeviceRgb bg = alt ? ROW_ALT : new DeviceRgb(255, 255, 255);
            addPdfRow(table, bg,
                    str(v.getId()),
                    v.getFullName(),
                    nvl(v.getCompany()),
                    v.getHostName(),
                    nvl(v.getPurpose()),
                    v.getStatus().name(),
                    nvl(v.getIdType()),
                    v.getGateCheckedInAt() != null ? v.getGateCheckedInAt().format(FMT) :
                    v.getCreatedAt() != null ? v.getCreatedAt().format(FMT) : "—"
            );
            alt = !alt;
        }

        doc.add(table);
        addPdfFooter(doc, visitors.size() + " record(s)");
        doc.close();
        return out.toByteArray();
    }

    public byte[] exportGateLogsPdf() throws IOException {
        List<GateLog> logs = gateService.getTodayLogs();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);

        addPdfTitle(doc, "Gate Log Report");

        Table table = new Table(UnitValue.createPercentArray(new float[]{8, 20, 12, 12, 10, 10, 18}));
        table.setWidth(UnitValue.createPercentValue(100));

        String[] headers = {"ID", "Visitor", "Gate", "Action", "QR Valid", "ID Verified", "Timestamp"};
        addPdfHeaders(table, headers);

        boolean alt = false;
        for (GateLog g : logs) {
            DeviceRgb bg = alt ? ROW_ALT : new DeviceRgb(255, 255, 255);
            addPdfRow(table, bg,
                    str(g.getId()),
                    nvl(g.getVisitorName()),
                    g.getGateId(),
                    g.getAction(),
                    g.isQrValid() ? "Yes" : "No",
                    g.isIdVerified() ? "Yes" : "No",
                    g.getTimestamp() != null ? g.getTimestamp().format(FMT) : "—"
            );
            alt = !alt;
        }

        doc.add(table);
        addPdfFooter(doc, logs.size() + " record(s)");
        doc.close();
        return out.toByteArray();
    }

    public byte[] exportAuditLogsPdf() throws IOException {
        List<AuditLog> logs = auditService.getRecentLogs();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);

        addPdfTitle(doc, "Audit Trail Report");

        Table table = new Table(UnitValue.createPercentArray(new float[]{8, 16, 12, 14, 14, 36}));
        table.setWidth(UnitValue.createPercentValue(100));

        String[] headers = {"ID", "Event", "Entity", "Performed By", "Timestamp", "Description"};
        addPdfHeaders(table, headers);

        boolean alt = false;
        for (AuditLog a : logs) {
            DeviceRgb bg = alt ? ROW_ALT : new DeviceRgb(255, 255, 255);
            addPdfRow(table, bg,
                    str(a.getId()),
                    a.getEventType(),
                    a.getEntityType(),
                    nvl(a.getPerformedBy()),
                    a.getTimestamp() != null ? a.getTimestamp().format(FMT) : "—",
                    nvl(a.getDescription())
            );
            alt = !alt;
        }

        doc.add(table);
        addPdfFooter(doc, logs.size() + " record(s)");
        doc.close();
        return out.toByteArray();
    }


    public byte[] exportVisitorsExcel() throws IOException {
        List<Visitor> visitors = visitorService.getAllVisitors();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Visitors");
            CellStyle headerStyle = createExcelHeaderStyle(wb);
            CellStyle altStyle    = createExcelAltStyle(wb);

            String[] headers = {"ID", "Name", "Email", "Phone", "Company", "Host", "Purpose", "Status", "ID Type", "ID Number", "Arrived"};
            createExcelHeaderRow(sheet, headers, headerStyle);

            int row = 1;
            for (Visitor v : visitors) {
                Row r = sheet.createRow(row);
                CellStyle cs = row % 2 == 1 ? altStyle : null;
                setExcelCells(r, cs,
                        str(v.getId()), v.getFullName(), v.getEmail(), v.getPhone(),
                        nvl(v.getCompany()), v.getHostName(), nvl(v.getPurpose()),
                        v.getStatus().name(), nvl(v.getIdType()), nvl(v.getIdNumber()),
                        v.getGateCheckedInAt() != null ? v.getGateCheckedInAt().format(FMT) :
                        v.getCreatedAt() != null ? v.getCreatedAt().format(FMT) : "—"
                );
                row++;
            }
            autoSizeColumns(sheet, headers.length);
            return toBytes(wb);
        }
    }

    public byte[] exportGateLogsExcel() throws IOException {
        List<GateLog> logs = gateService.getTodayLogs();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Gate Logs");
            CellStyle headerStyle = createExcelHeaderStyle(wb);
            CellStyle altStyle    = createExcelAltStyle(wb);

            String[] headers = {"ID", "Visitor ID", "Visitor Name", "Gate", "Action", "QR Valid", "ID Verified", "Verified By", "Remarks", "Timestamp"};
            createExcelHeaderRow(sheet, headers, headerStyle);

            int row = 1;
            for (GateLog g : logs) {
                Row r = sheet.createRow(row);
                CellStyle cs = row % 2 == 1 ? altStyle : null;
                setExcelCells(r, cs,
                        str(g.getId()), str(g.getVisitorId()), nvl(g.getVisitorName()),
                        g.getGateId(), g.getAction(),
                        g.isQrValid() ? "Yes" : "No",
                        g.isIdVerified() ? "Yes" : "No",
                        nvl(g.getVerifiedBy()), nvl(g.getRemarks()),
                        g.getTimestamp() != null ? g.getTimestamp().format(FMT) : "—"
                );
                row++;
            }
            autoSizeColumns(sheet, headers.length);
            return toBytes(wb);
        }
    }

    public byte[] exportAuditLogsExcel() throws IOException {
        List<AuditLog> logs = auditService.getRecentLogs();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Audit Trail");
            CellStyle headerStyle = createExcelHeaderStyle(wb);
            CellStyle altStyle    = createExcelAltStyle(wb);

            String[] headers = {"ID", "Event Type", "Entity Type", "Entity ID", "Performed By", "Role", "Description", "Timestamp"};
            createExcelHeaderRow(sheet, headers, headerStyle);

            int row = 1;
            for (AuditLog a : logs) {
                Row r = sheet.createRow(row);
                CellStyle cs = row % 2 == 1 ? altStyle : null;
                setExcelCells(r, cs,
                        str(a.getId()), a.getEventType(), a.getEntityType(),
                        str(a.getEntityId()), nvl(a.getPerformedBy()),
                        nvl(a.getPerformedByRole()), nvl(a.getDescription()),
                        a.getTimestamp() != null ? a.getTimestamp().format(FMT) : "—"
                );
                row++;
            }
            autoSizeColumns(sheet, headers.length);
            return toBytes(wb);
        }
    }


    private void addPdfTitle(Document doc, String title) {
        doc.add(new Paragraph("VRGT System — " + title)
                .setFontSize(16)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4));
        doc.add(new Paragraph("Generated: " + java.time.LocalDateTime.now().format(FMT))
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(new DeviceRgb(100, 100, 120))
                .setMarginBottom(12));
    }

    private void addPdfHeaders(Table table, String[] headers) {
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setBold().setFontSize(9).setFontColor(HEADER_FG))
                    .setBackgroundColor(HEADER_BG)
                    .setPadding(5));
        }
    }

    private void addPdfRow(Table table, DeviceRgb bg, String... values) {
        for (String val : values) {
            table.addCell(new Cell()
                    .add(new Paragraph(val).setFontSize(8))
                    .setBackgroundColor(bg)
                    .setPadding(4));
        }
    }

    private void addPdfFooter(Document doc, String summary) {
        doc.add(new Paragraph(summary)
                .setFontSize(9)
                .setFontColor(new DeviceRgb(100, 100, 120))
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(8));
    }


    private CellStyle createExcelHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle createExcelAltStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void createExcelHeaderRow(Sheet sheet, String[] headers, CellStyle style) {
        Row row = sheet.createRow(0);
        row.setHeight((short) 400);
        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void setExcelCells(Row row, CellStyle style, String... values) {
        for (int i = 0; i < values.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = row.createCell(i);
            cell.setCellValue(values[i]);
            if (style != null) cell.setCellStyle(style);
        }
    }

    private void autoSizeColumns(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(width + 512, 15000));
        }
    }

    private byte[] toBytes(XSSFWorkbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private String nvl(String s) { return s != null ? s : "—"; }
    private String str(Object o) { return o != null ? o.toString() : "—"; }
}
