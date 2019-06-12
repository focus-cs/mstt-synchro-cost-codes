package old.sql.com.schneider.api.cost_codes.data;

public class TaskImport {
	
	public final static int CREATION = 0;

	public final static int RENAME = 1;

	public final static int CLOSE = 2;

	public final static int REOPEN = 3;

	public final static int CHANGE_ID = 4;

	private String taskID;

	private String globalID;

	private String name;

	private String accSystem;

	private boolean closed;

	private int modification = -1;

	private PackageImport parentPackage;

	private String newID;

	private String newGlobalID;

	private String wbsElementID;

	private String workpackageMsId;

	private String reIPOwner;

	/**
	 * @return the reIPOwner
	 */
	public String getReIPOwner() {
		return this.reIPOwner;
	}

	/**
	 * @param reIPOwner the reIPOwner to set
	 */
	public void setReIPOwner(String reIPOwner) {
		this.reIPOwner = reIPOwner;
	}

	/**
	 * @return the taskID
	 */
	public String getTaskID() {
		return this.taskID;
	}

	/**
	 * @param taskID the taskID to set
	 */
	public void setTaskID(final String taskID) {
		this.taskID = taskID;
	}

	/**
	 * @return the globalID
	 */
	public String getGlobalID() {
		return this.globalID;
	}

	/**
	 * @param globalID the globalID to set
	 */
	public void setGlobalID(final String globalID) {
		this.globalID = globalID;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * @return the accSystem
	 */
	public String getAccSystem() {
		return this.accSystem;
	}

	/**
	 * @param accSystem the accSystem to set
	 */
	public void setAccSystem(final String accSystem) {
		this.accSystem = accSystem;
	}

	/**
	 * @return the closed
	 */
	public boolean isClosed() {
		return this.closed;
	}

	/**
	 * @param closed the closed to set
	 */
	public void setClosed(final boolean closed) {
		this.closed = closed;
	}

	/**
	 * @return the modification
	 */
	public int getModification() {
		return this.modification;
	}

	/**
	 * @param modification the modification to set
	 */
	public void setModification(final int modification) {
		this.modification = modification;
	}

	/**
	 * @return the parentPackage
	 */
	public PackageImport getParentPackage() {
		return this.parentPackage;
	}

	/**
	 * @param parentPackage the parentPackage to set
	 */
	public void setParentPackage(final PackageImport parentPackage) {
		this.parentPackage = parentPackage;
	}

	public String getNewID() {
		return this.newID;
	}

	public void setNewID(String newID) {
		this.newID = newID;
	}

	public String getNewGlobalID() {
		return this.newGlobalID;
	}

	public void setNewGlobalID(String newGlobalID) {
		this.newGlobalID = newGlobalID;
	}

	public String getWbsElementID() {
		return this.wbsElementID;
	}

	public void setWbsElementID(String wbsElementID) {
		this.wbsElementID = wbsElementID;
	}

	public String getWorkpackageMsId() {
		return this.workpackageMsId;
	}

	public void setWorkpackageMsId(String workpackageMsId) {
		this.workpackageMsId = workpackageMsId;
	}

}
