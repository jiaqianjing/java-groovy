package edu.buaa.qjjia;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.io.File;
import java.io.IOException;

/**
 * Created by Administrator on 2016/9/22.
 */
public class ReflectToGroovy {

    public void groovyToJava() throws IOException, IllegalAccessException, InstantiationException {
        ClassLoader parent = getClass().getClassLoader();
        GroovyClassLoader loader = new GroovyClassLoader(parent);
        //找到指定的groovy类
        Class groovyClass = loader.parseClass(new File("E:\\workspace\\AcousticSub\\GroovyDemo\\src\\script\\GrovvySystemConfigRead.groovy"));
        //将对象实例化并且强制转换为GroovyObject对象
        GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
        //readEmailCodeUrl方法名，null 参数值，没有为null
        System.out.println("" + groovyObject.invokeMethod("getName", "可乐"));
    }
}
