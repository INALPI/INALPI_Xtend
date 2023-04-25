/**
* README
* This extension is being used to add EXPI to EXTCUS
*
* Name: EXTCUS.add and Update for MMS480PF
* 
* 
* Date                        Changed By                    Description
* 20230330                  Ravina Kurkure           Add and Update for MMS480PF
* 20230425                  Ravina Kurkure          Added Function Javadoc Comments,added new fields CTQT, RGDT, RGTM, CHID, CHNO, LMDT
*/
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.lang.Math

public class OutputProcessExtension extends ExtendM3Trigger {
 
  private final MethodAPI method
  private final ProgramAPI program
  private final DatabaseAPI database
  private final LoggerAPI logger
  
  //GlobalVariables Declartion
    int lineNo_MMS480PF
    int currentCompany
    String orderNumber_MMS480PF 
    int orderCategory_MMS480PF 
    String warehouse_MMS480PF 
    long deliveryNumber_MMS480PF
    String newItemNumber_MMS480PF
    String oldItemNumber_MMS480PF
    String newLotNumber_MMS480PF
    String oldLotNumber_MMS480PF
    int oldlineNo_MMS480PF
    int newlineNo_MMS480PF
    int oldRIDX_MMS480PF
    int newRIDX_MMS480PF
    double olddeliveredQty_MMS480PF = 0.0
    double newdeliveredQty_MMS480PF = 0.0
    double tempDeliveredQty_MMS480PF= 0.0
    double oldcatchWeight_MMS480PF = 0.0
    double newcatchWeight_MMS480PF = 0.0
    double tempcatchWeight_MMS480PF = 0.0
    int colli_MMS480PF = 0
   
    double tempColli = 0.0
    
  public OutputProcessExtension(MethodAPI method, DatabaseAPI database,LoggerAPI logger,ProgramAPI program) {
    this.method = method
    this.program = program
    this.database = database
    this.logger =logger
  }
  /**
   * Main -Init Function
   * @return
   */
  public void main() {
    String printFile = method.getArgument(0) as String
    String jobNumber = method.getArgument(1) as String
    String structure = method.getArgument(2) as String
    int variant  = method.getArgument(3) as int
    int section  = method.getArgument(4) as int
    HashMap<String, Object> fieldMap =method.getArgument(5) as HashMap
    if(printFile.equals("MMS480PF")){
      if(structure.equals("IN1_CUS_03-01") && section == 6){
        createEXTCUSRecord(fieldMap)
      }
    }
  }
  /**
   * createEXTCUSRecord - enter new data into EXTCUS 
   * @return
   */
  boolean createEXTCUSRecord(HashMap<String,Object> fieldMap){
     orderNumber_MMS480PF =fieldMap.get("OQRIDN")
     orderCategory_MMS480PF = fieldMap.get("OQRORC") as int
     warehouse_MMS480PF = fieldMap.get("OQWHLO")
     deliveryNumber_MMS480PF = fieldMap.get("OQDLIX") as long
     deprecateItemEXTCUS()
     getDatafromMFTRNS()
  }
  
  /**
   * deprecateItemEXTCUS - Delete all the records from Current Company
   * @return
   */
  void deprecateItemEXTCUS() {
    currentCompany = (Integer)program.getLDAZD().CONO
    DBAction queryEXTCUS = database.table("EXTCUS").index("00").selection("EXCONO", "EXWHLO", "EXRORC", "EXRIDN").build()
    DBContainer container = queryEXTCUS.getContainer()
    container.set("EXCONO", currentCompany)
    container.set("EXWHLO",warehouse_MMS480PF )
    container.set("EXRORC", orderCategory_MMS480PF)
    container.set("EXRIDN",orderNumber_MMS480PF )
    queryEXTCUS.readAllLock(container,4, deleterCallback)
  }

  Closure<?> deleterCallback = { LockedResult lockedResult ->
    lockedResult.delete()
  }

