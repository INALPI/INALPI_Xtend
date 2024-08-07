
/**
	* README
	* This extension is used to Update to MWOMAT table record.
	*
	* Name: UpdMoMaterial
	* Description: Update record in MWOMAT
	* Date Changed By Description
	* 20240611 Amit Powar Initial Development
	* 
	*/
	import java.time.LocalDate;
	import java.time.LocalTime;
	import java.time.LocalDateTime;
	import java.time.format.DateTimeFormatter;
	public class UpdMoMaterial extends ExtendM3Transaction {
		public UpdMoMaterial(MIAPI mi, MICallerAPI miCaller, DatabaseAPI database, ProgramAPI program, LoggerAPI logger,IonAPI ion) {
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

		private String iCONO;
		private String iFACI;
		private String iPRNO;
		private String iMFNO;
		private String iMSEQ;

		int currentCompany = program.getLDAZD().CONO;
		String currentUser = program.getUser();

		String bycoProductCodeBYPR = "";
		String meterialStatusWMST = "";
		String warehouseWHLO = "";
		String componentNumberMTNO = "";
		String lotNumberBANO = "";
		String statusBlanceIDSTAS = "";

		public void main() {
			iCONO = mi.in.get("CONO") == null ? currentCompany : mi.in.get("CONO");
			iFACI = mi.in.get("FACI") == null ? "" : mi.in.get("FACI").toString().trim();
			iPRNO = mi.in.get("PRNO") == null ? "" : mi.in.get("PRNO").toString().trim();
			iMFNO = mi.in.get("MFNO") == null ? "" : mi.in.get("MFNO").toString().trim();
			iMSEQ = mi.in.get("MSEQ") == null ? "" : mi.in.get("MSEQ").toString().trim();

			Map<String, String> params= ["CONO":iCONO.toString(),"FACI":iFACI.toString(),"PRNO":iPRNO.toString(),"MFNO":iMFNO.toString(),"MSEQ":iMSEQ.toString()];
			Closure<?> callback = {
				Map<String, String> response ->
					if(response.BYPR != null){
						bycoProductCodeBYPR = response.BYPR; 
					}
					if(response.WMST != null){
						meterialStatusWMST = response.WMST;
					}
					if(response.MTNO != null){
						componentNumberMTNO = response.MTNO;
					}
					if(response.BANO != null){
						lotNumberBANO = response.BANO;
					}
			}
			miCaller.call("PMS100MI","GetMatLine",params,callback);

			if(bycoProductCodeBYPR == "1"){

				Map<String, String> paramsLstBalID= ["CONO":iCONO.toString(),"WHLO":warehouseWHLO.toString(),"ITNO":componentNumberMTNO.toString(),"BANO":lotNumberBANO.toString()];
				Closure<?> callbackLstBalID = {
				Map<String, String> responseLstBalID ->
					if(responseLstBalID.STAS != null){
						statusBlanceIDSTAS = responseLstBalID.STAS;
					}
				}
				miCaller.call("MMS060MI","LstBalID",paramsLstBalID,callbackLstBalID);

				if(((statusBlanceIDSTAS == "2" || statusBlanceIDSTAS == null || statusBlanceIDSTAS == "") && meterialStatusWMST != "99") || (statusBlanceIDSTAS == "1" && meterialStatusWMST == "99")){

					DBAction query = database.table("MWOMAT")
						.index("00")
						.build();
					DBContainer container = query.getContainer();
					container.set("VMCONO", iCONO.toInteger());
					container.set("VMFACI", iFACI);
					container.set("VMPRNO", iPRNO);
					container.set("VMMFNO", iMFNO);
					container.set("VMMSEQ", iMSEQ.toInteger());

					Closure<?> updateCallBack = { LockedResult lockedResult ->
						lockedResult.set("VMRPQA", lockedResult.getDouble("VMRVQA"));
						lockedResult.set("VMRPQT", lockedResult.getDouble("VMRVQT"));
						lockedResult.set("VMLMDT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger());
						lockedResult.set("VMCHNO", lockedResult.getInt("VMCHNO") + 1);
						lockedResult.set("VMCHID", program.getUser());

						lockedResult.update();
					}
					query.readLock(container,updateCallBack);

					if(currentUser != "MECSVC"){
						String endpoint = "/M3/m3api-rest/v2/execute/PMS100MI/UpdMatLine";
						Map<String, String> headers = ["Accept": "application/json"];
						Map<String, String> queryParameters = ["CONO":iCONO.toString(),"FACI":iFACI.toString(),"PRNO":iPRNO.toString(),"MFNO":iMFNO.toString(),"MSEQ":iMSEQ.toString()];
						Map<String, String> formParameters =(Map)null;
						IonResponse response = ion.get(endpoint, headers, queryParameters);
						if (response.getError()) {
							mi.error("Failed to make a call to the ION API at PMS100MI/UpdMatLine., detailed error message: ${response.getErrorMessage()}".toString());
							mi.outData.put("ECOD",response.getStatusCode().toString());
							mi.outData.put("EMSG","Failed to make a call to the ION API at PMS100MI/UpdMatLine., detailed error message: ${response.getErrorMessage()}".toString());
							return;
						}
					if (response.getStatusCode() != 200) {
						mi.error("Failed to make a call to the ION API at PMS100MI/UpdMatLine., Expected status 200 but got ${response.getStatusCode()} instead".toString());
						mi.outData.put("ECOD",response.getStatusCode().toString());
						mi.outData.put("EMSG","Failed to make a call to the ION API at PMS100MI/UpdMatLine., Expected status 200 but got ${response.getStatusCode()} instead".toString());
						return;
					}
					mi.outData.put("ECOD","200");
					mi.outData.put("EMSG","");
					}else{
						mi.outData.put("ECOD","401");
						mi.outData.put("EMSG","Failed to make a call to the ION API at PMS100MI/UpdMatLine. Current user :"+currentUser);
					}
				}else{
					mi.error("Please verify the status of the balance ID or check the material status. Status balance id is:"+statusBlanceIDSTAS+" Material status is :"+meterialStatusWMST);
					mi.outData.put("ECOD","500");
					mi.outData.put("EMSG","Please verify the status of the balance ID or check the material status. Status balance id is:"+statusBlanceIDSTAS+" Material status is :"+meterialStatusWMST);
				}
			}else{
				mi.error("Please verify the by-product code (BYPR) : ${bycoProductCodeBYPR}");
				mi.outData.put("ECOD","500");
				mi.outData.put("EMSG","Please verify the by-product code (BYPR) : ${bycoProductCodeBYPR}");
			}
		}
	}