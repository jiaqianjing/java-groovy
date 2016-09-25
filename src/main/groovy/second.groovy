/**
 知识点:
 1>如何定义和函数
 2>如何在函数里定义默认参数
 **/
def func(a, b) {
    println("Your name is: " + a + " and your age is: " + b);
}


def func1(a, b = 25) {
    if (b == null) {
        println("Your name is: " + a + " and your age is a secret");
    } else {
        println("Your name is: " + a + " and your age is: " + b);
    }
}

func("Diego", 30)
func1("Diego")
func1("Diego", 30)
func1("Diego", null)

