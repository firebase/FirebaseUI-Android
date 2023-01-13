plugins {
    `java-platform`
    id("com.vanniktech.maven.publish")
}

dependencies {
    constraints {
        api(project(":auth"))
        api(project(":database"))
        api(project(":firestore"))
        api(project(":storage"))
    }
}