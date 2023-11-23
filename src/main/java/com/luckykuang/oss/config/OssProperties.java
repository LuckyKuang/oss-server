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

package com.luckykuang.oss.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * minio 配置加载类
 * @author luckykuang
 * @date 2023/11/14 15:46
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "minio")
public class OssProperties {
    // API 端点
    private String endpoint;

    // 默认 Bucket 存储桶名称
    private String bucketName;

    // Access Key
    private String accessKey;

    // Secret Key
    private String secretKey;
}
