package com.hortonworks.digitalemil.hdpappstudio.web;

public class Location {
	public String latitude, longitude;
	public double n;
	private boolean pivot= false;
	
	public String toString() {
		return "{ ;latitude;:"+latitude+", ;longitude;:"+longitude+", ;n;:"+n+"}";
	}
	
	/*
	public Location() {
		super();
		pivot= false;
	}
	*/

	public Location(boolean p) {
		super();
		pivot= p;
	}

	@Override
	public int hashCode() {
		if(pivot)
			return super.hashCode();
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((latitude == null) ? 0 : latitude.hashCode());
		result = prime * result
				+ ((longitude == null) ? 0 : longitude.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(pivot)
			return super.equals(obj);
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Location other = (Location) obj;
		if (latitude == null) {
			if (other.latitude != null)
				return false;
		} else if (!latitude.equals(other.latitude))
			return false;
		if (longitude == null) {
			if (other.longitude != null)
				return false;
		} else if (!longitude.equals(other.longitude))
			return false;
		return true;
	}
}
