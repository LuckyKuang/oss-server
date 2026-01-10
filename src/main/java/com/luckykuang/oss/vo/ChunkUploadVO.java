package com.luckykuang.oss.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件分片上传VO
 * @author luckykuang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文件分片上传VO")
public class ChunkUploadVO {

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件MD5")
    private String fileMd5;

    @Schema(description = "上传会话ID，用于区分不同的上传任务")
    private String uploadSessionId;

    @Schema(description = "分片序号，从0开始")
    private Integer chunkNumber;

    @Schema(description = "总分片数")
    private Integer totalChunks;

    @Schema(description = "分片大小")
    private Long chunkSize;

    @Schema(description = "文件总大小")
    private Long totalSize;

    @Schema(description = "分片文件")
    private MultipartFile file;
}
