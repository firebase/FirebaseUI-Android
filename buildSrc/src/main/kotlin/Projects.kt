import org.gradle.api.Project
import org.gradle.api.Task

inline fun Project.check(crossinline block: Task.() -> Unit) {
    tasks.whenTaskAdded {
        if (name == "check") block()
    }
}
