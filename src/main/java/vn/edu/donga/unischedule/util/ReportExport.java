package vn.edu.donga.unischedule.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import vn.edu.donga.unischedule.model.Report;

import java.awt.Color;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class ReportExport {
    private static final String[] FONT_PATHS={
        "C:/Windows/Fonts/arial.ttf",
        "C:/Windows/Fonts/segoeui.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/Library/Fonts/Arial Unicode.ttf"
    };

    private ReportExport() { }

    public static void pdf(Report report,Path path) throws IOException {
        atomicWrite(path,output->{
            try(PDDocument document=new PDDocument()) {
                PDFont font=loadUnicodeFont(document);
                try(PdfWriter writer=new PdfWriter(document,font)) {
                    writer.heading("Báo cáo thống kê UniSchedule",16);
                    writer.line(report.filter().from()+" - "+report.filter().to(),10);
                    writer.line(report.scope(),9);
                    writer.line("Buổi học: "+report.sessions()+"   Phòng có lịch: "+report.usedRooms()+"   Lượt đăng ký: "+report.registrations()+"   Yêu cầu: "+report.requests(),9);
                    for(Report.Table table:report.tables())writer.table(table);
                }
                document.save(output);
            }
        });
    }

    public static void word(Report report,Path path) throws IOException {
        atomicWrite(path,output->{
            try(XWPFDocument document=new XWPFDocument()) {
                paragraph(document,"Báo cáo thống kê UniSchedule",18,true);
                paragraph(document,report.filter().from()+" - "+report.filter().to(),11,false);
                paragraph(document,report.scope(),10,false);
                paragraph(document,"Buổi học: "+report.sessions()+" | Phòng có lịch: "+report.usedRooms()+" | Lượt đăng ký: "+report.registrations()+" | Yêu cầu: "+report.requests(),10,false);
                for(Report.Table data:report.tables()) {
                    paragraph(document,data.title(),14,true);
                    paragraph(document,data.note(),9,false);
                    int rowCount=Math.max(1,data.rows().size())+1;
                    XWPFTable table=document.createTable(rowCount,data.columns().size());
                    for(int column=0;column<data.columns().size();column++)setWordCell(table.getRow(0).getCell(column),data.columns().get(column),true);
                    if(data.rows().isEmpty()) {
                        setWordCell(table.getRow(1).getCell(0),"Không có dữ liệu",false);
                    } else {
                        for(int row=0;row<data.rows().size();row++)
                            for(int column=0;column<data.columns().size();column++)
                                setWordCell(table.getRow(row+1).getCell(column),String.valueOf(data.rows().get(row).get(column)),false);
                    }
                }
                document.write(output);
            }
        });
    }

    public static void excel(Report report,Path path) throws IOException {
        atomicWrite(path,output->{
            try(XSSFWorkbook workbook=new XSSFWorkbook()) {
                CellStyle heading=headingStyle(workbook);
                Sheet summary=workbook.createSheet("Tổng quan");
                String[][] overview={{"BÁO CÁO THỐNG KÊ UNISCHEDULE",""},{"Từ ngày",report.filter().from().toString()},{"Đến ngày",report.filter().to().toString()},{"Phạm vi",report.scope()},{"Buổi học",String.valueOf(report.sessions())},{"Phòng có lịch",String.valueOf(report.usedRooms())},{"Lượt đăng ký",String.valueOf(report.registrations())},{"Yêu cầu",String.valueOf(report.requests())}};
                for(int row=0;row<overview.length;row++) {
                    Row excelRow=summary.createRow(row);
                    excelRow.createCell(0).setCellValue(overview[row][0]);excelRow.createCell(1).setCellValue(overview[row][1]);
                }
                summary.getRow(0).getCell(0).setCellStyle(heading);summary.setColumnWidth(0,26*256);summary.setColumnWidth(1,55*256);
                for(Report.Table data:report.tables()) {
                    Sheet sheet=workbook.createSheet(safeSheetName(data.title()));
                    Row note=sheet.createRow(0);note.createCell(0).setCellValue(data.note());
                    Row header=sheet.createRow(1);
                    for(int column=0;column<data.columns().size();column++){Cell cell=header.createCell(column);cell.setCellValue(data.columns().get(column));cell.setCellStyle(heading);}
                    for(int row=0;row<data.rows().size();row++) {
                        Row excelRow=sheet.createRow(row+2);
                        for(int column=0;column<data.columns().size();column++)setExcelCell(excelRow.createCell(column),data.rows().get(row).get(column));
                    }
                    sheet.createFreezePane(0,2);
                    if(!data.columns().isEmpty())sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(1,Math.max(1,data.rows().size()+1),0,data.columns().size()-1));
                    for(int column=0;column<data.columns().size();column++){sheet.autoSizeColumn(column);sheet.setColumnWidth(column,Math.min(sheet.getColumnWidth(column)+512,60*256));}
                }
                workbook.write(output);
            }
        });
    }

    private static void paragraph(XWPFDocument document,String text,int size,boolean bold) {
        XWPFRun run=document.createParagraph().createRun();run.setText(text);run.setFontFamily("Arial");run.setFontSize(size);run.setBold(bold);
    }

    private static void setWordCell(XWPFTableCell cell,String text,boolean bold) {
        cell.removeParagraph(0);XWPFRun run=cell.addParagraph().createRun();run.setText(text);run.setFontFamily("Arial");run.setFontSize(9);run.setBold(bold);
    }

    private static CellStyle headingStyle(Workbook workbook) {
        CellStyle style=workbook.createCellStyle();Font font=workbook.createFont();font.setBold(true);font.setColor(IndexedColors.WHITE.getIndex());style.setFont(font);style.setFillForegroundColor(IndexedColors.INDIGO.getIndex());style.setFillPattern(FillPatternType.SOLID_FOREGROUND);style.setWrapText(true);return style;
    }

    private static void setExcelCell(Cell cell,Object value) {
        if(value instanceof Number number)cell.setCellValue(number.doubleValue());
        else if(value instanceof Boolean bool)cell.setCellValue(bool);
        else cell.setCellValue(String.valueOf(value));
    }

    private static String safeSheetName(String value) {
        String result=value.replaceAll("[\\\\/?*\\[\\]:]"," ").trim();return result.substring(0,Math.min(31,result.length()));
    }

    private static PDFont loadUnicodeFont(PDDocument document) throws IOException {
        for(String candidate:FONT_PATHS)if(Files.isRegularFile(Path.of(candidate)))return PDType0Font.load(document,Path.of(candidate).toFile());
        throw new IOException("Không tìm thấy font Unicode để tạo PDF.");
    }

    @FunctionalInterface private interface OutputWriter {void write(OutputStream output) throws IOException;}
    private static void atomicWrite(Path path,OutputWriter writer) throws IOException {
        Path target=path.toAbsolutePath();Path parent=target.getParent();if(parent!=null)Files.createDirectories(parent);
        Path temp=Files.createTempFile(parent,"report-",".tmp");
        try {
            try(OutputStream output=Files.newOutputStream(temp)){writer.write(output);}
            try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ex){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temp);}
    }

    private static final class PdfWriter implements AutoCloseable {
        private static final float MARGIN=24,WIDTH=PDRectangle.A4.getHeight()-MARGIN*2,TOP=PDRectangle.A4.getWidth()-MARGIN;
        private final PDDocument document;private final PDFont font;private PDPageContentStream stream;private float y;
        PdfWriter(PDDocument document,PDFont font) throws IOException{this.document=document;this.font=font;newPage();}
        void heading(String text,float size) throws IOException{ensure(28);text(text,MARGIN,y,size,true);y-=size+10;}
        void line(String text,float size) throws IOException{for(String part:wrap(text,WIDTH,size)){ensure(size+5);text(part,MARGIN,y,size,false);y-=size+4;}y-=3;}
        void table(Report.Table table) throws IOException {
            ensure(55);heading(table.title(),13);line(table.note(),8);
            drawTableRow(table.columns(),true,table.columns());
            if(table.rows().isEmpty())line("Không có dữ liệu",9);
            else for(List<Object> row:table.rows())drawTableRow(row.stream().map(String::valueOf).toList(),false,table.columns());
            y-=10;
        }
        private void drawTableRow(List<String> cells,boolean header,List<String> columns) throws IOException {
            float columnWidth=WIDTH/columns.size(),fontSize=columns.size()>6?6.5f:7.5f;
            List<List<String>> wrapped=new ArrayList<>();int lines=1;
            for(String cell:cells){List<String> parts=wrap(cell,columnWidth-6,fontSize);wrapped.add(parts);lines=Math.max(lines,parts.size());}
            float height=lines*(fontSize+2)+6;
            if(y-height<MARGIN){newPage();if(!header)drawTableRow(columns,true,columns);}
            stream.setStrokingColor(new Color(180,185,195));stream.setLineWidth(.5f);
            stream.addRect(MARGIN,y-height,WIDTH,height);stream.stroke();
            for(int i=1;i<columns.size();i++){float x=MARGIN+i*columnWidth;stream.moveTo(x,y);stream.lineTo(x,y-height);stream.stroke();}
            for(int column=0;column<wrapped.size();column++)for(int row=0;row<wrapped.get(column).size();row++)text(wrapped.get(column).get(row),MARGIN+column*columnWidth+3,y-fontSize-4-row*(fontSize+2),fontSize,header);
            y-=height;
        }
        private List<String> wrap(String value,float maxWidth,float size) throws IOException {
            if(value==null||value.isBlank())return List.of("");List<String> lines=new ArrayList<>();StringBuilder current=new StringBuilder();
            for(String word:value.replace('\n',' ').split("\\s+")){String candidate=current.isEmpty()?word:current+" "+word;if(font.getStringWidth(candidate)/1000*size<=maxWidth)current=new StringBuilder(candidate);else{if(!current.isEmpty())lines.add(current.toString());current=new StringBuilder(word);}}
            if(!current.isEmpty())lines.add(current.toString());return lines.isEmpty()?List.of(""):lines;
        }
        private void ensure(float height) throws IOException{if(y-height<MARGIN)newPage();}
        private void text(String value,float x,float baseline,float size,boolean bold) throws IOException{stream.beginText();stream.setFont(font,size);stream.setNonStrokingColor(bold?new Color(45,42,120):Color.DARK_GRAY);stream.newLineAtOffset(x,baseline);stream.showText(value);stream.endText();}
        private void newPage() throws IOException{if(stream!=null)stream.close();PDPage page=new PDPage(new PDRectangle(PDRectangle.A4.getHeight(),PDRectangle.A4.getWidth()));document.addPage(page);stream=new PDPageContentStream(document,page);y=TOP;}
        public void close() throws IOException{if(stream!=null)stream.close();}
    }
}
