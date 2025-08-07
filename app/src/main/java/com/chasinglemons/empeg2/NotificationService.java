package com.chasinglemons.empeg2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

@SuppressLint("NewApi")
public class NotificationService extends Service {
    private static final String CHANNEL_ID = "empeg_channel";
    private NotificationManager mNM;
	private final Handler handler = new Handler();
	SharedPreferences config;
	String playerIP;
//	private NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
	//Different Id's will show up as different notifications
//	private int mNotificationId = 1;
	private boolean firstTime = true;
	Intent appIntent,button1Intent,button2Intent,button3Intent,button4Intent;
	PendingIntent pIntent,pb1,pb2,pb3,pb4;
	RemoteViews contentView;
	Notification notificator;

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Empeg Notifications";
            String description = "Channel for Empeg Now Playing";
            int importance = NotificationManager.IMPORTANCE_LOW;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
	 * Class for clients to access.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with
	 * IPC.
	 */
	public class LocalBinder extends Binder {
		NotificationService getService() {
			return NotificationService.this;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		createNotificationChannel();
		appIntent = new Intent(this, Start.class);
		appIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		pIntent = PendingIntent.getActivity(this, 0, appIntent, PendingIntent.FLAG_IMMUTABLE);

		button1Intent = new Intent(this, NotifButtonListener.class);
		button1Intent.putExtra("action", "up");
		pb1 = PendingIntent.getBroadcast(this, 351, button1Intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

		button2Intent = new Intent(this, NotifButtonListener.class);
		button2Intent.putExtra("action", "left");
		pb2 = PendingIntent.getBroadcast(this, 352, button2Intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

		button3Intent = new Intent(this, NotifButtonListener.class);
		button3Intent.putExtra("action", "right");
		pb3 = PendingIntent.getBroadcast(this, 353, button3Intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

		button4Intent = new Intent(this, NotifButtonListener.class);
		button4Intent.putExtra("action", "down");
		pb4 = PendingIntent.getBroadcast(this, 354, button4Intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

		contentView = new RemoteViews(getPackageName(), R.layout.custom_notification);
		contentView.setOnClickPendingIntent(R.id.imageButton1, pb1);
		contentView.setOnClickPendingIntent(R.id.imageButton2, pb2);
		contentView.setOnClickPendingIntent(R.id.imageButton3, pb3);
		contentView.setOnClickPendingIntent(R.id.imageButton4, pb4);

		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		config = PreferenceManager.getDefaultSharedPreferences(this);
		playerIP = config.getString("activeEmpegIP", "none");
		// Display a notification about us starting.  We put an icon in the status bar.
		updateNotification("","","");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handler.removeCallbacks(sendUpdatesToNotif);
		handler.postDelayed(sendUpdatesToNotif, 1000); // 1 second
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		// Cancel the persistent notification.
		mNM.cancel(0);

		handler.removeCallbacks(sendUpdatesToNotif);	
		super.onDestroy();

	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	// This is the object that receives interactions from clients.  See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();

	/**
	 * Show a notification while this service is running.
	 * @return 
	 */
	@SuppressLint("NewApi")
	private void updateNotification(String eArtist, String eTitle, String eTime) {
		// Build your custom RemoteViews layout
		RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.custom_notification);
		contentView.setTextViewText(R.id.notification_title, "Empeg Remote");
		contentView.setTextViewText(R.id.notification_text, eArtist + " - " + eTitle + " " + eTime);

		if (firstTime) {
			// Build the notification (only once)
			notificator = new NotificationCompat.Builder(this, CHANNEL_ID)
					.setSmallIcon(R.drawable.player_white)
					.setCustomContentView(contentView)
					.setCustomBigContentView(contentView)
					.setStyle(new NotificationCompat.DecoratedCustomViewStyle())
					.setContentIntent(pIntent)
					.setOnlyAlertOnce(true)
					.setWhen(0)
					.build();

			firstTime = false;
		} else {
			// Update the content view
			notificator.contentView = contentView;
			notificator.bigContentView = contentView;
		}

		mNM.notify(0, notificator);
	}

	/*    private void showNotification() {

    	Intent intent = new Intent(this, Start.class);
    	PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

    	Intent switchIntent = new Intent(this, TestButtonListener.class);
        PendingIntent pendingSwitchIntent = PendingIntent.getBroadcast(this, 0, switchIntent, 0);

    	// Build notification
    	noti = new NotificationCompat.Builder(this)
    	        .setContentTitle("Empeg Remote")
    	        .setOnlyAlertOnce(true)
    	        .setContentText("Artist - Song - (time)")
    	        .setSmallIcon(R.drawable.ic_launcher)
    	        .setContentIntent(pIntent)
    	        .addAction(R.drawable.button_a4sm, "Power", pendingSwitchIntent)
    	        .addAction(R.drawable.button_f3sm, "Play/Pause", pIntent).build();

        // Send the notification.
        mNM.notify(NOTIFICATION, noti);
    }*/

	private Runnable sendUpdatesToNotif = new Runnable() {
		public void run() {

			new DownloadDataTask().execute("http://"+playerIP+"/proc/empeg_notify");

			handler.postDelayed(this, 1000); // 1 second
		}
	};

	public class DownloadDataTask extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... url) {
			StringBuilder responseBody = new StringBuilder();
			HttpURLConnection connection = null;
			BufferedReader reader = null;

			try {
				URL requestUrl = new URL(url[0]);
				connection = (HttpURLConnection) requestUrl.openConnection();
				connection.setRequestMethod("GET");
				connection.setConnectTimeout(10000);
				connection.setReadTimeout(10000);
				connection.connect();

				InputStream inputStream = connection.getInputStream();
				reader = new BufferedReader(new InputStreamReader(inputStream));

				String line;
				while ((line = reader.readLine()) != null) {
					responseBody.append(line);
				}
			} catch (IOException e) {
				// Log or handle the error
			} finally {
				if (reader != null) {
					try { reader.close(); } catch (IOException ignored) {}
				}
				if (connection != null) {
					connection.disconnect();
				}
			}

			return responseBody.toString();
		}

		@Override
		protected void onPostExecute(String result) {
			result = result.replaceAll("(\\r|\\n)", "");

			Pattern VERSE_PATTERN = Pattern.compile(
					"notify_FidTime = \"(.*?)\";notify_Artist = \"(.*?)\";notify_FID = \"(.*?)\";" +
							"notify_Genre = \"(.*?)\";notify_MixerInput = \"(.*?)\";notify_Track = \"(.*?)\";" +
							"notify_Sound = \"(.*?)\";notify_Title = \"(.*?)\";notify_Volume = \"(.*?)\";"
			);
			Matcher m = VERSE_PATTERN.matcher(result);
			while (m.find()) {
				String timeHolder;
				String timePart = m.group(1).split("  ")[1];
				if (timePart.startsWith("0:0")) {
					timeHolder = timePart.substring(3);
				} else if (timePart.startsWith("0:")) {
					timeHolder = timePart.substring(2);
				} else {
					timeHolder = timePart;
				}

				updateNotification(m.group(2), m.group(8), "(" + timeHolder + ")");
			}
		}
	}


	public static class NotifButtonListener extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			SharedPreferences config = PreferenceManager.getDefaultSharedPreferences(context);
			String playerIP = config.getString("activeEmpegIP", "none");

			Bundle extras = intent.getExtras();
			if (extras != null) {

				if (extras.getString("action").equals("up")) {
					new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Top");
				} else if (extras.getString("action").equals("left")) {
					new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Left");
				} else if (extras.getString("action").equals("right")) {
					new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Right");
				} else if (extras.getString("action").equals("down")) {
					new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Bottom");
				}
			}
		}

		public class sendCommand extends AsyncTask<String, String, String> {
			@Override
			protected String doInBackground(String... url) {
				String responseBody = "";
				try {
					HttpClient httpclient = new DefaultHttpClient();
					//				//Log.i("EMPEG","FETCHING: "+url[0]);
					HttpGet httpget = new HttpGet(url[0]);
					ResponseHandler<String> responseHandler = new BasicResponseHandler();
					responseBody = httpclient.execute(httpget, responseHandler);

					httpclient.getConnectionManager().shutdown();
				} catch (MalformedURLException e) {
					//Log.i("PHONEMAIN","MalformedURLException - sendCommand");
				} catch (IOException e) {
					//Log.i("PHONEMAIN","IOException - sendCommand");
				}
				return responseBody;
			}

			@Override
			protected void onProgressUpdate(String... progress) {
			}

			@Override
			protected void onPostExecute(String result) {
				//			//Log.i("EMPEG","onPostExecute: "+result);
			}
		}
	}
}
