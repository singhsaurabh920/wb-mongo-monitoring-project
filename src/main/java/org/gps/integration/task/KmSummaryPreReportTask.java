package org.gps.integration.task;

import com.mongodb.client.result.DeleteResult;
import com.tb.core.domain.*;
import com.tb.core.domain.service.KmSummaryReportService;
import com.tb.core.domain.service.TrackobitUserService;
import com.tb.core.domain.service.VehicleService;
import com.tb.core.enums.Km;
import com.tb.core.enums.RoleTypes;
import com.tb.core.enums.TBTimeZone;
import com.tb.core.util.CoreDateTimeUtils;
import com.tb.core.util.TprUtils;
import org.apache.log4j.Logger;
import org.gps.core.utils.ConversionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Component
@Profile({"dev","prod"})
public class KmSummaryPreReportTask{
	private final static Logger logger = Logger.getLogger(KmSummaryPreReportTask.class);
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	private TrackobitUserService trackobitUserService;
	@Autowired
	private KmSummaryReportService kmSummaryReportService;
	
	@PostConstruct
	public void init() {
		KmTime preKmTime=mongoTemplate.findOne(Query.query(Criteria.where("name").is(Km.PRE_KM)), KmTime.class);
		if(preKmTime==null) {
			preKmTime=new KmTime();
			preKmTime.setName(Km.PRE_KM);
			preKmTime.setAdded(new Date());
		}
		preKmTime.setUpdated(new Date());
		preKmTime.setStartTime(CoreDateTimeUtils.startKmEpochTime());
		preKmTime.setEndTime(CoreDateTimeUtils.endKmEpochTime());
		mongoTemplate.save(preKmTime);
		logger.info("Km pre job Initalized");
	}
	
	public void createZonedKmReport() {
		logger.info("Km pre job started");
		Query query=Query.query(Criteria.where("name").is(Km.PRE_KM));
		KmTime preKmTime=mongoTemplate.findOne(query, KmTime.class);
		long startEpoch=preKmTime.getStartTime();
		logger.info("KM REPORT S_ELIGIBLE DATE- "+new Date(startEpoch));
		long endEpoch=preKmTime.getEndTime();
		logger.info("KM REPORT E_ELIGIBLE DATE- "+new Date(endEpoch));
		preKmTime.setUpdated(new Date());
		preKmTime.setStartTime(CoreDateTimeUtils.startKmEpochTime());
		preKmTime.setEndTime(CoreDateTimeUtils.endKmEpochTime());
		mongoTemplate.save(preKmTime);
		Predicate<Long> predicate=(tzStartDayEpoch)->tzStartDayEpoch>startEpoch&&tzStartDayEpoch<=endEpoch;
		for(TBTimeZone timeZone:TBTimeZone.values()) {
			String timeZoneID=timeZone.getTimeZoneID();
			try {
				Date tzSodDateInUtc=CoreDateTimeUtils.getUserDateSodDateInUTC(timeZoneID);
				//logger.info("DAILY KM TIME[ "+tzSodDateInUtc.getTime()+" ]"+tzSodDateInUtc);
				if(predicate.test(tzSodDateInUtc.getTime())) {
					logger.info("KM REPORT ELIGIBLE TIMEZONE - " +tzSodDateInUtc+" [ "+ConversionUtils.displayTimeZone(timeZoneID)+" ] ");
					createKmReport(timeZoneID);
				}
			}catch (Exception e) {
				logger.error("EXCEPTION IN ZONNED KM REPORT ["+ConversionUtils.displayTimeZone(timeZoneID)+" ]",e);
			}	
		}
		logger.info("Km pre job stopped");
	}

	private void createKmReport(String timezoneId) {
		KmLog log=new KmLog();
		log.setName(Km.PRE_KM);
		log.setZone(timezoneId);
		log.setStart(new Date());
		mongoTemplate.save(log);
		List<TrackobitUser> trackobitUsers=trackobitUserService.findByTimezoneAndRolesName(timezoneId,RoleTypes.ROLE_TRANSPORTER.name());
		logger.info("TRANSPORTER SIZE - "+trackobitUsers.size());
		Map<String,Integer> transporterMap=new HashMap<>();
		for(TrackobitUser trackobitUser:trackobitUsers) {
			Config config=trackobitUser.getConfig();
			if(config==null) {
				transporterMap.put(trackobitUser.getUsername(),0);
			}else {
				transporterMap.put(trackobitUser.getUsername(),config.getDistanceVariation());
			}	
		}
		Date kmReportDate=CoreDateTimeUtils.getSodDateBackInUserTimeZone(timezoneId,1);
		logger.info("KM REPORT DATE- "+kmReportDate);
		Date cTprDeleteStart=CoreDateTimeUtils.getUserDateSodBackDateInUTC(timezoneId,2);
		Date cTprDeleteEnd=CoreDateTimeUtils.getUserDateEodBackDateInUTC(timezoneId, 2);
		logger.info("CURRENT TPR DELETE S_DATE - "+cTprDeleteStart);
		logger.info("CURRENT TPR DELETE E_DATE - "+cTprDeleteEnd);
		List<Vehicle> vehicles = vehicleService.findByTimezone(timezoneId);
		logger.info("VEHICLE SIZE - "+vehicles.size());
		log.setCount(Long.valueOf(vehicles.size()));
		mongoTemplate.save(log);
		for (Vehicle vehicle : vehicles) {
			try {
				Integer distanceVariation=transporterMap.get(vehicle.getTransUserName());
				long totDur = vehicle.getTotDu();
				double totKm = vehicle.getTotkm();
				double totFuelConsumed = vehicle.getTotFuelConsumed();
				long duration = totDur - vehicle.getStartDayDu();
				double distance =vehicle.getTodayKm()+vehicle.getTempTodayKm();
				double fuel=totFuelConsumed-vehicle.getStartDayFuelConsumed();
				if(distanceVariation!=null) {
				  distance=distance+(distance*distanceVariation)/100;
				}
				KmSummaryReport kmSummaryReport = new KmSummaryReport();
				kmSummaryReport.setOuid(vehicle.getOuid());
				kmSummaryReport.setAdded(kmReportDate);
				kmSummaryReport.setDistance(distance);
				kmSummaryReport.setDuration(duration);
				vehicle.setTodayKm(0);
				vehicle.setTempTodayKm(0);
				vehicle.setStartDayDu(totDur);
				vehicle.setTotkm(totKm+distance);
				vehicle.setStartDayFuelConsumed(totFuelConsumed);
				vehicleService.save(vehicle);
				kmSummaryReportService.save(kmSummaryReport);
				logger.info(vehicle.getVehicleNo() + " - KM REPORT CREATED ["+distance+"]");
				DeleteResult writeResult=deleteRecordInCurrentTpr(vehicle,cTprDeleteStart,cTprDeleteEnd);
				logger.info(vehicle.getVehicleNo()+" - CURRENT TPR DELETED ["+writeResult.getDeletedCount()+"]");
			} catch (Exception e) {
				logger.info("EXCEPTION IN VEHILE KM REPORT [ "+ vehicle.getVehicleNo()+" ]",e);
			}
		}
		log.setEnd(new Date());
		mongoTemplate.save(log);
	}
	
	private DeleteResult deleteRecordInCurrentTpr(Vehicle vehicle, Date start, Date end){
		Query query=Query.query(Criteria.where("ouid").is(vehicle.getOuid()).and("add").gte(start).lte(end));
		return mongoTemplate.remove(query, TprUtils.getTprCurCollection(vehicle.getTprGroup()));
	}

}