package old.sql.com.schneider.api.cost_codes.business;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sciforma.psnext.api.Project;
import com.sciforma.psnext.api.Session;

public class PSNextManager {

	private static final Logger LOG = Logger.getLogger(PSNextManager.class);

	private final transient Session session;

	private final transient Map<String, List<Project>> projectsByVersion;

	/**
	 * @param session
	 */
	public PSNextManager(final Session session) {
		this.session = session;
		this.projectsByVersion = new HashMap<String, List<Project>>();
	}

	/**
	 * 
	 * @param projectId
	 * @return A project
	 */
	@SuppressWarnings("unchecked")
	public Project getProjectById(final String projectId) {
		Project proj = null;
		List<Project> projectList = this.projectsByVersion.get(String.valueOf(Project.VERSION_ALL));

		// Skeep load project if the list is not null
		if (projectList == null) {
			
			try {
				projectList = this.session.getProjectList(Project.VERSION_WORKING, Project.READWRITE_ACCESS);
				this.projectsByVersion.put(String.valueOf(Project.VERSION_ALL), projectList);
			} catch (Exception e) {
				LOG.error(e);
				e.printStackTrace();
			}
			
		}

		if (projectList != null) {
			// find project by id filed
			for (Project currentProject : projectList) {
				
				try {
					final String projectID = currentProject.getStringField("ID");
					
					if (projectID.equals(projectId)) {
						proj = currentProject;
						break;
					}

				} catch (Exception e) {
					LOG.error(e);
					e.printStackTrace();
					continue;
				}
				
			}
			
		}
		
		return proj;
	}

	/**
	 * 
	 * @param projectId
	 * @param version
	 * @return A project
	 */
	@SuppressWarnings("unchecked")
	public Project getProjectById(final String projectId, final int version) {
		Project proj = null;
		List<Project> projectList = this.projectsByVersion.get(String.valueOf(version));

		// Skeep load project if the list is not null
		if (projectList == null) {
			
			try {
				projectList = this.session.getProjectList(version, Project.ADMIN_ACCESS);
				this.projectsByVersion.put(String.valueOf(version), projectList);
			} catch (Exception e) {
				LOG.error(e);
				e.printStackTrace();
			}
			
		}

		if (projectList != null) {
			// find project by id filed
			for (Project currentProject : projectList) {
				
				try {
					final String projectID = currentProject.getStringField("ID");
					
					if (projectID.equals(projectId)) {
						proj = currentProject;
						break;
					}

				} catch (Exception e) {
					LOG.error(e);
					e.printStackTrace();
					continue;
				}
				
			}
			
		}
		
		return proj;
	}
	
}
