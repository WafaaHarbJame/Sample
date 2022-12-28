package com.bixolon.labelprintersample;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.bixolon.commonlib.BXLCommonConst;
import com.bixolon.commonlib.log.LogService;
import com.bixolon.labelprinter.BixolonLabelPrinter;
import com.bixolon.labelprintersample.R;
import com.bixolon.labelprinter.BixolonLabelPrinter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONObject;

import static android.content.ContentValues.TAG;

@SuppressLint({ "HandlerLeak", "NewApi" })
public class MainActivity extends ListActivity
{

	private static final String[] FUNCTIONS = { "drawText", "drawVectorFontText", "draw1dBarcode", "drawMaxicode", "drawPdf417", "drawQrCode", "drawDataMatrix", "drawBlock", "drawCircle", "setCharacterSet", "setPrintingType", "setMargin", "setLength", "setWidth", "setBufferMode", "clearBuffer", "setSpeed", "setDensity", "setOrientation", "setOffset", "setCutterPosition", "drawBitmap", "initializePrinter", "printInformation", "setAutoCutter", "getStatus", "getPrinterInformation", "executeDirectIo", "printSample", "drawPDF", "setupRFID", "calibrateRFID", "setRFIDPosition", "writeRFID"};

	// Name of the connected device
	private String mConnectedDeviceName = null;
	private final int REQUEST_PERMISSION = 0;
	private ListView mListView;

	private boolean mIsConnected;

	static BixolonLabelPrinter mBixolonLabelPrinter;
	static int count = 1;
	private boolean checkedManufacture = false;

	public Handler m_hHandler = null;
	public BluetoothAdapter m_BluetoothAdapter = null;
	public BluetoothLeScanner mLEScanner = null;
	public ScanSettings settings = null;
	public List<ScanFilter> filters;
	public ArrayAdapter<String> adapter = null;
	public ArrayList<BluetoothDevice> m_LeDevices;

	private ScanCallback mScanCallback;
	private final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private PendingIntent mPermissionIntent;
	private UsbManager usbManager;
	private UsbDevice device;
	private boolean tryedAutoConnect = false;

	private String[] permissions = {
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.ACCESS_COARSE_LOCATION,
			Manifest.permission.ACCESS_FINE_LOCATION
	};

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		MainActivity.count = 1;
		ArrayList<String> list = new ArrayList<String>();
		for(int i = 0; i < FUNCTIONS.length; i++)
		{
			list.add(FUNCTIONS[i]);
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
		mListView = (ListView) findViewById(android.R.id.list);
		mListView.setAdapter(adapter);
//		mListView.setEnabled(false);

		mBixolonLabelPrinter = new BixolonLabelPrinter(this, mHandler, Looper.getMainLooper());
		LogService.InitDebugLog(true, true, BXLCommonConst._LOG_LEVEL_HIGH);

		final int ANDROID_NOUGAT = 24;
		if(Build.VERSION.SDK_INT >= ANDROID_NOUGAT)
		{
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		checkVerify();
		usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        Thread.setDefaultUncaughtExceptionHandler(new SampleUncaughtExceptionHandler());
	}

	@Override
	public void onResume() {
		super.onResume();

		if(!tryedAutoConnect) {
			isConnectedPrinter();
		}
	}

	@Override
	public void onDestroy()
	{
		try
		{
			unregisterReceiver(mUsbReceiver);
		}
		catch(IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		mBixolonLabelPrinter.disconnect();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		if(mIsConnected)
		{

		}
		else
		{
			menu.getItem(0).setEnabled(true);
			menu.getItem(1).setEnabled(true);
			menu.getItem(2).setEnabled(true);
			menu.getItem(3).setEnabled(true);
			menu.getItem(4).setEnabled(true);
			menu.getItem(5).setEnabled(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.item1:
				mBixolonLabelPrinter.findBluetoothPrinters();
				break;

			case R.id.item2:
				if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
				{
					Toast.makeText(this, "BluetoothLE Not Supported", Toast.LENGTH_SHORT).show();
					return false;
				}

				final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
				m_BluetoothAdapter = bluetoothManager.getAdapter();

				// Checks if Bluetooth is supported on the device.
				if(m_BluetoothAdapter == null)
				{
					Toast.makeText(this, "Error Bluetooth Not Supported", Toast.LENGTH_SHORT).show();

					return false;
				}

				if(Build.VERSION.SDK_INT >= 21)
				{
					mLEScanner = m_BluetoothAdapter.getBluetoothLeScanner();
					settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
					//settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_OPPORTUNISTIC).build();
					filters = new ArrayList<ScanFilter>();
				}

				m_hHandler = new Handler();
				adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.select_dialog_singlechoice);
				m_LeDevices = new ArrayList<BluetoothDevice>();

				adapter.clear();
				m_LeDevices.clear();
				scanLeDevice(true);

				AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
				alertBuilder.setIcon(R.drawable.ic_launcher);
				alertBuilder.setTitle("연결할 프린터를 선택하세요");

				alertBuilder.setNegativeButton("취소", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				});

				alertBuilder.setAdapter(adapter, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						scanLeDevice(false);

						BluetoothDevice btDevice = m_LeDevices.get(id);
						mConnectedDeviceName = btDevice.getName();
						mBixolonLabelPrinter.connect(btDevice.getAddress(), 1);

					}
				});
				alertBuilder.show();
				break;

			case R.id.item3:
				mBixolonLabelPrinter.findNetworkPrinters(3);
				return true;

			case R.id.item4:
				DialogManager.showWifiDialog(MainActivity.this, mBixolonLabelPrinter);
				return true;

			case R.id.item5:
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1)
				{
					mBixolonLabelPrinter.findUsbPrinters();
				}
				return true;

