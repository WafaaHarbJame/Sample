package com.bixolon.labelprintersample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bixolon.commonlib.BXLCommonConst;
import com.bixolon.labelprinter.BixolonLabelPrinter;
import com.bixolon.labelprintersample.R;

import java.io.File;

@SuppressLint("NewApi")
public class DrawPDFActivity extends Activity implements View.OnClickListener{
    private static final int FILE_SELECT_CODE = 1;

    private ImageView mImageView;
    private TextView mTextView;
    private Spinner mSpinner;
    private Spinner compressSpinner;
    private ImageButton btnLeft, btnRight;
    private int currentPage = 1;
    private int maxPage = 0;
    boolean _isBtnDown = false;
    private Uri uri;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw_pdf);
        mImageView = (ImageView) findViewById(R.id.imageView1);
        mTextView = (TextView) findViewById(R.id.textView5);
        mSpinner = (Spinner)findViewById(R.id.spinner1);
        compressSpinner = (Spinner)findViewById(R.id.spinner2);
        Button button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(this);
        button = (Button) findViewById(R.id.button2);
        button.setOnClickListener(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,R.layout.layout_textview,new String[]{"Use","Unused"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        compressSpinner.setAdapter(adapter);
        mSpinner.setAdapter(adapter);
        btnLeft = (ImageButton)findViewById(R.id.btnLeft);
        btnRight = (ImageButton)findViewById(R.id.btnRight);

//		btnLeft.setOnTouchListener(onBtnTouchListener);
//		btnRight.setOnTouchListener(onBtnTouchListener);
        btnLeft.setOnClickListener(this);
        btnRight.setOnClickListener(this);

    }

    private void onLeftClick(){
        if(currentPage <= 1){
            return;
        }else{
            currentPage-=1;

            if(currentPage == 1){
                btnLeft.setVisibility(View.INVISIBLE);
            }
            if(currentPage < maxPage){
                btnRight.setVisibility(View.VISIBLE);
            }

            String pathName = mTextView.getText().toString();
            Bitmap bitmap = null;

            bitmap = MainActivity.mBixolonLabelPrinter.getPdfPage(uri, currentPage);

            if(bitmap != null){
                mImageView.setImageBitmap(bitmap);
            }
        }
    }

    private void onRightClick(){
        if(currentPage >= maxPage){
            return;
        }else{
            currentPage+=1;
            if(currentPage > 1){
                btnLeft.setVisibility(View.VISIBLE);
            }
            if(currentPage == maxPage){
                btnRight.setVisibility(View.INVISIBLE);
            }
            String pathName = mTextView.getText().toString();
            Bitmap bitmap = null;


            bitmap = MainActivity.mBixolonLabelPrinter.getPdfPage(uri, currentPage);


            if(bitmap != null){
                mImageView.setImageBitmap(bitmap);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_draw_bitmap, menu);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == FILE_SELECT_CODE)
        {
            switch (requestCode) {
                case FILE_SELECT_CODE:
                    if (resultCode == RESULT_OK) {
                        currentPage = 1;

                        // Get the Uri of the selected file
                        uri = data.getData();
                        mTextView.setText(getRealPathFromURI(uri));

                        Bitmap bitmap = null;

                        long result = MainActivity.mBixolonLabelPrinter.setPDFLicenseKey("PDF_STD_04062021");
                        if(result == BXLCommonConst._BXL_RC_SUCCESS){
                            bitmap = MainActivity.mBixolonLabelPrinter.getPdfPage(uri, currentPage);

                            if(bitmap != null){
                                mImageView.setImageBitmap(bitmap);

                                maxPage = MainActivity.mBixolonLabelPrinter.getCountPdfPages(uri);
                                EditText editText = (EditText) findViewById(R.id.editText2);
                                editText.setText(Integer.toString(maxPage));

                                if(maxPage > 0){
                                    btnLeft.setVisibility(View.VISIBLE);
                                    btnRight.setVisibility(View.VISIBLE);
                                    editText = (EditText) findViewById(R.id.editText1);
                                    editText.setText("1");
                                }
                            }
                        }else{
                            Toast.makeText(DrawPDFActivity.this, "invalid license key", Toast.LENGTH_SHORT).show();
                        }

                    }
                    break;
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.button1:
                printPDF();
                break;

            case R.id.button2:
                pickFile();
                break;

            case R.id.btnRight:
                onRightClick();
                break;
            case R.id.btnLeft:
                onLeftClick();
                break;
        }
    }

    private void pickFile() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);

        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File"),FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void printPDF()
    {
        String pathName = mTextView.getText().toString();
        if(pathName == null || pathName.length() == 0)
        {
            Toast.makeText(getApplicationContext(), "No pdf file!", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText editText = (EditText) findViewById(R.id.editText1);
        int startPage = Integer.parseInt(editText.getText().toString());
        editText = (EditText) findViewById(R.id.editText2);
        int endPage = Integer.parseInt(editText.getText().toString());

        editText = (EditText) findViewById(R.id.editText3);
        int width = Integer.parseInt(editText.getText().toString());

        editText = (EditText) findViewById(R.id.editText4);
        int level = Integer.parseInt(editText.getText().toString());

        boolean dithering = mSpinner.getSelectedItemPosition()== 0?true:false;
        MainActivity.mBixolonLabelPrinter.beginTransactionPrint();
        for(int i = startPage; i<endPage+1; i++){
            MainActivity.mBixolonLabelPrinter.drawPDFFile(uri, 0, 0, i, width, level,
                    dithering, compressSpinner.getSelectedItemPosition() == 0 ? true : false);
            MainActivity.mBixolonLabelPrinter.print(1, 1);
        }

        MainActivity.mBixolonLabelPrinter.endTransactionPrint();
    }


    private String getRealPathFromURI(Uri uri) {
        String filePath = "";
        filePath = uri.getPath();
        if (filePath.startsWith("/"))
            return filePath;

        String wholeID = DocumentsContract.getDocumentId(uri);
        String id = wholeID.split(":")[1];
        String[] column = { MediaStore.Files.FileColumns.DATA };

        String sel = MediaStore.Files.FileColumns.DATA + " LIKE '%" + id + "%'";

        Cursor cursor = getContentResolver().query(MediaStore.Files.getContentUri("external"),
                column, sel, null, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

}
