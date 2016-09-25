/**
 知识点:
 1>如何方便定义collection
 2>collection的各种操作
 3>注意groovy重载了+和-运算符. 所以collection可以很方便的用+和-删除元素. 在这样做的时候, 最好加上().
 4>*是很酷的一个功能, 方便的遍历集合元素.
 **/
def myrange = 25 .. 10;
println(myrange);

myrange = 11 .. 19;
println(myrange);

println("First element of collection is: "+myrange[0]);
//myrange<<</span>20;//This statement will cause exception.
println("Last element of collection is: "+myrange[-1]);
println("Sub collection: " + myrange[2,5]);
println("Reverse: " + myrange.reverse());
println("Remove element: " + (myrange - 18));
println("Remove sub collection: " + (myrange - [12,13,14,15]));

//==================

def coll = ["C","C++","Java","JavaScript","Python"]
println("Program anguages you're knowing: "+coll);
coll <<  "Groovy" // It's == coll.add("Groovy")
println("Now you're learning: " + coll[-1])

//cool syntax
coll = coll*.toUpperCase()
println(coll);

