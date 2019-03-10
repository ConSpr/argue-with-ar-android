package de.home.constantinsprenger.imagerecognition;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Objects;

import de.home.constantinsprenger.imagerecognition.helper.CameraPermissionHelper;
import de.home.constantinsprenger.imagerecognition.helper.FullScreenHelper;

public class MainActivity extends AppCompatActivity {
	//STATICS
	private static final String TAG = MainActivity.class.getSimpleName();
	public static Toast toastMessage;
	public static AugmentedImageDatabase augmentedImageDatabase;


//FIELDS

	private ArSceneView arSceneView;
	private boolean installRequested;

	private Session session;
	private boolean shouldConfigureSession = false;
	Vibrator vibrator;
	File dbfile;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		arSceneView = findViewById(R.id.surfaceview);
		dbfile = new File(getApplicationContext().getFilesDir() + "/arimagedb.imgdb");
		installRequested = false;
		initializeSceneView();
		this.vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		ImageButton addImageButton = findViewById(R.id.addButton);
		ImageButton recordImageButton = findViewById(R.id.recordButton);
		addImageButton.setOnClickListener(this::onClickAddImageButton);
		recordImageButton.setOnClickListener(this::onClickRecordImageButton);

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

	private void onUpdateFrame(FrameTime frameTime) {
		Frame frame = arSceneView.getArFrame();
		Collection<AugmentedImage> updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);
		for (AugmentedImage augmentedImage : updatedAugmentedImages) {
			if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
				// Check camera image matches our reference image and show a Toast
				if (!augmentedImage.getName().equals(null)) {
					makeMessage("WOW we found " + augmentedImage.getName() + "!");
					vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
				}
			}
		}

	}
	private static byte[] NV21toJPEG(byte[] nv21, int width, int height) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
		yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);
		return out.toByteArray();
	}

	private static byte[] YUV_420_888toNV21(Image image) {
		byte[] nv21;
		ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
		ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
		ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

		int ySize = yBuffer.remaining();
		int uSize = uBuffer.remaining();
		int vSize = vBuffer.remaining();

		nv21 = new byte[ySize + uSize + vSize];

		//U and V are swapped
		yBuffer.get(nv21, 0, ySize);
		vBuffer.get(nv21, ySize, vSize);
		uBuffer.get(nv21, ySize + vSize, uSize);

		return nv21;
	}



	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){

		if (requestCode == 2) {
			if(resultCode == 2){
				try {
					String name = (String) data.getExtras().get("name");
					float width = (float) data.getExtras().get("size");
					byte[] bitmap = (byte[]) data.getExtras().get("image");
					Bitmap btm = BitmapFactory.decodeByteArray(bitmap, 0, bitmap.length);
					addImage(btm, name, width);
					makeMessage("Image saved");
				} catch (NullPointerException e) {
					e.printStackTrace();
				}

			}
			else if (resultCode == RESULT_CANCELED){
				makeMessage("No image saved");
			}
		} else {
			makeMessage("Something crazy happened");
		}
	}

	private void configureDB() {
		deserializeDB();
		Config config = new Config(session);
		config.setAugmentedImageDatabase(augmentedImageDatabase);
		config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
		config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
		session.configure(config);
	}

	private void serializeDB() {
		try {
			OutputStream os = new FileOutputStream(dbfile);
			augmentedImageDatabase.serialize(os);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void deserializeDB() {
		try {
			if (dbfile.exists()) {
				Log.d("AugmentedImagesDatabase", "Found Database");
				InputStream is = new FileInputStream(dbfile);
				augmentedImageDatabase = AugmentedImageDatabase.deserialize(session, is);
			} else {
				Log.d("AugmentedImagesDatabase", "Creating new Database");
				String[] imgs = this.getAssets().list("");
				Bitmap bm;
				augmentedImageDatabase = new AugmentedImageDatabase(session);
				for (String img : Objects.requireNonNull(imgs))
					if (img.contains(".jpg") || img.contains(".jpeg")) {
						bm = BitmapFactory.decodeStream(this.getAssets().open(img));
						augmentedImageDatabase.addImage(img,bm);
					}
					serializeDB();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void addImage(Bitmap bitmap, String name, float length) {
		augmentedImageDatabase.addImage(name, bitmap, length);
		serializeDB();
		deserializeDB();
	}

	private void onClickAddImageButton(View v) {
		Intent intent = new Intent(this, AddImageActivity.class);
		intent.putExtra("Type", 1);
		startActivityForResult(intent, 2);
	}

	private void onClickRecordImageButton(View v) {
		Intent intent = new Intent(this, AddImageActivity.class);
		try {
			Image currentImage = arSceneView.getArFrame().acquireCameraImage();
			int imageFormat = currentImage.getFormat();
			if (imageFormat == ImageFormat.YUV_420_888) {
				Log.d("ImageFormat", "Image format is YUV_420_888");
				byte[] jpeg = NV21toJPEG(YUV_420_888toNV21(currentImage), currentImage.getWidth(), currentImage.getHeight());
				intent.putExtra("jpeg", jpeg);
				intent.putExtra("Type", 2);
			}
		} catch (NullPointerException | NotYetAvailableException e) {
			e.printStackTrace();
			Log.e(TAG, "Cant get image from camera stream.");
			intent.putExtra("Type", 1);

		}

		startActivityForResult(intent, 2);
	}

}
