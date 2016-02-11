package com.neoteric.jenkins

import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.assertThat
import groovy.mock.interceptor.MockFor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test
import org.junit.contrib.java.lang.system.StandardOutputStreamLog

import com.neoteric.jenkins.JenkinsJobManager;
import com.neoteric.jenkins.TemplateJob;

class JenkinsJobManagerTests {

	@Rule
	public final StandardOutputStreamLog log = new StandardOutputStreamLog();
	
	final shouldFail = new GroovyTestCase().&shouldFail

	@Test
	public void testFindTemplateJobs() {
		JenkinsJobManager jenkinsJobManager =
				new JenkinsJobManager(templateJob: "foo-develop", jobPrefix: "myproj", jenkinsUrl: "http://dummy.com", gitUrl: "git@dummy.com:company/myproj.git")

		List<String> allJobNames = [
			"myproj-foo-master",
			"otherproj-foo-master",
			"foo-develop"
		]
		List<TemplateJob> templateJobs = jenkinsJobManager.findRequiredTemplateJobs(allJobNames)
		assert templateJobs.size() == 3
		TemplateJob templateJob = templateJobs.first()
		assert templateJob.jobName == "foo-develop"
		assert templateJob.baseJobName == "foo"
		assert templateJob.templateBranchName == "feature"
	}

	@Test
	public void testFindTemplateJobsSimpleBranchModel() {
		JenkinsJobManager jenkinsJobManager =
				new JenkinsJobManager(templateJob: "foo-next", jobPrefix: "myproj", jenkinsUrl: "http://dummy.com", gitUrl: "git@dummy.com:company/myproj.git", branchModel: "simple")

		List<String> allJobNames = [
				"myproj-foo-master",
				"otherproj-foo-master",
				"foo-next"
		]
		List<TemplateJob> templateJobs = jenkinsJobManager.findRequiredTemplateJobs(allJobNames)
		assert templateJobs.size() == 2
		TemplateJob templateJob = templateJobs.first()
		assert templateJob.jobName == "foo-next"
		assert templateJob.baseJobName == "foo"
		assert templateJob.templateBranchName == "feature"
	}

	@Test
	public void testGetTemplateJobs() {
		JenkinsJobManager jenkinsJobManager = new JenkinsJobManager(jobPrefix: "NeoDocs", templateJob: "NeoDocs-build-develop", gitUrl: "git@dummy.com:company/myproj.git", jenkinsUrl: "http://dummy.com")

		List<String> allJobNames = [
			"NeoDocs-build-develop",
			"NeoDocs-build",
			"NeoDocs-deploy-develop",
			"NeoDocs-build-hotfix"
		]
		List<TemplateJob> templateJobs = [
			new TemplateJob(jobName: "NeoDocs-build-develop", baseJobName: "NeoDocs-build", templateBranchName: "feature", jobCategory: "feature"),
			new TemplateJob(jobName: "NeoDocs-build-develop", baseJobName: "NeoDocs-build", templateBranchName: "release", jobCategory: "release"),
			new TemplateJob(jobName: "NeoDocs-build-develop", baseJobName: "NeoDocs-build", templateBranchName: "hotfix", jobCategory: "hotfix")
		]

		assert templateJobs == jenkinsJobManager.findRequiredTemplateJobs(allJobNames)
	}

	@Test
	public void testGetTemplateJobsSimpleBranchModel() {
		JenkinsJobManager jenkinsJobManager = new JenkinsJobManager(jobPrefix: "NeoDocs", templateJob: "NeoDocs-build-next", gitUrl: "git@dummy.com:company/myproj.git", jenkinsUrl: "http://dummy.com", branchModel: "simple")

		List<String> allJobNames = [
				"NeoDocs-build-next",
				"NeoDocs-build",
				"NeoDocs-deploy-next",
				"NeoDocs-build-hotfix"
		]
		List<TemplateJob> templateJobs = [
				new TemplateJob(jobName: "NeoDocs-build-next", baseJobName: "NeoDocs-build", templateBranchName: "feature", jobCategory: "feature"),
				new TemplateJob(jobName: "NeoDocs-build-next", baseJobName: "NeoDocs-build", templateBranchName: "release", jobCategory: "release"),
		]

		assert templateJobs == jenkinsJobManager.findRequiredTemplateJobs(allJobNames)
	}

