package com.project.estate.service;

import com.project.estate.dto.response.FileUploadResponse;
import io.minio.*;
import io.minio.http.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

  @Value("${minio.url:http://localhost:9000}")
  private String minioUrl;

  @Value("${minio.bucket-name:my-estate-bucket}")
  private String bucketName;

  @Value("${minio.presigned-url-expiration:3600}")
  private int presignedUrlExpiration;

  private final MinioClient minioClient;

  public FileUploadResponse putObject(MultipartFile file) throws Exception {
    createBucketIfNotExists();

    String originalFilename =
        file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
    String extension = "";
    int dotIndex = originalFilename.lastIndexOf('.');
    if (dotIndex >= 0) {
      extension = originalFilename.substring(dotIndex);
    }

    String objectName = UUID.randomUUID() + extension;

    minioClient.putObject(
        PutObjectArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .contentType(
                file.getContentType() != null ? file.getContentType() : "application/octet-stream")
            .stream(file.getInputStream(), file.getSize(), -1)
            .build());

    String publicUrl = getPublicUrl(objectName);

    return FileUploadResponse.builder()
        .originalFileName(originalFilename)
        .objectName(objectName)
        .url(publicUrl)
        .contentType(file.getContentType())
        .size(file.getSize())
        .build();
  }

  public List<FileUploadResponse> putObjects(List<MultipartFile> files) throws Exception {
    List<FileUploadResponse> responses = new ArrayList<>();
    for (MultipartFile file : files) {
      if (!file.isEmpty()) {
        responses.add(putObject(file));
      }
    }
    return responses;
  }

  public void removeObject(String objectName) throws Exception {
    minioClient.removeObject(
        RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
    log.info("Removed object from MinIO bucket {}: {}", bucketName, objectName);
  }

  public String getPublicUrlPrefix() {
    String baseUrl = minioUrl.replaceAll("/+$", "");
    return String.format("%s/%s/", baseUrl, bucketName);
  }

  public String getPublicUrl(String objectName) {
    if (objectName == null || objectName.isBlank()) {
      return objectName;
    }
    String baseUrl = minioUrl.replaceAll("/+$", "");
    return String.format("%s/%s/%s", baseUrl, bucketName, objectName);
  }

  /** Extracts raw object key/name if a full URL was provided. */
  public String extractObjectName(String keyOrUrl) {
    if (keyOrUrl == null || keyOrUrl.isBlank()) {
      return keyOrUrl;
    }
    String prefix = getPublicUrlPrefix();
    if (keyOrUrl.startsWith(prefix)) {
      return keyOrUrl.substring(prefix.length());
    }
    int slashIdx = keyOrUrl.lastIndexOf('/');
    if (keyOrUrl.startsWith("http://") || keyOrUrl.startsWith("https://")) {
      // If it points to our bucket or generic s3
      if (keyOrUrl.contains("/" + bucketName + "/")) {
        return keyOrUrl.substring(
            keyOrUrl.indexOf("/" + bucketName + "/") + bucketName.length() + 2);
      }
      return keyOrUrl;
    }
    return keyOrUrl;
  }

  /** Resolves raw object key to complete public URL for frontend consumption. */
  public String buildFullUrl(String keyOrUrl) {
    if (keyOrUrl == null || keyOrUrl.isBlank()) {
      return keyOrUrl;
    }
    if (keyOrUrl.startsWith("http://") || keyOrUrl.startsWith("https://")) {
      return keyOrUrl;
    }
    return getPublicUrl(keyOrUrl);
  }

  public String presignedUrl(String objectName) throws Exception {
    return minioClient.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .method(Method.GET)
            .bucket(bucketName)
            .object(objectName)
            .expiry(presignedUrlExpiration, TimeUnit.SECONDS)
            .build());
  }

  private void createBucketIfNotExists() throws Exception {
    boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    if (!found) {
      minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
      log.info("Created MinIO bucket: {}", bucketName);
      setBucketPublicReadPolicy();
    }
  }

  private void setBucketPublicReadPolicy() {
    try {
      String policy =
          """
          {
              "Version": "2012-10-17",
              "Statement": [
                  {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                  }
              ]
          }
          """
              .formatted(bucketName);
      minioClient.setBucketPolicy(
          SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build());
      log.info("Configured Public Read Policy for MinIO bucket: {}", bucketName);
    } catch (Exception e) {
      log.warn("Failed to set public policy on MinIO bucket {}: {}", bucketName, e.getMessage());
    }
  }
}
