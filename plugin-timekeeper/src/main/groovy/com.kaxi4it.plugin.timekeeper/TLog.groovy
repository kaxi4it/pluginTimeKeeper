package com.kaxi4it.plugin.timekeeper

class TLog{
    static void d(String msg){
        if (TimeKeeperGlobal.instance.debug){
            println(msg)
        }
    }
}