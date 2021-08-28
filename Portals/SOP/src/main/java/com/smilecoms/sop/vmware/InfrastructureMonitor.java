/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sop.vmware;

import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.base.ops.Syslog;
import com.smilecoms.commons.base.ops.SyslogDefs;
import com.smilecoms.commons.util.Utils;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.GuestDiskInfo;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */
public class InfrastructureMonitor implements BaseListener {

    private static final int DEFAULT_MIN_FREE_PERCENT = 5;
    /* min. percentage of free space left before we start raising alarms */

    private static final String MINFREEDISKPERCENT_PROP_NAME = "env.vmware.infrastructure.diskspace.minpercentfree";
    private static final String VMS_IGNORE_DISK_CHECK_PROP_NAME = "env.vmware.infrastructure.diskspace.vmstoignoreregex";
    private static final String DATASTORE_IGNORE_DISK_CHECK_PROP_NAME = "env.vmware.infrastructure.diskspace.datastorestoignoreregex";
    private static final String STAT_NAME = "IT Infrastructure";
    private static GuestDiskInfo[] guestDisks;
    private static String message;
    private static int minPercentFree;
    private static String vmsToIgnoreFromDiskCheckRegex;
    /* regex of vms to ignore from disk check */

    private static String datastoresToIgnoreFromDiskCheckRegex;
    /* regex of datastores to ignore from disk check */

    private static Folder rootFolder;
    private static ServiceInstance si;
    private static String vcHost;
    private static String user;
    private static String password;
    private static String syslogHostname;

    private static final Logger log = LoggerFactory.getLogger(InfrastructureMonitor.class);
    private static ScheduledFuture runner1 = null;

    public InfrastructureMonitor() {
        log.warn("InfrastructureMonitor constructor");
        BaseUtils.registerForPropsAvailability(this);
    }

