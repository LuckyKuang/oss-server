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

package com.luckykuang.oss.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 存储桶策略配置工具类
 * @author luckykuang
 * @date 2023/11/23 14:19
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OssUtils {
    /**
     * 存储桶默认策略
     * @param bucketName 存储桶名称
     * @return
     */
    public static String defaultBucketPolicy(String bucketName){
        return  "{\n" +
                "  \"Version\": \"2012-10-17\",\n" +
                "  \"Statement\": [\n" +
                "    {\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Action\": [\n" +
                "                \"s3:ListAllMyBuckets\",\n" +
                "                \"s3:ListBucket\",\n" +
                "                \"s3:GetBucketLocation\",\n" +
                "                \"s3:GetObject\",\n" +
                "                \"s3:PutObject\",\n" +
                "                \"s3:DeleteObject\"\n" +
                "      ],\n" +
                "      \"Principal\":\"*\",\n" +
                "      \"Resource\": [\n" +
                "        \"arn:aws:s3:::"+bucketName+"/*\"\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    /**
     * 存储桶自定义策略
     * @param bucketName
     * @return
     */
    public static String customBucketPolicy(String bucketName, List<String> bucketPolicyList){
        StringBuilder rule = new StringBuilder();
        for (int i = 0; i < bucketPolicyList.size(); i++) {
            rule.append("\"");
            rule.append(bucketPolicyList.get(i));
            if (i == bucketPolicyList.size() - 1){
                rule.append("\"\n");
                break;
            }
            rule.append("\",\n");
        }
        return  "{\n" +
                "  \"Version\": \"2012-10-17\",\n" +
                "  \"Statement\": [\n" +
                "    {\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Action\": [\n" +
                rule +
                "      ],\n" +
                "      \"Principal\":\"*\",\n" +
                "      \"Resource\": [\n" +
                "        \"arn:aws:s3:::"+bucketName+"/*\"\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    /**
     * 存储桶只读策略
     * @param bucketName 存储桶名称
     * @return
     */
    public static String readOnlyBucketPolicy(String bucketName){
        return  "{\n" +
                "  \"Version\": \"2012-10-17\",\n" +
                "  \"Statement\": [\n" +
                "    {\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Action\": [\n" +
                "                \"s3:GetObject\"\n" +
                "      ],\n" +
                "      \"Principal\":\"*\",\n" +
                "      \"Resource\": [\n" +
                "        \"arn:aws:s3:::"+bucketName+"/*\"\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }
}
