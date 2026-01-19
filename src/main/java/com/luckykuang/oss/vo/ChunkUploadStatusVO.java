package com.luckykuang.oss.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文件分片上传状态VO
 * @author luckykuang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文件分片上传状态VO")
public class ChunkUploadStatusVO {

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件MD5")
    private String fileMd5;

    @Schema(description = "文件总大小")
    private Long totalSize;

    @Schema(description = "分片大小")
    private Long chunkSize;

    @Schema(description = "总分片数")
    private Integer totalChunks;

    @Schema(description = "已上传的分片序号列表")
    private List<Integer> uploadedChunks;

    @Schema(description = "是否已全部上传完成")
    private Boolean isCompleted;

    @Schema(description = "秒传成功时的文件访问路径")
    private String filePath;

    @Schema(description = "是否为秒传（文件已存在）")
    private Boolean isInstant;
}
