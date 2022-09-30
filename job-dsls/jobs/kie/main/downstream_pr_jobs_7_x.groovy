/**
 * Creates full downstream pullrequest (PR) jobs for kiegroup GitHub org. units.
 * These jobs execute the full downstream build for a specific PR to make sure the changes do not break the downstream repos.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : Constants.GITHUB_ORG_UNIT,
        branch                 : "7.x",
        timeoutMins            : 720,
        label                  : "kie-rhel7 && kie-mem24g && !built-in",
        executionNumber        : 10,
        ghAuthTokenId          : "kie-ci-token",
        ghJenkinsfilePwd       : "kie-ci",
        artifactsToArchive     : [],
        checkstyleFile         : Constants.CHECKSTYLE_FILE,
        findbugsFile           : Constants.FINDBUGS_FILE,
        buildJDKTool           : '',
        buildMavenTool         : '',
        buildChainGroup        : 'kiegroup',
        buildChainBranch       : 'main'
]
// override default config for specific repos (if needed)
def final REPO_CONFIGS = [
        "droolsjbpm-knowledge"               : [],
        "drools"                             : [],
        "optaplanner"                        : []
]


for (repoConfig in REPO_CONFIGS) {
    Closure<Object> get = { String key -> repoConfig.value[key] ?: DEFAULTS[key] }

    String repo = repoConfig.key
    String repoBranch = get("branch")
    String ghOrgUnit = get("ghOrgUnit")
    String ghAuthTokenId = get("ghAuthTokenId")
    String ghJenkinsfilePwd = get("ghJenkinsfilePwd")
    String additionalLabel = get("label")
    def exeNum = get("executionNumber")
    String additionalArtifacts = get("artifactsToArchive")
    additionalArtifacts = additionalArtifacts.replaceAll("[\\[\\]]", "")
    String additionalExcludedArtifacts = ""
    String additionalTimeout = get("timeoutMins")
    String gitHubJenkinsfileRepUrl = "https://github.com/${ghOrgUnit}/droolsjbpm-build-bootstrap/"
    String findbugsFile = get("findbugsFile")
    String checkstyleFile = get("checkstyleFile")
    String buildJDKTool = get("buildJDKTool")
    String buildMavenTool = get("buildMavenTool")
    String buildChainGroup = get('buildChainGroup')
    String buildChainBranch = get('buildChainBranch')

    // Creation of folders where jobs are stored
    folder("KIE")
    folder("KIE/${repoBranch}")
    folder("KIE/${repoBranch}/" + Constants.FDB_FOLDER){
        displayName(Constants.FDB_FOLDER_DISPLAY_NAME)
    }
    def folderPath = ("KIE/${repoBranch}/" + Constants.FDB_FOLDER)

    // FDB name
    String jobName = "${folderPath}/${repo}-${repoBranch}.fdb"

    pipelineJob(jobName) {

        description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.
                    |""".stripMargin())

        logRotator {
            numToKeep(exeNum)
        }

        properties {
            githubProjectUrl("https://github.com/${ghOrgUnit}/${repo}")
        }

        parameters {
            stringParam ("sha1","","this parameter will be provided by the PR")
            stringParam ("ADDITIONAL_ARTIFACTS_TO_ARCHIVE","${additionalArtifacts}","this parameter is provided by the job")
            stringParam ("ADDITIONAL_LABEL","${additionalLabel}","this parameter is provided by the job")
            stringParam ("ADDITIONAL_EXCLUDED_ARTIFACTS","${additionalExcludedArtifacts}","this parameter is provided by the job")
            stringParam ("ADDITIONAL_TIMEOUT","${additionalTimeout}","this parameter is provided by the job")
            stringParam ("CHECKSTYLE_FILE","${checkstyleFile}","")
            stringParam ("FINDBUGS_FILE","${findbugsFile}","")
            stringParam ("PR_TYPE","Full Downstream Build","")
            stringParam ("BUILD_JDK_TOOL","${buildJDKTool}","")
            stringParam ("BUILD_MAVEN_TOOL","${buildMavenTool}","")
            stringParam ("BUILDCHAIN_GROUP","${buildChainGroup}",'')
            stringParam ("BUILDCHAIN_BRANCH","${buildChainBranch}",'')
        }

        definition {
            cpsScm {
                scm {
                    gitSCM {
                        userRemoteConfigs {
                            userRemoteConfig {
                                url("${gitHubJenkinsfileRepUrl}")
                                credentialsId("${ghJenkinsfilePwd}")
                                name("")
                                refspec("")
                            }
                        }
                        branches {
                            branchSpec {
                                name("*/main")
                            }
                        }
                        browser { }
                        doGenerateSubmoduleConfigurations(false)
                        gitTool("")
                    }
                }
                scriptPath(".ci/jenkins/Jenkinsfile.buildchain")
            }
        }

        properties {
            pipelineTriggers {
                triggers {
                    ghprbTrigger {
                        onlyTriggerPhrase(true)
                        gitHubAuthId("${ghAuthTokenId}")
                        adminlist("")
                        orgslist("${ghOrgUnit}")
                        whitelist("")
                        cron("")
                        triggerPhrase(".*[j|J]enkins,?.*(execute|run|trigger|start|do) fdb.*")
                        allowMembersOfWhitelistedOrgsAsAdmin(true)
                        whiteListTargetBranches {
                            ghprbBranch {
                                branch("${repoBranch}")
                            }
                        }
                        useGitHubHooks(true)
                        permitAll(false)
                        autoCloseFailedPullRequests(false)
                        displayBuildErrorsOnDownstreamBuilds(false)
                        blackListCommitAuthor("")
                        commentFilePath("")
                        skipBuildPhrase("")
                        msgSuccess("Success")
                        msgFailure("Failure")
                        commitStatusContext("")
                        buildDescTemplate("")
                        blackListLabels("")
                        whiteListLabels("")
                        extensions {
                            ghprbSimpleStatus {
                                commitStatusContext("Linux - Full Downstream Build")
                                addTestResults(true)
                                showMatrixStatus(false)
                                statusUrl("")
                                triggeredStatus("")
                                startedStatus("")
                            }
                            ghprbCancelBuildsOnUpdate {
                                overrideGlobal(true)
                            }
                        }
                        includedRegions("")
                        excludedRegions("")
                    }
                }
            }
        }
    }
}
