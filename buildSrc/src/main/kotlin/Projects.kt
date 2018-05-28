import org.gradle.api.Project
import org.gradle.api.Task

fun Project.check(block: Task.() -> Unit) {
    "check"(this, block)
}

private inline operator fun String.invoke(project: Project, crossinline block: Task.() -> Unit) =
        project.tasks.whenTaskAdded { if (name == this@invoke) block() }
