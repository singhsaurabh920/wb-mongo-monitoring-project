package org.gps.integration.client.push.mahamining;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.gps.core.utils.ConversionUtils;
import org.gps.core.utils.CustomTimezoneUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tb.core.constants.ApiConstant;
import com.tb.core.domain.ClientDataPushReport;
import com.tb.core.domain.TerminalPacketMeta;
import com.tb.core.domain.Vehicle;
import com.tb.core.domain.VenderDetail;
import com.tb.core.domain.service.VehicleService;
import com.tb.core.enums.VehicleStatus;
import com.tb.core.enums.Vender;

@Service
@Profile("prod")
public class MahaMiningClientPushDataService {

	private static final Logger logger = Logger.getLogger(MahaMiningClientPushDataService.class);
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	private ObjectMapper objectMapper;

	private static final String MAHAMINING_API_URL = "http://gps.mahamining.com/Service.asmx?WSDL";
	private static final String INSERT_DATA_METHOD = "InsertGPRSdata";

	@Scheduled(fixedRate = 1000 * 60 * 5)
	public void mahaMiningAPiScheduler() {
		String xmlString = "";
		logger.info("MAHAMINING API SCHEDULER STARTED");
		List<VenderDetail> venderDetailList = mongoTemplate
				.find(Query.query(Criteria.where("vender").is(Vender.MAHA_MINING_PUSH.name())), VenderDetail.class);
		logger.info("MAHAMINING API TRANSPORTER SIZE-" + venderDetailList.size());
		for (VenderDetail venderDetail : venderDetailList) {
			List<Vehicle> vehicles = vehicleService.findByTransporterUsernameAndActive(venderDetail.getTransporter(),true);
			logger.info("MAHAMINING VEHICLE SIZE-" + vehicles.size());
			ClientDataPushReport clientDataPushReport = startClientDataPushReport(venderDetail.getTransporter(),vehicles.size());
			clientDataPushReport.setStatus(ApiConstant.Status.SUCCESS);

			for (Vehicle vehicle : vehicles) {
				MahaMiningApiRequestModel mahaminingData = new MahaMiningApiRequestModel();
				VehicleStatus vehicleStatus = VehicleStatus.getByUiValue(vehicle.getTerminalPacketRecord().getStatus());
				if (vehicleStatus == VehicleStatus.IDLE || vehicleStatus == VehicleStatus.RUNNING
						|| vehicleStatus == VehicleStatus.OVERSPEED) {
					mahaminingData.setIgnition(1);
				} else {
					mahaminingData.setIgnition(0);
				}
				mahaminingData.setPowerCut(0);
				mahaminingData.setReason("A");
				mahaminingData.setCommandKey("0");
				mahaminingData.setCommandkeyValue("0");
				mahaminingData.setMsgKey("0");
				mahaminingData.setSpeed(vehicle.getTerminalPacketRecord().getSpeed());
				TerminalPacketMeta tpm = vehicle.getTerminalPacketMeta();
				if (tpm != null) {
					int sattellite = tpm.getSatellites();
					if (sattellite > 12) {
						mahaminingData.setSatVisible(12);
					} else {
						mahaminingData.setSatVisible(sattellite);
					}
				} else {
					mahaminingData.setSatVisible(0);
				}
				double[] location = (vehicle.getTerminalPacketRecord().getLocation());
				mahaminingData.setLatitude(String.valueOf(ConversionUtils.roundDown6(location[1])));
				mahaminingData.setLongitude(String.valueOf(ConversionUtils.roundDown6(location[0])));
				String dateTime = CustomTimezoneUtils.UTCDateToWeTrackDateTimeStringHHmmss(vehicle.getTransporterTz(),vehicle.getLu());
				String[] dateAndTime = dateTime.split(" ");
				mahaminingData.setDate(dateAndTime[0]);
				mahaminingData.setUnit(Long.parseLong(vehicle.getDevice().getImei()));
				mahaminingData.setTime(dateAndTime[1]);
				mahaminingData.setDeviceCompanyId(23);
				xmlString = xlmPushData(xmlString, mahaminingData);
			}
			String response = pushMahaMiningData(xmlString).split("<")[0];
			MahaMiningApiResponseData responseData;
			try {
				responseData = objectMapper.readValue(response, MahaMiningApiResponseData.class);
				switch (responseData.getData()) {
				case "0":
						int id=responseData.getData1().get(0).getId();
						if(id>0) {
							clientDataPushReport.setStatus(ApiConstant.Status.SUCCESS);
						}else {
							clientDataPushReport.setSuccessCount(0);
							clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
						}
					break;
				default:
					clientDataPushReport.setSuccessCount(0);
					clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
					break;
				}
			} catch (JsonMappingException e) {
				clientDataPushReport.setSuccessCount(0);
				clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
				e.printStackTrace();
			} catch (JsonProcessingException e) {
				clientDataPushReport.setSuccessCount(0);
				clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
				e.printStackTrace();
			}
			stopClientDataPushReport(clientDataPushReport);
		}
		logger.info("MAHAMINING API SCHEDULER STOPPED");
	}