    @Override
    public void propsAreReadyTrigger() {
        log.warn("InfrastructureMonitor is starting up as properties are ready");
        BaseUtils.deregisterForPropsAvailability(this);
        BaseUtils.registerForPropsChanges(this);
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("SOP.InfrastructureMonitor") {
            @Override
            public void run() {
                trigger();
            }
        }, 30000, 5 * 60 * 1000);
        updateConfiguration();
    }

    public void shutdown() {
        log.warn("In SOP Infrastructure monitor shutdown");
        BaseUtils.deregisterForPropsChanges(this);
        Async.cancel(runner1);
    }

    @Override
    public void propsHaveChangedTrigger() {
        updateConfiguration();
    }

    private void trigger() {
        try {

            if (BaseUtils.getBooleanProperty("env.is.training.environment", false)) {
                log.debug("Training environment so VMWare monitor need not run");
                return;
            }
            log.warn("In VMWare monitor trigger");
            boolean haveWarning = false;
            float freeSpace, capacity;
            float percentageFree;

            try {
                log.debug("Creating service instance");
                si = new ServiceInstance(new URL("https://" + vcHost + "/sdk"), user, password, true);
                log.debug("Created service instance. Getting root folder");
                rootFolder = si.getRootFolder();
                log.debug("Got root folder");
            } catch (MalformedURLException | RemoteException e) {
                if (!BaseUtils.getBooleanPropertyFailFast("env.development.mode", false)) {
                    log.warn("SOP Infrastructure monitor failed to connect VCenter host {}", vcHost);
                    log.warn("Error: ", e);
                }
                return;
            }

            ManagedEntity[] mes = null;
            try {
                log.debug("Creating ManagedEntities");
                mes = new InventoryNavigator(rootFolder).searchManagedEntities("Datastore");
                log.debug("Created ManagedEntities");
            } catch (RemoteException e) {
                log.warn("Error getting ManagedEntities for datastore", e);
            }

            if (mes == null || mes.length == 0) {
                log.warn("ManagedEntities for Datastore is null");
                return;
            }

            Datastore ds;
            DatastoreSummary summary;

            float gbDenominator = 1000000000f;
            for (ManagedEntity me : mes) {
                ds = (Datastore) me;
                summary = ds.getSummary();
                capacity = (float) summary.capacity / gbDenominator;
                freeSpace = (float) summary.freeSpace / gbDenominator;
                if (capacity > 0) {
                    percentageFree = (float) freeSpace / capacity * 100;
                    if (percentageFree <= minPercentFree) {
                        if (Utils.matches(summary.getName(), datastoresToIgnoreFromDiskCheckRegex)) {
                            continue;
                        }
                        message = "Datastore usage running high on " + summary.getName() + ": free space: " + freeSpace + "GB and capacity is " + capacity;
                        Syslog.sendSyslog(syslogHostname, "IT Infrastructure", SyslogDefs.LOG_LOCAL6, SyslogDefs.LOG_DEBUG, message);
                        haveWarning = true;
                    }
                }
            }

            try {
                /* so far we have checked overall datastores, now we need to check the actual VM's space on the disks it is assigned/using */
                mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
            } catch (RemoteException e) {
                log.warn("Error getting ManagedEntities for VirtualMachine", e);
            }
            if (mes == null || mes.length == 0) {
                log.debug("ManagedEntities for VirtualMachine is null");
                return;
            }
            VirtualMachine vm;
            log.debug("Looping through managed entities");
            for (ManagedEntity me : mes) {
                vm = (VirtualMachine) me;
                //ignore freenas boxes
                if (vm.getName().toLowerCase().contains("freenas")) {
                    log.debug("VM is skipped as its a nas [{}]", vm.getName());
                    continue;
                }

                guestDisks = vm.getGuest().disk;
                if (guestDisks == null) {
                    log.debug("guestDisks is null for VM [{}]", vm.getName());
                    continue;
                }
                if (Utils.matches(vm.getName(), vmsToIgnoreFromDiskCheckRegex)) {
                    log.debug("Ignoring VM [{}]", vm.getName());
                    continue;
                }
                for (int j = 0; j < guestDisks.length; j++) {
                    log.debug("Looking as disk [{}]", j);
                    capacity = (float) (guestDisks[j].getCapacity() / gbDenominator);
                    freeSpace = (float) (guestDisks[j].getFreeSpace() / gbDenominator);
                    if (capacity > 0) {
                        percentageFree = freeSpace / capacity * 100;
                        message = "VM: " + vm.getName() + ": Disk: " + j + ", Capacity: " + guestDisks[j].getCapacity() / gbDenominator + "GB, Free: " + guestDisks[j].getFreeSpace() / gbDenominator + "GB - percentage Free: " + percentageFree;
                        log.debug("VMName [{}] Result [{}]", vm.getName(), message);
                        if (percentageFree <= minPercentFree) {
                            haveWarning = true;
                            log.warn(message);
                            Syslog.sendSyslog(syslogHostname, STAT_NAME, SyslogDefs.LOG_LOCAL6, SyslogDefs.LOG_DEBUG, message);
                        }
                    } else {
                        log.debug("Capacity is < 0 [{}]", capacity);
                    }
                }
            }

            si.getServerConnection().logout();

            if (haveWarning) {
                log.debug("We have a warning");
                BaseUtils.sendStatistic("IT Infrastructure", "IT Infrastructure", "isup", 0, "Java Test Script");
                Syslog.sendSyslog(syslogHostname, STAT_NAME, SyslogDefs.LOG_LOCAL6, SyslogDefs.LOG_ERR, "IT Infrastructure errors");
            } else {
                log.debug("We dont have a warning");
                BaseUtils.sendStatistic("IT Infrastructure", "IT Infrastructure", "isup", 1, "Java Test Script");
            }
        } catch (Throwable e) {
            log.warn("Error: ", e);
        }
    }

    private void updateConfiguration() {
        log.debug("In VMWare monitor updateConfiguration");
        syslogHostname = BaseUtils.getProperty("env.syslog.hostname");
        vcHost = BaseUtils.getProperty("env.vmware.vcenter.host");
        user = BaseUtils.getProperty("env.vmware.vcenter.user");
        password = BaseUtils.getProperty("env.vmware.vcenter.password");
        minPercentFree = BaseUtils.getIntProperty(MINFREEDISKPERCENT_PROP_NAME, DEFAULT_MIN_FREE_PERCENT);
        vmsToIgnoreFromDiskCheckRegex = BaseUtils.getProperty(VMS_IGNORE_DISK_CHECK_PROP_NAME, "NONE");
        datastoresToIgnoreFromDiskCheckRegex = BaseUtils.getProperty(DATASTORE_IGNORE_DISK_CHECK_PROP_NAME, "NONE");
        log.debug("Finished VMWare monitor updateConfiguration [{}][{}][{}][{}][{}][{}][{}]", new Object[]{syslogHostname, vcHost, user, password, minPercentFree, vmsToIgnoreFromDiskCheckRegex, datastoresToIgnoreFromDiskCheckRegex});
    }
}
