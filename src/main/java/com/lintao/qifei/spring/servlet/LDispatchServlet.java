package com.lintao.qifei.spring.servlet;

import com.lintao.qifei.spring.annotation.LAutowired;
import com.lintao.qifei.spring.annotation.LController;
import com.lintao.qifei.spring.annotation.LRequestMapping;
import com.lintao.qifei.spring.annotation.LService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @program: lspring
 * @description: LDispatchServlet
 * @author: Mr.Lin
 * @create: 2021-05-12 14:38
 **/
public class LDispatchServlet extends HttpServlet{

    /**
     *  上下文配置
     */
    private Properties contextConfig = new Properties();

    /**
     * 类名List
     */
    private List<String> classNameList = new ArrayList<String>();

    /**
     * IOC容器
     */
    private Map<String,Object> iocMap = new HashMap<String,Object>();

    /**
     * handleMapping处理器映射器
     */
    private Map<String,Method> handleMapping = new HashMap<String,Method>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req,resp);
        }catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception Detail:\n" + Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     * 【6】运行时处理
     * @param req
     * @param resp
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws InvocationTargetException, IllegalAccessException {
        //1.request拦截url并处理
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");
        System.out.println("[L-INFO-6] request url-->" + url);
        //2.判断handleMapping中是否含有该url
        if(!handleMapping.containsKey(url)){
            try {
                resp.getWriter().write("404 NOT FOUND!!");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //3.通过url获取method并invoke
        Method method = handleMapping.get(url);
        System.out.println("[L-INFO-6] method -->" + method);

        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(iocMap.get(beanName), req, resp);
        System.out.println("[L-INFO-6] method.invoke -->" + iocMap.get(beanName));
    }


    @Override
    public void init(ServletConfig servletconfig) throws ServletException {
        //1.加载配置文件
        doLoadConfig(servletconfig.getInitParameter("contextConfigLocation"));
        //2.扫描所有相关的类
        doScanner(contextConfig.getProperty("scan-package"));
        //3.初始化所有相关类的实例，并保存在IOC容器中
        doInstence();
        //4.依赖注入
        doAutowired();
        //5.初始化HandleMapping(处理器映射器)
        initHandleMapping();
        //6.运行时处理 doPost() ==>  doDispatch()
    }

    /**
     * 【1】加载配置文件
     * @param contextConfigLocation 来自web.xml的init-param参数
     */
    private void doLoadConfig(String contextConfigLocation) {
        //1.通过contextConfigLocation映射的路径读取配置项到流
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            //2.添加到上下文配置contextConfig中
            contextConfig.load(inputStream);
            System.out.println("[L-INFO-1] property file has been saved contextConfig.");
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null != inputStream){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 【2】扫描所有的相关类
     * @param scanPackage 来自application.properties的scan-package参数
     */
    private void doScanner(String scanPackage) {
        //1.读取properties的scan-package，并转化为URL包路径
        URL resourcePath = this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));
        //2.判断包路径为空则返回
        if(null == resourcePath){
            return;
        }
        //3.循环递归包路径，获取class添加到classNameList中
        File filePath = new File(resourcePath.getFile());
        for (File file : filePath.listFiles()){
            if(file.isDirectory()){
                System.out.println("[L-INFO-2]"+file.getName()+"is a directory.");
                //子目录递归
                doScanner(scanPackage+"."+file.getName());
            }else{
                if(!file.getName().endsWith(".class")){
                    System.out.println("[L-INFO-2]"+file.getName()+"is not a class file.");
                    continue;
                }
                String className = (scanPackage+"."+ file.getName()).replace(".class","");
                //保存到classNameList
                classNameList.add(className);
                System.out.println("[L-INFO-2] {" + className + "} has been saved in classNameList.");
            }
        }
    }

    /**
     * 【3】初始化所有相关类的实例，并保存在IOC容器中
     */
    private void doInstence() {
        //1.判断类名list为空则返回
        if(classNameList.isEmpty()){
            return;
        }
        try {
            //2.循环classNameList并通过反射将name和实例存入IOC容器中
            for(String className : classNameList){
                Class<?> clazz = Class.forName(className);
                //判断为@LController注解修饰的类
                if(clazz.isAnnotationPresent(LController.class)){
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    Object instence = clazz.newInstance();
                    iocMap.put(beanName,instence);
                    System.out.println("[L-INFO-3] {" + beanName + "} has been saved in iocMap.");
                }else if(clazz.isAnnotationPresent(LService.class)){
                //判断为@LService注解修饰的类
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    LService lService = clazz.getAnnotation(LService.class);
                    if(!"".equals(lService.value())){
                        beanName = toLowerFirstCase(lService.value());
                    }
                    Object instence = clazz.newInstance();
                    iocMap.put(beanName,instence);
                    System.out.println("[L-INFO-3] {" + beanName + "} has been saved in iocMap.");

                    //找service类的接口
                    for (Class<?> c : clazz.getInterfaces()){
                        if(iocMap.containsKey(c.getName())){
                            throw new Exception("The Bean Name"+ beanName+" Is Exist");
                        }
                        iocMap.put(c.getName(),instence);
                        System.out.println("[L-INFO-3] {" + c.getName() + "} has been saved in iocMap.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 【4】依赖注入
     */
    private void doAutowired() {
        //1.判断IOC容器iocMap为空则返回
        if(iocMap.isEmpty()){
            return;
        }
        //2.循环ioc容器获取对象实例
        for(Map.Entry<String,Object> entry : iocMap.entrySet()){
            //3.遍历对象属性判断是否被@LAutowired注解修饰
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for(Field field : fields){
                if(!field.isAnnotationPresent(LAutowired.class)){
                   continue;
                }
                LAutowired lAutowired = field.getAnnotation(LAutowired.class);
                String beanName = lAutowired.value();
                if("".equals(beanName)){
                    System.out.println("[L-INFO-4] {" + field.getName() + "} lAutowired.value() is null.");
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);//使可以通过反射的方式读取private属性
                try {
                    //4.通过反射给对象属性赋值
                    field.set(entry.getValue(),iocMap.get(beanName));
                    System.out.println("[L-INFO-4] field set {" + entry.getValue() + "} - {" + iocMap.get(beanName) + "}.");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 【5】初始化HandleMappings(处理器映射器)
     */
    private void initHandleMapping() {
        //1.判断IOC容器iocMap为空则返回
        if(iocMap.isEmpty()){
            return;
        }
        //2.循环ioc容器获取对象实例
        for(Map.Entry<String,Object> entry : iocMap.entrySet()){
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(LController.class)){
                continue;
            }
            //3.拼接@LRequestMapping的value
            String baseUrl = "";
            if(clazz.isAnnotationPresent(LRequestMapping.class)){
                LRequestMapping lRequestMapping = clazz.getAnnotation(LRequestMapping.class);
                baseUrl = lRequestMapping.value();
            }
            for(Method method : clazz.getMethods()){
                if(!method.isAnnotationPresent(LRequestMapping.class)){
                    continue;
                }
                LRequestMapping lRequestMapping1 = method.getAnnotation(LRequestMapping.class);
                String url = ("/"+baseUrl+"/"+lRequestMapping1.value()).replaceAll("/+","/");
                handleMapping.put(url,method);
                System.out.println("[L-INFO-5] handleMapping put {" + url + "} - {" + method + "}.");
            }
        }
    }

    /**
     * 首字母小写转化
     * @param className
     * @return
     */
    private String toLowerFirstCase(String className) {
        char[] charArray = className.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }
}
