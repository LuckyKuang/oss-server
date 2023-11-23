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

package com.luckykuang.oss.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * 图片类的文件枚举类
 * @author luckykuang
 * @date 2023/11/17 18:47
 */
@Getter
public enum ContentTypeEnum {
    DEFAULT("default","application/octet-stream"),
    JPG("jpg", "image/jpeg"),
    TIFF("tiff", "image/tiff"),
    GIF("gif", "image/gif"),
    JFIF("jfif", "image/jpeg"),
    PNG("png", "image/png"),
    TIF("tif", "image/tiff"),
    ICO("ico", "image/x-icon"),
    JPEG("jpeg", "image/jpeg"),
    WBMP("wbmp", "image/vnd.wap.wbmp"),
    FAX("fax", "image/fax"),
    NET("net", "image/pnetvue"),
    JPE("jpe", "image/jpeg"),
    RP("rp", "image/vnd.rn-realpix");

    private final String ext;

    private final String type;

    ContentTypeEnum(String ext, String type) {
        this.ext = ext;
        this.type = type;
    }

    /**
     * 获取类型
     * @param ext 后缀
     * @return 返回类型
     */
    public static String getContentType(String ext){
        if(StringUtils.isEmpty(ext)){
            return DEFAULT.getType();
        }
        for (ContentTypeEnum value : ContentTypeEnum.values()) {
            if(ext.equalsIgnoreCase(value.getExt())){
                return value.getType();
            }
        }
        return DEFAULT.getType();
    }
}
