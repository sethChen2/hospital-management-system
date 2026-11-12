package com.hospital.utils;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.ExcelImportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.ImportParams;
import org.apache.poi.ss.usermodel.Workbook;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;

/**
 * Excel 导入导出（基于 easypoi）
 */
public final class ExcelUtil {

    private ExcelUtil() {
    }

    /** 导出并直接写入响应流，文件名按 RFC 5987 编码避免中文乱码 */
    public static void export(HttpServletResponse response, String fileName,
                              String sheetName, Class<?> clazz, List<?> data) throws IOException {
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(null, sheetName), clazz, data);
        try {
            response.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String encoded = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment;filename*=UTF-8''" + encoded + ".xlsx");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            workbook.write(response.getOutputStream());
            response.flushBuffer();
        } finally {
            workbook.close();
        }
    }

    /** 读取第一行表头的数据行 */
    public static <T> List<T> importExcel(InputStream in, Class<T> clazz) throws Exception {
        ImportParams params = new ImportParams();
        params.setHeadRows(1);
        params.setNeedVerify(false);
        return ExcelImportUtil.importExcel(in, clazz, params);
    }
}
