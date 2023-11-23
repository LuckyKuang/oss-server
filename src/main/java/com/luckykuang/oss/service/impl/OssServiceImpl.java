/*
 * Copyright 2015-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.luckykuang.oss.service.impl;

import com.luckykuang.oss.base.ApiResult;
import com.luckykuang.oss.base.BusinessException;
import com.luckykuang.oss.base.ErrorCode;
import com.luckykuang.oss.config.OssProperties;
import com.luckykuang.oss.service.OssService;
import com.luckykuang.oss.util.OssUtils;
import com.luckykuang.oss.vo.BucketVO;
import io.minio.*;
import io.minio.messages.Bucket;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * @author luckykuang
 * @date 2023/11/14 15:45
 */
@Slf4j
@Service
public class OssServiceImpl implements OssService {

    // 日期格式化
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("/yyyy/MM/dd/");

    @Resource
    private MinioClient minioClient;

    @Resource
    private OssProperties ossProperties;

    @Override
    public ApiResult<String> createBucket(String bucketName) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (found) {
                return ApiResult.failed(ErrorCode.BUCKET_EXIST);
            }
            MakeBucketArgs args = MakeBucketArgs.builder().bucket(bucketName).build();
            // 新建存储桶
            minioClient.makeBucket(args);
            // 设置存储桶只读策略
            String bucketPolicy = OssUtils.readOnlyBucketPolicy(bucketName);
            SetBucketPolicyArgs args2 = SetBucketPolicyArgs.builder()
                    .bucket(bucketName).
                    config(bucketPolicy).build();
            // 设置存储桶策略
            minioClient.setBucketPolicy(args2);
        } catch (Exception e){
            log.error("存储桶创建异常",e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }
        return ApiResult.success();
    }

    @Override
    public ApiResult<String> createCustomBucket(BucketVO bucketVO) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketVO.getBucketName()).build());
            if (found) {
                return ApiResult.failed(ErrorCode.BUCKET_EXIST);
            }
            MakeBucketArgs args = MakeBucketArgs.builder().bucket(bucketVO.getBucketName()).build();
            // 新建存储桶
            minioClient.makeBucket(args);
            // 设置存储桶自定义策略
            String bucketPolicy = OssUtils.customBucketPolicy(bucketVO.getBucketName(),bucketVO.getBucketPolicyList());
            SetBucketPolicyArgs args2 = SetBucketPolicyArgs.builder()
                    .bucket(bucketVO.getBucketName()).
                    config(bucketPolicy).build();
            // 设置存储桶策略
            minioClient.setBucketPolicy(args2);
        } catch (Exception e){
            log.error("存储桶创建异常",e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }
        return ApiResult.success();
    }

    @Override
    public ApiResult<String> deleteBucket(String bucketName) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                return ApiResult.failed(ErrorCode.BUCKET_NOT_EXIST);
            }
            RemoveBucketArgs args = RemoveBucketArgs.builder().bucket(bucketName).build();
            minioClient.removeBucket(args);
        } catch (Exception e){
            log.error("存储桶删除异常",e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }
        return ApiResult.success();
    }

    @Override
    public ApiResult<List<String>> listBuckets() {
        try {
            List<Bucket> buckets = minioClient.listBuckets();
            List<String> bucketNames = buckets.stream().map(Bucket::name).toList();
            return ApiResult.success(bucketNames);
        } catch (Exception e){
            log.error("获取存储桶异常",e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }
    }

    @Override
    public ApiResult<String> uploadFile(MultipartFile file, String bucketName) {
        String filePath;
        bucketName = StringUtils.isBlank(bucketName) ? ossProperties.getBucketName() : bucketName;
        // 文件大小
        long size = file.getSize();
        if (size == 0) {
            return ApiResult.failed(ErrorCode.NOT_UPLOAD_EMPTY_FILE);
        }

        // 文件名称
        String fileName = file.getOriginalFilename();
        if (StringUtils.isBlank(fileName)) {
            return ApiResult.failed(ErrorCode.NOT_UPLOAD_EMPTY_NAME);
        }

        int index = fileName.lastIndexOf(".");
        if (index == -1) {
            return ApiResult.failed(ErrorCode.NOT_UPLOAD_EMPTY_EXT);
        }

        // 文件后缀
        String ext = fileName.substring(index);

        // 文件类型
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        // 根据日期打散目录，使用 UUID 重命名文件
        filePath = formatter.format(LocalDate.now()) +
                UUID.randomUUID().toString().replace("-", "") +
                ext;

        log.info("文件名称：{}", fileName);
        log.info("文件大小：{}", size);
        log.info("文件类型：{}", contentType);
        log.info("文件路径：{}", filePath);

        try (InputStream inputStream = file.getInputStream()) {

            PutObjectArgs args = PutObjectArgs.builder()
                    // 指定 Bucket 存储桶名称,默认 public
                    .bucket(bucketName)
                    // 指定 Content Type
                    .contentType(contentType)
                    // 指定文件的路径
                    .object(filePath)
                    // 文件的 InputStream 流
                    .stream(inputStream, size, -1)
                    .build();

            // 上传文件到客户端
            minioClient.putObject(args);
        } catch (Exception e){
            log.error("上传文件异常",e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }

        // 访问路径
        return ApiResult.success(ossProperties.getEndpoint() + bucketName + filePath);
    }

    @Override
    public ApiResult<String> uploadFile(String fileName, InputStream inputStream, String contentType) {
        String filePath;
        // 文件名称
        if (StringUtils.isBlank(fileName)) {
            return ApiResult.failed(ErrorCode.NOT_UPLOAD_EMPTY_NAME);
        }

        int index = fileName.lastIndexOf(".");
        if (index == -1) {
            return ApiResult.failed(ErrorCode.NOT_UPLOAD_EMPTY_EXT);
        }

        // 文件类型
        if (contentType == null) {
            return ApiResult.failed(ErrorCode.NOT_UPLOAD_EMPTY_TYPE);
        }

        // 文件后缀
        String ext = fileName.substring(index);

        // 根据日期打散目录，使用 UUID 重命名文件
        filePath = formatter.format(LocalDate.now()) +
                UUID.randomUUID().toString().replace("-", "") +
                ext;

        log.info("文件名称：{}", fileName);
        log.info("文件类型：{}", contentType);
        log.info("文件路径：{}", filePath);

        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    // 指定 Bucket 存储桶名称,默认 public
                    .bucket(ossProperties.getBucketName())
                    // 指定 Content Type
                    .contentType(contentType)
                    // 指定文件的路径
                    .object(filePath)
                    // 文件的 InputStream 流
                    .stream(inputStream, inputStream.available(), -1)
                    .build();

            // 上传文件到客户端
            minioClient.putObject(args);
        } catch (Exception e){
            log.error("上传文件异常",e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }

        // 访问路径
        return ApiResult.success(ossProperties.getEndpoint() + ossProperties.getBucketName() + filePath);
    }

    @Override
    public void downloadFile(String bucketName, String filePath, HttpServletResponse response) {
        bucketName = StringUtils.isBlank(bucketName) ? ossProperties.getBucketName() : bucketName;
        String objectName = filePath.replace(ossProperties.getEndpoint() + bucketName,"");
        String[] fileSplits = filePath.split("/");
        if (fileSplits.length == 0){
            throw new BusinessException(ErrorCode.FILE_PATH_INCORRECT);
        }
        String filename = fileSplits[fileSplits.length - 1];
        int index = filename.lastIndexOf(".");
        if (index == -1) {
            throw new BusinessException(ErrorCode.FILE_PATH_INCORRECT);
        }
        String fileNameUrl = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + fileNameUrl);
        response.addHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
        response.addHeader("X-Original-File-Name", filename);
        response.setContentType("application/octet-stream");

        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build();

        try(InputStream fileInputStream = minioClient.getObject(args);
            ServletOutputStream fileOutputStream = response.getOutputStream()) {
            IOUtils.copy(fileInputStream, fileOutputStream);
            fileOutputStream.flush();
        } catch (Exception e) {
            log.error("下载文件异常",e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }
    }

    @Override
    public void removeFile(String bucketName, String filePath) {
        bucketName = StringUtils.isBlank(bucketName) ? ossProperties.getBucketName() : bucketName;
        String objectName = filePath.replace(ossProperties.getEndpoint() + bucketName,"");
        RemoveObjectArgs args = RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build();
        try {
            minioClient.removeObject(args);
        } catch (Exception e) {
            log.error("删除文件异常",e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }
    }
}