	public String pushMahaMiningData(String xmlString) {
		String responseString = "";
		String outputString = "";
		URL url = null;
		URLConnection connection = null;
		String xmlInput = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
				+ "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
				+ "<soap:Body> " + xmlString + " </soap:Body> " + "</soap:Envelope>";
		logger.info(xmlInput);
		try {
			url = new URL(MAHAMINING_API_URL);
		} catch (MalformedURLException e) {
			logger.error("MalformedURLException- ",e);
			return outputString;
		}
		try {
			connection = url.openConnection();
			HttpURLConnection httpConn = (HttpURLConnection) connection;
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			byte[] buffer = new byte[xmlInput.length()];
			buffer = xmlInput.getBytes();
			bout.write(buffer);
			byte[] b = bout.toByteArray();
			httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
			httpConn.setRequestMethod("POST");
			httpConn.setDoOutput(true);
			httpConn.setDoInput(true);
			OutputStream out = httpConn.getOutputStream();
			out.write(b);
			out.close();
			InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
			BufferedReader in = new BufferedReader(isr);
			while ((responseString = in.readLine()) != null) {
				outputString = outputString + responseString;
			}
		} catch (IOException e) {
			logger.error("IOException- ",e);
			return outputString;
		}
		logger.info("Mahamining Output "+outputString);
		return outputString;
	}

	private ClientDataPushReport startClientDataPushReport(String transporter, int count) {
		ClientDataPushReport clientDataPushReport = new ClientDataPushReport();
		clientDataPushReport.setVender(Vender.MAHA_MINING_PUSH.name());
		clientDataPushReport.setStatus(ApiConstant.Status.START);
		clientDataPushReport.setCount(count);
		clientDataPushReport.setStartTime(new Date());
		clientDataPushReport.setUsername(transporter);
		mongoTemplate.save(clientDataPushReport);
		return clientDataPushReport;
	}

	private ClientDataPushReport stopClientDataPushReport(ClientDataPushReport clientDataPushReport) {
		clientDataPushReport.setEndTime(new Date());
		mongoTemplate.save(clientDataPushReport);
		return clientDataPushReport;
	}

	public String xlmPushData(String xmlInputString, MahaMiningApiRequestModel mahaminingData) {
		xmlInputString = xmlInputString + "<" + INSERT_DATA_METHOD + " xmlns=\"http://gps.mahamining.com/\"> "
				+ "<Unit>" + mahaminingData.getUnit() + "</Unit>" + "<Reason>" + mahaminingData.getReason()
				+ "</Reason>" + "<CommandKey>" + mahaminingData.getCommandKey() + "</CommandKey>" + "<Commandkeyvalue>"
				+ mahaminingData.getCommandkeyValue() + "</Commandkeyvalue>" + "<Ignition>"
				+ mahaminingData.getIgnition() + "</Ignition>" + "<PowerCut>" + mahaminingData.getPowerCut()
				+ "</PowerCut>" + "<MSGKey>" + mahaminingData.getMsgKey() + "</MSGKey>" + "<Speed>"
				+ mahaminingData.getSpeed() + "</Speed>" + "<SatVisible>" + mahaminingData.getSatVisible()
				+ "</SatVisible>" + "<Latitude>" + mahaminingData.getLatitude() + "</Latitude>" + "<Longitude>"
				+ mahaminingData.getLongitude() + "</Longitude>" + "<Time>" + mahaminingData.getTime() + "</Time>"
				+ "<DATE>" + mahaminingData.getDate() + "</DATE>" + "<DeviceCompanyId>"
				+ mahaminingData.getDeviceCompanyId() + "</DeviceCompanyId>" + "</" + INSERT_DATA_METHOD + ">";
		return xmlInputString;
	}
}