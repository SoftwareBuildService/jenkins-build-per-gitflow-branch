package com.neoteric.jenkins

import static org.assertj.core.api.Assertions.assertThat

import org.apache.http.conn.HttpHostConnectException
import static org.joox.JOOX.*;
import org.junit.Before;
import org.junit.Test
import org.w3c.dom.Document

import groovy.mock.interceptor.MockFor

import org.apache.http.client.HttpResponseException

import com.neoteric.jenkins.ConcreteJob;
import com.neoteric.jenkins.JenkinsApi;
import com.neoteric.jenkins.TemplateJob;

import groovyx.net.http.RESTClient
import net.sf.json.JSON
import net.sf.json.JSONObject

class JenkinsApiGradleJobTests {
	
	final shouldFail = new GroovyTestCase().&shouldFail


	@Test
	public void shouldChangeConfigBranchName() {
		JenkinsApi api = new JenkinsApi(jenkinsServerUrl: "http://localhost:9090/jenkins")
		def result = api.processConfig(CONFIG_GRADLE_JOB, "release-1.0.0", "newGitUrl");
		assertThat(result).contains("<name>*/release-1.0.0</name>")
	}

	@Test
	public void shouldChangeSonarBranchName() {
		JenkinsApi api = new JenkinsApi(jenkinsServerUrl: "http://localhost:9090/jenkins")
		def result = api.processConfig(CONFIG_GRADLE_JOB, "release-1.0.0", "newGitUrl", "1.0.0", "release");
		assertThat(result).contains("<branch>release-1.0.0</branch>")
	}
	
	@Test
	public void shouldNotThrowExceptionWhenNoSonarConfig() {
		JenkinsApi api = new JenkinsApi(jenkinsServerUrl: "http://localhost:9090/jenkins")
		def result = api.processConfig(CONFIG_GRADLE_JOB_NO_SONAR, "release-1.0.0", "newGitUrl");
	}
	
	public void withJsonResponse(Map toJson, Closure closure) {
		JSON json = toJson as JSONObject
		MockFor mockRESTClient = new MockFor(RESTClient)
		mockRESTClient.demand.get { Map<String, ?> args ->
			return [data: json]
		}

		mockRESTClient.use { closure() }
	}
	
	static final String CONFIG_GRADLE_JOB='''
<project>
  <actions/>
  <description></description>
  <logRotator class="hudson.tasks.LogRotator">
    <daysToKeep>-1</daysToKeep>
    <numToKeep>50</numToKeep>
    <artifactDaysToKeep>-1</artifactDaysToKeep>
    <artifactNumToKeep>-1</artifactNumToKeep>
  </logRotator>
  <keepDependencies>false</keepDependencies>
  <properties>
  </properties>
  <scm class="hudson.plugins.git.GitSCM" plugin="git@2.4.1">
    <configVersion>2</configVersion>
    <userRemoteConfigs>
      <hudson.plugins.git.UserRemoteConfig>
        <url>https://github.com/SoftwareBuildService/jenkins-build-per-gitflow-branch.git</url>
      </hudson.plugins.git.UserRemoteConfig>
    </userRemoteConfigs>
    <branches>
      <hudson.plugins.git.BranchSpec>
        <name>*/next</name>
      </hudson.plugins.git.BranchSpec>
    </branches>
    <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
    <submoduleCfg class="list"/>
    <extensions>
      <hudson.plugins.git.extensions.impl.WipeWorkspace/>
    </extensions>
  </scm>
  <canRoam>true</canRoam>
  <disabled>false</disabled>
  <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>
  <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>
  <jdk>(Default)</jdk>
  <triggers>
    <hudson.triggers.SCMTrigger>
      <spec>H/5 * * * *</spec>
      <ignorePostCommitHooks>false</ignorePostCommitHooks>
    </hudson.triggers.SCMTrigger>
  </triggers>
  <concurrentBuild>false</concurrentBuild>
  <builders>
    <hudson.plugins.gradle.Gradle plugin="gradle@1.24">
      <description></description>
      <switches></switches>
      <tasks>clean test</tasks>
      <rootBuildScriptDir></rootBuildScriptDir>
      <buildFile></buildFile>
      <gradleName>gradle-2.6</gradleName>
      <useWrapper>false</useWrapper>
      <makeExecutable>false</makeExecutable>
      <fromRootBuildScriptDir>true</fromRootBuildScriptDir>
      <useWorkspaceAsHome>false</useWorkspaceAsHome>
    </hudson.plugins.gradle.Gradle>
  </builders>
  <publishers>
    <hudson.plugins.sonar.SonarPublisher plugin="sonar@2.1">
      <jdk>(Inherit From Job)</jdk>
      <branch>toBeChanged</branch>
      <language></language>
      <mavenOpts></mavenOpts>
      <jobAdditionalProperties>-Dsonar.java.source=1.7</jobAdditionalProperties>
      <settings class="jenkins.mvn.DefaultSettingsProvider"/>
      <globalSettings class="jenkins.mvn.DefaultGlobalSettingsProvider"/>
      <usePrivateRepository>false</usePrivateRepository>
    </hudson.plugins.sonar.SonarPublisher>
    <hudson.tasks.junit.JUnitResultArchiver plugin="junit@1.10">
      <testResults>build/test-results/*.xml</testResults>
      <keepLongStdio>false</keepLongStdio>
      <healthScaleFactor>1.0</healthScaleFactor>
      <allowEmptyResults>false</allowEmptyResults>
    </hudson.tasks.junit.JUnitResultArchiver>
  </publishers>
  <buildWrappers/>
</project>'''
	
