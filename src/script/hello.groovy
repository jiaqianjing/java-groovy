/**
 * Created by Administrator on 2016/9/23.
 */
def helloWithoutParam(){
    println "start to call helloWithoutParam!"
    return "success, helloWithoutParam";
}

def helloWithParam(person, id){
    println "start to call helloWithParam, param{person:" + person + ", id:" + id + "}";
    return "success, helloWithParam";
}