package com.miittech.you.ble.update;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.miittech.you.ble.BleClient;
import com.miittech.you.ble.BleUUIDS;
import com.miittech.you.ble.gatt.BaseOptionCallback;
import com.miittech.you.ble.gatt.BleNotifyCallback;
import com.miittech.you.ble.gatt.BleWriteCallback;
import com.miittech.you.dialog.DialogUtils;
import com.miittech.you.impl.OnMsgTipOptions;
import com.ryon.mutils.LogUtils;

import java.util.ArrayDeque;
import java.util.Date;
import java.util.HashMap;
import java.util.Queue;

import static android.content.ContentValues.TAG;

public class OtaOptions {
	static final String TAG = "OtaOptions";
	private Context context;
	private UpdateFile updateFile;
	private BluetoothGatt bluetoothGatt;
	private IOtaUpdateListener iOtaUpdateListener;
	private String mac;
	public static final int MEMORY_TYPE_EXTERNAL_SPI = 0x13;
	//const
    private static final int fileBlockSize = 240;
	public static final int END_SIGNAL = 0xfe000000;
	public static final int REBOOT_SIGNAL = 0xfd000000;

	// SUOTA
	int imageBank= 0x0;
	int miso_gpio = 0x05;
	int mosi_gpio = 0x07;
	int gs_gpio = 0x03;
	int sck_gpio = 0x00;

	HashMap errors;

	boolean lastBlock = false;
	boolean lastBlockSent = false;
	boolean preparedForLastBlock = false;
	boolean endSignalSent = false;
	boolean rebootsignalSent = false;
	boolean finished = false;
	boolean hasError = false;
	boolean refreshPending;
	public int type;
	private int memDevValue=-1;
	int blockCounter = 0;
	int chunkCounter = -1;
	int gpioMapPrereq = 0;
	long uploadStart;
	PowerManager.WakeLock wakeLock;

	public Queue characteristicsQueue;


	public void init(String path,String mac) throws Exception {
    	this.mac = mac;
    	if(bluetoothGatt==null) {
			bluetoothGatt = BleClient.getInstance().getBluetoothGatt(this.mac);
		}else{
    		throw new Exception("gatt is null");
		}
		this.updateFile = UpdateFile.getByFilePath(path);
	}

