package org.gps.integration.client.push.nicerglobal;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.gps.core.utils.ConversionUtils;
import org.gps.core.utils.CustomTimezoneUtils;
import org.gps.integration.client.push.ApiDataService;
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

import com.google.gson.Gson;
import com.tb.core.constants.ApiConstant;
import com.tb.core.domain.ClientDataPushReport;
import com.tb.core.domain.TerminalPacketRecord;
import com.tb.core.domain.Vehicle;
import com.tb.core.domain.VenderDetail;
import com.tb.core.enums.VehicleStatus;
import com.tb.core.enums.Vender;

@Service
@Profile("prod")
public class NicerGlobalPushDataService {
	private static final Logger logger = Logger.getLogger(NicerGlobalPushDataService.class);
	@Autowired
	@Qualifier("customRestTemplate")
	private RestTemplate restTemplate;
	@Autowired
	private ApiDataService apiDataService;
	@Autowired
	private MongoTemplate mongoTemplate;
	
	@Scheduled(fixedRate = 1000 * 60 * 3)
	public void pushNicerApiData(){
		logger.info("NICER GLOBAL API SCHEDULER STARTED");
		List<VenderDetail> venderDetailList = mongoTemplate.find(Query.query(Criteria.where("vender").is(Vender.NICER_GLOBAL_PUSH.name())), VenderDetail.class);
		logger.info("NICER GLOBAL API TRANSPORTER SIZE-" + venderDetailList.size());
		for (VenderDetail venderDetail : venderDetailList) {
			List<Vehicle> vehicles =apiDataService.getVehicleList(venderDetail);
			logger.info("NICER GLOBAL VEHICLE SIZE-" + vehicles.size());
			String apiUsername=apiDataService.getPushReportUsername(venderDetail);
			ClientDataPushReport clientDataPushReport = apiDataService.startClientDataPushReport(apiUsername,vehicles.size(),Vender.NICER_GLOBAL_PUSH.name());
			clientDataPushReport.setType(ApiConstant.ApiType.PUSH);
			clientDataPushReport.setStatus(ApiConstant.Status.SUCCESS);
			int successCount=0;
			try {
				for (Vehicle vehicle : vehicles) {
					NicerGlobalApiResponse response=nicerGlobalPushData(vehicle);
					if(response.getStatus().equalsIgnoreCase("OK")) {
						successCount++;
					}
				}
				clientDataPushReport.setSuccessCount(successCount);
			} catch (Exception e) {
				logger.info(e.getMessage());
				clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
				e.printStackTrace();
			}
			clientDataPushReport.setEndTime(new Date());
			mongoTemplate.save(clientDataPushReport);
			logger.info("NICER GLOBAL API SCHEDULER STOPPED");
		}
	}

	private NicerGlobalApiResponse nicerGlobalPushData(Vehicle vehicle) {
		TerminalPacketRecord tpr = vehicle.getTerminalPacketRecord();
		VehicleStatus vehicleStatus=VehicleStatus.getByDbValue(tpr.getStatus());
		double[] location = tpr.getLocation();
		//
	    NicerGlobalRequestData requestData=new NicerGlobalRequestData();
	    NicerGlobalData data=new NicerGlobalData();
	    //
	    data.setLATITUDE(String.valueOf(ConversionUtils.roundDown6(location[1])));
	    data.setLONGITUDE(String.valueOf(ConversionUtils.roundDown6(location[0])));
	    data.setSPEED(String.valueOf(tpr.getSpeed()));
	    data.setHEADING("0");
	    //
	    if(vehicleStatus==VehicleStatus.IDLE||vehicleStatus==VehicleStatus.RUNNING||vehicleStatus==VehicleStatus.OVERSPEED) {
			data.setIGNSTATUS(String.valueOf(1));
			data.setDATETIME(CustomTimezoneUtils.UTCDateToNicerGlobalDateTimeStringHHmmss(vehicle.getLu()));
	    } else {
	    	data.setIGNSTATUS(String.valueOf(0));
	    	data.setDATETIME(CustomTimezoneUtils.UTCDateToNicerGlobalDateTimeStringHHmmss(new Date()));
	    }
	    data.setLOCATION(tpr.getAddress());
	    requestData.setDATAELEMENTS(data);
	    //
	    NicerGlobalApiData nicerGlobalApiData = new NicerGlobalApiData();
	    nicerGlobalApiData.setDATAELEMENTS(requestData);
	    nicerGlobalApiData.setVEHICLENO(vehicle.getVehicleNo());
	    nicerGlobalApiData.setGPSPROVIDERKEY(ApiConstant.NICERGLOBAL.GPSPROVIDERKEY);
	   return nicerGlobalPushApi(nicerGlobalApiData);
		
	}
	
	private NicerGlobalApiResponse nicerGlobalPushApi(NicerGlobalApiData body) {
		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl("no-cache");
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> request = new HttpEntity<>(new Gson().toJson(body), headers);
		ResponseEntity<NicerGlobalApiResponse> response = restTemplate.postForEntity(ApiConstant.NICERGLOBAL.API_URL, request, NicerGlobalApiResponse.class);
		NicerGlobalApiResponse apiResponse=response.getBody();
		logger.info("NICER GLOBAL API Response-" + apiResponse);
		return apiResponse;
	}

	
	
}
