package org.gps.integration.client.push.tci;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.gps.core.utils.ConversionUtils;
import org.gps.core.utils.CustomTimezoneUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.google.gson.Gson;
import com.tb.core.constants.ApiConstant;
import com.tb.core.domain.ClientDataPushReport;
import com.tb.core.domain.GeoFence;
import com.tb.core.domain.GeoFenceReport;
import com.tb.core.domain.MissedTrip;
import com.tb.core.domain.ScheduledReport;
import com.tb.core.domain.SmsAndEmailAlertData;
import com.tb.core.domain.TciData;
import com.tb.core.domain.TciMissedTripData;
import com.tb.core.domain.TerminalPacketMeta;
import com.tb.core.domain.TerminalPacketRecord;
import com.tb.core.domain.Tour;
import com.tb.core.domain.Trip;
import com.tb.core.domain.Vehicle;
import com.tb.core.domain.VehicleGroups;
import com.tb.core.domain.service.GeoFenceReportService;
import com.tb.core.domain.service.GeoFenceService;
import com.tb.core.domain.service.GpsEmailService;
import com.tb.core.domain.service.ScheduledReportService;
import com.tb.core.domain.service.SmsAndEmailAlertService;
import com.tb.core.domain.service.TCPConnectionService;
import com.tb.core.domain.service.TourService;
import com.tb.core.domain.service.TrackobitUserService;
import com.tb.core.domain.service.TripService;
import com.tb.core.domain.service.VehicleGroupsService;
import com.tb.core.domain.service.VehicleService;
import com.tb.core.enums.ClientDetails;
import com.tb.core.enums.Reports;
import com.tb.core.enums.RoleTypes;
import com.tb.core.enums.SendType;
import com.tb.core.enums.SmsAndEmailDetailType;
import com.tb.core.enums.TCPRequestType;
import com.tb.core.enums.VehicleStatus;
import com.tb.core.modal.email.EmailTemplate;
import com.tb.core.service.TciTripService;

@Service
@Profile("prod")
public class TciClientPushDataService {
	private static final Logger logger = Logger.getLogger(TciClientPushDataService.class);
	private static final List<String> TCI_FIELDS = new ArrayList<>();
	@Autowired
	private TourService tourService;
	@Autowired
	private TripService tripService;
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	private GeoFenceService geoFenceService;
	@Autowired
	private GpsEmailService gpsEmailService;
	@Autowired
	private TCPConnectionService tcpConnectionService;
	@Autowired
	private GeoFenceReportService geoFenceReportService;
	@Autowired
	private TciTripService tciTripService;
	@Autowired
	private SmsAndEmailAlertService smsAndEmailAlertService;
	@Autowired
	private ScheduledReportService scheduledReportService;
	@Autowired
	private TrackobitUserService trackobitUserService;
	@Autowired
	private VehicleGroupsService vehicleGroupsService;
	@Autowired
	@Qualifier("customRestTemplate")
	private RestTemplate restTemplate;
	
	
	@PostConstruct
	private void init() {
		TCI_FIELDS.add("TripSheetNumber");
		TCI_FIELDS.add("OriginHub");
		TCI_FIELDS.add("OriginHubName");
		TCI_FIELDS.add("DestinationHub");
		TCI_FIELDS.add("DestinationHubName");
		TCI_FIELDS.add("VehicleNumber");
		TCI_FIELDS.add("ScheduleArrivalDate");
		TCI_FIELDS.add("Distance");
		TCI_FIELDS.add("ScheduleHours");
		TCI_FIELDS.add("VendorCode");
		TCI_FIELDS.add("VendorName");
		TCI_FIELDS.add("ArrivalDepartDate");
		TCI_FIELDS.add("TripSheetDate");
		logger.info("TCI field added");
	}
	
	//@Scheduled(fixedRate =200000000)
	@Scheduled(cron="0 30 3 * * *")
	public void tciUnreachableVehiclesEmailScheduler9() {
		logger.info("TCI UNREACHABLE VEHICLE EMAIL AT 9 SCHEDULER STARTED");
		sendTciUnreachableVehiclesEmail();
		logger.info("TCI UNREACHABLE VEHICLE EMAIL AT 9 SCHEDULER STOPPED");
	}
	
