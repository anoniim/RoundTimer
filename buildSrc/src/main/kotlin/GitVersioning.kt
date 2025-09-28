import org.gradle.api.Project


fun Project.getGitVersionCode(): Int {
    return try {
        getTotalCommitCount(this)
    } catch (e: Exception) {
        1
    }
}

fun Project.getGitVersionName(): String {
    return try {
        getCurrentVersion(this)
    } catch (e: Exception) {
        // The current commit is not tagged, get the snapshot version
        getSnapshotVersion(this)
    }
}

/**
 * Get the current version from the tag of the current commit
 */
private fun getCurrentVersion(project: Project): String {
    return project.providers.exec {
        commandLine("git", "describe", "--tags", "--exact-match")
    }.standardOutput.asText.get().trim()
}

/**
 * Get the snapshot version from the latest tag and the number of commits since that tag
 */
private fun getSnapshotVersion(project: Project): String {
    return if (hasTags(project)) {
        val latestTag = project.providers.exec {
            commandLine("git", "describe", "--tags", "--abbrev=0")
        }.standardOutput.asText.get().trim()
        latestTag + "+" + getCommitCountSince(project, latestTag)
    } else {
        "snapshot"
    }
}

private fun getCommitCountSince(project: Project, latestTag: String): String {
    return project.providers.exec {
        commandLine("git", "rev-list", "--count", "$latestTag..HEAD")
    }.standardOutput.asText.get().trim()
}

private fun hasTags(project: Project): Boolean {
    val tagOutput = project.providers.exec {
        commandLine("git", "tag")
    }.standardOutput.asText.get().trim()
    return tagOutput.isNotEmpty() && tagOutput.lines().isNotEmpty()
}

/**
 * Get the total number of commits in the repository.
 */
private fun getTotalCommitCount(project: Project): Int {
    return project.providers.exec {
        commandLine("git", "rev-list", "--all", "--count")
    }.standardOutput.asText.get().trim().toInt()
}
