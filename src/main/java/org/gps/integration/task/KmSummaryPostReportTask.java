package org.gps.integration.task;

import com.tb.core.domain.*;
import com.tb.core.domain.service.KmSummaryReportService;
import com.tb.core.domain.service.TerminalPacketRecordCurrentService;
import com.tb.core.domain.service.VehicleService;
import com.tb.core.enums.Km;
import com.tb.core.enums.TBTimeZone;
import com.tb.core.util.CoreDateTimeUtils;
import com.tb.core.util.CoreUtil;
import com.tb.core.util.TprUtils;
import org.apache.log4j.Logger;
import org.gps.core.utils.ConversionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Component
@Profile({"dev","prod"})
public class KmSummaryPostReportTask {
	private final static Logger logger = Logger.getLogger(KmSummaryPostReportTask.class);
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	@Qualifier("scheduledExecutorService")
	private ScheduledExecutorService executor;
	@Autowired
	private KmSummaryReportService kmSummaryReportService;
	@Autowired
	private TerminalPacketRecordCurrentService terminalPacketRecordCurrentService; 
	
	@PostConstruct
	public void init() {
		//executor.schedule( () -> createBackKmSummaryReportScheduler(), 15, TimeUnit.SECONDS);
		KmTime postKmTime=mongoTemplate.findOne(Query.query(Criteria.where("name").is(Km.POST_KM)), KmTime.class);
		if(postKmTime==null) {
			postKmTime=new KmTime();
			postKmTime.setName(Km.POST_KM);
			postKmTime.setAdded(new Date());
		}
		postKmTime.setUpdated(new Date());
		postKmTime.setStartTime(CoreDateTimeUtils.startKmEpochTime());
		postKmTime.setEndTime(CoreDateTimeUtils.endKmEpochTime());
		mongoTemplate.save(postKmTime);
		logger.info("Km post job Initalized");
	}
	
	public void createZonedPostKmReport() {
		logger.info("Km post job started");
		Query query=Query.query(Criteria.where("name").is(Km.POST_KM));
		KmTime postKmTime=mongoTemplate.findOne(query, KmTime.class);
		long startEpoch=postKmTime.getStartTime();
		logger.info("KM POST REPORT S_ELISIBLE DATE[ "+startEpoch+" ]"+new Date(startEpoch));
		long endEpoch=postKmTime.getEndTime();
		logger.info("KM POST REPORT E_ELISIBLE DATE[ "+endEpoch+" ]"+new Date(endEpoch));
		postKmTime.setUpdated(new Date());
		postKmTime.setStartTime(CoreDateTimeUtils.startKmEpochTime());
		postKmTime.setEndTime(CoreDateTimeUtils.endKmEpochTime());
		mongoTemplate.save(postKmTime);
		Predicate<Long> predicate=(tzStartDayEpoch)->tzStartDayEpoch>startEpoch&&tzStartDayEpoch<=endEpoch;
		for(TBTimeZone timeZone:TBTimeZone.values()) {
			String timeZoneID=timeZone.getTimeZoneID();
			try {
				Date tzSodDateInUtc=CoreDateTimeUtils.addTimeInDate(CoreDateTimeUtils.getUserDateSodDateInUTC(timeZoneID),2,TimeUnit.HOURS);
				//logger.info("POST KM TIME[ "+tzSodDateInUtc.getTime()+" ]"+tzSodDateInUtc);
				if(predicate.test(tzSodDateInUtc.getTime())) {
					logger.info("KM POST REPORT ELIGIBLE TIMEZONE - " +tzSodDateInUtc+" [ "+ConversionUtils.displayTimeZone(timeZoneID)+" ] ");
					createPostKmReport(timeZoneID);
				}
			}catch (Exception e) {
				logger.error("EXCEPTION IN ZONNED KM REPORT ["+ConversionUtils.displayTimeZone(timeZoneID)+" ]",e);
			}
		}
		logger.info("Km post job stopped");
	}
	

