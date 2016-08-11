package com.neoteric.jenkins

import java.util.regex.Pattern

class JenkinsJobManager {

	String templateJob
	String jobPrefix
	String gitUrl
	String jenkinsUrl
	String createJobInView
	String jenkinsUser
	String jenkinsPassword

	Boolean dryRun = false
	Boolean noDelete = false
	Boolean noFeatureDeploy = false
	Boolean mergeBeforeBuild = false
	
	String featureSuffix = "feature-"
	String hotfixSuffix = "hotfix-"
	String releaseSuffix = "release-"

	String templateFeatureSuffix = "feature"
	String templateHotfixSuffix = "hotfix"
	String templateReleaseSuffix = "release"

	String branchModel = "default"
	String developmentBranch = "develop"
	
	String releaseProperty=""

	String featureSonarProperties = ""
	String releaseSonarProperties = ""
	String hotfixSonarProperties = ""
	
	def branchSuffixMatch = []
	JenkinsApi jenkinsApi
	GitApi gitApi

	JenkinsJobManager(Map props) {
		for (property in props) {
			this."${property.key}" = property.value
		}
		this.branchSuffixMatch = [(templateFeatureSuffix): featureSuffix,
								 (templateHotfixSuffix) : hotfixSuffix,
								 (templateReleaseSuffix): releaseSuffix]
		initJenkinsApi()
		initGitApi()
	}

	void syncWithRepo() {
		List<String> allBranchNames = gitApi.branchNames
		println "-------------------------------------"
		println "All branch names:" + allBranchNames

		List<String> allJobNames = jenkinsApi.getJobNames(jobPrefix)
		println "-------------------------------------"
		println "All job names with prefix "+jobPrefix+":" + allJobNames

		List<TemplateJob> templateJobs = findRequiredTemplateJobs(allJobNames)
		println "-------------------------------------"
		println "Template Job:" + templateJobs

		List<String> jobsWithJobPrefix = allJobNames.findAll { jobName ->
			jobName.startsWith(jobPrefix + '-')
		}
		println "-------------------------------------"
		println "Jobs with provided prefix (${jobPrefix}):" + jobsWithJobPrefix

		// create any missing template jobs and delete any jobs matching the template patterns that no longer have branches
		syncJobs(allBranchNames, jobsWithJobPrefix, templateJobs)

	}

	public List<TemplateJob> findRequiredTemplateJobs(List<String> allJobNames) {
		TemplateJob templateJobRelease
		TemplateJob templateJobHotfix
		TemplateJob templateJobFeature =  templateJobRelease = templateJobHotfix = null

        String baseJobName = templateJob.replace("-"+developmentBranch, "")
  		templateJobFeature = new TemplateJob(jobName: templateJob, baseJobName: baseJobName, templateBranchName: templateFeatureSuffix, jobCategory: "feature")
    	templateJobRelease = new TemplateJob(jobName: templateJob, baseJobName: baseJobName, templateBranchName: templateReleaseSuffix, jobCategory: "release")
		templateJobHotfix = new TemplateJob(jobName: templateJob, baseJobName: baseJobName, templateBranchName: templateHotfixSuffix, jobCategory: "hotfix")
		if(this.branchModel == "default"){
			return [ templateJobFeature, templateJobRelease, templateJobHotfix ] 
		}else if (this.branchModel == "simple"){
			return [ templateJobFeature, templateJobRelease ]
		}
		[] 
	}

	public void syncJobs(List<String> allBranchNames, List<String> jobNames, List<TemplateJob> templateJobs) {

		def templateJobsByBranch = templateJobs.groupBy({ template -> template.templateBranchName })

		List<ConcreteJob> missingJobs = [];
		List<String> jobsToDelete = [];

		templateJobsByBranch.keySet().each { templateBranchToProcess ->
			println "-> Checking $templateBranchToProcess branches"
			List<String> branchesWithCorrespondingTemplate = allBranchNames.findAll { branchName ->
				branchName.startsWith(branchSuffixMatch[templateBranchToProcess])
			}

			println "---> Founded corresponding branches: $branchesWithCorrespondingTemplate"
			branchesWithCorrespondingTemplate.each { branchToProcess ->
				println "-----> Processing branch: $branchToProcess"
				List<ConcreteJob> expectedJobsPerBranch = templateJobsByBranch[templateBranchToProcess].collect { TemplateJob templateJob ->
					templateJob.concreteJobForBranch(jobPrefix, branchToProcess, branchToProcess.replaceAll(branchSuffixMatch[templateBranchToProcess], ""))
				}
				println "-------> Expected jobs:"
				expectedJobsPerBranch.each { println "           $it" }
				List<String> jobNamesPerBranch = jobNames.findAll{ it.endsWith(branchToProcess.replaceAll('/', '_')) }
				println "-------> Job Names per branch:"
				jobNamesPerBranch.each { println "           $it" }
				List<ConcreteJob> missingJobsPerBranch = expectedJobsPerBranch.findAll { expectedJob ->
					!jobNamesPerBranch.any {it.contains(expectedJob.jobName) }
				}
				println "-------> Missing jobs:"
				missingJobsPerBranch.each { println "           $it" }
				missingJobs.addAll(missingJobsPerBranch)
			}
			
			
			List<String> deleteCandidates = jobNames.findAll {  it.contains(branchSuffixMatch[templateBranchToProcess]) || it.contains(branchSuffixMatch[templateBranchToProcess].replaceAll("/","_")) }
			List<String> jobsToDeletePerBranch = deleteCandidates.findAll { candidate ->
				!branchesWithCorrespondingTemplate.any { candidate.endsWith(it.replaceAll("/","_")) }
			}

			println "-----> Jobs to delete:"
			jobsToDeletePerBranch.each { println "         $it" }
			jobsToDelete.addAll(jobsToDeletePerBranch)
		}
		
		println "\nSummary:\n---------------"
		if (missingJobs) {
			for(ConcreteJob missingJob in missingJobs) {
				println "Creating missing job: ${missingJob.jobName} from ${missingJob.templateJob.jobName}"
				jenkinsApi.cloneJobForBranch(jobPrefix, missingJob, createJobInView, gitUrl, noFeatureDeploy, branchModel, mergeBeforeBuild, releaseProperty, featureSonarProperties, releaseSonarProperties, hotfixSonarProperties)
				jenkinsApi.startJob(missingJob)
			}
		}
		
		if (!noDelete && jobsToDelete) {
			println "Deleting deprecated jobs:\n\t${jobsToDelete.join('\n\t')}"
			jobsToDelete.each { String jobName ->
				jenkinsApi.deleteJob(jobName)
			}
		}
	}
	
	JenkinsApi initJenkinsApi() {
		if (!jenkinsApi) {
			assert jenkinsUrl != null
			if (dryRun) {
				println "DRY RUN! Not executing any POST commands to Jenkins, only GET commands"
				this.jenkinsApi = new JenkinsApiReadOnly(jenkinsServerUrl: jenkinsUrl)
			} else {
				this.jenkinsApi = new JenkinsApi(jenkinsServerUrl: jenkinsUrl)
			}

			if (jenkinsUser || jenkinsPassword) this.jenkinsApi.addBasicAuth(jenkinsUser, jenkinsPassword)
		}

		return this.jenkinsApi
	}

	GitApi initGitApi() {
		this.gitApi = new GitApi(gitUrl: gitUrl)
		return this.gitApi
	}
}
