/**
 * README
 * This extension is used to Update transaction date to creation date in FCAAVP.
 *
 * Name: UpdTransDate
 * Description: Update record in FCAAVP
 * Date	          Changed By			      Description
 * 20241024       Amit Powar            Initial Development - Update transaction date to creation date in FCAAVP.
 * 
 */
 
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class UpdTransDate extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final ProgramAPI program;
  
  private int inCONO;
  private String inFACI;
  private String inGTYP;
  private int inRGDT;
  private int inSIML;
  private int noOfRecords;
  
  public UpdTransDate(MIAPI mi, DatabaseAPI database, ProgramAPI program) {
    this.mi = mi;
    this.database=database;
    this.program=program;
  }
  
  public void main() {
   inCONO =  mi.in.get("CONO") == null ? 0 : (Integer)mi.in.get("CONO");
   inFACI =  mi.in.get("FACI") == null ? "" : mi.in.get("FACI").toString().trim();
   inGTYP =  mi.in.get("GTYP") == null ? "" : mi.in.get("GTYP").toString().trim();
   inRGDT =  mi.in.get("RGDT") == null ? 0 : (Integer)mi.in.get("RGDT");
   inSIML =  mi.in.get("SIML") == null ? 0 : (Integer)mi.in.get("SIML");
   noOfRecords=0;
   
   ExpressionFactory expression = database.getExpressionFactory("FCAAVP");
   expression = expression.eq("A7RGDT", String.valueOf(inRGDT)).and(expression.ne("A7TRDT", String.valueOf(inRGDT)));
   DBAction dbAction = database.table("FCAAVP").index("00").matching(expression).selection("A7TRDT","A7ITNO","A7RGDT").build();
   DBContainer FCAAVP = dbAction.getContainer();
   FCAAVP.set("A7CONO", inCONO);
   FCAAVP.set("A7FACI", inFACI);
   
   Closure<?> updateFCAAVP = { LockedResult record ->
    
      if(inSIML==1){
       mi.outData.put("ITNO", record.get("A7ITNO").toString());
       mi.outData.put("RGDT", record.get("A7RGDT").toString());
       mi.outData.put("TRDT", record.get("A7TRDT").toString());
       mi.outData.put("URDT", record.get("A7TRDT").toString());
       mi.write();
      }
      else{
         DBAction dbAction_insert = database.table("FCAAVP").index("00").build();
         DBContainer FCAAVP_insert = dbAction_insert.getContainer();
         FCAAVP_insert.set('A7CONO', record.get('A7CONO'));
         FCAAVP_insert.set('A7DIVI', record.get('A7DIVI'));
         FCAAVP_insert.set('A7FACI', record.get('A7FACI'));
         FCAAVP_insert.set('A7ITNO', record.get('A7ITNO'));
         FCAAVP_insert.set('A7RGDT', record.get('A7TRDT'));
         FCAAVP_insert.set('A7RGTM', record.get('A7RGTM'));
         FCAAVP_insert.set('A7TMSX', record.get('A7TMSX'));
         FCAAVP_insert.set('A7TRDT', record.get('A7TRDT'));
         FCAAVP_insert.set('A7WHLO', record.get('A7WHLO'));
         FCAAVP_insert.set('A7APPR', record.get('A7APPR'));
         FCAAVP_insert.set('A7APPO', record.get('A7APPO'));
         FCAAVP_insert.set('A7TONQ', record.get('A7TONQ'));
         FCAAVP_insert.set('A7TOOQ', record.get('A7TOOQ'));
         FCAAVP_insert.set('A7TRQT', record.get('A7TRQT'));
         FCAAVP_insert.set('A7MFCO', record.get('A7MFCO'));
         FCAAVP_insert.set('A7ORAC', record.get('A7ORAC'));
         FCAAVP_insert.set('A7GTYP', record.get('A7GTYP'));
         FCAAVP_insert.set('A7OCAT', record.get('A7OCAT'));
         FCAAVP_insert.set('A7RIDN', record.get('A7RIDN'));
         FCAAVP_insert.set('A7RIDL', record.get('A7RIDL'));
         FCAAVP_insert.set('A7SINO', record.get('A7SINO'));
         FCAAVP_insert.set('A7INYR', record.get('A7INYR'));
         FCAAVP_insert.set('A7SUNO', record.get('A7SUNO'));
         FCAAVP_insert.set('A7RESP', record.get('A7RESP'));
         FCAAVP_insert.set('A7REPN', record.get('A7REPN'));
         FCAAVP_insert.set('A7RELP', record.get('A7RELP'));
         FCAAVP_insert.set('A7INLP', record.get('A7INLP'));
         FCAAVP_insert.set('A7RIDX', record.get('A7RIDX'));
         FCAAVP_insert.set('A7LMTS', record.get('A7LMTS'));
         FCAAVP_insert.set('A7APPE', record.get('A7APPE'));
         FCAAVP_insert.set('A7APPL', record.get('A7APPL'));
         FCAAVP_insert.set('A7CANB', record.get('A7CANB'));
    
         record.delete();
         Closure<?> insertFCAAVP= { DBContainer container ->
        }
        if(!dbAction.insert(FCAAVP_insert,insertFCAAVP)){
          mi.error("Record already exists");
          return;
        }
      }
    }
    
    if (!dbAction.readAllLock(FCAAVP,2, updateFCAAVP)) {
      mi.error("Record was not updated");
      return;
    }
  }
}