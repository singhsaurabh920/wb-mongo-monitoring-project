package org.gps.integration.config;

import org.apache.log4j.Logger;
import org.gps.integration.task.KmSummaryPostReportTask;
import org.gps.integration.task.KmSummaryPreReportTask;
import org.gps.integration.task.ToDayKmTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Date;

@Configuration
@Profile({"dev","prod"})
public class KmSchedulerConfig implements SchedulingConfigurer{	
	private final static Logger logger = Logger.getLogger(KmSchedulerConfig.class);

	@Value("${cron.km.pre}")
	private String cronKmPre;
	@Value("${cron.km.post}")
	private String cronKmPost;
	@Value("${cron.km.today}")
	private String cronKmToday;
	@Autowired
	private ToDayKmTask toDayKmTask;
	@Autowired
	private KmSummaryPreReportTask kmSummaryPreReportTask;
	@Autowired
	private KmSummaryPostReportTask kmSummaryPostReportTask;

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler threadPoolTaskScheduler =new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(3);
        threadPoolTaskScheduler.setThreadNamePrefix("Km-scheduler-thread");
        threadPoolTaskScheduler.initialize();
        threadPoolTaskScheduler.schedule(()->toDayKmTask.updateToDayKm(), new Trigger() {
        	@Override
            public Date nextExecutionTime(TriggerContext triggerContext) {
        		return new CronTrigger(cronKmToday).nextExecutionTime(triggerContext);
            }
        });
        threadPoolTaskScheduler.schedule(()->kmSummaryPreReportTask.createZonedKmReport(), new Trigger() {
        	@Override
            public Date nextExecutionTime(TriggerContext triggerContext) {
        		return new CronTrigger(cronKmPre).nextExecutionTime(triggerContext);
            }
        });
        threadPoolTaskScheduler.schedule(()->kmSummaryPostReportTask.createZonedPostKmReport(), new Trigger() {
        	@Override
            public Date nextExecutionTime(TriggerContext triggerContext) {
        		return new CronTrigger(cronKmPost).nextExecutionTime(triggerContext);
            }
        });
        taskRegistrar.setTaskScheduler(threadPoolTaskScheduler);
	}
}
