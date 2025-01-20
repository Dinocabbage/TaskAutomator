package org.example;

import java.sql.*;
import java.io.*;
import java.util.Scanner;

public class CsvToDatabase {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("관리번호 : ");
        String fileName = sc.nextLine();
        System.out.println();
        System.out.print("DB명 : ");
        String dbName = sc.nextLine();
        System.out.println();
        System.out.print("테이블명 : ");
        String tableName = sc.nextLine();


        // 데이터베이스 연결 변수
        String url = "jdbc:mariadb://localhost:3306/" + dbName;
        String user = "root";
        String password = "1234";

        // CSV 파일이 저장된 경로
        File dir = new File("D:/data_busan/export/raw_export");

        // Connection 객체 선언
        Connection connection = null;
        Statement statement = null;



        try {
            // MariaDB JDBC 드라이버 로드
            Class.forName("org.mariadb.jdbc.Driver");

            // 데이터베이스 연결
            connection = DriverManager.getConnection(url, user, password);
            statement = connection.createStatement();

            // 디렉토리에서 fileName으로 시작하는 모든 CSV 파일을 찾기
            File[] files = dir.listFiles((dir1, name) -> name.startsWith(fileName) && name.endsWith(".csv"));

            // 파일들에 대해 반복하며 데이터 삽입
            for (File file : files) {
                // LOAD DATA INFILE 쿼리 생성
                String query = String.format("LOAD DATA INFILE '%s' INTO TABLE " + tableName + " " +
                                "FIELDS TERMINATED BY '|' ENCLOSED BY '\"' LINES TERMINATED BY '\\r\\n' IGNORE 1 LINES;",
                        file.getAbsolutePath().replace("\\", "/"));  // 경로 구분자는 '/'로 변경

                System.out.print("파일 " + file.getName() + " 데이터 삽입 중...");
                // SQL 실행
                statement.executeUpdate(query);
                System.out.print("\r");
                System.out.println("파일 " + file.getName() + "이(가) 데이터베이스에 삽입되었습니다.");
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            // 자원 해제
            try {
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
