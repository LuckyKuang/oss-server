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

package com.luckykuang.oss;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p>OSS 服务</p>
 * 参考 <a href="https://min.io/docs/minio/linux/developers/java/API.html?ref=docs-redirect">Minio 官方API文档</a>
 * @author luckykuang
 * @date 2023/11/14 15:36
 */
@Slf4j
@SpringBootApplication
public class OssApplication {
    public static void main(String[] args) {
        SpringApplication.run(OssApplication.class,args);
        log.info("Github Url:https://github.com/LuckyKuang/oss-server");
        log.info("Swagger Url:http://localhost:9099/swagger-ui/index.html#/");
    }
}
