package ferm_ctrl;

public class FermVessel {
	private double sp;
	private double temp;
	private double bathTemp;
	private double diff;
	private int number;
	private boolean relay;
	//other defing vars go here, reserved for future use
	//size, type could help with chilling logic

	
	
	public FermVessel(int number, double sp, double diff){
		this.number=number;
		this.sp=sp;
		this.diff=diff;
		this.temp=0;
		this.bathTemp=0;
		this.relay=false;
	}
	
	public synchronized double getSP(){
		return this.sp;	
	}
	public synchronized double getTemp(){
		return this.temp;
	}
	public synchronized int getNumber(){
		return this.number;
	}
	public synchronized double getBathTemp(){
		return this.bathTemp;
	}
	public synchronized double getDiff(){
		return this.diff;
	}
	
	public synchronized boolean isChilling(){
		return this.relay;
	}
	
	public synchronized void setSP(double sp){
		this.sp=sp;
	}
	
	public synchronized void setDiff(double diff){
		this.diff=diff;
	}
	
	public synchronized void setTemp(double temp){
		this.temp=temp;
	}
	public synchronized void setBathTemp(double bathTemp){
		this.bathTemp=bathTemp;
	}
	public synchronized void setNumber(int number){
		this.number=number;
	}
	
	public synchronized void setChilling(boolean relay){
		this.relay=relay;
	}
}
