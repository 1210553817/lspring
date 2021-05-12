# 起飞！Right now！！！
## 【一、创建项目】
### 1.New Project
![很干净
](https://img-blog.csdnimg.cn/20210512142619740.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM1Mzc0Mjk3,size_16,color_FFFFFF,t_70)
### 2.pom引入servlet依赖
```java
   	<dependency>
          <groupId>javax.servlet</groupId>
          <artifactId>javax.servlet-api</artifactId>
          <version>3.1.0</version>
    </dependency>
```
### 3.创建web.xml
```java
<!DOCTYPE web-app PUBLIC
     "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
     "http://java.sun.com/dtd/web-app_2_3.dtd" >

<web-app>
    <display-name>My Spring Application</display-name>

</web-app>
```
## 【二、核心代码】
### 1.创建DispatchServlet
	- 1.创建自己的LDispatchServlet，继承HttpServlet，接管servlet容器来处理web请求。
	
	- 2.重写doGet(),doPost(),init()等方法。
### 2.配置web.xml
	- 1.在web.xml中配置映射LDispatchServlet
	- 2.创建application.properties
	- 3.在application.properties中配置 scan-package=com.lintao.qifei  #包扫描路径
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210512211131478.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM1Mzc0Mjk3,size_16,color_FFFFFF,t_70)

### 3.创建自定义注解
	-1.@LAutowired
	-2.@LController
	-3.@LService
	-4.@LRequestMapping
```java
/**
 * @program: lspring
 * @description: LAutowired注解
 * @author: Mr.Lin
 * @create: 2021-05-12 15:11
 **/
@Target(ElementType.FIELD)//注解作用在 FIELD (字段)上
@Retention(RetentionPolicy.RUNTIME)//注解生命周期在 RUNTIME 时
@Documented//注解将被包含在javadoc中
public @interface LAutowired {
    String value() default "";
}
```
```java
/**
 * @program: lspring
 * @description: LController注解
 * @author: Mr.Lin
 * @create: 2021-05-12 15:11
 **/
@Target(ElementType.TYPE)//注解作用在 TYPE (类、接口、枚举、注解)上
@Retention(RetentionPolicy.RUNTIME)//注解生命周期在 RUNTIME 时
@Documented//注解将被包含在javadoc中
public @interface LController {
    String value() default "";
}
```
```java
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
```
```java
/**
 * @program: lspring
 * @description: LRequestMapping注解
 * @author: Mr.Lin
 * @create: 2021-05-12 15:11
 **/
@Target({ElementType.TYPE,ElementType.METHOD})//注解作用在 TYPE/METHOD (类、接口、枚举、注解)/(方法)上
@Retention(RetentionPolicy.RUNTIME)//注解生命周期在 RUNTIME 时
@Documented//注解将被包含在javadoc中
public @interface LRequestMapping {
    String value() default "";
}
```
### 4.重写DispatchServlet的init()方法
列清步骤
 - 1.加载配置文件
 - 2.扫描所有相关的类
 - 3.初始化所有相关类的实例，并保存在IOC容器中
 - 4.依赖注入
 - 5.初始化HandleMapping(处理器映射器)
#### (1)加载配置文件
```java
 	/**
     *  上下文配置
     */
    private Properties contextConfig = new Properties();

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
```
#### (2)扫描所有相关的类
```java
	/**
     * 类名List
     */
    private List<String> classNameList = new ArrayList<String>();

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
```
#### (3)初始化所有相关类的实例，并保存在IOC容器中
```java
	/**
     * IOC容器
     */
    private Map<String,Object> iocMap = new HashMap<String,Object>();

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
     * 首字母小写转化
     * @param className
     * @return
     */
    private String toLowerFirstCase(String className) {
        char[] charArray = className.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }
```
#### (4)依赖注入
```java
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
```
#### (5)初始化HandleMapping(处理器映射器)
```java
 	/**
     * handleMapping处理器映射器
     */
    private Map<String,Method> handleMapping = new HashMap<String,Method>();

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
```
### 5.重写DispatchServlet的doPost()方法
#### 运行时处理 doDispatch()
```java
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
```
## 【三、测试】
- DemoController
```java
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
```
- DemoService
```java
/**
 * @program: lspring
 * @description: DemoService
 * @author: Mr.Lin
 * @create: 2021-05-12 21:03
 **/
public interface DemoService {
    String getClassNameList();
}
```
- DemoServiceImpl
```java
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
```
- 配置tomcat启动
- 浏览器访问 http://localhost:6666/getName?name=zhangsan
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210512211854808.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM1Mzc0Mjk3,size_16,color_FFFFFF,t_70)
- 浏览器访问 http://localhost:6666/getClassNameList
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210512211939628.png)


大功告成！！！
## 【四、成果】
-	项目结构
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210512211444661.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM1Mzc0Mjk3,size_16,color_FFFFFF,t_70)
- 项目Gitee链接： [https://gitee.com/lintao233/lspring](https://gitee.com/lintao233/lspring).