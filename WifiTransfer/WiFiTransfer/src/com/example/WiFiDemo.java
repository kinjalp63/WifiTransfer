package com.example;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.example.adapter.WiFiPeer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class WiFiDemo extends Activity implements OnClickListener {
	private static final String TAG = "WiFiDemo";
	public WifiManager wifi;
	private BroadcastReceiver receiver;

	private TextView textStatus;
	private EditText edtNumber;
	private Button buttonScan, btnSendCall, btnEndCall, btnGallery;
	private ListView lstPeer;
	private List<WiFiPeer> peerList = new ArrayList<WiFiPeer>();
	WifiListAdapter lstAdapter;
	private List<ScanResult> scanResultList;
	private SocketConnection socketConection;
	private int GALLERY_ACTIVITY_REQUEST_CODE = 2;
	private Thread thread_ = null;
	
	private ProgressDialog mDialog;
	String server;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// Setup UI
		lstPeer = (ListView) findViewById(R.id.lstPeer);
		edtNumber = (EditText) findViewById(R.id.edtNumber);
		btnSendCall = (Button) findViewById(R.id.btnSendCall);
		btnGallery = (Button) findViewById(R.id.btnGallery);
		textStatus = (TextView) findViewById(R.id.textStatus);
		buttonScan = (Button) findViewById(R.id.buttonScan);
		btnEndCall = (Button) findViewById(R.id.btnEndCall);
		
		edtNumber.setFocusable( true );
		
		buttonScan.setOnClickListener(this);
		btnSendCall.setOnClickListener(this);
		btnEndCall.setOnClickListener(this);
		btnGallery.setOnClickListener(this);
		
		lstAdapter = new WifiListAdapter(this, peerList);
		
		lstPeer.setAdapter( lstAdapter );
		lstPeer.setOnItemClickListener( new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				makeConnection1( position );
			}
		
		});

		ImageHandler imageHandler_ = new ImageHandler();
		socketConection = new SocketConnection( this, imageHandler_ );
		
		setWiFiStatus();
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		 MenuInflater inflater = getMenuInflater();
	     inflater.inflate(R.menu.activity_main, menu);
	     return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		case R.id.menu_create:
			new Thread( new Runnable() {
//				
				@Override
				public void run() {
					socketConection.initServerSocket();
					
				}
			}).start();

			break;
		case R.id.menu_join:
			wifi.startScan();
			break;
		
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void setWiFiStatus() {
		// Setup WiFi
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifi.setWifiEnabled( true );

		WifiInfo info = wifi.getConnectionInfo();

		// Register Broadcast Receiver
		if (receiver == null)
			receiver = new WiFiScanReceiver(this);

//		registerReceiver(receiver, new IntentFilter(
//				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		Log.d(TAG, "onCreate()");
	}

	private void makeConnection1( int position ) {
		    Log.i(TAG, "* connectToAP");

		    WifiConfiguration wifiConfiguration = new WifiConfiguration();

		    String networkSSID = this.peerList.get( position ).getSsId();
		    WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		    for (ScanResult result : scanResultList) {
		        if (result.SSID.equals(networkSSID)) {

		            String securityMode = getScanResultSecurity(result);

		            if (securityMode.equalsIgnoreCase("OPEN")) {

		                wifiConfiguration.SSID = "\"" + networkSSID + "\"";
		                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		                int res = wifiManager.addNetwork(wifiConfiguration);
		                Log.d(TAG, "# add Network returned, SSID " + res + "," + wifiConfiguration.SSID);

		                boolean b = wifiManager.enableNetwork(res, true);
		                Log.d(TAG, "# enableNetwork returned " + b);

		                String ip = intToIP(wifiManager.getConnectionInfo().getIpAddress());
		                System.out.println("IP1::" + ip);
		                wifiManager.setWifiEnabled(true);

		            }
		            this.peerList.get( position ).setStatus( true );
		        }
		    }
		}

		public String getScanResultSecurity(ScanResult scanResult) {
		    Log.i(TAG, "* getScanResultSecurity");

		    final String cap = scanResult.capabilities;
		    final String[] securityModes = { "WEP", "PSK", "EAP" };

		    for (int i = securityModes.length - 1; i >= 0; i--) {
		        if (cap.contains(securityModes[i])) {
		            return securityModes[i];
		        }
		    }

		    return "OPEN";
		}
	
	@Override
	public void onStop() {
		super.onStop();
		unregisterReceiver(receiver);
	}

	
	public void notifyUI( List<ScanResult> scanResult ) {
		peerList.clear();
		this.scanResultList = scanResult;
		
		for (ScanResult result : scanResult) {
			WiFiPeer wifiPeer = new WiFiPeer();
			wifiPeer.setBssId( result.BSSID );
			wifiPeer.setSsId( result.SSID );
			peerList.add( wifiPeer );
		}
		this.lstAdapter.notifyDataSetChanged();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(receiver, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		
		SAProgress.getInstance().hide();
	}

	public void onClick(View view) {
		Toast.makeText(this, "On Click Clicked. Toast to that!!!",
				Toast.LENGTH_LONG).show();

		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		DhcpInfo info = wifiManager.getDhcpInfo();
		server = intToIP(info.serverAddress);
		final String data;
		
		if (view.getId() == R.id.buttonScan) {
			
		}
		
		else if (view.getId() == R.id.btnSendCall) {
			data = this.edtNumber.getText().toString();
			async( server, "call_" + data );
		}
		
		else if (view.getId() == R.id.btnEndCall) {
			data = this.edtNumber.getText().toString();
			async( server, "end_" + data );
		}
		
		else if(view.getId() == R.id.btnGallery) {
			this.openGallery();
			
		}
		
	}
	
	private void async(final String ipAddress, final Object obj ) {
		try {
			if( thread_ != null && thread_.isAlive() ) {
				thread_.join( 500 );
			}
			
			thread_ = new Thread( new Runnable() {
				
				@Override
				public void run() {
					socketConection.initClientSocket( ipAddress, obj);
				}
			});
			
			thread_.start();
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public String getWifiApIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
	                .hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            if (intf.getName().contains("wlan")) {
	                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
	                        .hasMoreElements();) {
	                    InetAddress inetAddress = enumIpAddr.nextElement();
	                    if (!inetAddress.isLoopbackAddress()
	                            && (inetAddress.getAddress().length == 4)) {
	                        Log.d(TAG, inetAddress.getHostAddress());
	                        return inetAddress.getHostAddress();
	                    }
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        Log.e(TAG, ex.toString());
	    }
	    return null;
	}
	
	public String intToIP(int i) {
	       return (( i & 0xFF)+ "."+((i >> 8 ) & 0xFF)+
	                          "."+((i >> 16 ) & 0xFF)+"."+((i >> 24 ) & 0xFF));
	} 
	
	private void openGallery() {
		Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(intent, GALLERY_ACTIVITY_REQUEST_CODE);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		super.onActivityResult(requestCode, resultCode, data);
		String picturePath = "";
		
		if( requestCode == GALLERY_ACTIVITY_REQUEST_CODE && null != data ) {
	        Uri selectedImage = data.getData();
	        String[] filePathColumn = { MediaStore.Images.Media.DATA };
	        Cursor cursor = getContentResolver().query(selectedImage,
	                filePathColumn, null, null, null);
	        cursor.moveToFirst();
	        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
	        picturePath = cursor.getString(columnIndex);
	        
	        BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 10;
            
	        Bitmap bMap = BitmapFactory.decodeFile( picturePath, options );
	        ByteArrayOutputStream stream = new ByteArrayOutputStream();
	        bMap.compress(Bitmap.CompressFormat.PNG, 100, stream);
	        byte[] byteArray = stream.toByteArray();
	        
	        async( server, byteArray );
		}

	}
	
	private class WifiListAdapter extends BaseAdapter {

		private Context cxt;
		private LayoutInflater inFlater;
		private List<WiFiPeer> aList;
		
		WifiListAdapter(Context context, List<WiFiPeer> wifiList) {
			this.aList = wifiList;
			this.cxt = context;
			inFlater = (LayoutInflater)this.cxt.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		}
		
		@Override
		public int getCount() {
			
			return this.aList.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return this.aList.get( position );
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			
			if( convertView == null ) {
				convertView = inFlater.inflate( R.layout.wifi_peer_list, null );
				viewHolder = new ViewHolder();
				viewHolder.txtView = (TextView)convertView.findViewById( R.id.textName );
				viewHolder.txtStatus = (TextView)convertView.findViewById( R.id.textStatus );
				convertView.setTag( viewHolder );
			}
			
			else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			
			viewHolder.txtView.setText( this.aList.get( position ).getSsId() );
			String status = this.aList.get( position ).isStatus() ? "Connected" : ""; 
			viewHolder.txtStatus.setText( status );
			
			return convertView;
		}
		
	}
	
	class ViewHolder {
		TextView txtView;
		TextView txtStatus;
	}
	
	class ImageHandler extends Handler {
		
		private ProgressDialog mDialog;
		
		@Override
		public void handleMessage(Message msg) {
			byte[] b = (byte[])msg.obj;
			final String path = saveImageToFile( b );
			
	        AlertDialog.Builder aBuider = new AlertDialog.Builder(WiFiDemo.this);
	        aBuider.setMessage("Image is received");
	        aBuider.setCancelable(false);
	        aBuider.setPositiveButton( "View", new DialogInterface.OnClickListener()
	        {
	            @Override
	            public void onClick(DialogInterface dialog, int which)
	            {
	                dialog.dismiss();
	                hideProgress();
	                Intent intent = new Intent( WiFiDemo.this, ImageView.class);
	                intent.putExtra("my_image", path);
	                startActivity( intent );
	            }
	        });
	        aBuider.setNegativeButton( "Cancel", new DialogInterface.OnClickListener()
	        {
	            @Override
	            public void onClick(DialogInterface dialog, int which)
	            {
	                dialog.dismiss();
	            }
	        });
	        AlertDialog aDialog = aBuider.create();
	        // aDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextSize(20);
	        aDialog.show();
		    
			super.handleMessage(msg);
		}
		
		public void showProgressDialog() {
			runOnUiThread( new Runnable() {
				
				@Override
				public void run() {
					SAProgress.getInstance().show(WiFiDemo.this, "Receiving image");
				}
			});
		}
		
		public void hideProgress() {
			runOnUiThread( new Runnable() {
				
				@Override
				public void run() {
					SAProgress.getInstance().hide();
				}
			});
		}
		
	}

	private String saveImageToFile( byte[] b ) {
		String filePath = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + System.currentTimeMillis() + ".png";
		File file = new File(filePath);
	      
	      try {

	         FileOutputStream outStream = new FileOutputStream(file);
	         
	         outStream.write( b );
	         outStream.flush();
	         outStream.close();
	         
	      } catch (Exception e) {
	         e.printStackTrace();
	      }
	      
	      return filePath;
	}
}