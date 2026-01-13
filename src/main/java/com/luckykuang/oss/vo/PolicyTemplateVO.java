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

package com.luckykuang.oss.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author luckykuang
 * @date 2026/01/10
 */
@Data
@Schema(description = "策略模板VO类")
public class PolicyTemplateVO {
    @NotBlank
    @Schema(description = "模板名称")
    private String templateName;

    @NotBlank
    @Schema(description = "模板描述")
    private String description;

    @NotBlank
    @Schema(description = "策略类型：public(公有), private(私有), readonly(只读), custom(自定义)")
    private String policyType;

    @Schema(description = "策略内容JSON，自定义类型时必填")
    private String policy;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;
}
