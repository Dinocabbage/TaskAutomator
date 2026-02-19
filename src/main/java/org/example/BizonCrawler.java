package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * SBIZ 상권 polygon 수집기 (CSV 버전)
 *
 *  - bizonNum 10000 ~ 20000까지 반복
 *  - 각 요청 결과(JSON 배열)를 파싱
 *  - 결과를 sbiz_bizon_shapes.csv 로 저장
 *
 * CSV 컬럼:
 * req_bizonNum,mjrBzznno,mjrBizonNm,centerXCrdnt,centerYCrdnt,geom
 */
public class BizonCrawler {

    // 응답 JSON 구조 DTO
    public static class BizonRecord {
        public Double centerXCrdnt;
        public String mjrBzznno;
        public Double centerYCrdnt;
        public String mjrBizonNm;
        public String geom;

        public BizonRecord() {}
    }

    public static void main(String[] args) {
        final int START = 10000;
        final int END   = 51000;

        ObjectMapper mapper = new ObjectMapper();

        // try-with-resources 로 파일 열기
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("sbiz_bizon_shapes.csv", false))) {

            // 1) 헤더 라인 쓰기
            bw.write("req_bizonNum,mjrBzznno,mjrBizonNm,centerXCrdnt,centerYCrdnt,geom");
            bw.newLine();

            // 2) 메인 루프
            for (int bizonNum = START; bizonNum <= END; bizonNum++) {
                try {
                    String urlStr = "https://bigdata.sbiz.or.kr/gis/api/searchBizonShpeData.json?bizonNum=" + bizonNum;

                    System.out.print("요청 url : " + urlStr);

                    HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10_000);
                    conn.setReadTimeout(10_000);

                    int code = conn.getResponseCode();
                    if (code != 200) {
                        conn.disconnect();
                        System.out.print(" 연결 실패 ");
                        continue;
                    }
                    System.out.print(" 연결 성공 ");

                    try (InputStream is = conn.getInputStream()) {
                        List<BizonRecord> list = mapper.readValue(is, new TypeReference<List<BizonRecord>>() {});
                        if (list == null || list.isEmpty()) {
                            System.out.println("값 없음");
                            continue;
                        }

                        System.out.println("값 있음!!");

                        // 3) 결과를 CSV로 한 줄씩 기록
                        for (BizonRecord rec : list) {
                            String line = toCsvRow(
                                    String.valueOf(bizonNum),
                                    rec.mjrBzznno,
                                    rec.mjrBizonNm,
                                    rec.centerXCrdnt != null ? rec.centerXCrdnt.toString() : "",
                                    rec.centerYCrdnt != null ? rec.centerYCrdnt.toString() : "",
                                    rec.geom
                            );
                            bw.write(line);
                            bw.newLine();
                        }
                    } finally {
                        conn.disconnect();
                    }

                    // (선택) 너무 빠르게 두드리지 않도록 약간 쉼 주고 싶으면 주석 해제
                    // Thread.sleep(50);

                } catch (Exception e) {
                    // 한 bizonNum에서 실패해도 전체는 계속
                    e.printStackTrace();
                }
            }

            // BufferedWriter 는 try-with-resources에서 알아서 flush/close 됨

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("완료: sbiz_bizon_shapes.csv 생성");
    }

    /**
     * CSV 한 줄 만들기
     * - RFC4180 스타일로 각 필드를 처리
     * - 필드 안에 [", \n, \r, ,] 가 있으면 전체를 큰따옴표로 감싸고
     *   내부의 " 는 "" 로 이스케이프
     */
    private static String toCsvRow(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');

            String v = fields[i];
            if (v == null) v = "";

            boolean needQuote =
                    v.contains(",") ||
                            v.contains("\"") ||
                            v.contains("\n") ||
                            v.contains("\r");

            if (needQuote) {
                sb.append('"');
                // 안의 " 를 "" 로 치환
                sb.append(v.replace("\"", "\"\""));
                sb.append('"');
            } else {
                sb.append(v);
            }
        }
        return sb.toString();
    }
}
