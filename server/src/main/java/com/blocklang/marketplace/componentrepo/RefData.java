package com.blocklang.marketplace.componentrepo;

import com.blocklang.marketplace.data.RepoConfigJson;

public class RefData {
	private String gitUrl;
	private String fullRefName;
	private String shortRefName;
	private RepoConfigJson repoConfig;
	private Integer createUserId;
	private boolean invalidData = false;

	public String getGitUrl() {
		return gitUrl;
	}

	public void setGitUrl(String gitUrl) {
		this.gitUrl = gitUrl;
	}

	public String getFullRefName() {
		return fullRefName;
	}

	public void setFullRefName(String fullRefName) {
		this.fullRefName = fullRefName;
	}

	public String getShortRefName() {
		return shortRefName;
	}

	public void setShortRefName(String shortRefName) {
		this.shortRefName = shortRefName;
	}

	public RepoConfigJson getRepoConfig() {
		return repoConfig;
	}

	public void setRepoConfig(RepoConfigJson repoConfig) {
		this.repoConfig = repoConfig;
	}

	public Integer getCreateUserId() {
		return createUserId;
	}

	public void setCreateUserId(Integer createUserId) {
		this.createUserId = createUserId;
	}

	public void readFailed() {
		this.invalidData = true;
	}

	public boolean isInvalidData() {
		return invalidData;
	}
	
}
