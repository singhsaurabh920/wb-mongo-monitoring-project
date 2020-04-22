package org.gps.integration.scheduler;

import com.mongodb.client.result.DeleteResult;
import com.tb.core.enums.TBTimeZone;
import com.tb.core.util.CoreDateTimeUtils;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Date;

//@Component
//@Profile({"prod"})
public class DeleteRecordsScheduler {
	private final static Logger logger = Logger.getLogger(DeleteRecordsScheduler.class);
	@Autowired
	private MongoTemplate mongoTemplate;
	
	private ThreadPoolTaskScheduler deleteRecordsTaskScheduler;
	
	//@PostConstruct
	public void init() {
		deleteRecordsTaskScheduler =new ThreadPoolTaskScheduler();
		deleteRecordsTaskScheduler.setPoolSize(1);
		deleteRecordsTaskScheduler.setThreadNamePrefix("Delete-scheduler-thread");
		deleteRecordsTaskScheduler.initialize();
        deleteRecordsTaskScheduler.schedule(()-> deleteRecordsJob(), new Trigger() {
        	@Override
            public Date nextExecutionTime(TriggerContext triggerContext) {
        		return new CronTrigger("0 15 1 1 * ?").nextExecutionTime(triggerContext);
            }
        });
		logger.info("Delete records job Initalized");
	}

	private void deleteRecordsJob() {
		logger.info("Delete records job started");
		deleteLockReport();
		deleteKmSummaryRecords();
		//deleteTprHistoryRecords();
		deleteStatusReportRecords();
		deleteGeofenceReportRecords();
		deleteUnreachableReportRecords();
		deleteClientPushDataReportRecords();
		logger.info("Delete records job stopped");
	}
	
	private void deleteLockReport() {
		Date start=CoreDateTimeUtils.getUserDateSodBackDateInUTC(TBTimeZone.UTC.getTimeZoneID(), 6*30);
		Date end=CoreDateTimeUtils.getUserDateEodBackDateInUTC(TBTimeZone.UTC.getTimeZoneID(), 5*30);
		logger.info("LOCK REPORT DELETE S_DATE- "+start);
		logger.info("LOCK REPORT DELETE E_DATE- "+end);
		Query query = Query.query(Criteria.where("id").gte(new ObjectId(start)).lte(new ObjectId(end)));
		DeleteResult writeResult=mongoTemplate.remove(query,"lock_report");
		logger.info("LOCK REPORT DELETED-"+writeResult.getDeletedCount());
	}

	private void deleteKmSummaryRecords() {
		Date start=CoreDateTimeUtils.getUserDateSodBackDateInUTC(TBTimeZone.UTC.getTimeZoneID(), 13*30);
		Date end=CoreDateTimeUtils.getUserDateEodBackDateInUTC(TBTimeZone.UTC.getTimeZoneID(),12*30);
		logger.info("KM SUMMARY DELETE S_DATE- "+start);
		logger.info("KM SUMMARY DELETE E_DATE- "+end);
		Query query = Query.query(Criteria.where("id").gte(new ObjectId(start)).lte(new ObjectId(end)));
		DeleteResult writeResult=mongoTemplate.remove(query,"km_summary_report");
		logger.info("KM SUMMARY DELETED-"+writeResult.getDeletedCount());
	}
	
	private void deleteTprHistoryRecords() {
		Date start=CoreDateTimeUtils.getUserDateSodBackDateInUTC(TBTimeZone.UTC.getTimeZoneID(), 13*30);
		Date end=CoreDateTimeUtils.getUserDateEodBackDateInUTC(TBTimeZone.UTC.getTimeZoneID(), 12*30);
		logger.info("TPR HISTORY DELETE S_DATE- "+start);
		logger.info("TPR HISTORY DELETE E_DATE- "+end);
		Query query = Query.query(Criteria.where("id").gte(new ObjectId(start)).lte(new ObjectId(end)));
		DeleteResult writeResult=mongoTemplate.remove(query,"tpr_history");
		logger.info("TPR HISTORY DELETED-"+writeResult.getDeletedCount());
	}
	
	private void deleteStatusReportRecords() {
		Date start=CoreDateTimeUtils.getUserDateSodBackDateInUTC(TBTimeZone.UTC.getTimeZoneID(), 13*30);
		Date end=CoreDateTimeUtils.getUserDateEodBackDateInUTC(TBTimeZone.UTC.getTimeZoneID(), 12*30);
		logger.info("STATUS REPORT DELETE S_DATE- "+start);
		logger.info("STATUS REPORT DELETE E_DATE- "+end);
		Query query = Query.query(Criteria.where("id").gte(new ObjectId(start)).lte(new ObjectId(end)));
		DeleteResult writeResult=mongoTemplate.remove(query,"status_report");
		logger.info("STATUS REPORT DELETED-"+writeResult.getDeletedCount());
	}
	
	private void deleteGeofenceReportRecords() {
		Date start = CoreDateTimeUtils.getUserDateSodBackDateInUTC(TBTimeZone.UTC.getTimeZoneID(), 6 * 30);
		Date end = CoreDateTimeUtils.getUserDateEodBackDateInUTC(TBTimeZone.UTC.getTimeZoneID(), 5 * 30);
		logger.info("GEOFENCE REPORT DELETE S_DATE- " + start);
		logger.info("GEOFENCE REPORT DELETE E_DATE- " + end);
		Query query = Query.query(Criteria.where("id").gte(new ObjectId(start)).lte(new ObjectId(end)));
		DeleteResult writeResult = mongoTemplate.remove(query, "geofence_report");
		logger.info("GEOFENCE REPORT DELETED-" + writeResult.getDeletedCount());
	}
	
	private void deleteUnreachableReportRecords() {
		Date start = CoreDateTimeUtils.getUserDateSodBackDateInUTC(TBTimeZone.UTC.getTimeZoneID(), 6 * 30);
		Date end = CoreDateTimeUtils.getUserDateEodBackDateInUTC(TBTimeZone.UTC.getTimeZoneID(), 5 * 30);
		logger.info("UNREACHABLE REPORT DELETE S_DATE- " + start);
		logger.info("UNREACHABLE REPORT DELETE E_DATE- " + end);
		Query query = Query.query(Criteria.where("id").gte(new ObjectId(start)).lte(new ObjectId(end)));
		DeleteResult writeResult = mongoTemplate.remove(query, "unr_vehicle");
		logger.info("UNREACHABLE REPORT DELETED-" + writeResult.getDeletedCount());
	}
	
	private void deleteClientPushDataReportRecords() {
		Date start = CoreDateTimeUtils.getUserDateSodBackDateInUTC(TBTimeZone.UTC.getTimeZoneID(),  2* 30);
		Date end = CoreDateTimeUtils.getUserDateEodBackDateInUTC(TBTimeZone.UTC.getTimeZoneID(), 30);
		logger.info("CLIENT PUSH DATA REPORT DELETE S_DATE- " + start);
		logger.info("CLIENT PUSH DATA REPORT DELETE E_DATE- " + end);
		Query query = Query.query(Criteria.where("id").gte(new ObjectId(start)).lte(new ObjectId(end)));
		DeleteResult writeResult = mongoTemplate.remove(query, "client_push_data_report");
		logger.info("CLIENT PUSH DATA REPORT DELETED-" + writeResult.getDeletedCount());
	}
}
