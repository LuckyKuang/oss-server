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
import com.luckykuang.oss.processor.OssProcessor;
import com.luckykuang.oss.service.OssService;
import com.luckykuang.oss.vo.*;
import io.minio.*;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author luckykuang
 * @date 2023/11/14 15:45
 */
@Slf4j
@Service
public class OssServiceImpl implements OssService {

    // 日期格式化
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("/yyyy/MM/dd/");

    @Resource(name = "minioClient")
    private MinioClient minioClient;

    @Resource(name = "minioCdnClient")
    private MinioClient minioCdnClient;

    @Resource
    private OssProperties ossProperties;

    @Override
    public ApiResult<String> createBucket(String bucketName) {
        try {
            boolean found = OssProcessor.bucketExists(bucketName);
            if (found) {
                return ApiResult.failed(ErrorCode.BUCKET_EXIST);
            }
            MakeBucketArgs args2 = MakeBucketArgs.builder().bucket(bucketName).build();
            // 新建存储桶
            minioClient.makeBucket(args2);
            // 设置存储桶只读策略
            String bucketPolicy = OssProcessor.readOnlyBucketPolicy(bucketName);
            SetBucketPolicyArgs args3 = SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(bucketPolicy)
                    .build();
            // 设置存储桶策略
            minioClient.setBucketPolicy(args3);
        } catch (Exception e){
            log.error("存储桶创建异常",e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }
        return ApiResult.success();
    }

    @Override
    public ApiResult<String> createCustomBucket(BucketVO bucketVO) {
        try {
            boolean found = OssProcessor.bucketExists(bucketVO.getBucketName());
            if (found) {
                return ApiResult.failed(ErrorCode.BUCKET_EXIST);
            }
            MakeBucketArgs args2 = MakeBucketArgs.builder()
                    .bucket(bucketVO.getBucketName())
                    .build();
            // 新建存储桶
            minioClient.makeBucket(args2);
            // 设置存储桶自定义策略
            String bucketPolicy = OssProcessor.customBucketPolicy(bucketVO.getBucketName(),bucketVO.getBucketPolicyList());
            SetBucketPolicyArgs args3 = SetBucketPolicyArgs.builder()
                    .bucket(bucketVO.getBucketName())
                    .config(bucketPolicy)
                    .build();
            // 设置存储桶策略
            minioClient.setBucketPolicy(args3);
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
            RemoveBucketArgs args = RemoveBucketArgs.builder()
                    .bucket(bucketName)
                    .build();
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
        return ApiResult.success(ossProperties.getEndpointCdn() + bucketName + filePath);
    }

    @Override
    public ApiResult<String> uploadFileByStream(UploadFileVO uploadFileVO) {
        String fileName = uploadFileVO.getFileName();
        InputStream inputStream = uploadFileVO.getInputStream();
        String contentType = uploadFileVO.getContentType();
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
        return ApiResult.success(ossProperties.getEndpointCdn() + ossProperties.getBucketName() + filePath);
    }

    @Override
    public void downloadFile(String bucketName, String filePath, HttpServletResponse response) {
        bucketName = StringUtils.isBlank(bucketName) ? ossProperties.getBucketName() : bucketName;
        String objectName = filePath.replace(ossProperties.getEndpointCdn() + bucketName,"");
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
        String objectName = filePath.replace(ossProperties.getEndpointCdn() + bucketName,"");
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

    @Override
    public List<String> listFilesByBucketName(String bucketName,String prefix,Integer size) {
        // 处理前缀：如果是空字符串，设置为null
        String effectivePrefix = StringUtils.isBlank(prefix) ? null : prefix;
        if (effectivePrefix != null && !effectivePrefix.endsWith("/")) {
            effectivePrefix = effectivePrefix + "/";
        }

        log.info("查询文件列表 - 存储桶: {}, 前缀: {}", bucketName, effectivePrefix);

        ListObjectsArgs args = ListObjectsArgs.builder()
                .bucket(bucketName)
                // 前缀
                .prefix(effectivePrefix)
                // 不递归，只返回直接子项
                .recursive(false)
                // 避免性能问题，暂时最多查询100条数据
                .maxKeys(size > 100 ? 100 : size)
                .build();
        Iterable<Result<Item>> results = minioClient.listObjects(args);
        List<String> files = new ArrayList<>();
        try {
            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();

                // 跳过前缀本身
                if (effectivePrefix != null && effectivePrefix.equals(objectName)) {
                    log.info("跳过前缀本身: {}", objectName);
                    continue;
                }

                // 判断是否为文件夹（以/结尾）
                boolean isFolder = objectName.endsWith("/");

                // 移除前缀，获取相对路径用于过滤
                String relativePath = effectivePrefix != null ?
                        objectName.substring(effectivePrefix.length()) : objectName;

                // 过滤出直接子项：相对路径中不应该再包含斜杠（文件夹除外）
                if (!isFolder && relativePath.contains("/")) {
                    log.info("跳过深层文件: {}", objectName);
                    continue;
                }

                // 返回完整路径（带前缀），方便前端处理
                files.add(objectName);

                log.debug("添加文件: {} (文件夹: {})", objectName, isFolder);
            }
        } catch (Exception e){
            log.error("查询文件列表异常",e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }

        log.info("查询完成，返回 {} 个文件", files.size());
        return files;
    }

    @Override
    public ApiResult<String> setBucketPolicy(BucketPolicyVO bucketPolicyVO) {
        String bucketName = bucketPolicyVO.getBucketName();
        String policy = bucketPolicyVO.getPolicy();
        SetBucketPolicyArgs args = SetBucketPolicyArgs.builder()
                .bucket(bucketName)
                // 策略配置
                .config(policy)
                .build();
        try {
            // 设置存储桶策略
            minioClient.setBucketPolicy(args);
        } catch (Exception e){
            log.error("设置存储桶策略异常",e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }
        return ApiResult.success();
    }

    @Override
    public String getBucketPolicy(String bucketName) {
        GetBucketPolicyArgs args = GetBucketPolicyArgs.builder()
                .bucket(bucketName)
                .build();
        try {
            // 查询存储桶策略
            return minioClient.getBucketPolicy(args);
        } catch (Exception e){
            log.error("查询存储桶策略异常",e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }
    }

    @Override
    public String getPresignedObjectUrl(String bucketName, String objectName, Integer expirySeconds) {
        // 如果未指定过期时间，默认为1小时（3600秒）
        int expiry = (expirySeconds != null && expirySeconds > 0) ? expirySeconds : 3600;

        GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .method(io.minio.http.Method.GET)
                .expiry(expiry, TimeUnit.SECONDS)
                .build();
        try {
            // 使用 CDN 客户端生成 presigned URL，这样访问时会通过 CDN
            String presignedUrl = minioCdnClient.getPresignedObjectUrl(args);
            log.info("生成的 presigned URL: {}", presignedUrl);
            return presignedUrl;
        } catch (Exception e){
            log.error("生成临时访问url异常",e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }
    }

    @Override
    public Long getFileChunkNumber(String bucketName, String objectName, Long length) {
        StatObjectResponse statObject = OssProcessor.getStatObject(bucketName, objectName);
        // 文件的长度
        long size = statObject.size();
        return size / length;
    }

    @Override
    public void downloadFileChunk(String bucketName,String objectName,Long offset,Long length,HttpServletResponse response) {
        StatObjectResponse statObject = OssProcessor.getStatObject(bucketName, objectName);
        // 文件的长度
        long size = statObject.size();
        if (offset > size) {
            throw new BusinessException(ErrorCode.UNKNOWN);
        }
        if (length != null && (offset + length) > size){
            throw new BusinessException(ErrorCode.UNKNOWN);
        }
        String filename;
        String oldName = OssProcessor.getFileNameByObjectName(objectName);
        if (length == null){
            filename = oldName + "_1";
        } else {
            long num = (offset % length);
            filename = oldName + "_" + num;
        }

        String fileNameUrl = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + fileNameUrl);
        response.addHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
        response.addHeader("X-Original-File-Name", filename);
        response.setContentType("application/octet-stream");

        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .offset(offset)
                .length(length)
                .build();
        try(InputStream fileInputStream = minioClient.getObject(args);
            ServletOutputStream fileOutputStream = response.getOutputStream()) {
            IOUtils.copy(fileInputStream, fileOutputStream);
            fileOutputStream.flush();
        } catch (Exception e) {
            log.error("下载文件块异常",e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }
    }

    /**
     * 分片上传临时路径前缀
     */
    private static final String CHUNK_UPLOAD_PREFIX = ".chunk-uploads/";

    @Override
    public ApiResult<ChunkUploadStatusVO> initChunkUpload(ChunkUploadInitVO chunkUploadInitVO) {
        String fileName = chunkUploadInitVO.getFileName();
        String fileMd5 = chunkUploadInitVO.getFileMd5();
        String uploadSessionId = chunkUploadInitVO.getUploadSessionId();
        Long totalSize = chunkUploadInitVO.getTotalSize();
        Long chunkSize = chunkUploadInitVO.getChunkSize();

        if (StringUtils.isBlank(fileName)) {
            return ApiResult.failed(ErrorCode.NOT_UPLOAD_EMPTY_NAME);
        }

        if (StringUtils.isBlank(uploadSessionId)) {
            return ApiResult.failed(ErrorCode.INVALID_PARAMETER.getCode(), "上传会话ID不能为空");
        }

        // 验证分片大小 >= 5MB
        final long MIN_CHUNK_SIZE = 5 * 1024 * 1024; // 5MB
        if (chunkSize == null || chunkSize < MIN_CHUNK_SIZE) {
            log.warn("分片大小 {} 小于最小值 5MB", chunkSize);
            return ApiResult.failed(ErrorCode.INVALID_PARAMETER.getCode(), "分片大小不能低于 5MB");
        }

        int index = fileName.lastIndexOf(".");
        if (index == -1) {
            return ApiResult.failed(ErrorCode.NOT_UPLOAD_EMPTY_EXT);
        }

        // 计算分片数量
        int totalChunks = (int) Math.ceil((double) totalSize / chunkSize);

        // 分片上传到默认存储桶的临时路径，使用 uploadSessionId 区分不同的上传会话
        // 路径格式: .chunk-uploads/{uploadSessionId}/{fileMd5}/
        String chunkUploadDir = CHUNK_UPLOAD_PREFIX + uploadSessionId + "/" + fileMd5 + "/";
        String bucketName = ossProperties.getBucketName();

        // 查询已上传的分片
        List<Integer> uploadedChunks = new ArrayList<>();
        try {
            ListObjectsArgs args = ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(chunkUploadDir)
                    .recursive(true)
                    .build();
            Iterable<Result<Item>> results = minioClient.listObjects(args);
            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();
                if (objectName.endsWith(".chunk")) {
                    String chunkFileName = objectName.substring(objectName.lastIndexOf("/") + 1);
                    chunkFileName = chunkFileName.replace(".chunk", "");
                    try {
                        int chunkIndex = Integer.parseInt(chunkFileName);
                        uploadedChunks.add(chunkIndex);
                    } catch (NumberFormatException e) {
                        log.warn("解析分片序号失败: {}", chunkFileName);
                    }
                }
            }
        } catch (Exception e) {
            log.error("查询分片上传记录异常", e);
        }

        // 返回上传状态
        ChunkUploadStatusVO statusVO = new ChunkUploadStatusVO();
        statusVO.setFileName(fileName);
        statusVO.setFileMd5(fileMd5);
        statusVO.setTotalSize(totalSize);
        statusVO.setChunkSize(chunkSize);
        statusVO.setTotalChunks(totalChunks);
        statusVO.setUploadedChunks(uploadedChunks);
        statusVO.setIsCompleted(uploadedChunks.size() >= totalChunks);

        return ApiResult.success(statusVO);
    }

    @Override
    public ApiResult<String> uploadChunk(ChunkUploadVO chunkUploadVO) {
        String fileName = chunkUploadVO.getFileName();
        String fileMd5 = chunkUploadVO.getFileMd5();
        String uploadSessionId = chunkUploadVO.getUploadSessionId();
        Integer chunkNumber = chunkUploadVO.getChunkNumber();
        Integer totalChunks = chunkUploadVO.getTotalChunks();
        MultipartFile file = chunkUploadVO.getFile();

        if (file == null || file.isEmpty()) {
            return ApiResult.failed(ErrorCode.NOT_UPLOAD_EMPTY_FILE);
        }

        if (StringUtils.isBlank(uploadSessionId)) {
            return ApiResult.failed(ErrorCode.INVALID_PARAMETER, "上传会话ID不能为空");
        }

        if (chunkNumber < 0 || chunkNumber >= totalChunks) {
            return ApiResult.failed(ErrorCode.UNKNOWN);
        }

        // 分片文件上传到默认存储桶的临时路径: .chunk-uploads/{uploadSessionId}/{fileMd5}/{chunkNumber}.chunk
        String chunkPath = CHUNK_UPLOAD_PREFIX + uploadSessionId + "/" + fileMd5 + "/" + chunkNumber + ".chunk";
        String bucketName = ossProperties.getBucketName();

        log.info("上传分片 - 文件: {}, MD5: {}, 会话ID: {}, 分片: {}/{}, 大小: {}",
                fileName, fileMd5, uploadSessionId, chunkNumber + 1, totalChunks, file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(chunkPath)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType("application/octet-stream")
                    .build();

            minioClient.putObject(args);
        } catch (Exception e) {
            log.error("上传分片异常", e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }

        return ApiResult.success("分片上传成功");
    }

    @Override
    public ApiResult<String> completeChunkUpload(ChunkUploadCompleteVO chunkUploadCompleteVO) {
        String fileName = chunkUploadCompleteVO.getFileName();
        String fileMd5 = chunkUploadCompleteVO.getFileMd5();
        String uploadSessionId = chunkUploadCompleteVO.getUploadSessionId();
        Integer totalChunks = chunkUploadCompleteVO.getTotalChunks();

        if (StringUtils.isBlank(uploadSessionId)) {
            return ApiResult.failed(ErrorCode.INVALID_PARAMETER, "上传会话ID不能为空");
        }

        // 文件扩展名
        int index = fileName.lastIndexOf(".");
        if (index == -1) {
            return ApiResult.failed(ErrorCode.NOT_UPLOAD_EMPTY_EXT);
        }
        String ext = fileName.substring(index);

        // 最终文件路径
        String finalFilePath = formatter.format(LocalDate.now()) +
                UUID.randomUUID().toString().replace("-", "") +
                ext;

        // 分片文件目录（在目标存储桶的临时路径中）
        String chunkUploadDir = CHUNK_UPLOAD_PREFIX + uploadSessionId + "/" + fileMd5 + "/";
        String bucketName = ossProperties.getBucketName();

        log.info("开始合并分片 - 文件: {}, MD5: {}, 会话ID: {}, 总分片: {}, 目标存储桶: {}",
                fileName, fileMd5, uploadSessionId, totalChunks, bucketName);

        try {
            // 验证分片大小 >= 5MB (MinIO composeObject API 硬性要求)
            String firstChunkPath = chunkUploadDir + "0.chunk";
            StatObjectArgs statArgs = StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(firstChunkPath)
                    .build();
            long firstChunkSize = minioClient.statObject(statArgs).size();

            final long MIN_CHUNK_SIZE = 5 * 1024 * 1024; // 5MB
            if (firstChunkSize < MIN_CHUNK_SIZE) {
                log.error("分片大小 {} 小于最小值 5MB，无法使用 composeObject", firstChunkSize);
                return ApiResult.failed(ErrorCode.INVALID_PARAMETER, "分片大小不能低于 5MB");
            }

            // 使用 MinIO 原生的 composeObject API 在服务端合并分片
            log.info("使用 composeObject 服务端合并分片");
            List<io.minio.ComposeSource> sources = new ArrayList<>();
            for (int i = 0; i < totalChunks; i++) {
                String chunkPath = chunkUploadDir + i + ".chunk";
                sources.add(io.minio.ComposeSource.builder()
                        .bucket(bucketName)
                        .object(chunkPath)
                        .build());
                log.info("添加分片 {} 到合并列表: {}", i + 1, chunkPath);
            }

            ComposeObjectArgs composeArgs = ComposeObjectArgs.builder()
                    .bucket(bucketName)
                    .object(finalFilePath)
                    .sources(sources)
                    .build();

            minioClient.composeObject(composeArgs);

            log.info("使用 composeObject 服务端合并分片成功: {}", finalFilePath);

            // 删除本次上传会话的所有临时分片文件（包含整个会话目录）
            ListObjectsArgs listArgs = ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(CHUNK_UPLOAD_PREFIX + uploadSessionId + "/")
                    .recursive(true)
                    .build();
            Iterable<Result<Item>> results = minioClient.listObjects(listArgs);

            int deletedCount = 0;
            for (Result<Item> result : results) {
                try {
                    Item item = result.get();
                    String objectName = item.objectName();
                    RemoveObjectArgs removeArgs = RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build();
                    minioClient.removeObject(removeArgs);
                    deletedCount++;
                    log.info("已删除临时文件: {}", objectName);
                } catch (Exception e) {
                    log.error("删除临时文件失败", e);
                }
            }

            log.info("已删除 {} 个临时分片文件，会话ID: {}", deletedCount, uploadSessionId);

            log.info("分片合并成功 - 文件路径: {}", finalFilePath);

            // 返回文件访问URL
            return ApiResult.success(ossProperties.getEndpointCdn() + bucketName + finalFilePath);

        } catch (Exception e) {
            log.error("合并分片异常", e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }
    }

    @Override
    public ApiResult<ChunkUploadStatusVO> getChunkUploadStatus(String fileMd5, String bucketName) {
        // 使用默认存储桶
        String useBucketName = ossProperties.getBucketName();

        // 查询临时分片路径下的所有分片
        String chunkUploadDir = CHUNK_UPLOAD_PREFIX + fileMd5 + "/";

        // 查询已上传的分片
        List<Integer> uploadedChunks = new ArrayList<>();
        int totalChunks = 0;
        Long totalSize = 0L;
        Long chunkSize = 0L;
        String fileName = "";
        boolean isCompleted = false;

        try {
            ListObjectsArgs args = ListObjectsArgs.builder()
                    .bucket(useBucketName)
                    .prefix(chunkUploadDir)
                    .recursive(true)
                    .build();
            Iterable<Result<Item>> results = minioClient.listObjects(args);

            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();

                if (objectName.endsWith(".chunk")) {
                    String chunkFileName = objectName.substring(objectName.lastIndexOf("/") + 1);
                    chunkFileName = chunkFileName.replace(".chunk", "");
                    try {
                        int chunkIndex = Integer.parseInt(chunkFileName);
                        // 避免重复添加同一分片（不同会话可能有相同分片）
                        if (!uploadedChunks.contains(chunkIndex)) {
                            uploadedChunks.add(chunkIndex);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("解析分片序号失败: {}", chunkFileName);
                    }
                }
            }

            if (totalChunks == 0 && !uploadedChunks.isEmpty()) {
                totalChunks = Collections.max(uploadedChunks) + 1;
            }

            isCompleted = uploadedChunks.size() >= totalChunks;

        } catch (Exception e) {
            log.error("查询分片上传状态异常", e);
        }

        ChunkUploadStatusVO statusVO = new ChunkUploadStatusVO();
        statusVO.setFileName(fileName);
        statusVO.setFileMd5(fileMd5);
        statusVO.setTotalSize(totalSize);
        statusVO.setChunkSize(chunkSize);
        statusVO.setTotalChunks(totalChunks);
        statusVO.setUploadedChunks(uploadedChunks.stream().sorted().collect(Collectors.toList()));
        statusVO.setIsCompleted(isCompleted);

        return ApiResult.success(statusVO);
    }

    @Override
    public ApiResult<String> cancelChunkUpload(String fileMd5, String bucketName, String uploadSessionId) {
        // 使用默认存储桶
        String useBucketName = ossProperties.getBucketName();

        log.info("取消分片上传 - MD5: {}, 会话ID: {}, 存储桶: {}", fileMd5, uploadSessionId, useBucketName);

        try {
            // 如果提供了 uploadSessionId，只删除该会话的文件
            String prefix;
            if (StringUtils.isNotBlank(uploadSessionId)) {
                prefix = CHUNK_UPLOAD_PREFIX + uploadSessionId + "/";
            } else {
                // 否则删除所有匹配MD5的分片文件（包含所有会话）
                prefix = CHUNK_UPLOAD_PREFIX + fileMd5 + "/";
            }

            ListObjectsArgs listArgs = ListObjectsArgs.builder()
                    .bucket(useBucketName)
                    .prefix(prefix)
                    .recursive(true)
                    .build();
            Iterable<Result<Item>> results = minioClient.listObjects(listArgs);

            int deletedCount = 0;
            for (Result<Item> result : results) {
                try {
                    Item item = result.get();
                    String objectName = item.objectName();
                    RemoveObjectArgs removeArgs = RemoveObjectArgs.builder()
                            .bucket(useBucketName)
                            .object(objectName)
                            .build();
                    minioClient.removeObject(removeArgs);
                    deletedCount++;
                    log.info("已删除临时文件: {}", objectName);
                } catch (Exception e) {
                    log.error("删除临时文件失败", e);
                }
            }

            log.info("已删除 {} 个分片文件", deletedCount);

            return ApiResult.success("分片上传已取消");

        } catch (Exception e) {
            log.error("取消分片上传异常", e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }
    }

    // ==================== 策略模板管理 ====================

    // 使用内存存储策略模板（生产环境应使用数据库）
    private static final java.util.Map<String, PolicyTemplateVO> policyTemplateMap = new java.util.concurrent.ConcurrentHashMap<>();

    // 预定义的策略
    private static final String PUBLIC_POLICY = "{\n" +
            "  \"Version\": \"2012-10-17\",\n" +
            "  \"Statement\": [\n" +
            "    {\n" +
            "      \"Effect\": \"Allow\",\n" +
            "      \"Principal\": {\"AWS\": [\"*\"]},\n" +
            "      \"Action\": [\"s3:GetBucketLocation\", \"s3:ListBucket\", \"s3:ListBucketMultipartUploads\"],\n" +
            "      \"Resource\": [\"arn:aws:s3:::{bucket}\"]\n" +
            "    },\n" +
            "    {\n" +
            "      \"Effect\": \"Allow\",\n" +
            "      \"Principal\": {\"AWS\": [\"*\"]},\n" +
            "      \"Action\": [\"s3:GetObject\", \"s3:ListMultipartUploadParts\", \"s3:PutObject\", \"s3:AbortMultipartUpload\", \"s3:DeleteObject\"],\n" +
            "      \"Resource\": [\"arn:aws:s3:::{bucket}/*\"]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private static final String READONLY_POLICY = "{\n" +
            "  \"Version\": \"2012-10-17\",\n" +
            "  \"Statement\": [\n" +
            "    {\n" +
            "      \"Effect\": \"Allow\",\n" +
            "      \"Principal\": {\"AWS\": [\"*\"]},\n" +
            "      \"Action\": [\"s3:GetObject\"],\n" +
            "      \"Resource\": [\"arn:aws:s3:::{bucket}/*\"]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private static final String PRIVATE_POLICY = "{\n" +
            "  \"Version\": \"2012-10-17\",\n" +
            "  \"Statement\": []\n" +
            "}";

    static {
        // 初始化默认策略模板
        long now = System.currentTimeMillis();
        PolicyTemplateVO publicTemplate = new PolicyTemplateVO();
        publicTemplate.setTemplateName("public");
        publicTemplate.setDescription("公有访问策略 - 允许匿名读写删除");
        publicTemplate.setPolicyType("public");
        publicTemplate.setPolicy(PUBLIC_POLICY);
        publicTemplate.setCreateTime(now);
        publicTemplate.setUpdateTime(now);
        policyTemplateMap.put("public", publicTemplate);

        PolicyTemplateVO readonlyTemplate = new PolicyTemplateVO();
        readonlyTemplate.setTemplateName("readonly");
        readonlyTemplate.setDescription("只读访问策略 - 允许匿名读取");
        readonlyTemplate.setPolicyType("readonly");
        readonlyTemplate.setPolicy(READONLY_POLICY);
        readonlyTemplate.setCreateTime(now);
        readonlyTemplate.setUpdateTime(now);
        policyTemplateMap.put("readonly", readonlyTemplate);

        PolicyTemplateVO privateTemplate = new PolicyTemplateVO();
        privateTemplate.setTemplateName("private");
        privateTemplate.setDescription("私有访问策略 - 禁止匿名访问");
        privateTemplate.setPolicyType("private");
        privateTemplate.setPolicy(PRIVATE_POLICY);
        privateTemplate.setCreateTime(now);
        privateTemplate.setUpdateTime(now);
        policyTemplateMap.put("private", privateTemplate);
    }

    @Override
    public ApiResult<String> createPolicyTemplate(PolicyTemplateVO policyTemplateVO) {
        if (policyTemplateMap.containsKey(policyTemplateVO.getTemplateName())) {
            return ApiResult.failed("001", "策略模板名称已存在");
        }

        policyTemplateVO.setCreateTime(System.currentTimeMillis());
        policyTemplateVO.setUpdateTime(System.currentTimeMillis());

        // 根据类型生成默认策略
        String policyType = policyTemplateVO.getPolicyType();
        if ("public".equals(policyType)) {
            policyTemplateVO.setPolicy(PUBLIC_POLICY);
        } else if ("readonly".equals(policyType)) {
            policyTemplateVO.setPolicy(READONLY_POLICY);
        } else if ("private".equals(policyType)) {
            policyTemplateVO.setPolicy(PRIVATE_POLICY);
        } else if ("custom".equals(policyType)) {
            if (StringUtils.isBlank(policyTemplateVO.getPolicy())) {
                return ApiResult.failed("002", "自定义策略类型必须提供策略内容");
            }
        } else {
            return ApiResult.failed("003", "无效的策略类型");
        }

        policyTemplateMap.put(policyTemplateVO.getTemplateName(), policyTemplateVO);
        log.info("创建策略模板成功: {}", policyTemplateVO.getTemplateName());
        return ApiResult.success();
    }

    @Override
    public ApiResult<List<PolicyTemplateVO>> listPolicyTemplates() {
        return ApiResult.success(new ArrayList<>(policyTemplateMap.values()));
    }

    @Override
    public ApiResult<PolicyTemplateVO> getPolicyTemplate(String templateName) {
        PolicyTemplateVO template = policyTemplateMap.get(templateName);
        if (template == null) {
            return ApiResult.failed("004", "策略模板不存在");
        }
        return ApiResult.success(template);
    }

    @Override
    public ApiResult<String> updatePolicyTemplate(PolicyTemplateVO policyTemplateVO) {
        PolicyTemplateVO existing = policyTemplateMap.get(policyTemplateVO.getTemplateName());
        if (existing == null) {
            return ApiResult.failed("005", "策略模板不存在");
        }

        // 如果更新的是内置模板，不允许修改模板名称和类型
        if (isBuiltInTemplate(policyTemplateVO.getTemplateName())) {
            if (!existing.getPolicyType().equals(policyTemplateVO.getPolicyType())) {
                return ApiResult.failed("006", "不允许修改内置模板的策略类型");
            }
        }

        policyTemplateVO.setCreateTime(existing.getCreateTime());
        policyTemplateVO.setUpdateTime(System.currentTimeMillis());
        policyTemplateMap.put(policyTemplateVO.getTemplateName(), policyTemplateVO);

        log.info("更新策略模板成功: {}", policyTemplateVO.getTemplateName());
        return ApiResult.success();
    }

    @Override
    public ApiResult<String> deletePolicyTemplate(String templateName) {
        if (!policyTemplateMap.containsKey(templateName)) {
            return ApiResult.failed("007", "策略模板不存在");
        }

        if (isBuiltInTemplate(templateName)) {
            return ApiResult.failed("008", "不允许删除内置策略模板");
        }

        policyTemplateMap.remove(templateName);
        log.info("删除策略模板成功: {}", templateName);
        return ApiResult.success();
    }

    @Override
    public ApiResult<String> applyPolicyTemplate(String bucketName, String templateName) {
        PolicyTemplateVO template = policyTemplateMap.get(templateName);
        if (template == null) {
            return ApiResult.failed("009", "策略模板不存在");
        }

        try {
            String policy = template.getPolicy().replace("{bucket}", bucketName);
            SetBucketPolicyArgs args = SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(policy)
                    .build();
            minioClient.setBucketPolicy(args);
            log.info("应用策略模板成功 - 存储桶: {}, 模板: {}", bucketName, templateName);
            return ApiResult.success();
        } catch (Exception e) {
            log.error("应用策略模板失败", e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }
    }

    private boolean isBuiltInTemplate(String templateName) {
        return "public".equals(templateName) ||
               "readonly".equals(templateName) ||
               "private".equals(templateName);
    }
}
