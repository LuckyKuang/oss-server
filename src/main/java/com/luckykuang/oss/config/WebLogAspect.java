package com.luckykuang.oss.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 全局日志捕获
 * @author Pontus
 * @since 2025/1/7 13:59
 */
@Aspect
@Component
@AllArgsConstructor
public class WebLogAspect {
    private final Logger logger = LoggerFactory.getLogger(WebLogAspect.class);
    private final ObjectMapper objectMapper;

    @Pointcut("execution(public * com.luckykuang.oss.controller..*.*(..))")
    public void controllerLog(){}

    @Around("controllerLog()")
    public Object logControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) Objects.requireNonNull(requestAttributes)).getRequest();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        Object[] args = joinPoint.getArgs();
        String[] parameterNames = signature.getParameterNames();

        Map<String, Object> headerMap = new HashMap<>();
        Map<String, Object> paramMap = new HashMap<>();

        // 请求头信息
        Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            // 跳过token信息打印
            if ("Authorization".equalsIgnoreCase(headerName)) {
                continue;
            }
            String headerValue = httpServletRequest.getHeader(headerName);
            headerMap.put(headerName, headerValue);
        }

        boolean requestFlag = false;
        boolean multipartFileFlag = false;
        boolean requestFlag2 = false;
        boolean multipartFileFlag2 = false;

        // 请求参数
        for (int i = 0; i < parameterNames.length; i++) {
            // HttpServletRequest入参获取
            if (args[i] instanceof HttpServletRequest) {
                HttpServletRequest request = (HttpServletRequest) args[i];
                Map<String, String[]> parameterMap = request.getParameterMap();
                for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                    paramMap.put(entry.getKey(), String.join(",", entry.getValue()));
                }
                requestFlag = true;
            }
            // 文件入参获取
            if (args[i] instanceof MultipartFile) {
                MultipartFile file = (MultipartFile) args[i];
                paramMap.put(parameterNames[i], file.getOriginalFilename());
                multipartFileFlag = true;
            }
            // 其他入参获取
            if (!(args[i] instanceof HttpServletRequest) && !requestFlag && !multipartFileFlag) {
                if (args[i] instanceof String && args[i].toString().endsWith(",")) {
                    paramMap.put(parameterNames[i], args[i].toString().substring(0, args[i].toString().lastIndexOf(",")));
                } else if (this.isEntityClass(args[i])) {
                    Class<?> aClass = args[i].getClass();
                    Field[] fields = aClass.getDeclaredFields();
                    for (Field field : fields) {
//                        field.setAccessible(true);
                        String name = field.getName();
                        // 跳过序列化id
                        if ("serialVersionUID".equals(name)) {
                            continue;
                        }
                        /*
                         * 得到字段值 通过get属性获得
                         * */
                        PropertyDescriptor pd = new PropertyDescriptor(name, aClass);
                        Method getMethod = pd.getReadMethod();
                        Object invoke = getMethod.invoke(args[i]);
                        // HttpServletRequest入参获取
                        if (invoke instanceof HttpServletRequest) {
                            HttpServletRequest request = (HttpServletRequest) invoke;
                            Map<String, String[]> parameterMap = request.getParameterMap();
                            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                                paramMap.put(entry.getKey(), String.join(",", entry.getValue()));
                            }
                            requestFlag2 = true;
                        }
                        // 文件入参获取
                        if (invoke instanceof MultipartFile) {
                            MultipartFile file = (MultipartFile) invoke;
                            paramMap.put(name, file.getOriginalFilename());
                            multipartFileFlag2 = true;
                        }
                        // 其他
                        if (!requestFlag2 && !multipartFileFlag2) {
                            paramMap.put(name, invoke);
                        }
                    }
                } else {
                    paramMap.put(parameterNames[i], args[i]);
                }
            }
        }

        // 记录请求参数
        try {
            String headers = objectMapper.writeValueAsString(headerMap);
            String params = objectMapper.writeValueAsString(paramMap);
            logger.info("################The Request Url: [{}], Class: [{}#{}], IP: [{}], Headers:[{}] Params: [{}]",
                    httpServletRequest.getRequestURL(),method.getDeclaringClass().getName(),method.getName(),httpServletRequest.getRemoteAddr(),headers,params);
        } catch (Exception e) {
            logger.error("################The Request Url: [{}], Class: [{}#{}], IP: [{}], errorMsg: [{}]",
                    httpServletRequest.getRequestURL(),method.getDeclaringClass().getName(),method.getName(),httpServletRequest.getRemoteAddr(),e.getMessage());
        }
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long end = System.currentTimeMillis();

        // 记录返回结果
        try {
            String response = objectMapper.writeValueAsString(result);
            logger.info("################The Response Url: [{}], Class: [{}#{}], IP: [{}], Time(ms): [{}], Result: [{}]",
                    httpServletRequest.getRequestURL(), method.getDeclaringClass().getName(), method.getName(),httpServletRequest.getRemoteAddr(), end - start, response);
        } catch (Exception e) {
            logger.error("################The Response Url: [{}], Class: {}#{}, IP: [{}], Time(ms): [{}]",
                    httpServletRequest.getRequestURL(), method.getDeclaringClass().getName(), method.getName(),httpServletRequest.getRemoteAddr(),end - start,e);
        }

        return result;
    }

    private boolean isEntityClass(Object entity) {
        if (entity == null) {
            return false;
        }
        String simpleName = entity.getClass().getSimpleName();
        // DTO 或 QO 或 VO 结尾
        return simpleName.endsWith("DTO") || simpleName.endsWith("QO") || simpleName.endsWith("VO");
    }
}
