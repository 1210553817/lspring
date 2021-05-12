package com.lintao.qifei.spring.annotation;

import java.lang.annotation.*;

/**
 * @program: lspring
 * @description: LService注解
 * @author: Mr.Lin
 * @create: 2021-05-12 15:11
 **/
@Target(ElementType.TYPE)//注解作用在 TYPE (类、接口、枚举、注解)上
@Retention(RetentionPolicy.RUNTIME)//注解生命周期在 RUNTIME 时
@Documented//注解将被包含在javadoc中
public @interface LService {
    String value() default "";
}
