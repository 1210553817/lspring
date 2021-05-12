package com.lintao.qifei.demo.controller;

import com.lintao.qifei.demo.service.DemoService;
import com.lintao.qifei.spring.annotation.LAutowired;
import com.lintao.qifei.spring.annotation.LController;
import com.lintao.qifei.spring.annotation.LRequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @program: lspring
 * @description: demo测试
 * @author: Mr.Lin
 * @create: 2021-05-12 19:11
 **/
@LController
public class DemoController {

    @LAutowired
    private DemoService demoService;

    @LRequestMapping("/getName")
    public void getName(HttpServletRequest request, HttpServletResponse response){
        String name = request.getParameter("name");
        try {
            response.getWriter().write(name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @LRequestMapping("/getClassNameList")
    public void getClassNameList(HttpServletRequest request, HttpServletResponse response){
        String result = demoService.getClassNameList();
        try {
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
