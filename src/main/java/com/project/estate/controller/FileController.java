package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.dto.response.FileUploadResponse;
import com.project.estate.service.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
@Tag(
    name = "File & Media Storage",
    description = "Endpoints for uploading, previewing and deleting media files")
public class FileController {

  private final MinioService minioService;

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Upload single file to Object Storage")
  public ApiResponse<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file)
      throws Exception {
    return ApiResponse.success(minioService.putObject(file));
  }

  @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Upload multiple files (e.g. property image gallery) to Object Storage")
  public ApiResponse<List<FileUploadResponse>> uploadMultipleFiles(
      @RequestParam("files") List<MultipartFile> files) throws Exception {
    return ApiResponse.success(minioService.putObjects(files));
  }

  @GetMapping("/presigned-url")
  @Operation(summary = "Get temporary presigned preview/download URL for an object")
  public ApiResponse<String> getPresignedUrl(@RequestParam("objectName") String objectName)
      throws Exception {
    return ApiResponse.success(minioService.presignedUrl(objectName));
  }

  @DeleteMapping("/{objectName}")
  @Operation(summary = "Delete an object from Object Storage")
  public ApiResponse<Void> deleteFile(@PathVariable String objectName) throws Exception {
    minioService.removeObject(objectName);
    return ApiResponse.success();
  }
}
