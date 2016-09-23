package edu.buaa.qjjia2;

import groovy.lang.GroovyObject;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;


import java.io.IOException;

/**
 * Created by Administrator on 2016/9/23.
 */
public  class GroovyCommonUtil {
    //该变量用于指明groovy脚本所在的父目录
//    static String root[] = new String[]{"E:/workspace/AcousticSub/GroovyDemo/src/script/"};
    static String root = new String("E:/workspace/AcousticSub/GroovyDemo/src/script/");
    static GroovyScriptEngine groovyScriptEngine;

    static {
        try {
            groovyScriptEngine = new GroovyScriptEngine(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 用于调用指定Groovy脚本中的指定方法
     *
     * @param scriptName 脚本名称
     * @param methodName 方法名称
     * @param params     方法参数
     * @return
     */
    @SuppressWarnings({"rawtypes"})
    public static Object invokeMethod(String scriptName, String methodName, Object... params) throws Exception {
        Object ret = null;
        Class scriptClass = null;
        GroovyObject scriptInstance = null;

        try {
            scriptClass = groovyScriptEngine.loadScriptByName(scriptName);
            scriptInstance = (GroovyObject) scriptClass.newInstance();
        } catch (ResourceException | ScriptException | InstantiationException | IllegalAccessException e1) {
            throw new Exception("加载脚本" + scriptName + "失败");
        }

        try {
            ret = (String) scriptInstance.invokeMethod(methodName, params);
        } catch (IllegalArgumentException e) {
            throw new Exception("调用方法[" + methodName + "]失败，因参数不合法");
        } catch (Exception e) {
            throw new Exception("调用方法[" + methodName + "]失败");
        }

        return ret;
    }
}
