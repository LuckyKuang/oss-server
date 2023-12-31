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

import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * spring bean 工具类
 * @author luckykuang
 * @date 2023/11/28 11:05
 */
@Component
public class ApplicationContextUtils  implements ApplicationContextAware {

    // 获取applicationContext
    @Getter
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (ApplicationContextUtils.applicationContext == null){
            ApplicationContextUtils.applicationContext = applicationContext;
        }
    }

    // 通过name获取Bean
    public static Object getBean(String name){
        return applicationContext.getBean(name);
    }

    // 通过class获取Bean
    public static <T> T getBean(Class<T> clazz){
        return applicationContext.getBean(clazz);
    }

    // 通过name和Clazz返回指定的Bean
    public static <T> T getBean(String name,Class<T> clazz){
        return applicationContext.getBean(name, clazz);
    }
}
