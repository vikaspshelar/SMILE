/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.util;

import com.smilecoms.commons.base.BaseUtils;
import java.sql.Connection;
import java.sql.DriverManager;
import javax.ejb.SessionContext;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import org.slf4j.*;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.OptimisticLockException;
import javax.persistence.Persistence;

/**
 *
 * @author paul
 */
public class JPAUtils {

    private static final Logger log = LoggerFactory.getLogger(JPAUtils.class.getName());
    //private static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";

    /**
     * Helper method to initialise the Java Persistence Framework. We need this
     * as we are controlling our own transaction boundaries due to the
     * persistence unit being RESOURCE_LOCAL
     */
    public static EntityManager getEM(EntityManagerFactory emf) {
        log.debug("Getting an EM");
        EntityManager em = emf.createEntityManager();
        return em;
    }

    /**
     * Returns an EMF. Whats very important is that you close the EMF at
     * undeployment time or else a new deployment will get issues with class
     * cast exceptions due to the old EMF still being used. The best way to
     * avoid this when using RESOURCE_LOCAL PU's and getEMF() is to call getEMF
     * in postConstruct of a singleton EJB and closeEMF on preDestroy method.
     * See RatingEngineFactory as an example
     *
     * @param pu
     * @return
     */
    public static EntityManagerFactory getEMF(String pu) {
        log.debug("Creating an EMF [{}]", pu);
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(pu);
        return emf;
    }

    /**
     *
     * see getEMF for details
     *
     * @param emf
     */
    public static void closeEMF(EntityManagerFactory emf) {
        if (emf != null && emf.isOpen()) {
            if (log.isDebugEnabled()) {
                log.debug("Closing an EMF ");
            }
            emf.close();
        } else if (log.isDebugEnabled()) {
            log.debug("Could not close an entity manager factory as it was null or already closed. Will ignore");
        }
    }

    /**
     * Close and set the entity manager to null
     */
    public static boolean closeEM(EntityManager em) {
        boolean committed = false;
        try {
            if (em == null) {
                return committed;
            }
            if (em.isOpen() && em.getTransaction().isActive()) {
                log.debug("Entity manager has an active transaction that will be attempted to be committed before closing");
                committed = commitTransaction(em);
            }
            if (em.isOpen()) {
                if (log.isDebugEnabled()) {
                    log.debug("Closing an EM");
                }
                em.close();
            } else if (log.isDebugEnabled()) {
                log.debug("Could not close an entity manager as it was already closed. Will ignore");
            }
        } catch (Exception ex) {
            log.warn("Error closing entity manager : [{}]. Printing stack trace", ex.toString());
            log.warn("Error: ", ex);
        }
        return committed;
    }

    /**
     * For RESOURCE_LOCAL PU's
     *
     * @param em
     */
    public static void beginTransaction(EntityManager em) {
        if (em != null && !em.getTransaction().isActive()) {
            if (log.isDebugEnabled()) {
                log.debug("Beginning a transaction on an entity manager");
            }
            em.getTransaction().begin();
        } else if (log.isDebugEnabled()) {
            log.debug("Tried to begin a transaction on an entity manager that already had an open transaction or was null. Will ignore");
        }
    }

    public static void restartTransaction(EntityManager em) {
        if (em != null && em.isOpen() && em.getTransaction().isActive()) {
            log.debug("Rolling back currently open transaction");
            em.getTransaction().rollback();
        }
        if (em != null && !em.getTransaction().isActive()) {
            log.debug("Beginning transaction");
            em.getTransaction().begin();
        }
    }

     public static boolean commitTransactionAndClear(EntityManager em) {
        boolean ret = commitTransaction(em);
        em.clear();
        return ret;
     }
     
    /**
     * For RESOURCE_LOCAL PU's
     *
     * @param em
     */
    public static boolean commitTransaction(EntityManager em) {
        boolean committed = false;
        if (em != null && em.isOpen() && em.getTransaction().isActive() && !em.getTransaction().getRollbackOnly()) {
            if (log.isDebugEnabled()) {
                log.debug("Committing currently open transaction");
            }
            em.getTransaction().commit();
            committed = true;
            if (log.isDebugEnabled()) {
                log.debug("Finished committing currently open transaction");
            }
        } else if (em != null && em.isOpen() && em.getTransaction().isActive() && em.getTransaction().getRollbackOnly()) {
            log.warn("Rolling back transaction that wanted to be committed as getRollBackOnly is true");
            em.getTransaction().rollback();
            if (BaseUtils.getBooleanProperty("env.jpautils.commitrollbackonly.printstacktrace", false)) {
                log.warn("Stacktrace: ", new Exception());
            }
            log.debug("Finished rolling back transaction that wanted to be committed as getRollBackOnly is true");
        } else if (em == null) {
            log.debug("em is null!");
        } else if (!em.isOpen()) {
            log.debug("em is not open!");
        } else {
            log.warn("There is no active transaction [{}] and yet a commit was requested. Who did this? Going to print stack trace", em.getTransaction().isActive());
            log.warn("Stacktrace: ", new Exception());
        }
        return committed;
    }

    /**
     * For RESOURCE_LOCAL PU's
     *
     * @param em
     */
    public static void rollbackTransaction(EntityManager em) {
        if (em != null && em.isOpen() && em.getTransaction().isActive()) {
            if (log.isDebugEnabled()) {
                log.debug("Rolling back currently open transaction");
            }
            em.getTransaction().rollback();
            if (log.isDebugEnabled()) {
                log.debug("Finished rolling back open transaction");
            }
        }
    }

