package com.cedge.rg.PFMSFTPProject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cedge.rg.PFMSFTPProject.util.GenericUtilities;

public class ExecuteScheduler {

	private static final Logger logger = LogManager.getLogger(ExecuteScheduler.class);



public static void main(String[] args) {
	
	Timer timer = new Timer();	
	timer.schedule(new TimerTask() {

		@Override
		public void run() 
		{	
			Connection con = null;
			try { 
				Properties props = GenericUtilities.databaseProp;
				String dataBaseServer1="";
				String userName1="";
				String password1 ="";
				String Driver="";
				
				
				 dataBaseServer1 = props.getProperty("DatabaseServerForAllApp");
				 userName1 = props.getProperty("UsernameForAllApp");
				 password1 = props.getProperty("PasswordForAllApp");
				 Driver=props.getProperty("Driver");
				
				Class.forName(Driver);  
				con = DriverManager.getConnection(dataBaseServer1,userName1,password1); 
				Statement stmt1=con.createStatement(java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE,java.sql.ResultSet.CONCUR_UPDATABLE);
			//	String sql = "select distinct cpsmsbankcode from all_banks where PSFTPFLMVFLG='Y'";
				String sql = "select distinct bankcode from all_banks1 where PSFTPFLMVFLG='Y'";
				ResultSet res1 = stmt1.executeQuery(sql); 
				String bankcode = "";
				while(res1.next()){
					String getbankcode= res1.getString("bankcode");
					bankcode=bankcode.concat(getbankcode);
					bankcode=bankcode.concat(",");
				}
				if(res1!=null)
				res1.close();
				if(stmt1!=null)
				stmt1.close();
				

				int length = bankcode.length();
				String AllBankCode = bankcode.substring(0, length-1);

				String[] acarray=AllBankCode.split(",");
				for (final String bankcode1 : acarray)
				{
					try
					{
							new ExecuteScheduler().runQuick(bankcode1,con);
						
					}catch(Exception e)
					{
						logger.info(e+"for bank "+bankcode1);
					}
				}
				
				//					}
			}  catch (Exception e) {
				e.printStackTrace();
				logger.info(e);
				logger.error(e);
			} 
			finally{
				if (con!=null)
				{
					try {
						con.close();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	},1000,30000);
}
public void runQuick(final String bankCode,Connection con){
	logger.info("Start Time for Bank --"+bankCode);
	long startTime = System.nanoTime();
	logger.info("Start Time --" + new Date());		
	new MiddleWare().processFunction2(bankCode,con);
	
		try {
			if(con!=null)
			con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	long endTime = System.nanoTime();
	Date date1 = new Date();
	logger.info("End Time --" + date1);
	long totalTime = (endTime - startTime)/1000000000;
	logger.info("Total Run time Taken  "+totalTime + " Seconds \n");
	//

}
}

