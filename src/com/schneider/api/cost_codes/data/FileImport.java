package com.schneider.api.cost_codes.data;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.schneider.api.cost_codes.business.UserLog;

public class FileImport {

	private static final UserLog USER_LOG = UserLog.getInstance();

	private static final Logger LOG = Logger.getLogger(FileImport.class);

	private final transient List<ProjectImport> projectImportList;

	private final static String CREATION_OPER = "creation";

	private final static String RENAME_OPER = "rename";

	private final static String CLOSE_OPER = "close";

	private final static String REOPEN_OPER = "reopen";

	private static final String CHANGE_ID_OPER = "changeid";

	private final static String SEPARATOR = ";";

	private final transient String sourceFile;

	private static final int PACKAGE_ID_INDEX = 0;

	private static final int PROJECT_ID_INDEX = 1;

	private static final int TASK_ID_INDEX = 2;

	private static final int GLOBAL_ID_INDEX = 3;

	private static final int NAME_INDEX = 6;

	private static final int ACC_SYSTEM_INDEX = 7;

	private static final int STR_CLOSED_INDEX = 8;

	private static final int OPERATION_INDEX = 9;

	private static final int NEW_ID_INDEX = 4;

	private static final int NEW_GLOBAL_ID_INDEX = 5;

	private static final int WBS_ELEMENT_ID = 10;

	private static final int WORKPACKAGE_MS_ID = 11;

	private static final int RE_IP_OWNER_INDEX = 12;

	public FileImport(final String sourceFile) {
		this.projectImportList = new ArrayList<ProjectImport>();
		this.sourceFile = sourceFile;
	}

	protected void addProjectImport(final ProjectImport projectImport) {
		this.projectImportList.add(projectImport);
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

	public List<ProjectImport> getData() throws IOException {
		BufferedReader buff = null;
		InputStream ips = null;
		
		try {
			// LOading file
			ips = new FileInputStream(this.sourceFile);
			InputStreamReader ipsr = new InputStreamReader(ips);
			buff = new BufferedReader(ipsr);
			
			// To skip the header line
			buff.readLine();
			@SuppressWarnings("unused")
			int lineNumber = 1;
			String ligne;

			while ((ligne = buff.readLine()) != null) {
				lineNumber++;
				// Split the line in the array
				final String[] row = ligne.split(SEPARATOR, -2);

				if (row.length >= 13) // 13 columns in the csv file
				{
					// get access cost code.
					final String packageID = row[PACKAGE_ID_INDEX].trim();
					// get project ID.
					final String projectID = row[PROJECT_ID_INDEX].trim();
					// get task ID.
					final String taskID = row[TASK_ID_INDEX].trim();
					// get task globalID.
					final String globalID = row[GLOBAL_ID_INDEX].trim();
					// get new id.
					final String newID = row[NEW_ID_INDEX].trim();
					// get new globalID.
					final String newGlobalID = row[NEW_GLOBAL_ID_INDEX].trim();
					// get task name.
					final String name = row[NAME_INDEX].trim();
					// get task accSystem.
					final String accSystem = row[ACC_SYSTEM_INDEX].trim();
					// get task Closed.
					final String strClosed = row[STR_CLOSED_INDEX].trim();
					// get the operation.
					final String operation = row[OPERATION_INDEX].trim();
					// get the wbsElementID
					final String wbsElementID = row[WBS_ELEMENT_ID].trim();
					// get the workpackageMsId
					final String workpackageMsId = row[WORKPACKAGE_MS_ID].trim();
					// get the workpackageMsId
					final String reIPOwner = row[RE_IP_OWNER_INDEX].trim();

					// Skip the line when find an empty string
					if ("".equals(packageID) || "".equals(projectID) || "".equals(taskID) || "".equals(name)
							|| "".equals(accSystem) || "".equals(strClosed) || "".equals(operation)) {
						// USER_LOG.warning(String.format("Bad line format at line number : %s",
						// lineNumber));
						USER_LOG.warning(projectID, globalID, taskID, 20);
						continue;
					}

					final TaskImport taskImport = new TaskImport();
					
					// construct task
					taskImport.setGlobalID(globalID);
					taskImport.setName(name);
					taskImport.setTaskID(taskID);
					taskImport.setAccSystem(accSystem);
					taskImport.setNewID(newID);
					taskImport.setNewGlobalID(newGlobalID);
					taskImport.setWbsElementID(wbsElementID);
					taskImport.setWorkpackageMsId(workpackageMsId);
					taskImport.setReIPOwner(reIPOwner);
					
					// Calculate Closed
					final boolean closed = "yes".equalsIgnoreCase(strClosed) ? true : false;
					taskImport.setClosed(closed);

					// Calculate operation
					if (CREATION_OPER.equalsIgnoreCase(operation)) {
						taskImport.setModification(TaskImport.CREATION);
						// packageImport.setCanCreate(true);
					} else if (RENAME_OPER.equalsIgnoreCase(operation)) {
						taskImport.setModification(TaskImport.RENAME);
					} else if (CLOSE_OPER.equalsIgnoreCase(operation)) {
						taskImport.setModification(TaskImport.CLOSE);
					} else if (REOPEN_OPER.equalsIgnoreCase(operation)) {
						taskImport.setModification(TaskImport.REOPEN);
					} else if (CHANGE_ID_OPER.equalsIgnoreCase(operation)) {
						taskImport.setModification(TaskImport.CHANGE_ID);
					} else {
						// USER_LOG.warning(String.format("The type of change <<%s>> is not identified
						// in the list of standard actions at line number : %s", operation,
						// lineNumber));
						USER_LOG.warning(projectID, globalID, taskID, 8);
						continue;
					}

					// construct project
					ProjectImport projectImport = getProjectImport(projectID);
					if (projectImport == null) {
						projectImport = new ProjectImport();
						projectImport.setProjectID(projectID);
						this.addProjectImport(projectImport);
					}

					PackageImport packageImport = projectImport.getPackageImport(packageID);
					if (packageImport == null) {
						packageImport = new PackageImport();
						packageImport.setPackageID(packageID);
						projectImport.addPackageImport(packageImport);
					}

					// add a warning message if the task is listed several time
					if (packageImport.getTaskImport(taskID) != null) {
						// USER_LOG.warning(String.format("The task <<%s>> is listed several time in
						// <<%s>>.", taskID, sourceFile));
						USER_LOG.warning(projectID, globalID, taskID, 3);
					}

					packageImport.addTaskImport(taskImport);

				} else {
					// USER_LOG.warning(String.format("Bad line format at line number : %s",
					// lineNumber));
					USER_LOG.warning("", "", "", 11);
				}
				
			}
			
		} catch (IOException e1) {
			LOG.error(e1);
			throw new IOException("ERROR to loading file : " + this.sourceFile);
		} finally {
			
			if (ips != null) {
				ips.close();
			}
			
			if (buff != null) {
				buff.close();
			}
			
		}
		
		LOG.debug(this.projectImportList.size() + " projects found");
		return this.projectImportList;
	}

}
