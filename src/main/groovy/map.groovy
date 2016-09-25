/**
 知识点:
 1>如何使用map
 2>如何遍历map
 **/

def mymap = ["name": "Diego", age: 30, hobbies: ["Football", "Reading", "Bible"]]
println("Your name is " + mymap["name"] + " ,age is " + mymap["age"] + " ,hobbies: " + mymap["hobbies"]);

//add element
mymap.location = "Shenzhen"
mymap.test = "test"
println(mymap);

//loop map by closure.

mymap.each { key, value ->
    println("Key: " + key + ",value: " + value);
}

println(mymap.keySet())
