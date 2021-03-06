package old.sql.com.schneider.api.cost_codes.business;

import old.sql.com.schneider.api.cost_codes.dao.BaseTamponDao;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import old.sql.com.schneider.api.cost_codes.data.FileImport;
import old.sql.com.schneider.api.cost_codes.data.PackageImport;
import old.sql.com.schneider.api.cost_codes.data.ProjectImport;
import old.sql.com.schneider.api.cost_codes.data.TaskImport;
import old.sql.com.schneider.api.cost_codes.database.DbConnection;
import old.sql.com.schneider.api.cost_codes.database.DbController;
import old.sql.com.schneider.api.cost_codes.database.DbError;
import com.sciforma.psnext.api.DataFormatException;
import com.sciforma.psnext.api.DataViewRow;
import com.sciforma.psnext.api.Global;
import com.sciforma.psnext.api.LockException;
import com.sciforma.psnext.api.PSException;
import com.sciforma.psnext.api.Project;
import com.sciforma.psnext.api.Session;
import com.sciforma.psnext.api.Task;
import com.sciforma.psnext.api.TaskOutlineList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

public class ProjectsManager {

    /**
     * PSNextManager instance.
     */
    private transient final PSNextManager pSNextManager;

    /**
     * UserLog instance.
     */
    private static UserLog USER_LOG = UserLog.getInstance();

    /**
     * 'Global Id' filed name.
     */
    private static final String GLOBAL_ID = "Global Id";

    /**
     * 'Accounting system' filed name.
     */
    private static final String ACCO_SYS = "Accounting system";

    /**
     * 'Closed' filed name.
     */
    private static final String CLOSED = "Closed";

    /**
     * 'Allow My Work Add' filed name.
     */
    private static final String ALLOW_WORK = "Allow My Work Add";

    /**
     * 'Name' filed name.
     */
    private static final String NAME = "Name";

    /**
     * 'ID' task filed name.
     */
    private static final String FIELD_ID = "ID";

    /**
     * 'Access cost code' task filed name.
     */
    private static final String ACC_COS_CODE = "Access cost code";

    /**
     * 'Work Package ID' task filed name.
     */
    private static final String WPACKAGE_ID = "Work Package ID";

    /**
     * 'Work Package ID' task filed name.
     */
    private static final String RE_IP_OWNER = "RE IP Owner";

    /**
     * Logger instance.
     */
    private static final Logger LOG = Logger.getLogger(ProjectsManager.class);

    /**
     * Before calling the method TaskOutlineList.add(), temporarily replace the
     * ID field to skip an exception if the same ID already exists
     */
    private final static String TMP_STR = "%.%.%.%.%";

    private Global global;

    final Session session;
    
    private static String packageName;


    private static ArrayList<DataViewRow> dataViewRowList;

    /**
     * Constructor method.
     */
    public ProjectsManager(final Session session, String packageName) {
        this.pSNextManager = new PSNextManager(session);
        this.session = session;
        this.global = new Global();
        this.packageName = packageName;
    }

    /**
     *
     * @param inputDir
     */
    @SuppressWarnings("unchecked")
    public void execute(final Properties properties, Session session, DbConnection dbcon) {

        try {
            dataViewRowList = (ArrayList<DataViewRow>) session.getDataViewRowList("RE_IP_Owner", this.global);
            LOG.debug("RE_IP_Owner data view row list size = " + dataViewRowList.size());
        } catch (PSException e) {
            USER_LOG.error("", "", "", 23, packageName);
            LOG.error("Fail to retrieve dataView 'RE_IP_Owner'");
        }        
        Boolean allowPurge = Boolean.parseBoolean(properties.getProperty("allow.purge.data"));

        BaseTamponDao dao = new BaseTamponDao(dbcon);
        List<ProjectImport> listProjectImport = dao.readDB(packageName);

        for (ProjectImport projectImport : listProjectImport) {
            updateProject(projectImport, session);
        }

        if(allowPurge){
            dao.cleanData(packageName);
            LOG.debug("Purge data for " + packageName);
        }
    }

