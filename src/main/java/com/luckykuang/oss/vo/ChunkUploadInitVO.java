package com.luckykuang.oss.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件分片上传初始化VO
 * @author luckykuang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文件分片上传初始化VO")
public class ChunkUploadInitVO {

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件MD5")
    private String fileMd5;

    @Schema(description = "文件总大小")
    private Long totalSize;

    @Schema(description = "分片大小")
    private Long chunkSize;

    @Schema(description = "上传会话ID，用于区分不同的上传任务")
    private String uploadSessionId;
}
