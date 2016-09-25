/**
 知识点
 1>如何读写文件
 2>groovy的文件操作
 **/
BufferedReader reader = new File('E:/workspace/AcousticSub/GroovyDemo/src/file/abc.txt').newReader('GBK')
BufferedWriter writer = new File('E:/workspace/AcousticSub/GroovyDemo/src/file/abc.csv').newWriter('UTF-8')
reader.eachLine { line ->
    if(line && line[0] != '#') {
        print(line)
        writer.writeLine(line)
    }
}
writer.close()

def createFile(path,createIfNotExist){
    def file = new File(path);
    if( !file.exists() ){
        if(createIfNotExist){
            if(!file.getParentFile().exists()){
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
        }else{
            throw NullPointerException("Missing file: "+path);
        }
    }
    return file;
}

def copyFile(String frompath,String topath,boolean createDestIfNotExist){
    def fromfile = new File(frompath);
    if( !fromfile.exists()){
        println(" ###################Missing file: " + fromfile+"\n");
        return false;
    }else{
        println(" Copying file: " + frompath+"\n");
    }

    def tofile = createFile(topath,createDestIfNotExist);

    tofile.withWriter { file ->
        fromfile.eachLine { line ->
            file.writeLine(line)
        }
    }
    return true;
}
