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

package com.luckykuang.oss.controller;

import com.luckykuang.oss.base.ApiResult;
import com.luckykuang.oss.service.OssService;
import com.luckykuang.oss.vo.BucketVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * @author luckykuang
 * @date 2023/11/17 18:16
 */
@RestController
@RequestMapping("oss")
public class OssController {

    @Resource
    private OssService ossService;

    /**
     * 创建存储桶
     * @param bucketName 存储桶名称
     */
    @GetMapping("createBucket")
    public ApiResult<String> createBucket(String bucketName){
        return ossService.createBucket(bucketName);
    }

    /**
     * 创建自定义策略存储桶
     * @param bucketVO 入参对象
     */
    @PostMapping("createCustomBucket")
    public ApiResult<String> createCustomBucket(@RequestBody BucketVO bucketVO){
        return ossService.createCustomBucket(bucketVO);
    }

    /**
     * 删除存储桶 (必须是空桶)
     * @param bucketName 存储桶名称
     */
    @GetMapping("deleteBucket")
    public ApiResult<String> deleteBucket(String bucketName){
        return ossService.deleteBucket(bucketName);
    }


    /**
     * 查询 bucket 列表
     */
    @GetMapping("listBuckets")
    public ApiResult<List<String>> listBuckets(){
        return ossService.listBuckets();
    }

    /**
     * 文件上传
     * @param file 上传的文件
     * @param bucketName 存储桶名称
     */
    @PostMapping("uploadFile")
    public ApiResult<String> uploadFile(@RequestPart MultipartFile file, String bucketName){
        return ossService.uploadFile(file,bucketName);
    }

    /**
     * 文件上传
     * @param fileName 上传的文件名
     * @param inputStream 上传的文件流
     * @param contentType 上传的文件类型
     */
    @PostMapping("uploadFile2")
    public ApiResult<String> uploadFile(String fileName, InputStream inputStream, String contentType){
        return ossService.uploadFile(fileName,inputStream,contentType);
    }

    /**
     * 下载文件
     * @param bucketName 存储桶名称
     * @param filePath 下载的文件路径
     */
    @GetMapping("downloadFile")
    public void downloadFile(String bucketName, String filePath, HttpServletResponse response){
        ossService.downloadFile(bucketName,filePath,response);
    }

    /**
     * 删除文件
     * @param bucketName 存储桶名称
     * @param filePath 删除的文件路径
     */
    @DeleteMapping("removeFile/{bucketName}")
    public void removeFile(@PathVariable String bucketName,String filePath){
        ossService.removeFile(bucketName,filePath);
    }
}
