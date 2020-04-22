package org.gps.integration.task;

import com.mongodb.bulk.BulkWriteResult;
import com.tb.core.domain.KmLog;
import com.tb.core.domain.StatusReport;
import com.tb.core.domain.Vehicle;
import com.tb.core.domain.service.TCPConnectionService;
import com.tb.core.domain.service.TerminalPacketRecordCurrentService;
import com.tb.core.domain.service.VehicleService;
import com.tb.core.enums.Km;
import com.tb.core.enums.TCPRequestType;
import com.tb.core.enums.TprGroup;
import com.tb.core.enums.VehicleStatus;
import com.tb.core.util.TprUtils;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.gps.core.utils.CustomTimezoneUtils;
import org.gps.integration.modal.VehicleMappedResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Profile({"dev","prod"})
public class ToDayKmTask {
	private final static Logger logger = Logger.getLogger(ToDayKmTask.class);
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	private TCPConnectionService tCPConnectionService;
	@Autowired
	private TerminalPacketRecordCurrentService terminalPacketRecordCurrentService;

	@PostConstruct
	public void init() {
		logger.info("Today km job Initalized");
	}
	
	public void updateToDayKm() {
		Date backDate = CustomTimezoneUtils.getBackDateFromCurrentDateInUTC(TimeUnit.MINUTES, 30);
		updateVehicleKmJob(backDate);
		updateStatusReportKmJob(backDate);

	}
	
	private void updateVehicleKmJob(Date backDate) {
		logger.info("Vehicle km job started");
		KmLog kmLog=new KmLog();
		kmLog.setName(Km.TODAY_KM);
		kmLog.setStart(new Date());
		mongoTemplate.save(kmLog);
		logger.info("Vehicle km date -" + backDate);
		Stream.of(TprGroup.values()).forEach(gp->{
			String tprCollection=TprUtils.getTprCurCollection(gp.getName());
			List<String> ouids=getModifiableVehicleKM(backDate,tprCollection);
			logger.info(gp+" vehicle size -" + ouids.size());
			long count=kmLog.getCount();
			kmLog.setCount(count+Long.valueOf(ouids.size()));
			updateVehicleKm(ouids,tprCollection);
		});
		kmLog.setEnd(new Date());
		mongoTemplate.save(kmLog);
		logger.info("Vehicle km job stopped");
	}
	
	
	private void updateVehicleKm(List<String> ouids,String tprCollection) {
		if (ouids!=null&&ouids.size() >0) {
			Date kmTPRStartDate = CustomTimezoneUtils.getIstDateSOD(0);
			Date kmTPREndDate = CustomTimezoneUtils.getCurrentDateInUTC();
			logger.info("Vehicle km start date -" + kmTPRStartDate);
			logger.info("Vehicle km end date -" + kmTPREndDate);
			final AtomicInteger counter = new AtomicInteger();
			Collection<List<String>> ouidsChunks = ouids.stream()
													  	.collect(Collectors.groupingBy(v->counter.getAndIncrement()/15))
													  	.values();
			ouidsChunks.forEach(chunk->{
				List<Pair<Query, Update>> updates=chunk.stream().map(ouid->{
					double distance=terminalPacketRecordCurrentService.getKm(ouid,kmTPRStartDate,new Date(),tprCollection);
					Query query = Query.query(Criteria.where("ouid").is(ouid));
					Update update = Update.update("todayKm", distance).set("tempTodayKm", 0);
					return Pair.of(query, update);
				}).collect(Collectors.toList());
				BulkWriteResult bulkWriteResult =mongoTemplate.bulkOps(BulkMode.UNORDERED, Vehicle.class, "vehicle")
						  									  .updateOne(updates)
						  									  .execute();
				logger.info("Vehicle km bulk update result-"+bulkWriteResult.getModifiedCount());
				List<Vehicle> vehicles=vehicleService.findByOuidInAndActive(chunk, true);
				tCPConnectionService.sendKmVehicleUpdate(TCPRequestType.KM, vehicles);
			});
		}
	}
	private List<String> getModifiableVehicleKM(Date backDate,String tprCollection){
		AggregationOptions aggregationOptions=Aggregation.newAggregationOptions().allowDiskUse(true).build();
		Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(Criteria.where("_id").gte(new ObjectId(backDate))),
															 Aggregation.group("ouid").first("ouid").as("ouid"),
															 Aggregation.project("ouid")).withOptions(aggregationOptions);
		List<VehicleMappedResult> vehicleMappedResults = mongoTemplate.aggregate(aggregation, tprCollection, VehicleMappedResult.class).getMappedResults();
		List<String> ouids = vehicleMappedResults.stream().map(VehicleMappedResult::getOuid).collect(Collectors.toList());
		return ouids;
	}
	
	private void updateStatusReportKmJob(Date end) {
		logger.info("Status report km job started");
		KmLog srLog=new KmLog();
		srLog.setName(Km.SR_KM);
		srLog.setStart(new Date());
		mongoTemplate.save(srLog);
		Date start = CustomTimezoneUtils.getBackDateFromDateInUTC(end,TimeUnit.MINUTES, 30);
		logger.info("Status report km  start date -"+start);
		logger.info("Status report km  end date -"+end);
		List<StatusReport> statusReports=getModifiableStatusReportKM(start,end);
		logger.info("Status Report size -"+statusReports.size());
		srLog.setCount(Long.valueOf(statusReports.size()));
		mongoTemplate.save(srLog);
		statusReports.forEach(sr->{	
			if(sr.getEndLocation()!=null) {
				String ouid = sr.getOuid();
				Date rStart = sr.getAdded();
				Date rEnd = sr.getEnded();
				String tprGroup=vehicleService.findTprGroup(ouid);
				String tprCollection=TprUtils.getTprCurCollection(tprGroup);
				double distance=terminalPacketRecordCurrentService.getKm(ouid,rStart,rEnd,tprCollection);
				logger.info("Status report ["+sr.getId()+"] km "+sr.getDistanceKM()+" to "+distance);
				Query query=Query.query(Criteria.where("id").is(sr.getId()));
				Update update=Update.update("diskm", distance);
				mongoTemplate.updateFirst(query,update,StatusReport.class);
			}
		});
		srLog.setEnd(new Date());
		mongoTemplate.save(srLog);
		logger.info("Status report km job stopped");
	}
	
	private List<StatusReport> getModifiableStatusReportKM(Date startDate,Date endDate){
		List<String> reportTypes=Arrays.asList(VehicleStatus.STARTED.getDbValue(),VehicleStatus.AC_ON.getDbValue(),VehicleStatus.OVERSPEED.getDbValue());
		Query query=Query.query(Criteria.where("ended").gte(startDate).lte(endDate).and("r_type").in(reportTypes));
		query.fields().include("id").include("ouid").include("diskm").include("added").include("ended").include("endLocation");
		List<StatusReport> statusReport=mongoTemplate.find(query, StatusReport.class);
		return statusReport;
	}
}
