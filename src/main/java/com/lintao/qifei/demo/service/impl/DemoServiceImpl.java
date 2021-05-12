package com.lintao.qifei.demo.service.impl;

import com.lintao.qifei.demo.service.DemoService;
import com.lintao.qifei.spring.annotation.LService;

/**
 * @program: lspring
 * @description: DemoServiceImpl
 * @author: Mr.Lin
 * @create: 2021-05-12 21:03
 **/
@LService
public class DemoServiceImpl implements DemoService {
    public String getClassNameList() {
        return "abc";
    }
}
