package org.gps.integration.client.push.minda;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.gps.core.utils.CustomTimezoneUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.Lists;
import com.tb.core.constants.ApiConstant;
import com.tb.core.domain.ClientDataPushReport;
import com.tb.core.domain.TerminalPacketRecord;
import com.tb.core.domain.Vehicle;
import com.tb.core.domain.VenderDetail;
import com.tb.core.domain.service.VehicleService;
import com.tb.core.enums.VehicleStatus;
import com.tb.core.enums.Vender;

@Service
@Profile("prod")
public class MindaClientPushDataService {
	private static final Logger logger = Logger.getLogger(MindaClientPushDataService.class);
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	@Qualifier("customRestTemplate")
	private RestTemplate restTemplate;

	@Scheduled(fixedRate = 1000*60*10)
	public void pushMindaApiData(){
		logger.info("Minda API scheduler started");
		List<VenderDetail> venderDetailList=mongoTemplate.find(Query.query(Criteria.where("vender").is(Vender.MINDA_PUSH.name())), VenderDetail.class);
		logger.info("MINDA API TRANSPORTER SIZE-"+venderDetailList.size());
		for(VenderDetail venderDetail:venderDetailList) {
			ClientDataPushReport clientDataPushReport=null;
			MindaApiData data = mindaApiData(venderDetail.getTransporter());
			List<MindaApiRequestModel> body=data.getMindaApiRequestModels();
			clientDataPushReport=startClientDataPushReport(venderDetail.getTransporter(),data.getCount());
			clientDataPushReport.setStatus(ApiConstant.Status.SUCCESS);
			try {
				mindaPushApi(body);
				clientDataPushReport.setSuccessCount(body.size());
			}catch(Exception e) {
				logger.info(e.getMessage());
				clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
				e.printStackTrace();
			}
			clientDataPushReport.setEndTime(new Date());
			mongoTemplate.save(clientDataPushReport);
			logger.info("Minda API scheduler Stopped");
		}
	}

	public MindaApiData mindaApiData(String transporter) {
		MindaApiData mindaApiData=new MindaApiData();
		List<MindaApiRequestModel> mindaApiModels = new LinkedList<>();
		List<Vehicle> vehicles = vehicleService.findByTransporterUsernameAndActive(transporter, true);
		logger.info("MINDA VEHICLE COUNT - "+vehicles.size());
		Map<String, Vehicle> map = vehicles.stream().collect(Collectors.toMap(Vehicle::getOuid, vehicle -> vehicle));
		Date end = CustomTimezoneUtils.getCurrentDateInUTC();
		Date start = CustomTimezoneUtils.getMinuteBackDateInUTC(10, end);
		logger.info("Start data-" + start + " End data-" + end);
		Query query=Query.query(Criteria.where("ouid").in(map.keySet()).and("id").gte(new ObjectId(start)).lte(new ObjectId(end)));
		List<TerminalPacketRecord> terminalPacketRecords = mongoTemplate.find(query, TerminalPacketRecord.class);
		logger.info("TPR SIZE=="+terminalPacketRecords.size());
		terminalPacketRecords = Lists.reverse(terminalPacketRecords);
		Set<String> ouids=new HashSet<>();
		int running=0;
		for(Vehicle vehicle :vehicles) {
			TerminalPacketRecord tpr=vehicle.getTerminalPacketRecord();
			VehicleStatus vehicleStatus=VehicleStatus.getByDbValue(tpr.getStatus());
			if(vehicleStatus==VehicleStatus.UNREACHABLE || vehicleStatus==VehicleStatus.STOPPED || vehicleStatus==VehicleStatus.IDLE ) {
				MindaApiRequestModel mindaApiModel = createMindaApiModel(tpr,new Date());
				mindaApiModel=setTotalKm(mindaApiModel,vehicle,tpr);
				mindaApiModels.add(mindaApiModel);
				ouids.add(vehicle.getOuid());
			}else if (vehicleStatus==VehicleStatus.RUNNING||vehicleStatus==VehicleStatus.STARTED) {
				++running;
			} 
		}
		logger.info("Runnnig vehicle size - "+ running);
		logger.info("Idle, Stop and Unreachble synced vehicle - "+ouids.size());
		for (TerminalPacketRecord tpr : terminalPacketRecords) {
			Vehicle vehicle = map.get(tpr.getOuid());
			ouids.add(vehicle.getOuid());
			MindaApiRequestModel mindaApiModel = createMindaApiModel(tpr,tpr.getAdded());
			mindaApiModel=setTotalKm(mindaApiModel,vehicle,tpr);
			mindaApiModels.add(mindaApiModel);
		}
		logger.info("Minda API scheduler synced-" + terminalPacketRecords.size());
		logger.info("Total synced vehicle - "+ouids.size());
		mindaApiData.setCount(ouids.size());
		
		//log
		/*for (MindaApiRequestModel model : mindaApiModels) {
			logger.info("VEHICLE NO : "+model.getVehicleUIN()+"   EPOCH TIME  "+model.getTimestamp()+"   VEHICLE STATUS  "+model.getIgnitionStatus());
		}*/
		
		mindaApiData.setMindaApiRequestModels(mindaApiModels);
		//
		return mindaApiData;
	}

	private MindaApiRequestModel setTotalKm(MindaApiRequestModel mindaApiModel, Vehicle vehicle,TerminalPacketRecord tpr) {
		double totKm = vehicle.getTotkm() - (tpr.getDistance() / 1000);
		vehicle.setTotkm(totKm);
		mindaApiModel.setDistanceCovered(totKm);
		mindaApiModel.setVehicleUIN(vehicle.getVehicleNo());
		return mindaApiModel;
	}

	private Object mindaPushApi(List<MindaApiRequestModel> body) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("Authorization", ApiConstant.MINDA.AUTHORIZATION);
		HttpEntity<List<MindaApiRequestModel>> request = new HttpEntity<>(body, headers);
		ResponseEntity<Object> response = restTemplate.postForEntity(ApiConstant.MINDA.API_URL, request, Object.class);
		logger.info("Minda API Response-" + response.getBody());
		return response.getBody();
	}
	
	private ClientDataPushReport startClientDataPushReport(String transporter,int count) {
		ClientDataPushReport clientDataPushReport = new ClientDataPushReport();
		clientDataPushReport.setVender(Vender.MINDA_PUSH.name());
		clientDataPushReport.setStatus(ApiConstant.Status.START);
		clientDataPushReport.setCount(count);
		clientDataPushReport.setStartTime(new Date());
		clientDataPushReport.setUsername(transporter);
		mongoTemplate.save(clientDataPushReport);
		return clientDataPushReport;
	}
	private MindaApiRequestModel createMindaApiModel(TerminalPacketRecord tpr,Date addedDate) {
		MindaApiRequestModel mindaApiModel = new MindaApiRequestModel();
		mindaApiModel.setSpeed(tpr.getSpeed());
		mindaApiModel.setTimestamp(addedDate.getTime());
		mindaApiModel.setLatitude(tpr.getLocation()[1]);
		mindaApiModel.setLongitude(tpr.getLocation()[0]);
		VehicleStatus vehicleStatus=VehicleStatus.getByDbValue(tpr.getStatus());
		if(vehicleStatus==VehicleStatus.IDLE||vehicleStatus==VehicleStatus.RUNNING||vehicleStatus==VehicleStatus.OVERSPEED) {
			mindaApiModel.setIgnitionStatus("ON");
		} else {
			mindaApiModel.setIgnitionStatus("OFF");
		}
		return mindaApiModel;
	}
}
