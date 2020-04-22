
package org.gps.integration.client.push.Senseable;

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
import com.tb.core.domain.TerminalPacketMeta;
import com.tb.core.domain.TerminalPacketRecord;
import com.tb.core.domain.Vehicle;
import com.tb.core.domain.VenderDetail;
import com.tb.core.domain.service.VehicleService;
import com.tb.core.enums.VehicleStatus;
import com.tb.core.enums.Vender;
import com.tb.core.enums.VenderDetailFor;

@Service

@Profile("prod")
public class SenseableClientPushDataService {
	private static final Logger logger = Logger.getLogger(SenseableClientPushDataService.class);

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private VehicleService vehicleService;

	@Autowired

	@Qualifier("customRestTemplate")
	private RestTemplate restTemplate;

	@Scheduled(fixedRate = 1000 * 60 * 5)
	public void pushSenseableApiDataScheduler() {
		logger.info("SENSEABLE API scheduler started");
		List<VenderDetail> venderDetailList = mongoTemplate
				.find(Query.query(Criteria.where("vender").is(Vender.SENSEABLE.name())), VenderDetail.class);
		logger.info("SENSEABLE API VENDER DETAIL SIZE-" + venderDetailList.size());
		for (VenderDetail venderDetail : venderDetailList) {
			ClientDataPushReport clientDataPushReport = null;
			List<Vehicle> vehicles = getVehicleList(venderDetail);
			SenseableApiData data = senseableApiData(vehicles);
			clientDataPushReport = startClientDataPushReport(getPushReportUsername(venderDetail));
			clientDataPushReport.setCount(data.getData().size());
			clientDataPushReport.setStatus(ApiConstant.Status.SUCCESS);
			try {
				SenseableApiResponseData responseData = senseablePushApi(data);
				if (!responseData.isSuccess()) {
					clientDataPushReport.setSuccessCount(0);
					clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
				}
			} catch (Exception e) {
				logger.info(e.getMessage());
				clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
				e.printStackTrace();
			}
			clientDataPushReport.setEndTime(new Date());
			mongoTemplate.save(clientDataPushReport);
			logger.info("SENSEABLE API scheduler Stopped");
		}
	}

	private SenseableApiData senseableApiData(List<Vehicle> vehicles) {
		logger.info("SENSEABLE VEHICLE COUNT - " + vehicles.size());
		SenseableApiData apiData = new SenseableApiData();
		List<SenseableApiRequestModel> requestModels = new ArrayList<>();
		logger.info("SENSEABLE VEHICLE NO " + vehicles);
		for (Vehicle vehicle : vehicles) {
			SenseableApiRequestModel model = new SenseableApiRequestModel();
			TerminalPacketRecord tpr = vehicle.getTerminalPacketRecord();
			TerminalPacketMeta tpm = vehicle.getTerminalPacketMeta();
			model.setVehicle_no(vehicle.getVehicleNo());
			model.setImei(vehicle.getDevice().getImei());
			double[] location = tpr.getLocation();
			model.setLatitude(location[1]);
			model.setLongitude(location[0]);
			model.setTimestamp(CustomTimezoneUtils.UTCDateToFareyeDateTimeStringHHmmss(vehicle.getTransporterTz(),
					vehicle.getLu()));
			model.setSpeed(tpr.getSpeed());
			VehicleStatus vehicleStatus = VehicleStatus.getByUiValue(vehicle.getTerminalPacketRecord().getStatus());
			if (vehicleStatus == VehicleStatus.IDLE || vehicleStatus == VehicleStatus.RUNNING
					|| vehicleStatus == VehicleStatus.OVERSPEED || vehicleStatus==VehicleStatus.STARTED) {
				model.setIgnitionStatus("on");
			} else {
				model.setIgnitionStatus("off");
			}
			if (tpm != null) {
				int sattellite = tpm.getSatellites();
				int gsmSignals = tpm.getGsmSignals();
				if (sattellite > 12) {
					model.setSatellites(12);
					model.setGsmSignals(gsmSignals);
				} else {
					model.setGsmSignals(gsmSignals);
					model.setSatellites(sattellite);
				}
			} 
			requestModels.add(model);
		}
		apiData.setData(requestModels);
		return apiData;
	}

	private SenseableApiResponseData senseablePushApi(SenseableApiData data) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("Authorization", ApiConstant.SENSEABLE.AUTHORIZATION);
		HttpEntity<SenseableApiData> request = new HttpEntity<>(data, headers);
		ResponseEntity<SenseableApiResponseData> response = restTemplate.postForEntity(ApiConstant.SENSEABLE.API_URL, request, SenseableApiResponseData.class); 
		logger.info("SENSEABLE API Response-" +response.getBody()); 
		return response.getBody();
	}

	private ClientDataPushReport startClientDataPushReport(String transporter) {
		ClientDataPushReport clientDataPushReport = new ClientDataPushReport();
		clientDataPushReport.setVender(Vender.SENSEABLE.name());
		clientDataPushReport.setStatus(ApiConstant.Status.START);
		clientDataPushReport.setStartTime(new Date());
		clientDataPushReport.setUsername(transporter);
		mongoTemplate.save(clientDataPushReport);
		return clientDataPushReport;
	}

	private List<Vehicle> getVehicleList(VenderDetail venderDetail) {
		VenderDetailFor detailFor = VenderDetailFor.getByName(venderDetail.getApiFor());
		List<Vehicle> vehicles = new ArrayList<>();
		if (detailFor == null) {
			return vehicles;
		}
		switch (detailFor) {
		case CLIENT:
			vehicles = vehicleService.findByClientUsernameAndActive(venderDetail.getClient(), true);
			break;
		case TRANSPORTER:
			vehicles = vehicleService.findByTransporterUsernameAndActive(venderDetail.getTransporter(), true);
			break;
		case TEMP_USER:
			vehicles = vehicleService.findByUserAndActive(venderDetail.getTempUser(), true);
			break;
		case VEHICLE:
			vehicles = vehicleService.findByOuidInAndActive(venderDetail.getOuids(), true);
			break;
		default:
			break;
		}
		return vehicles;
	}

	private String getPushReportUsername(VenderDetail venderDetail) {
		VenderDetailFor detailFor = VenderDetailFor.getByName(venderDetail.getApiFor());
		String username = "";
		if (detailFor == null) {
			logger.info("SENSEABLE API NOT EXECUTE ");
			logger.info(venderDetail);
			return username;
		}
		switch (detailFor) {
		case CLIENT:
			username = venderDetail.getClient();
			logger.info("SENSEABLE API EXECUTE FOR CLIENT : " + username);
			break;
		case TRANSPORTER:
			username = venderDetail.getTransporter();
			logger.info("SENSEABLE API EXECUTE FOR TRANSPORTER : " + username);
			break;
		case TEMP_USER:
			username = venderDetail.getTempUser();
			logger.info("SENSEABLE API EXECUTE FOR USER : " + username);
			break;
		case VEHICLE:
			username = venderDetail.getTransporter();
			logger.info("SENSEABLE API EXECUTE FOR TRANSPORTER'S VEHICLE : " + username);
			break;
		default:
			logger.info("SENSEABLE API NOT EXECUTE ");
			logger.info(venderDetail);
			break;
		}
		return username;
	}

}