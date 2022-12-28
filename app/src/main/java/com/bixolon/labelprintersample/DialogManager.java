package com.bixolon.labelprintersample;

import java.util.Iterator;
import java.util.Set;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.Toast;

import com.bixolon.labelprinter.BixolonLabelPrinter;
import com.bixolon.labelprintersample.R;

import org.json.JSONException;
import org.json.JSONObject;

public class DialogManager {
	
	static void showBluetoothDialog(Context context, final Set<BluetoothDevice> pairedDevices) {
		final String[] items = new String[pairedDevices.size()];
		int index = 0;
		for (BluetoothDevice device : pairedDevices) {
			items[index++] = device.getName() + "\n" + device.getAddress();
		}

		new AlertDialog.Builder(context).setTitle("Paired Bluetooth printers")
				.setItems(items, new OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						
						String strSelectList = items[which];
						String temp;
						int indexSpace = 0;
						for(int i = 5; i<strSelectList.length(); i++){
							temp = strSelectList.substring(i-5, i);
							if((temp.equals("00:10"))||(temp.equals("74:F0"))||(temp.equals("00:15")) || (temp.equals("DD:C5")) || (temp.equals("40:19"))){
								indexSpace = i;
								i = 100;
							}
						}
						String strDeviceInfo = null;
						strDeviceInfo = strSelectList.substring(indexSpace-5, strSelectList.length());

						MainActivity.mBixolonLabelPrinter.connect(strDeviceInfo);
						
					}
				}).show();
	}
	
	static void showUsbDialog(final Context context, final Set<UsbDevice> usbDevices, final BroadcastReceiver usbReceiver) {
		final String[] items = new String[usbDevices.size()];
		int index = 0;
		for (UsbDevice device : usbDevices) {
			items[index++] = "Device name: " + device.getProductName() + ", Product ID: " + device.getProductId() + ", Device ID: " + device.getDeviceId();
		}

		new AlertDialog.Builder(context).setTitle("Connected USB printers")
				.setItems(items, new OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						MainActivity.mBixolonLabelPrinter.connect((UsbDevice) usbDevices.toArray()[which]);

						// listen for new devices
						IntentFilter filter = new IntentFilter();
						filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
						filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
						context.registerReceiver(usbReceiver, filter);
					}
				}).show();
	}

	static void showNetworkDialog(Context context, String list) {
		if (list != null && list.length()>0) {
			try {
				if (list != null && list.length() > 0) {
					int j = 0;
					for (int i = list.length() - 1; i >= 0; i--) {
						char ch = list.charAt(i);
						if (ch == '{') {
							String pre = list.substring(0, i);
							String post = list.substring(i);
							String ins = "printer" + j + ":";
							list = pre + ins + post;
							j++;
						} else {
							String pre = list.substring(0, i);
							String post = list.substring(i);
							String ins = "printer" + j + ":";
						}
					}
				}
				list = "{" + list + "}";
				JSONObject jsonObject = new JSONObject(list);
				Iterator<String> tempGroupKey = jsonObject.keys();


				int i = 0;
				String address = "", port = "";
				final String[] items = new String[jsonObject.length()];

				while (tempGroupKey.hasNext()) {
					String grpKey = tempGroupKey.next();

					JSONObject obj = new JSONObject(jsonObject.get(grpKey).toString());
					Iterator<String> tempChildKey = obj.keys();
					while (tempChildKey.hasNext()) {
						String key = tempChildKey.next();
						if (key.equals("address")) {
							address = obj.getString(key);
						} else if (key.equals("portNumber")) {
							port = obj.getString(key);

							items[i++] = address + ":" + port;
						}
					}
				}
				new AlertDialog.Builder(context).setTitle("Connectable network printers")
						.setItems(items, new OnClickListener() {

							public void onClick(DialogInterface dialog, int which) {
								MainActivity.mBixolonLabelPrinter.connect(items[which].split(":")[0], Integer.valueOf(items[which].split(":")[1]), 5000);
							}
						}).show();
			}catch(JSONException e){
				e.printStackTrace();
			}


		}
	}
	
	static void showWifiDialog(Context context, final BixolonLabelPrinter printer) {
		AlertDialog dialog = null;
		if (dialog == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View layout = inflater.inflate(R.layout.dialog_wifi, null);

			dialog = new AlertDialog.Builder(context).setView(layout).setTitle("Wi-Fi Connect")
					.setPositiveButton("OK", new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							EditText editText = (EditText) layout.findViewById(R.id.editText1);
							String ip = editText.getText().toString();

							editText = (EditText) layout.findViewById(R.id.editText2);
							int port = Integer.parseInt(editText.getText().toString());

							printer.connect(ip, port, 5000);
						}
					}).setNegativeButton("Cancel", new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub

						}
					}).create();
		}
		dialog.show();
	}

	static void showPrinterInformationDialog(Context context, final BixolonLabelPrinter printer) {
		AlertDialog dialog = null;
		if (dialog == null) {
			final CharSequence[] ITEMS = {
					"Model name",
					"Firmware version"
			};
			dialog = new AlertDialog.Builder(context).setTitle("Get printer ID")
					.setItems(ITEMS, new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							
							if (which == 0) {
								printer.getPrinterInformation(BixolonLabelPrinter.PRINTER_INFORMATION_MODEL_NAME);
							} else if (which == 1) {
								printer.getPrinterInformation(BixolonLabelPrinter.PRINTER_INFORMATION_FIRMWARE_VERSION);
							}

						}
					}).create();

		}
		dialog.show();
	}
	
	private static int mmCheckedItem = 0;
	static void showSetPrintingTypeDialog(Context context) {
		AlertDialog dialog = null;
		mmCheckedItem = 0;
		
		if (dialog == null) {
			final CharSequence[] ITEMS = {
					"Direct thermal",
					"Thermal transter"
			};
			dialog = new AlertDialog.Builder(context).setTitle(R.string.set_printing_type)
					.setSingleChoiceItems(ITEMS, 0, new OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							mmCheckedItem = which;
						}
					}).setPositiveButton("OK", new OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							int type = BixolonLabelPrinter.PRINTING_TYPE_DIRECT_THERMAL;
							if (mmCheckedItem == 1) {
								type = BixolonLabelPrinter.PRINTING_TYPE_THERMAL_TRANSFER;
							}
							MainActivity.mBixolonLabelPrinter.setPrintingType(type);
						}
					}).setNegativeButton("Cancel", new OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							
						}
					}).create();
		}
		dialog.show();
	}
	
	static void showSetMarginValueDialog(Context context) {
		AlertDialog dialog = null;
		if (dialog == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View view = inflater.inflate(R.layout.dialog_set_margin, null);
			
			dialog = new AlertDialog.Builder(context).setTitle(R.string.set_margin).setView(view)
					.setPositiveButton("OK", new OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							EditText editText = (EditText) view.findViewById(R.id.editText1);
							if(editText.getText().toString().equals("")){
								editText.setText("0");
							}
							int horizontalMargin = Integer.parseInt(editText.getText().toString());
							editText = (EditText) view.findViewById(R.id.editText2);
							if(editText.getText().toString().equals("")){
								editText.setText("0");
							}
							int verticalMargin = Integer.parseInt(editText.getText().toString());
							MainActivity.mBixolonLabelPrinter.setMargin(horizontalMargin, verticalMargin);
						}
					}).setNegativeButton("Cancel", new OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							
						}
					}).create();
		}
		dialog.show();
	}
	
	static void showSetLabelWidthDialog(Context context) {
		AlertDialog dialog = null;
		if (dialog == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View view = inflater.inflate(R.layout.dialog_set_width, null);
			
			dialog = new AlertDialog.Builder(context).setTitle(R.string.set_width).setView(view)
					.setPositiveButton("OK", new OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							EditText editText = (EditText) view.findViewById(R.id.editText1);
							String string = editText.getText().toString();
							
							if (string != null && string.length() > 0) {
								int labelWidth = Integer.parseInt(string);
								MainActivity.mBixolonLabelPrinter.setWidth(labelWidth);
							}
						}
					}).setNegativeButton("Cancel", new OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							
						}
					}).create();
		}
		dialog.show();
	}
	
	static void showSetLabelLengthAndGapDialog(final Context context) {
		AlertDialog dialog = null;
		if (dialog == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View view = inflater.inflate(R.layout.dialog_set_length, null);
			
			dialog = new AlertDialog.Builder(context).setTitle(R.string.set_length).setView(view)
					.setPositiveButton("OK", new OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							try {
								EditText editText = (EditText) view.findViewById(R.id.editText1);
								String string1 = editText.getText().toString();
								int labelLength = Integer.parseInt(editText.getText().toString());

								editText = (EditText) view.findViewById(R.id.editText2);
								String string2 = editText.getText().toString();
								int gapLength = Integer.parseInt(editText.getText().toString());
								if (string1 != null && string1.length() > 0 && string2 != null && string2.length() > 0) {

									int mediaType = BixolonLabelPrinter.MEDIA_TYPE_GAP;
									RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup1);
									switch (radioGroup.getCheckedRadioButtonId()) {
										case R.id.radio0:
											mediaType = BixolonLabelPrinter.MEDIA_TYPE_GAP;
											break;
										case R.id.radio1:
											mediaType = BixolonLabelPrinter.MEDIA_TYPE_CONTINUOUS;
											break;
										case R.id.radio2:
											mediaType = BixolonLabelPrinter.MEDIA_TYPE_BLACK_MARK;
											break;
									}

									int offsetLength = 0;
									editText = (EditText) view.findViewById(R.id.editText3);
									String string = editText.getText().toString();
									if (string != null && string.length() > 0) {
										offsetLength = Integer.parseInt(string);
									}
									MainActivity.mBixolonLabelPrinter.setLength(labelLength, gapLength, mediaType, offsetLength);
								}
							}catch(Exception e){
								Toast.makeText(context, "please input data", Toast.LENGTH_SHORT).show();
							}
						}
					}).setNegativeButton("Cancel", new OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							
						}
					}).create();
		}
		dialog.show();
	}
	
	static void showSetBufferModeDialog(Context context) {
		AlertDialog dialog = null;
		mmCheckedItem = 0;
		if (dialog == null) {
			dialog = new AlertDialog.Builder(context).setTitle("Set buffer mode")
						.setSingleChoiceItems(new String[] {"false", "true"}, 0, new OnClickListener() {
							
							public void onClick(DialogInterface arg0, int arg1) {
								mmCheckedItem = arg1;
							}
						}).setPositiveButton("OK", new OnClickListener() {
							
							public void onClick(DialogInterface dialog, int which) {
								MainActivity.mBixolonLabelPrinter.setBufferMode(mmCheckedItem == 1);
							}
						}).setNegativeButton("Cancel", new OnClickListener() {
							
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								
							}
						}).create();
		}
		dialog.show();


		

	}
	static void showSetSpeedDialog(Context context) {
		AlertDialog dialog = null;
		mmCheckedItem = 0;
		if (dialog == null) {
			String[] items = {
					"2.5 ips",
					"3.0 ips",
					"4.0 ips",
					"5.0 ips",
					"6.0 ips",
					"7.0 ips",
					"8.0 ips"
			};
			dialog = new AlertDialog.Builder(context).setTitle("Set speed")
						.setSingleChoiceItems(items, 0, new OnClickListener() {
							
							public void onClick(DialogInterface arg0, int arg1) {
								mmCheckedItem = arg1;
								
							}
						}).setPositiveButton("OK", new OnClickListener() {
							
							public void onClick(DialogInterface dialog, int which) {
								switch (mmCheckedItem) {
								case 0:
									MainActivity.mBixolonLabelPrinter.setSpeed(BixolonLabelPrinter.SPEED_25IPS);
									break;
								case 1:
									MainActivity.mBixolonLabelPrinter.setSpeed(BixolonLabelPrinter.SPEED_30IPS);
									break;
								case 2:
									MainActivity.mBixolonLabelPrinter.setSpeed(BixolonLabelPrinter.SPEED_40IPS);
									break;
								case 3:
									MainActivity.mBixolonLabelPrinter.setSpeed(BixolonLabelPrinter.SPEED_50IPS);
									break;
								case 4:
									MainActivity.mBixolonLabelPrinter.setSpeed(BixolonLabelPrinter.SPEED_60IPS);
									break;
								case 5:
									MainActivity.mBixolonLabelPrinter.setSpeed(BixolonLabelPrinter.SPEED_70IPS);
									break;
								case 6:
									MainActivity.mBixolonLabelPrinter.setSpeed(BixolonLabelPrinter.SPEED_80IPS);
									break;
								}
							}
						}).setNegativeButton("Cancel", new OnClickListener() {
							
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								
							}
						}).create();
		}
		dialog.show();
	}
	
	static void showSetDensityDialog(final Context context) {
		AlertDialog dialog = null;
		if (dialog == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View view = inflater.inflate(R.layout.dialog_set_density, null);
			
			dialog = new AlertDialog.Builder(context).setTitle(R.string.set_density).setView(view)
						.setPositiveButton("OK", new OnClickListener() {
							
							public void onClick(DialogInterface dialog, int which) {
								try{
									EditText editText = (EditText) view.findViewById(R.id.editText1);
									int density = Integer.parseInt(editText.getText().toString());
									MainActivity.mBixolonLabelPrinter.setDensity(density);
								}catch(Exception e){
									Toast.makeText(context, "please input data", Toast.LENGTH_SHORT).show();
								}
							}
						}).setNegativeButton("Cancel", new OnClickListener() {
							
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								
							}
						}).create();
		}
		dialog.show();
	}
	
	static void showSetOrientationDialog(Context context) {
		AlertDialog dialog = null;
		mmCheckedItem = 0;
		if (dialog == null) {
			dialog = new AlertDialog.Builder(context).setTitle("Set orientation")
						.setSingleChoiceItems(new String[] {"Print from top to bottom (default)", "Print from bottom to top"}, 0, new OnClickListener() {
							
							public void onClick(DialogInterface arg0, int arg1) {
								mmCheckedItem = arg1;
								
							}
						}).setPositiveButton("OK", new OnClickListener() {
							
							public void onClick(DialogInterface dialog, int which) {
								if (mmCheckedItem == 0) {
									MainActivity.mBixolonLabelPrinter.setOrientation(BixolonLabelPrinter.ORIENTATION_TOP_TO_BOTTOM);
								} else {
									MainActivity.mBixolonLabelPrinter.setOrientation(BixolonLabelPrinter.ORIENTATION_BOTTOM_TO_TOP);
								}
							}
						}).setNegativeButton("Cancel", new OnClickListener() {
							
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								
							}
						}).create();
		}
		dialog.show();
	}
	
	static void showSetOffsetBetweenBlackMarkDialog(final Context context) {
		AlertDialog dialog = null;
		if (dialog == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View view = inflater.inflate(R.layout.dialog_set_offset, null);
			
			dialog = new AlertDialog.Builder(context).setTitle(R.string.set_offset).setView(view)
						.setPositiveButton("OK", new OnClickListener() {
							
							public void onClick(DialogInterface dialog, int which) {
								try{
									EditText editText = (EditText) view.findViewById(R.id.editText1);
									int offset = Integer.parseInt(editText.getText().toString());
									MainActivity.mBixolonLabelPrinter.setOffset(offset);
								}catch(Exception e){
									Toast.makeText(context, "please input data", Toast.LENGTH_SHORT).show();
								}
							}
						}).setNegativeButton("Cancel", new OnClickListener() {
							
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								
							}
						}).create();
		}
		dialog.show();
	}
	
	static void showCutterPositionSettingDialog(final Context context) {
		AlertDialog dialog = null;
		if (dialog == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View view = inflater.inflate(R.layout.dialog_set_offset, null);
			
			dialog = new AlertDialog.Builder(context).setTitle("Cutter position setting").setView(view)
						.setPositiveButton("OK", new OnClickListener() {
							
							public void onClick(DialogInterface dialog, int which) {
								try{
									EditText editText = (EditText) view.findViewById(R.id.editText1);
									int position = Integer.parseInt(editText.getText().toString());
									MainActivity.mBixolonLabelPrinter.setCutterPosition(position);
								}catch(Exception e){
									Toast.makeText(context, "please input data", Toast.LENGTH_SHORT).show();
								}
							}
						}).setNegativeButton("Cancel", new OnClickListener() {
							
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								
							}
						}).create();
		}
		dialog.show();
	}
	
	static void showAutoCutterDialog(final Context context) {
		AlertDialog dialog = null;
		if (dialog == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(R.layout.dialog_set_auto_cutter, null);
			
			final EditText editText = (EditText) view.findViewById(R.id.editText1);
			
			final RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup1);
			radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					editText.setEnabled(checkedId == R.id.radio0);
				}
			});
			
			dialog = new AlertDialog.Builder(context).setTitle(R.string.cutting_action).setView(view)
						.setPositiveButton("OK", new OnClickListener() {
							
							public void onClick(DialogInterface dialog, int which) {
								try{
									int cuttingPeriod = Integer.parseInt(editText.getText().toString());
									MainActivity.mBixolonLabelPrinter.setAutoCutter(radioGroup.getCheckedRadioButtonId() == R.id.radio0, cuttingPeriod);
								}catch(Exception e){
									Toast.makeText(context, "please input data", Toast.LENGTH_SHORT).show();
								}
							}
						}).setNegativeButton("Cancel", new OnClickListener() {
							
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								
							}
						}).create();
		}
		dialog.show();
	}

	static void showSetupRFID(final Context context){
		AlertDialog dialog = null;
		if (dialog == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View view = inflater.inflate(R.layout.dialog_setup_rfid, null);
			final Spinner spinRfidType = view.findViewById(R.id.spinRfidType);
			ArrayAdapter<String> adapter=new ArrayAdapter<>(context, R.layout.layout_textview, context.getResources().getStringArray(R.array.arr_rfid_transponder_type));
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinRfidType.setAdapter(adapter);

			dialog = new AlertDialog.Builder(context).setTitle("Setup RFID").setView(view)
					.setPositiveButton("OK", new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							try{
								int rfidType=0;
								int numberOfRetries=0;
								int numberOfLabel=0;
								int radioPower=0;


								rfidType = spinRfidType.getSelectedItemPosition();

								EditText editText = view.findViewById(R.id.editText2);
								numberOfRetries = Integer.parseInt(editText.getText().toString());

								editText = view.findViewById(R.id.editText3);
								numberOfLabel = Integer.parseInt(editText.getText().toString());

								editText = view.findViewById(R.id.editText4);
								radioPower = Integer.parseInt(editText.getText().toString());

								MainActivity.mBixolonLabelPrinter.setupRFID(rfidType, numberOfRetries, numberOfLabel, radioPower);
							}catch(Exception e){
								Toast.makeText(context, "please input data", Toast.LENGTH_SHORT).show();
							}
						}
					}).setNegativeButton("Cancel", new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub

						}
					}).create();

		}
		dialog.show();
	}

	static void showSetRFIDPosition(final Context context){
		AlertDialog dialog = null;
		if (dialog == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View view = inflater.inflate(R.layout.dialog_position_rfid, null);


			dialog = new AlertDialog.Builder(context).setTitle("Set RFID Position").setView(view)
					.setPositiveButton("OK", new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							try{
								int transPosition=0;

								EditText editText = view.findViewById(R.id.editText1);
								transPosition = Integer.parseInt(editText.getText().toString());

								MainActivity.mBixolonLabelPrinter.setRFIDPosition(transPosition);
							}catch(Exception e){
								Toast.makeText(context, "please input data", Toast.LENGTH_SHORT).show();
							}
						}
					}).setNegativeButton("Cancel", new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub

						}
					}).create();



		}
		dialog.show();


	}

	static void showWriteRFID(final Context context){
		AlertDialog dialog = null;
		if (dialog == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View view = inflater.inflate(R.layout.dialog_write_rfid, null);

			final CheckBox ckLockRFID = view.findViewById(R.id.ckLockRFID);

			final Spinner spinDataType = view.findViewById(R.id.spinDataType);
			ArrayAdapter<String> adapter=new ArrayAdapter<>(context, R.layout.layout_textview, context.getResources().getStringArray(R.array.arr_rfid_data_type));
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinDataType.setAdapter(adapter);

			ckLockRFID.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(isChecked){
						view.findViewById(R.id.layoutPassword).setVisibility(View.VISIBLE);
					}else{
						view.findViewById(R.id.layoutPassword).setVisibility(View.GONE);
					}
				}
			});

			dialog = new AlertDialog.Builder(context).setTitle("Write RFID").setView(view)
					.setPositiveButton("OK", new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {

							try{
								MainActivity.mBixolonLabelPrinter.drawText("RFID Test Print", 50, 50, BixolonLabelPrinter.FONT_SIZE_18,
										1, 1, 0, 0, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
								int totalSize = 0;
								String fieldSizes = "";
								int dataType = 0;
								int startingBlockNumber = 4;
								int dataLength = 12;
								String data = "";
								String text = "";
								EditText editText = null;
								switch(spinDataType.getSelectedItemPosition()){
									case 0:
										dataType = 'A';
										break;
									case 1:
										dataType = 'H';
										break;
									case 2:
										dataType = 'E';
										break;
									case 3:
										dataType = 'U';
										break;
								}

								//rfid lock check
								if(dataType == 'E' && ckLockRFID.isChecked()){
									//if lock checked then set password
									String oldAccessPW="";
									String oldKillPW="";
									String newAccessPW="";
									String newKillPW="";
									editText = view.findViewById(R.id.etOldAccessPW);
									oldAccessPW = editText.getText().toString();
									editText = view.findViewById(R.id.etOldKillPW);
									oldKillPW = editText.getText().toString();
									editText = view.findViewById(R.id.etNewAccessPW);
									newAccessPW = editText.getText().toString();
									editText = view.findViewById(R.id.etNewKillPW);
									newKillPW = editText.getText().toString();
									MainActivity.mBixolonLabelPrinter.setRFIDPassword(oldAccessPW, oldKillPW, newAccessPW, newKillPW);
								}

								if(dataType == 'E') {
									//set epc data structure
									editText = view.findViewById(R.id.editText1);
									totalSize = Integer.parseInt(editText.getText().toString());

									editText = view.findViewById(R.id.editText2);
									fieldSizes = editText.getText().toString();

									MainActivity.mBixolonLabelPrinter.setEPCDataStructure(totalSize, fieldSizes);
								}else{
									editText = view.findViewById(R.id.editText4);
									startingBlockNumber = Integer.parseInt(editText.getText().toString());

									editText = view.findViewById(R.id.editText5);
									dataLength = Integer.parseInt(editText.getText().toString());
								}

								editText = view.findViewById(R.id.editText6);
								data = editText.getText().toString();

								MainActivity.mBixolonLabelPrinter.writeRFID(dataType, startingBlockNumber, dataLength, data);

								if(ckLockRFID.isChecked()) {
									MainActivity.mBixolonLabelPrinter.lockRFID();
								}

								MainActivity.mBixolonLabelPrinter.print(1,1);
							}catch(Exception e){
								Toast.makeText(context, "please input data", Toast.LENGTH_SHORT).show();
							}
						}
					}).setNegativeButton("Cancel", new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub

						}
					}).create();

		}
		dialog.show();

	}

	static void showSetRFIDPassword(final Context context){
		AlertDialog dialog = null;
		if (dialog == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View view = inflater.inflate(R.layout.dialog_set_rfid_password, null);

			dialog = new AlertDialog.Builder(context).setTitle("Set RFID Password").setView(view)
					.setPositiveButton("OK", new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							try{
								String oldAccessPwd = "";
								String oldKillPwd = "";
								String newAccessPwd = "";
								String newKillPwd = "";

								EditText editText = view.findViewById(R.id.editText1);
								oldAccessPwd = editText.getText().toString();

								editText = view.findViewById(R.id.editText2);
								oldKillPwd = editText.getText().toString();

								editText = view.findViewById(R.id.editText3);
								newAccessPwd = editText.getText().toString();

								editText = view.findViewById(R.id.editText4);
								newKillPwd = editText.getText().toString();

								MainActivity.mBixolonLabelPrinter.setRFIDPassword(oldAccessPwd, oldKillPwd, newAccessPwd, newKillPwd);
							}catch(Exception e){
								Toast.makeText(context, "please input data", Toast.LENGTH_SHORT).show();
							}
						}
					}).setNegativeButton("Cancel", new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub

						}
					}).create();



		}
		dialog.show();
	}

	static void showSetEPCDataStructure(final Context context){
		AlertDialog dialog = null;
		if (dialog == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View view = inflater.inflate(R.layout.dialog_set_epc_data_structure, null);


			dialog = new AlertDialog.Builder(context).setTitle("Set EPC Data Structure").setView(view)
					.setPositiveButton("OK", new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							try{
								int totalSize=0;
								String fieldSizes="";

								EditText editText = view.findViewById(R.id.editText1);
								totalSize = Integer.parseInt(editText.getText().toString());

								editText = view.findViewById(R.id.editText2);
								fieldSizes = editText.getText().toString();

								MainActivity.mBixolonLabelPrinter.setEPCDataStructure(totalSize, fieldSizes);
							}catch(Exception e){
								Toast.makeText(context, "please input data", Toast.LENGTH_SHORT).show();
							}
						}
					}).setNegativeButton("Cancel", new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub

						}
					}).create();


		}
		dialog.show();

	}
}
