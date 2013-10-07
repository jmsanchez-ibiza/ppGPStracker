package com.jmsg.android.ppgpstracker;

public class DbPointsManager {
	
	// Variables privadas de la clase
    int _id;
    String _gmttimestamp;
    Double _latitude, _longitude, _altitude, _accuracy, _speed, _bearing;
    boolean _puntoenviado;
     
    // Constructor vacío
    public DbPointsManager(){
         
    }
    // constructor Completo
    public DbPointsManager(int id,
    		String gmttimestamp, 
    		double latitude,
    		double longitude,
    		double altitude,
    		double accuracy,
    		double speed,
    		double bearing,
    		boolean puntoenviado){
    	
        this._id = id;
        this._gmttimestamp = gmttimestamp;
        this._latitude = latitude;
        this._longitude = longitude;
        this._altitude = altitude;
        this._accuracy = accuracy;
        this._speed = speed;
        this._bearing = bearing;
        this._puntoenviado = puntoenviado;
        
    }
     
    // constructor Sin ID
    public DbPointsManager(String gmttimestamp, 
    		double latitude,
    		double longitude,
    		double altitude,
    		double accuracy,
    		double speed,
    		double bearing,
    		boolean puntoenviado){
    	
    	this._gmttimestamp = gmttimestamp;
        this._latitude = latitude;
        this._longitude = longitude;
        this._altitude = altitude;
        this._accuracy = accuracy;
        this._speed = speed;
        this._bearing = bearing;
        this._puntoenviado = puntoenviado;
    }
    // Devolver el ID
    public int getID(){
        return this._id;
    }
     
    // Establecer el ID
    public void setID(int id){
        this._id = id;
    }
    
    // Pasar toda la información del punto a un string
    public String toString() {
    	String ret;
    	
    	ret = String.valueOf(this.getID());
    	ret = ret + ", " + this.getGmttimestamp();
    	ret = ret + ", " + 
    			String.format("Lat:%1.6f, Lon:%1.6f, Alt:%1.0f, Acc:%1.0f, Vel:%1.0f",
    					this.getLatitude(),
    					this.getLongitude(),
    					this.getAltitude(),
    					this.getAccuracy(),
    					this.getSpeed()
    					);	
    	return ret;
    }
    
    // Getters y Setters de todos los demás campos
     
    public String getGmttimestamp() {
		return _gmttimestamp;
	}
	public void setGmttimestamp(String gmttimestamp) {
		this._gmttimestamp = gmttimestamp;
	}
	public Double getLatitude() {
		return _latitude;
	}
	public void setLatitude(Double latitude) {
		this._latitude = latitude;
	}
	public Double getLongitude() {
		return _longitude;
	}
	public void setLongitude(Double longitude) {
		this._longitude = longitude;
	}
	public Double getAltitude() {
		return _altitude;
	}
	public void setAltitude(Double altitude) {
		this._altitude = altitude;
	}
	public Double getAccuracy() {
		return _accuracy;
	}
	public void setAccuracy(Double accuracy) {
		this._accuracy = accuracy;
	}
	public Double getSpeed() {
		return _speed;
	}
	public void setSpeed(Double speed) {
		this._speed = speed;
	}
	public Double getBearing() {
		return _bearing;
	}
	public void setBearing(Double bearing) {
		this._bearing = bearing;
	}
	public boolean getPuntoEnviado() {
		return _puntoenviado;
	}
	public void setPuntoEnviado(boolean puntoenviado) {
		this._puntoenviado = puntoenviado;
	}

}
