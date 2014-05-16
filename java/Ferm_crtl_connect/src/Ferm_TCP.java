package ferm_ctrl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JOptionPane;

import ferm_ctrl.ConnectionState;

public class Ferm_TCP implements Runnable{

	static double values[] = new double[9];
	static long currentTime=0;
	static long beginTime;
	static long timeOutInt = 2000;
	static boolean keepGoing = true;
	public FermVessel FV1;
	public FermVessel FV2;
	public FermVessel FVC;
	private double tcAMB;

//Ip Address and Port on which the Arduino Server is operating
	private String serverIP="68.47.140.19";
	private static final int serverPort=8888;
	public enum valueNames{sp1,sp2,spC,tc1,fv1Bath,tc2,fv2Bath,tcC,tcAMB};
	public Socket clientSocket;
	private volatile ConnectionState state;

public Ferm_TCP(){
	this.state=ConnectionState.STOPPED;
	FV1 = new FermVessel(1,75,0.2);
	FV2 = new FermVessel(2,75,0.2);
	FVC = new FermVessel(0,38,2.0);
}
public Ferm_TCP(FermVessel FV1, FermVessel FV2, FermVessel FVC){
	this.FV1=FV1;
	this.FV2=FV1;
	this.FVC=FV1;
	this.state=ConnectionState.STOPPED;
//	run();
}
	public double gettcAMB(){
		return tcAMB;
	}

	public void setIP(String ip){
		serverIP=ip;
	}
	
	public FermVessel getFV1(){
		return this.FV1;
	}
	
	public FermVessel getFV2(){
		return this.FV2;
	}
	
	public FermVessel getFVC(){
		return this.FVC;
	}
	
