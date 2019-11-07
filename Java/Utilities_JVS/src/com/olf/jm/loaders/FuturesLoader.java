package com.olf.jm.loaders;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;


public class FuturesLoader implements IScript { 

	private static final String CONTEXT = "Loader";
	private static final String SUBCONTEXT = "Futures";
	private static ConstRepository repository = null;

	@Override
	public void execute(IContainerContext context) throws OException {

		setUpLog();
			
		Table tblExistingTrans = null;

		PluginLog.info("START FutureLoader");

		try{
		
			repository = new ConstRepository(CONTEXT, SUBCONTEXT);
	
			String strInputFileName;
			String strUploadDir;
			String strProcessingDir;
			String strDoneDir;
			String strArchiveDir;
	
			strInputFileName = repository.getStringValue("filename");
			
			strUploadDir = repository.getStringValue("upload_dir");
			
			strProcessingDir = repository.getStringValue("processing_dir");
			
			strDoneDir = repository.getStringValue("done_dir");
			
			strArchiveDir = repository.getStringValue("archive_dir");
		
			File fileDir = new File(strUploadDir);
			String [] strFilesInDir = fileDir.list();
			
			int intFileCount =0;
		    for ( int i=0; i<strFilesInDir.length; i++ )
		    {
		    	PluginLog.info("file: " + strFilesInDir[i]);
		    	if(strFilesInDir[i].equals(strInputFileName)){
		    		intFileCount++;
		    	}
		    }
		    
		    if(intFileCount == 0){
		    	
		    	PluginLog.info("No input file found. Exiting");
		    	Util.exitSucceed();
		    }
		    
		    if(intFileCount > 1){
		    	
		    	PluginLog.info("More than one input file found. Exiting");
		    	Util.exitSucceed();
		    }
		    
		    
		    if(intFileCount == 1){
		    	
		    	PluginLog.info("Found one input file. ");
		    	
				File fileUpload = new File(strUploadDir + "\\"+ strInputFileName);
				File fileProcessing = new File(strProcessingDir + "\\"+ strInputFileName);
				
				Files.copy(fileUpload.toPath(), fileProcessing.toPath());
				
				String strTimeStamp = Long.toString(System.currentTimeMillis());
				
				File fileArchive = new File(strArchiveDir + "\\"+ strTimeStamp + strInputFileName);
				Files.copy(fileUpload.toPath(), fileArchive.toPath());
				
				fileUpload.delete();
		    	
				// INPUT FROM FILE
				Table tblInput = Table.tableNew();
	
				tblInput.addCol("ticker", COL_TYPE_ENUM.COL_STRING);
				tblInput.addCol("reference", COL_TYPE_ENUM.COL_STRING);
				tblInput.addCol("price", COL_TYPE_ENUM.COL_DOUBLE);
				tblInput.addCol("lots", COL_TYPE_ENUM.COL_DOUBLE);
				tblInput.addCol("internal_bunit", COL_TYPE_ENUM.COL_STRING);
				tblInput.addCol("internal_pfolio", COL_TYPE_ENUM.COL_STRING);
				tblInput.addCol("external_bunit", COL_TYPE_ENUM.COL_STRING);
			
				tblInput.inputFromCSVFile(strProcessingDir + "\\"+ strInputFileName);
				
				tblInput.delRow(1);
				
				tblInput.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
				tblInput.addCol("err_msg", COL_TYPE_ENUM.COL_STRING);
			
				// VALIDATION - remove existing references from input

				Table tblRef = 	tblInput.cloneTable();
	
				String strWhat = "reference";
				String strWhere = "reference NE ''";
				
				tblRef.select(tblInput,strWhat,strWhere);
				
				String strReferences;
				
				strReferences ="";
				for(int i=1;i<=tblRef.getNumRows();i++){
					
					if((tblRef.getNumRows() == 1) || i ==1){
						
						strReferences += "'" + tblRef.getString("reference",i) + "'";
					}
					else {
						
						strReferences += "," + "'" + tblRef.getString("reference",i) + "'";
					}
				}
				
				tblExistingTrans = Table.tableNew();
				
				String strSQL;
				
				strSQL = "SELECT ab.tran_num , ab.reference FROM ab_tran ab WHERE ab.reference in (" + strReferences + " ) and tran_status = 3\n";
				DBaseTable.execISql(tblExistingTrans,strSQL);
			
				if(tblExistingTrans.getNumRows() > 0){
	
					for(int i=1;i<=tblInput.getNumRows();i++){
					
						for(int j=1;j<=tblExistingTrans.getNumRows();j++){
	
							if(tblInput.getString("reference",i).equals(tblExistingTrans.getString("reference",j))){
	
								tblInput.setInt("tran_num",i,tblExistingTrans.getInt("tran_num",j));
							}
						}
					}
				}
			
				/// BOOK TRANSACTION
				for(int i=1;i<=tblInput.getNumRows();i++){
					
					PluginLog.info("Processing " + i + " out of " + tblInput.getNumRows() + " rows.");
					
					if(tblInput.getInt("tran_num", i) == 0 ){
						
						PluginLog.info("Preparing to create tran for ref " + tblInput.getString("reference",i) );
						
						String strTicker = tblInput.getString("ticker",i);
						String strRef = tblInput.getString("reference",i);
						double dblPrice = tblInput.getDouble("price",i);
						double dblLots = tblInput.getDouble("lots",i);
						int intIntBunit = Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, tblInput.getString("internal_bunit",i));
						int intInternalPfolio = Ref.getValue(SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE, tblInput.getString("internal_pfolio",i));
						int intExtBunit = Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, tblInput.getString("external_bunit",i));
						
						int intTranNum=0;
						
						StringBuilder sbErrMsg = new StringBuilder();
						
						intTranNum = bookFuture(strTicker,strRef,dblPrice,dblLots,intIntBunit,intInternalPfolio,intExtBunit, sbErrMsg);
						
						tblInput.setInt("tran_num",i,intTranNum);
						tblInput.setString("err_msg",i, sbErrMsg.toString());
						
					}else{
						
						PluginLog.info("Reference " + tblInput.getString("reference",i) + " has an existing tran " + tblInput.getInt("tran_num",i)  );
					}
					
				}

				tblInput.printTableDumpToFile(strDoneDir + "\\"+ strTimeStamp + "_" +  strInputFileName);

				fileProcessing.delete();
			
		    }	
			
		}catch(Exception e){
		
			PluginLog.info("Exception found " + e.toString());
		}
		finally{
			
			if(Table.isTableValid(tblExistingTrans) == 1){	tblExistingTrans.destroy();}

		}

		PluginLog.info("END FuturesLoader");

	}

	private int bookFuture(String strTicker, String strRef, double dblPrice,double dblLots, int intIntBunit,int intInternalPfolio,int intExtBunit, StringBuilder sbErrMsg)  { 
	
		int intTranNum = 0;
		
		Table tblTicker = null;
		
		try{
			
			int intExistingTranNum;
			
			String strSQL = "select top 1 ab.tran_num from ab_tran ab inner join header h on ab.ins_num = h.ins_num and h.ticker = '" + strTicker + "' and ab.tran_status = 3";
			tblTicker = Table.tableNew();
			DBaseTable.execISql(tblTicker, strSQL);
			
			if(tblTicker.getNumRows() == 1){
				
				intExistingTranNum = tblTicker.getInt("tran_num",1);

				int intRetVal;
				
				Transaction tranPtr;
				
				tranPtr = Transaction.retrieveCopy(intExistingTranNum,1);
				
				tranPtr.setPrice(dblPrice);

				tranPtr.setField(TRANF_FIELD.TRANF_REFERENCE.jvsValue(),0,"",strRef);
				
				tranPtr.setField(TRANF_FIELD.TRANF_POSITION.jvsValue(), 0, "", Double.toString(dblLots));
				
				tranPtr.setInternalBunit(intIntBunit);
				
				tranPtr.setInternalPortfolio(intInternalPfolio);
				
				tranPtr.setExternalBunit(intExtBunit);
				
				intRetVal = tranPtr.insertByStatus(TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED);

				if(intRetVal == 1){
					
					PluginLog.info("Succesfully created tran " + tranPtr.getTranNum());
				}else{

					PluginLog.info("Failed to create created tran " + tranPtr.getTranNum());
				
				}
				
				intTranNum = tranPtr.getTranNum();
				
				tranPtr.destroy();
					
			}
			else{
				
				PluginLog.info("Unable to find existing valid transaction to clone for ticker " + strTicker);
			}
			
			if(Table.isTableValid(tblTicker)==1){tblTicker.destroy();}
			
		}catch(Exception e){
			
			PluginLog.info("exception " + e.toString());
			sbErrMsg.append(e.toString());
		}
		
		return intTranNum;
		
	}


	
		private static void setUpLog() throws OException {
		
    	String logDir   = Util.reportGetDirForToday();
    	String logFile =  "FuturesLoader.log";
    	
		try{
			PluginLog.init("DEBUG", logDir, logFile );	
		}
		catch(Exception e){
			
        	String msg = "Failed to initialise log file: " + Util.reportGetDirForToday() + "\\" + logFile;
        	throw new OException(msg);
		}
	}


}