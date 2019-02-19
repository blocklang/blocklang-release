package com.blocklang.release.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.blocklang.core.constant.CmPropKey;
import com.blocklang.core.dao.UserDao;
import com.blocklang.core.git.GitUtils;
import com.blocklang.core.model.UserInfo;
import com.blocklang.core.service.PropertyService;
import com.blocklang.core.test.AbstractServiceTest;
import com.blocklang.develop.dao.ProjectDao;
import com.blocklang.develop.model.Project;
import com.blocklang.develop.model.ProjectContext;
import com.blocklang.develop.service.ProjectService;
import com.blocklang.release.dao.AppDao;

public class ProjectServiceImplTest extends AbstractServiceTest{
	
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	
	@Autowired
	private ProjectService projectService;
	
	@Autowired
	private ProjectDao projectDao;
	
	@Autowired
	private UserDao userDao;
	
	@Autowired
	private AppDao appDao;
	
	@MockBean
	private PropertyService propertyService;
	
	@Test
	public void find_no_data() {
		Optional<Project> projectOption = projectService.find("not-exist-owner", "not-exist-name");
		assertThat(projectOption).isEmpty();
	}
	
	@Test
	public void find_success() {
		UserInfo userInfo = new UserInfo();
		userInfo.setLoginName("user_name");
		userInfo.setAvatarUrl("avatar_url");
		userInfo.setEmail("email");
		userInfo.setMobile("mobile");
		userInfo.setCreateTime(LocalDateTime.now());
		Integer userId = userDao.save(userInfo).getId();
		
		Project project = new Project();
		project.setName("project_name");
		project.setIsPublic(true);
		project.setLastActiveTime(LocalDateTime.now());
		project.setCreateUserId(userId);
		project.setCreateTime(LocalDateTime.now());
		
		projectDao.save(project);
		
		Optional<Project> projectOption = projectService.find("user_name", "project_name");
		assertThat(projectOption).isPresent();
	}
	
	@Test
	public void create_success() throws IOException {
		UserInfo userInfo = new UserInfo();
		userInfo.setLoginName("user_name");
		userInfo.setAvatarUrl("avatar_url");
		userInfo.setEmail("email");
		userInfo.setMobile("mobile");
		userInfo.setCreateTime(LocalDateTime.now());
		Integer userId = userDao.save(userInfo).getId();
		
		Project project = new Project();
		project.setName("project_name");
		project.setIsPublic(true);
		project.setDescription("description");
		project.setLastActiveTime(LocalDateTime.now());
		project.setCreateUserId(userId);
		project.setCreateTime(LocalDateTime.now());
		project.setCreateUserName("user_name");
		
		File rootFolder = tempFolder.newFolder();
		when(propertyService.findStringValue(CmPropKey.BLOCKLANG_ROOT_PATH)).thenReturn(Optional.of(rootFolder.getPath()));
		
		Integer projectId = projectService.create(userInfo, project).getId();
		
		// 断言
		// 项目基本信息已保存
		assertThat(projectDao.findById(projectId).get()).hasNoNullFieldsOrPropertiesExcept("lastUpdateUserId", "lastUpdateTime", "avatarUrl");
		
		// APP 基本信息已保存
		assertThat(appDao.findByProjectId(projectId).get()).hasNoNullFieldsOrPropertiesExcept("lastUpdateUserId", "lastUpdateTime");
		
		// git 仓库已创建
		// 测试时将 projectsRootPath 导向到 junit 的临时文件夹
		ProjectContext context = new ProjectContext("user_name", "project_name", rootFolder.getPath());
		assertThat(GitUtils.isGitRepo(context.getGitRepositoryDirectory())).isTrue();
	}
	
}