	@Test
	public void testSync() {

		List<TemplateJob> templateJobs = [
			new TemplateJob(jobName: "NeoDocs-build-develop", baseJobName: "NeoDocs-build", templateBranchName: "feature"),
			new TemplateJob(jobName: "NeoDocs-deploy-develop", baseJobName: "NeoDocs-deploy", templateBranchName: "feature"),
			new TemplateJob(jobName: "NeoDocs-build-develop", baseJobName: "NeoDocs-build", templateBranchName: "hotfix")
		]

		List<String> jobNames = [
			"NeoDocs-build-feature-test1",
			// add missing deploy test1
			"NeoDocs-deploy-feature-test2",
			// add missing build test2
			"NeoDocs-deploy-feature-test3",
			// to delete
			"NeoDocs-build-hotfix-emergency",
			// do nothing - already there
			"NeoDocs-build-release" // do nothing - no template avail
		]

		List<String> branchNames = [
			"feature-test1",
			"feature-test2",
			"master",
			"release-1.0.0",
			"hotfix-emergency"
		]
		JenkinsJobManager jenkinsJobManager = new JenkinsJobManager(jobPrefix: "NeoDocs", templateJob: "NeoDocs-build-develop", gitUrl: "git@dummy.com:company/myproj.git", jenkinsUrl: "http://dummy.com")

		jenkinsJobManager.jenkinsApi = new JenkinsApiMocked()
		jenkinsJobManager.syncJobs(branchNames ,jobNames, templateJobs)

		assertThat(log.getLog().substring(log.getLog().indexOf("Summary"))).containsSequence( 
			"Creating", "NeoDocs-deploy-feature-test1 from NeoDocs-deploy-develop",
						"NeoDocs-build-feature-test2 from NeoDocs-build-develop",
			"Deleting", "NeoDocs-deploy-feature-test3")
		}

	@Test
	public void testSyncSimpleModel() {

		List<TemplateJob> templateJobsBuild = [
				new TemplateJob(jobName: "NeoDocs-build-next", baseJobName: "NeoDocs-build", templateBranchName: "feature"),
				new TemplateJob(jobName: "NeoDocs-build-next", baseJobName: "NeoDocs-build", templateBranchName: "release")
		]
        List<TemplateJob> templateJobsDeploy = [
                new TemplateJob(jobName: "NeoDocs-deploy-next", baseJobName: "NeoDocs-deploy", templateBranchName: "feature"),
                new TemplateJob(jobName: "NeoDocs-deploy-next", baseJobName: "NeoDocs-deploy", templateBranchName: "release"),
        ]

		List<String> jobNames = [
				"NeoDocs-build-topic_PROJ-1234",
				// to delete
				"NeoDocs-build-topic_PROJ-1235",
                // to delete
				"NeoDocs-build-maint_1.0.1",
                // do nothing - already there
                "NeoDocs-build-maint_1.0.2",
				"NeoDocs-build-maint_1.0.0"
		]

		List<String> branchNames = [
				"topic/PROJ-1234_shortDescription",
				"topic/PROJ-1236_shortDescription2",
				"master",
				"maint/1.0.0",
				"maint/1.0.2"
		]
		JenkinsJobManager jenkinsJobManager = new JenkinsJobManager(jobPrefix: "NeoDocs", templateJob: "NeoDocs-build-develop", gitUrl: "git@dummy.com:company/myproj.git", featureSuffix: "topic/", releaseSuffix: "maint/", jenkinsUrl: "http://dummy.com", branchModel: "simple")

		jenkinsJobManager.jenkinsApi = new JenkinsApiMocked()
		jenkinsJobManager.syncJobs(branchNames ,jobNames, templateJobsBuild)

		assertThat(log.getLog().substring(log.getLog().indexOf("Summary"))).containsSequence(
				"Creating", "NeoDocs-build-topic_PROJ-1236 from NeoDocs-build-next",
				"Deleting", "NeoDocs-build-topic_PROJ-1235")
        //jenkinsJobManager.syncJobs(branchNames ,jobNames, templateJobsDeploy)
	}

	class JenkinsApiMocked extends JenkinsApi {
		
		@Override
		public void cloneJobForBranch(String jobPrefix, ConcreteJob missingJob, String createJobInView, String gitUrl, Boolean noFeatureDeploy, String branchModel) {
		}
		
		@Override
		public void deleteJob(String jobName) {
		}
		
		@Override
		public void startJob(ConcreteJob job) {
		}
	}
	
}
