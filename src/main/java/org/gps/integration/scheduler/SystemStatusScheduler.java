package org.gps.integration.scheduler;

import com.tb.core.domain.Vehicle;
import com.tb.core.domain.service.VehicleService;
import com.tb.core.enums.DurationUnit;
import com.tb.core.util.CoreUtil;
import org.apache.log4j.Logger;
import org.gps.core.gcm.service.VehicleMetaPushSenderService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;

@Component
@Profile("prod")
public class SystemStatusScheduler {
	private final static Logger logger = Logger.getLogger(SystemStatusScheduler.class);
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	private VehicleMetaPushSenderService vehicleMetaPushSenderService;
	
	@PostConstruct
	public void init() {
		logger.info("System status job Initalized");
	}
	
	@Scheduled(fixedRate = 1000 * 60 *5)
	public void checkTcpStatus() {
		logger.info("TCP STATUS SCHEDULER STARTED");
		Query query=new Query();
		query.fields().include("lu");
		List<Vehicle> vehicles=vehicleService.find(query);
		logger.info("Total vehicle size- "+vehicles.size());
		Date date=new DateTime(DateTimeZone.UTC).toDate();
		for(Vehicle vehicle:vehicles) {
			int intreval=CoreUtil.calculateDuration(vehicle.getLu(),date,DurationUnit.MINUTE);
			if(intreval<5&&intreval>0){
				logger.info("Interval-"+intreval);
				return;
			}
		}
		vehicleMetaPushSenderService.sendSystemStatuseEmail("Tcp stopped");
		logger.info("TCP STATUS SCHEDULER STOPPED");
	}

}
