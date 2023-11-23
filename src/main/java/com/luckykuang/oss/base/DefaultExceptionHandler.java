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

package com.luckykuang.oss.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 * @author luckykuang
 * @date 2023/11/23 15:24
 */
@Slf4j
@RestControllerAdvice
public class DefaultExceptionHandler {
    /**
     * 处理业务异常 参见 {@link ErrorCode}
     *
     */
    @ExceptionHandler(value = BusinessException.class)
    public ApiResult<?> businessExceptionHandler(BusinessException ex) {
        log.info("[businessExceptionHandler]:{}", ex.getMessage());
        return ApiResult.failed(ex.getCode(), ex.getMessage());
    }

    /**
     * 兜底所有异常处理
     */
    @ExceptionHandler(value = Exception.class)
    public ApiResult<?> defaultExceptionHandler(Exception ex) {
        log.error("[defaultExceptionHandler]", ex);
        return ApiResult.failed(ErrorCode.UNKNOWN,ex.getMessage());
    }
}
