package com.blocklang.marketplace.task;

import java.nio.file.Path;
import java.util.Optional;

import com.blocklang.core.git.GitUtils;
import com.blocklang.core.git.exception.GitCloneFailedException;
import com.blocklang.core.git.exception.GitPullFailedException;
import com.blocklang.core.git.exception.GitRepoNotFoundException;

public class GitSyncApiRepoTask extends AbstractRepoPublishTask{

	private LocalRepoInfo localRepoInfo;
	
	public GitSyncApiRepoTask(MarketplacePublishContext marketplacePublishContext) {
		super(marketplacePublishContext);
		this.localRepoInfo = marketplacePublishContext.getApiRepo();
	}

	@Override
	public Optional<Boolean> run() {
		try {
			Path path = localRepoInfo.getRepoSourceDirectory();
			if(GitUtils.isGitRepo(path)) {
				logger.info("从 {0} 仓库拉取最新的 API 仓库源码", localRepoInfo.getGitUrl());
				GitUtils.pull(path);
			} else {
				logger.info("从 {0} 仓库克隆 API 仓库源码", localRepoInfo.getGitUrl());
				GitUtils.clone(localRepoInfo.getGitUrl(), path);
			}
			return Optional.of(true);
		} catch (GitRepoNotFoundException | GitPullFailedException | GitCloneFailedException e) {
			logger.error(e);
			return Optional.empty();
		}
	}
}