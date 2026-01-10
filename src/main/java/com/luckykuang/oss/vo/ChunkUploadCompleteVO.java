package com.luckykuang.oss.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件分片上传完成VO
 * @author luckykuang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文件分片上传完成VO")
public class ChunkUploadCompleteVO {

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件MD5")
    private String fileMd5;

    @Schema(description = "上传会话ID，用于区分不同的上传任务")
    private String uploadSessionId;

    @Schema(description = "总分片数")
    private Integer totalChunks;

    @Schema(description = "存储桶名称")
    private String bucketName;
}
