package de.home.constantinsprenger.imagerecognition;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;


public class AddImageActivity extends AppCompatActivity {

	private ImageView imageView;
	private Button button;
	private EditText tag;
	private EditText width;
	private Intent intent;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_image);
		//Get elements of view
		imageView = findViewById(R.id.imageView);
		imageView.setOnClickListener(this::onClickImageView);
		button = findViewById(R.id.button);
		button.setOnClickListener(this::onClickButton);
		tag = findViewById(R.id.tagField);
		width = findViewById(R.id.sizeField);

		intent = getIntent();
		try {
			byte[] jpeg = (byte[]) intent.getExtras().get("jpeg");
			imageView.setImageBitmap(BitmapFactory.decodeByteArray(jpeg,0,jpeg.length));
		}catch (Exception e){

		}
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	private void onClickButton(View v){
		String name = tag.getText().toString();
		float size = Float.parseFloat(width.getText().toString());
		if(name.isEmpty()){
			Toast.makeText(this,"Please enter a name",Toast.LENGTH_LONG);
			return;
		}
		Bitmap img = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		img.compress(Bitmap.CompressFormat.PNG, 100, stream);
		byte[] byteArray = stream.toByteArray();
		Intent intent = new Intent();
		intent.putExtra("name",name);
		intent.putExtra("size", size);
		intent.putExtra("image", byteArray);
		setResult(2, intent);
		finish();
	}
	private void onClickImageView(View v) {
		performFileSearch();
	}

	private void performFileSearch() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("image/*");
		startActivityForResult(intent, 42);
	}
}
