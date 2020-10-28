package com.kaxi4it.plugin.timekeeper

import org.gradle.api.GradleException
import org.objectweb.asm.AnnotationVisitor

class TimeKeeperAnnotationVisitor extends AnnotationVisitor{

    public String desc
    public String className
    public String methodName

    /**
     * Constructs a new {@link AnnotationVisitor}.
     *
     * @param api
     *            the ASM API version implemented by this visitor. Must be one
     *            of {@link //Opcodes#ASM4}, {@link //Opcodes#ASM5} or {@link //Opcodes#ASM6}.
     * @param av
     *            the annotation visitor to which this visitor must delegate
     *            method calls. May be null.
     */
    TimeKeeperAnnotationVisitor(int api, AnnotationVisitor av,String desc,String className,String methodName) {
        super(api, av)
        this.desc=desc
        this.className=className
        this.methodName=methodName
    }

    /**
     * Visits a primitive value of the annotation.
     *
     * @param name
     *            the value name.
     * @param value
     *            the actual value, whose type must be {@link Byte},
     * {@link Boolean}, {@link Character}, {@link Short},
     * {@link Integer} , {@link Long}, {@link Float}, {@link Double},
     * {@link String} or {@link //Type} of OBJECT or ARRAY sort. This
     *            value can also be an array of byte, boolean, short, char, int,
     *            long, float or double values (this is equivalent to using
     * {@link #visitArray visitArray} and visiting each array element
     *            in turn, but is more convenient).
     */
    @Override
    void visit(String name, Object value) {
        if (desc == TimeKeeperGlobal.instance.tkStartAnnotation){
            if (TimeKeeperGlobal.instance.tkStartAnnotationMap.containsValue(value)){
                throw new GradleException("uniqueKey[${value}] in @tkStart is not unique")
            }else {
                TimeKeeperGlobal.instance.tkStartAnnotationMap.put(className+methodName,value)
            }
        }
        if (desc == TimeKeeperGlobal.instance.tkEndAnnotation){
            if (TimeKeeperGlobal.instance.tkEndAnnotationMap.containsValue(value)){
                throw new GradleException("uniqueKey[${value}] in @tkEnd is not unique")
            }else {
                TimeKeeperGlobal.instance.tkEndAnnotationMap.put(className+methodName,value)
            }
        }
    }

    /**
     * Visits an enumeration value of the annotation.
     *
     * @param name
     *            the value name.
     * @param desc
     *            the class descriptor of the enumeration class.
     * @param value
     *            the actual enumeration value.
     */
    @Override
    void visitEnum(String name, String desc, String value) {
        TLog.d("visitEnum="+name+" "+desc+" "+value.toString())
        super.visitEnum(name, desc, value)
    }

    /**
     * Visits a nested annotation value of the annotation.
     *
     * @param name
     *            the value name.
     * @param desc
     *            the class descriptor of the nested annotation class.
     * @return a visitor to visit the actual nested annotation value, or
     *         <tt>null</tt> if this visitor is not interested in visiting this
     *         nested annotation. <i>The nested annotation value must be fully
     *         visited before calling other methods on this annotation
     *         visitor</i>.
     */
    @Override
    AnnotationVisitor visitAnnotation(String name, String desc) {
        TLog.d("visitAnnotation="+name+" "+desc+" ")
        return super.visitAnnotation(name, desc)
    }

    /**
     * Visits an array value of the annotation. Note that arrays of primitive
     * types (such as byte, boolean, short, char, int, long, float or double)
     * can be passed as value to {@link #visit visit}. This is what
     * {@link // ClassReader} does.
     *
     * @param name
     *            the value name.
     * @return a visitor to visit the actual array value elements, or
     *         <tt>null</tt> if this visitor is not interested in visiting these
     *         values. The 'name' parameters passed to the methods of this
     *         visitor are ignored. <i>All the array values must be visited
     *         before calling other methods on this annotation visitor</i>.
     */
    @Override
    AnnotationVisitor visitArray(String name) {
        TLog.d("visitArray="+name+" ")
        return super.visitArray(name)
    }

    /**
     * Visits the end of the annotation.
     */
    @Override
    void visitEnd() {
        TLog.d("annotation [${desc}] visitEnd")
        super.visitEnd()
    }
}