package org.gps.integration.scheduler;

import com.tb.core.domain.service.NotificationRecordService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Calendar;

@Component
@Profile("prod")
public class NotificationDeleteScheduler {
	private final static Logger logger = Logger.getLogger(NotificationDeleteScheduler.class);
	@Autowired
	private NotificationRecordService notificationRecordService;
	
	@PostConstruct
	public void init() {
		logger.info("Notification delete job Initalized");
	}

	
	@Scheduled(cron = "0 0 3 * * ?")
	public void deleteNotifications() throws Exception {
		logger.info("Delete notification schedular started");
		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -3);
		Long recordDeleted = notificationRecordService.deleteByAddedLessThan(cal.getTime());
		logger.info("Notification deleted-" + recordDeleted);
		logger.info("Delete notification schedular stopped");
	}
}
