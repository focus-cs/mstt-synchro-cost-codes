package com.schneider.api.cost_codes.data;

import java.util.ArrayList;
import java.util.List;

public class ProjectImport {

	private String projectID;

	private final transient List<PackageImport> packageImportList;

	public ProjectImport() {
		this.packageImportList = new ArrayList<PackageImport>();
	}

	public void addPackageImport(final PackageImport packageImport) {
		this.packageImportList.add(packageImport);
		packageImport.setParentProject(this);
	}

	public PackageImport getPackageImport(final String packageID) {
		PackageImport result = null;

		for (PackageImport packageImport : this.packageImportList) {
			
			if (packageID.equals(packageImport.getPackageID())) {
				result = packageImport;
				break;
			}
			
		}
		
		return result;
	}

	public List<PackageImport> getPackageImportList() {
		return this.packageImportList;
	}

	public String getProjectID() {
		return this.projectID;
	}

	public void setProjectID(final String projectID) {
		this.projectID = projectID;
	}
	
}
