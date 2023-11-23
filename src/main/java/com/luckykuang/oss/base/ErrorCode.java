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

/**
 * 错误码枚举类
 * @author luckykuang
 * @date 2023/4/10 20:02
 */
public enum ErrorCode {
    SUCCESS("0000","操作成功"),
    UNKNOWN("9999","操作失败"),
    BUCKET_EXIST("1001","bucket 已存在"),
    BUCKET_NOT_EXIST("1002","bucket 不存在"),
    NOT_UPLOAD_EMPTY_FILE("1003","禁止上传空文件"),
    NOT_UPLOAD_EMPTY_NAME("1004","禁止上传空白名称的文件"),
    NOT_UPLOAD_EMPTY_EXT("1005","禁止上传无后缀的文件"),
    NOT_UPLOAD_EMPTY_TYPE("1006","禁止上传无文件类型的文件"),
    FILE_PATH_INCORRECT("1007","文件路径有误"),
    ;
    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
