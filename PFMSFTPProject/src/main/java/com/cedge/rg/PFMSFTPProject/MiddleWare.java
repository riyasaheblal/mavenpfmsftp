package com.cedge.rg.PFMSFTPProject;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cedge.rg.PFMSFTPProject.dao.DatabaseMaster;
import com.cedge.rg.PFMSFTPProject.util.GenericUtilities;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;


public class MiddleWare 
{
	
	private static final Logger logger = LogManager.getLogger(MiddleWare.class);

	public void processFunction2(String bankcode,Connection connection)
	{

		logger.info("============Start processFunction2==========================");
		LinkedHashMap<Integer, HashMap> map=fetchcommonData(bankcode,connection);
		System.out.println(map);
		LinkedHashMap<String, String> serverInfo=server_details1(bankcode,connection);
		System.out.println(serverInfo);
		if(serverInfo.size()>0)
		{
			if(map.size()>0)
			{

				download_UploadFile(map, bankcode,serverInfo,connection);

			}
		}
		logger.info("============END processFunction2==========================");

	}



	public void download_UploadFile(LinkedHashMap<Integer, HashMap> map, String bankcode,LinkedHashMap<String,String>serverInfo,Connection conn1)
	{
		int flag=0;
		com.jcraft.jsch.Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;
		Vector filelist = null; 
		int port =Integer.parseInt("22");
		JSch jsch = new JSch();

		try
		{
			for (Integer parameter : map.keySet()) 
			{

				HashMap<String,String> Data =map.get(parameter);
				String value=Data.get("FLOW");
				if(flag==0)
				{
					flag=flag+1;
					session = jsch.getSession(serverInfo.get("username"),serverInfo.get("ip"),port); // connectivity to sftp server
					session.setPassword(serverInfo.get("pfms_password")); 
					Properties config =new Properties();
					config.put("StrictHostKeyChecking","no");
					session.setConfig(config); 
					logger.info("before connecting.....");
					session.connect();
					channel = session.openChannel("sftp"); 
					channel.connect(); 
					logger.info("after connecting.....");
					channelSftp = (ChannelSftp)channel; 
				}
				boolean	isDone=channelSftp.isConnected();
				if(isDone)
				{
//					String cdir="/home/apcob";
					if(value.equals("Download"))
					{
					String path=Data.get("source");
					String home="/pfmsftp/New/"+bankcode;
					System.out.println(home+path);
				//	String home="/sftp/cpsms/Banks/"+bankcode;
						channelSftp.cd(home+path);//source
						filelist = channelSftp.ls(home+path);
						String downloadPattern = bankcode+"*.xml";
						Vector<ChannelSftp.LsEntry> list = channelSftp.ls(downloadPattern);
						for (ChannelSftp.LsEntry oListItem : list) 
						{
							String filemtime=oListItem.getAttrs().getAtimeString();
							SimpleDateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy"); 
//							String Filetime=formatter.format(filemtime);
						    Date date = new Date();  
						    String systime=formatter.format(date);
						    Date d1 = null;
							Date d2 = null;
							try {
							    d1 = formatter.parse(filemtime);
							    d2 = formatter.parse(systime);
							} catch (ParseException e) {
							    e.printStackTrace();
							}    
							long diff = d2.getTime() - d1.getTime();        
							long diffMinutes = diff / (60 * 1000);
							if (diffMinutes>1){
							String destinationFolder1 ="";
								destinationFolder1 =Data.get("destination");
								String destinationFolder="D:/Drive/"+bankcode+destinationFolder1;
								File pathName = new File(destinationFolder);
								System.out.println(pathName.exists());
								if (!pathName.exists()) {
									pathName.mkdirs();}
								System.out.println("destinationFolder is:"+destinationFolder);

							channelSftp.lcd(destinationFolder);
							logger.debug("destinationFolder is:"+destinationFolder);
							if(oListItem.getFilename() != null && oListItem.getFilename().endsWith(".xml") && !oListItem.getFilename().contains("Processed") )
							{
								channelSftp.get(oListItem.getFilename(), oListItem.getFilename());
								logger.debug("downloaded file_name is : "+oListItem.getFilename());
								try{
									channelSftp.rm(oListItem.getFilename());
									DatabaseMaster.FileInformation(bankcode, oListItem.getFilename(),value,conn1);
//									channelSftp.rename(oListItem.getFilename(),Data.get("source_archive")+oListItem.getFilename()+".Processed");
									logger.debug(" download Archive destinationFolder"+Data.get("source_archive")+oListItem.getFilename());
									
								}catch(Exception e)
								{
									try
									{
										
//										channelSftp.rename(oListItem.getFilename(), oListItem.getFilename()+".Processed");
									}catch(Exception e1)
									{
										channelSftp.rm(oListItem.getFilename());
										logger.error("Error while moving file in to Archive for bank code"+bankcode);
									}
								}
							}

							}
						}
					} //donwload code
					else
					{
						System.out.println("File uploading part");
						boolean	isDone1=channelSftp.isConnected();
				//		String home="/sftp/cpsms/Banks/"+bankcode;
						String home="/pfmsftp/New/"+ bankcode;
						String source="D:/Drive/"+bankcode+Data.get("source");
        
						String destinationpath=Data.get("destination");
						String CATAGORY="";
						String destinationArchivePath="";
//						String currentDirectory="/home/apcob";
						File folder = new File(source);
						System.out.println(folder);
//						if(!folder.exists())
//							folder.mkdirs();
						FileFilter fileFilter = f1 -> !f1.isDirectory() && f1.getName().startsWith(bankcode)&&(f1.getName().endsWith(".xml")|| f1.getName().contains(".XML"));
						File[] file = folder.listFiles(fileFilter);
						if(file!=null)
						{	
							for (File f : file)
							{
								
								SimpleDateFormat sdf= new SimpleDateFormat("yy/MM/dd HH:mm:ss");
								 String fileTime =sdf.format(f.lastModified());
								    Date date = new Date();  
								    String systime=sdf.format(date);
								    Date d1 = null;
									Date d2 = null;
									try {
									    d1 = sdf.parse(fileTime);
									    d2 = sdf.parse(systime);
									} catch (ParseException e) {
									    e.printStackTrace();
									}
									long diff = d2.getTime() - d1.getTime();      
									long diffMinutes = diff / (60 * 1000);
									if (diffMinutes>1){
										if(isDone1)
										{
										channelSftp.cd(home+destinationpath);//source
										filelist = channelSftp.ls(home+destinationpath);
										
										String source2=f.toString();
										FileInputStream fis = null;
										fis = new FileInputStream(new File(source2));
										channelSftp.put(fis, f.getName());
										try
										{
											if(fis!=null)
												fis.close();
										}catch(Exception e)
										{
											
										}
											System.out.println("File name is"+f.getName());
										DatabaseMaster.FileInformation(bankcode,f.getName(),value,conn1);
										destinationArchivePath="D:/Drive/"+bankcode+Data.get("destination_archieve");
//										
										File dir = new File(destinationArchivePath);
										logger.info("destination upload file archive path="+destinationArchivePath);
										if(f.length() > 0){
											try
											{
												if (!dir.exists()) 
													dir.mkdirs();
												boolean success = f.renameTo(new File(dir, f.getName()));
												logger.info("File moved to Archive ToCPSMS >> ToCPSMSArchive"+success);
												if(success==false)
												{
													
													try
													{
														logger.info("File is not moove to Archive path:- "+f.getName());
														String shCommand="mv "+f.toString()+" "+dir+"/"+f.getName();
														logger.info("File is moving into achive :- "+shCommand);
														Runtime rt = Runtime.getRuntime();
														Process prcs = null;
														prcs = rt.exec(shCommand);
													}catch(Exception e1)
													{
														logger.error("Exception for MV cammand for file:- "+f.getName());
													}
											}
											}catch(Exception e)
											{
												try
												{
													logger.error("File is not moove to Archive path:- "+f.getName());
													String shCommand="mv "+f.toString()+" "+dir+"/"+f.getName();
													logger.error("File is moving into achive :- "+shCommand);
													Runtime rt = Runtime.getRuntime();
													Process prcs = null; 
													// run the command
													prcs = rt.exec(shCommand);
												}catch(Exception e1)
												{
													logger.error("Exception for MV cammand for file:- "+f.getName());
												}

											}
									}else{
										logger.info("File size is zero");
									}
										}
									
										}
										
									}
								
							}
						 }//upload code
						}
				
				}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			logger.error("Exception occurs while transfering file"+e);
			System.out.println("No file for transfer");

		}finally
		{

			try {
				//System.out.println("Inside finally block of FTP-upload()");
				if(session!=null)
					session.disconnect();
				if(channel!=null)
					channel.disconnect();
				if(channelSftp!=null)
				{
					channelSftp.disconnect();
					channelSftp.exit();
				}
				if(filelist!=null)
					filelist.clear();


			} catch (Exception e) {
				logger.error(GenericUtilities.printException(e));
				logger.info(GenericUtilities.printException(e));
				logger.error("Exception while closing connection in download files for bank code :"+bankcode);
			}

		}
	}
	public LinkedHashMap<String, String> server_details1(String bankcode,Connection connection3)
	{
//		Connection connection3 =  null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet=null;
		LinkedHashMap<String, String>serverInfo=new LinkedHashMap<String, String>();
		try
		{
			 if (connection3==null ||connection3.isClosed())
				{
			connection3=DatabaseMaster.getConnection(bankcode);
				}
			String query="select PFMSFTPIP,PFMSFTPUSER,PFMSFTPPWD from all_banks where bankcode='"+bankcode+"'";

		//	String query="select CPSMSFTPIP,CPSMSFTPUSR,CPSMSFTPPWD from all_banks where cpsmsbankcode='"+bankcode+"'";
			System.out.println(query);
			logger.info("server_details sQL="+query);
			preparedStatement = connection3.prepareStatement(query);
			resultSet = preparedStatement.executeQuery();
			if(resultSet.next())
			{
				serverInfo.put("ip", resultSet.getString("PFMSFTPIP"));
				serverInfo.put("username", resultSet.getString("PFMSFTPUSER"));
				serverInfo.put("pfms_password", resultSet.getString("PFMSFTPPWD"));
			}



		}catch(Exception e)
		{

		}finally
		{
			try
			{
				if(preparedStatement!=null)
					preparedStatement.close();
				if(resultSet!=null)
					resultSet.close();
				
			}catch(Exception e)
			{

			}

		}
		return serverInfo;

	}

