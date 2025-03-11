package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ExcelExporter {
    public static void saveDataToExcel(List<PopulationData> data, String filePath) throws IOException {
        if (data.isEmpty()) {
            System.out.println("⚠️ 저장할 데이터가 없습니다. 엑셀 파일을 생성하지 않습니다.");
            return;
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Population Data");

        // 헤더 작성
        Row headerRow = sheet.createRow(0);
        String[] headers = {"통계년월", "전입 행정기관", "전출 행정기관", "총 인구수", "남자 인구수", "여자 인구수"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // 데이터 추가
        int rowNum = 1;
        for (PopulationData item : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(item.getStatsYm());
            row.createCell(1).setCellValue(item.getMvinAdmmCd());
            row.createCell(2).setCellValue(item.getMvtAdmmCd());
            row.createCell(3).setCellValue(item.getTotNmprCnt());
            row.createCell(4).setCellValue(item.getMaleNmprCnt());
            row.createCell(5).setCellValue(item.getFemlNmprCnt());
        }

        // 파일 저장
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        }

        workbook.close();
        System.out.println("✅ Excel 파일 저장 완료: " + filePath);
    }
}
