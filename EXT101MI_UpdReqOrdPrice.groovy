/**
 * README
 * This extension is used to Update inventory account price  in  table MGLINE.
 *
 * Name: UpdReqOrdPrice
 * Description: Update record in MGLINE
 * Date	          Changed By			      Description
 * 20241024       Amit Powar            Initial Development - Update inventory account price in MGLINE table.
 * 
 */
 
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class UpdReqOrdPrice extends ExtendM3Transaction {
  
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final ProgramAPI program;
  
  private int inCONO;
  private String inTRNR;
  private int inPONR;
  private int inPOSX;
  private double inTRPR;
  
  
  public UpdReqOrdPrice(MIAPI mi, DatabaseAPI database, ProgramAPI program) {
    this.mi = mi;
    this.database = database;
    this.program = program;
  }
  
  public void main() {
    
   inCONO =  mi.in.get("CONO") == null ? 0 : (Integer)mi.in.get("CONO");
   inTRNR =  mi.in.get("TRNR") == null ? "" : mi.in.get("TRNR").toString().trim();
   inPONR =  mi.in.get("PONR") == null ? 0 : (Integer)mi.in.get("PONR");
   inPOSX =  mi.in.get("POSX") == null ? 0 : (Integer)mi.in.get("POSX");
   inTRPR =  mi.in.get("TRPR") == null ? 0.0d : (Double)mi.in.get("TRPR");
   
   
  DBAction dbAction = database.table("MGLINE").index("00").build();
    DBContainer MGLINE = dbAction.getContainer();
    MGLINE.set("MRCONO", inCONO);
    MGLINE.set("MRTRNR", inTRNR);
    MGLINE.set("MRPONR", inPONR);
    MGLINE.set("MRPOSX", inPOSX);
    
    if(!validation(inCONO, inTRNR, inPONR, inPOSX)){
      mi.error("Can not update line please check order status, transaction type and invoice accounting method.");
      return;
    }
    
    Closure<Boolean> updateMGLINE = { LockedResult record ->
      record.set("MRTRPR", inTRPR);
      record.set("MRLMDT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger());
      record.set("MRCHNO", record.getInt("MRCHNO") + 1);
      record.set("MRCHID", program.getUser());
      record.update();
    }
    
    if (!dbAction.readLock(MGLINE, updateMGLINE)) {
      mi.error("Record was not updated");
      return;
    }
  }
  
  /**
   * Checks whether the M3 table has data or not. 
   * M3 tables - MGHEAD, MGLINE, MGTYPE, MITFAC.
   * @param iCONO - company
   * @param iTRNR - order number
   * @param iPONR - order line number
   * @param iPOSX - line suffix
   * @return boolean value true or false - if satisfy all conditions then return true else return false.
   */
  private boolean validation(int iCONO,String iTRNR,int iPONR, int iPOSX){
    String facility;
    String orderType;
    String item;
    //Get orderType and Facility from MGHEAD table.
    DBAction query = database.table("MGHEAD").index("00").selection("MGTRTP","MGFACI","MGTRSL","MGTRSH").build();
    DBContainer container = query.getContainer();
    container.set("MGCONO", iCONO);
    container.set("MGTRNR", iTRNR);
    if (query.read(container)) {
      facility = container.get("MGFACI");
      orderType = container.get("MGTRTP");
      String lowestStatus = container.get("MGTRSL");
      String higestStatus = container.get("MGTRSH");
      if(lowestStatus != "99" && higestStatus != 99){
        //Read Item number from MGLINE table.
        DBAction queryMGLINE = database.table("MGLINE").index("00").selection("MRITNO").build();
        DBContainer containerMGLINE = queryMGLINE.getContainer();
        containerMGLINE.set("MRCONO", iCONO);
        containerMGLINE.set("MRTRNR", iTRNR);
        containerMGLINE.set("MRPONR", iPONR);
        containerMGLINE.set("MRPOSX", iPOSX);
        if (queryMGLINE.read(containerMGLINE)) {
          item = containerMGLINE.get("MRITNO");
          //Get transaction type from MGTYPE table and validate.
          DBAction queryMGTYPE = database.table("MGTYPE").index("00").selection("YXTTYP").build();
          DBContainer containerMGTYPE = queryMGTYPE.getContainer();
          containerMGTYPE.set("YXCONO", iCONO);
          containerMGTYPE.set("YXTRTP", orderType);
          if (queryMGTYPE.read(containerMGTYPE)) {
            String transactionType = containerMGTYPE.get("YXTTYP");
            if(transactionType != "40"){
              return false;
            }else{
              //Get invoice accounting method from MITFAC table and validate. 
              DBAction queryMITFAC = database.table("MITFAC").index("00").selection("M9VAMT").build();
              DBContainer containerMITFAC = queryMITFAC.getContainer();
              containerMITFAC.set("M9CONO", iCONO);
              containerMITFAC.set("M9FACI", facility);
              containerMITFAC.set("M9ITNO", item);
              if (queryMITFAC.read(containerMITFAC)) {
                String invoiceAccMethod = containerMITFAC.get("M9VAMT");
                if(invoiceAccMethod != "2"){
                  return false;
                }else{
                  return true;
                }
              }else{
                return false;
              }
            }
          }else{
            return false;
          }
        }else{
          return false;
        }  
      }
    }else{
      return false;
    }
  }
}