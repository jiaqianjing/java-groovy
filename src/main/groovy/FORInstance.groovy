/**
 知识点:
 1>两种循环风格.
 2>a .. repeat 这种风格等价于 i=a;i<=repeat;i++
 **/
//For statement
def forFunc(a, repeat = 10) {
    for (i = a; i < repeat; i++) {
        print(i + ",");
    }
}

def forFunc1(a, repeat = 10) {
    for (i in a..repeat) {
        print(i + ",");
    }
    println();
}

forFunc(2);
println();
forFunc1(2);

