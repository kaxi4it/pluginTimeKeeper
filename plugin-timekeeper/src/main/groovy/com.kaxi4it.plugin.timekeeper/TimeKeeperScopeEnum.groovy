package com.kaxi4it.plugin.timekeeper

enum TimeKeeperScopeEnum {
    SCOPE_NULL(0b000),//未配置
    SCOPE_APP(0b100),//主APP内所有方法
    SCOPE_ANNOTATION(0b010),//注解方法
    SCOPE_CUSTOM(0b001),//自定义配置方法
    SCOPE_APP_ANNOTATION(0b110),//主APP内方法+注解
    SCOPE_ANNOTATION_CUSTOM(0b011),//注解+自定义配置方法
    SCOPE_APP_CUSTOM(0b101),//主APP+自定义配置方法
    SCOPE_APP_ANNOTATION_CUSTOM(0b111),//APP+注解+自定义配置方法
    private int value
    TimeKeeperScopeEnum(int value){
        this.value=value
    }
    int getValue(){
        return value
    }
}