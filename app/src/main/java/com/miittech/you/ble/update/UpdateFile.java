/*
 *******************************************************************************
 *
 * Copyright (C) 2016 Dialog Semiconductor, unpublished work. This computer
 * program includes Confidential, Proprietary Information and is a Trade
 * Secret of Dialog Semiconductor. All use, disclosure, and/or reproduction
 * is prohibited unless authorized in writing. All Rights Reserved.
 *
 * bluetooth.support@diasemi.com
 *
 *******************************************************************************
 */

package com.miittech.you.ble.update;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class UpdateFile {
	private InputStream inputStream;
	private byte crc;
	private byte[] bytes;

	private byte[][][] blocks;

	private int fileBlockSize = 0;
	private int bytesAvailable;
	private int numberOfBlocks = -1;
	private int chunksPerBlockCount;
	private int totalChunkCount;

	private UpdateFile(InputStream inputStream) throws IOException {
		this.inputStream = inputStream;
		this.bytesAvailable = this.inputStream.available();
		setType();
	}

	public void setType() throws IOException {
		// Reserve 1 extra byte to the total array for the CRC code
		this.bytes = new byte[this.bytesAvailable + 1];
		this.inputStream.read(this.bytes);
		this.crc = calculateCrc();
		this.bytes[this.bytesAvailable] = this.crc;
//			this.bytes = new byte[this.bytesAvailable];
//			this.inputStream.read(this.bytes);
	}

	public int getFileBlockSize() {
		return this.fileBlockSize;
	}

	public int getNumberOfBytes() {
		return this.bytes.length;
	}

	public void setFileBlockSize(int fileBlockSize) {
		this.fileBlockSize = fileBlockSize;
		this.chunksPerBlockCount = (int) Math.ceil((double) fileBlockSize / (double) UpConst.fileChunkSize);
		this.numberOfBlocks = (int) Math.ceil((double) this.bytes.length / (double) this.fileBlockSize);
		this.initBlocks();
	}

	private void initBlocksSuota() {
		int totalChunkCounter = 0;
		blocks = new byte[this.numberOfBlocks][][];
		int byteOffset = 0;
		// Loop through all the bytes and split them into pieces the size of the default chunk size
		for (int i = 0; i < this.numberOfBlocks; i++) {
			int blockSize = this.fileBlockSize;
			if (i + 1 == this.numberOfBlocks) {
				blockSize = this.bytes.length % this.fileBlockSize;
			}
			int numberOfChunksInBlock = (int) Math.ceil((double) blockSize / UpConst.fileChunkSize);
			int chunkNumber = 0;
			blocks[i] = new byte[numberOfChunksInBlock][];
			for (int j = 0; j < blockSize; j += UpConst.fileChunkSize) {
				// Default chunk size
				int chunkSize = UpConst.fileChunkSize;
				// Last chunk of all
				if (byteOffset + UpConst.fileChunkSize > this.bytes.length) {
					chunkSize = this.bytes.length - byteOffset;
				}
				// Last chunk in block
				else if (j + UpConst.fileChunkSize > blockSize) {
					chunkSize = this.fileBlockSize % UpConst.fileChunkSize;
				}

				//Log.d("chunk", "total bytes: " + this.bytes.length + ", offset: " + byteOffset + ", block: " + i + ", chunk: " + (chunkNumber + 1) + ", blocksize: " + blockSize + ", chunksize: " + chunkSize);
				byte[] chunk = Arrays.copyOfRange(this.bytes, byteOffset, byteOffset + chunkSize);
				blocks[i][chunkNumber] = chunk;
				byteOffset += chunkSize;
				chunkNumber++;
				totalChunkCounter++;
			}
		}
		// Keep track of the total chunks amount, this is used in the UI
		this.totalChunkCount = totalChunkCounter;
	}


	private void initBlocksSpota() {
		this.numberOfBlocks = 1;
		this.fileBlockSize = this.bytes.length;
		this.totalChunkCount = (int) Math.ceil((double) this.bytes.length / (double) UpConst.fileChunkSize);
		this.blocks = new byte[numberOfBlocks][this.totalChunkCount][];
		int byteOffset = 0;
		int chunkSize = UpConst.fileChunkSize;
		for (int i = 0; i < this.totalChunkCount; i++) {
			if (byteOffset + UpConst.fileChunkSize > this.bytes.length) {
				chunkSize = this.bytes.length - byteOffset;
			}
			byte[] chunk = Arrays.copyOfRange(this.bytes, byteOffset, byteOffset + chunkSize);
			blocks[0][i] = chunk;
			byteOffset += UpConst.fileChunkSize;
		}
	}

	// Create the array of blocks using the given block size.
	private void initBlocks() {
		this.initBlocksSuota();
	}

	public byte[][] getBlock(int index) {
		return blocks[index];
	}

	public void close() {
		if (this.inputStream != null) {
			try {
				this.inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public int getNumberOfBlocks() {
		return this.numberOfBlocks;
	}

	public int getChunksPerBlockCount() {
		return chunksPerBlockCount;
	}

	public int getTotalChunkCount() {
		return this.totalChunkCount;
	}

	private byte calculateCrc() throws IOException {
		byte crc_code = 0;
		for (int i = 0; i < this.bytesAvailable; i++) {
			Byte byteValue = this.bytes[i];
			int intVal = byteValue.intValue();
			crc_code ^= intVal;
		}
		Log.d("crc", String.format("Fimware CRC: %#04x", crc_code & 0xff));
		return crc_code;
	}

	public byte getCrc() {
		return crc;
	}

	public static UpdateFile getByFilePath(String path) throws IOException {
		// Get the file and store it in fileStream
		InputStream is = new FileInputStream(path);
		return new UpdateFile(is);
	}

//    public static ArrayList<String> list() {
//		java.io.File f = new java.io.File(filesDir);
//		java.io.File file[] = f.listFiles();
//		if (file == null)
//			return null;
//        Arrays.sort(file, new Comparator<java.io.File>() {
//            @Override
//            public int compare(java.io.File lhs, java.io.File rhs) {
//                return lhs.getPath().compareToIgnoreCase(rhs.getPath());
//            }
//        });
//		Log.d("Files", "Size: "+ file.length);
//        ArrayList<String> names = new ArrayList<String>();
//		for (int i=0; i < file.length; i++)
//		{
//			Log.d("Files", "FileName:" + file[i].getName());
//            names.add(file[i].getName());
//		}
//        return names;
//	}

	public static boolean createFileDirectories(Context c) {
		java.io.File directory = new java.io.File(UpConst.file_blefirmware_download_path);
		boolean flag = directory.exists() || directory.mkdirs();
		return flag;
	}
}
