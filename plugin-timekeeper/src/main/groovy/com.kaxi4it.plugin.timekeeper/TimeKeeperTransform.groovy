package com.kaxi4it.plugin.timekeeper

import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.tasks.Workers
import com.android.build.gradle.options.Option
import com.android.ide.common.internal.WaitableExecutor
import com.android.ide.common.workers.WorkerExecutorFacade
import com.google.common.io.Files
import groovy.io.FileType
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.io.output.ByteArrayOutputStream
import org.bouncycastle.crypto.tls.TlsAEADCipher
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

import java.util.concurrent.Callable
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class TimeKeeperTransform extends Transform {

    int scopeInt = 0b000
    static TimeKeeperScopeEnum timeKeeperScopeEnum = TimeKeeperScopeEnum.SCOPE_NULL
//    WaitableExecutor waitableExecutor

    TimeKeeperTransform() {
//        waitableExecutor= WaitableExecutor.useGlobalSharedThreadPool()
    }
/**
 * Returns the unique name of the transform.
 *
 * <p>This is associated with the type of work that the transform does. It does not have to be
 * unique per variant.
 */

    @Override
    String getName() {
        return "TimeKeeperPlugin"
    }

    /**
     * Returns the type(s) of data that is consumed by the Transform. This may be more than
     * one type.
     *
     * <strong>This must be of type {@link QualifiedContent.DefaultContentType}</strong>
     */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * Returns the scope(s) of the Transform. This indicates which scopes the transform consumes.
     */
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    /**
     * Returns whether the Transform can perform incremental work.
     *
     * <p>If it does, then the TransformInput may contain a list of changed/removed/added files, unless
     * something else triggers a non incremental run.
     */
    @Override
    boolean isIncremental() {
        return false
    }

    /**
     * Executes the Transform.
     *
     * <p>The inputs are packaged as an instance of {@link TransformInvocation}
     * <ul>
     *     <li>The <var>inputs</var> collection of {@link TransformInput}. These are the inputs
     *     that are consumed by this Transform. A transformed version of these inputs must
     *     be written into the output. What is received is controlled through
     * {@link #getInputTypes()}, and {@link #getScopes()}.</li>
     *     <li>The <var>referencedInputs</var> collection of {@link TransformInput}. This is
     *     for reference only and should be not be transformed. What is received is controlled
     *     through {@link #getReferencedScopes()}.</li>
     * </ul>
     *
     * A transform that does not want to consume anything but instead just wants to see the content
     * of some inputs should return an empty set in {@link #getScopes()}, and what it wants to
     * see in {@link #getReferencedScopes()}.
     *
     * <p>Even though a transform's {@link Transform#isIncremental()} returns true, this method may
     * be receive <code>false</code> in <var>isIncremental</var>. This can be due to
     * <ul>
     *     <li>a change in secondary files ({@link #getSecondaryFiles()},
     * {@link #getSecondaryFileOutputs()}, {@link #getSecondaryDirectoryOutputs()})</li>
     *     <li>a change to a non file input ({@link #getParameterInputs()})</li>
     *     <li>an unexpected change to the output files/directories. This should not happen unless
     *     tasks are improperly configured and clobber each other's output.</li>
     *     <li>a file deletion that the transform mechanism could not match to a previous input.
     *     This should not happen in most case, except in some cases where dependencies have
     *     changed.</li>
     * </ul>
     * In such an event, when <var>isIncremental</var> is false, the inputs will not have any
     * incremental change information:
     * <ul>
     *     <li>{@link JarInput#getStatus()} will return {@link //Status#NOTCHANGED} even though
     *     the file may be added/changed.</li>
     *     <li>{@link DirectoryInput#getChangedFiles()} will return an empty map even though
     *     some files may be added/changed.</li>
     * </ul>
     *
     * @param transformInvocation the invocation object containing the transform inputs.
     * @throws IOException if an IO error occurs.
     * @throws InterruptedException* @throws TransformException Generic exception encapsulating the cause.
     */
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        TLog.d("====== ====== TimeKeeperTransform start ====== ======")
        def startTime = System.currentTimeMillis()
        def outputProvider=transformInvocation.outputProvider
        initTimeKeeperScope()

        if (!isIncremental()) {
            outputProvider.deleteAll()
        }

        //遍历输入文件
        transformInvocation.inputs.each { TransformInput input ->
            //遍历 jar
            input.jarInputs.each { JarInput jarInput ->
                handleJarInput(jarInput,outputProvider)
            }

            //遍历目录
            input.directoryInputs.each { DirectoryInput directoryInput ->
                handleDirectoryInput(directoryInput,outputProvider)
            }
        }

        def customTime = System.currentTimeMillis() - startTime
        TLog.d("use time = " + customTime + " ms")
        TLog.d("====== ====== TimeKeeperTransform end ====== ======")
    }

    private void initTimeKeeperScope() {
        TimeKeeperGlobal.instance.scope.each {
            if (it.toLowerCase() == 'app') {
                scopeInt += TimeKeeperScopeEnum.SCOPE_APP.value
            }
            if (it.toLowerCase() == 'annotation') {
                scopeInt += TimeKeeperScopeEnum.SCOPE_ANNOTATION.value
            }
            if (it.toLowerCase() == 'custom') {
                scopeInt += TimeKeeperScopeEnum.SCOPE_CUSTOM.value
            }
        }
        switch (scopeInt) {
            case TimeKeeperScopeEnum.SCOPE_APP.value:
                timeKeeperScopeEnum = TimeKeeperScopeEnum.SCOPE_APP
                break
            case TimeKeeperScopeEnum.SCOPE_ANNOTATION.value:
                timeKeeperScopeEnum = TimeKeeperScopeEnum.SCOPE_ANNOTATION
                break
            case TimeKeeperScopeEnum.SCOPE_CUSTOM.value:
                timeKeeperScopeEnum = TimeKeeperScopeEnum.SCOPE_CUSTOM
                break
            case TimeKeeperScopeEnum.SCOPE_APP_ANNOTATION.value:
                timeKeeperScopeEnum = TimeKeeperScopeEnum.SCOPE_APP_ANNOTATION
                break
            case TimeKeeperScopeEnum.SCOPE_APP_CUSTOM.value:
                timeKeeperScopeEnum = TimeKeeperScopeEnum.SCOPE_APP_CUSTOM
                break
            case TimeKeeperScopeEnum.SCOPE_ANNOTATION_CUSTOM.value:
                timeKeeperScopeEnum = TimeKeeperScopeEnum.SCOPE_ANNOTATION_CUSTOM
                break
            case TimeKeeperScopeEnum.SCOPE_APP_ANNOTATION_CUSTOM.value:
                timeKeeperScopeEnum = TimeKeeperScopeEnum.SCOPE_APP_ANNOTATION_CUSTOM
                break
        }
    }

    static void handleDirectoryInput(DirectoryInput directoryInput, TransformOutputProvider outputProvider) {
        if (directoryInput.file.isDirectory()) {
            directoryInput.file.eachFileRecurse { File file ->
                filterFileName(file)
            }
        }

        //处理完输入文件之后，要把输出给下一个任务
        def dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
        TLog.d("src path = " + directoryInput.file.absolutePath + " ,dest path = " + dest.absolutePath)
        FileUtils.copyDirectory(directoryInput.file, dest)
    }

    private static void filterFileName(File file) {
        def name = file.name
//                TLog.d("find name ="+ name+"  file.isFile() ="+ file.isFile())
        // 需要修改的类
        if (name.endsWith(".class") && !(name.startsWith('R$') || name.startsWith('R2$') ||
                name == 'R.class' || name == 'R2.class' || name == 'BuildConfig.class')) {
            TLog.d("=== modify file : " + name + "===")
            ClassReader classReader = new ClassReader(file.bytes)
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
            ClassVisitor classVisitor = new TimeKeeperClassVisitor(Opcodes.ASM6, classWriter, timeKeeperScopeEnum)
            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
            byte[] code = classWriter.toByteArray()
            FileOutputStream fos = new FileOutputStream(file.parentFile.absolutePath + File.separator + name)
            fos.write(code)
            fos.close()
        }
    }

    /**
     * 处理Jar中的class文件
     * @param jarInput
     * @param outputProvider
     */
    static void handleJarInput(JarInput jarInput, TransformOutputProvider outputProvider) {
        if (jarInput.file.getAbsolutePath().endsWith(".jar")) {
            //重名名输出文件,因为可能同名,会覆盖
            def jarName = jarInput.name
            def md5Name = DigestUtils.md5Hex(jarInput.file.absolutePath)
            if (jarName.endsWith(".jar")) {
                jarName = jarName.substring(0, jarName.length() - 4)
            }
            JarFile jarFile = new JarFile(jarInput.file)
            Enumeration enumeration = jarFile.entries()
            File tempFile = new File(jarInput.file.parent + File.separator + "temp.jar")
            //避免上次的缓存被重复插入
            if (tempFile.exists()) {
                tempFile.delete()
            }
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tempFile))
            //保存
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement()
                String entryName = jarEntry.name
                ZipEntry zipEntry = new ZipEntry(entryName)
                InputStream inputStream = jarFile.getInputStream(zipEntry)
                if (entryName.endsWith(".class") && !(entryName.startsWith('R$') || entryName.startsWith('R2$') ||
                        entryName == 'R.class' || entryName == 'R2.class' || entryName == 'BuildConfig.class')
                        && !(entryName.startsWith('com/kaxi4it/sdk/'))
                        && isReadJar(entryName)
                ) {
                    TLog.d("=== modify jar : " + entryName + " ===")
                    jarOutputStream.putNextEntry(zipEntry)
                    ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream))
                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                    ClassVisitor classVisitor = new TimeKeeperClassVisitor(Opcodes.ASM6, classWriter, timeKeeperScopeEnum)
                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                    byte[] bytes = classWriter.toByteArray()
                    jarOutputStream.write(bytes)
                } else {
                    jarOutputStream.putNextEntry(zipEntry)
                    jarOutputStream.write(IOUtils.toByteArray(inputStream))
                }
                jarOutputStream.closeEntry()
            }
            jarOutputStream.close()
            jarFile.close()
            def dest = outputProvider.getContentLocation(jarName + "_" + md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
            TLog.d("src jar = " + tempFile.absolutePath + " ,dest jar = " + dest.absolutePath)
            FileUtils.copyFile(tempFile, dest)
            tempFile.delete()
        }
    }

    static void handleDirectoryIncremental(DirectoryInput directoryInput, TransformOutputProvider outputProvider){
        Map<File, Status> map = directoryInput.getChangedFiles()
    }

    static boolean isReadJar(String name){
        boolean result=false
        TimeKeeperGlobal.instance.readJar.each {
            if (name.startsWith(it)){
                result=true
                return
            }
        }
        return result
    }

    private void foreachClass(DirectoryInput directoryInput) throws IOException {
        File dest = outputProvider.getContentLocation(directoryInput.getName(), directoryInput.getContentTypes(),
                directoryInput.getScopes(), Format.DIRECTORY)
        Map<File, Status> map = directoryInput.getChangedFiles()
        File dir = directoryInput.getFile()
        if (isIncremental()) {
            for (Map.Entry<File, Status> entry : map.entrySet()) {
                Status status = entry.getValue()
                File file = entry.getKey()
                String destFilePath = file.getAbsolutePath().replace(dir.getAbsolutePath(), dest.getAbsolutePath())
                File destFile = new File(destFilePath)
                switch (status) {
                    case NOTCHANGED:
                        break
                    case ADDED:
                    case CHANGED:
                        try {
                            FileUtils.touch(destFile)
                        } catch (Exception ignored) {
                            Files.createParentDirs(destFile)
                        }
                        modifySingleFile(dir, file, dest)
                        break;
                    case REMOVED:
                        Log.info(entry);
                        deleteDirectory(destFile, dest);
                        break;
                }
            }
        } else {
            changeFile(dir, dest);
        }
    }


    void forEachJar(boolean isIncremental, JarInput jarInput, TransformOutputProvider outputProvider, Context context) {
        String destName = jarInput.file.name
        //截取文件路径的 md5 值重命名输出文件，因为可能同名，会覆盖
        def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath).substring(0, 8)
        if (destName.endsWith(".jar")) {
            destName = destName.substring(0, destName.length() - 4)
        }
        //获得输出文件
        File destFile = outputProvider.getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
        if (isIncremental) {
            Status status = jarInput.getStatus()
            switch (status) {
                case Status.NOTCHANGED:
                    break
                case Status.ADDED:
                case Status.CHANGED:
                    handleJarInput(jarInput, outputProvider)
                    break
                case Status.REMOVED:
                    if (destFile.exists()) {
                        FileUtils.forceDelete(destFile)
                    }
                    break
                default:
                    break
            }
        } else {
            handleJarInput(jarInput, outputProvider)
        }
    }



    void forEachDirectory(boolean isIncremental, DirectoryInput directoryInput, TransformOutputProvider outputProvider, Context context) {
        File dir = directoryInput.file
        File dest = outputProvider.getContentLocation(directoryInput.getName(),
                directoryInput.getContentTypes(), directoryInput.getScopes(),
                Format.DIRECTORY)
        FileUtils.forceMkdir(dest)
        String srcDirPath = dir.absolutePath
        String destDirPath = dest.absolutePath
        if (isIncremental) {
            Map<File, Status> fileStatusMap = directoryInput.getChangedFiles()
            for (Map.Entry<File, Status> changedFile : fileStatusMap.entrySet()) {
                Status status = changedFile.getValue()
                File inputFile = changedFile.getKey()
                String destFilePath = inputFile.absolutePath.replace(srcDirPath, destDirPath)
                File destFile = new File(destFilePath)
                switch (status) {
                    case Status.NOTCHANGED:
                        break
                    case Status.REMOVED:
//                        Logger.info("目录 status = $status:$inputFile.absolutePath")
                        if (destFile.exists()) {
                            //noinspection ResultOfMethodCallIgnored
                            destFile.delete()
                        }
                        break
                    case Status.ADDED:
                    case Status.CHANGED:
                        filterFileName(inputFile)
                        break
                    default:
                        break
                }
            }
        } else {
//            FileUtils.copyDirectory(dir, dest)
//            dir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) {
//                File inputFile ->
//                    forEachDir(dir, inputFile, context, srcDirPath, destDirPath)
//            }
            handleDirectoryInput(directoryInput,outputProvider)
        }
    }

}