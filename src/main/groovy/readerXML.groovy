
/**
 知识点:
 1>怎样生成xml
 2>怎样读xml
 **/

def sample ="""
    We'll create this xml:
"""
println(sample);

//写xml
testxml = "E:/workspace/AcousticSub/GroovyDemo/src/file/test.xml";
import groovy.xml.MarkupBuilder
xml = new MarkupBuilder(new FileWriter(testxml));

xml.beans{
    bean(id:"myBean1",class:"com.diegochen.Bean1"){
        property(name:"dao",ref:"dao1")
    }
    bean(id:"myBean2",class:"com.diegochen.Bean2"){
        property(name:"dao",ref:"dao2")
    }
}
println("Done creation. Now reading xml\n")
//Read xml
start =System.currentTimeMillis();//传说XmlParser吃内存,XmlSlurper速度慢

def node = new XmlParser().parse(new File(testxml))
println("node name:"+node.name());//取得node的名字.为什么不是getname()???? 命名规则真混乱
end =System.currentTimeMillis();
println("elapsed time: "+ (end-start)+" ms");

/**
 node = new XmlSlurper().parse(new File(testxml))
 println("node name:"+node.name());
 end =System.currentTimeMillis();
 println("elapsed time: "+ (end-start)+" ms");
 **/

//访问子节点和属性
println("How many beans?: "+node.children().size());
def bean2 = node.children()[1];
println("2nd bean's id: "+bean2."@id");
println("2nd bean's class: "+bean2."@class");
println("2nd bean's dao property: "+bean2.children()[0]."@ref");