   /**
   * getDatafromMFTRNS - get all the required fields to be added in Table from MFTRNS Get per LINE, ITEM AND LOTNumber
   * @return
   */
  void getDatafromMFTRNS() {
        currentCompany = (Integer)program.getLDAZD().CONO
        DBAction query = database.table("MFTRNS").index("10").selection("OSCONO","OSWHLO","OSRORC","OSRIDN","OSRIDL","OSRIDX","OSITNO","OSBANO","OSDLQT","OSNEWE").build()
        DBContainer container = query.getContainer()
        container.set("OSCONO",currentCompany )
        container.set("OSWHLO", warehouse_MMS480PF)
        container.set("OSRORC",orderCategory_MMS480PF)
        container.set("OSRIDN",orderNumber_MMS480PF)
        query.readAll(container, 4, releasedItemProcessor)
  }

  Closure<?> releasedItemProcessor = { DBContainer container ->
    newlineNo_MMS480PF = container.get("OSRIDL")
    newRIDX_MMS480PF = container.get("OSRIDX")
    newItemNumber_MMS480PF = container.get("OSITNO")
    newLotNumber_MMS480PF = container.get("OSBANO")
    newdeliveredQty_MMS480PF = container.get("OSDLQT") as double
    newcatchWeight_MMS480PF = container.get("OSNEWE") as double
   
    if(newlineNo_MMS480PF.equals(oldlineNo_MMS480PF)&& newRIDX_MMS480PF.equals(oldRIDX_MMS480PF) && newItemNumber_MMS480PF.equals(oldItemNumber_MMS480PF) && newLotNumber_MMS480PF.equals(oldLotNumber_MMS480PF)){
        tempDeliveredQty_MMS480PF = olddeliveredQty_MMS480PF + newdeliveredQty_MMS480PF
        olddeliveredQty_MMS480PF = tempDeliveredQty_MMS480PF
        
        tempcatchWeight_MMS480PF = oldcatchWeight_MMS480PF + newcatchWeight_MMS480PF
        oldcatchWeight_MMS480PF = tempcatchWeight_MMS480PF
        
        if(tempDeliveredQty_MMS480PF != 0.0 && newLotNumber_MMS480PF.equals(oldLotNumber_MMS480PF) ){
          getColliData(newItemNumber_MMS480PF,tempDeliveredQty_MMS480PF)
          
          updateDeliveredQtyEXTCUS(tempDeliveredQty_MMS480PF,oldlineNo_MMS480PF,oldRIDX_MMS480PF,oldItemNumber_MMS480PF,oldLotNumber_MMS480PF,tempcatchWeight_MMS480PF,colli_MMS480PF)
          
          tempDeliveredQty_MMS480PF = 0.0
          tempcatchWeight_MMS480PF = 0.0
          tempColli = 0.0
        }
      } 
      else{
          getColliData(newItemNumber_MMS480PF,newdeliveredQty_MMS480PF)
          insertDataEXTCUS(newlineNo_MMS480PF,newRIDX_MMS480PF,newLotNumber_MMS480PF,newItemNumber_MMS480PF,newdeliveredQty_MMS480PF,newcatchWeight_MMS480PF,colli_MMS480PF)
          oldlineNo_MMS480PF = newlineNo_MMS480PF
          oldRIDX_MMS480PF = newRIDX_MMS480PF
          oldItemNumber_MMS480PF = newItemNumber_MMS480PF
          oldLotNumber_MMS480PF = newLotNumber_MMS480PF
          olddeliveredQty_MMS480PF = newdeliveredQty_MMS480PF
          oldcatchWeight_MMS480PF = newcatchWeight_MMS480PF
        }
  }
  
