package com.kaxi4it.plugin.timekeeper

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AdviceAdapter

class TimeKeeperAdviceAdapter extends AdviceAdapter {

    def isStart = false
    def isEnd = false
    def isCost = false
    def isCustom = false
    def timeKeeperAnnotationVisitor

    String methodName
    String className
    TimeKeeperScopeEnum timeKeeperScopeEnum
    Map methodStartLineMap

    /**
     * Constructs a new {@link AdviceAdapter}.
     *
     * @param api the ASM API version implemented by this visitor. Must be one of {@link
     *     Opcodes#ASM4}, {@link //Opcodes#ASM5}, {@link //Opcodes#ASM6} or {@link //Opcodes#ASM7}.
     * @param methodVisitor the method visitor to which this adapter delegates calls.
     * @param access the method's access flags (see {@link //Opcodes}).
     * @param name the method's name.
     * @param descriptor the method's descriptor (see {@link //Type Type}).
     */
    protected TimeKeeperAdviceAdapter(int api, TimeKeeperScopeEnum timeKeeperScopeEnum, String className, MethodVisitor methodVisitor, int access, String name, String descriptor) {
        super(api, methodVisitor, access, name, descriptor)
        this.timeKeeperScopeEnum = timeKeeperScopeEnum
        this.methodName = name
        this.className = className
        methodStartLineMap=[:]
    }

