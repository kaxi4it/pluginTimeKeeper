package com.kaxi4it.plugin.timekeeper

@Singleton
class TimeKeeperGlobal {
    def tkStartAnnotationMap
    def tkEndAnnotationMap
    def custom
    def debug
    def scope
    def isIncremental
    String tkStartAnnotation="Lcom/kaxi4it/sdk/annotations/TkStart;"
    String tkEndAnnotation="Lcom/kaxi4it/sdk/annotations/TkEnd;"
    String tkCostAnnotation="Lcom/kaxi4it/sdk/annotations/TkCost;"

    Set<String> ignoreMethod=['<init>','<clinit>']
    String[] readJar

    void init(){
        tkStartAnnotationMap=[:]
        tkEndAnnotationMap=[:]
        custom=[:]
        debug=false
        scope=TimeKeeperScopeEnum.SCOPE_NULL
        readJar=[]
    }
}