	@Scheduled(cron="0 30 7 * * *")
	public void tciUnreachableVehiclesEmailScheduler1() {
		logger.info("TCI UNREACHABLE VEHICLE EMAIL AT 1 SCHEDULER STARTED");
		sendTciUnreachableVehiclesEmail();
		logger.info("TCI UNREACHABLE VEHICLE EMAIL AT 1 SCHEDULER STOPPED");
	}
	
	@Scheduled(cron="0 30 10 * * *")
	public void tciUnreachableVehiclesEmailScheduler4() {
		logger.info("TCI UNREACHABLE VEHICLE EMAIL AT 4 SCHEDULER STARTED");
		sendTciUnreachableVehiclesEmail();
		logger.info("TCI UNREACHABLE VEHICLE EMAIL AT 4 SCHEDULER STOPPED");
	}
	
	@Scheduled(fixedRate = 1000 * 60 * 30)
	public void tciAPiScheduler() {
		logger.info("TCI API SCHEDULER STARTED");
		ClientDataPushReport clientDataPushReport=new ClientDataPushReport();
		clientDataPushReport.setType(ApiConstant.ApiType.PUSH);
		clientDataPushReport.setVender(ApiConstant.TCI.NAME);
		clientDataPushReport.setUsername(ApiConstant.TCI.TRANSPORTER);
		clientDataPushReport.setStartTime(new Date());
		clientDataPushReport.setStatus(ApiConstant.Status.START);
		Map<String, Vehicle> vehicleMap = new HashMap<>();
		List<Vehicle> vehicles = vehicleService.findByTransporterUsernameAndActive(ApiConstant.TCI.TRANSPORTER, true);
		clientDataPushReport.setCount(vehicles.size());
	    mongoTemplate.save(clientDataPushReport);
		for(Vehicle vehicle:vehicles) {
			vehicleMap.put(vehicle.getVehicleNo(),vehicle);
		}
		String tciResponse =tciTripService.pullTciTripData();
		logger.info(tciResponse);
		if (StringUtils.isNotEmpty(tciResponse)) {
			Document document = tciTripService.parseXmlFile(tciResponse);
			if (document != null) {
				NodeList nodeList = document.getElementsByTagName("Trip");
				logger.info("TCI API trip list-" + nodeList.getLength());
				if (nodeList.getLength() > 0) {
					for (int i = 0; i < nodeList.getLength(); i++) {
						String tripSheetNumber = tciTripService.getNodeValueyNodeNameAndIndex(document, TCI_FIELDS.get(0), i);
						String originHub = tciTripService.getNodeValueyNodeNameAndIndex(document, TCI_FIELDS.get(1), i);
						String destinationHub =tciTripService.getNodeValueyNodeNameAndIndex(document, TCI_FIELDS.get(3), i);
						String vehicleNumber =tciTripService.getNodeValueyNodeNameAndIndex(document, TCI_FIELDS.get(5), i);
						String distance = tciTripService.getNodeValueyNodeNameAndIndex(document, TCI_FIELDS.get(7), i);
						if (StringUtils.isEmpty(distance)) {
							distance = "0";
						}
						String scheduleHours = tciTripService.getNodeValueyNodeNameAndIndex(document, TCI_FIELDS.get(8), i);
						if (StringUtils.isEmpty(scheduleHours)) {
							scheduleHours = "0";
						}
						String tripSheetDate = tciTripService.getNodeValueyNodeNameAndIndex(document, TCI_FIELDS.get(12), i);
						String arrivalDepartDate =tciTripService. getNodeValueyNodeNameAndIndex(document, TCI_FIELDS.get(11), i);
						String scheduleArrivalDate =tciTripService. getNodeValueyNodeNameAndIndex(document, TCI_FIELDS.get(6), i);
						Vehicle vehicle = vehicleMap.get(vehicleNumber);
						TciData tciData = new TciData();
						tciData.setTcsNo(tripSheetNumber);
						tciData.setTcsDate(CustomTimezoneUtils.tciDateStringToUTCDate(ApiConstant.TCI.TIMEZONE,tripSheetDate));
						tciData.setTcsDepartureTime(CustomTimezoneUtils.tciDateStringToUTCDate(ApiConstant.TCI.TIMEZONE,arrivalDepartDate));
						if (vehicle == null) {
							logger.info("Vehicle not found:" + vehicleNumber);
							MissedTrip missedTrip=new MissedTrip();
							TciMissedTripData tciMissedTripData=new TciMissedTripData();
							tciMissedTripData.setVehicleNo(vehicleNumber);
							tciMissedTripData.setSource(originHub);
							tciMissedTripData.setDestination(destinationHub);
							tciMissedTripData.setTciData(tciData);
							tciMissedTripData.setScheduleArrivalTime(CustomTimezoneUtils.tciDateStringToUTCDate(ApiConstant.TCI.TIMEZONE, scheduleArrivalDate));
							tciMissedTripData.setCause("Vehicle not found " + vehicleNumber);
							missedTrip.setCreatedBy("tci");
							missedTrip.setAdded(new Date());
							missedTrip.setTci(tciMissedTripData);
							mongoTemplate.save(missedTrip);
							continue;
						}
						GeoFence originGeo = geoFenceService.findByName(originHub);
						if (originGeo == null) {
							logger.info("Geofence not found:" + originHub);
							MissedTrip missedTrip=new MissedTrip();
							TciMissedTripData tciMissedTripData=new TciMissedTripData();
							tciMissedTripData.setVehicleNo(vehicleNumber);
							tciMissedTripData.setSource(originHub);
							tciMissedTripData.setDestination(destinationHub);
							tciMissedTripData.setTciData(tciData);
							tciMissedTripData.setScheduleArrivalTime(CustomTimezoneUtils.tciDateStringToUTCDate(ApiConstant.TCI.TIMEZONE, scheduleArrivalDate));
							tciMissedTripData.setCause("Source geofence not found " + originHub);
							missedTrip.setCreatedBy("tci");
							missedTrip.setAdded(new Date());
							missedTrip.setTci(tciMissedTripData);
							mongoTemplate.save(missedTrip);
							continue;
						}
						GeoFence destinationGeo = geoFenceService.findByName(destinationHub);
						if (destinationGeo == null) {
							logger.info("Geofence not found:" + destinationHub);
							MissedTrip missedTrip=new MissedTrip();
							TciMissedTripData tciMissedTripData=new TciMissedTripData();
							tciMissedTripData.setVehicleNo(vehicleNumber);
							tciMissedTripData.setSource(originHub);
							tciMissedTripData.setDestination(destinationHub);
							tciMissedTripData.setTciData(tciData);
							tciMissedTripData.setScheduleArrivalTime(CustomTimezoneUtils.tciDateStringToUTCDate(ApiConstant.TCI.TIMEZONE, scheduleArrivalDate));
							tciMissedTripData.setCause("Destination geofence not found " + destinationHub);
							missedTrip.setCreatedBy("tci");
							missedTrip.setAdded(new Date());
							missedTrip.setTci(tciMissedTripData);
							mongoTemplate.save(missedTrip);
							continue;
						}
						Tour tour = tourService.findBySourceGeoIdAndDestinationGeoId(originGeo.getId(),destinationGeo.getId());
						if (tour == null) {
							tour = new Tour();
							tour.setSource(originGeo);
							tour.setDestination(destinationGeo);
							tour.setDistance(Double.parseDouble(distance));
							tour.setDuration(Integer.parseInt(scheduleHours) * 60 * 60);
							tour.setAdded(new Date());
							tour.setUpdated(new Date());
							tour = tourService.save(tour);
							logger.info("TCI tour created:" + tour);
						} else {
							logger.info("TCI tour already exists:" + tour);
						}
						Trip trip = tripService.findByTourIdAndOuidAndTcsNo(tour.getId(), vehicle.getOuid(),tripSheetNumber);
						if (trip == null) {
							trip = new Trip();
							trip.setTour(tour);
							trip.setOuid(vehicle.getOuid());
							trip.setScheduleArrivalTime(CustomTimezoneUtils.tciDateStringToUTCDate(vehicle.getTransporterTz(), scheduleArrivalDate));
							trip.setScheduleDepartureTime(CustomTimezoneUtils.tciDateStringToUTCDate(ApiConstant.TCI.TIMEZONE,arrivalDepartDate));
							trip.setTciData(tciData);
							GeoFenceReport geoFenceReport = geoFenceReportService.findByGeoIdAndOuidAndAfterOutTime(originGeo.getId(), vehicle.getOuid(),CustomTimezoneUtils.getBackDateFromCurrentDateInUTC(TimeUnit.HOURS, 10));
							if (geoFenceReport != null) {
								logger.info("TCI trip pre:"+geoFenceReport);
								trip.setActualDepartureTime(geoFenceReport.getOutTime());
								trip.setActualDepartingKm(geoFenceReport.getOutTotKm());
							}
							trip.setAdded(new Date());
							trip = tripService.save(trip);
							vehicle.addTrip(trip);
							vehicleService.save(vehicle);
							tcpConnectionService.sendVehicleUpdate(TCPRequestType.UPDATE,vehicle);
							logger.info("TCI trip created:" + trip);
						} else {
							logger.info("TCI trip already exists:" + trip);
						}
					}
				}
			}
		}
		tciPushVehicleDataFromGPSApi(clientDataPushReport);
		logger.info("TCI API SCHEDULER STOPPED");
	}

