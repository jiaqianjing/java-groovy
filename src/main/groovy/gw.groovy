import org.elasticsearch.common.xcontent.XContentFactory

/**
 * Created by Administrator on 2016/9/26.
 */
def parse(bc, record) {
    json = XContentFactory.jsonBuilder().startObject()
            .field(bc.TYPE, record.get(bc.TYPE))
            .field(bc.SID, record.get(bc.SID))
            .field(bc.UID, record.get(bc.UID));
    logs = record.get("logs")
    if (logs != null) {
        return "sucess"
    }
}

def toIPv4(ip){
    return "%d.%d.%d.%d".format(ip >> 24 & 0xff, ip >> 16 & 0xff, ip >> 8 & 0xff, ip & 0xff)
}