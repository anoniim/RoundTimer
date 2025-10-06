import org.gradle.api.Project
import org.gradle.api.provider.Provider

fun Project.getGitVersionCodeProvider(): Provider<Int> {
    return project.providers.exec {
        workingDir(project.rootDir)
        commandLine("git", "rev-list", "--all", "--count")
    }.standardOutput.asText.map { it.trim().toInt() }
}

fun Project.getGitVersionNameProvider(): Provider<String> {
    val exactMatchProvider = project.providers.exec {
        workingDir(project.rootDir)
        commandLine("git", "describe", "--tags", "--exact-match")
        isIgnoreExitValue = true
    }

    return exactMatchProvider.result.flatMap { result ->
        if (result.exitValue == 0) {
            exactMatchProvider.standardOutput.asText.map { it.trim() }
        } else {
            getSnapshotVersionProvider(project)
        }
    }
}

private fun getSnapshotVersionProvider(project: Project): Provider<String> {
    val gitTagOutputProvider = project.providers.exec {
        workingDir(project.rootDir)
        commandLine("git", "tag")
        isIgnoreExitValue = true
    }.standardOutput.asText

    return gitTagOutputProvider.flatMap { tagOutput ->
        if (tagOutput.trim().isEmpty()) {
            project.providers.provider { "snapshot" }
        } else {
            val latestTagProvider = project.providers.exec {
                workingDir(project.rootDir)
                commandLine("git", "describe", "--tags", "--abbrev=0")
            }.standardOutput.asText.map { it.trim() }

            val commitCountProvider = latestTagProvider.flatMap { latestTag ->
                project.providers.exec {
                    workingDir(project.rootDir)
                    commandLine("git", "rev-list", "--count", "$latestTag..HEAD")
                }.standardOutput.asText.map { it.trim() }
            }

            latestTagProvider.zip(commitCountProvider) { latestTag, commitCount ->
                "$latestTag+$commitCount"
            }
        }
    }
}