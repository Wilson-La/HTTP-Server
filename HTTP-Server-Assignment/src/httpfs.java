import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.StringTokenizer;
import java.lang.String;


public class httpfs implements Runnable{
	private static boolean isVerbose = false;
	private static String port = "8080";
	private static String directory = "";
	private Socket connect;
	
	public httpfs(Socket c) {
		connect = c;
	}
	// run method
	public void run() {
		// read the content from client request
		BufferedReader in = null; 
		// write the response to the client
		PrintWriter out = null; 
		// write the byte content of the file to the client
		BufferedOutputStream dataOut = null;
		String input = "";
		String fileRequested = null;
		String method = null;
		String content = null;
		System.out.println("Inside Run");
		int contentLength = 0;
		try {
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			out = new PrintWriter(connect.getOutputStream());
			dataOut = new BufferedOutputStream(connect.getOutputStream());
			input = in.readLine();
			StringTokenizer parse = new StringTokenizer(input);
			//gets the next token which is the method
			method = parse.nextToken().toUpperCase();
			//gets the whole path of the file to read or write
			fileRequested = parse.nextToken().toLowerCase();
			//get the next token which is the content to write in the file from the client in case of post request
			content = parse.nextToken();
			input = "";
			// iterates through the stream
			while ((input = in.readLine()) != null && (input.length() != 0)) {
				//once input is at Content-Length: line then save the next line in variable content and break out of while loop
				if (input.contains("Content-Length:")) {
					contentLength = Integer.parseInt(input.substring(input.indexOf(":")+2));
					System.out.println("content length>>>>>>"+contentLength);
					content = in.readLine();
					System.out.println("content >>"+content);
					break;
				}
				System.out.println("input >>"+input);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if(method.equals("GET"))
			get(out, fileRequested, dataOut);
		else
			post(out, fileRequested, dataOut, content, contentLength);
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//post method
	public void post(PrintWriter out, String fileRequested, BufferedOutputStream dataOut, String clientContent, int contentLength) {
		FileWriter fw = null;
		int fileLength = 0;
		String content = "text/plain";
		System.out.println("Inside Post, the client content is: "+clientContent);
		
		try { 
			// checks to see if the fileRequested String ends with a file name (e.g., file.txt)
			if (fileRequested.endsWith("/")) {
				System.out.println("HTTP ERROR 404");
				out.println("HTTP/1.1 HTTP ERROR 404");
				out.println("Server: my server");
				out.println("Date: " + new Date());
				out.println("Content-type: " + content);
				out.println("Content-length: " + fileLength);
				out.println(); // blank line between headers and content, very important !
				out.flush(); // flush character output stream buffer
				fileRequested = null;
			}
			// gets working directory
			String path = System.getProperty("user.dir");
			fileRequested = fileRequested.replaceAll("/","\\\\");
			System.out.println("Client is trying to access = "+path+fileRequested);
		    File f = new File(path+fileRequested);
			String requestedFolder = "";
			
			// for the event that the client passes a path without a text file at the end of it
		    if (f.isDirectory()) {
		    	
		    	System.out.println("Directory"); 
		    	System.out.println("Requested folder of the file: "+path+fileRequested);
		    	requestedFolder = path+fileRequested;
		    	
		    	//if the client specifies a path without a file print error 404 to their command line
		    	if(requestedFolder.equalsIgnoreCase(path+"\\"+directory)){  
		    		
		    		out.println("HTTP/1.0 HTTP ERROR 404");
					out.println("Server: my server");
					out.println("Date: " + new Date());
					out.println("Content-type: "+content);
					out.println("Content-length: "+fileLength);
					out.println(); // blank line between headers and content, very important !
					out.flush(); // flush character output stream buffer
				 }
		    	//if the client specifies a path without a file and does not have access to directory print error 403 to their command line
				else{           
					out.println("HTTP/1.0 403 Forbidden");
					out.println("User-Agent: Concordia");
					out.println("Server: my server");
					out.println("Date: " + new Date());
					out.println("Content-type: "+content);
					out.println("Content-length: "+fileLength);
					out.println(); // blank line between headers and content, very important !
					out.flush(); // flush character output stream buffer   
				
				}
            }
		    // for the event that the path from client includes a file 
			else {
				System.out.println("It is a File");
				System.out.println("Requested folder of the file: "+path+fileRequested.substring(0,fileRequested.lastIndexOf("\\")));	
				requestedFolder = path+fileRequested.substring(0,fileRequested.lastIndexOf("\\"));
		    			  
				System.out.println("Working Directory = "+path+"\\"+directory);
				//makes sure that the client has access to the directory it wants to get to
				if(requestedFolder.contains((path+"\\"+directory))){
					System.out.println("Access Granted");
					File reqFile = null;
					
					// makes sure the folder we want to access is not equal to null
					if(fileRequested != null){  
						File folder = new File(requestedFolder);
						boolean exists = folder.exists();
						// if folder exists
						if(exists) {
							System.out.println("folder exists");
							reqFile = new File(new File(requestedFolder + "."), fileRequested.substring(fileRequested.lastIndexOf("\\")+1));
							boolean fileExists = reqFile.exists();
							// if the folder exists and the file exists, then overwrite it with the new content from client
							if (fileExists) {
								fw = new FileWriter(reqFile, false);
								System.out.println(clientContent);
								fw.write(clientContent);
								// send HTTP Headers
								out.println("HTTP/1.0 200 OK");
								out.println("Server: Java HTTPFS Server : 1.0");
								out.println("Date: " + new Date());
								out.println("Content-type: " + content);
								out.println("Content-length: " + contentLength);
								out.println("Write done successfully");
								out.println(); // blank line between headers and content, very important !
								out.flush(); // flush character output stream buffer
								
							}
							// if the folder exists, but there is no such file, then create new file
							else {
								reqFile.createNewFile();
								System.out.println("File created is: "+reqFile);
								fw = new FileWriter(reqFile, false);
								
								fw.write(clientContent);
								// send HTTP Headers
								out.println("HTTP/1.0 200 OK");
								out.println("Server: Java HTTPFS Server : 1.0");
								out.println("Date: " + new Date());
								out.println("Content-type: " + content);
								out.println("Content-length: " + contentLength);
								out.println(); // blank line between headers and content, very important !
								out.flush(); // flush character output stream buffer
							}
						}
						//if folder doesn't exist and we need to create a new folder + file
						else{
							
							Path pathOfFolder = Paths.get(requestedFolder);
							Files.createDirectory(pathOfFolder);
							System.out.println("folder created");
							reqFile = new File(new File(requestedFolder + "."), fileRequested.substring(fileRequested.lastIndexOf("\\")+1));
							reqFile.createNewFile();
							System.out.println("File created is: "+reqFile);
							fw = new FileWriter(reqFile, false);
							
							fw.write(clientContent);
							// send HTTP Headers
							out.println("HTTP/1.0 200 OK");
							out.println("Server: Java HTTPFS Server : 1.0");
							out.println("Date: " + new Date());
							out.println("Content-type: " + content);
							out.println("Content-length: " + contentLength);
							out.println(); // blank line between headers and content, very important !
							out.flush(); // flush character output stream buffer
						}
					}	
				}
				// if client does not have access to specific folder, then send an error 403
				else{
					out.println("HTTP/1.0 403 Forbidden");
		
					out.println("User-Agent: Concordia");
					out.println("Server: my server");
					out.println("Date: " + new Date());
					out.println("Content-type: "+content);
					out.println("Content-length: "+fileLength);
					out.println(); // blank line between headers and content, very important !
					out.flush(); // flush character output stream buffer   
						
			  }
			}
		}
		catch(FileNotFoundException fe) {
			out.println("HTTP/1.1 HTTP ERROR 404");
			out.println("Server: my server");
			out.println("Date: " + new Date());
			out.println("Content-type: " + content);
			out.println("Content-length: " + fileLength);
			out.println(); // blank line between headers and content, very important !
			out.flush(); // flush character output stream buffer
		}
		catch(IOException ie) {
			System.out.println("Server Error.");
		}
		catch(Exception e) {
			System.out.println(e.toString());
		}
		finally {
			try {
				out.close();
				dataOut.close();
				if(fw!=null)
				 fw.close();
				connect.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Exception in closing");
				e.printStackTrace();
			}
		}
	}
	// get method
	public void get(PrintWriter out, String fileRequested, BufferedOutputStream dataOut) {
		int fileLength = 0;
		String content = "text/plain";
		
		try { 
			// checks to see if the fileRequested String ends with a file name (e.g., file.txt)
			if (fileRequested.endsWith("/")) {
				System.out.println("HTTP ERROR 404");
				out.println("HTTP/1.1 HTTP ERROR 404");
				out.println("Server: my server");
				out.println("Date: " + new Date());
				out.println("Content-type: " + content);
				out.println("Content-length: " + fileLength);
				out.println(); // blank line between headers and content, very important !
				out.flush(); // flush character output stream buffer
				fileRequested = null;
			}
			// gets working directory
			String path = System.getProperty("user.dir");
			//System.out.println("Working Directory = "+path);
			// converts all forward slashes with back slashes so that we can compare it with the working directory
			fileRequested = fileRequested.replaceAll("/","\\\\");
			System.out.println("Client is trying to access = "+path+fileRequested);
			
		    File f = new File(path+fileRequested);
			String requestedFolder = "";
			
			// if the path requested from the client does NOT include a file at the end
		    if (f.isDirectory()) {
		    	
		    	System.out.println("Directory"); 
		    	System.out.println("Requested folder of the file: "+path+fileRequested);
		    	requestedFolder = path+fileRequested;
		    	
		    	//if client has access to the requested directory and has not entered a specific file, then print a list of files available
		    	if(requestedFolder.equalsIgnoreCase(path+"\\"+directory)){  
		    		File folder = new File(requestedFolder);
		    		File[] listOfFiles = folder.listFiles();
		    		out.println("HTTP/1.0 200 OK");
		    		out.println("Server: my server");
					out.println("Date: " + new Date());
					out.println("Content-type: "+content);
					//out.println("Content-length: "+fileLength);
					out.println(); // blank line between headers and content, very important !
					for (int i = 0; i < listOfFiles.length; i++) {
						if (listOfFiles[i].isFile()) { 
									out.println("File: "+i+" "+listOfFiles[i].getName());
						} 
					}
					out.flush(); // flush character output stream buffer
				 }
		    	//if the client does not have access to the directory, then send an error 403
				 else{           
					 out.println("HTTP/1.0 403 Forbidden");
					 out.println("User-Agent: Concordia");
					 out.println("Server: my server");
					 out.println("Date: " + new Date());
					 out.println("Content-type: "+content);
					 out.println("Content-length: "+fileLength);
					 out.println(); // blank line between headers and content, very important !
					 out.flush(); // flush character output stream buffer   
				
				}
            }
		    // if the requested path from client includes a file at the end
			else {
				System.out.println("It is a File");
				System.out.println("Requested folder of the file: "+path+fileRequested.substring(0,fileRequested.lastIndexOf("\\")));
				// we use last index of \ to get the path to the folder of the file excluding the file name
				requestedFolder = path+fileRequested.substring(0,fileRequested.lastIndexOf("\\"));
				System.out.println("Working Directory = "+path+"\\"+directory);
				
				//checks to see if client has access to the working directory
				if(requestedFolder.contains((path+"\\"+directory))){
					
					File reqFile = null;
					System.out.println("Access Granted");
					System.out.println("Req file is "+reqFile);
					// if file requested is not equal to null
					if(fileRequested != null){  
						// we create file object having the path as requested folder plus the file name which we get from the fileRequested string
						reqFile = new File(new File(requestedFolder + "."), fileRequested.substring(fileRequested.lastIndexOf("\\")+1));
						fileLength = (int) reqFile.length();
						System.out.println("Filelength is:"+fileLength);
						System.out.println("Updated Req file is "+reqFile);
					}
					if(reqFile != null) {
						content = getContentType(fileRequested);
						System.out.println("Inside GET method");
						byte[] fileData = readFileData(reqFile, fileLength);
						System.out.println("After read file data");
						System.out.println("Filedata is: "+fileData);
						// if the file is not present
						if(fileData == null){   
							//byte[] fileData = readFileData(file, fileLength);
		
							out.println("HTTP/1.0 HTTP ERROR 404");
							out.println("Server: my server");
							out.println("Date: " + new Date());
							out.println("Content-type: "+content);
							out.println("Content-length: "+fileLength);
							out.println(); // blank line between headers and content, very important !
							out.flush(); // flush character output stream buffer
						
						}
						// if the file exists and has content, then send the content to the client
						else
						{
							// send HTTP Headers
							out.println("HTTP/1.0 200 OK");
							out.println("Server: Java HTTPFS Server : 1.0");
							out.println("Date: " + new Date());
							out.println("Content-type: " + content);
							out.println("Content-length: " + fileLength);
							out.println(); // blank line between headers and content, very important !
							out.flush(); // flush character output stream buffer
							dataOut.write(fileData, 0, fileLength);
							dataOut.flush();
						
						}
					}
				}
				// client does not have access to directory
				else{
					out.println("HTTP/1.0 403 Forbidden");
		
					out.println("User-Agent: Concordia");
					out.println("Server: my server");
					out.println("Date: " + new Date());
					out.println("Content-type: "+content);
					out.println("Content-length: "+fileLength);
					out.println(); // blank line between headers and content, very important !
					out.flush(); // flush character output stream buffer   
						
			  }
			}

		}
		catch(FileNotFoundException fe) {
			out.println("HTTP/1.1 HTTP ERROR 404");
			out.println("Server: my server");
			out.println("Date: " + new Date());
			out.println("Content-type: " + content);
			out.println("Content-length: " + fileLength);
			out.println(); // blank line between headers and content, very important !
			out.flush(); // flush character output stream buffer
		}
		catch(IOException ie) {
			System.out.println("Server Error.");
		}
		catch(Exception e) {
			System.out.println(e.toString());
		}
		finally {
			try {
				out.close();
				dataOut.close();
				connect.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	// method to read the file data and return it as an array of byte
	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream inputStream = null;
		System.out.println("Inside read file data");
		byte[] fileData = new byte[fileLength];
		try {
			       boolean exists = file.exists();
			       //checking if file exists
				   if(exists)
				   {    System.out.println("File exists");
					    inputStream = new FileInputStream(file);
			            inputStream.read(fileData);
				       }
				   //if it does not exist we send null to the byte array
				   else
					   fileData = null;
		}
		
		catch(Exception e)
		{
			  System.out.println("Exception inside reading file data is"+e);
		}
		finally {
			if(inputStream!=null)
			   inputStream.close();
		}
		return fileData;
	}
	// method to get content type of the file requested
	private String getContentType(String fileRequested) {
		if(fileRequested.endsWith(".htm") || fileRequested.endsWith(".html"))
			return "text/html";
		else 
			return "text/plain";
	}
	
	// method to parse command line args (to get the verbose, port, and directory)
	public static void commandParser(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-v")) {
                isVerbose = true;
            } else if (args[i].equalsIgnoreCase("-p")) {
                port = args[i + 1];
                i++;
            } else if (args[i].equalsIgnoreCase("-d")) {
                directory = (args[i + 1]);
                i++;
            } else if (args[i].equalsIgnoreCase("help")) {
                help();
            }
        }
    }
	
	public static void help() {
		System.out.println("httpfs is a simple file server.\n"
				+ "usage: httpfs [-v] [-p PORT] [-d PATH-TO-DIR]\n"
				+ "-v Prints debugging messages.\n"
				+ "-p Specifies the port number that the server will listen and serve at.\n"
				+ "Default is 8080.\n"
				+ "-d Specifies the directory that the server will use to read/write requested files. Default is the current directory when launching the application.");
	}
	
	public static void main(String[] args) {
		//if there are more command line arguments then 6 then exit the program
		if (args.length >= 6) {
			System.exit(0);
		}
		else {
			commandParser(args);
			try {
				//creating new server socket with the specified port
				ServerSocket serverConnect = new ServerSocket(Integer.parseInt(port));
				System.out.println("Server started. Listening on port "+port);
				//while loop which runs to accept client requests
				while(true) {
					httpfs myServer = new httpfs(serverConnect.accept());
					if(isVerbose) {
						System.out.println("Verbose mode is enabled."+new Date());
					}
					//A new thread is created for each new request that comes to the server
					Thread clientThread = new Thread(myServer);
					//starting thread for request
					clientThread.start();
				}
			}
			catch(Exception e) {
				System.out.println(e.toString());
			}
		}
	}
}