	public  LinkedHashMap<Integer, HashMap> fetchcommonData(String bankcode, Connection connection1)
	{
//		Connection connection1=null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet=null;
		LinkedHashMap<Integer, HashMap>map=new LinkedHashMap<Integer, HashMap>();
		try
		{ if (connection1==null ||connection1.isClosed())
		{
			connection1=DatabaseMaster.getConnection(bankcode);
		}
			int count=0;
			String query="select PFMS_SFTP_PATH,CEDGE_SFTP_PATH,RTRIM(TRIM(PFMS_SFTP_PATH),'/')||'Archive/' AS PFMS_SFTP_PATH_ARCH,FLOW from APP_SFTP_PATH1";
			logger.info("fetchcommonData SQL:-"+query);
			preparedStatement = connection1.prepareStatement(query);
			resultSet = preparedStatement.executeQuery();


			LinkedHashMap<String, String>map1=null;
			while(resultSet.next())
			{
				count=count+1;
				map1=new LinkedHashMap<String, String>();
				map1.put("source", resultSet.getString("PFMS_SFTP_PATH"));
//				map1.put("source_archive", resultSet.getString("CEDGE_SFTP_PATH_ARCH"));
				map1.put("destination", resultSet.getString("CEDGE_SFTP_PATH"));
				map1.put("destination_archieve", resultSet.getString("PFMS_SFTP_PATH_ARCH"));
				map1.put("FLOW", resultSet.getString("FLOW"));

				map.put(count, map1);	 
			}

		}catch(Exception e)
		{
			e.printStackTrace();
		}finally
		{
			try
			{
				if(preparedStatement!=null)
					preparedStatement.close();
				if(resultSet!=null)
					resultSet.close();

			}catch(Exception e)
			{

			}
		}
		return map;

	}
}

