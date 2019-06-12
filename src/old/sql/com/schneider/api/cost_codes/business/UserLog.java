package old.sql.com.schneider.api.cost_codes.business;

import old.sql.com.schneider.api.cost_codes.dao.BaseTamponDao;
import old.sql.com.schneider.api.cost_codes.database.DbConnection;
import java.util.ArrayList;
import java.util.List;

public class UserLog {

    private static UserLog classInstance;

    private static String fileName;

    public static final int ERROR_STATUS = 2;

    public static final int WARNING_STATUS = 1;

    public static final int OK_STATUS = 0;

    private transient int status = 0;

    private transient final List<String> traces;

    private static DbConnection dbcon;

    private static BaseTamponDao dao;

    /**
     *
     * @return The unique instance of UserLog.
     */
    public static UserLog getInstance() {
        return classInstance == null ? classInstance = new UserLog() : getClassInstance();
    }

    public static UserLog getInstance(String csvFileName) {

        if (!csvFileName.equals(fileName)) {
            classInstance = new UserLog();
        }

        return getClassInstance();
    }

    public static UserLog getInstance(DbConnection dbcon) {
        return classInstance == null ? classInstance = new UserLog(dbcon) : getClassInstance();
    }

    public UserLog() {
        this.traces = new ArrayList<String>();
    }

    public UserLog(DbConnection dbcon) {
        this.traces = new ArrayList<String>();
        this.dbcon = dbcon;
        this.dao = new BaseTamponDao(dbcon);
    }

    public void setConnection(DbConnection dbcon) {
        this.dbcon = dbcon;
        this.dao = new BaseTamponDao(dbcon);
    }

    /**
     *
     * @param msg
     */
    public int warning(final String msg) {
        this.traces.add("WARN: " + msg);

        if (this.status == OK_STATUS) {
            this.status = WARNING_STATUS;
        }

        return WARNING_STATUS;
    }

    /**
     *
     * @param msg
     */
    public void error(final String msg) {
        this.traces.add("ERROR: " + msg);
        this.status = ERROR_STATUS;
    }

    /**
     *
     * @param msg
     */
    public void info(final String msg) {
        this.traces.add("INFO: " + msg);
    }

    /**
     *
     * @return On of values {ERROR_STATUS,WARNING_STATUS,OK_STATUS}.
     */
    public int getStatus() {
        return this.status;
    }

    /**
     * @return the classIsnatnce
     */
    private static UserLog getClassInstance() {
        return classInstance;
    }

    /**
     * @return the traces
     */
    public List<String> getTraces() {
        return this.traces;
    }

    public int warning(final String projectID, final String globalID, final String id, final int errorCode, final String packageName) {
        //this.traces.add(WARNING_STATUS + ";" + projectID + ";" + globalID + ";" + id + ";" + errorCode);

        if (this.status == OK_STATUS) {
            this.status = WARNING_STATUS;
        }
        dao.writeLog(status, projectID, globalID, id, errorCode, packageName);
        return this.status;
    }

    public void error(final String projectID, final String globalID, final String id, final int errorCode, final String packageName) {
        //this.traces.add(ERROR_STATUS + ";" + projectID + ";" + globalID + ";" + id + ";" + errorCode);
        this.status = ERROR_STATUS;
        dao.writeLog(status, projectID, globalID, id, errorCode, packageName);
    }

    public void info(final String projectID, final String globalID, final String id, final int errorCode, final String packageName) {
        //this.traces.add(OK_STATUS + ";" + projectID + ";" + globalID + ";" + id + ";" + errorCode);
        dao.writeLog(OK_STATUS, projectID, globalID, id, errorCode, packageName);
    }

}
