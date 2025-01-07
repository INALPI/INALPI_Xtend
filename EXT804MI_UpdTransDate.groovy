/**
 * README
 * This extension is used to Update transaction date to creation date in FCAAVP.
 *
 * Name: UpdTransDate
 * Description: Update record in FCAAVP
 * Date	          Changed By			      Description
 * 20241024       Amit Powar            Initial Development - Update transaction date to creation date in FCAAVP.
 * 20241128       Amit Powar            Updated infor review changes.
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

  public UpdTransDate(MIAPI mi, DatabaseAPI database, ProgramAPI program) {
    this.mi = mi;
    this.database = database;
    this.program = program;
  }

  public void main() {
    inCONO = mi.in.get("CONO") == null ? 0 : (Integer)mi.in.get("CONO");
    inFACI = mi.in.get("FACI") == null ? "" : mi.in.get("FACI").toString().trim();
    inGTYP = mi.in.get("GTYP") == null ? "" : mi.in.get("GTYP").toString().trim();
    inRGDT = mi.in.get("RGDT") == null ? 0 : (Integer)mi.in.get("RGDT");
    inSIML = mi.in.get("SIML") == null ? 0 : (Integer)mi.in.get("SIML");
   
   ExpressionFactory expression = database.getExpressionFactory("FCAAVP");
    expression = expression.eq("A7RGDT", String.valueOf(inRGDT)).and(expression.ne("A7TRDT", String.valueOf(inRGDT)));
   DBAction dbAction = database.table("FCAAVP").index("00").matching(expression).selection("A7TRDT", "A7ITNO", "A7RGDT").build();
   DBContainer FCAAVP = dbAction.getContainer();
    FCAAVP.set("A7CONO", inCONO);
    FCAAVP.set("A7FACI", inFACI);
   
   int nrOfRecords = mi.getMaxRecords() <= 0 || mi.getMaxRecords() >= 10000 ? 10000 : mi.getMaxRecords();
    Closure <?> readCallback = { DBContainer readResult ->

      DBAction query = database.table("FCAAVP")
        .index("00")
        .build();
    DBContainer container = query.getContainer();
      container.set("A7CONO", readResult.get("A7CONO"));
      container.setString("A7FACI", readResult.get("A7FACI").toString());
      container.setString("A7ITNO", readResult.get("A7ITNO").toString());
      container.set("A7CANB", readResult.get("A7CANB"));
      container.set("A7RGDT", readResult.get("A7RGDT"));
      container.set("A7RGTM", readResult.get("A7RGTM"));
      container.set("A7TMSX", readResult.get("A7TMSX"));

      Closure <?> updateCallBack = { LockedResult record ->
      if (inSIML == 1) {
          mi.outData.put("ITNO", record.get("A7ITNO").toString());
          mi.outData.put("RGDT", record.get("A7RGDT").toString());
          mi.outData.put("TRDT", record.get("A7TRDT").toString());
          mi.outData.put("URDT", record.get("A7TRDT").toString());
          mi.write();
        }
        else {
         DBAction dbActionInsert = database.table("FCAAVP").index("00").build();
         DBContainer FCAAVPInsert = dbActionInsert.getContainer();
          FCAAVPInsert.set('A7CONO', record.get('A7CONO'));
          FCAAVPInsert.set('A7DIVI', record.get('A7DIVI'));
          FCAAVPInsert.set('A7FACI', record.get('A7FACI'));
          FCAAVPInsert.set('A7ITNO', record.get('A7ITNO'));
          FCAAVPInsert.set('A7RGDT', record.get('A7TRDT'));
          FCAAVPInsert.set('A7RGTM', record.get('A7RGTM'));
          FCAAVPInsert.set('A7TMSX', record.get('A7TMSX'));
          FCAAVPInsert.set('A7TRDT', record.get('A7TRDT'));
          FCAAVPInsert.set('A7WHLO', record.get('A7WHLO'));
          FCAAVPInsert.set('A7APPR', record.get('A7APPR'));
          FCAAVPInsert.set('A7APPO', record.get('A7APPO'));
          FCAAVPInsert.set('A7TONQ', record.get('A7TONQ'));
          FCAAVPInsert.set('A7TOOQ', record.get('A7TOOQ'));
          FCAAVPInsert.set('A7TRQT', record.get('A7TRQT'));
          FCAAVPInsert.set('A7MFCO', record.get('A7MFCO'));
          FCAAVPInsert.set('A7ORAC', record.get('A7ORAC'));
          FCAAVPInsert.set('A7GTYP', record.get('A7GTYP'));
          FCAAVPInsert.set('A7OCAT', record.get('A7OCAT'));
          FCAAVPInsert.set('A7RIDN', record.get('A7RIDN'));
          FCAAVPInsert.set('A7RIDL', record.get('A7RIDL'));
          FCAAVPInsert.set('A7SINO', record.get('A7SINO'));
          FCAAVPInsert.set('A7INYR', record.get('A7INYR'));
          FCAAVPInsert.set('A7SUNO', record.get('A7SUNO'));
          FCAAVPInsert.set('A7RESP', record.get('A7RESP'));
          FCAAVPInsert.set('A7REPN', record.get('A7REPN'));
          FCAAVPInsert.set('A7RELP', record.get('A7RELP'));
          FCAAVPInsert.set('A7INLP', record.get('A7INLP'));
          FCAAVPInsert.set('A7RIDX', record.get('A7RIDX'));
          FCAAVPInsert.set('A7LMTS', record.get('A7LMTS'));
          FCAAVPInsert.set('A7APPE', record.get('A7APPE'));
          FCAAVPInsert.set('A7APPL', record.get('A7APPL'));
          FCAAVPInsert.set('A7CANB', record.get('A7CANB'));
         
          record.delete();
          Closure <?> insertFCAAVP= { DBContainer containerFCAAVP ->
  
        }
          if (!dbActionInsert.insert(FCAAVPInsert, insertFCAAVP)) {
            mi.error("Record already exists");
            return;
          }
        }
      }

      query.readLock(container, updateCallBack);


    }
    if (!dbAction.readAll(FCAAVP, 2, nrOfRecords, readCallback)) {
      mi.error("Record was not updated");
      return;
    }

  }
}