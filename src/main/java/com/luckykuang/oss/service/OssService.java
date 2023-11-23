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

package com.luckykuang.oss.service;

import com.luckykuang.oss.base.ApiResult;
import com.luckykuang.oss.vo.BucketVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * @author luckykuang
 * @date 2023/11/14 15:45
 */
public interface OssService {

    /**
     * 创建存储桶
     * @param bucketName 存储桶名称
     */
    ApiResult<String> createBucket(String bucketName);

    /**
     * 创建自定义策略存储桶
     * @param bucketVO 入参对象
     */
    ApiResult<String> createCustomBucket(BucketVO bucketVO);

    /**
     * 删除存储桶 (必须是空桶)
     * @param bucketName 存储桶名称
     */
    ApiResult<String> deleteBucket(String bucketName);


    /**
     * 查询存储桶列表
     */
    ApiResult<List<String>> listBuckets();

    /**
     * 文件上传
     * @param file 上传的文件
     * @param bucketName 存储桶名称
     */
    ApiResult<String> uploadFile(MultipartFile file, String bucketName);

    /**
     * 文件上传
     * @param fileName 上传的文件名
     * @param inputStream 上传的文件流
     * @param contentType 上传的文件类型
     */
    ApiResult<String> uploadFile(String fileName, InputStream inputStream, String contentType);

    /**
     * 下载文件
     * @param bucketName 存储桶名称
     * @param filePath 下载的文件路径
     */
    void downloadFile(String bucketName, String filePath, HttpServletResponse response);

    /**
     * 删除文件
     * @param bucketName 存储桶名称
     * @param filePath 删除的文件路径
     */
    void removeFile(String bucketName, String filePath);
}