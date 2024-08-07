/**
	* README
	* This extension is used to Update to MWOHED table record.
	*
	* Name: UpdMoMaterial
	* Description: Update record in MWOHED
	* Date Changed By Description
	* 20240612 Amit Powar Initial Development
	* 
	*/

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UpdMOHeader extends ExtendM3Transaction {

	public UpdMOHeader(MIAPI mi, MICallerAPI miCaller, DatabaseAPI database, ProgramAPI program, LoggerAPI logger,IonAPI ion) {
		this.mi = mi;
		this.database = database;
		this.program = program;
		this.logger = logger;
		this.miCaller = miCaller;
		this.ion = ion;
	}
	private final MIAPI mi;
	private final MICallerAPI miCaller;
	private final DatabaseAPI database;
	private final ProgramAPI program;
	private final LoggerAPI logger;
	private final IonAPI ion;

	int currentCompany = program.getLDAZD().CONO;
	String currentUser = program.getUser();

	private String iCONO;
	private String iFACI;
	private String iPRNO;
	private String iMFNO;

	String manufacturingQtyMAQA = "0.0";
	String receivedQtyRVQA = "0.0";
	String warehouseWHLO = "";
	String productPRNO = "";
	String lotNumberBANO = "";
	String statusBlanceIDSTAS = "";
	String statusManufacturingOrderWHST = "";

	public void main() {
		iCONO = mi.in.get("CONO") == null ? currentCompany : mi.in.get("CONO");
		iFACI = mi.in.get("FACI") == null ? "" : mi.in.get("FACI").toString().trim();
		iPRNO = mi.in.get("PRNO") == null ? "" : mi.in.get("PRNO").toString().trim();
		iMFNO = mi.in.get("MFNO") == null ? "" : mi.in.get("MFNO").toString().trim();

		Map<String, String> params= ["CONO":iCONO.toString(),"FACI":iFACI.toString(),"PRNO":iPRNO.toString(),"MFNO":iMFNO.toString()];
		Closure<?> callback = {
			Map<String, String> response ->
				if(response.MAQA != null){
					manufacturingQtyMAQA = response.MAQA; 
				}
				if(response.RVQA != null){
					receivedQtyRVQA = response.RVQA;
				}
				if(response.WHLO != null){
					warehouseWHLO = response.WHLO;
				}
				if(response.PRNO != null){
					productPRNO = response.PRNO;
				}
				if(response.BANO != null){
					lotNumberBANO = response.BANO;
				}
		}
		miCaller.call("PMS100MI","Get",params,callback);

		if(manufacturingQtyMAQA.toDouble() != receivedQtyRVQA.toDouble()){

			Map<String, String> paramsLstBalID= ["CONO":iCONO.toString(),"WHLO":warehouseWHLO.toString(),"ITNO":productPRNO.toString(),"BANO":lotNumberBANO.toString()];
			Closure<?> callbackLstBalID = {
			Map<String, String> responseLstBalID ->
				if(responseLstBalID.STAS != null){
					statusBlanceIDSTAS = responseLstBalID.STAS;
				}
			}
			miCaller.call("MMS060MI","LstBalID",paramsLstBalID,callbackLstBalID);

			if(statusBlanceIDSTAS == "2" || statusBlanceIDSTAS == null || statusBlanceIDSTAS == "" || statusBlanceIDSTAS == "3" || statusBlanceIDSTAS == "1"){
				//upate record in MWOHED table
				DBAction query = database.table("MWOHED")
					.index("00")
					.build();
				DBContainer container = query.getContainer();
				container.set("VHCONO", iCONO.toInteger());
				container.set("VHFACI", iFACI);
				container.set("VHPRNO", iPRNO);
				container.set("VHMFNO", iMFNO);

				Closure<?> updateCallBack = { LockedResult lockedResult ->
					statusManufacturingOrderWHST = lockedResult.getString("VHWHST");
					lockedResult.set("VHMAQA", lockedResult.getDouble("VHRVQA"));
					lockedResult.set("VHMAQT", lockedResult.getDouble("VHRVQT"));
					lockedResult.set("VHLMDT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger());
					lockedResult.set("VHCHNO", lockedResult.getInt("VHCHNO") + 1);
					lockedResult.set("VHCHID", program.getUser());

					lockedResult.update();
				}
				query.readLock(container,updateCallBack);

				if(statusManufacturingOrderWHST == "80"){
					if(currentUser != "MECSVC"){
						String endpoint = "/M3/m3api-rest/v2/execute/PMS100MI/UpdMO";
						Map<String, String> headers = ["Accept": "application/json"];
						Map<String, String> queryParameters = ["CONO":iCONO.toString(),"FACI":iFACI.toString(),"PRNO":iPRNO.toString(),"MFNO":iMFNO.toString()];
						Map<String, String> formParameters =(Map)null;
						IonResponse response = ion.get(endpoint, headers, queryParameters);
						if (response.getError()) {
							mi.error("Failed to call the ION API for PMS100MI/UpdMO, detailed error message: ${response.getErrorMessage()}".toString());
							mi.outData.put("ECOD",response.getStatusCode().toString());
							mi.outData.put("EMSG","Failed to call the ION API for PMS100MI/UpdMO, detailed error message: ${response.getErrorMessage()}".toString());
							return;
						}
						if (response.getStatusCode() != 200) {
							mi.outData.put("ECOD",response.getStatusCode().toString());
							mi.outData.put("EMSG","Failed to call the ION API for PMS100MI/UpdMO, Expected status 200 but got ${response.getStatusCode()} instead".toString());
							mi.error("Failed to call the ION API for PMS100MI/UpdMO, Expected status 200 but got ${response.getStatusCode()} instead".toString());
							return;
						}
						mi.outData.put("ECOD","200");
						mi.outData.put("EMSG","");
					}else{
						mi.outData.put("ECOD","401");
						mi.outData.put("EMSG","Failed to call the ION API for PMS100MI/UpdMO. Current user :"+currentUser);
					}
				}else{
					mi.error("The status of the manufacturing order is different from 80");
					mi.outData.put("ECOD","500");
					mi.outData.put("EMSG","The status of the manufacturing order is different from 80");
				}
			}else{
				mi.error("Status of balance id is :"+statusBlanceIDSTAS);
				mi.outData.put("ECOD","500");
				mi.outData.put("EMSG","Status of balance id is :"+statusBlanceIDSTAS);
			}
		}else{
			mi.error("MAQA $manufacturingQtyMAQA and RVQA $receivedQtyRVQA quantities match.");
			mi.outData.put("ECOD","500");
			mi.outData.put("EMSG","MAQA $manufacturingQtyMAQA and RVQA $receivedQtyRVQA quantities match.");
		}
	}
}