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

package com.luckykuang.oss.processor;

import com.luckykuang.oss.base.BusinessException;
import com.luckykuang.oss.base.ErrorCode;
import com.luckykuang.oss.util.ApplicationContextUtils;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 抽取的方法
 * @author luckykuang
 * @date 2023/11/28 11:02
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OssProcessor {

    private static final MinioClient minioClient = ApplicationContextUtils.getBean(MinioClient.class);

    /**
     * 判断存储桶是否存在
     * @param bucketName 存储桶名称
     * @return 存在-true 不存在-false
     */
    public static boolean bucketExists(String bucketName){
        BucketExistsArgs args = BucketExistsArgs.builder()
                .bucket(bucketName)
                .build();
        try {
            return minioClient.bucketExists(args);
        } catch (Exception e){
            log.error("查询存储桶状态异常",e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }
    }

    /**
     * 获取文件信息
     * @param bucketName 存储桶名称
     * @param objectName url
     * @return 返回文件信息
     */
    public static StatObjectResponse getStatObject(String bucketName, String objectName) {
        StatObjectArgs args = StatObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build();
        try {
            return minioClient.statObject(args);
        } catch (Exception e){
            log.error("获取对象信息异常",e);
            throw new BusinessException(ErrorCode.UNKNOWN);
        }
    }

    /**
     * 获取文件名
     * @param objectName url
     * @return 无后缀的文件名
     */
    public static String getFileNameByObjectName(String objectName){
        if (StringUtils.isBlank(objectName)){
            return null;
        }
        String[] split = objectName.split("/");
        String nameAndExt = split[split.length - 1];
        return nameAndExt.substring(nameAndExt.lastIndexOf(".") + 1);
    }

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
                "      \"Principal\":{\n" +
                "                \"AWS\": [\n" +
                "                    \"*\"\n" +
                "                ]\n" +
                "            },\n" +
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
                "      \"Principal\":{\n" +
                "                \"AWS\": [\n" +
                "                    \"*\"\n" +
                "                ]\n" +
                "            },\n" +
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
                "      \"Principal\":{\n" +
                "                \"AWS\": [\n" +
                "                    \"*\"\n" +
                "                ]\n" +
                "            },\n" +
                "      \"Resource\": [\n" +
                "        \"arn:aws:s3:::"+bucketName+"/*\"\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }
}
