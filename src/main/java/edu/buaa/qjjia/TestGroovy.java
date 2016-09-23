package edu.buaa.qjjia;

import groovy.lang.MetaClass;
import groovy.lang.MetaMethod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TestGroovy {
    public static void main(String[] args) throws IOException {
        parseGroovy();
    }


    public static void parseGroovy() throws IOException {
        String groovyfile = getGroovyFile();
        Map<String, MetaMethod> methodCache = new HashMap<String, MetaMethod>();

        groovy.lang.Script groovy = new groovy.lang.GroovyShell().parse(groovyfile);
        MetaClass meta = groovy.getMetaClass();
        for (MetaMethod method : meta.getMethods()) {
            methodCache.put(method.getName(), method);
        }

        MetaMethod catmethod = methodCache.get("catmethod");
        Object[] cat = {"cat"};
        String result1 = (String) catmethod.doMethodInvoke(groovy, cat);
        System.out.println(result1);

        MetaMethod dogmethod = methodCache.get("dogmethod");
        Object[] dog = {"dog"};
        String result2 = (String) catmethod.doMethodInvoke(groovy, dog);
        System.out.println(result2);

    }


    private static String getGroovyFile() throws IOException {
        StringBuffer bf = new StringBuffer();

        BufferedReader br = new BufferedReader(new FileReader(new File("E:/workspace/AcousticSub/GroovyDemo/src/script/test.groovy")));
        String line;
        while ((line = br.readLine()) != null) {
            bf.append(line + "\n");
        }
        return bf.toString();
    }
}
