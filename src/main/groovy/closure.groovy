/**
 知识点:
 1>闭包(closure)的直观例子
 2>如果没有特别声明, 闭包将用it作为变量名字
 3>如何定义和调用闭包,如何在闭包自定义变量名字
 **/

//Traditional looping collection.
def mydata = ["Java","Groovy","JavaScript"]
def printUpperCase(a){
    println(a.toUpperCase());
}

for (i in 0..2){
    printUpperCase(mydata[i]);
}

println("use closure=====================");
mydata.each{
    println(it.toUpperCase());
}

println("def closure=====================");

def myclosure = {myvar->
    println(myvar.toUpperCase());
}

mydata.each(myclosure);


println("closure and map=====================");
def mymap = ["name":"Diego",age:30,hobbies:["Football","Reading","Bible"]]
mymap.each{key,value->
    println("key is: "+key+" and value is: " + value);
}
println("closure and string=====================");
def mystring= "Diego"
mystring.each{s->
    print(s+",");
}
