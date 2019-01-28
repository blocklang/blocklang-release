package com.blocklang.git;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * git 测试用例
 * 
 * 实现和测试用例，参考 
 * 
 * https://dev.tencent.com/u/jinzw/p/doufuding/git/tree/master/src/main/java/com/doufuding/math/git
 * 
 * https://dev.tencent.com/u/jinzw/p/doufuding/git/blob/master/src/test/java/com/doufuding/math/git/GitUtilsTest.java
 * 
 * @author Zhengwei Jin
 */
public class GitUtilsTest {
	
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private String gitRepoDirectory = "gitRepo";
	private String gitUserName = "user";
	private String gitUserMail = "user@email.com";
	
	@Test
	public void is_git_repo_folder_not_exist() throws IOException {
		File folder = tempFolder.newFolder(gitRepoDirectory);
		assertThat(GitUtils.isGitRepo(folder.toPath().resolve("not-exist-folder")), is(false));
	}
	
	@Test
	public void is_git_repo_is_not_a_folder() throws IOException {
		File file = tempFolder.newFile(gitRepoDirectory);
		assertThat(GitUtils.isGitRepo(file.toPath()), is(false));
	}
	
	@Test
	public void git_init_success() throws IOException {
		File folder = tempFolder.newFolder(gitRepoDirectory);
		
		assertThat(GitUtils.isGitRepo(folder.toPath()), is(false));
		
		GitUtils.init(folder.toPath(), gitUserName, gitUserMail);
		
		assertThat(GitUtils.isGitRepo(folder.toPath()), is(true));
	}
	
	@Test
	public void git_commit_success() throws IOException {
		File folder = tempFolder.newFolder(gitRepoDirectory);
		GitUtils.init(folder.toPath(), gitUserName, gitUserMail);
		
		// 在初始化时有一次 commit
		assertThat(GitUtils.getLogCount(folder.toPath()), is(1));
		
		GitUtils.commit(folder.toPath(), "/a/b", "c.txt", "hello", "usera", "usera@email.com", "firstCommit");
		assertThat(GitUtils.getLogCount(folder.toPath()), is(2));
		assertContentEquals(folder.toPath().resolve("a").resolve("b").resolve("c.txt"), "hello");
		
		GitUtils.commit(folder.toPath(), "/a/b", "c.txt", "hello world", "usera", "usera@email.com", "secondCommit");
		assertThat(GitUtils.getLogCount(folder.toPath()), is(3));
		assertContentEquals(folder.toPath().resolve("a").resolve("b").resolve("c.txt"), "hello world");
	}
	
	@Test
	public void git_tag_success() throws IOException {
		// 新建一个 git 仓库
		File folder = tempFolder.newFolder(gitRepoDirectory);
		String commitId = GitUtils.init(folder.toPath(), gitUserName, gitUserMail);
		System.out.println(commitId);
		
		// 断言仓库的标签数
		assertThat(GitUtils.getTagCount(folder.toPath()), is(0));
		// 为 git 仓库打标签
		GitUtils.tag(folder.toPath(), "v0.1.0", "message");
		// 断言仓库的标签数
		assertThat(GitUtils.getTagCount(folder.toPath()), is(1));
	}

	private void assertContentEquals(Path filePath, String content) throws IOException{
		assertThat(Files.readString(filePath), equalTo(content));
	}

}