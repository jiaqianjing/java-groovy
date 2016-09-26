package edu.buaa.qjjia2;

/**
 * Created by Administrator on 2016/9/26.
 */
public class BizConstants {

    public BizConstants() {
    }

    /** -------- toplevel -------- */
    /** log typpe. */
    final String TYPE = "type";

    /** session id. */
    final String SID = "sid";

    /** user id. */
    final String UID = "uid";

    /** sync id. */
    final String SYNCID = "syncid";

    /** timestamp. */
    final String TIMESTAMP = "timestamp";

    /** timestamp. */
    final String IP = "ip";

    /** callName. */
    final String CALLNAME = "callName";

    /** fields in top-level. */
//    final val META_FIELDS = Set(TYPE, SID, UID, SYNCID, TIMESTAMP, IP, CALLNAME)

    /** -------- in rawlog.extras -------- */
    /** subjects. */
    final String SUB = "sub";

    /** audio encoding. */
    final String AUE = "aue";

    /** datacenter locations. */
    final String LOC = "loc";

    /** recognition result. */
    final String RECS = "recs";

    /** call name. */
    final String CMD = "cmd";

    /** city. */
    final String CITY = "city";

    /** fields in extras. */
//    final val EXTRA_FIELDS = Set(SUB, AUE, LOC, RECS, CMD, CITY)
}
