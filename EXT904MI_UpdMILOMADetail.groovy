/**
 * README
 * This extension is used to Update dates information to creation date in MILOMA.
 *
 * Name: UpdMILOMADetail
 * Description: Update record in MILOMA
 * Date          Changed By          Description
 * 20241024      Amit Powar         Initial Development - Update dates information to creation date in MILOMA.
 */
 
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.text.ParseException;

public class UpdMILOMADetail extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final ProgramAPI program;
  
  private int inCONO;
  private String inITNO;
  private String inBANO;
  private int inMFDT;
  private int inREDA;
  private int inEXPI;
  private int inSEDT;
  private int inBBDT;
  
  public UpdMILOMADetail(MIAPI mi, DatabaseAPI database, ProgramAPI program) {
    this.mi = mi;
    this.database=database;
    this.program=program;
  }
  
  public void main() {
   inCONO =  mi.in.get("CONO") == null ? 0 : (Integer)mi.in.get("CONO");
   inITNO =  mi.in.get("ITNO") == null ? "" : mi.in.get("ITNO").toString().trim();
   inBANO =  mi.in.get("BANO") == null ? "" : mi.in.get("BANO").toString().trim();
   inMFDT =  mi.in.get("MFDT") == null ? 0 : (Integer)mi.in.get("MFDT");
   inREDA =  mi.in.get("REDA") == null ? 0 : (Integer)mi.in.get("REDA");
   inEXPI =  mi.in.get("EXPI") == null ? 0 : (Integer)mi.in.get("EXPI");
   inSEDT =  mi.in.get("SEDT") == null ? 0 : (Integer)mi.in.get("SEDT");
   inBBDT =  mi.in.get("BBDT") == null ? 0 : (Integer)mi.in.get("BBDT");
  
  if(!checkDateValidity(inMFDT)){
    mi.error("${inMFDT} is an invalid MFDT");
    return;
  }
  
  if(!checkDateValidity(inREDA)){
    mi.error("${inREDA} is an invalid REDA");
    return;
  }
  
  if(!checkDateValidity(inEXPI)){
    mi.error("${inEXPI} is an invalid EXPI");
    return;
  }
  
  if(!checkDateValidity(inSEDT)){
    mi.error("${inSEDT} is an invalid SEDT");
    return;
  }
  
  if(!checkDateValidity(inBBDT)){
    mi.error("${inBBDT} is an invalid BBDT");
    return;
  }
  DBAction dbAction = database.table("MILOMA").index("00").build();
  DBContainer MILOMA = dbAction.getContainer();
  MILOMA.set("LMCONO", inCONO);
  MILOMA.set("LMITNO", inITNO);
  MILOMA.set("LMBANO", inBANO);
   
  Closure<?> updateMILOMA = { LockedResult record ->
        if(inMFDT!=0)
          record.set("LMMFDT",inMFDT);
        if(inREDA!=0)
          record.set("LMREDA",inREDA);
        if(inEXPI!=0)
          record.set("LMEXPI",inEXPI);
        if(inSEDT!=0)
          record.set("LMSEDT",inSEDT);
        if(inBBDT!=0)
          record.set("LMBBDT",inBBDT);
        if(inMFDT!=0 || inREDA!=0 || inEXPI!=0 || inSEDT!=0 || inBBDT!=0 ){
          record.set("LMLMDT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger());
          record.set("LMCHNO", record.getInt("LMCHNO") + 1);
          record.set("LMCHID", program.getUser());
          record.update();
        }
    }
    
    if (!dbAction.readLock(MILOMA, updateMILOMA)) {
      mi.error("Record does not exist. Could not udpate record");
      return;
    }
  }
  
  /**
  * validate date. 
  * @param inputDate
  * @return boolean
  */
  public boolean checkDateValidity(int inputDate){
    if(inputDate==0){
      return true;
    }
    DateFormat df = new SimpleDateFormat("yyyyMMdd");
    df.setLenient(false);
    try {
        Date date = df.parse(inputDate.toString());
        // If it comes here, then its a valid format
    } catch (ParseException e) {
        // If it comes here, then its not a valid date of this format.
        return false;
    }
    return true;
  }
}