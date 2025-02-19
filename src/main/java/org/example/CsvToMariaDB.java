package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.*;
import java.sql.*;
import java.util.regex.Pattern;

public class CsvToMariaDB {

    // 폴더 경로 (역슬래시(\)는 이스케이프 처리하거나, 전체 경로 앞에 r"..."와 같이 raw string을 쓸 수 없음)
    private static final String FOLDER_PATH = "D:\\data_busan\\비스타컨설팅연구소(주)_250211\\Data";
    // MariaDB 접속 정보 (실제 환경에 맞게 수정)
    private static final String JDBC_URL = "jdbc:mariadb://localhost:3306/vcl_raw_db";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "1234";

    public static void main(String[] args) {
        try {
            // JDBC 드라이버 로드 (MariaDB Connector/J 2.0 이상에서는 생략 가능)
            Class.forName("org.mariadb.jdbc.Driver");

            // 데이터베이스 연결
            try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
                // 지정한 폴더 내의 모든 *.csv 파일 가져오기
                DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(FOLDER_PATH), "*.csv");
                for (Path filePath : stream) {
                    String fileName = filePath.getFileName().toString();
                    System.out.println("Processing file: " + fileName);

                    // 파일명으로부터 타겟 테이블 결정
                    String tableName = determineTableName(fileName);
                    if (tableName == null) {
                        System.out.println("Skipping file (table mapping not found): " + fileName);
                        continue;
                    }
                    System.out.println("Target table: " + tableName);

                    // 파일의 절대 경로를 얻어, 백슬래시를 슬래시로 치환 (MariaDB가 인식)
                    String absolutePath = filePath.toAbsolutePath().toString().replace("\\", "/");

                    // LOAD DATA LOCAL INFILE 쿼리 작성
                    // - FIELDS TERMINATED BY ',' : 필드 구분자
                    // - OPTIONALLY ENCLOSED BY '"' : 필드가 큰따옴표로 감싸져 있는 경우 처리
                    // - LINES TERMINATED BY '\n' : 각 행의 구분자 (환경에 따라 '\r\n' 필요할 수 있음)
                    // - IGNORE 1 LINES : 헤더(첫 번째 행) 무시
                    String loadQuery = "LOAD DATA LOCAL INFILE '" + absolutePath + "' " +
                            "INTO TABLE " + tableName + " " +
                            "FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"' " +
                            "LINES TERMINATED BY '\\r\\n' " +
                            "IGNORE 1 LINES";

                    System.out.println("Executing query: " + loadQuery);
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(loadQuery);
                        System.out.println("Loaded file into table: " + tableName);
                    } catch (Exception e) {
                        System.err.println("Error loading file " + fileName + " into table " + tableName);
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 파일명에서 키워드를 기반으로 타겟 테이블명을 결정합니다.
     * 매핑되지 않는 경우 null을 반환합니다.
     */
    private static String determineTableName(String fileName) {
        String lowerName = fileName.toLowerCase();
        String tableName;
        if (lowerName.contains("성연령별 카드소비")) {
            tableName = "busan_cd_cspt_sa";
        } else if (lowerName.contains("시간대별 카드소비")) {
            tableName = "busan_cd_cspt_tb";
        } else if (lowerName.contains("업종별 카드소비")) {
            tableName = "busan_cd_cspt_ind";
        } else if (lowerName.contains("유입지별 카드소비")) {
            tableName = "busan_cd_cspt_inf";
        } else if (lowerName.contains("성연령별 생활인구")) {
            tableName = "busan_cd_sa_pop";
        } else if (lowerName.contains("시간대별 생활인구")) {
            tableName = "busan_cd_tb_pop";
        } else {
            return null;
        }

        // 파일명에 "비율X" (또는 "비율x")가 포함되어 있으면 테이블명 뒤에 _noratio 추가
        if (lowerName.contains("비율x")) {
            tableName += "_noratio";
        }

        return tableName;
    }
}