	public void startUpdate(IOtaUpdateListener iOtaUpdateListener) {
		this.iOtaUpdateListener=iOtaUpdateListener;
		this.updateFile.setFileBlockSize(fileBlockSize);
		processStep(1);
	}
	public void processStep(int step) {
		if(memDevValue >= 0) {
			String stringValue = String.format("%#10x", memDevValue);
			LogUtils.d(TAG, "processMemDevValue() step: " + step + ", value: " + stringValue);
			if(step==2){
				if (memDevValue == 0x1) {
					LogUtils.d(TAG,"Set SPOTA_MEM_DEV: 0x1");
					processStep(3);
				}
			}
		}
		// If a step is set, change the global step to this value
		if (step < 0) {
			readNextCharacteristic();
		}
		Log.d(TAG, "step " + step);
		switch (step) {
			// Enable notifications
			case 1:
				if (Build.VERSION.SDK_INT >= 21) {
					Log.d(TAG, "Connection parameters update request (high)");

					bluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
				}
				BleClient.getInstance().notify(mac,BleUUIDS.SPOTA_SERV_STATUS_UUID, new BleNotifyCallback() {
					@Override
					public void onOptionSucess() {
						processStep(2);
					}

					@Override
					public void notifyDate(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
						super.notifyDate(gatt, characteristic);
						int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
						LogUtils.d(TAG, String.format("SPOTA_SERV_STATUS notification: %#04x", value));

						int step = -1;
						int error = -1;
						int memDevValue = -1;
						// Set memtype callback
						if (value == 0x10) {
							step = 3;
						}
						// Successfully sent a block, send the next one
						else if (value == 0x02) {
							step=5;
						} else if (value == 0x03 || value == 0x01) {
							memDevValue = value;
						} else {
							error = value;
						}
						if (step >= 0 || error >= 0 || memDevValue >= 0) {
							processStep(step);
						}
					}
				});
				break;
			// Init mem type
			case 2:
				if(iOtaUpdateListener!=null){
					iOtaUpdateListener.updateTitle("正在加载镜像文件...");
				}
				LogUtils.d(TAG,String.format("Firmware CRC: %#04x", updateFile.getCrc() & 0xff));
				String fwSizeMsg = "Upload size: " + updateFile.getNumberOfBytes() + " bytes";
				LogUtils.d(TAG, fwSizeMsg);
				// Acquire wake lock to keep CPU running during upload procedure
				LogUtils.d(TAG, "Acquire wake lock");
				PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
				wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SUOTA");
				wakeLock.acquire();
				uploadStart = new Date().getTime();

				int memType = (MEMORY_TYPE_EXTERNAL_SPI << 24) | imageBank;
				BleClient.getInstance().write(
						mac,BleUUIDS.SPOTA_SERVICE_UUID,
						BleUUIDS.SPOTA_MEM_DEV_UUID,
						memType,
						BluetoothGattCharacteristic.FORMAT_UINT32,
						0,
						new BleWriteCallback(){
							@Override
							public void onOptionSucess() {
								super.onOptionSucess();
								processStep(3);
							}
						});

				LogUtils.d(TAG, "setSpotaMemDev: " + String.format("%#10x", memType));
				LogUtils.d(TAG,"Set SPOTA_MEM_DEV: " + String.format("%#10x", memType));
				break;
			// Set mem_type for SPOTA_GPIO_MAP_UUID
			case 3:
				// After setting SPOTAR_MEM_DEV and SPOTAR_IMG_STARTED notification is received, we must set the GPIO map.
				// The order of the callbacks is unpredictable, so the notification may be received before the write response.
				// We don't have a GATT operation queue, so the SPOTA_GPIO_MAP write will fail if the SPOTAR_MEM_DEV hasn't finished yet.
				// Since this call is synchronized, we can wait for both broadcast intents from the callbacks before proceeding.
				// The order of the callbacks doesn't matter with this implementation.
				if (++gpioMapPrereq == 2) {
					int memInfoData = (miso_gpio << 24) | (mosi_gpio << 16) | (gs_gpio << 8) | sck_gpio;
					LogUtils.d(TAG, "Set SPOTA_GPIO_MAP: " + String.format("%#10x", memInfoData));
					BleClient.getInstance().write(
							mac, BleUUIDS.SPOTA_SERVICE_UUID,
							BleUUIDS.SPOTA_GPIO_MAP_UUID,
							memInfoData,
							BluetoothGattCharacteristic.FORMAT_UINT32,
							0,
							new BleWriteCallback() {
								@Override
								public void onOptionSucess() {
									super.onOptionSucess();
									processStep(4);
								}
							});

				}
				break;
			// Set SPOTA_PATCH_LEN_UUID
			case 4:
				int blocksize = updateFile.getFileBlockSize();
				if (lastBlock) {
					blocksize = this.updateFile.getNumberOfBytes() % updateFile.getFileBlockSize();
					preparedForLastBlock = true;
				}
				LogUtils.d(TAG, "setPatchLength: " + blocksize + " - " + String.format("%#4x", blocksize));
				LogUtils.d(TAG,"Set SPOTA_PATCH_LENGTH: " + blocksize);
				BleClient.getInstance().write(
						mac,BleUUIDS.SPOTA_SERVICE_UUID,
						BleUUIDS.SPOTA_PATCH_LEN_UUID,
						blocksize,
						BluetoothGattCharacteristic.FORMAT_UINT16,
						0,
						new BleWriteCallback(){
							@Override
							public void onOptionSucess() {
								super.onOptionSucess();
								processStep(5);
							}
						});

				break;
			// Send a block containing blocks of 20 bytes until the patch length (default 240) has been reached
			// Wait for response and repeat this action
			case 5:
				if (!lastBlock) {
					sendBlock();
				} else {
					if (!preparedForLastBlock) {
						processStep(4);
					} else if (!lastBlockSent) {
						sendBlock();
					} else if (!endSignalSent) {
//						activity.progressChunk.setVisibility(View.GONE);
						sendEndSignal();
					} else{
						onSuccess();
					}
				}
				break;
		}
	}

	public OtaOptions(Context context) {
		this.context = context;
		initErrorMap();
		characteristicsQueue = new ArrayDeque<BluetoothGattCharacteristic>();
	}



	public boolean getError() {
		return hasError;
	}