	public void sendTciUnreachableVehiclesEmail() {
		List<ScheduledReport> scheduledReports= scheduledReportService.findByCreatedByAndReport("srtms",Reports.UNREACHABLE.name());
		List<Vehicle> vehicles=new ArrayList<>();
		for (ScheduledReport scheduledReport : scheduledReports) {	
			SendType type=SendType.getByDbValue(scheduledReport.getAssignedType());
			String username=scheduledReport.getUsername();
			switch (type) {
			case TRANSPORTER:
				vehicles=vehicleService.findByTransporterUsernameAndStatusAndActive(username,VehicleStatus.UNREACHABLE.getDbValue(),true);
				break;
			case USER:
				VehicleGroups vehicleGroups = trackobitUserService.findByUsernameAndRole(username, RoleTypes.ROLE_TEMPUSER.name()).getGroup();
				vehicles=vehicleService.findByGroupAndStatusAndActive(vehicleGroups,VehicleStatus.UNREACHABLE.getDbValue(),true);
				break;
			case GROUP:
				VehicleGroups vehicleGroup=vehicleGroupsService.find(username);
				vehicles=vehicleService.findByGroupAndStatusAndActive(vehicleGroup,VehicleStatus.UNREACHABLE.getDbValue(),true);
				break;
			default:
				break;
			}
			logger.info("TCI UNREACHABLE VEHICLE- "+vehicles.size());
			String msg="<p style='color: #285bc1;'>Please find enclosed herewith list of Inactive vehicle dated :"+CustomTimezoneUtils.UTCDateToUserDateTimeString(ApiConstant.TCI.TIMEZONE,new Date())+"<br/>"
					   +"<p>Total number of vehicles is "+vehicles.size()+"<br/>"
					   +"<table style='background: #c5b7b7;border-spacing: 1px;'>"
			           +"<thead style='font-weight: bold;font-size: 14px;background: #1d3ee2;color: white;text-align: center;'>"
			           		+"<tr>"
			           			+ "<td style='width: 30px;text-align: center;'>SN</td>"
			           			+ "<td>Vehicle</td>"
			           			+ "<td>Hub</td>"
			           			+ "<td>Cause</td>"
			           			+ "<td>Last Update</td>"
			           			+ "<td style='background:#1d3ee2 !important;'>Address</td>"
			           		+ "</tr>"
			           		+"</thead>"
			           +"<tbody style='color: black;background:#dee6e8;font-size: 12px;'>";
			int i=0;
			for(Vehicle vehicle:vehicles) {
				 String cause="Unknown";
				 String address="Unknown";
				 String branch="Unknown";
				 TerminalPacketMeta tpm=vehicle.getTerminalPacketMeta();
				 if(tpm!=null) {
					 if(!tpm.getBatteryConnected()) {
						cause="Battery Disconnected";
					 }else if (tpm.getBattery()==0) {
						 cause="Battery Disconnected";
					 }else if(tpm.getSatelites()==0) {
						 cause="GPS Singnal";
					}
				 }
				 if (StringUtils.isNotEmpty(vehicle.getAlias())) {
					 branch=vehicle.getAlias();
				 }
				 TerminalPacketRecord tpr=vehicle.getTerminalPacketRecord();
				 if (tpr!=null) {
					 if(StringUtils.isNotEmpty(tpr.getAddress())) {
						 address=tpr.getAddress();
					 };
				 }
				 String lastUpdated = CustomTimezoneUtils.UTCDateToUserDateTimeStringHHmmss(vehicle.getTransporterTz(), vehicle.getLu());
				 msg=msg+"<tr>"
				 			+ "<td style='width: 30px;text-align: center;'>"+ ++i +"</td>"
				 			+ "<td>"+vehicle.getVehicleNo() +"</td>"
				 			+ "<td>"+branch +"</td>"
				 			+ "<td>"+cause+"</td>"
				 			+ "<td>"+lastUpdated +"</td>"
				 			+ "<td>"+address +"</td>"
				 	    + "</tr>";
			}
			msg=msg+"</tbody></table>";
			SmsAndEmailAlertData smsAndEmailAlertData =smsAndEmailAlertService.findByUsernameAndDetailsFor(scheduledReport.getUsername(),SmsAndEmailDetailType.REPORT.name());
			String toRecipients=smsAndEmailAlertData.getEmailTo().stream().collect(Collectors.joining(","));
			String cc=smsAndEmailAlertData.getEmailCc().stream().collect(Collectors.joining(","));
			String bcc=smsAndEmailAlertData.getEmailBcc().stream().collect(Collectors.joining(","));
			String from=smsAndEmailAlertData.getEmailFrom();
			
			ClientDetails clientDetails = ClientDetails.TRUCKERS;
			String clientName = clientDetails.getClientName();
			EmailTemplate mailTemplate = new EmailTemplate();
			mailTemplate.setToRecipients(toRecipients);
			mailTemplate.setCc(cc);
			mailTemplate.setBcc(bcc);
			mailTemplate.setTemplateName("mail_templates/tci_unreachable_vehicle_email.vm");
			mailTemplate.setSender(from);
			Map<String, Object> attributes = new HashMap<String, Object>();
			attributes.put("User",ApiConstant.TCI.EMAIL_USER);
			attributes.put("clientName",clientName);
			attributes.put("clientEmail",from);
			attributes.put("msg", msg);
			mailTemplate.setSubject("Inactive Vehicle-"+CustomTimezoneUtils.UTCDateToUserDateTimeString(ApiConstant.TCI.TIMEZONE,new Date()));
			mailTemplate.setAttributes(attributes);
			try {
				logger.info("TCI UNREACHABLE VEHICLE EMAIL SENDING..........");
				gpsEmailService.sendMail(mailTemplate);
			} catch (IOException e) {
				e.printStackTrace();
				logger.info("TCI UNREACHABLE VEHICLE EMAIL EXCEPTION", e);
			}
			
		}
		
		/*List<Vehicle> vehicles=vehicleService.findByTransporterUsernameAndStatusAndActive(ApiConstant.TCI.TRANSPORTER, VehicleStatus.UNREACHABLE.getDbValue(),true);
		logger.info("TCI UNREACHABLE VEHICLE- "+vehicles.size());
		String msg="<p style='color: #285bc1;'>Please find enclosed herewith list of Inactive vehicle dated :"+CustomTimezoneUtils.UTCDateToUserDateTimeString(ApiConstant.TCI.TIMEZONE,new Date())+"<br/>"
				   +"<p>Total number of vehicles is "+vehicles.size()+"<br/>"
				   +"<table style='background: #c5b7b7;border-spacing: 1px;'>"
		           +"<thead style='font-weight: bold;font-size: 14px;background: #1d3ee2;color: white;text-align: center;'>"
		           		+"<tr>"
		           			+ "<td style='width: 30px;text-align: center;'>SN</td>"
		           			+ "<td>Vehicle</td>"
		           			+ "<td>Hub</td>"
		           			+ "<td>Cause</td>"
		           			+ "<td>Last Update</td>"
		           			+ "<td style='background:#1d3ee2 !important;'>Address</td>"
		           		+ "</tr>"
		           		+"</thead>"
		           +"<tbody style='color: black;background:#dee6e8;font-size: 12px;'>";
		int i=0;
		for(Vehicle vehicle:vehicles) {
			 String cause="Unknown";
			 String address="Unknown";
			 String branch="Unknown";
			 TerminalPacketMeta tpm=vehicle.getTerminalPacketMeta();
			 if(tpm!=null) {
				 if(!tpm.getBatteryConnected()) {
					cause="Battery Disconnected";
				 }else if (tpm.getBattery()==0) {
					 cause="Battery Disconnected";
				 }else if(tpm.getSatelites()==0) {
					 cause="GPS Singnal";
				}
			 }
			 if (StringUtils.isNotEmpty(vehicle.getAlias())) {
				 branch=vehicle.getAlias();
			 }
			 TerminalPacketRecord tpr=vehicle.getTerminalPacketRecord();
			 if (tpr!=null) {
				 if(StringUtils.isNotEmpty(tpr.getAddress())) {
					 address=tpr.getAddress();
				 };
			 }
			 String lastUpdated = CustomTimezoneUtils.UTCDateToUserDateTimeStringHHmmss(vehicle.getTransporterTz(), vehicle.getLu());
			 msg=msg+"<tr>"
			 			+ "<td style='width: 30px;text-align: center;'>"+ ++i +"</td>"
			 			+ "<td>"+vehicle.getVehicleNo() +"</td>"
			 			+ "<td>"+branch +"</td>"
			 			+ "<td>"+cause+"</td>"
			 			+ "<td>"+lastUpdated +"</td>"
			 			+ "<td>"+address +"</td>"
			 	    + "</tr>";
		}
		msg=msg+"</tbody></table>";
		SmsAndEmailAlertData smsAndEmailAlertData =smsAndEmailAlertService.findByUsername(ApiConstant.TCI.TRANSPORTER);
		String toRecipients=smsAndEmailAlertData.getEmailTo().stream().collect(Collectors.joining(","));
		String cc=smsAndEmailAlertData.getEmailCc().stream().collect(Collectors.joining(","));
		String bcc=smsAndEmailAlertData.getEmailBcc().stream().collect(Collectors.joining(","));
		String from=smsAndEmailAlertData.getEmailFrom();
		
		ClientDetails clientDetails = ClientDetails.TRUCKERS;
		String clientName = clientDetails.getClientName();
		EmailTemplate mailTemplate = new EmailTemplate();
		mailTemplate.setToRecipients(toRecipients);
		mailTemplate.setCc(cc);
		mailTemplate.setBcc(bcc);
		mailTemplate.setTemplateName("mail_templates/tci_unreachable_vehicle_email.vm");
		mailTemplate.setSender(from);
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("User",ApiConstant.TCI.EMAIL_USER);
		attributes.put("clientName",clientName);
		attributes.put("clientEmail",from);
		attributes.put("msg", msg);
		mailTemplate.setSubject("Inactive Vehicle-"+CustomTimezoneUtils.UTCDateToUserDateTimeString(ApiConstant.TCI.TIMEZONE,new Date()));
		mailTemplate.setAttributes(attributes);
		try {
			logger.info("TCI UNREACHABLE VEHICLE EMAIL SENDING..........");
			gpsEmailService.sendMail(mailTemplate);
		} catch (IOException e) {
			e.printStackTrace();
			logger.info("TCI UNREACHABLE VEHICLE EMAIL EXCEPTION", e);
		}*/
	}
	
