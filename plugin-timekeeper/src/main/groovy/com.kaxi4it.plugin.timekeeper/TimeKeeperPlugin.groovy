package com.kaxi4it.plugin.timekeeper

import org.gradle.api.Plugin
import org.gradle.api.Project

class TimeKeeperPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println("=====\tTimeKeeperPlugin\t=====")
        println("=\t\tauthor   kaxi4it\t\t=")
        println("=\t\t上海安卓内推面试群\t\t=")
        println("=\t\tQQ群号  465532338\t\t=")
        println("=====\tTimeKeeperPlugin\t=====")
        def extension=project.extensions.create("TimeKeeperConfig", TimeKeeperConfigExtension)
        TimeKeeperGlobal.instance.init()
        project.afterEvaluate {
            TimeKeeperGlobal.instance.debug = extension.debug
            TimeKeeperGlobal.instance.scope = extension.scope
            TimeKeeperGlobal.instance.custom = extension.custom
            TimeKeeperGlobal.instance.isIncremental = extension.isIncremental
            TimeKeeperGlobal.instance.readJar = extension.readJar
        }
        def android=project.android
        android.registerTransform(new TimeKeeperTransform())
    }


}