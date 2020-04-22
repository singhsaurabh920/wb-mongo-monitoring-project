package org.gps.integration.client.push.fareye;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.gps.core.utils.ConversionUtils;
import org.gps.core.utils.CustomTimezoneUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.tb.core.constants.ApiConstant;
import com.tb.core.domain.ClientDataPushReport;
import com.tb.core.domain.TerminalPacketRecord;
import com.tb.core.domain.Vehicle;
import com.tb.core.domain.VenderDetail;
import com.tb.core.domain.service.VehicleService;
import com.tb.core.enums.Vender;

//@Service
//@Profile("prod")
public class FareyePushDataService {
	private static final Logger logger = Logger.getLogger(FareyePushDataService.class);
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	@Qualifier("customRestTemplate")
	private RestTemplate restTemplate;
	
	//@Scheduled(fixedRate = 1000 * 60 * 5)
	public void pushFareyeData() {
		logger.info("FAREYE API scheduler started");
		List<VenderDetail> venderDetailList=mongoTemplate.find(Query.query(Criteria.where("vender").is(Vender.FAREYE_PUSH.name())), VenderDetail.class);
		logger.info("FAR API VENDER SIZE-"+venderDetailList.size());
		for (VenderDetail venderDetail : venderDetailList) {
			ClientDataPushReport clientDataPushReport=null;
			FareyeData data=fareyeApiData(venderDetail.getTransporter());
			clientDataPushReport=startClientDataPushReport(venderDetail.getTransporter());
			clientDataPushReport.setCount(data.getPings().size());
			clientDataPushReport.setStatus(ApiConstant.Status.SUCCESS);
			try {
				FareyeResponseData responseData=fareyePushApi(data);
				if(!responseData.isSuccess()) {
					clientDataPushReport.setSuccessCount(0);
					clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
					logger.info(responseData.getReason());
				}
			}catch(Exception e) {
				logger.info(e.getMessage());
				clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
				e.printStackTrace();
			}
			clientDataPushReport.setEndTime(new Date());
			mongoTemplate.save(clientDataPushReport);
			logger.info("FAREYE API scheduler Stopped");
		}
	}

	private FareyeData fareyeApiData(String transporter) {
		logger.info("Transporter "+transporter);
		List<Vehicle> vehicles = vehicleService.findByTransporterUsernameAndActive(transporter, true);
		logger.info("FAREYE VEHICLE COUNT - "+vehicles.size());
		FareyeData data=new FareyeData();
		List<FareyeRequestData> requestModels=new ArrayList<>();
		
		for (Vehicle vehicle : vehicles) {
			FareyeRequestData model=new FareyeRequestData();
			TerminalPacketRecord tpr=vehicle.getTerminalPacketRecord();
			model.setVehicle_no(vehicle.getVehicleNo());
			double[] location=tpr.getLocation();
			model.setLatitude(ConversionUtils.roundDown6(location[1]));
			model.setLongitude(ConversionUtils.roundDown6(location[0]));
			model.setTimestamp(CustomTimezoneUtils.UTCDateToFareyeDateTimeStringHHmmss(vehicle.getTransporterTz(),vehicle.getLu()));
			model.setSpeed(tpr.getSpeed());
			requestModels.add(model);
		}
		data.setPings(requestModels);
		return data;
	}
	
	private FareyeResponseData fareyePushApi(FareyeData data) {
		logger.info(new Gson().toJson(data));
		HttpHeaders headers = new HttpHeaders();
		headers.set("cache-control", "no-cache");
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<FareyeData> request = new HttpEntity<>(data, headers);
		ResponseEntity<FareyeResponseData> response = restTemplate.postForEntity(ApiConstant.FAREYE.API_URL, request, FareyeResponseData.class);
		logger.info(response.getBody());
		return response.getBody();
	}
	
	private ClientDataPushReport startClientDataPushReport(String transporter) {
		ClientDataPushReport clientDataPushReport = new ClientDataPushReport();
		clientDataPushReport.setVender(Vender.FAREYE_PUSH.name());
		clientDataPushReport.setStatus(ApiConstant.Status.START);
		clientDataPushReport.setStartTime(new Date());
		clientDataPushReport.setUsername(transporter);
		mongoTemplate.save(clientDataPushReport);
		return clientDataPushReport;
	}
}