	static final String CONFIG_GRADLE_JOB_NO_SONAR='''
<project>
  <actions/>
  <description></description>
  <logRotator class="hudson.tasks.LogRotator">
    <daysToKeep>-1</daysToKeep>
    <numToKeep>50</numToKeep>
    <artifactDaysToKeep>-1</artifactDaysToKeep>
    <artifactNumToKeep>-1</artifactNumToKeep>
  </logRotator>
  <keepDependencies>false</keepDependencies>
  <properties>
  </properties>
  <scm class="hudson.plugins.git.GitSCM" plugin="git@2.4.1">
    <configVersion>2</configVersion>
    <userRemoteConfigs>
      <hudson.plugins.git.UserRemoteConfig>
        <url>https://github.com/SoftwareBuildService/jenkins-build-per-gitflow-branch.git</url>
      </hudson.plugins.git.UserRemoteConfig>
    </userRemoteConfigs>
    <branches>
      <hudson.plugins.git.BranchSpec>
        <name>*/next</name>
      </hudson.plugins.git.BranchSpec>
    </branches>
    <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
    <submoduleCfg class="list"/>
    <extensions>
      <hudson.plugins.git.extensions.impl.WipeWorkspace/>
    </extensions>
  </scm>
  <canRoam>true</canRoam>
  <disabled>false</disabled>
  <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>
  <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>
  <jdk>(Default)</jdk>
  <triggers>
    <hudson.triggers.SCMTrigger>
      <spec>H/5 * * * *</spec>
      <ignorePostCommitHooks>false</ignorePostCommitHooks>
    </hudson.triggers.SCMTrigger>
  </triggers>
  <concurrentBuild>false</concurrentBuild>
  <builders>
    <hudson.plugins.gradle.Gradle plugin="gradle@1.24">
      <description></description>
      <switches></switches>
      <tasks>clean test</tasks>
      <rootBuildScriptDir></rootBuildScriptDir>
      <buildFile></buildFile>
      <gradleName>gradle-2.6</gradleName>
      <useWrapper>false</useWrapper>
      <makeExecutable>false</makeExecutable>
      <fromRootBuildScriptDir>true</fromRootBuildScriptDir>
      <useWorkspaceAsHome>false</useWorkspaceAsHome>
    </hudson.plugins.gradle.Gradle>
  </builders>
  <publishers>
    <hudson.plugins.sonar.SonarPublisher plugin="sonar@2.1">
      <jdk>(Inherit From Job)</jdk>
      <branch>toBeChanged</branch>
      <language></language>
      <mavenOpts></mavenOpts>
      <jobAdditionalProperties>-Dsonar.java.source=1.7</jobAdditionalProperties>
      <settings class="jenkins.mvn.DefaultSettingsProvider"/>
      <globalSettings class="jenkins.mvn.DefaultGlobalSettingsProvider"/>
      <usePrivateRepository>false</usePrivateRepository>
    </hudson.plugins.sonar.SonarPublisher>
    <hudson.tasks.junit.JUnitResultArchiver plugin="junit@1.10">
      <testResults>build/test-results/*.xml</testResults>
      <keepLongStdio>false</keepLongStdio>
      <healthScaleFactor>1.0</healthScaleFactor>
      <allowEmptyResults>false</allowEmptyResults>
    </hudson.tasks.junit.JUnitResultArchiver>
  </publishers>
  <buildWrappers/>
</project>'''
}

