/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.util;

import com.smilecoms.commons.base.cache.CacheHelper;
import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.util.Random;
import javassist.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class Javassist {

    private static final Random r = new Random();
    private static final Logger log = LoggerFactory.getLogger(Javassist.class);

    @SuppressWarnings("unchecked")
    public static Object runCode(Class[] classesForClassPath, String code, Object... params) throws Exception {
        try {
            long now = 0;
            RuntimeObjectHolder runtimeObjectHolder = CacheHelper.getFromLocalCache(code, RuntimeObjectHolder.class);
            if (runtimeObjectHolder == null) {
                if (log.isDebugEnabled()) {
                    now = System.currentTimeMillis();
                    log.debug("No object in cache with code [{}]", code);
                }

                ClassPool pool = new ClassPool();
                pool.appendSystemPath();
                for (Class c : classesForClassPath) {
                    pool.insertClassPath(new ClassClassPath(c));
                }
                try {
                    String libPath = Javassist.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath().replaceAll("smile-commons-util.*jar", "*");
                    log.debug("Adding all jars in [{}]", libPath);
                    pool.insertClassPath(libPath);
                } catch (Exception e) {
                    log.warn("Error adding class path", e);
                }

                try {
                    String libPath = Javassist.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath().replaceAll("lib/smile-commons-util.*jar", "");
                    File f = new File(libPath);
                    FileFilter platformDirectoryFilter = new FileFilter() {
                        @Override
                        public boolean accept(File file) {
                            return (file.isDirectory() && file.getName().endsWith("_jar"));
                        }
                    };
                    File[] platformDirs = f.listFiles(platformDirectoryFilter);
                    for (File platformDir : platformDirs) {
                        String path = platformDir.getPath();
                        log.debug("Adding path [{}]", path);
                        pool.insertClassPath(path);
                    }
                } catch (Exception e) {
                    log.warn("Error adding class path", e);
                }
                pool.insertClassPath(new ClassClassPath(Utils.class));
                pool.importPackage("com.smilecoms.commons.sca");
                pool.importPackage("com.smilecoms.commons.sca.beans");
                pool.importPackage("com.smilecoms.commons.util");
                pool.importPackage("com.smilecoms.commons.base");
                pool.importPackage("java.text");
                pool.importPackage("java.util");
                pool.importPackage("org.slf4j");

                CtClass evalClass = pool.makeClass("OBJ" + System.currentTimeMillis() + Math.abs(r.nextLong()));
                try {
                    CtMethod ctMethod = CtNewMethod.make(code, evalClass);
                    evalClass.addMethod(ctMethod);
                    Class clazz = evalClass.toClass();
                    runtimeObjectHolder = new RuntimeObjectHolder(clazz.newInstance());
                    CacheHelper.putInLocalCache(code, runtimeObjectHolder, 3600);
                } catch (Throwable t) {
                    new ExceptionManager(log).reportError(t);
                    throw t;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Javassist init for object took [{}]ms", System.currentTimeMillis() - now);
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Doing javassist call");
                now = System.currentTimeMillis();
            }
            Object ret = runtimeObjectHolder.invoke(params);
            if (log.isDebugEnabled()) {
                log.debug("Javassist call took [{}]ms", System.currentTimeMillis() - now);
            }
            return ret;
        } catch (Exception e) {
            throw e;
        } catch (Throwable t) {
            log.warn("Error running javassist code [{}]", t.toString());
            throw new Exception(t);
        }
    }
}

class RuntimeObjectHolder {

    private Object o;
    private Method m;

    public RuntimeObjectHolder(Object o) {
        this.o = o;
        m = o.getClass().getDeclaredMethods()[0];
    }

    public Object invoke(Object... params) throws Exception {
        return m.invoke(o, params);
    }
}