			case R.id.item6:
				mBixolonLabelPrinter.disconnect();

				setStatus(R.string.title_not_connected);
                mListView.setEnabled(false);
                mIsConnected = false;
                invalidateOptionsMenu();
				return true;

		}
		return false;
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public void scanLeDevice(final boolean bEnable)
	{
		if(bEnable)
		{
			if(Build.VERSION.SDK_INT >= 21){
				try {
					setScanCallback();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
			}
			// Stops scanning after a pre-defined scan period.
			m_hHandler.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					if(Build.VERSION.SDK_INT < 21)
					{
						m_BluetoothAdapter.stopLeScan(m_LeScanCallback);
					}
					else
					{
						mLEScanner.stopScan(mScanCallback);
					}
				}
			}, 10000);

			if(Build.VERSION.SDK_INT < 21)
			{
				m_BluetoothAdapter.startLeScan(m_LeScanCallback);
			}
			else
			{
				mLEScanner.startScan(filters, settings, mScanCallback);
			}
		}
		else
		{
			if(Build.VERSION.SDK_INT < 21)
			{
				m_BluetoothAdapter.stopLeScan(m_LeScanCallback);
			}
			else
			{
				mLEScanner.stopScan(mScanCallback);
			}
		}
	}

	private void setScanCallback() throws NoSuchMethodException{
		mScanCallback = new ScanCallback()
		{
			@Override
			public void onScanResult(int callbackType, ScanResult result)
			{
				super.onScanResult(callbackType, result);

				Log.i("callbackType", String.valueOf(callbackType));
				Log.i("result", result.toString());
				BluetoothDevice btDevice = result.getDevice();

				if(!m_LeDevices.contains(btDevice))
				{
					m_LeDevices.add(btDevice);
					adapter.add(btDevice.getName() + "\n" + btDevice.getAddress());
				}
				else
				{
					return;
				}
			}

			@Override
			public void onBatchScanResults(List<ScanResult> results)
			{
				super.onBatchScanResults(results);

				for(ScanResult sr:results)
				{
					Log.i("ScanResult - Results", sr.toString());
				}
			}

			@Override
			public void onScanFailed(int errorCode)
			{
				super.onScanFailed(errorCode);

				Log.e("Scan Failed", "Error Code: " + errorCode);
			}
		};
	}


	public BluetoothAdapter.LeScanCallback m_LeScanCallback = new BluetoothAdapter.LeScanCallback()
	{
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if(!m_LeDevices.contains(device))
					{
						m_LeDevices.add(device);
						adapter.add(device.getName() + "\n" + device.getAddress());
					}
				}
			});
		}
	};

	Intent intent;

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		switch (position)
		{
			case 0: // drawText
				Intent intent = new Intent(MainActivity.this, DrawTextActivity.class);
				startActivity(intent);
				break;

			case 1: // drawVectorFontText
				intent = new Intent(MainActivity.this, DrawVectorTextActivity.class);
				startActivity(intent);
				break;

			case 2: // draw1dBarcode
				intent = new Intent(MainActivity.this, Draw1dBarcodeActivity.class);
				startActivity(intent);
				break;

			case 3: // drawMaxicode
				intent = new Intent(MainActivity.this, DrawMaxicodeActivity.class);
				startActivity(intent);
				break;

			case 4: // drawPdf417
				intent = new Intent(MainActivity.this, DrawPdf417Activity.class);
				startActivity(intent);
				break;

			case 5: // drawQrCode
				intent = new Intent(MainActivity.this, DrawQrCodeActivity.class);
				startActivity(intent);
				break;

			case 6: // drawDataMatrix
				intent = new Intent(MainActivity.this, DrawDataMatrixActivity.class);
				startActivity(intent);
				break;

			case 7: // drawBlock
				intent = new Intent(MainActivity.this, DrawBlockActivity.class);
				startActivity(intent);
				break;

			case 8: // drawCircle
				intent = new Intent(MainActivity.this, DrawCircleActivity.class);
				startActivity(intent);
				break;

			case 9: // setCharacterSet
				intent = new Intent(MainActivity.this, CharacterSetSelectionActivity.class);
				startActivity(intent);
				break;

			case 10: // setPrintingType
				DialogManager.showSetPrintingTypeDialog(MainActivity.this);
				break;

			case 11: // setMarginValue
				DialogManager.showSetMarginValueDialog(MainActivity.this);
				break;

			case 12: // setLabelLengthAndGap
				DialogManager.showSetLabelLengthAndGapDialog(MainActivity.this);
				break;

			case 13: // setLabelWidth
				DialogManager.showSetLabelWidthDialog(MainActivity.this);
				break;

			case 14: // setBufferMode
				DialogManager.showSetBufferModeDialog(MainActivity.this);
				break;

			case 15: // clearBuffer
				mBixolonLabelPrinter.clearBuffer();
				break;

			case 16: // setSpeed
				DialogManager.showSetSpeedDialog(MainActivity.this);
				break;

			case 17: // setDensity
				DialogManager.showSetDensityDialog(MainActivity.this);
				break;

			case 18: // setOrientation
				DialogManager.showSetOrientationDialog(MainActivity.this);
				break;

			case 19: // setOffsetBetweenBlackMark
				DialogManager.showSetOffsetBetweenBlackMarkDialog(MainActivity.this);
				break;

			case 20: // setCutterPosition
				DialogManager.showCutterPositionSettingDialog(MainActivity.this);
				break;

			case 21: // drawCompressionImage
				intent = new Intent(MainActivity.this, DrawBitmapActivity.class);
				startActivity(intent);
				break;

			case 22: // initializePrinter
				mBixolonLabelPrinter.initializePrinter();
				break;

			case 23: // printInformation
				mBixolonLabelPrinter.printInformation();
				break;

			case 24: // setAutoCutter
				DialogManager.showAutoCutterDialog(MainActivity.this);
				break;

			case 25: // getStatus
				mBixolonLabelPrinter.getStatus(true);
				break;

			case 26: // getPrinterInformation
				DialogManager.showPrinterInformationDialog(MainActivity.this, mBixolonLabelPrinter);
				break;

			case 27: // executeDirectIo
				intent = new Intent(MainActivity.this, DirectIoActivity.class);
				startActivity(intent);
				break;

			case 28: // Sample Receipt
				printSampleReceipt();
				break;

			case 29:
				intent = new Intent(MainActivity.this, DrawPDFActivity.class);
				startActivity(intent);
				break;

			case 30:
				DialogManager.showSetupRFID(MainActivity.this);
				break;
			case 31:
				mBixolonLabelPrinter.calibrateRFID();
				break;
			case 32:
				DialogManager.showSetRFIDPosition(MainActivity.this);
				break;
			case 33:
				DialogManager.showWriteRFID(MainActivity.this);
				break;
		}
	}

	private final void setStatus(int resId)
	{
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(resId);
	}

	private void printSampleReceipt()
	{
		mBixolonLabelPrinter.beginTransactionPrint();
		mBixolonLabelPrinter.drawText("75-C51", 50, 1200, BixolonLabelPrinter.FONT_SIZE_24, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, true, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Guri-si", 35, 900, BixolonLabelPrinter.FONT_SIZE_15, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Inchang-dong", 85, 900, BixolonLabelPrinter.FONT_SIZE_15, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);

		mBixolonLabelPrinter.drawText("DLVY 1026-1287-1927", 160, 1200, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Date   2017/12/31", 190, 1200, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.draw1dBarcode("123456789012", 160, 850, BixolonLabelPrinter.BARCODE_CODE128, 2, 10, 60, BixolonLabelPrinter.ROTATION_270_DEGREES, BixolonLabelPrinter.HRI_NOT_PRINTED, 0);

		mBixolonLabelPrinter.drawText("Bob      010-1234-5678", 230, 1200, BixolonLabelPrinter.FONT_SIZE_12, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Gyunggi-do Guri-si Inchang-dong 3912", 260, 1200, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Apt 204dong / 702ho", 290, 1200, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Nike Sport   02-468-4317", 330, 1200, BixolonLabelPrinter.FONT_SIZE_12, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Seoul-si Jonglo-gu Younji-dong 361-6", 360, 1200, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Nike mall 2F", 390, 1200, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Credit", 430, 1200, BixolonLabelPrinter.FONT_SIZE_12, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, true, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Airmax GX-100", 480, 1200, BixolonLabelPrinter.FONT_SIZE_12, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, true, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);

		mBixolonLabelPrinter.drawText("31-C1", 30, 600, BixolonLabelPrinter.FONT_SIZE_24, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, true, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Guri-si", 120, 600, BixolonLabelPrinter.FONT_SIZE_12, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Inchang-dong", 170, 600, BixolonLabelPrinter.FONT_SIZE_12, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("BIXOLON Apt 204dong / 702ho", 220, 600, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Bob  010-1234-5678", 300, 600, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("BIXOLON Apt 204dong 702ho", 430, 600, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Gyunggi-do Guri-si Inchang-dong 3912", 260, 600, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Gyunggi-do Guri-si Inchang-dong 3912", 400, 600, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);

		mBixolonLabelPrinter.drawText("DLVY 1026-1287-1927", 30, 400, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Date   2017/12/31", 70, 400, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);

		mBixolonLabelPrinter.drawText("Bob 010-1234-5678", 120, 400, BixolonLabelPrinter.FONT_SIZE_12, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);

		mBixolonLabelPrinter.drawText("Nike Sport", 300, 250, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("02-648-4317", 330, 250, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Airmax GX-100", 360, 250, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);

		mBixolonLabelPrinter.drawQrCode("www.bixolon.com", 500, 200, BixolonLabelPrinter.QR_CODE_MODEL2, BixolonLabelPrinter.ECC_LEVEL_15, 4, BixolonLabelPrinter.ROTATION_270_DEGREES);

		mBixolonLabelPrinter.print(1, 1);
		mBixolonLabelPrinter.endTransactionPrint();
	}

	private final void setStatus(CharSequence subtitle)
	{
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(subtitle);
	}

	@SuppressLint("HandlerLeak")
	private void dispatchMessage(Message msg)
	{
		switch (msg.arg1)
		{
			case BixolonLabelPrinter.PROCESS_GET_STATUS:
				byte[] report = (byte[]) msg.obj;
				StringBuffer buffer = new StringBuffer();
				if((report[0] & BixolonLabelPrinter.STATUS_1ST_BYTE_PAPER_EMPTY) == BixolonLabelPrinter.STATUS_1ST_BYTE_PAPER_EMPTY)
				{
					buffer.append("Paper Empty.\n");
				}
				if((report[0] & BixolonLabelPrinter.STATUS_1ST_BYTE_COVER_OPEN) == BixolonLabelPrinter.STATUS_1ST_BYTE_COVER_OPEN)
				{
					buffer.append("Cover open.\n");
				}
				if((report[0] & BixolonLabelPrinter.STATUS_1ST_BYTE_CUTTER_JAMMED) == BixolonLabelPrinter.STATUS_1ST_BYTE_CUTTER_JAMMED)
				{
					buffer.append("Cutter jammed.\n");
				}
				if((report[0] & BixolonLabelPrinter.STATUS_1ST_BYTE_TPH_OVERHEAT) == BixolonLabelPrinter.STATUS_1ST_BYTE_TPH_OVERHEAT)
				{
					buffer.append("TPH(thermal head) overheat.\n");
				}
				if((report[0] & BixolonLabelPrinter.STATUS_1ST_BYTE_AUTO_SENSING_FAILURE) == BixolonLabelPrinter.STATUS_1ST_BYTE_AUTO_SENSING_FAILURE)
				{
					buffer.append("Gap detection error. (Auto-sensing failure)\n");
				}
				if((report[0] & BixolonLabelPrinter.STATUS_1ST_BYTE_RIBBON_END_ERROR) == BixolonLabelPrinter.STATUS_1ST_BYTE_RIBBON_END_ERROR)
				{
					buffer.append("Ribbon end error.\n");
				}

				if(report.length == 2)
				{
					if((report[1] & BixolonLabelPrinter.STATUS_2ND_BYTE_BUILDING_IN_IMAGE_BUFFER) == BixolonLabelPrinter.STATUS_2ND_BYTE_BUILDING_IN_IMAGE_BUFFER)
					{
						buffer.append("On building label to be printed in image buffer.\n");
					}
					if((report[1] & BixolonLabelPrinter.STATUS_2ND_BYTE_PRINTING_IN_IMAGE_BUFFER) == BixolonLabelPrinter.STATUS_2ND_BYTE_PRINTING_IN_IMAGE_BUFFER)
					{
						buffer.append("On printing label in image buffer.\n");
					}
					if((report[1] & BixolonLabelPrinter.STATUS_2ND_BYTE_PAUSED_IN_PEELER_UNIT) == BixolonLabelPrinter.STATUS_2ND_BYTE_PAUSED_IN_PEELER_UNIT)
					{
						buffer.append("Issued label is paused in peeler unit.\n");
					}
				}
				if(buffer.length() == 0)
				{
					buffer.append("No error");
				}
				Toast.makeText(getApplicationContext(), buffer.toString(), Toast.LENGTH_SHORT).show();
				break;
			case BixolonLabelPrinter.PROCESS_GET_INFORMATION_MODEL_NAME:
			case BixolonLabelPrinter.PROCESS_GET_INFORMATION_FIRMWARE_VERSION:
			case BixolonLabelPrinter.PROCESS_EXECUTE_DIRECT_IO:
				Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
				break;
			case BixolonLabelPrinter.PROCESS_OUTPUT_COMPLETE:
				Toast.makeText(getApplicationContext(), "Output Complete", Toast.LENGTH_SHORT).show();
				break;
		}
	}

	private final Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
				case BixolonLabelPrinter.MESSAGE_STATE_CHANGE:
					switch (msg.arg1)
					{
						case BixolonLabelPrinter.STATE_CONNECTED:
							setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
							mListView.setEnabled(true);
							mIsConnected = true;
							invalidateOptionsMenu();
							break;

						case BixolonLabelPrinter.STATE_CONNECTING:
							setStatus(R.string.title_connecting);
							break;

						case BixolonLabelPrinter.STATE_NONE:
							Log.e("NONE", msg.toString());
							setStatus(R.string.title_not_connected);
							//mListView.setEnabled(false);
							mIsConnected = false;
							invalidateOptionsMenu();
							break;
					}
					break;

				case BixolonLabelPrinter.MESSAGE_READ:
					MainActivity.this.dispatchMessage(msg);
					break;

				case BixolonLabelPrinter.MESSAGE_DEVICE_NAME:
					mConnectedDeviceName = msg.getData().getString(BixolonLabelPrinter.DEVICE_NAME);
					Toast.makeText(getApplicationContext(), mConnectedDeviceName, Toast.LENGTH_LONG).show();
					break;

				case BixolonLabelPrinter.MESSAGE_TOAST:
					mListView.setEnabled(false);
					Toast.makeText(getApplicationContext(), msg.getData().getString(BixolonLabelPrinter.TOAST), Toast.LENGTH_SHORT).show();
					break;

				case BixolonLabelPrinter.MESSAGE_LOG:
					Toast.makeText(getApplicationContext(), msg.getData().getString(BixolonLabelPrinter.LOG), Toast.LENGTH_SHORT).show();
					break;

				case BixolonLabelPrinter.MESSAGE_BLUETOOTH_DEVICE_SET:
					if(msg.obj == null)
					{
						Toast.makeText(getApplicationContext(), "No paired device", Toast.LENGTH_SHORT).show();
					}
					else
					{
						DialogManager.showBluetoothDialog(MainActivity.this, (Set<BluetoothDevice>) msg.obj);
					}
					break;

				case BixolonLabelPrinter.MESSAGE_USB_DEVICE_SET:
					if(msg.obj == null)
					{
						Toast.makeText(getApplicationContext(), "No connected device", Toast.LENGTH_SHORT).show();
					}
					else
					{
						DialogManager.showUsbDialog(MainActivity.this, (Set<UsbDevice>) msg.obj, mUsbReceiver);
					}
					break;

				case BixolonLabelPrinter.MESSAGE_NETWORK_DEVICE_SET:
					if(msg.obj == null)
					{
						Toast.makeText(getApplicationContext(), "No connectable device", Toast.LENGTH_SHORT).show();
					}
					DialogManager.showNetworkDialog(MainActivity.this, msg.obj.toString());
					break;
			}
		}
	};


	private BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
	{

		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();

			if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
			{
				if(mBixolonLabelPrinter != null && !mBixolonLabelPrinter.isConnected()) {
					isConnectedPrinter();
					Toast.makeText(getApplicationContext(), "Found USB device", Toast.LENGTH_SHORT).show();
				}

			}
			else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
			{
				mBixolonLabelPrinter.disconnect();
				Toast.makeText(getApplicationContext(), "USB device removed", Toast.LENGTH_SHORT).show();
			} else if(ACTION_USB_PERMISSION.equals(action)){
				if(device != null) {
					mBixolonLabelPrinter.connect(device);
					device = null;
				}
			}
		}
	};

	private void isConnectedPrinter(){
		HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
		if(!tryedAutoConnect && deviceList.size() > 0) {
			mBixolonLabelPrinter.findUsbPrinters();
		}
	}

	public void checkVerify()
	{
		int result = 0;
		ArrayList<String> permissionList = new ArrayList<String>();

		for(int i = 0; i < permissions.length; i++)
		{
			result = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[i]);
			if(result != PackageManager.PERMISSION_GRANTED) {
				permissionList.add(permissions[i]);
			}
		}

		if(permissionList.size() > 0)
		{
			String[] reqPermissionArray = new String[permissionList.size()];
			reqPermissionArray = permissionList.toArray(reqPermissionArray);
			requestPermissions(reqPermissionArray, REQUEST_PERMISSION);
		}
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == REQUEST_PERMISSION)
		{
			if (grantResults.length > 0)
			{
				for (int i=0; i<grantResults.length; ++i)
				{
					if (grantResults[i] == PackageManager.PERMISSION_DENIED)
					{
						new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.notice)).setMessage(getResources().getString(R.string.grant_permission))
								.setPositiveButton(getResources().getString(R.string.exit), new DialogInterface.OnClickListener(){
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
										finish();
									}
								}).setNegativeButton(getResources().getString(R.string.set_permission), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
										.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
								getApplicationContext().startActivity(intent);
							}
						}).setCancelable(false).show();
						return;
					}
				}
			}
		}
	}

    public class SampleUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler
    {
        @Override
        public void uncaughtException(Thread thread, final Throwable ex)
        {
            // 여기서 에러를 처리
            finish();
        }
    }
}
