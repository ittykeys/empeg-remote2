package com.chasinglemons.empeg2;


public class Empeg {
	private String empegName;
	private String empegIP;

    public Empeg(String empegName, String empegIP) {
    	this.empegIP = empegIP;
    	this.empegName = empegName;
    }
    
    public void setempegIP(String empegIP) {
        this.empegIP = empegIP;
    }
    public String getempegIP() {
        return empegIP;
    }
    public void setempegName(String empegName) {
        this.empegName = empegName;
    }
    public String getempegName() {
        return empegName;
    }
}