	public void tciPushVehicleDataFromGPSApi(ClientDataPushReport clientDataPushReport) {
		List<TciPushVehicleDataFromGPSRequestModel> body=tciVehicleData();
		logger.info("TciPushVehicleDataFromGPSRequest Data Size    :    "+body.size());
		clientDataPushReport.setCount(body.size());
		Gson gson=new Gson();
		String json=gson.toJson(body);
		String response=tciTripService.pushTciData(json,ApiConstant.TCI.PUSH_VEHICLE_DATA_METHOD);
		Document document =tciTripService.parseXmlFile(response);
		String output = document.getElementsByTagName("pushVehicleDataFromGPSResult").item(0).getTextContent();
		TciPushApiResponse apiResponse=gson.fromJson(output, TciPushApiResponse.class);
		logger.info("TCI VEHICLE DATA PUSH API REPONSE "+apiResponse);
		if(apiResponse.getSUCCESS().equalsIgnoreCase("true")) {
			clientDataPushReport.setSuccessCount(body.size());
			clientDataPushReport.setStatus(ApiConstant.Status.SUCCESS);
		}else {
			clientDataPushReport.setSuccessCount(0);
			clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
		}
		clientDataPushReport.setEndTime(new Date());
		 mongoTemplate.save(clientDataPushReport);
	}
	
