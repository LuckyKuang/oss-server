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

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 配置类
 * @author luckykuang
 * @date 2023/11/28 17:57
 */
@Configuration
public class SwaggerConfig {
    /**
     * GroupedOpenApi 是对接口文档分组，等同于 swagger 的 Docket
     */
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("oss")
                .pathsToMatch("/oss/**")
                .build();
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("oss-server")
                        .description("Minio connect client")
                        .version("v1.0.0")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://github.com/LuckyKuang/spring-boot-base-project/blob/master/LICENSE"))
                        .contact(new Contact()
                                .name("luckykuang")
                                .email("luckykuang@foxmail.com")
                                .url("https://github.com/luckykuang")))
                .externalDocs(new ExternalDocumentation()
                        .description("SpringDoc Wiki Documentation")
                        .url("https://springdoc.org/v2"));
    }
}