    /**
     * For RESOURCE_LOCAL PU's
     *
     * @param em
     */
    public static void setRollbackOnly(EntityManager em) {
        if (em != null && em.isOpen() && em.getTransaction().isActive()) {
            if (log.isDebugEnabled()) {
                log.debug("Setting roll back only on currently open transaction");
            }
            em.getTransaction().setRollbackOnly();
            if (log.isDebugEnabled()) {
                log.debug("Finished setting roll back only on currently open transaction");
            }
        }
    }

    /**
     * For container managed PU's
     */
    public static void setRollbackOnly() {
        try {
            InitialContext ic = new InitialContext();
            SessionContext sctxLookup = (SessionContext) ic.lookup("java:comp/EJBContext");
            sctxLookup.setRollbackOnly();
        } catch (Exception e) {
            log.error("Error trying to setrollbackonly on CMP transaction", e);
        }
    }

    /**
     * Helper
     *
     * @param em
     * @param e
     */
    public static void persistAndFlush(EntityManager em, Object e) {
        if (log.isDebugEnabled()) {
            log.debug("Persisting an object and flushing the entity manager");
        }
        em.persist(e);
        em.flush();
    }

    /**
     * For RESOURCE_LOCAL PU's
     *
     * @param em
     */
    public static boolean commitTransactionAndClose(EntityManager em) {
        boolean committed;
        committed = commitTransaction(em);
        closeEM(em);
        return committed;
    }

    /**
     * Wraps the entitymanager and throws our EntityNotFoundException if no row
     * is found
     *
     * @param <T>
     * @param cls
     * @param e
     * @return Object
     * @throws com.smilecoms.commons.platform.EntityNotFoundException
     */
    @SuppressWarnings(value = "unchecked")
    public static <T> T findAndThrowENFE(EntityManager em, Class<T> cls, Object e) {
        Object ret = em.find(cls, e);
        if (ret == null) {
            throw new javax.persistence.EntityNotFoundException();
        }
        return (T) ret;
    }

    @SuppressWarnings(value = "unchecked")
    public static <T> T findAndThrowENFE(EntityManager em, Class<T> cls, Object e, javax.persistence.LockModeType l) {
        Object ret = em.find(cls, e, l);
        if (ret == null) {
            throw new javax.persistence.EntityNotFoundException();
        }
        return (T) ret;
    }

    /**
     * Throws a DirtyDataException if the two version are not the same. Used to
     * disallow updates where the data has been updated between when the caller
     * got their version and where they tried to make their update.
     *
     * @param lastModifiedFromCaller
     * @param lastModifiedInDB
     * @throws com.smilecoms.commons.platform.DirtyDataException
     */
    public static void checkLastModified(int callersVersion, int dbVersion) throws OptimisticLockException {
        if (callersVersion != dbVersion) {
            log.debug("DB Version : {} Callers Version : {}", dbVersion, callersVersion);
            throw new OptimisticLockException();
        }
    }

    public static boolean prefixNameWithJava = true;

    public static Connection getNonJTAConnection(String dsName) throws Exception {
        if (prefixNameWithJava) {
            dsName = "java:" + dsName;
        }
        Connection conn;
        javax.sql.DataSource ds;
        InitialContext initialContext = new InitialContext();
        if (log.isDebugEnabled()) {
            log.debug("Getting a connection from pool with JNDI name " + dsName);
        }
        try {
            ds = (javax.sql.DataSource) initialContext.lookup(dsName);
        } catch (Exception e) {
            log.warn("Could not find the datasource at [{}]", dsName);
            if (prefixNameWithJava) {
                dsName = dsName.substring(5);
            }
            try {
                ds = (javax.sql.DataSource) initialContext.lookup(dsName);
                if (prefixNameWithJava) {
                    prefixNameWithJava = false;
                }
            } catch (Exception e2) {
                log.warn("Error getting datasource from JNDI", e2);
                throw e2;
            }
        }

        conn = ds.getConnection();
        if (log.isDebugEnabled()) {
            log.debug("Successfully got connection using datasource " + dsName);
        }
        return conn;
    }

    /**
     * Recursively exhaust the JNDI tree
     */
    private static final void listContext(Context ctx, String indent) {
        try {
            NamingEnumeration list = ctx.listBindings("");
            while (list.hasMore()) {
                Binding item = (Binding) list.next();
                String className = item.getClassName();
                String name = item.getName();
                log.warn(indent + className + " " + name);
                Object o = item.getObject();
                if (o instanceof javax.naming.Context) {
                    listContext((Context) o, indent + " ");
                }
            }
        } catch (Exception ex) {
            log.warn("JNDI failure: ", ex);
        }
    }

    public static Connection getMSSQLConnection(String userName, String password, String host, String db) throws Exception {
        String url = "jdbc:sqlserver://" + host + ";databaseName=" + db;
        log.debug("Getting MSSQL connection to [{}]", url);
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        return DriverManager.getConnection(url, userName, password);
    }
    
    public static Connection getPostgresConnection(String userName, String password, String host, String db) throws Exception {
        String url = "jdbc:postgresql://" + host + "/" + db;
        log.debug("Getting Postgres connection to [{}]", url);
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(url, userName, password);
    }
}
