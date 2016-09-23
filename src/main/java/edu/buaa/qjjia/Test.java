package edu.buaa.qjjia;

import java.io.IOException;

/**
 * Created by Administrator on 2016/9/22.
 */
public class Test {
    public static void main(String[] args) {

        ReflectToGroovy reflectToGroovy = new ReflectToGroovy();
        try {
            reflectToGroovy.groovyToJava();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }
}
