package com.billingos.certificate;

import com.billingos.config.AppProperties;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final MinioClient minioClient;
    private final AppProperties appProperties;

    public void store(String objectKey, InputStream data, long size, String contentType) {
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket())
                    .object(objectKey)
                    .stream(data, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (MinioException e) {
            throw new StorageException("Failed to upload " + objectKey + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new StorageException("Unexpected storage error: " + e.getMessage(), e);
        }
    }

    public InputStream retrieve(String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket())
                    .object(objectKey)
                    .build());
        } catch (MinioException e) {
            throw new StorageException("Failed to retrieve " + objectKey + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new StorageException("Unexpected storage error: " + e.getMessage(), e);
        }
    }

    public String presignedUrl(String objectKey, int expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket())
                    .object(objectKey)
                    .expiry(expirySeconds)
                    .build());
        } catch (MinioException e) {
            throw new StorageException("Failed to generate presigned URL for " + objectKey + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new StorageException("Unexpected error generating presigned URL: " + e.getMessage(), e);
        }
    }

    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket())
                    .object(objectKey)
                    .build());
        } catch (MinioException e) {
            log.warn("Failed to delete object {}: {}", objectKey, e.getMessage());
        } catch (Exception e) {
            log.warn("Unexpected error deleting object {}: {}", objectKey, e.getMessage());
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket()).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket()).build());
        }
    }

    private String bucket() {
        return appProperties.getMinio().getBucket();
    }
}