	public void getValues(){
    	//this method will send the get command to the server and receive those values as a string
		//each get is started as a thread	
		if (state==ConnectionState.CLOSED){
			connect();
		}
		if (state==ConnectionState.CONNECTED){
			Thread t=new Thread(new Runnable(){
    			public void run(){ 	
    	try{
    	state=ConnectionState.GETTING;
    	String getmsg = "GET";
    	DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
    	BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    	outToServer.writeBytes(getmsg+'\n');
    	beginTime=System.currentTimeMillis();
    	currentTime=beginTime;
    	String getReply="";
    	System.out.println("wrote get statement");
    	while(currentTime-beginTime<timeOutInt&&getReply==""){
    	getReply = inFromServer.readLine();
    	currentTime=System.currentTimeMillis();
    	}
    	if(getReply!=""){
    	System.out.println(getReply);
    	if (getReply!=null){
    		analyzeMsg(getReply);
    		try {
				dbLogFile();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
    	}
        outToServer.close();
        inFromServer.close();
        clientSocket.close();
    	}else if (getReply==""||getReply==null){
    		clientSocket.close();
    		state=ConnectionState.CLOSED;
    		return;
    	}
//        clientSocket.close();
        state=ConnectionState.CLOSED;
    }catch(SocketException se){
    	if (se.getMessage()=="Connection Reset"){
    		try{
    			JOptionPane.showMessageDialog(null, se.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    			
    			connect();
    		
    		}catch (Exception ee){
//    			Frame jF=ferm_crtl_GUI.getFrames()[0];
    			JOptionPane.showMessageDialog(null, ee.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    			System.err.println(ee);
    		}
    	}try {
			stopThis();

		} catch (Exception e) {

			e.printStackTrace();
		}
    }catch (IOException ioe){
    	JOptionPane.showMessageDialog(null, ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    	System.err.println(ioe);
    }
    			}		
    		});
			t.start();
		}
    }
	
    public void stopThis()throws Exception{
 //   	keepGoing = false;
    	if(clientSocket!=null){
    	clientSocket.close();
    	}
    	this.state=ConnectionState.STOPPED;
    }
    public void connect(){
    	if (this.state==ConnectionState.CLOSED||this.state==ConnectionState.STOPPED){
    		Thread t=new Thread(new Runnable(){
    			public void run(){ 		
    	try{
    		state=ConnectionState.CONNECTING;
    	clientSocket = new Socket(serverIP, serverPort);//making the socket connection
    	if (clientSocket.isConnected()){
    		state=ConnectionState.CONNECTED;
    	}
        System.out.println("Connected to:"+serverIP+" on port:"+serverPort);
    	}catch (UnknownHostException e){
    		JOptionPane.showMessageDialog(null, "Unknown Host "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    		System.out.println(e.toString());
    		state=ConnectionState.STOPPED;
    	}catch (IOException ioe){
    		JOptionPane.showMessageDialog(null, ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    		System.out.println(ioe.toString());
    		state=ConnectionState.STOPPED;
    	}
    		}
    	});
    	t.start();
    	}
    }
 /*   private void logFile(String entry){
    	//write data entry to log file
    		Path pa = Paths.get(System.getProperty("user.home"),"ferm_ctrl.txt");
    		File f = new File(pa.toString());   		
    	try{
    		if (!f.isFile()){
    			f.createNewFile();
    		}
    		Date date= new Date();
    		Format formatter = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
    		BufferedWriter writer = new BufferedWriter(new FileWriter(f,true));
    		writer.write(formatter.format(date)+" "+":"+" "+entry);
    		writer.newLine();
    		writer.close();
    	}catch(IOException ioe){
    		System.err.println(ioe);	
    	}
    } */
    
    private void dbLogFile() throws ClassNotFoundException{
    	Class.forName("org.sqlite.JDBC");
    	Connection con = null;
    	Path dbPa = Paths.get(System.getProperty("user.home"),"ferm_ctrl.db");
    	if (!new File(dbPa.toString()).isFile()){
    		//Check to see if db file exists
    		//if it doesn't exist, create it
    		File db = new File(dbPa.toString());
    	}
    	try{
    		Date date= new Date();
    		Format formatter = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
    		 con = DriverManager.getConnection("jdbc:sqlite:"+dbPa.toString());
    	      Statement statement = con.createStatement();
    	      statement.setQueryTimeout(30);  // set timeout to 30 sec.
    	      //TODO loop that builds this statement 
    	      statement.executeUpdate("create table if not exists log (Id INTEGER PRIMARY KEY autoincrement, time text, sp1 real, sp2 real, spC real, tc1 real, fv1Bath real, tc2 real, fv2Bath real, tcC real, tcAMB real)");
//    	      statement.executeUpdate("insert into log values(null, '"+System.currentTimeMillis()+"', '"+FV1.getSP()+"', '"+FV2.getSP()+"', '"+FVC.getSP()+"', '"+FV1.getTemp()+"', '"+FV2.getTemp()+"', '"+FVC.getTemp()+"')");   		
    	    //TODO loop that builds this statement 
    	      statement.executeUpdate("insert into log values(null, '"+formatter.format(date)+"', '"+FV1.getSP()+"', '"+FV2.getSP()+"', '"+FVC.getSP()+"', '"+FV1.getTemp()+"', '"+FV1.getBathTemp()+"', '"+FV2.getTemp()+"', '"+FV2.getBathTemp()+"', '"+FVC.getTemp()+"','"+tcAMB+"')");   		
    	}catch (SQLException sqle){
    		System.err.println(sqle.getMessage());
    	}finally{
    		try{
    			if(con != null)
    		          con.close();
    		      }
    		      catch(SQLException e)
    		      {
    		        // connection close failed.
    		        System.err.println(e);
    		      }
    		}
    	}
    	
    
    public void setValues(final double sp1, final double sp2, final double spC){
    	//this method sends values to the controller
    	if (state==ConnectionState.STOPPED){
    		JOptionPane.showMessageDialog(null, new String("Must be Connected to set values!"), "Error", JOptionPane.ERROR_MESSAGE);
    		return;
    	}
    	if (state==ConnectionState.CLOSED){
			connect();
		}
    	while(state!=ConnectionState.CONNECTED){}
    	//This should allow the method to wait while the machine connects
    	if (this.state==ConnectionState.CONNECTED){
    		Thread t=new Thread(new Runnable(){
    			public void run(){
    	try{
    		state=ConnectionState.SETTING;
    	String msgToServer = "SET&"+Double.toString(sp1)+"&"+Double.toString(sp2)+"&"+Double.toString(spC)+"&/";
    	DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
//    	BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    	//TODO hmm, maybe send a get after sending the send to check the values and assure a successful set
    	//System.out.println("Attempting Set");
    	outToServer.writeBytes(msgToServer+'\n');
    	outToServer.flush();
    	FV1.setSP(sp1);
    	FV2.setSP(sp2);
    	FVC.setSP(spC);
    	outToServer.close();
    	clientSocket.close();
    	state=ConnectionState.CLOSED;
    	}catch(IOException ioe){
    		JOptionPane.showMessageDialog(null, ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
//    		clientSocket.close();
    		state=ConnectionState.CLOSED;
    	}
    			}
    			});
    		t.start();
    		}
    }
    
    public void analyzeMsg(String msg){
	 //takes apart the received message, as a string, and extracts the values
	 //AS OF THIS VERSION: the order is sp1*&sp2&spC&tc1&tc2&tcC, where * is present depending on relay status
    //Also, follow the valueNames. And the tests for this (look for the stub!)
	 boolean fv1Relay=FV1.isChilling();
	 boolean fv2Relay=FV2.isChilling();
	 boolean fvCRelay=FVC.isChilling();
	 int begin=0;
	 int index=0;
	 int msgLength =msg.length();
	 if (msgLength==0){
		 return;
	 }
	 int i=0;
	 begin=msg.indexOf("&&")+1;
	 while(index<msgLength && i<9){
		 int end = msg.indexOf("&", begin+1);
		 if (end == -1){
			 //no more &, so string is most likely ending
			 index=msg.length();
		 }else {
		 String value = msg.substring(begin+1, end);
		 System.out.println(value);		 
			 if (value.charAt(value.length()-1)=='*'){
				 if (i==3){
					 fv1Relay=(value.charAt(value.length()-1)=='*');
					 
					value=value.replace('*', ' ');
					value= value.trim();
				 }
				 if (i==5){
					 fv2Relay=(value.charAt(value.length()-1)=='*');
					 value=value.replace('*', ' ');
					 value=value.trim();
				 }
				 if (i==7){
					 fvCRelay=(value.charAt(value.length()-1)=='*');
					 value=value.replace('*', ' ');
					 value=value.trim();
				 }
			 }else {
				 if (i==3){
					 fv1Relay=(value.charAt(value.length()-1)=='*');
				 }
				 if (i==5){
					 fv2Relay=(value.charAt(value.length()-1)=='*');
					
				 }
				 if (i==7){
					 fvCRelay=(value.charAt(value.length()-1)=='*');
					
				 }
			 }
			 //ok, successfully checked for *, set value of relay boolean
		FV1.setChilling(fv1Relay);
		FV2.setChilling(fv2Relay);
		FVC.setChilling(fvCRelay);
		 values[i] =Double.parseDouble(value);
		 begin=end;
		 i++;
		 }
	 }
	 
	 for(valueNames vN : valueNames.values()){

		 if (vN.ordinal()==0){
			 //Sp1
			 FV1.setSP(values[0]);
		 }else if (vN.ordinal()==1){
			 //Sp2
			 FV2.setSP(values[1]);
		 }else  if (vN.ordinal()==2){
			 //Sp3
			 FVC.setSP(values[2]);
		 }else if (vN.ordinal()==3){
			 //tc1
			 FV1.setTemp(values[3]);
		 }else if (vN.ordinal()==4){
			 //tcbath1
			 FV1.setBathTemp(values[4]);
		 }else if (vN.ordinal()==5){
			 //tc2
			 FV2.setTemp(values[5]);
		 }else if (vN.ordinal()==6){
			 //tcbath2
			 FV2.setBathTemp(values[6]);
		 }else if (vN.ordinal()==7){
			 //tc3
			 FVC.setTemp(values[7]);
		 }else if (vN.ordinal()==8){
			 //tcAMB
			 tcAMB=values[8];
		 }		 
	 }	 
 }
 
	public ConnectionState getState(){
		return this.state;
	}
 
 public void run(){

 }
	
}
