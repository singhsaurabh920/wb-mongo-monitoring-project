package org.gps.integration.client.push.itcms;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
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

import com.google.gson.Gson;
import com.tb.core.constants.ApiConstant;
import com.tb.core.domain.ClientDataPushReport;
import com.tb.core.domain.TerminalPacketRecord;
import com.tb.core.domain.Vehicle;
import com.tb.core.domain.VenderDetail;
import com.tb.core.domain.service.VehicleService;
import com.tb.core.enums.Vender;
import com.tb.core.enums.VenderDetailFor;

@Service
@Profile("prod")
public class ITCMSClientPushDataService {
	private static final Logger logger = Logger.getLogger(ITCMSClientPushDataService.class);
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	@Qualifier("customRestTemplate")
	private RestTemplate restTemplate;
	
	@Scheduled(fixedRate = 1000 * 60 * 5)
	public void pushITCMSApiData(){
		logger.info("ITCMS API scheduler started");
		List<VenderDetail> venderDetailList=mongoTemplate.find(Query.query(Criteria.where("vender").is(Vender.ITCMS_PUSH.name())), VenderDetail.class);
		logger.info("ITCMS API VENDER DETAIL SIZE-"+venderDetailList.size());
		for (VenderDetail venderDetail : venderDetailList) {
			ClientDataPushReport clientDataPushReport=null;
			List<Vehicle> vehicles=getVehicleList(venderDetail);
			ITCMSApiData data=itcmsApiData(vehicles);
			clientDataPushReport=startClientDataPushReport(getPushReportUsername(venderDetail));
			clientDataPushReport.setCount(data.getGData().size());
			clientDataPushReport.setStatus(ApiConstant.Status.SUCCESS);
			try {
				ITCMSResponseData responseData=itcmsPushApi(data);
				if(!responseData.isSuccess()) {
					clientDataPushReport.setSuccessCount(0);
					clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
				}
			}catch(Exception e) {
				logger.info(e.getMessage());
				clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
				e.printStackTrace();
			}
			clientDataPushReport.setEndTime(new Date());
			mongoTemplate.save(clientDataPushReport);
			logger.info("ITCMS API scheduler Stopped");
		}
	}

	private ITCMSApiData itcmsApiData(List<Vehicle> vehicles) { 
		logger.info("ITCMS VEHICLE COUNT - "+vehicles.size());
		ITCMSApiData apiData=new ITCMSApiData();
		List<ITCMSApiRequestModel> requestModels=new ArrayList<>();
		logger.info("ITCMS VEHICLE NO "+vehicles);
		for (Vehicle vehicle : vehicles) {
			ITCMSApiRequestModel model=new ITCMSApiRequestModel();
			TerminalPacketRecord tpr=vehicle.getTerminalPacketRecord();
			model.setIMEI(vehicle.getDevice().getImei());
			double[] location=tpr.getLocation();
			model.setLAT(location[1]);
			model.setLON(location[0]);
			model.setCTIME(CustomTimezoneUtils.UTCDateToITCMSDateTimeStringHHmmss(vehicle.getTransporterTz(),vehicle.getLu()));
			model.setSPEED(tpr.getSpeed());
			model.setDISTANCE((int)(tpr.getDistance()));
			requestModels.add(model);
		}
		apiData.setGData(requestModels);
		return apiData;
	}
	
	private ITCMSResponseData itcmsPushApi(ITCMSApiData data) {
		logger.info("ITCMS API JSON   :  "+(new Gson().toJson(data)));
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("UserName", ApiConstant.ITCMS.USERNAME);
		headers.add("Password", ApiConstant.ITCMS.PASSWORD);
		HttpEntity<ITCMSApiData> request = new HttpEntity<>(data, headers);
		ResponseEntity<ITCMSResponseData> response = restTemplate.postForEntity(ApiConstant.ITCMS.API_URL, request, ITCMSResponseData.class);
		logger.info(response.getBody());
		return response.getBody();
	}
	
	private ClientDataPushReport startClientDataPushReport(String transporter) {
		ClientDataPushReport clientDataPushReport = new ClientDataPushReport();
		clientDataPushReport.setVender(Vender.ITCMS_PUSH.name());
		clientDataPushReport.setStatus(ApiConstant.Status.START);
		clientDataPushReport.setStartTime(new Date());
		clientDataPushReport.setUsername(transporter);
		mongoTemplate.save(clientDataPushReport);
		return clientDataPushReport;
	}
	

	private List<Vehicle> getVehicleList(VenderDetail venderDetail) {
		VenderDetailFor detailFor=VenderDetailFor.getByName(venderDetail.getApiFor());
		List<Vehicle> vehicles=new ArrayList<>();
		if(detailFor==null) {
			return vehicles;
		}
		switch (detailFor) {
			case CLIENT:
				vehicles =vehicleService.findByClientUsernameAndActive(venderDetail.getClient(),true);
				break;
			case TRANSPORTER :
				vehicles=vehicleService.findByTransporterUsernameAndActive(venderDetail.getTransporter(),true);
				break;
			case TEMP_USER :
				vehicles=vehicleService.findByUserAndActive(venderDetail.getTempUser(), true);
				break ;
			case VEHICLE:
				vehicles=vehicleService.findByOuidInAndActive(venderDetail.getOuids(), true);
				break;
			default:
				break;
		}
		return vehicles;
	}
	
	private String getPushReportUsername(VenderDetail venderDetail) {
		VenderDetailFor detailFor=VenderDetailFor.getByName(venderDetail.getApiFor());
		String username="";
		if(detailFor==null) {
			logger.info("ITCMS API NOT EXECUTE ");
			logger.info(venderDetail);
			return username;
		}
		switch (detailFor) {
			case CLIENT:
				username=venderDetail.getClient();
				logger.info("ITCMS API EXECUTE FOR CLIENT : "+username);
				break;
			case TRANSPORTER :
				username=venderDetail.getTransporter();
				logger.info("ITCMS API EXECUTE FOR TRANSPORTER : "+username);
				break;
			case TEMP_USER :
				username=venderDetail.getTempUser();
				logger.info("ITCMS API EXECUTE FOR USER : "+username);
				break ;
			case VEHICLE:
				username=venderDetail.getTransporter();
				logger.info("ITCMS API EXECUTE FOR TRANSPORTER'S VEHICLE : "+username);
				break;
			default:
				logger.info("ITCMS API NOT EXECUTE ");
				logger.info(venderDetail);
				break;
		}	
		return username;
	}

}
