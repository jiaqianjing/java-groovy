package edu.buaa.qjjia2;

/**
 * Created by Administrator on 2016/9/23.
 */
public class Test {

    public static void main(String[] args) {

//        testGroovyWithoutParam();
        testGroovyWithParam();
//        testMyGroovy();
    }
    /**
     * 测试没有参数的方法调用
     */
    public static void testGroovyWithoutParam(){
        String result = null;
        try {
            result = (String) GroovyCommonUtil.invokeMethod("hello.groovy", "helloWithoutParam");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("testGroovy4: " + result + "\n");
    }

    /**
     * 测试携带参数的方法调用
     */
    public static void testGroovyWithParam(){
        Person person = new Person("wchi", "nanjing", 30);
        person.setPhone("17098317684");
        String result = null;
        try {
            result = (String) GroovyCommonUtil.invokeMethod("hello.groovy", "helloWithParam", person, "testGroovy4");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("testGroovy4: " + result + "\n");
    }

    /**
     * 测试携带参数的方法调用
     */
    public static void testMyGroovy(){
       String name = "cola";
        String result = null;
        try {
            result = (String) GroovyCommonUtil.invokeMethod("GrovvySystemConfigRead.groovy", "getName", name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("testGroovy5: " + result + "\n");
    }
}
