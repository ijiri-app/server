description = "사진 업로드 URL 발급, 연결 확인, 저장소(R2 또는 DB)"

dependencies {
    // Cloudflare R2 (S3 호환) 사진 저장소: presigned URL, HEAD, 복사, 삭제
    implementation(platform("software.amazon.awssdk:bom:2.57.9"))
    implementation("software.amazon.awssdk:s3") {
        exclude(group = "software.amazon.awssdk", module = "netty-nio-client")
    }
}
