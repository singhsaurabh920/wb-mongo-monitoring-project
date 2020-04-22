package org.gps.integration.client.push.nivaata;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.tb.core.constants.ApiConstant;
import com.tb.core.domain.ClientDataPushReport;
import com.tb.core.domain.TerminalPacketMeta;
import com.tb.core.domain.TerminalPacketRecord;
import com.tb.core.domain.Vehicle;
import com.tb.core.domain.VenderDetail;
import com.tb.core.domain.service.VehicleService;
import com.tb.core.enums.VehicleStatus;
import com.tb.core.enums.Vender;

@Service
@Profile("prod")
public class NivaataClientPushDataService {
	private static final Logger logger = Logger.getLogger(NivaataClientPushDataService.class);
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	@Qualifier("customRestTemplate")
	private RestTemplate restTemplate;

	//@Scheduled(fixedRate = 1000 * 30)
	public void pushNivaataApiData() throws Exception {
		logger.info("Nivaata API scheduler started");
		List<VenderDetail> venderDetailList=mongoTemplate.find(Query.query(Criteria.where("vender").is(Vender.NIVAATA_PUSH.name())), VenderDetail.class);
		logger.info("NIVAATA API TRANSPORTER SIZE-"+venderDetailList.size());
		for(VenderDetail venderDetail:venderDetailList) {
			NivaataApiRequestModel nivaataDataModel = nivaataApiData(venderDetail.getTransporter());
			ClientDataPushReport clientDataPushReport=startClientDataPushReport(venderDetail.getTransporter(),nivaataDataModel.getData().size());
			clientDataPushReport.setStatus(ApiConstant.Status.SUCCESS);
			try {
				nivaataPushApi(nivaataDataModel);
				clientDataPushReport.setSuccessCount(nivaataDataModel.getData().size());
			}catch(Exception e) {
				logger.info(e.getMessage());
				clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
				e.printStackTrace();
			}
			clientDataPushReport.setEndTime(new Date());
			mongoTemplate.save(clientDataPushReport);
		}
		logger.info("Nivaata API scheduler Stopped");
	}

	public NivaataApiRequestModel nivaataApiData(String transporter) {
		List<Vehicle> vehicles = vehicleService.findByTransporterUsernameAndActive(transporter, true);
		Map<String, Vehicle> vMap = vehicles.stream().collect(Collectors.toMap(Vehicle::getOuid, vehicle -> vehicle));
		Date end = CustomTimezoneUtils.getCurrentDateInUTC();
		Date start = CustomTimezoneUtils.getSecBackDateInUTC(30, end);
		logger.info("Start date-" + start+ " End date-" + end);
		Query query=Query.query(Criteria.where("ouid").in(vMap.keySet()).and("id").gte(new ObjectId(start)).lte(new ObjectId(end)));
		List<TerminalPacketRecord> terminalPacketRecords = mongoTemplate.find(query, TerminalPacketRecord.class);
		logger.info("TPR SIZE=="+terminalPacketRecords.size());
		Map<String, List<TerminalPacketRecord>> tMap = new HashMap<>();
		for (TerminalPacketRecord tpr : terminalPacketRecords) {
			String ouid = tpr.getOuid();
			if (tMap.containsKey(ouid)) {
				List<TerminalPacketRecord> terminalPacketRecordList = tMap.get(ouid);
				terminalPacketRecordList.add(tpr);
				tMap.put(ouid, terminalPacketRecordList);
			} else {
				List<TerminalPacketRecord> terminalPacketRecordList = new ArrayList<>();
				terminalPacketRecordList.add(tpr);
				tMap.put(ouid, terminalPacketRecordList);
			}
		}
		NivaataApiRequestModel nivaataDataModel = new NivaataApiRequestModel();
		List<NivaataData> data = new ArrayList<>();
		NivaataData nivaataData = new NivaataData();
		List<NivaataLocation> locations = new ArrayList<>();
		for (Entry<String, List<TerminalPacketRecord>> entry : tMap.entrySet()) {
			String ouid = entry.getKey();
			Vehicle vehicle = vMap.get(ouid);
			TerminalPacketMeta tpm = vehicle.getTerminalPacketMeta();
			List<TerminalPacketRecord> tprList = entry.getValue();
			NivaataLocation nivaataLocation = new NivaataLocation();
			nivaataLocation.setVehiclename(vehicle.getVehicleNo());
			List<NivaataLocationModel> location = new ArrayList<>();
			for (TerminalPacketRecord tpr : tprList) {
				NivaataLocationModel nivaataLocationModel = new NivaataLocationModel();
				VehicleStatus vehicleStatus=VehicleStatus.getByDbValue(tpr.getStatus());
				if(vehicleStatus==VehicleStatus.IDLE||vehicleStatus==VehicleStatus.RUNNING||vehicleStatus==VehicleStatus.OVERSPEED) {
					nivaataLocationModel.setIgn(1);
				} else {
					nivaataLocationModel.setIgn(0);
				}
				nivaataLocationModel.setTs((tpr.getAdded().getTime()) / 1000);
				nivaataLocationModel.setSpeed(tpr.getSpeed());
				nivaataLocationModel.setLat(tpr.getLocation()[1]);
				nivaataLocationModel.setLng(tpr.getLocation()[0]);
				if (tpm == null) {
					nivaataLocationModel.setAccuracy(1);
				} else {
					nivaataLocationModel.setAccuracy(tpm.getGsmSignals());
				}
				location.add(nivaataLocationModel);
			}
			nivaataLocation.setLocation(location);
			locations.add(nivaataLocation);
		}
		nivaataData.setLocations(locations);
		data.add(nivaataData);
		nivaataDataModel.setData(data);
		logger.info("Nivaata API scheduler synced-" + terminalPacketRecords.size());
		return nivaataDataModel;
	}

	private Object nivaataPushApi(NivaataApiRequestModel body) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<NivaataApiRequestModel> request = new HttpEntity<>(body, headers);
		ResponseEntity<NivaataApiResponseModel> response = restTemplate.postForEntity(ApiConstant.NIVAATA.API_URL, request,NivaataApiResponseModel.class);
		logger.info("Nivaata API Response-" + response.getBody());
		return response.getBody();
	}
	
	private ClientDataPushReport startClientDataPushReport(String transporter,int count) {
		ClientDataPushReport clientDataPushReport = new ClientDataPushReport();
		clientDataPushReport.setVender(Vender.NIVAATA_PUSH.name());
		clientDataPushReport.setStatus(ApiConstant.Status.START);
		clientDataPushReport.setCount(count);
		clientDataPushReport.setStartTime(new Date());
		clientDataPushReport.setUsername(transporter);
		mongoTemplate.save(clientDataPushReport);
		return clientDataPushReport;
	}
}
