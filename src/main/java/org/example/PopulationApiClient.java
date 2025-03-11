package org.example;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class PopulationApiClient {
    private static final String API_URL = "http://apis.data.go.kr/1741000/ppltnDataStus/selectPpltnDataStus";
    private static final String SERVICE_KEY = "VPfUxqZoQBAbJkpf0+u5WFKO01U8a2DFhDtrnLVin0zWTVdm7nhZ7mzJtipAGCqc4M4DWTsq3kWFb+lBHwVotw==";

    /**
     * 3개월 단위로 날짜 범위를 생성해서, 지정된 범위 내 모든 데이터를 조회하고 반환합니다.
     */
    public static List<PopulationData> fetchAllPopulationData() throws IOException {
        OkHttpClient client = new OkHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();
        List<PopulationData> allData = new ArrayList<>();

        // 3개월 단위 조회 날짜 범위 생성 (예: 2021년 10월 ~ 2025년 2월)
        String[][] dateRanges = generateDateRanges("202110", "202502");

        // 날짜 범위별로 반복
        for (String[] range : dateRanges) {
            int pageNo = 1;
            boolean hasMoreData = true;

            while (hasMoreData) {
                // API 요청 URL 구성
                String requestUrl = API_URL +
                        "?serviceKey=" + URLEncoder.encode(SERVICE_KEY, "UTF-8") +
                        "&mvinAdmmCd=5211357000" +  // 예: 전입 행정기관 (덕진구 덕진동)
                        "&mvtAdmmCd=1000000000" +  // 예: 전출 행정기관 (전주시)
                        "&srchFrYm=" + range[0] +
                        "&srchToYm=" + range[1] +
                        "&lv=3" +
                        "&type=JSON" +
                        "&numOfRows=100" +
                        "&pageNo=" + pageNo;

                Request request = new Request.Builder().url(requestUrl).get().build();
                Response response = client.newCall(request).execute();
                String jsonData = response.body().string();

                // 디버그 출력
                System.out.println("🔍 API 원본 응답:\n" + jsonData);
                System.out.println("🔍 [" + range[0] + " ~ " + range[1] + "] Page: " + pageNo + " 응답 확인");

                // JSON 파싱
                JsonNode rootNode = objectMapper.readTree(jsonData);

                // 1) "Response" 노드 확인
                JsonNode responseNode = rootNode.get("Response");
                if (responseNode == null || responseNode.isNull()) {
                    System.out.println("⚠️ 'Response' 노드가 없습니다. 응답 구조가 예상과 다릅니다.");
                    break; // 이 날짜 범위는 중단
                }

                // 2) head 노드
                JsonNode headNode = responseNode.get("head");

                // 3) items -> item 접근
                JsonNode itemsNode = responseNode.path("items").path("item");
                if (itemsNode.isMissingNode() || itemsNode.isNull()) {
                    System.out.println("⚠️ items 필드가 비어 있음!");
                    hasMoreData = false;
                } else {
                    // 배열인지 단일 object인지 판별
                    if (itemsNode.isArray()) {
                        List<PopulationData> pageData = objectMapper.convertValue(itemsNode, new TypeReference<List<PopulationData>>() {});
                        if (!pageData.isEmpty()) {
                            allData.addAll(pageData);
                            System.out.println("✅ 변환된 데이터 개수: " + pageData.size());
                        } else {
                            System.out.println("⚠️ items 배열이 비어 있음!");
                            hasMoreData = false;
                        }
                    } else {
                        // 단일 객체
                        PopulationData singleData = objectMapper.convertValue(itemsNode, PopulationData.class);
                        allData.add(singleData);
                        System.out.println("✅ 변환된 데이터(1건) 추가");
                    }

                    // 다음 페이지로 이동
                    pageNo++;
                }
            }
        }

        System.out.println("✅ 총 조회된 데이터 개수: " + allData.size());
        return allData;
    }

    /**
     * startYm(YYYYMM)부터 endYm(YYYYMM)까지 3개월 단위로 날짜 범위를 생성하는 메서드
     */
    private static String[][] generateDateRanges(String startYm, String endYm) {
        List<String[]> ranges = new ArrayList<>();
        int startYear = Integer.parseInt(startYm.substring(0, 4));
        int startMonth = Integer.parseInt(startYm.substring(4, 6));
        int endYear = Integer.parseInt(endYm.substring(0, 4));
        int endMonth = Integer.parseInt(endYm.substring(4, 6));

        while (startYear < endYear || (startYear == endYear && startMonth <= endMonth)) {
            int nextYear = startYear;
            int nextMonth = startMonth + 2;  // 3개월 단위로 조회

            if (nextMonth > 12) {
                nextYear += 1;
                nextMonth -= 12;
            }

            String rangeStart = String.format("%04d%02d", startYear, startMonth);
            String rangeEnd = String.format("%04d%02d", nextYear, nextMonth);

            if (Integer.parseInt(rangeEnd) > Integer.parseInt(endYm)) {
                rangeEnd = endYm;
            }

            ranges.add(new String[] { rangeStart, rangeEnd });

            startYear = nextYear;
            startMonth = nextMonth + 1;
            if (startMonth > 12) {
                startYear += 1;
                startMonth = 1;
            }
        }

        return ranges.toArray(new String[0][0]);
    }
}