    /**
     *
     * @param projectImport
     */
    private void updateProject(final ProjectImport projectImport, Session session) {
        LOG.debug("project process : " + projectImport.getProjectID());
        int projectUpdateCode = UserLog.OK_STATUS;
        final Project proj = pSNextManager.getProjectById(projectImport.getProjectID());
        // add error message if the project not exist.
        if (proj == null) {
            LOG.debug("proj not found for " + projectImport.getProjectID());
            USER_LOG.warning(projectImport.getProjectID(), "", "", 5, packageName);
        } else {
            // if there is at least one successful operation,it must save the project.
            boolean atLastOneSucc = false;
            String projectName = null;
            try {
                // open project in writing
                projectName = proj.getStringField(NAME);
                LOG.debug("project found: " + projectName);
                proj.open(false);
                LOG.debug("project open");
                if ("Under simulation".equalsIgnoreCase(proj.getStringField("Status"))) {
                    USER_LOG.warning(projectImport.getProjectID(), "", "", 19, packageName);
                } else {
                    // start process
                    for (PackageImport packageImport : projectImport.getPackageImportList()) {
                        final String packageID = packageImport.getPackageID();

                        LOG.debug("find work package");
                        final HashMap<String, Object> fieldsVales = new HashMap<String, Object>();
                        fieldsVales.put(ACC_COS_CODE, packageID);
                        Task wPackage = findTask(proj, fieldsVales);

                        if (wPackage == null) {
                            LOG.debug("the work package is null ==> create a new + insert template");
                            if (packageImport.isCanCreate()) {
                                wPackage = createWPackage(proj, packageID);
                            } else {
                                projectUpdateCode = Math.max(projectUpdateCode, USER_LOG.warning(projectImport.getProjectID(), "", "", 6, packageName));
                            }
                        }

                        if (wPackage == null) {
                            continue;
                        }

                        for (TaskImport taskImport : packageImport.getTaskImportList()) {
                            final int tmpCode = updateTask(proj, taskImport, session);
                            projectUpdateCode = Math.max(projectUpdateCode, tmpCode);
                            if (tmpCode == UserLog.OK_STATUS) {
                                atLastOneSucc = true;
                            }
                        }

                    }
                    if (atLastOneSucc) {
                        try {
                            LOG.debug("project save");
                            proj.save();

                            try {
                                proj.publish();
                                LOG.debug("project published");
                            } catch (Exception e) {
                                // Exception to publish project.
                                LOG.error(String.format("The API cannot publish the project <%s>",projectImport.getProjectID()));
                                USER_LOG.warning(projectImport.getProjectID(), "", "", 17, packageName);
                            }
                        } catch (Exception e) {
                            // Exception to save project.
                            USER_LOG.warning(projectImport.getProjectID(), "", "", 16, packageName);
                            LOG.error(String.format("The API cannot save the project <%s>",projectImport.getProjectID()));
                        }
                    }
                }
            } catch (Exception e) {
                LOG.debug("Exception when openning the project : " + e);
                LOG.debug("Exception when openning the project : " + e.getLocalizedMessage());
                LOG.debug("Stack = " + e.getStackTrace());
                StackTraceElement[] listste = e.getStackTrace();
                for (StackTraceElement te : listste) {
                    LOG.debug(te.getClassName() + " - " + te.getMethodName() + " - " + te.getFileName() + " - " + te.getLineNumber());
                }

                // Exception to open project.
                final StringBuffer strBu = new StringBuffer(String.format("The API Cannot open the project <ID:'%s', Name :'%s'> in writing", projectImport.getProjectID(), projectName));
                // get locking user
                if (e.getClass().equals(LockException.class)) {
                    strBu.append(", Locked by <" + ((LockException) e).getLockingUser() + ">");
                }

                USER_LOG.warning(projectImport.getProjectID(), "", "", 4, packageName);
                LOG.error(strBu.toString());
            } finally {
                // Closing project
                try {
                    proj.close();
                } catch (Exception e) {
                    USER_LOG.error(projectImport.getProjectID(), "", "", 18, packageName);
                    LOG.error(String.format("The API cannot close project <%s>", projectImport.getProjectID()));
                }

            }
        }
    }

