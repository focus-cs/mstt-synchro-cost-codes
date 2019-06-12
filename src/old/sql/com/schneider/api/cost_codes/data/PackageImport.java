package old.sql.com.schneider.api.cost_codes.data;

import java.util.ArrayList;
import java.util.List;

public class PackageImport {

	private String packageID;

	private final transient List<TaskImport> taskImportList;

	private transient boolean canCreate;

	private ProjectImport parentProject;

	public PackageImport() {
		this.taskImportList = new ArrayList<TaskImport>();
		this.canCreate = false;
	}

	public void addTaskImport(final TaskImport taskImport) {
		this.taskImportList.add(taskImport);
		taskImport.setParentPackage(this);
		this.canCreate = taskImport.getModification() == TaskImport.CREATION ? true : false;
	}

	/**
	 * @return the taskImportList
	 */
	public List<TaskImport> getTaskImportList() {
		return this.taskImportList;
	}

	public TaskImport getTaskImport(final String taskID) {
		TaskImport result = null;

		for (TaskImport taskImport : this.taskImportList) {
			
			if (taskID.equals(taskImport.getTaskID())) {
				result = taskImport;
				break;
			}
			
		}

		return result;
	}

	/**
	 * @return the packageID
	 */
	public String getPackageID() {
		return this.packageID;
	}

	/**
	 * @param packageID the packageID to set
	 */
	public void setPackageID(final String packageID) {
		this.packageID = packageID;
	}

	/**
	 * @return the parentProject
	 */
	public ProjectImport getParentProject() {
		return this.parentProject;
	}

	/**
	 * @param parentProject the parentProject to set
	 */
	public void setParentProject(final ProjectImport parentProject) {
		this.parentProject = parentProject;
	}

	/**
	 * @return the canCreate
	 */
	public boolean isCanCreate() {
		return this.canCreate;
	}

	/**
	 * @param canCreate the canCreate to set
	 */
	public void setCanCreate(final boolean canCreate) {
		this.canCreate = canCreate;
	}

}
