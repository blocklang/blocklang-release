package com.blocklang.release.model;

import javax.persistence.Column;
import javax.persistence.Entity;

import com.blocklang.core.model.PartialOperateFields;

@Entity
public class App extends PartialOperateFields{

	private static final long serialVersionUID = 8193183175716557496L;
	
	// 如果是上传的 APP，则 projectId 就可能为空
	@Column(name = "project_id", unique = true)
	private Integer projectId;
	
	@Column(name = "app_name", nullable = false, unique = true, length = 32)
	private String appName;

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

}
