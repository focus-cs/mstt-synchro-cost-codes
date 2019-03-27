/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.schneider.api.cost_codes.dao;

import com.schneider.api.cost_codes.business.UserLog;
import com.schneider.api.cost_codes.data.PackageImport;
import com.schneider.api.cost_codes.data.ProjectImport;
import com.schneider.api.cost_codes.data.TaskImport;
import com.schneider.api.cost_codes.database.DbConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import org.apache.log4j.Logger;

/**
 *
 * @author lahou
 */
public class BaseTamponDao {

    private DbConnection dbcon;
    private static final Logger LOG = Logger.getLogger(BaseTamponDao.class);
    private static final UserLog USER_LOG = UserLog.getInstance();
    private List<ProjectImport> projectImportList;
    private final static String CREATION_OPER = "creation";
    private final static String RENAME_OPER = "rename";
    private final static String CLOSE_OPER = "close";
    private final static String REOPEN_OPER = "reopen";
    private static final String CHANGE_ID_OPER = "changeid";
    private SimpleDateFormat sdf;
    private Calendar cal;

    public BaseTamponDao(DbConnection dbcon) {
        this.dbcon = dbcon;
        this.cal = Calendar.getInstance();
        this.sdf = new SimpleDateFormat("yyyy-MM-dd");
    }

    public DbConnection getDbcon() {
        return dbcon;
    }

    public void setDbcon(DbConnection dbcon) {
        this.dbcon = dbcon;
    }

    public List<ProjectImport> readDB(String packageName) {
        try {
            projectImportList = new ArrayList<ProjectImport>();
            String query;
            query = "SELECT \"AccessCostCode\", \"ProjectID\", \"ID\", \"GlobalID\", \"NewID\", \"NewGlobalID\", \"Name\", \"AccountingSystem\", \"Closed\", \"Modification\", \"WBS Element ID\", \"Workpackage MS Id\", \"RE IP Owner\" FROM psnext.\"C1_mstt_synchro_cost_codes_IN\" WHERE \"PackageName\" ='" +packageName+"'";
            LOG.debug(query);
            ResultSet rs = dbcon.executeRequete(query);
            while (rs.next()) {
                if ("".equals(rs.getString(1)) || "".equals(rs.getString(2)) || "".equals(rs.getString(3)) || "".equals(rs.getString(7)) || "".equals(rs.getString(8)) || "".equals(rs.getString(10))) {
                    USER_LOG.warning(rs.getString(2), rs.getString(4), rs.getString(3), 20, packageName);
                    continue;
                }
                TaskImport taskImport = new TaskImport();
                taskImport.setGlobalID(rs.getString(4));
                taskImport.setName(rs.getString(7));
                taskImport.setTaskID(rs.getString(3));
                taskImport.setAccSystem(rs.getString(8));
                taskImport.setNewID(rs.getString(5));
                taskImport.setNewGlobalID(rs.getString(6));
                taskImport.setWbsElementID(rs.getString(11));
                taskImport.setWorkpackageMsId(rs.getString(12));
                taskImport.setReIPOwner(rs.getString(13));
                taskImport.setClosed(rs.getBoolean(9));
                String operation = rs.getString(10).toLowerCase();
                if (CREATION_OPER.equalsIgnoreCase(operation)) {
                    taskImport.setModification(TaskImport.CREATION);
                } else if (RENAME_OPER.equalsIgnoreCase(operation)) {
                    taskImport.setModification(TaskImport.RENAME);
                } else if (CLOSE_OPER.equalsIgnoreCase(operation)) {
                    taskImport.setModification(TaskImport.CLOSE);
                } else if (REOPEN_OPER.equalsIgnoreCase(operation)) {
                    taskImport.setModification(TaskImport.REOPEN);
                } else if (CHANGE_ID_OPER.equalsIgnoreCase(operation)) {
                    taskImport.setModification(TaskImport.CHANGE_ID);
                } else {
                    USER_LOG.warning(rs.getString(2), rs.getString(4), rs.getString(3), 8, packageName);
                    continue;
                }

                ProjectImport projectImport = getProjectImport(rs.getString(2));
                if (projectImport == null) {
                    projectImport = new ProjectImport();
                    projectImport.setProjectID(rs.getString(2));
                    this.addProjectImport(projectImport);
                }
                PackageImport packageImport = projectImport.getPackageImport(rs.getString(1));
                if (packageImport == null) {
                    packageImport = new PackageImport();
                    packageImport.setPackageID(rs.getString(1));
                    projectImport.addPackageImport(packageImport);
                }

                if (packageImport.getTaskImport(rs.getString(3)) != null) {
                    USER_LOG.warning(rs.getString(2), rs.getString(4), rs.getString(3), 3, packageName);
                }
                packageImport.addTaskImport(taskImport);
            }

        } catch (SQLException ex) {
            LOG.error(ex);
        }
        return projectImportList;
    }

    protected ProjectImport getProjectImport(final String projectID) {
        ProjectImport result = null;

        for (ProjectImport projectImport : this.projectImportList) {

            if (projectID.equals(projectImport.getProjectID())) {
                result = projectImport;
                break;
            }

        }

        return result;
    }

    protected void addProjectImport(final ProjectImport projectImport) {
        this.projectImportList.add(projectImport);
    }

    public void cleanData(String packageName) {
        try {
            String query;
            query = "DELETE FROM psnext.\"C1_mstt_synchro_cost_codes_IN\" WHERE \"PackageName\" ='" +packageName+"'";
            LOG.debug(query);
            dbcon.executeUpdate(query);
        } catch (SQLException ex) {
            LOG.error(ex);
        }
    }
    
    public List<String> getAllPackageName(){
        List<String> packages = new ArrayList<>();
        try {
            String query;
            query = "UPDATE psnext.\"C1_mstt_synchro_cost_codes_IN\" SET \"PackageName\"='"+UUID.randomUUID().toString()+"' WHERE \"PackageName\" is null and \"ExecutionDate\" is null";
            LOG.debug(query);
            dbcon.executeUpdate(query);
            query = "SELECT distinct \"PackageName\" FROM psnext.\"C1_mstt_synchro_cost_codes_IN\" WHERE \"ExecutionDate\" is null";
            LOG.debug(query);
            ResultSet rs = dbcon.executeRequete(query);
            while (rs.next()) {
                packages.add(rs.getString(1).trim());
            }
            query = "UPDATE psnext.\"C1_mstt_synchro_cost_codes_IN\" SET \"ExecutionDate\"='"+sdf.format(cal.getTime())+"' WHERE \"ExecutionDate\" is null";
            LOG.debug(query);
            dbcon.executeUpdate(query);
        } catch (SQLException ex) {
            LOG.error(ex);
        }
        return packages;
    }
    
    public void writeLog(int status, String projectID, String globalID, String id,int errorCode, String packageName){
        String query = "INSERT INTO psnext.\"C1_mstt_synchro_cost_codes_OUT\"(\"STATUS\", \"ProjectID\", \"GlobalID\", \"ID\", \"ERROR_CODE\", \"PackageName\", \"ExecutionDate\") "
                     + "VALUES ("+status+", '"+projectID+"', '"+globalID+"', '"+id+"', "+errorCode+", '"+packageName+"', '"+sdf.format(cal.getTime())+"');";
        try {
            LOG.debug(query);
            dbcon.executeUpdate(query);
        } catch (SQLException ex) {
            LOG.error(ex);
        }
    }

}
