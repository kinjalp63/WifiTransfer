package com.example.adapter;

public class WiFiPeer {

	String ssId, bssId, hostIpAddress;
	boolean status;

	/**
	 * @return the status
	 */
	public boolean isStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(boolean status) {
		this.status = status;
	}

	/**
	 * @return the ssId
	 */
	public String getSsId() {
		return ssId;
	}

	/**
	 * @return the bssId
	 */
	public String getBssId() {
		return bssId;
	}

	/**
	 * @return the hostIpAddress
	 */
	public String getHostIpAddress() {
		return hostIpAddress;
	}

	/**
	 * @param ssId the ssId to set
	 */
	public void setSsId(String ssId) {
		this.ssId = ssId;
	}

	/**
	 * @param bssId the bssId to set
	 */
	public void setBssId(String bssId) {
		this.bssId = bssId;
	}

	/**
	 * @param hostIpAddress the hostIpAddress to set
	 */
	public void setHostIpAddress(String hostIpAddress) {
		this.hostIpAddress = hostIpAddress;
	}
}
