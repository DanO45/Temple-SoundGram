package edu.temple.soundgram;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.nostra13.universalimageloader.core.ImageLoader;

import edu.temple.soundgram.util.API;
import edu.temple.soundgram.util.UploadSoundGramService;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	int userId = 111;
	

	int TAKE_PICTURE_REQUEST_CODE = 11111111;
	int RECORD_AUDIO_REQUEST_CODE = 11111112;
	
	File photo, audio;
	LinearLayout ll;
	
	
	// Refresh stream
	private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
        	if (intent.getAction().equals(UploadSoundGramService.REFRESH_ACTION)){
        		try {
        			loadStream();
        		} catch (Exception e) {}
        	}
        }
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		// Register listener for messages received while app is in foreground
        IntentFilter filter = new IntentFilter();
        filter.addAction(UploadSoundGramService.REFRESH_ACTION);
        registerReceiver(refreshReceiver, filter);
		
		
		ll = (LinearLayout) findViewById(R.id.imageLinearLayout);
		
		File storageDirectory = new File(Environment.getExternalStorageDirectory().toString() + "/" + getString(R.string.audio_directory));
		
		if (!storageDirectory.exists()) 
		{
			storageDirectory.mkdir();
        }
		
		loadStream();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.new_soundgram:
			newSoundGram();
			return true;
		case R.id.load_soundgram:
			loadStream();
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	
	Uri imageUri;
	private void newSoundGram(){
		
		Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		
		File storageDirectory = new File(Environment.getExternalStorageDirectory() + "/" + getString(R.string.app_name));
		
		storageDirectory.mkdir();
		
		photo = new File(storageDirectory, String.valueOf(System.currentTimeMillis()) + ".jpg"); // Temporary file name
		pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
				Uri.fromFile(photo));
		
		imageUri = Uri.fromFile(photo);
		startActivityForResult(pictureIntent, TAKE_PICTURE_REQUEST_CODE); // Launches an external activity/application to take a picture
		
		Toast.makeText(this, "Creating new SoundGram", Toast.LENGTH_LONG).show();
	}
	ImageView imageView;
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK && requestCode == TAKE_PICTURE_REQUEST_CODE) {
			
			imageView = new ImageView(this);
			
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(600, 600); // Set our image view to thumbnail size

			imageView.setLayoutParams(lp);

			ImageLoader.getInstance().displayImage(imageUri.toString(), imageView);
			getAudioClip();
			
			
		} else if (resultCode == Activity.RESULT_OK && requestCode == RECORD_AUDIO_REQUEST_CODE){
			
			imageView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					MediaPlayer mPlayer = new MediaPlayer();
			        try {
			            mPlayer.setDataSource(audio.toString());
			            mPlayer.prepare();
			            mPlayer.start();
			        } catch (IOException e) {
			            e.printStackTrace();
			        }
					
				}
			});
			
			//addViewToStream(imageView);
			
			uploadSoundGram();
		}
		
	}
	
	private void getAudioClip(){
		Intent audioIntent = new Intent(this, RecordAudio.class);
		File storageDirectory = new File(Environment.getExternalStorageDirectory() + "/" + getString(R.string.app_name));
		audio = new File(storageDirectory, String.valueOf(System.currentTimeMillis())); // Temporary file name
		
		audioIntent.putExtra("fileName", audio.getAbsolutePath());
		
		startActivityForResult(audioIntent, RECORD_AUDIO_REQUEST_CODE);
	}
	
	
	private void addViewToStream(View view){
		ll.addView(view);
		
		
		View seperatorLine = new View(this);
        TableLayout.LayoutParams layoutParams = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        layoutParams.setMargins(30,30,30,30);
        seperatorLine.setLayoutParams(layoutParams);
        seperatorLine.setBackgroundColor(Color.rgb(180, 180, 180));
        ll.addView(seperatorLine);
	}
	
	private void uploadSoundGram(){
		
		Intent uploadSoundGramIntent = new Intent(this, UploadSoundGramService.class);
		uploadSoundGramIntent.putExtra(UploadSoundGramService.directory, Environment.getExternalStorageDirectory() + "/" + getString(R.string.app_name));
		uploadSoundGramIntent.putExtra(UploadSoundGramService.image, photo.getAbsolutePath());
		uploadSoundGramIntent.putExtra(UploadSoundGramService.audio, audio.getAbsolutePath());

		startService(uploadSoundGramIntent);
		Toast.makeText(this, "Uploading SoundGram", Toast.LENGTH_SHORT).show();
	}
	
	private void loadStream(){
		
		Thread t = new Thread(){
			@Override
			public void run(){
				try {
					JSONArray streamArray = API.getSoundGrams(MainActivity.this, userId);
					
					Message msg = Message.obtain();
					msg.obj = streamArray;
					
					displayStreamHandler.sendMessage(msg);
				} catch (Exception e) {
				}
			}
		};
		t.start();
		
	}
	
	Handler displayStreamHandler = new Handler(new Handler.Callback() {
		
		@Override
		public boolean handleMessage(Message msg) {
			
			
			JSONArray streamArray = (JSONArray) msg.obj;
			if (streamArray != null) {
				ll.removeAllViews();
				for (int i = 0; i < streamArray.length(); i++){
					try {
						addViewToStream(getSoundGramView(streamArray.getJSONObject(i)));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
			return false;
		}
	});
	
	private View getSoundGramView(final JSONObject soundgramObject){
		LinearLayout soundgramLayout = new LinearLayout(this);
		
		
		ImageView soundgramImageView = new ImageView(this);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(600, 600); // Set our image view to thumbnail size
		soundgramImageView.setLayoutParams(lp);
		try {
			ImageLoader.getInstance().displayImage(soundgramObject.getString("image_url"), soundgramImageView);
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		
		soundgramImageView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				MediaPlayer mPlayer = new MediaPlayer();
				
		        try {
		        	File storageDirectory = new File(Environment.getExternalStorageDirectory() + "/" + getString(R.string.audio_directory));
		       
		        	Uri uri = Uri.parse(soundgramObject.getString("audio_url"));
		        	final String audioFileName = uri.getQueryParameter("filename") + ".mp3";
		        	final File tempfile = new File(storageDirectory, audioFileName);
		        	
		        	Thread downloadThread = new Thread(){
		        		@Override
		        		public void run (){
		        			try{
		        				URL url = new URL(soundgramObject.getString("audio_url").toString());
		        				BufferedInputStream input = new BufferedInputStream(url.openStream());
		        				FileOutputStream output = new FileOutputStream(tempfile);
		        				BufferedOutputStream bufferedout = new BufferedOutputStream(output,1024);

		        				byte[] audioData = new byte[1024];

		        				int x = 0;

		        				while((x = input.read(audioData,0,1024)) >= 0)
		        				{
		        					bufferedout.write(audioData,0,x);               
		        				}
		        				
		        				output.flush();
		        	            bufferedout.flush();
		        	            output.close();
		        	            bufferedout.close();
		        	            input.close();
		        			}
		        			
		        			catch(Exception e)
		        			{
		        				e.printStackTrace();
		        			}
		        			
		        		}
		        	};
		        	
		        	
		        	
		        	File[] fileArray = storageDirectory.listFiles();
		        	File targetFile = null;
		        	
		        	for(int i = 0; i < fileArray.length; i++)
		        	{
		        		if (audioFileName.equals(fileArray[i].getName().toString()))
		        		{
		        			targetFile = fileArray[i];
		        			System.out.println("Got to this point");
		        			break;
		        		}
		        		
		        		else
		        		{
		        			continue;
		        		}
		        	}
		        	
		        	if (targetFile != null)
		        	{
		        		mPlayer.setDataSource(storageDirectory.toString() + "/" + targetFile.getName().toString());
		        		System.out.println("I hope this is working!!!");
        	        	mPlayer.prepare();
    		            mPlayer.start();	
		        	}
		        	
		        	else
		        	{
		        		downloadThread.start();
		        		
		        		mPlayer.setDataSource(tempfile.getName().toString());
		        		System.out.println("First Download");
			            mPlayer.prepare();
			            mPlayer.start();
		        	}
		        	
		            
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		soundgramLayout.addView(soundgramImageView);
		
		return soundgramLayout;
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		unregisterReceiver(refreshReceiver);
	}
	
}