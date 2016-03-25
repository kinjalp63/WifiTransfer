package com.example;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import com.example.WiFiDemo.ImageHandler;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

public class SocketConnection {

	private Context context;
	private ServerSocket ss;
	private boolean end = false;
	private ImageHandler mHandler;
	
	public SocketConnection( Context cxt, ImageHandler mHandler ) {
		this.context = cxt;
		this.mHandler = mHandler;
	}
	
	public void initServerSocket() {
		try {
	        ss = new ServerSocket();
	        ss.setReuseAddress(true);
	        ss.setSoTimeout(600000);
	        ss.bind(new InetSocketAddress(12345));
	        while(!end){
	                //Server is waiting for client here, if needed
					System.out.println("1");
	                
					Socket s = ss.accept();
	    			System.out.println("4");
	    			
					ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
					Object obj = ois.readObject();
					if( obj instanceof byte[] ) {
						byte[] b = (byte[]) obj;
						sendEventToGUI( obj );
					}
					else {
			            String message = (String) obj;
			            if( message.startsWith("call") )
			            	callNumber(message.split("_")[1]);
			            else
			            	endCall();
			            System.out.println("Message Received: " + message);
					}

		            ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
		            oos.writeObject("Hi Client Message Received");

		            ois.close();
		            oos.close();
		            
	                s.close();
	        }
	        
			ss.close();
			
			} catch (Exception e) {
			        e.printStackTrace();
			}
	}
	
	public void initClientSocket( String ipAddress, Object message ) {
		try {
			Socket socket = new Socket(ipAddress, 12345);
            //write to socket using ObjectOutputStream
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            System.out.println("Sending request to Socket Server");
            
            if( message instanceof byte[] ) {
            	oos.writeObject( message );
            }
            else if( TextUtils.isEmpty( message.toString() ))
            	oos.writeObject("Hello World !");
            else
            	oos.writeObject(message);
            
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            String message1 = (String) ois.readObject();
            System.out.println("Message: " + message1);
            
            ois.close();
            oos.close();
	        

		} catch (Exception e) {
		        e.printStackTrace();
		}
	}
	
	private void callNumber( String number ) {
		 Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
		 context.startActivity( intent );
	}

	private void endCall() {
		TelephonyManager tm = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		Class c;
		try {
			c = Class.forName(tm.getClass().getName());
		
			Method m = c.getDeclaredMethod("getITelephony");
			m.setAccessible(true);
			Object telephonyService = m.invoke(tm);
			c = Class.forName(telephonyService.getClass().getName());
			m = c.getDeclaredMethod("endCall");
			m.setAccessible(true);
			m.invoke(telephonyService);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void sendEventToGUI( Object object ) {
		Message msg =  new Message();
		msg.obj = object;
		this.mHandler.sendMessage( msg );
	}
	
}
