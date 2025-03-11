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
}

tasks.test {
    useJUnitPlatform()
}