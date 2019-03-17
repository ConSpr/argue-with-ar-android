package de.home.constantinsprenger.imagerecognition.testapp;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import de.home.constantinsprenger.imagerecognition.testapp.helper.CameraPermissionHelper;
import de.home.constantinsprenger.imagerecognition.testapp.helper.FullScreenHelper;

public class MainActivity extends AppCompatActivity {
	//STATICS
	private static final String TAG = MainActivity.class.getSimpleName();
	public static Toast toastMessage;
	public static AugmentedImageDatabase augmentedImageDatabase;
	public static long TIMESTAMP = 0;
	private String logfilepath = "/logging.txt";

//FIELDS

	private ArSceneView arSceneView;
	private boolean installRequested;

	private Session session;
	private boolean shouldConfigureSession = false;
	private File logfile;
	private int numberOfRestarts = 100;
	private boolean found = false;
	private String currentLog = "";
	private OutputStreamWriter myOutWriter;

	public static String convertStreamToString(InputStream is) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}
		is.close();
		reader.close();
		return sb.toString();
	}


	@Override
	protected void onResume() {
		super.onResume();

		if (session == null) {
			Exception exception = null;
			String message = null;
			try {
				switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
					case INSTALL_REQUESTED:
						installRequested = true;
						return;
					case INSTALLED:
						break;
				}

				// ARCore requires camera permissions to operate. If we did not yet obtain runtime
				// permission on Android M and above, now is a good time to ask the user for it.
				if (!CameraPermissionHelper.hasCameraPermission(this)) {
					CameraPermissionHelper.requestCameraPermission(this);
					return;
				}

				session = new Session(/* context = */ this);
			} catch (UnavailableArcoreNotInstalledException
					| UnavailableUserDeclinedInstallationException e) {
				message = "Please install ARCore";
				exception = e;
			} catch (UnavailableApkTooOldException e) {
				message = "Please update ARCore";
				exception = e;
			} catch (UnavailableSdkTooOldException e) {
				message = "Please update this app";
				exception = e;
			} catch (Exception e) {
				message = "This device does not support AR";
				exception = e;
			}

			if (message != null) {
				makeMessage(message);
				Log.e(TAG, "Exception creating session", exception);
				return;
			}
			shouldConfigureSession = true;
		}
		if (shouldConfigureSession) {
			configureDB();
			shouldConfigureSession = false;
			arSceneView.setupSession(session);
		}
		try {
			session.resume();
			arSceneView.resume();
		} catch (CameraNotAvailableException e) {

			makeMessage("Camera not available. Please restart the app.");
			session = null;
			return;
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (session != null) {
			// Note that the order matters - GLSurfaceView is paused first so that it does not try
			// to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
			// still call session.update() and get a SessionPausedException.
			arSceneView.pause();
			session.pause();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
		if (!CameraPermissionHelper.hasCameraPermission(this)) {
			Toast.makeText(
					this, "Camera permissions are needed to run this application", Toast.LENGTH_LONG)
					.show();
			if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
				// Permission denied with checking "Do not ask again".
				CameraPermissionHelper.launchPermissionSettings(this);
			}
			finish();
		}
		if (ContextCompat.checkSelfPermission(MainActivity.this,
				Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ;
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
	}

	private void initializeSceneView() {
		arSceneView.getScene().addOnUpdateListener((this::onUpdateFrame));
	}

	private void makeMessage(String m) {
		if (toastMessage != null) {
			toastMessage.cancel();
		}
		toastMessage = Toast.makeText(this, m, Toast.LENGTH_SHORT);
		toastMessage.show();
	}

	public static String getStringFromFile(String filePath) throws Exception {
		File fl = new File(filePath);
		FileInputStream fin = new FileInputStream(fl);
		String ret = convertStreamToString(fin);
		fin.close();
		return ret;
	}

	private void configureDB() {
		deserializeDB();
		Config config = new Config(session);
		config.setAugmentedImageDatabase(augmentedImageDatabase);
		config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
		config.setFocusMode(Config.FocusMode.AUTO);
		config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
		session.configure(config);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		arSceneView = findViewById(R.id.surfaceview);
		installRequested = false;
		initializeSceneView();
		logfile = new File(getExternalFilesDir(null) + logfilepath);
		try {
			if (logfile.exists()) {
				currentLog = getStringFromFile(logfile.getPath());
			} else {
				logfile.createNewFile();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (getIntent().getExtras() != null) {
			numberOfRestarts = getIntent().getExtras().getInt("restarts", 100);
		}

	}

	private void onUpdateFrame(FrameTime frameTime) {
		Frame frame = arSceneView.getArFrame();

		if (frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
			if (TIMESTAMP == 0) {
				TIMESTAMP = System.currentTimeMillis();
				Timer t = new Timer();
				t.schedule(new TimerTask() {
					@Override
					public void run() {
						MainActivity.this.restart();
					}
				}, 15 * 1000);
			}
		}

		Collection<AugmentedImage> updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);
		for (AugmentedImage augmentedImage : updatedAugmentedImages) {
			if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
				// Check camera image matches our reference image and show a Toast
				if (!augmentedImage.getName().equals(null)) {
					makeMessage("WOW we found " + augmentedImage.getName() + "!");
					found = true;
					this.restart();
				}
			}
		}

	}

	private void deserializeDB() {
		try {
			Log.d("AugmentedImagesDatabase", "Found Database");
			augmentedImageDatabase = AugmentedImageDatabase.deserialize(session, getAssets().open("arimagedb.imgdb"));
		} catch (Exception e) {
			//TODO: Handle Exceptions
		}

	}

	private void restart() {
		try {
			FileOutputStream fOut = new FileOutputStream(logfile);
			Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			myOutWriter = new OutputStreamWriter(fOut);
			if (found) {
				myOutWriter.append(currentLog).append((System.currentTimeMillis() - TIMESTAMP) + ",");
				v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
			} else {
				myOutWriter.append(currentLog).append(",");
			}
			myOutWriter.close();
			fOut.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (numberOfRestarts > 1) {
			numberOfRestarts--;
			Intent mStartActivity = new Intent(this, MainActivity.class);
			mStartActivity.putExtra("restarts", numberOfRestarts);
			int mPendingIntentId = 123456;
			//TODO: Write duration or ERROR
			PendingIntent mPendingIntent = PendingIntent.getActivity(this, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
			AlarmManager mgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
			mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 150, mPendingIntent);
		}
		System.exit(0);
	}

}
