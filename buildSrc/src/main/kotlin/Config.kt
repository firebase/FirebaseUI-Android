object Config {
    const val version = "10.0.0-beta02"
    val submodules = listOf("auth", "common", "firestore", "database", "storage")

    object SdkVersions {
        const val compile = 36
        const val target = 36
        const val min = 23
    }

}
