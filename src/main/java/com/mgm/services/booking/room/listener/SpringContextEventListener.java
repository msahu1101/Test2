/**
 * 
 */
package com.mgm.services.booking.room.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.collections.MapUtils;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.service.cache.impl.AbstractCacheService;

import lombok.extern.log4j.Log4j2;

/**
 * Spring context event listener to start invoke all cache services class to
 * load cache information during application start up.
 * 
 */
@Component
@Log4j2
public class SpringContextEventListener implements ApplicationListener<ApplicationEvent> {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.context.ApplicationListener#onApplicationEvent(org.
     * springframework.context.ApplicationEvent)
     */
    @Override
    public void onApplicationEvent(ApplicationEvent arg0) {
        if (arg0 instanceof ContextRefreshedEvent) {
            final ApplicationContext appContext = ((ContextRefreshedEvent) arg0).getApplicationContext();
            final Map<String, AbstractCacheService> cachingServiceBeans = appContext
                    .getBeansOfType(AbstractCacheService.class);
            log.info("cachingServiceBeans {}", cachingServiceBeans);
            if (!MapUtils.isEmpty(cachingServiceBeans)) {
                final ExecutorService taskExecutor = Executors.newFixedThreadPool(cachingServiceBeans.size());
                final List<Future<Object>> tasks = new ArrayList<Future<Object>>();
                for (final AbstractCacheService cacheService : cachingServiceBeans.values()) {
                    final Callable<Object> task = new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            cacheService.firstLoadDataToCache();
                            return cacheService;
                        }
                    };
                    tasks.add(taskExecutor.submit(task));
                }
                for (final Future<Object> future : tasks) {
                    try {
                        future.get();
                    } catch (CancellationException | InterruptedException | ExecutionException e) {
                        log.error("Error building cache: ", e);
                    }
                }
                taskExecutor.shutdown();
            }
        } else if (arg0 instanceof ContextClosedEvent) {
            final ApplicationContext appContext = ((ContextClosedEvent) arg0).getApplicationContext();
            final EhCacheManagerFactoryBean cacheFactory = appContext.getBean(EhCacheManagerFactoryBean.class);
            if (cacheFactory != null) {
                cacheFactory.getObject().removeAllCaches();
                cacheFactory.getObject().shutdown();
            }
            final ScheduledExecutorService executor = appContext.getBean(ScheduledExecutorService.class);
            if (executor != null) {
                final List<Runnable> scheduledTask = executor.shutdownNow();
                log.info("{} Tasks are terminated successfully: {}", scheduledTask.size(), executor.isTerminated());
            }
        }
    }

}
