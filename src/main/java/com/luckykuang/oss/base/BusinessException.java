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

import lombok.Getter;

import java.io.Serial;

/**
 * 自定义异常类
 * @author luckykuang
 * @date 2023/11/23 15:22
 */
@Getter
public class BusinessException extends RuntimeException{
    @Serial
    private static final long serialVersionUID = -706327203433627774L;
    /**
     * 错误码
     */
    private final String code;
    /**
     * 错误提示
     */
    private final String message;

    public BusinessException(ErrorCode errorCode){
        super();
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }
}
