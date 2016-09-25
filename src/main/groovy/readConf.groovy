
def propfile = "${System.properties['user.dir']}/config.gy";
println("--config file: "+propfile);
def  config = new  ConfigSlurper().parse( new  File( propfile ).toURL()) ;

def fileUpdateList = config.path.file_update_list;
def workspace_root_path = config.path.workspace_root_path;
def backup_path = config.path.backup_path;
def debugFlag = config.constants.debug;

if(debugFlag == "Y"){
    println("config:==[fileUpdateList] : "+ fileUpdateList );
    println("config:==[workspace_root_path] : "+ workspace_root_path );
    println("config:==[backup_path] : "+ backup_path );
    println("config:==[debugFlag] : "+ debugFlag );
}