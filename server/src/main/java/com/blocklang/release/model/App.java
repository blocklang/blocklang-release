package com.blocklang.release.model;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class App extends PartialOperateFields{

	private static final long serialVersionUID = 8193183175716557496L;
	
	@Column(name = "project_id")
	private Integer projectId;
	
	@Column(name = "app_name", nullable = false, unique = true, length = 32)
	private String appName;
	
	@Column(name = "registration_token", unique = true, length = 22)
	private String registrationToken;

	public Integer getProjectId() {
		return projectId;
	}

	public void setProjectId(Integer projectId) {
		this.projectId = projectId;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getRegistrationToken() {
		return registrationToken;
	}

	public void setRegistrationToken(String registrationToken) {
		this.registrationToken = registrationToken;
	}

}