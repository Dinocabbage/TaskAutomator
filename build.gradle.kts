plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.mariadb.jdbc:mariadb-java-client:3.2.0")
    implementation("com.googlecode.juniversalchardet:juniversalchardet:1.0.3")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.apache.poi:poi-ooxml:5.2.3") // Excel 저장용
    implementation("com.squareup.okhttp3:okhttp:4.11.0") // HTTP 요청
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2") // JSON 처리
    implementation("org.projectlombok:lombok:1.18.28")
    annotationProcessor("org.projectlombok:lombok:1.18.28")
    // PDF 처리 (병합/페이지 추가/이미지 그리기 등)
    implementation("org.apache.pdfbox:pdfbox:3.0.5")

    // [옵션] EXIF 방향(가로/세로) 읽어서 이미지 자동 회전하고 싶을 때
    implementation("com.drewnoakes:metadata-extractor:2.19.0")
}

tasks.test {
    useJUnitPlatform()
}