	private List<TciPushVehicleDataFromGPSRequestModel> tciVehicleData() {
		List<TciPushVehicleDataFromGPSRequestModel> models=new ArrayList<>();
		List<Vehicle> vehicles = vehicleService.findByTransporterUsernameAndActive(ApiConstant.TCI.TRANSPORTER, true);
		logger.info("TCI VEHICLE COUNT   :   "+vehicles.size());
		for (Vehicle vehicle : vehicles) {
			TciPushVehicleDataFromGPSRequestModel model=createTciVehicleData(vehicle);
			models.add(model);
		}
		return models;
	}

	private TciPushVehicleDataFromGPSRequestModel createTciVehicleData(Vehicle vehicle) {
		TciPushVehicleDataFromGPSRequestModel model=new TciPushVehicleDataFromGPSRequestModel();
		String vehicelNo=vehicle.getVehicleNo();
		String from = "";
		String to = "";
		String delayTime="0";
		String travelledKm ="0";
		List<Trip> trips = vehicle.getTrips();
		if (trips != null && !trips.isEmpty()) {
			Trip trip = trips.get(0);
			Tour tour = trip.getTour();
			from = tour.getSource().getName();
			to = tour.getDestination().getName();
			double tripKm=vehicle.getTotkm()-trip.getActualDepartingKm();
			travelledKm=ConversionUtils.decimalFormat(tripKm);
			if(trip.getActualDepartureTime()!=null) {
			  long seconds=tciTripService.getDelayTime(trip.getActualDepartureTime(),tripKm);
			  delayTime=ConversionUtils.convertSecondToHMm(seconds);
			}
		}
		TerminalPacketRecord terminalPacketRecord=vehicle.getTerminalPacketRecord();
		String date_time=CustomTimezoneUtils.UTCDateToTciDateTimeStringHHmmss(vehicle.getTransporterTz(), vehicle.getLu());
		String status =from+" - "+to+" "+CustomTimezoneUtils.UTCDateToUserDateTimeStringHHmm(vehicle.getTransporterTz(),new Date());
		String speed =String.valueOf(terminalPacketRecord.getSpeed());
		String location =terminalPacketRecord.getAddress();
		String stopTime = "0";
		String deviceStatus = "ACTIVE";
		//
		model.setVehicle_no(vehicelNo);
		model.setDate_time(date_time);
		model.setLocation(location);
		model.setSpeed(speed);
		model.setStop_time(stopTime);
		model.setStatus(status);
		model.setFrom(from);
		model.setTo(to);
		model.setDevice_status(deviceStatus);
		model.setTravelled_km(travelledKm);
		model.setDelay_time(delayTime);
		double[] loc=terminalPacketRecord.getLocation();
		model.setLattitude((new DecimalFormat(".########")).format(loc[1]));
		model.setLongitude((new DecimalFormat(".########")).format(loc[0]));
		return model;
	}
	//

