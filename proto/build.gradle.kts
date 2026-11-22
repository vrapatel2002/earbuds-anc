// Pure Kotlin, no Android deps — so it can be unit-tested on the dev host
// and reused by the Android app as a plain module.
plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
