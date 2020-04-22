package org.gps.integration.scheduler;

import com.tb.core.domain.Vehicle;
import com.tb.core.domain.VehicleServiceAndMaintenanceAlerts;
import com.tb.core.domain.service.VehicleService;
import com.tb.core.domain.service.VehicleServiceAndMaintenanceAlertsService;
import com.tb.core.enums.NotificationType;
import com.tb.core.enums.ServiceType;
import org.apache.log4j.Logger;
import org.gps.core.gcm.service.VehicleMetaPushSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Component
@Profile("prod")
public class NotificationSendScheduler {
	private final static Logger logger = Logger.getLogger(NotificationSendScheduler.class);
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	private VehicleMetaPushSenderService vehicleMetaPushSenderService;
	@Autowired
	private VehicleServiceAndMaintenanceAlertsService vehicleServiceAndMaintenanceAlertsService;
	
	@PostConstruct
	public void init() {
		logger.info("Notification sender job Initalized");
	}

	@Scheduled(cron = "0 30 01 * * ?")
	public void sendNotificationScheduler() {
		logger.info("Notification schedular started");
		checkForMetaServicesDue();
		logger.info("Notification schedular stopped");
	}
	
	private void checkForMetaServicesDue() {
		LocalDateTime localDateTime = LocalDate.now().atStartOfDay();
		List<Vehicle> vehiclesList = vehicleService.findByActive(true);
		List<VehicleServiceAndMaintenanceAlerts> alerts = vehicleServiceAndMaintenanceAlertsService.findAll();
		int dateDifference;
		for (Vehicle vehicle : vehiclesList) {
			dateDifference = getDaysDifference(vehicle.getSubscriptionDue(), localDateTime);
			if (isNotication(dateDifference)) {
				vehicleMetaPushSenderService.sendNotification(NotificationType.VEHICLE_SUBSCRIPTION, dateDifference,vehicle);
				vehicleMetaPushSenderService.send_email(NotificationType.VEHICLE_SUBSCRIPTION, vehicle,vehicle.getTransporterUsername(), dateDifference + "");
				vehicleMetaPushSenderService.send_email(NotificationType.VEHICLE_SUBSCRIPTION, vehicle,vehicle.getClientUsername(), dateDifference + "");
			}
			for (VehicleServiceAndMaintenanceAlerts vehicleAlerts : alerts) {
				if (vehicleAlerts.getVehicleOuid().equals(vehicle.getOuid())) {
					sendAlerts(vehicleAlerts, localDateTime, vehicle);

				}
			}
		}
	}

	private void sendAlerts(VehicleServiceAndMaintenanceAlerts alert, LocalDateTime localDateTime, Vehicle vehicle) {
		String type = alert.getServiceType();
		if (ServiceType.VEHICLE_SERVICE_ALERT.getDbValue().equals(type)) {
			int dateDifference = getDaysDifference(alert.getLastServiceDate(), localDateTime);
			logger.info("Service date diff " + dateDifference);
			checkForServiceDue(dateDifference, vehicle, alert);
		} else {
			int dateDifference = getDaysDifference(alert.getDueDate(), localDateTime);
			if (dateDifference<=7) {
				vehicleMetaPushSenderService.sendNotification(NotificationType.findByDbValue(type), dateDifference,
						vehicle);
				vehicleMetaPushSenderService.send_email(NotificationType.findByDbValue(type), vehicle,
						alert.getCreatedBy(), dateDifference + "");
			}
		}

	}

	private int getDaysDifference(Date vehicleMetaServiceDueDate, LocalDateTime currentDate) {
		int dateDifference = -1;
		if (vehicleMetaServiceDueDate == null) {
			return dateDifference;
		}
		LocalDateTime vehicleMetaServiceDueLocalDateTime = vehicleMetaServiceDueDate.toInstant()
				.atZone(ZoneId.systemDefault()).toLocalDateTime();
		Duration duration = Duration.between(currentDate, vehicleMetaServiceDueLocalDateTime);
		dateDifference = (int) duration.toDays();
		return dateDifference;
	}

	private void checkForServiceDue(int dateDifference, Vehicle vehicle, VehicleServiceAndMaintenanceAlerts alert) {
		try {
			Date dateObject = alert.getLastServiceDate();
			SimpleDateFormat format = new SimpleDateFormat("dd/mm/yyyy", Locale.ENGLISH);
			String lstSerDate = format.format(dateObject);
			// service due date differece will be negative -----
			if (dateDifference == -180 || dateDifference == -240 || dateDifference == -360) {
				vehicleMetaPushSenderService.sendServiceNotification(NotificationType.VEHICLE_SERVICE_TIME, vehicle,
						lstSerDate);
				vehicleMetaPushSenderService.send_email(NotificationType.VEHICLE_SERVICE_TIME, vehicle,
						alert.getCreatedBy(), lstSerDate);
				return;
			}
			int lstSevOdo = alert.getOdometer();
			int odometerDifference = vehicle.getOdometer() - lstSevOdo;
			if (odometerDifference >= 10000 && odometerDifference <= 10100) {
				vehicleMetaPushSenderService.sendServiceNotification(NotificationType.VEHICLE_SERVICE_ODOMETER, vehicle,
						String.valueOf(lstSevOdo));
				vehicleMetaPushSenderService.send_email(NotificationType.VEHICLE_SERVICE_ODOMETER, vehicle,
						alert.getCreatedBy(), String.valueOf(lstSevOdo));
			}
		} catch (Exception e) {
			logger.debug(e.getMessage(), e);
		}
	}

	private boolean isNotication(int dateDifference) {
		if (dateDifference == 1 || dateDifference == 7 || dateDifference == 15 || dateDifference == 30) {
			return true;
		}
		return false;
	}

}