	private String response = "<?xml version=\"1.0\" encoding=\"utf-8\"?> <soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"> <soap:Body> <getVehicleDataForGPSResponse xmlns=\"http://www.tciexpress.in/\"> <getVehicleDataForGPSResult> <GpsData xmlns=\"\"> <Trip> <TripSheetNumber>4006158</TripSheetNumber> <OriginHub>XMDH</OriginHub> <OriginHubName>MADRAS-HUB</OriginHubName> <DestinationHub>XRWH</DestinationHub> <DestinationHubName>REWARI-HUB</DestinationHubName> <VehicleNumber>RJ14GE8340</VehicleNumber> <ScheduleArrivalDate>27-AUG-2018 14:31:57</ScheduleArrivalDate> <Distance>2300</Distance> <ScheduleHours>72</ScheduleHours> <VendorCode>K94R</VendorCode> <VendorName>RAM KUMAR</VendorName> <ArrivalDepartDate>24-AUG-2018 14:31:57</ArrivalDepartDate> <TripSheetDate>23-AUG-2018 00:00:00</TripSheetDate> </Trip> <Trip> <TripSheetNumber>4006162</TripSheetNumber> <OriginHub>XRWH</OriginHub> <OriginHubName>REWARI-HUB</OriginHubName> <DestinationHub>XAMH</DestinationHub> <DestinationHubName>AMBALA-HUB</DestinationHubName> <VehicleNumber>RJ14GF8323</VehicleNumber> <ScheduleArrivalDate>28-NOV-2018 03:16:37</ScheduleArrivalDate> <Distance>299</Distance> <ScheduleHours>12</ScheduleHours> <VendorCode>G47M</VendorCode> <VendorName>MUKESH</VendorName> <ArrivalDepartDate>27-NOV-2018 15:16:37</ArrivalDepartDate> <TripSheetDate>27-NOV-2018 00:00:00</TripSheetDate> </Trip></GpsData> </getVehicleDataForGPSResult> </getVehicleDataForGPSResponse> </soap:Body> </soap:Envelope>";
}
