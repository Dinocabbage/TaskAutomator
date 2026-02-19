package org.example;

import java.sql.*;
import java.io.*;
import java.util.Scanner;

public class CsvToDatabase {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("관리번호 : ");
        String fileName = sc.nextLine().trim();
        System.out.print("DB명 : ");
        String dbName = sc.nextLine().trim();
        System.out.print("테이블명 : ");
        String tableName = sc.nextLine().trim();

        String url = "jdbc:mariadb://localhost:3306/" + dbName + "?allowLoadLocalInfile=true";
        String user = "root";
        String password = "1234";

        File dir = new File("C:/Users/VCL_Cabbage/Downloads/20250627_4번방_배준영님");
        Connection connection = null;
        Statement statement = null;

        try {
            Class.forName("org.mariadb.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, password);
            statement = connection.createStatement();

            File[] files = dir.listFiles((d, name) -> name.contains(fileName) && name.endsWith(".csv"));
            if (files == null || files.length == 0) {
                System.out.println("일치하는 CSV 파일을 찾을 수 없습니다.");
                return;
            }

            for (File file : files) {
                // 첫 줄을 읽어 구분자 확인
                String headerLine;
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    headerLine = reader.readLine();
                }

                if (headerLine == null) {
                    System.out.println("파일 " + file.getName() + "은 비어 있습니다. 건너뜁니다.");
                    continue;
                }

                // 구분자 확인 (기본 콤마)
                String delimiter = ",";
                if (!headerLine.contains(",") && headerLine.contains("|")) {
                    delimiter = "|";
                }

                // 컬럼 목록 명시 (테이블 컬럼 순서와 맞춰서 지정)
                String columns = "TYPE_NM, COMM_NM, STD_YM, H_M_0009, H_M_1014, H_M_1519, H_M_2024, H_M_2529, H_M_3034, H_M_3539, H_M_4044, H_M_4549, H_M_5054, H_M_5559, H_M_6064, H_M_6569, H_M_7000, H_W_0009, H_W_1014, H_W_1519, H_W_2024, H_W_2529, H_W_3034, H_W_3539, H_W_4044, H_W_4549, H_W_5054, H_W_5559, H_W_6064, H_W_6569, H_W_7000, W_M_0009, W_M_1014, W_M_1519, W_M_2024, W_M_2529, W_M_3034, W_M_3539, W_M_4044, W_M_4549, W_M_5054, W_M_5559, W_M_6064, W_M_6569, W_M_7000, W_W_0009, W_W_1014, W_W_1519, W_W_2024, W_W_2529, W_W_3034, W_W_3539, W_W_4044, W_W_4549, W_W_5054, W_W_5559, W_W_6064, W_W_6569, W_W_7000, V_M_0009, V_M_1014, V_M_1519, V_M_2024, V_M_2529, V_M_3034, V_M_3539, V_M_4044, V_M_4549, V_M_5054, V_M_5559, V_M_6064, V_M_6569, V_M_7000, V_W_0009, V_W_1014, V_W_1519, V_W_2024, V_W_2529, V_W_3034, V_W_3539, V_W_4044, V_W_4549, V_W_5054, V_W_5559, V_W_6064, V_W_6569, V_W_7000";

                // 쿼리 생성
                String query = String.format(
                        "LOAD DATA LOCAL INFILE '%s' INTO TABLE %s " +
                                "CHARACTER SET utf8 " +
                                "FIELDS TERMINATED BY '%s' ENCLOSED BY '\"' " +
                                "LINES TERMINATED BY '\n' IGNORE 1 LINES (%s);",
                        file.getAbsolutePath().replace("\\", "/"),
                        tableName,
                        delimiter,
                        columns
                );

                System.out.printf("[%s] 로딩 시작...\n", file.getName());
                int count = statement.executeUpdate(query);
                System.out.printf("[%s] %d개 행 삽입 완료\n", file.getName(), count);
            }
        } catch (SQLException e) {
            System.err.println("SQL 오류 발생: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC 드라이버 로드 실패: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("파일 입출력 오류: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
