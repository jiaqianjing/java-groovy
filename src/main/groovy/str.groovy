/**
 知识点:
 1>模板式字符串,这是个很好很强大的功能.
 2>如何多段式风格字符串.
 3>如何用groovy的风格完成substring功能.
 4>中文字符串长度
 **/

def country="南非"

def stringTemplate(country){
    return "${country}世界杯来了";
}

def s = stringTemplate(country);

println(s+",length: "+s.length());

println("Country is: "+s[0,2]);
println("Country is: "+s.substring(0,2));


def greetingWithFormat = """
        欢迎来到${country}
        6.11-7.11, 世界杯将会激情展开!
        haha
    """;
println(greetingWithFormat);

