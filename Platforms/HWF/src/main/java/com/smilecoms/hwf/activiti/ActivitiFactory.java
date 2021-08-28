package com.smilecoms.hwf.activiti;

import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.base.props.PropertyFetchException;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.hwf.db.model.HwfProcessDefinition;
import com.smilecoms.hwf.db.op.DAO;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.ProcessEngines;
import org.slf4j.*;

@Singleton
@Startup
@Local({BaseListener.class})
public class ActivitiFactory implements BaseListener {

    private EntityManagerFactory emf = null;
    private EntityManager em;
    private static final Logger log = LoggerFactory.getLogger(ActivitiFactory.class);
    private static ProcessEngine processEngine;
    private static ClassLoader activitiClassLoader;
    private static ScheduledFuture runner1 = null;

    @PostConstruct
    public void startUp() {
        BaseUtils.registerForPropsAvailability(this);
        emf = JPAUtils.getEMF("HWFPU_RL");
        activitiClassLoader = this.getClass().getClassLoader();
    }

    @Override
    public void propsAreReadyTrigger() {
        initialise();
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("HWF.CacheCheck") {
            @Override
            public void run() {
                trigger();
            }
        }, 10000, 60 * 1000, 90 * 1000);
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsAvailability(this);
        Async.cancel(runner1);
        JPAUtils.closeEMF(emf);
        log.debug("Stopping Activiti Process Engine");
        if (processEngine != null) {
            processEngine.close();
        }
        ProcessEngines.destroy();
        log.debug("Finished stopping Activiti Process Engine");
    }

    /**
     * Run any batches that are due
     *
     * @param string
     */
    private void trigger() {

        if (processEngine == null) {
            log.debug("Activiti is not initialised yet so processes cannot be deployed");
            return;
        }

        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            List<String> processes = DAO.getProcessDefinitionsInState(em, "PE");
            for (String resourceName : processes) {
                HwfProcessDefinition pd = DAO.getLockedDefinition(em, resourceName);
                if (!pd.getStatus().equals("PE")) {
                    log.debug("This process has subsequently been deployed by another HWF process. Ignoring");
                    continue;
                }
                String bpmn = pd.getBpmn();
                log.debug("Deploying Resource [{}] with BPMN [{}]", resourceName, bpmn);
                try {
                    processEngine.getRepositoryService().createDeployment().addString(resourceName + ".bpmn20.xml", bpmn).deploy();
                    log.debug("Finished Deploying Resource [{}]", resourceName);
                    pd.setStatus("AC");
                    em.persist(pd);
                } catch (Exception e) {
                    log.warn("Error deploying process", e);
                    pd.setStatus("FA");
                    em.persist(pd);
                }

            }
        } catch (Exception e) {
            log.warn("Error deploying business process definition", e);
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
    }

    private synchronized static void initialise() {
        if (processEngine != null) {
            ProcessEngines.destroy();
        }
        log.debug("Starting Activiti Process Engine");
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        try {
            activitiClassLoader.loadClass("org.activiti.engine.ProcessEngineConfiguration");
            Thread.currentThread().setContextClassLoader(activitiClassLoader);
            ProcessEngineConfiguration pec = ProcessEngineConfiguration.createProcessEngineConfigurationFromInputStream(BaseUtils.getPropertyAsStream("env.hwf.activiti.config"));
            processEngine = pec.buildProcessEngine();
        } catch (PropertyFetchException pfe) {
            log.warn("Cant initialise until properties are ready");
        } catch (Exception e) {
            log.warn("Error initialising Activiti:", e);
            if (processEngine != null) {
                ProcessEngines.destroy();
            }
            processEngine = null;
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
        log.debug("Finished starting Activiti Process Engine");
    }

    public static ProcessEngine getProcessEngine() {
        if (!BaseUtils.isPropsAvailable()) {
            throw new RuntimeException("Props are not available yet");
        }
        if (processEngine == null) {
            initialise();
        }
        if (processEngine == null) {
            throw new RuntimeException("Process Engine is not initialised");
        }
        return processEngine;
    }

    @Override
    public void propsHaveChangedTrigger() {
    }

}