	public synchronized float sendBlock() {
		final float progress = ((float) (blockCounter + 1) / (float) updateFile.getNumberOfBlocks()) * 100;
		if (!lastBlockSent) {
			if(iOtaUpdateListener!=null){
				iOtaUpdateListener.onProgress((int) progress);
			}

			byte[][] block = updateFile.getBlock(blockCounter);
			int i = ++chunkCounter;
			if (chunkCounter == 0)
				LogUtils.d(TAG, "Current block: " + (blockCounter + 1) + " of " + updateFile.getNumberOfBlocks());
			boolean lastChunk = false;
			if (chunkCounter == block.length - 1) {
				chunkCounter = -1;
				lastChunk = true;
			}
			byte[] chunk = block[i];

			final int chunkNumber = (blockCounter * updateFile.getChunksPerBlockCount()) + i + 1;
			//final String message = "Sending chunk " + chunkNumber + " of " + file.getTotalChunkCount() + " (with " + chunk.length + " bytes)";
			if (chunkNumber == 1) {
				LogUtils.d(TAG, "Update procedure started.");
//				activity.progressChunk.setVisibility(View.VISIBLE);
			}
			if(iOtaUpdateListener!=null){
				String process = "正在更新  "+(chunkNumber*100)/updateFile.getTotalChunkCount()+"%  ，请勿进行其他操作\n Sending chunk " + chunkNumber + " of " + updateFile.getTotalChunkCount();
				iOtaUpdateListener.updateTitle(process);
			}
			String systemLogMessage = "Sending block " + (blockCounter + 1) + ", chunk " + (i + 1) + " of " + block.length + ", size " + chunk.length;
			Log.d(TAG, systemLogMessage);
			BleClient.getInstance().write(mac,
					BleUUIDS.SPOTA_SERVICE_UUID,
					BleUUIDS.SPOTA_PATCH_DATA_UUID,
					chunk,
					new BleWriteCallback(){
						@Override
						public void onOptionSucess() {
							super.onOptionSucess();
							if(chunkCounter != -1) {
								sendBlock();
							}
						}

						@Override
						public synchronized void onWriteSuccess(BluetoothDevice device) {
							super.onWriteSuccess(device);
							LogUtils.d(TAG, "writeCharacteristic: " + true);
						}

						@Override
						public synchronized void onWriteFialed(BluetoothDevice device) {
							super.onWriteFialed(device);
							LogUtils.d(TAG, "writeCharacteristic: " + false);
						}
					});
			if (lastChunk) {

				// SUOTA
				if (!lastBlock) {
					blockCounter++;
				} else {
					lastBlockSent = true;
				}
				if (blockCounter + 1 == updateFile.getNumberOfBlocks()) {
					lastBlock = true;
				}

				// SPOTA
//				if (type == SpotaManager.TYPE) {
//					lastBlockSent = true;
//				}
			}
		}
		return progress;
	}
	public void sendEndSignal() {
		LogUtils.d(TAG, "sendEndSignal");
		LogUtils.d(TAG,"send SUOTA END command");
		BleClient.getInstance().write(mac,
				BleUUIDS.SPOTA_SERVICE_UUID,
				BleUUIDS.SPOTA_MEM_DEV_UUID,
				END_SIGNAL,BluetoothGattCharacteristic.FORMAT_UINT32, 0,
				new BleWriteCallback(){
					@Override
					public void onOptionSucess() {
						super.onOptionSucess();
					}
				});
		endSignalSent = true;
	}
	public void sendRebootSignal() {
		LogUtils.d(TAG, "sendRebootSignal");
		LogUtils.d(TAG,"send SUOTA REBOOT command");
		BleClient.getInstance().write(mac,
				BleUUIDS.SPOTA_SERVICE_UUID,
				BleUUIDS.SPOTA_MEM_DEV_UUID,
				REBOOT_SIGNAL,
				BluetoothGattCharacteristic.FORMAT_UINT32, 0,
				new BleWriteCallback(){
					@Override
					public void onOptionSucess() {
						super.onOptionSucess();
						rebootsignalSent = true;
					}
				});

	}

	public void readNextCharacteristic() {
		if (characteristicsQueue.size() >= 1) {
			BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) characteristicsQueue.poll();
			bluetoothGatt.readCharacteristic(characteristic);
			Log.d(TAG, "readNextCharacteristic");
		}
	}

	public void disconnect() {
		if (wakeLock != null && wakeLock.isHeld()) {
			Log.d(TAG, "Release wake lock");
			wakeLock.release();
		}
		BleClient.getInstance().disConnect(mac);
		try {
			if (updateFile != null) {
				updateFile.close();
			}
		} catch (Exception e) {
			Log.d(TAG, e.getLocalizedMessage());
		}
	}

	protected void onSuccess() {
		finished = true;
		refreshPending = true;
		double elapsed = (new Date().getTime() - uploadStart) / 1000.;
		LogUtils.d(TAG,"Upload completed");
		LogUtils.d(TAG,"Elapsed time: " + elapsed + " seconds");
		LogUtils.d(TAG, "Upload completed in " + elapsed + " seconds");
		if (wakeLock.isHeld()) {
			Log.d(TAG, "Release wake lock");
			wakeLock.release();
		}
		if (Build.VERSION.SDK_INT >= 21) {
			Log.d(TAG, "Connection parameters update request (balanced)");
			bluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
		}
		if(iOtaUpdateListener!=null){
			iOtaUpdateListener.onUpdateComplete();
		}
	}

	private void initErrorMap() {
		this.errors = new HashMap<Integer, String>();
		// Value zero must not be used !! Notifications are sent when status changes.
		errors.put(0x03, "Forced exit of SPOTA service. See Table 1");
		errors.put(0x04, "Patch Data CRC mismatch.");
		errors.put(0x05, "Received patch Length not equal to PATCH_LEN characteristic value.");
		errors.put(0x06, "External Memory Error. Writing to external device failed.");
		errors.put(0x07, "Internal Memory Error. Not enough internal memory space for patch.");
		errors.put(0x08, "Invalid memory device.");
		errors.put(0x09, "Application error.");

		// SUOTAR application specific error codes
		errors.put(0x11, "Invalid image bank");
		errors.put(0x12, "Invalid image header");
		errors.put(0x13, "Invalid image size");
		errors.put(0x14, "Invalid product header");
		errors.put(0x15, "Same Image Error");
		errors.put(0x16, "Failed to read from external memory device");

		// Application error codes
//		errors.put(Statics.ERROR_COMMUNICATION, "Communication error.");
//		errors.put(Statics.ERROR_SUOTA_NOT_FOUND, "The remote device does not support SUOTA.");
	}
}
