package com.example;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ListView;

public class ImageView extends Activity {

	private Bitmap bMap;
	private android.widget.ImageView imageView;
	private boolean isImageProceed = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView( R.layout.activity_view );
		
		imageView = (android.widget.ImageView)findViewById( R.id.imgView );
		new Thread( new Runnable() {
			
			@Override
			public void run() {
				String path = getIntent().getStringExtra("my_image");
				bMap = BitmapFactory.decodeFile( path );
				isImageProceed = true;
			}
		}).start();
		
		while( !isImageProceed ) {}
		
		imageView.setImageBitmap( bMap );
		isImageProceed = false;
	}
	
	@Override
	protected void onStop() {
		if( bMap != null ) {
			bMap.recycle();
			bMap = null;
		}
		super.onStop();
	}
	
}
