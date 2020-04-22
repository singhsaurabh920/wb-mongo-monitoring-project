package org.gps.integration.task;

import com.mongodb.client.result.UpdateResult;
import com.tb.core.domain.KmTime;
import com.tb.core.domain.TrackobitUser;
import com.tb.core.domain.service.SmsAllocationDetailService;
import com.tb.core.domain.service.TrackobitUserService;
import com.tb.core.enums.Km;
import com.tb.core.enums.RoleTypes;
import com.tb.core.enums.TBTimeZone;
import com.tb.core.util.CoreDateTimeUtils;
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
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Profile("prod")
public class MessageCountTask {
	private final static Logger logger = Logger.getLogger(MessageCountTask.class);
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private TrackobitUserService trackobitUserService;
	@Autowired
	private SmsAllocationDetailService smsAllocationDetailService;


	@PostConstruct
	public void init() {
		KmTime dailyCountUpdateTime = mongoTemplate.findOne(Query.query(Criteria.where("name").is(Km.DAILY_MESSAGE_INITIALIZE)), KmTime.class);
		if (dailyCountUpdateTime == null) {
			dailyCountUpdateTime = new KmTime();
			dailyCountUpdateTime.setName(Km.DAILY_MESSAGE_INITIALIZE);
			dailyCountUpdateTime.setAdded(new Date());
		}
		dailyCountUpdateTime.setUpdated(new Date());
		dailyCountUpdateTime.setStartTime(CoreDateTimeUtils.startKmEpochTime());
		dailyCountUpdateTime.setEndTime(CoreDateTimeUtils.endKmEpochTime());
		mongoTemplate.save(dailyCountUpdateTime);
		logger.info("Daily message count initializer task Initalized");
	}

	public void updateDailyMessageInitialization() {
		logger.info("Daily Message count update job started");
		Query query = Query.query(Criteria.where("name").is(Km.DAILY_MESSAGE_INITIALIZE));
		KmTime dailyCountUpdateTime = mongoTemplate.findOne(query, KmTime.class);
		long startEpoch = dailyCountUpdateTime.getStartTime();
		logger.info("DAILY MESSAGE REPORT START_ELIGIBLE DATE- " + new Date(startEpoch));
		long endEpoch = dailyCountUpdateTime.getEndTime();
		logger.info("DAILY MESSAGE REPORT END_ELIGIBLE DATE- " + new Date(endEpoch));
		dailyCountUpdateTime.setUpdated(new Date());
		dailyCountUpdateTime.setStartTime(CoreDateTimeUtils.startKmEpochTime());
		dailyCountUpdateTime.setEndTime(CoreDateTimeUtils.endKmEpochTime());
		mongoTemplate.save(dailyCountUpdateTime);
		Predicate<Long> predicate = (tzStartDayEpoch) -> tzStartDayEpoch > startEpoch && tzStartDayEpoch <= endEpoch;
		for (TBTimeZone timeZone : TBTimeZone.values()) {
			String timeZoneID = timeZone.getTimeZoneID();
			try {
				Date tzSodDateInUtc = CoreDateTimeUtils.getUserDateSodDateInUTC(timeZoneID);
				if (predicate.test(tzSodDateInUtc.getTime())) {
					logger.info("DAILY MESSAGE CONFIG ELIGIBLE TIMEZONE - " + tzSodDateInUtc + " [ "
							+ ConversionUtils.displayTimeZone(timeZoneID) + " ] ");
					updateDailyMessageCount(timeZoneID);
				}
			} catch (Exception e) {
				logger.error("EXCEPTION IN ZONNED DAILY MESSAGE CONFIG REPORT [" + ConversionUtils.displayTimeZone(timeZoneID)
						+ " ]", e);
			}
		}
		logger.info("Daily Message config job stopped");
	}

	private UpdateResult updateDailyMessageCount(String timeZoneID) {
		List<String> transporter = trackobitUserService
				.findByTimezoneAndRolesName(timeZoneID, RoleTypes.ROLE_TRANSPORTER.name()).stream()
				.map(TrackobitUser::getUsername).collect(Collectors.toList());
		List<String> client = trackobitUserService.findByTimezoneAndRolesName(timeZoneID, RoleTypes.ROLE_CLIENT.name())
				.stream().map(TrackobitUser::getUsername).collect(Collectors.toList());
		List<String> trackobitUsers = Stream.of(transporter, client).flatMap(t -> t.stream())
				.collect(Collectors.toList());
		return smsAllocationDetailService.updateDailyCounterByUsername(trackobitUsers);
	}

	public void updateMonthlyMessageInitialization() {
		logger.info("Monthly Message count update job started");
		long startEpoch = CoreDateTimeUtils.currentJobStartEpochTime();
		long endEpoch = CoreDateTimeUtils.currentJobEndEpochTime();
		Predicate<Long> predicate = (tzStartDayEpoch) -> tzStartDayEpoch > startEpoch && tzStartDayEpoch <= endEpoch;
		for (TBTimeZone timeZone : TBTimeZone.values()) 	{
			String timeZoneID = timeZone.getTimeZoneID();
			try {
				Date tzSodDateInUtc = CoreDateTimeUtils.getUserDateSodDateInUTC(timeZoneID);
				if (predicate.test(tzSodDateInUtc.getTime())) {
					logger.info("MONTHLY MESSAGE CONFIG ELIGIBLE TIMEZONE - " + tzSodDateInUtc + " [ "+ ConversionUtils.displayTimeZone(timeZoneID) + " ] ");
					updateMonthlyMessageCount(timeZoneID);
				}
			} catch (Exception e) {
				logger.error("EXCEPTION IN ZONNED MONTHLY MESSAGE CONFIG REPORT ["+ ConversionUtils.displayTimeZone(timeZoneID) + " ]", e);
			}
		}
		logger.info("Monthly Message config job stopped");
	}

	private UpdateResult updateMonthlyMessageCount(String timeZoneID) {
		List<String> transporter = trackobitUserService.findByTimezoneAndRolesName(timeZoneID, RoleTypes.ROLE_TRANSPORTER.name()).stream()
				.map(TrackobitUser::getUsername).collect(Collectors.toList());
		List<String> client = trackobitUserService.findByTimezoneAndRolesName(timeZoneID, RoleTypes.ROLE_CLIENT.name())
				.stream().map(TrackobitUser::getUsername).collect(Collectors.toList());
		List<String> trackobitUsers = Stream.of(transporter, client).flatMap(t -> t.stream()).collect(Collectors.toList());
		return smsAllocationDetailService.updatemonthlyCounterByUsername(trackobitUsers);
	}

}
