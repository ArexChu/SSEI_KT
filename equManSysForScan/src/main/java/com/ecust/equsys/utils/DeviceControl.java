package com.ecust.equsys.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DeviceControl
{
	private BufferedWriter CtrlFile;
	
	DeviceControl(String path) throws IOException	
	{
		File DeviceName = new File(path);
		CtrlFile = new BufferedWriter(new FileWriter(DeviceName, false));	//open file
	}
	
	public void PowerOnDevice() throws IOException		//poweron as3992 device
	{
		CtrlFile.write("-wdout94 1");
		CtrlFile.flush();
	}
	
	public void PowerOffDevice() throws IOException	//poweroff as3992 device
	{
  		CtrlFile.write("-wdout94 0");
  		CtrlFile.flush();
	}
	
	public void DeviceClose() throws IOException		//close file
	{
		CtrlFile.close();
	}
}