	private void createPostKmReport(String timezoneId) {
		KmLog log=new KmLog();
		log.setName(Km.POST_KM);
		log.setZone(timezoneId);
		log.setStart(new Date());
		mongoTemplate.save(log);
		Date kmReportDate=CoreDateTimeUtils.getSodDateBackInUserTimeZone(timezoneId,1);
		logger.info("KM REPORT DATE- "+kmReportDate);
		Date start = CoreDateTimeUtils.getUserDateSodBackDateInUTC(timezoneId, 1);
		Date end = CoreDateTimeUtils.getUserDateEodBackDateInUTC(timezoneId, 1);
		logger.info("KM REPORT S_DATE- " + start);
		logger.info("KM REPORT E_DATE- " + end);
		List<Vehicle> vehicles = vehicleService.findByTimezone(timezoneId);
		logger.info("VEHICLE SIZE- "+vehicles.size());
		log.setCount(Long.valueOf(vehicles.size()));
		mongoTemplate.save(log);
		for (Vehicle vehicle : vehicles) {
			try {
				String ouid = vehicle.getOuid();
				String tprGroup = vehicle.getTprGroup();
				KmSummaryReport kmSummaryReport = kmSummaryReportService.findByOuidAndAdded(ouid, kmReportDate);
				if (kmSummaryReport == null) {
					continue;
				}
				double distance = terminalPacketRecordCurrentService.getKm(ouid,start,end,TprUtils.getTprCurCollection(tprGroup));
				logger.info(vehicle.getVehicleNo() + " TPR DISTANCE : " + distance+ " KM REPORT DISTANCE : " + kmSummaryReport.getDistance());
				kmSummaryReport.setDistance(distance);
				kmSummaryReportService.save(kmSummaryReport);
			} catch (Exception e) {
				logger.error("EXCEPTION IN VEHILE KM POST REPORT [ "+ vehicle.getVehicleNo()+" ]", e);
			}
		}
		log.setEnd(new Date());
		mongoTemplate.save(log);
	}
	
	
	private  void createBackKmSummaryReportScheduler() {
		logger.info("Km back job started");
		List<Vehicle> vehicles=vehicleService.findAll();
		logger.info("Vehicle list-"+vehicles.size());
		for(int i=1; i>=1;i--) {
			Date start = CoreDateTimeUtils.getUserDateSodBackDateInUTC(TBTimeZone.IST.getTimeZoneID(), i);
			Date end = CoreDateTimeUtils.getUserDateEodBackDateInUTC(TBTimeZone.IST.getTimeZoneID(), i);
			Date kmReportDate=CoreDateTimeUtils.getUserDateSodBackDateInUTC(TBTimeZone.UTC.getTimeZoneID(),i);
			logger.info("KM REPORT S_DATE- " + start);
			logger.info("KM REPORT E_DATE- " + end);
			logger.info("KM REPORT DATE- "+kmReportDate);
			//createBackKmSummaryReport(vehicles,kmReportDate,start,end);
		}
		logger.info("Km back job stopped");
	}
	
	
	private void createBackKmSummaryReport(List<Vehicle> vehicles,Date kmReportDate,Date start,Date end) {
		for(Vehicle vehicle:vehicles) {
			//createBackKmSummaryReport(vehicle,kmReportDate,start,end);
		}
	}
	
	private void createBackKmSummaryReport(Vehicle vehicle,Date kmReportDate,Date start,Date end) {
		try {
			String ouid = vehicle.getOuid();
			String tprGroup = vehicle.getTprGroup();
			double distance = terminalPacketRecordCurrentService.getKm(ouid,start,end,TprUtils.getTprCurCollection(tprGroup));
			long duration = CoreUtil.calculateDuration(distance);
			KmSummaryReport kmSummaryReport = kmSummaryReportService.findByOuidAndAdded(ouid, kmReportDate);
			if(kmSummaryReport==null) {
				kmSummaryReport = new KmSummaryReport();
			} 
			kmSummaryReport.setOuid(ouid);
			kmSummaryReport.setDistance(distance);
			kmSummaryReport.setDuration(duration);
			kmSummaryReport.setAdded(kmReportDate);
			kmSummaryReportService.save(kmSummaryReport);
			logger.info(vehicle.getVehicleNo() + " - KM BACK REPORT CREATED ["+distance+"] "+kmReportDate);
		} catch (Exception e) {
			logger.error("EXCEPTION IN VEHILE KM BACK REPORT [ "+ vehicle.getVehicleNo()+" ]", e);
		}
	}

}