   /**
   * getColliData - Get the rounded colli data from MITAUN base on DMCF
   * @return
   */
  void getColliData(newItemNumber_MMS480PF,newdeliveredQty_MMS480PF){
          
          int currentCompany = (Integer)program.getLDAZD().CONO
          DBAction queryMITAUN = database.table("MITAUN").index("00").selection("MUCONO", "MUITNO", "MUAUTP", "MUALUN","MUCOFA","MUDMCF").build()
          DBContainer containerMITAUN = queryMITAUN.getContainer()
          containerMITAUN.set("MUCONO", currentCompany)
          containerMITAUN.set("MUITNO",newItemNumber_MMS480PF)
          containerMITAUN.set("MUAUTP",1)
          containerMITAUN.set("MUALUN","CT")
          
          if (queryMITAUN.read(containerMITAUN)) {
            double COFA = containerMITAUN.get("MUCOFA")
            int DMCF = containerMITAUN.get("MUDMCF")
            if(DMCF == 1){
              tempColli = (double)newdeliveredQty_MMS480PF / COFA
            }else if(DMCF == 2){
              tempColli = (double)newdeliveredQty_MMS480PF * COFA
            }
            colli_MMS480PF = Math.round((double)tempColli) as int
          }
  }
  /**
   * updateDeliveredQtyEXTCUS - Update only deliveredQty and Catch Weight to get the sum of it.
   * @return
   */
  void updateDeliveredQtyEXTCUS(tempDeliveredQty_MMS480PF,oldlineNo_MMS480PF,oldRIDX_MMS480PF,oldItemNumber_MMS480PF,oldLotNumber_MMS480PF,tempcatchWeight_MMS480PF,colli_MMS480PF){
        int currentCompany = (Integer)program.getLDAZD().CONO
        DBAction query = database.table("EXTCUS").index("00").selection("EXCONO","EXWHLO","EXRORC","EXRIDN","EXRIDL","EXITNO","EXBANO","EXDLQT").build()
        DBContainer containerEXTCUS = query.getContainer()
        containerEXTCUS.set("EXCONO", currentCompany)
        containerEXTCUS.set("EXWHLO", warehouse_MMS480PF)
        containerEXTCUS.set("EXRORC", orderCategory_MMS480PF)
        containerEXTCUS.set("EXRIDN", orderNumber_MMS480PF)
        containerEXTCUS.set("EXRIDL", oldlineNo_MMS480PF)
        containerEXTCUS.set("EXRIDX", oldRIDX_MMS480PF)
        containerEXTCUS.set("EXITNO", oldItemNumber_MMS480PF)
        containerEXTCUS.set("EXBANO", oldLotNumber_MMS480PF)
        query.readLock(containerEXTCUS, updateCallBack)
  }
  Closure<?> updateCallBack = { LockedResult lockedResult ->
    lockedResult.set("EXDLQT", tempDeliveredQty_MMS480PF)
    lockedResult.set("EXKGQT", tempcatchWeight_MMS480PF)
    lockedResult.set("EXCTQT", colli_MMS480PF)
    lockedResult.set("EXLMDT", LocalDate.now().format(DateTimeFormatter.ofPattern("YYYYMMdd")).toInteger())
    lockedResult.set("EXCHNO", lockedResult.getInt("EXCHNO") + 1)
    lockedResult.set("EXCHID", program.getUser())
    lockedResult.update()
  }
  
  /**
   * insertDataEXTCUS - insert all required data into EXTCUS
   * @return
   */
 void insertDataEXTCUS(newlineNo_MMS480PF,newRIDX_MMS480PF,newLotNumber_MMS480PF,newItemNumber_MMS480PF,newdeliveredQty_MMS480PF,newcatchWeight_MMS480PF,colli_MMS480PF){
        
        DBContainer container1 = database.createContainer("EXTCUS")
        DBAction query = database.table("EXTCUS")
        .index("00")
        .selectAllFields().build()
        
        container1.set("EXCONO",currentCompany)
        container1.set("EXWHLO",warehouse_MMS480PF)
        container1.set("EXRORC",orderCategory_MMS480PF)
        container1.set("EXRIDN",orderNumber_MMS480PF)
        container1.set("EXRIDL",newlineNo_MMS480PF)
        container1.set("EXRIDX",newRIDX_MMS480PF)
        container1.set("EXBANO",newLotNumber_MMS480PF)
        container1.set("EXITNO",newItemNumber_MMS480PF)
        container1.set("EXDLQT",newdeliveredQty_MMS480PF)
        container1.set("EXKGQT",newcatchWeight_MMS480PF)
        container1.set("EXCTQT",colli_MMS480PF)
        container1.set("EXRGDT", LocalDate.now().format(DateTimeFormatter.ofPattern("YYYYMMdd")).toInteger())
        container1.set("EXRGTM", LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss")).toInteger())
        container1.set("EXLMDT", LocalDate.now().format(DateTimeFormatter.ofPattern("YYYYMMdd")).toInteger())
        container1.set("EXCHNO", 0)
        container1.set("EXCHID", program.getUser())
        query.insert(container1)
 } 
}
