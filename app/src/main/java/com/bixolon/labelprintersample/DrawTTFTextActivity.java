package com.bixolon.labelprintersample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.bixolon.commonlib.emul.image.LabelImage;
import com.bixolon.labelprinter.BixolonLabelPrinter;

import java.io.IOException;

public class DrawTTFTextActivity extends Activity implements View.OnClickListener{

	private EditText editText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_draw_ttf_text);

		Button button = (Button) findViewById(R.id.button1);
		button.setOnClickListener(this);
		button = (Button) findViewById(R.id.button2);
		button.setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_draw_text1, menu);
		return true;
	}
	String[] fontList = null;
	public void onClick(View v) {
		switch (v.getId()) {
            case R.id.button1:
                fontList = getResources().getStringArray(R.array.ttf_font_list);
                new AlertDialog.Builder(DrawTTFTextActivity.this).setItems(fontList, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((TextView)findViewById(R.id.textView3)).setText(fontList[which]);
                    }
                }).show();

                break;
            case R.id.button2:
                printLabel();
                break;
		}
	}
	

	private void printLabel() {
		EditText editText = (EditText) findViewById(R.id.editText1);
		
		String data = editText.getText().toString();

		editText = (EditText) findViewById(R.id.editText2);
		if(editText.getText().toString().equals("")){
			editText.setText("50");
		}
		int horizontalPosition = Integer.parseInt(editText.getText().toString());
		
		editText = (EditText) findViewById(R.id.editText3);
		if(editText.getText().toString().equals("")){
			editText.setText("100");
		}
		int verticalPosition = Integer.parseInt(editText.getText().toString());

		String fontName = ((TextView)findViewById(R.id.textView3)).getText().toString();

		editText = findViewById(R.id.editText4);
		if(editText.getText().toString().equals("")){
		    editText.setText("30");
        }
		int fontSize = Integer.parseInt(editText.getText().toString());

		RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
		int rotation = BixolonLabelPrinter.ROTATION_NONE;
		switch (radioGroup.getCheckedRadioButtonId()) {
		case R.id.radio1:
			rotation = BixolonLabelPrinter.ROTATION_90_DEGREES;
			break;
		case R.id.radio2:
			rotation = BixolonLabelPrinter.ROTATION_180_DEGREES;
			break;
		case R.id.radio3:
			rotation = BixolonLabelPrinter.ROTATION_270_DEGREES;
			break;
		}

		CheckBox checkBox = (CheckBox) findViewById(R.id.checkBox1);
		boolean italic = checkBox.isChecked();

		checkBox = (CheckBox) findViewById(R.id.checkBox2);
		boolean bold = checkBox.isChecked();
        Bitmap bitmap = null;
		try {
            bitmap = onDrawText(data, fontName, fontSize, bold, italic);
        }catch(RuntimeException e){
            Toast.makeText(this, "Please select font", Toast.LENGTH_SHORT).show();
            return;
        }
        int width = bitmap.getWidth();
        if(rotation %2 != 0){
            width = bitmap.getHeight();
        }
        MainActivity.mBixolonLabelPrinter.drawImage(bitmap, horizontalPosition, verticalPosition, width, 100, 0, BixolonLabelPrinter.LABEL_IMAGE_RLE, rotation);
		MainActivity.mBixolonLabelPrinter.print(1, 1);
	}

    private Bitmap onDrawText(String data, String fontname, int fontSize, boolean bold, boolean italic){
        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        AssetManager am = this.getAssets();
        Typeface plain = Typeface.createFromAsset(am, fontname);
        int fontStyle = (bold && italic) ? Typeface.BOLD_ITALIC : (bold && !italic) ? Typeface.BOLD : (!bold && italic) ? Typeface.ITALIC : Typeface.NORMAL;
        // init params - size, color, typeface
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(fontSize);
        textPaint.setTypeface(Typeface.create(plain, fontStyle));
        int measuredTextWidth = (int)textPaint.measureText(data);
        StaticLayout sl = new StaticLayout(data, textPaint, measuredTextWidth, Layout.Alignment.ALIGN_NORMAL, 1, 1, true);

        int boundsHeight = sl.getHeight();

        Bitmap bmp = Bitmap.createBitmap(measuredTextWidth, boundsHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bmp, 0f, 0f, null);
        canvas.save();
        sl.draw(canvas);
        canvas.restore();
        return bmp;
    }

}