    /**
     * Visits an annotation of this method.
     *
     * @param descriptor the class descriptor of the annotation class.
     * @param visible {@literal true} if the annotation is visible at runtime.
     * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
     *     interested in visiting this annotation.
     */
    @Override
    AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if ((timeKeeperScopeEnum.value & TimeKeeperScopeEnum.SCOPE_ANNOTATION.value) == TimeKeeperScopeEnum.SCOPE_ANNOTATION.value) {
            def annotationVisitor = super.visitAnnotation(descriptor, visible)
            timeKeeperAnnotationVisitor = new TimeKeeperAnnotationVisitor(api, annotationVisitor, descriptor, className, methodName)
            if (TimeKeeperGlobal.instance.tkStartAnnotation == descriptor) {
                isStart = true
            }
            if (TimeKeeperGlobal.instance.tkEndAnnotation == descriptor) {
                isEnd = true
            }
            if (TimeKeeperGlobal.instance.tkCostAnnotation == descriptor) {
                isCost = true
            }
            return timeKeeperAnnotationVisitor
        } else {
            return super.visitAnnotation(descriptor, visible)
        }

    }



    @Override
    protected void onMethodEnter() {
        if ((timeKeeperScopeEnum.value & TimeKeeperScopeEnum.SCOPE_APP.value )== TimeKeeperScopeEnum.SCOPE_APP.value) {
            TLog.d("method[${methodName}]start insert,@tkCostIn")
            mv.visitMethodInsn(INVOKESTATIC, "com/kaxi4it/sdk/TimeCostUtils", "getInstance", "()Lcom/kaxi4it/sdk/TimeCostUtils;", false)
            mv.visitLdcInsn(className)
            mv.visitLdcInsn(methodName)
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/kaxi4it/sdk/TimeCostUtils", "tkCostIn", "(Ljava/lang/String;Ljava/lang/String;)V", false)
        }
        if ((timeKeeperScopeEnum.value & TimeKeeperScopeEnum.SCOPE_ANNOTATION.value )== TimeKeeperScopeEnum.SCOPE_ANNOTATION.value) {
            // 方法开始
            if (isStart) {
                TLog.d("method[${methodName}]start insert, @tkStart")
                mv.visitMethodInsn(INVOKESTATIC, "com/kaxi4it/sdk/TimeCostUtils", "getInstance", "()Lcom/kaxi4it/sdk/TimeCostUtils;", false)
                mv.visitLdcInsn(TimeKeeperGlobal.instance.tkStartAnnotationMap.get(className + methodName))
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/kaxi4it/sdk/TimeCostUtils", "tkStart", "(Ljava/lang/String;)V", false)
            }

            if (isCost&&!((timeKeeperScopeEnum.value & TimeKeeperScopeEnum.SCOPE_APP.value )== TimeKeeperScopeEnum.SCOPE_APP.value)) {
                TLog.d("method[${methodName}]start insert,@tkCostIn")
                mv.visitMethodInsn(INVOKESTATIC, "com/kaxi4it/sdk/TimeCostUtils", "getInstance", "()Lcom/kaxi4it/sdk/TimeCostUtils;", false)
                mv.visitLdcInsn(className)
                mv.visitLdcInsn(methodName)
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/kaxi4it/sdk/TimeCostUtils", "tkCostIn", "(Ljava/lang/String;Ljava/lang/String;)V", false)
            }
        }
        if ((timeKeeperScopeEnum.value & TimeKeeperScopeEnum.SCOPE_CUSTOM.value )== TimeKeeperScopeEnum.SCOPE_CUSTOM.value) {
            TLog.d("SCOPE_CUSTOM className[${className}],method[${methodName}]")
            if (TimeKeeperGlobal.instance.custom.containsKey(className)){
                Map<String,Object> aa=new HashMap()
                aa.get(className)
                def methodList = TimeKeeperGlobal.instance.custom.get(className)
                if (methodList instanceof List){
                    methodList.each {
                        if (methodName==it){
                            isCustom=true
                            return
                        }
                    }
                }
            }
        }
        super.onMethodEnter()
    }

    @Override
    protected void onMethodExit(int opcode) {
        def line=methodStartLineMap.get(methodName)
        if ((timeKeeperScopeEnum.value & TimeKeeperScopeEnum.SCOPE_APP.value) == TimeKeeperScopeEnum.SCOPE_APP.value) {
            TLog.d("method[${methodName}]start insert,@tkCostOut")
            mv.visitMethodInsn(INVOKESTATIC, "com/kaxi4it/sdk/TimeCostUtils", "getInstance", "()Lcom/kaxi4it/sdk/TimeCostUtils;", false)
            mv.visitLdcInsn(className)
            mv.visitLdcInsn(methodName)
            mv.visitIntInsn(BIPUSH, line)
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/kaxi4it/sdk/TimeCostUtils", "tkCostOut", "(Ljava/lang/String;Ljava/lang/String;I)V", false)
        }
        if ((timeKeeperScopeEnum.value & TimeKeeperScopeEnum.SCOPE_ANNOTATION.value )== TimeKeeperScopeEnum.SCOPE_ANNOTATION.value) {
            if (isEnd) {
                TLog.d("method[${methodName}]start insert,@tkEnd")
                mv.visitMethodInsn(INVOKESTATIC, "com/kaxi4it/sdk/TimeCostUtils", "getInstance", "()Lcom/kaxi4it/sdk/TimeCostUtils;", false)
                mv.visitLdcInsn(TimeKeeperGlobal.instance.tkEndAnnotationMap.get(className + methodName))
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/kaxi4it/sdk/TimeCostUtils", "tkEnd", "(Ljava/lang/String;)V", false)
            }
            if (isCost&&!((timeKeeperScopeEnum.value & TimeKeeperScopeEnum.SCOPE_APP.value )== TimeKeeperScopeEnum.SCOPE_APP.value)) {
                TLog.d("method[${methodName}]start insert,@tkCostOut")
                mv.visitMethodInsn(INVOKESTATIC, "com/kaxi4it/sdk/TimeCostUtils", "getInstance", "()Lcom/kaxi4it/sdk/TimeCostUtils;", false)
                mv.visitLdcInsn(className)
                mv.visitLdcInsn(methodName)
                mv.visitIntInsn(BIPUSH, line)
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/kaxi4it/sdk/TimeCostUtils", "tkCostOut", "(Ljava/lang/String;Ljava/lang/String;I)V", false)
            }

        }
        super.onMethodExit(opcode)
    }

    /**
     * Visits a line number declaration.
     *
     * @param line a line number. This number refers to the source file from which the class was
     *     compiled.
     * @param start the first instruction corresponding to this line number.
     * @throws IllegalArgumentException if {@code start} has not already been visited by this visitor
     *     (by the {@link #visitLabel} method).
     */
    @Override
    void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start)
        if (!methodStartLineMap.containsKey(methodName)){
            methodStartLineMap.put(methodName,line)
        }

    }


}