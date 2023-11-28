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
import com.luckykuang.oss.vo.BucketPolicyVO;
import com.luckykuang.oss.vo.BucketVO;
import com.luckykuang.oss.vo.UploadFileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author luckykuang
 * @date 2023/11/17 18:16
 */
@Validated
@RestController
@RequestMapping("oss")
public class OssController {

    @Resource
    private OssService ossService;

    @Operation(summary = "创建存储桶", description = "创建存储桶", parameters = {
            @Parameter(name = "bucketName",description = "存储桶名称")
    })
    @GetMapping("createBucket")
    public ApiResult<String> createBucket(@NotBlank String bucketName){
        return ossService.createBucket(bucketName);
    }

    @Operation(summary = "创建自定义策略存储桶", description = "创建自定义策略存储桶")
    @PostMapping("createCustomBucket")
    public ApiResult<String> createCustomBucket(@RequestBody @Validated BucketVO bucketVO){
        return ossService.createCustomBucket(bucketVO);
    }

    @Operation(summary = "删除存储桶", description = "删除存储桶 (必须是空桶)", parameters = {
            @Parameter(name = "bucketName",description = "存储桶名称")
    })
    @DeleteMapping("deleteBucket/{bucketName}")
    public ApiResult<String> deleteBucket(@PathVariable String bucketName){
        return ossService.deleteBucket(bucketName);
    }

    @Operation(summary = "查询存储桶列表", description = "查询存储桶列表")
    @GetMapping("listBuckets")
    public ApiResult<List<String>> listBuckets(){
        return ossService.listBuckets();
    }

    @Operation(summary = "文件上传", description = "上传File文件")
    @PostMapping(value = "uploadFile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<String> uploadFile(@Schema(description = "上传的文件",type = "file") @RequestPart MultipartFile file,
                                        @Schema(description = "存储桶名称") @RequestPart String bucketName){
        return ossService.uploadFile(file,bucketName);
    }

    @Operation(summary = "文件上传", description = "上传文件流")
    @PostMapping(value = "uploadFileByStream",consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> uploadFileByStream(@RequestBody @Validated UploadFileVO uploadFileVO){
        return ossService.uploadFileByStream(uploadFileVO);
    }

    @Operation(summary = "下载文件", description = "下载文件", parameters = {
            @Parameter(name = "bucketName",description = "存储桶名称"),
            @Parameter(name = "filePath",description = "下载的文件路径")
    })
    @GetMapping("downloadFile")
    public void downloadFile(@NotBlank String bucketName, @NotBlank String filePath, HttpServletResponse response){
        ossService.downloadFile(bucketName,filePath,response);
    }

    @Operation(summary = "删除文件", description = "删除文件", parameters = {
            @Parameter(name = "bucketName",description = "存储桶名称"),
            @Parameter(name = "filePath",description = "删除的文件路径")
    })
    @DeleteMapping("removeFile/{bucketName}")
    public void removeFile(@PathVariable String bucketName,@RequestParam @NotBlank String filePath){
        ossService.removeFile(bucketName,filePath);
    }

    @Operation(summary = "查询存储桶下所有文件", description = "查询存储桶下所有文件 - 存储桶必须是非私有", parameters = {
            @Parameter(name = "bucketName",description = "存储桶名称"),
            @Parameter(name = "prefix",description = "路径前缀"),
            @Parameter(name = "size",description = "查询条数 - 最大100条")
    })
    @GetMapping("listFilesByBucketName")
    public ApiResult<List<String>> listFilesByBucketName(@NotBlank String bucketName,@NotBlank String prefix,
                                                         @NotNull Integer size){
        return ApiResult.success(ossService.listFilesByBucketName(bucketName,prefix,size));
    }

    @Operation(summary = "设置存储桶策略", description = "设置存储桶策略")
    @PostMapping("setBucketPolicy")
    public ApiResult<String> setBucketPolicy(@RequestBody @Validated BucketPolicyVO bucketPolicyVO){
        return ossService.setBucketPolicy(bucketPolicyVO);
    }

    @Operation(summary = "查询存储桶策略", description = "查询存储桶策略", parameters = {
            @Parameter(name = "bucketName",description = "存储桶名称")
    })
    @GetMapping("getBucketPolicy")
    public ApiResult<String> getBucketPolicy(@NotBlank String bucketName){
        return ApiResult.success(ossService.getBucketPolicy(bucketName));
    }

    @Operation(summary = "生成临时访问url", description = "生成临时访问url - 用于私有存储桶生成临时查看权限", parameters = {
            @Parameter(name = "bucketName",description = "存储桶名称"),
            @Parameter(name = "objectName",description = "文件路径")
    })
    @GetMapping("getPresignedObjectUrl")
    public ApiResult<String> getPresignedObjectUrl(@NotBlank String bucketName,@NotBlank String objectName){
        return ApiResult.success(ossService.getPresignedObjectUrl(bucketName,objectName));
    }

    @Operation(summary = "获取文件分片数量", description = "根据指定步长，得到文件被分片的数量", parameters = {
            @Parameter(name = "bucketName",description = "存储桶名称"),
            @Parameter(name = "objectName",description = "文件路径"),
            @Parameter(name = "length",description = "分片长度")
    })
    @GetMapping("getFileChunkNumber")
    public ApiResult<Long> getFileChunkNumber(@NotBlank String bucketName,@NotBlank String objectName,@NotNull Long length){
        return ApiResult.success(ossService.getFileChunkNumber(bucketName,objectName,length));
    }

    @Operation(summary = "下载文件 - 分片下载", description = "下载文件 - 支持分片下载", parameters = {
            @Parameter(name = "bucketName",description = "存储桶名称"),
            @Parameter(name = "objectName",description = "文件路径"),
            @Parameter(name = "offset",description = "起始字节的位置"),
            @Parameter(name = "length",description = "分片长度 - 如果为空则代表读到文件结尾")
    })
    @GetMapping("downloadFileChunk")
    public void downloadFileChunk(@NotBlank String bucketName, @NotBlank String objectName, @NotNull Long offset,
                                  @RequestParam(required = false) Long length, HttpServletResponse response){
        ossService.downloadFileChunk(bucketName,objectName,offset,length,response);
    }
}
