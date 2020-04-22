package org.gps.integration.scheduler;

import com.mongodb.client.result.UpdateResult;
import com.tb.core.constants.Tpr;
import com.tb.core.domain.*;
import com.tb.core.domain.repository.TerminalPacketRecordHistoryRepository;
import com.tb.core.domain.repository.TerminalPacketRecordRepository;
import org.apache.log4j.Logger;
import org.gps.core.utils.CustomTimezoneUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Profile({"prod"})
public class HistoryScheduler {
	private final static Logger logger = Logger.getLogger(HistoryScheduler.class);
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired 
	@Qualifier("tprExecutorService")
	private ScheduledExecutorService scheduledExecutorService;
	@Autowired
	private TerminalPacketRecordRepository terminalPacketRecordRepository;
	@Autowired
	TerminalPacketRecordHistoryRepository terminalPacketRecordHistoryRepository;
	
	@PostConstruct
	public void init() {
		KmTime skipCount=mongoTemplate.findOne(Query.query(Criteria.where("name").is(Tpr.VEHICLE_SKIP_COUNT)), KmTime.class);
		if(skipCount==null) {
			skipCount=new KmTime();
			skipCount.setName(Tpr.VEHICLE_SKIP_COUNT);
			skipCount.setSkipCount(20260);
			skipCount.setAdded(new Date());
			skipCount.setUpdated(new Date());
			mongoTemplate.save(skipCount);
		}
		scheduledExecutorService.scheduleWithFixedDelay(() -> moveTPRtohistoryTPR(), 10, 60, TimeUnit.SECONDS);
		logger.info("Tpr history job Initalized");
	}
	
	@Scheduled(cron="0 0 0 * * ?")
	public void initalizeTprHistorySyncing() {
		logger.info("TPR HISTORY INITALIZER STARTED");
		logger.info("CURREN DATE-"+new Date());
		Update update=Update.update("skipCount", 0);
		UpdateResult updateResult=mongoTemplate.updateFirst(Query.query(Criteria.where("name").is(Tpr.VEHICLE_SKIP_COUNT)),update, KmTime.class);
		logger.info("Updated count- "+updateResult.getModifiedCount());
		logger.info("TPR HISTORY INITALIZER STOPPED");
	}

	public void moveTPRtohistoryTPR() {
		logger.info("TPR HISTORY SCEDULER STARTED");
		Date sDate = CustomTimezoneUtils.getBackDateSODInUTC(61);
		Date eDate = CustomTimezoneUtils.getBackDateSODInUTC(60);
		logger.info("Start date==" + sDate + ", End date==" + eDate);
		tprToTprHistory(sDate,eDate);
		logger.info("TPR HISTORY SCEDULER STOPPED");
	}

	private void tprToTprHistory(Date sDate, Date eDate) {
		KmTime skipCountDoc=mongoTemplate.findOne(Query.query(Criteria.where("name").is(Tpr.VEHICLE_SKIP_COUNT)),KmTime.class);
		long skipCount=skipCountDoc.getSkipCount();
		logger.info("Vehicle skip count- "+skipCount );
		Query query=new Query();
		query.fields().include("ouid");
		query.skip(skipCount).limit(100);
		LinkedList<String> ouids = mongoTemplate.find(query,Vehicle.class).stream().map(Vehicle::getOuid).collect(Collectors.toCollection(LinkedList::new));		
		logger.info("Vehicle size-"+ouids.size());
		if (ouids.size()==0) {
			return;
		}
		Query query1=Query.query(Criteria.where("name").is(Tpr.VEHICLE_SKIP_COUNT));
		Update update1=Update.update("skipCount", skipCount+100).set("updated", new Date());
		UpdateResult updateResult=mongoTemplate.updateFirst(query1,update1,KmTime.class);	
		syncTpr(ouids, sDate, eDate,skipCount);	
	}
	
	private void syncTpr(List<String> ouids,Date sDate, Date eDate,long skipCount) {
		TprHistoryLog log=new TprHistoryLog();
		log.setStart(new Date());
		log.setVehicleCount(ouids.size());
		log.setVehicleSkipCount(skipCount);
        mongoTemplate.save(log);
		Criteria criteria=Criteria.where("ouid").in(ouids).andOperator(Criteria.where("add").gte(sDate).lte(eDate));
		Query query=Query.query(criteria);
		List<TerminalPacketRecord> tprs = mongoTemplate.find(query,TerminalPacketRecord.class,"terminal_packet_record");
		List<TerminalPacketRecordHistory> terminalPacketRecordHistoryList = new ArrayList<>();
		logger.info("TPR size-" + tprs.size());
		log.setTprFetchCount(tprs.size());
        mongoTemplate.save(log);
		for (TerminalPacketRecord tpr : tprs) {
			TerminalPacketRecordHistory terminalPacketRecordHistory = new TerminalPacketRecordHistory();
			terminalPacketRecordHistory.setId(tpr.getId());
			terminalPacketRecordHistory.setOuid(tpr.getOuid());
			terminalPacketRecordHistory.setSpeed(tpr.getSpeed());
			terminalPacketRecordHistory.setAdded(tpr.getAdded());
			terminalPacketRecordHistory.setStatus(tpr.getStatus());
			terminalPacketRecordHistory.setLocation(tpr.getLocation());
			terminalPacketRecordHistory.setDuration(tpr.getDuration());
			terminalPacketRecordHistory.setDistance(tpr.getDistance());
			terminalPacketRecordHistory.setDirection(tpr.getDirection());
			terminalPacketRecordHistory.setDeviceType(tpr.getDeviceType());
			terminalPacketRecordHistory.setRefk(tpr.getRefk());
			terminalPacketRecordHistoryList.add(terminalPacketRecordHistory);
		}
		logger.info("TPR history size-" + terminalPacketRecordHistoryList.size());
		if (!terminalPacketRecordHistoryList.isEmpty() && terminalPacketRecordHistoryList.size() == tprs.size()) {
			terminalPacketRecordHistoryRepository.saveAll(terminalPacketRecordHistoryList);
			log.setTprHistoryInsertCount(terminalPacketRecordHistoryList.size());
			logger.info("TPR history added - " + terminalPacketRecordHistoryList.size());
	        mongoTemplate.save(log);
			terminalPacketRecordRepository.deleteAll(tprs);
			logger.info("TPR deleted - " + terminalPacketRecordHistoryList.size());
			log.setTprDeleteCount(terminalPacketRecordHistoryList.size());
		}
		log.setEnd(new Date());
        mongoTemplate.save(log);
	}

}
