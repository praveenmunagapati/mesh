package com.gentics.mesh.core.rest.project;

import com.gentics.mesh.core.rest.common.AbstractRestModel;


public class ProjectUpdateRequest extends AbstractRestModel {

	private String name;

	public ProjectUpdateRequest() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