    /**
     *
     * @param proj
     * @param packageID
     * @return The PSNext task.
     */
    private Task createWPackage(final Project proj, final String packageID) {

        Task result = null;

        try {
            // insert a new template at the end.
            proj.insertTemplate(packageID, null);
            final TaskOutlineList listTask = proj.getTaskOutlineList();
            result = (Task) listTask.get(listTask.size() - 1);
            result.setStringField(ACC_COS_CODE, packageID);
            result.setBooleanField(CLOSED, false);
            listTask.setOutlineLevel(result, 1);
        } catch (Exception e) {
            // Exception to insert template.
            try {
                USER_LOG.warning(proj.getStringField("ID"), "", "", 7, packageName);
            } catch (PSException e1) {
                USER_LOG.warning(proj.toString(), "", "", 7, packageName);
            }
            LOG.error(String.format("The API cannot create the Work package < %s = %s >", ACC_COS_CODE, packageID));
        }

        return result;
    }

    /**
     *
     * @param proj
     * @param field
     * @param value
     * @return The PSNext task;
     */
    @SuppressWarnings("unchecked")
    private static Task findTask(final Project proj, final Map<String, Object> fieldsValues) {
        Task result = null;
        try {
            final List<Task> taskList = proj.getTaskOutlineList();
            for (Task task : taskList) {
                boolean equals = false;
                for (String fieldName : fieldsValues.keySet()) {
                    if (fieldsValues.get(fieldName).equals(task.getValue(fieldName))) {
                        equals = true;
                    } else {
                        equals = false;
                        break;
                    }
                }
                if (equals) {
                    result = task;
                    break;
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

        return result;
    }

    /**
     * @param proj
     * @param taskImport
     * @return The PSNext task.
     */
    private static int createTask(final Project proj, final TaskImport taskImport) {
        int returnedCode = UserLog.OK_STATUS;
        Task newTask = null;
        try {
            LOG.debug("Creating task " + taskImport.getName());
            newTask = new Task(taskImport.getName(), TMP_STR + taskImport.getTaskID() + TMP_STR, proj);

            try {
                LOG.debug("Trying to set account system with value " + taskImport.getAccSystem());
                newTask.setStringField(ACCO_SYS, taskImport.getAccSystem());
                LOG.debug("Trying to set Global ID with value " + taskImport.getGlobalID());
                newTask.setStringField(GLOBAL_ID, taskImport.getGlobalID());
                LOG.debug("Trying to set Closed with value " + taskImport.isClosed());
                newTask.setBooleanField(CLOSED, taskImport.isClosed());
                LOG.debug("Trying to set Allow Work with value " + true);
                newTask.setBooleanField(ALLOW_WORK, true);
                TaskOutlineList listTask = null;

                try {
                    // if all ok add this task to the project
                    listTask = proj.getTaskOutlineList();
                    final int intPosi = getInsertPosition(proj, taskImport.getParentPackage().getPackageID());
                    LOG.debug("Inserting task at position " + intPosi);
                    listTask.add(intPosi, newTask);
                    listTask.setOutlineLevel(newTask, 2);
                    LOG.debug("Setting task ID to " + taskImport.getTaskID());
                    newTask.setStringField("ID", taskImport.getTaskID());
                } catch (Exception e) {

                    if ((listTask != null) && (listTask.contains(newTask))) {
                        listTask.remove(newTask);
                    }

                    // Exception to create a new task.
                    returnedCode = USER_LOG.warning(taskImport.getParentPackage().getParentProject().getProjectID(), taskImport.getGlobalID(), taskImport.getTaskID(), 9, packageName);
                    LOG.error(String.format(
                            "The API cannot create the code imputation <%s, %s, %s>, because Id already exists",
                            taskImport.getParentPackage().getParentProject().getProjectID(), taskImport.getTaskID(),
                            taskImport.getName()));
                    return returnedCode;
                }

                // replace WBS Element ID with the new one from csv file
                try {
                    System.out.println("Trying to set WBS Element ID with value " + taskImport.getWbsElementID());
                    newTask.setStringField("WBS Element ID", taskImport.getWbsElementID());
                    LOG.debug("Testing if WorkPackageMSID != 0 for line with data name = " + taskImport.getName() + ", "
                            + " task id = " + taskImport.getTaskID() + ", new id = " + taskImport.getNewID()
                            + ", global id = " + taskImport.getGlobalID() + ", global id = "
                            + taskImport.getNewGlobalID() + ", wbs element id = " + taskImport.getWbsElementID());
                    try {

                        if (Integer.parseInt(taskImport.getWorkpackageMsId()) != 0) {
                            LOG.debug("Looking for workpackage task with Workpackage MS ID "
                                    + taskImport.getWorkpackageMsId());

                            if (isWorkpakageMSIdequalsToATask(proj, taskImport)) {
//                newTask.setStringField("WPMSLink", taskImport.getWorkpackageMsId());
                                System.out.println("Trying to set WPMSLink with value " + taskImport.getWbsElementID());
                                LOG.debug("Updating field WPMSLink with value " + taskImport.getWorkpackageMsId());
                                newTask.setDoubleField("WPMSLink", Double.parseDouble(taskImport.getWorkpackageMsId()));
                            } else {
                                returnedCode = USER_LOG.warning(
                                        taskImport.getParentPackage().getParentProject().getProjectID(),
                                        taskImport.getGlobalID(), taskImport.getTaskID(), 20, packageName);
                            }

                        }

                    } catch (NumberFormatException nfe) {
                        LOG.error("The value " + taskImport.getWorkpackageMsId() + " is not parseable to an Integer ; WPMSLink cannot be updated.");
                        returnedCode = USER_LOG.warning(taskImport.getParentPackage().getParentProject().getProjectID(), taskImport.getGlobalID(), taskImport.getTaskID(), 20, packageName);
                        nfe.printStackTrace();
                    }

                } catch (PSException e) {
                    LOG.error(e.getMessage());
                    if (e.getMessage() != null
                            && e.getMessage().contains("Error setting Pick List Entry for Pick List type field.")) {
                        // returnedCode = USER_LOG.warning(String.format("The API cannot set the
                        // 'Accounting system' field code, because the value '%s' is not declared in the
                        // list.", taskImport.getAccSystem()));
                        returnedCode = USER_LOG.warning(taskImport.getParentPackage().getParentProject().getProjectID(),
                                taskImport.getGlobalID(), taskImport.getTaskID(), 15, packageName);
                    } else if (e.getMessage() != null && e.getMessage().contains("was not found in the list")) {
                        // returnedCode = USER_LOG.warning(String.format("The API cannot set the
                        // 'Accounting system' field code, because the value '%s' is not declared in the
                        // list.", taskImport.getAccSystem()));
                        returnedCode = USER_LOG.warning(taskImport.getParentPackage().getParentProject().getProjectID(),
                                taskImport.getGlobalID(), taskImport.getTaskID(), 15, packageName);
                    }

                }

                try {
                    // replace re IP Owner with the new one from csv file
                    boolean verif = false;

                    /**
                     * **************** Test re IP Owner
                     * ************************
                     */
                    LOG.debug("Searching  " + taskImport.getReIPOwner() + " in RE_IP_Owner data view row list ");
                    LOG.debug("RE_IP_Owner data view row list size = " + dataViewRowList.size());

                    for (DataViewRow row : dataViewRowList) {
                        String rowname = row.getStringField("Name");
                        LOG.debug("Found value =" + rowname);

                        if (taskImport.getReIPOwner().equals(rowname)) {
                            LOG.debug("it is a match !");
                            verif = true;
                            break;
                        }

                    }

                    if (verif) {
                        System.out.println("Trying to set RE IP Owner with value " + taskImport.getReIPOwner());
                        newTask.setStringField(RE_IP_OWNER, taskImport.getReIPOwner());
                    } else {
                        System.out.println("REIP Owner not set because there is no match.");
                        returnedCode = USER_LOG.warning(taskImport.getParentPackage().getParentProject().getProjectID(),
                                taskImport.getGlobalID(), taskImport.getTaskID(), 21, packageName);
                    }

                } catch (NumberFormatException e) {
                    LOG.error(e.getMessage());
                    e.printStackTrace();
                } catch (PSException e) {
                    LOG.error(e.getMessage());
                    e.printStackTrace();

                    if (e.getMessage() != null
                            && e.getMessage().contains("Error setting Pick List Entry for Pick List type field.")) {
                        // returnedCode = USER_LOG.warning(String.format("The API cannot set the
                        // 'Accounting system' field code, because the value '%s' is not declared in the
                        // list.", taskImport.getAccSystem()));
                        returnedCode = USER_LOG.warning(taskImport.getParentPackage().getParentProject().getProjectID(),
                                taskImport.getGlobalID(), taskImport.getTaskID(), 15, packageName);
                    } else if (e.getMessage() != null && e.getMessage().contains("was not found in the list")) {
                        // returnedCode = USER_LOG.warning(String.format("The API cannot set the
                        // 'Accounting system' field code, because the value '%s' is not declared in the
                        // list.", taskImport.getAccSystem()));
                        returnedCode = USER_LOG.warning(taskImport.getParentPackage().getParentProject().getProjectID(),
                                taskImport.getGlobalID(), taskImport.getTaskID(), 15, packageName);
                    }

                }

            } catch (Exception e) {
                // Exception to Set fields.
                LOG.debug("Exception when setting Pick List : " + e);
                LOG.debug("Exception when setting Pick List : " + e.getLocalizedMessage());
                LOG.debug("Stack = " + e.getStackTrace());
                StackTraceElement[] listste = e.getStackTrace();
                for (StackTraceElement te : listste) {
                    LOG.debug(te.getClassName() + " - " + te.getMethodName() + " - " + te.getFileName() + " - "
                            + te.getLineNumber());
                }

                if (e.getMessage() != null
                        && e.getMessage().contains("Error setting Pick List Entry for Pick List type field.")) {
                    // returnedCode = USER_LOG.warning(String.format("The API cannot set the
                    // 'Accounting system' field code, because the value '%s' is not declared in the
                    // list.", taskImport.getAccSystem()));
                    returnedCode = USER_LOG.warning(taskImport.getParentPackage().getParentProject().getProjectID(),
                            taskImport.getGlobalID(), taskImport.getTaskID(), 15, packageName);
                } else if (e.getMessage() != null && e.getMessage().contains("was not found in the list")) {
                    // returnedCode = USER_LOG.warning(String.format("The API cannot set the
                    // 'Accounting system' field code, because the value '%s' is not declared in the
                    // list.", taskImport.getAccSystem()));
                    returnedCode = USER_LOG.warning(taskImport.getParentPackage().getParentProject().getProjectID(),
                            taskImport.getGlobalID(), taskImport.getTaskID(), 15, packageName);
                }

                LOG.error(String.format(
                        "The API cannot set the 'Accounting system' field code, because the value '%s' is not declared in the list.",
                        taskImport.getAccSystem()));
                e.printStackTrace();
            }

        } catch (PSException e) {
            LOG.error(e.getMessage());

            if (e.getMessage() != null
                    && e.getMessage().contains("Error setting Pick List Entry for Pick List type field.")) {
                // returnedCode = USER_LOG.warning(String.format("The API cannot set the
                // 'Accounting system' field code, because the value '%s' is not declared in the
                // list.", taskImport.getAccSystem()));
                returnedCode = USER_LOG.warning(taskImport.getParentPackage().getParentProject().getProjectID(),
                        taskImport.getGlobalID(), taskImport.getTaskID(), 15, packageName);
            } else if (e.getMessage() != null && e.getMessage().contains("was not found in the list")) {
                // returnedCode = USER_LOG.warning(String.format("The API cannot set the
                // 'Accounting system' field code, because the value '%s' is not declared in the
                // list.", taskImport.getAccSystem()));
                returnedCode = USER_LOG.warning(taskImport.getParentPackage().getParentProject().getProjectID(),
                        taskImport.getGlobalID(), taskImport.getTaskID(), 15, packageName);
            }

            e.printStackTrace();
        }

        return returnedCode;
    }

    /**
     *
     * @param proj
     * @param taskImport
     * @return on of values {UserLog.WARNING_STATUS , UserLog.OK_STATUS}
     */
    private static int updateTask(final Project proj, final TaskImport taskImport, Session session) {
        int returnedCode = UserLog.OK_STATUS;

        if (taskImport.getModification() == TaskImport.CREATION) {
            returnedCode = createTask(proj, taskImport);
        } else {
            final HashMap<String, Object> fieldsVales = new HashMap<String, Object>();
            fieldsVales.put(FIELD_ID, taskImport.getTaskID());
            fieldsVales.put(WPACKAGE_ID, taskImport.getParentPackage().getPackageID());

            final Task task = findTask(proj, fieldsVales);

            if (task == null) {
                final PackageImport wPackage = taskImport.getParentPackage();
				// final String packageID = wPackage.getPackageID();

                // returnedCode = USER_LOG.warning(String.format("The API cannot find the code
                // imputation < Task ID:'%s', Task Name:'%s' > because the ID does not exist in
                // WP <Access code system:'%s'>",
                // taskImport.getTaskID(), taskImport.getName(), packageID));
                returnedCode = USER_LOG.warning(wPackage.getParentProject().getProjectID(), taskImport.getGlobalID(),
                        taskImport.getTaskID(), 13, packageName);
            } else {
                switch (taskImport.getModification()) {
                    // close operation
                    case TaskImport.CLOSE: {

                        try {
                            final boolean closed = task.getBooleanField(CLOSED);

                            if (closed) {
                                // returnedCode = USER_LOG.warning(String.format("The API cannot disable the
                                // code imputation <task id : '%s', task name '%s'> because it is already
                                // disabled", taskImport.getTaskID(),
                                // taskImport.getName()));
                                returnedCode = USER_LOG.warning(
                                        taskImport.getParentPackage().getParentProject().getProjectID(),
                                        taskImport.getGlobalID(), taskImport.getTaskID(), 12, packageName);
                            } else {
                                task.setBooleanField(CLOSED, true);
                                task.setBooleanField(ALLOW_WORK, false);
                            }

                        } catch (Exception e) {
                            LOG.error(e.getMessage());
                            e.printStackTrace();
                        }

                        break;
                    }
                    // rename operation
                    case TaskImport.RENAME: {

                        try {
                            task.setStringField(NAME, taskImport.getName());
                        } catch (Exception e) {
                            LOG.error(e.getMessage());
                            e.printStackTrace();
                        }

                        break;
                    }

                    // reopen operation
                    case TaskImport.REOPEN: {

                        try {
                            final boolean closed = task.getBooleanField(CLOSED);

                            if (!closed) {
                                // returnedCode = USER_LOG.warning(String.format("The API cannot re-open the
                                // code imputation <task id : '%s', task name '%s'> because it is already
                                // active", taskImport.getTaskID(),
                                // taskImport.getName()));
                                returnedCode = USER_LOG.warning(
                                        taskImport.getParentPackage().getParentProject().getProjectID(),
                                        taskImport.getGlobalID(), taskImport.getTaskID(), 14, packageName);
                            }

                            task.setBooleanField(CLOSED, false);
                            task.setBooleanField(ALLOW_WORK, true);
                        } catch (Exception e) {
                            LOG.error(e.getMessage());
                        }

                        break;
                    }

                    // change ID operation
                    case TaskImport.CHANGE_ID: {
                        try {

                            if (!taskImport.getNewID().isEmpty()) {
                                task.setStringField("ID", taskImport.getNewID());
                            }

                            if (!taskImport.getNewGlobalID().isEmpty()) {
                                task.setStringField(GLOBAL_ID, taskImport.getNewGlobalID());
                            }

                        } catch (Exception e) {
                            // returnedCode = USER_LOG.warning("The API cannot change ID for Task ID <" +
                            // taskImport.getTaskID() + "> to <" + taskImport.getNewID() + "> because this
                            // ID already exist.");
                            returnedCode = USER_LOG.warning(taskImport.getParentPackage().getParentProject().getProjectID(),
                                    taskImport.getGlobalID(), taskImport.getTaskID(), 10 , packageName);
                            LOG.error("The API cannot change ID for Task ID <" + taskImport.getTaskID() + "> to <"
                                    + taskImport.getNewID() + "> because this ID already exist.");
                        }

                        try {
                            // replace WBS Element ID with the new one from csv file
                            System.out.println("Trying to update WBS Element ID " + taskImport.getWbsElementID());
                            task.setStringField("WBS Element ID", taskImport.getWbsElementID());

                            LOG.debug("Testing if WorkPackageMSID != 0 for line with data name = " + taskImport.getName()
                                    + ", " + " task id = " + taskImport.getTaskID() + ", new id = " + taskImport.getNewID()
                                    + ", global id = " + taskImport.getGlobalID() + ", global id = "
                                    + taskImport.getNewGlobalID() + ", wbs element id = " + taskImport.getWbsElementID());
                            try {

                                if (Integer.parseInt(taskImport.getWorkpackageMsId()) != 0) {
                                    LOG.debug("Looking for workpackage task with Workpackage MS ID "
                                            + taskImport.getWorkpackageMsId());

                                    if (isWorkpakageMSIdequalsToATask(proj, taskImport)) {
                                        LOG.debug("Updating field WPMSLink with value " + taskImport.getWorkpackageMsId());
                                        System.out.println(
                                                "Trying to update WPMSLink with value " + taskImport.getWorkpackageMsId());
                                        task.setDoubleField("WPMSLink",
                                                Double.parseDouble(taskImport.getWorkpackageMsId()));
                                    } else {
                                        returnedCode = USER_LOG.warning(
                                                taskImport.getParentPackage().getParentProject().getProjectID(),
                                                taskImport.getGlobalID(), taskImport.getTaskID(), 20, packageName);
                                    }

                                }

                            } catch (NumberFormatException nfe) {
                                LOG.error("The value " + taskImport.getWorkpackageMsId()
                                        + " is not parseable to an Integer ; WPMSLink cannot be updated.");
                                nfe.printStackTrace();
                            }
                        } catch (PSException e) {
                            LOG.error(e.getMessage());
                            e.printStackTrace();
                        }

                        try {

                            // replace re IP Owner with the new one from csv file
                            boolean verif = false;

                            /**
                             * **************** Test re IP Owner
                             * ************************
                             */
                            LOG.debug("Searching  " + taskImport.getReIPOwner() + " in RE_IP_Owner data view row list ");
                            LOG.debug("RE_IP_Owner data view row list size = " + dataViewRowList.size());
                            for (DataViewRow row : dataViewRowList) {
                                String rowname = row.getStringField("Name");
                                LOG.debug("Found value =" + rowname);
                                if (taskImport.getReIPOwner().equals(rowname)) {
                                    LOG.debug("it is a match !");
                                    verif = true;
                                    break;
                                }

                            }

                            if (verif) {
                                System.out.println("Trying to update Re IP Owner with value " + taskImport.getReIPOwner());
                                task.setStringField(RE_IP_OWNER, taskImport.getReIPOwner());
                            } else {
                                System.out.println("No match for RE Ip Owner.");
                                returnedCode = USER_LOG.warning(
                                        taskImport.getParentPackage().getParentProject().getProjectID(),
                                        taskImport.getGlobalID(), taskImport.getTaskID(), 21, packageName);

                            }

                        } catch (NumberFormatException e) {
                            LOG.error(e.getMessage());
                            e.printStackTrace();
                        } catch (PSException e) {
                            LOG.error(e.getMessage());
                            e.printStackTrace();
                        }

                        break;
                    }

                    default:
                        break;
                }

            }

        }

        if (returnedCode == UserLog.OK_STATUS) {
            USER_LOG.info(taskImport.getParentPackage().getParentProject().getProjectID(), taskImport.getGlobalID(),
                    taskImport.getTaskID(), 0, packageName);
        }

        return returnedCode;
    }

    private static boolean isWorkpakageMSIdequalsToATask(final Project proj, final TaskImport taskImport)
            throws PSException, DataFormatException {
        // for each task
        for (int i = 0; i < proj.getTaskOutlineList().size(); i++) {
            Task currentTask = (Task) proj.getTaskOutlineList().get(i);
            // if msId == task id => ok
            if (currentTask.getDoubleField("Internal ID") == Double.parseDouble(taskImport.getWorkpackageMsId())) {
                LOG.debug("Found task " + currentTask + " with internal ID " + taskImport.getWorkpackageMsId());
                System.out.println("Found task " + currentTask + " with internal ID " + taskImport.getWorkpackageMsId());
                return true;
            }

        }

        System.out.println("Could not find any task with internal ID " + taskImport.getWorkpackageMsId());
        return false;
    }

    /**
     * Get the position to insert a new task, under the work package.
     *
     * @param proj
     * @param packageID
     * @return
     * @throws PSException
     */
    private static int getInsertPosition(final Project proj, final String packageID) throws PSException {
        @SuppressWarnings("unchecked")
        final List<Task> listTask = proj.getTaskOutlineList();
        boolean packageFounded = false;
        int position = 0;

        for (Task task : listTask) {
            final String accCostCo = task.getStringField(ACC_COS_CODE);
            final int level = task.getIntField("Outline Level");

            // break 'for' for the next package
            if (packageFounded && level == 1) {
                break;
            }
            if (packageID.equalsIgnoreCase(accCostCo) && level == 1) {
                packageFounded = true;
            }
            position++;
        }
        return position;
    }

   

}
