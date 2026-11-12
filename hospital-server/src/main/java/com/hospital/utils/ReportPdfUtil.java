package com.hospital.utils;

import com.hospital.entity.po.Prescription;
import com.hospital.entity.po.RegisterCheck;
import com.hospital.entity.po.RegisterRecord;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 就诊报告单 PDF 生成
 */
public final class ReportPdfUtil {

    /** 中文字体候选（Windows 自带），取到第一个能用的 */
    private static final String[] FONT_CANDIDATES = {
            "C:/Windows/Fonts/simsun.ttc,0",
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/msyh.ttc,0",
            "STSong-Light"
    };

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final float[] WIDTHS_DRUG = {4f, 2f, 1.5f, 2f};
    private static final float[] WIDTHS_INFO = {2f, 3f, 2f, 3f};

    private ReportPdfUtil() {
    }

    public static byte[] build(RegisterRecord record) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 45, 45);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont baseFont = resolveBaseFont();
            Font title = new Font(baseFont, 20, Font.BOLD);
            Font h2 = new Font(baseFont, 12.5f, Font.BOLD);
            Font body = new Font(baseFont, 10.5f);
            Font small = new Font(baseFont, 9.5f);

            Paragraph t = new Paragraph("医院就诊报告单", title);
            t.setAlignment(Element.ALIGN_CENTER);
            t.setSpacingAfter(6);
            document.add(t);

            Paragraph sub = new Paragraph("挂号单号：" + record.getRegisterId(), small);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(14);
            document.add(sub);

            document.add(sectionTitle("一、就诊信息", h2));
            document.add(infoTable(record, body, small));

            document.add(sectionTitle("二、诊断结论", h2));
            Paragraph diagnosis = new Paragraph(
                    isBlank(record.getDiagnosis()) ? "—" : record.getDiagnosis(), body);
            diagnosis.setSpacingAfter(14);
            diagnosis.setLeading(18);
            document.add(diagnosis);

            document.add(sectionTitle("三、处方明细", h2));
            document.add(drugTable(record.getPrescriptionList(), body, small));

            document.add(sectionTitle("四、检查项目", h2));
            document.add(checkTable(record.getCheckList(), body, small));

            Paragraph total = new Paragraph("合计费用：" + money(record.getTotalCost()) + " 元",
                    new Font(baseFont, 12.5f, Font.BOLD));
            total.setSpacingBefore(12);
            document.add(total);

            Paragraph footer = new Paragraph(
                    "本报告单由医院管理系统自动生成，仅供就诊参考。", small);
            footer.setSpacingBefore(26);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
        } catch (DocumentException e) {
            throw new IllegalStateException("生成报告单失败：" + e.getMessage(), e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private static BaseFont resolveBaseFont() {
        for (String path : FONT_CANDIDATES) {
            try {
                return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception ignored) {
                // 换下一个候选字体
            }
        }
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new IllegalStateException("无法加载 PDF 字体", e);
        }
    }

    private static Paragraph sectionTitle(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(10);
        p.setSpacingAfter(6);
        return p;
    }

    private static PdfPTable infoTable(RegisterRecord r, Font body, Font small) {
        PdfPTable table = new PdfPTable(WIDTHS_INFO);
        table.setWidthPercentage(100);
        addPair(table, "患者姓名", r.getPatientName(), body);
        addPair(table, "性别", r.getPatientGender(), body);
        addPair(table, "联系电话", r.getPatientPhone(), body);
        addPair(table, "就诊科室", r.getDepartment(), body);
        addPair(table, "接诊医生", r.getDoctorName(), body);
        addPair(table, "就诊时间", r.getAppointTime() == null
                ? "—" : r.getAppointTime().format(TIME_FMT), body);
        addPair(table, "挂号状态", r.getRegisterStatus(), body);
        addPair(table, "缴费状态", r.getPayStatus(), body);
        return table;
    }

    private static void addPair(PdfPTable table, String label, String value, Font body) {
        table.addCell(headerCell(label, body));
        table.addCell(valueCell(value, body));
    }

    private static PdfPTable drugTable(List<Prescription> list, Font body, Font small) {
        PdfPTable table = new PdfPTable(WIDTHS_DRUG);
        table.setWidthPercentage(100);
        table.addCell(headerCell("药品名称", body));
        table.addCell(headerCell("单价(元)", body));
        table.addCell(headerCell("数量", body));
        table.addCell(headerCell("小计(元)", body));
        if (list == null || list.isEmpty()) {
            table.addCell(spanCell("本次就诊未开具处方", small, 4));
            return table;
        }
        for (Prescription p : list) {
            table.addCell(valueCell(p.getDrugName(), body));
            table.addCell(valueCell(money(p.getPrice()), body));
            table.addCell(valueCell(String.valueOf(p.getDrugNum()), body));
            table.addCell(valueCell(money(p.getSubtotal()), body));
        }
        return table;
    }

    private static PdfPTable checkTable(List<RegisterCheck> list, Font body, Font small) {
        PdfPTable table = new PdfPTable(new float[]{4f, 2f});
        table.setWidthPercentage(100);
        table.addCell(headerCell("检查项目", body));
        table.addCell(headerCell("费用(元)", body));
        if (list == null || list.isEmpty()) {
            table.addCell(spanCell("本次就诊未开具检查", small, 2));
            return table;
        }
        for (RegisterCheck c : list) {
            table.addCell(valueCell(c.getCheckName(), body));
            table.addCell(valueCell(money(c.getPrice()), body));
        }
        return table;
    }

    private static PdfPCell headerCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new com.itextpdf.text.BaseColor(238, 242, 248));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private static PdfPCell valueCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setPadding(5);
        return cell;
    }

    private static PdfPCell spanCell(String text, Font font, int colspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setColspan(colspan);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private static String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
