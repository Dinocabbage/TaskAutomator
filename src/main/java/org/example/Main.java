package org.example;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            // 2021년 10월 ~ 2025년 3월 데이터 조회
            List<PopulationData> populationData = PopulationApiClient.fetchAllPopulationData();

            System.out.println("🔍 조회된 데이터 개수: " + populationData.size());
            for (PopulationData data : populationData) {
                System.out.println(data);
            }

            // Excel 파일 저장
            ExcelExporter.saveDataToExcel(populationData, "population_movement.xlsx");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
