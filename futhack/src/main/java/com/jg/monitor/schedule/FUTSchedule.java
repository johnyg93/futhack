package com.jg.monitor.schedule;

import com.jg.monitor.logic.FUTLogic;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Startup bean to create scheduled task.
 * <p>
 * Created by Jonatan on 27/03/2017.
 */
@Startup
@Singleton
public class FUTSchedule implements Serializable {
    private static final long serialVersionUID = 1;

    @Inject
    private transient FUTLogic FUTLogic;

    @Resource(lookup = "java:jboss/ee/concurrency/scheduler/default")
    private transient ManagedScheduledExecutorService scheduledExecutorService;

    @PostConstruct
    private void init() {
        int seconds = Integer.parseInt(System.getProperty("transfers.check-interval"));
        scheduledExecutorService.scheduleAtFixedRate(FUTLogic::checkTransfers, 0, seconds, TimeUnit.SECONDS);
    }
}
