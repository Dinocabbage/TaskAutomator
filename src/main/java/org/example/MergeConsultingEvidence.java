package org.example;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class MergeConsultingEvidence {

    // === 경로 설정 ===
    // 원본 루트 (컨설턴트명 폴더들이 바로 하위에 있음)
    private static final Path SRC_ROOT = Paths.get("C:\\Users\\VCL_Cabbage\\Desktop\\export");
    // 결과 루트 (컨설턴트명 폴더를 생성하여 그 안에 4개 파일을 생성)
    private static final Path DST_ROOT = Paths.get("C:\\Users\\VCL_Cabbage\\Desktop\\251219_컨설팅 5차 수당 지급 증빙 자료");

    // 이미지 → PDF 변환 시 여백(포인트). 72pt = 1인치
    private static final float IMAGE_MARGIN_PT = 36f;
    // PNG 투명 배경을 흰색으로 깔 것인지
    private static final boolean WHITE_BG_FOR_PNG = true;
    // EXIF 방향 자동 반영 (스마트폰 사진 90° 문제 방지)
    private static final boolean AUTOROTATE_BY_EXIF = true;

    public static void main(String[] args) throws Exception {
        requireExists(SRC_ROOT);
        Files.createDirectories(DST_ROOT);

        // 1) 컨설턴트명 폴더 나열
        List<Path> consultantDirs = Files.list(SRC_ROOT)
                .filter(Files::isDirectory)
                .sorted(Comparator.comparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (consultantDirs.isEmpty()) {
            System.out.println("컨설턴트 폴더가 없습니다: " + SRC_ROOT);
            return;
        }

        int totalConsultants = 0;
        for (Path consultantDir : consultantDirs) {
            String consultantName = consultantDir.getFileName().toString().trim();
            if (consultantName.isEmpty()) continue;

            System.out.println("\n=== 컨설턴트: " + consultantName + " ===");

            // 2) 하위 '컨설팅ID_점포명' 폴더 모으기 (디렉터리만, 이름순 정렬)
            List<Path> caseDirs = Files.list(consultantDir)
                    .filter(Files::isDirectory)
                    .sorted((a, b) -> compareByLeadingNumberThenName(a.getFileName().toString(), b.getFileName().toString()))
                    .toList();

            // 3) 항목별 파일 수집
            List<Path> filesForConfirm = new ArrayList<>();     // 컨설팅 확인서 (pdf/jpg/png)
            List<Path> filesForReport = new ArrayList<>();      // 결과보고서 (pdf)
            List<Path> filesForAllowance = new ArrayList<>();   // 수당 서명서 (pdf)

            for (Path caseDir : caseDirs) {
                List<Path> files = Files.list(caseDir)
                        .filter(Files::isRegularFile)
                        .collect(Collectors.toList());

                // 파일명 정규화 후 매칭 (공백/언더스코어 제거, 소문자)
                Optional<Path> confirm = findFirstByContains(files, "컨설팅확인서", true); // 이미지 가능
                Optional<Path> report  = findFirstByContains(files, "결과보고서", false);
                Optional<Path> allowance = findFirstByContains(files, "수당서명서", false);

                confirm.ifPresent(filesForConfirm::add);
                report.ifPresent(filesForReport::add);
                allowance.ifPresent(filesForAllowance::add);

                if (confirm.isEmpty()) {
                    // 확실히 이름이 다를 수 있어 보조 패턴도 한 번 더 탐색
                    confirm = findFirstByAny(files, Arrays.asList("확인서", "컨설팅_확인서", "consulting_confirm"));
                    confirm.ifPresent(filesForConfirm::add);
                }
            }

            // 4) 개인정보 동의서(컨설턴트 루트에 1개 있다고 가정) → 이름 바꿔서 복사
            Optional<Path> privacy = Files.list(consultantDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> normalizeName(p.getFileName().toString()).contains("개인정보동의서"))
                    .findFirst();

            Path outDir = DST_ROOT.resolve(consultantName);
            Files.createDirectories(outDir);

            // 5) 항목별 병합
            if (!filesForConfirm.isEmpty()) {
                Path out = outDir.resolve("1. " + consultantName + "_컨설팅 확인서.pdf");
                mergeMixedToPdf(filesForConfirm, out);
            } else {
                System.out.println("  [경고] 컨설팅 확인서 파일을 찾지 못했습니다.");
            }

            if (!filesForReport.isEmpty()) {
                Path out = outDir.resolve("2. " + consultantName + "_결과보고서.pdf");
                mergeMixedToPdf(filesForReport, out); // 혹시 이미지가 있어도 처리 가능
            } else {
                System.out.println("  [경고] 결과보고서 파일을 찾지 못했습니다.");
            }

            if (!filesForAllowance.isEmpty()) {
                Path out = outDir.resolve("3. " + consultantName + "_수당 서명서.pdf");
                mergeMixedToPdf(filesForAllowance, out); // 혹시 이미지가 있어도 처리 가능
            } else {
                System.out.println("  [경고] 수당 서명서 파일을 찾지 못했습니다.");
            }

            if (privacy.isPresent()) {
                Path out = outDir.resolve("4. 개인정보 동의서_" + consultantName + ".pdf");
                if (isPdf(privacy.get())) {
                    // PDF면 그대로 복사(이름 변경)
                    Files.copy(privacy.get(), out, StandardCopyOption.REPLACE_EXISTING);
                } else if (isImage(privacy.get())) {
                    // 이미지면 PDF로 변환해서 저장
                    try (InputStream pdf = imageToSinglePagePdfStream(privacy.get(), IMAGE_MARGIN_PT, WHITE_BG_FOR_PNG, AUTOROTATE_BY_EXIF)) {
                        Files.write(out, pdf.readAllBytes());
                    }
                } else {
                    System.out.println("  [경고] 개인정보 동의서가 PDF/이미지가 아닙니다: " + privacy.get());
                }
            } else {
                System.out.println("  [경고] 개인정보 동의서.pdf 를 찾지 못했습니다.");
            }

            totalConsultants++;
        }

        System.out.println("\n완료. 처리한 컨설턴트 수: " + totalConsultants);
        System.out.println("출력 경로: " + DST_ROOT.toString());
    }

    // --- 병합기: PDF + (필요 시) 이미지 → PDF 변환 후 병합 ---
    private static void mergeMixedToPdf(List<Path> inputs, Path outPdf) throws Exception {
        Files.createDirectories(outPdf.getParent());
        PDFMergerUtility util = new PDFMergerUtility();
        util.setDestinationFileName(outPdf.toString());

        List<InputStream> tempStreams = new ArrayList<>();
        try {
            for (Path p : inputs) {
                if (isPdf(p)) {
                    util.addSource(p.toFile());
                } else if (isImage(p)) {
                    InputStream imgPdf = imageToSinglePagePdfStream(p, IMAGE_MARGIN_PT, WHITE_BG_FOR_PNG, AUTOROTATE_BY_EXIF);
                    tempStreams.add(imgPdf);
                    util.addSource(new RandomAccessReadBuffer(imgPdf));
                } else {
                    System.out.println("  [건너뜀] 알 수 없는 형식: " + p);
                }
            }
            util.mergeDocuments(IOUtils.createTempFileOnlyStreamCache());
            System.out.println("  병합 완료: " + outPdf.getFileName() + " (" + inputs.size() + "개)");
        } finally {
            for (InputStream is : tempStreams) try { is.close(); } catch (IOException ignore) {}
        }
    }

    // --- 이미지 1장을 A4 1페이지 PDF로 변환 ---
    private static InputStream imageToSinglePagePdfStream(Path imgPath, float marginPt, boolean whiteBg, boolean autoRotateByExif) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (PDDocument doc = new PDDocument()) {
            BufferedImage bi = ImageIO.read(imgPath.toFile());
            if (bi == null) throw new IOException("이미지 로드 실패: " + imgPath);

            if (autoRotateByExif) {
                Integer orientation = readExifOrientation(imgPath).orElse(1);
                bi = applyOrientation(bi, orientation);
            }

            // A4 세로/가로 자동 선택
            boolean landscape = bi.getWidth() > bi.getHeight();
            PDRectangle A4 = PDRectangle.A4; // 595 x 842 pt
            PDRectangle pageSize = landscape ? new PDRectangle(A4.getHeight(), A4.getWidth()) : A4;

            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            float pw = page.getMediaBox().getWidth();
            float ph = page.getMediaBox().getHeight();

            float iw = bi.getWidth();
            float ih = bi.getHeight();

            float maxW = pw - 2 * marginPt;
            float maxH = ph - 2 * marginPt;
            float scale = Math.min(maxW / iw, maxH / ih);
            float w = iw * scale;
            float h = ih * scale;
            float x = (pw - w) / 2f;
            float y = (ph - h) / 2f;

            // 배경 깔기 + 이미지 그리기
            PDImageXObject imgX = LosslessFactory.createFromImage(doc, ensureOpaque(bi, whiteBg));
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                if (whiteBg) {
                    cs.setNonStrokingColor(Color.WHITE);
                    cs.addRect(0, 0, pw, ph);
                    cs.fill();
                }
                cs.drawImage(imgX, x, y, w, h);
            }

            doc.save(bos);
        }
        return new ByteArrayInputStream(bos.toByteArray());
    }

    // --- 파일 찾기 유틸 ---
    private static Optional<Path> findFirstByContains(List<Path> files, String keywordKor, boolean allowImage) {
        String kw = keywordKor; // 예: "컨설팅확인서"
        return files.stream()
                .filter(p -> {
                    String n = normalizeName(p.getFileName().toString());
                    boolean hit = n.contains(kw);
                    if (!hit) return false;
                    if (isPdf(p)) return true;
                    return allowImage && isImage(p);
                })
                .findFirst();
    }

    private static Optional<Path> findFirstByAny(List<Path> files, List<String> keywords) {
        return files.stream().filter(p -> {
            String n = normalizeName(p.getFileName().toString());
            for (String k : keywords) if (n.contains(k)) return true;
            return false;
        }).findFirst();
    }

    private static String normalizeName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        // 공백/언더스코어/하이픈/괄호 등 제거, 확장자 제거
        lower = lower.replaceAll("\\s+", "")
                .replace("_", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "")
                .replace(".pdf", "")
                .replace(".jpg", "")
                .replace(".jpeg", "")
                .replace(".png", "");
        // 한글 키워드도 붙어서 매칭되도록
        lower = lower.replace(" ", "");
        // 흔한 변형 흡수
        lower = lower.replace("수당서명서_pdf", "수당서명서");
        return lower;
    }

    private static boolean isPdf(Path p) {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".pdf");
    }

    private static boolean isImage(Path p) {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png");
    }

    private static void requireExists(Path p) throws IOException {
        if (!Files.exists(p)) throw new FileNotFoundException("경로 없음: " + p);
    }

    // 디렉터리 이름이 "1234_점포명"처럼 시작하면 숫자 우선 정렬
    private static int compareByLeadingNumberThenName(String a, String b) {
        OptionalInt na = leadingInt(a);
        OptionalInt nb = leadingInt(b);
        if (na.isPresent() && nb.isPresent()) {
            int diff = Integer.compare(na.getAsInt(), nb.getAsInt());
            if (diff != 0) return diff;
        } else if (na.isPresent()) {
            return -1;
        } else if (nb.isPresent()) {
            return 1;
        }
        return a.compareToIgnoreCase(b);
    }

    private static OptionalInt leadingInt(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) sb.append(c);
            else break;
        }
        if (sb.length() == 0) return OptionalInt.empty();
        try {
            return OptionalInt.of(Integer.parseInt(sb.toString()));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    // --- EXIF & 회전/투명 처리 ---
    private static Optional<Integer> readExifOrientation(Path image) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(image.toFile());
            ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (dir != null && dir.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return Optional.of(dir.getInt(ExifIFD0Directory.TAG_ORIENTATION));
            }
        } catch (Exception ignore) {}
        return Optional.empty();
    }

    private static BufferedImage applyOrientation(BufferedImage img, int orientation) {
        // 1=정상, 3=180, 6=90CW, 8=270CW 등
        double angle;
        switch (orientation) {
            case 3: angle = Math.PI; break;             // 180
            case 6: angle = Math.PI / 2; break;         // 90 CW
            case 8: angle = -Math.PI / 2; break;        // 270 CW
            default: return img;
        }
        return rotateImage(img, angle);
    }

    private static BufferedImage rotateImage(BufferedImage src, double angle) {
        double sin = Math.abs(Math.sin(angle)), cos = Math.abs(Math.cos(angle));
        int w = src.getWidth(), h = src.getHeight();
        int newW = (int) Math.floor(w * cos + h * sin);
        int newH = (int) Math.floor(h * cos + w * sin);

        BufferedImage dst = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dst.createGraphics();
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        AffineTransform at = new AffineTransform();
        at.translate(newW / 2.0, newH / 2.0);
        at.rotate(angle);
        at.translate(-w / 2.0, -h / 2.0);
        g2.drawRenderedImage(src, at);
        g2.dispose();
        return dst;
    }

    private static BufferedImage ensureOpaque(BufferedImage src, boolean whiteBg) {
        if (src.getTransparency() == Transparency.OPAQUE) return src;
        // 투명 → 흰 배경으로 합성
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(whiteBg ? Color.WHITE : new Color(255, 255, 255, 0));
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }
}
