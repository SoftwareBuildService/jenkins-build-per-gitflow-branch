package com.neoteric.jenkins

import groovy.transform.EqualsAndHashCode;
import groovy.transform.ToString;

@ToString
@EqualsAndHashCode
class TemplateJob {
    String jobName
    String baseJobName
    String templateBranchName
	String jobCategory

    String jobNameForBranch(String branchName) {
        // git branches often have a forward slash in them, but they make jenkins cranky, turn it into an underscore
        String safeBranchName=""
        if(branchName.lastIndexOf("_") != -1) {
            safeBranchName = branchName.substring(0, branchName.lastIndexOf("_")).replaceAll('/', '_')
        }else{
            safeBranchName = branchName.replaceAll('/', '_')
        }
        return "$baseJobName-$safeBranchName"
    }
	
	String addFolderPath(Boolean jobInFolder, String jobPrefix){
		if (jobInFolder){
			return "${jobPrefix}/job"
		} else {
			return ""
		}
	}
    
    ConcreteJob concreteJobForBranch(String jobPrefix, String branchName, String featureName="", Boolean jobInFolder) {
        ConcreteJob concreteJob = new ConcreteJob(templateJob: this, branchName: branchName, jobName: jobNameForBranch(branchName), featureName: featureName, folderPath: addFolderPath(jobInFolder, jobPrefix) )
    }
}
