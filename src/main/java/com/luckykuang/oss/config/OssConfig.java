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

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * minio 客户端配置
 * @author luckykuang
 * @date 2023/11/16 9:55
 */
@Component
public class OssConfig {

    /**
     * 实例化客户端（使用 MinIO 原始 endpoint）
     * @param ossProperties 配置加载类
     * @return minio 客户端
     */
    @Bean(name = "minioClient")
    public MinioClient minioClient(OssProperties ossProperties){
        return MinioClient.builder()
                .endpoint(ossProperties.getEndpoint())
                .credentials(ossProperties.getAccessKey(), ossProperties.getSecretKey())
                .build();
    }

    /**
     * 实例化客户端（使用 CDN endpoint）- 用于生成 presigned URL
     * @param ossProperties 配置加载类
     * @return minio 客户端
     */
    @Bean(name = "minioCdnClient")
    public MinioClient minioCdnClient(OssProperties ossProperties){
        return MinioClient.builder()
                .endpoint(ossProperties.getEndpointCdn())
                .credentials(ossProperties.getAccessKey(), ossProperties.getSecretKey())
                .build();
    }
}
