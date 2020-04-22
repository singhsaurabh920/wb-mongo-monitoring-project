package org.gps.integration.config;

import org.apache.log4j.Logger;
import org.gps.integration.task.MessageCountTask;
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

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;

@Configuration
@Profile("prod")
public class SmsSchedularConfig implements SchedulingConfigurer {
	private final static Logger logger = Logger.getLogger(SmsSchedularConfig.class);
	@Autowired
	private MessageCountTask tessageCountTask;

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
		threadPoolTaskScheduler.setPoolSize(2);
		threadPoolTaskScheduler.setThreadNamePrefix("Message-scheduler-thread");
		threadPoolTaskScheduler.initialize();
		threadPoolTaskScheduler.schedule(() -> tessageCountTask.updateDailyMessageInitialization(), new Trigger() {
			@Override
			public Date nextExecutionTime(TriggerContext triggerContext) {
				return new CronTrigger("0 0/30 * * * ?").nextExecutionTime(triggerContext);
			}
		});
		threadPoolTaskScheduler.schedule(() -> tessageCountTask.updateMonthlyMessageInitialization(), new Trigger() {
			@Override
			public Date nextExecutionTime(TriggerContext triggerContext) {
				return new CronTrigger(getNextCronOfLastDayOfMonth()).nextExecutionTime(triggerContext);
			}
		});
		threadPoolTaskScheduler.schedule(() -> tessageCountTask.updateMonthlyMessageInitialization(), new Trigger() {
			@Override
			public Date nextExecutionTime(TriggerContext triggerContext) {
				return new CronTrigger("0 0/30 0-12 1 * ?").nextExecutionTime(triggerContext);
			}
		});
	}

	public static String getNextCronOfLastDayOfMonth() {
		String cronExpression = "0 0/30 10-23 {0} * ?";
		return MessageFormat.format(cronExpression, getNoOfDayOfThisMonth());
	}

	public static int getNoOfDayOfThisMonth() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
	}

}
