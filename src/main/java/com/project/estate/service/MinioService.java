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

    String url = presignedUrl(objectName);

    return FileUploadResponse.builder()
        .originalFileName(originalFilename)
        .objectName(objectName)
        .url(url)
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
    }
  }
}
