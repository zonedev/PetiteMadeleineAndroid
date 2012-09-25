/*
 * Copyright (C) 2012 ZONE Media GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.zone.madeleine.example;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import at.zone.madeleine.R;
import at.zone.madeleine.core.PetiteMadeleineCallbacks;
import at.zone.madeleine.core.PetiteMadeleineCore;
import at.zone.madeleine.core.Utils;
import at.zone.madeleine.data.Entry;
import at.zone.madeleine.data.FileDownloadCallback;
import at.zone.madeleine.data.FileDownloader;
import at.zone.madeleine.data.MediaAttachment;
import at.zone.madeleine.data.MediaAttachment.MediaType;
import at.zone.madeleine.ir.PixlinqSearch;
import at.zone.madeleine.slideshow.SlideshowActivity;

public class PetiteMadeleineBasicActivity extends Activity implements PetiteMadeleineCallbacks, FileDownloadCallback {
	
	private String masterListUrl = "_INSERT_URL_TO_MASTER_LIST_DATA_ON_SERVER_";
	
	private PetiteMadeleineCore pmCore;
	
	private Uri capturedImageUri;
	
	private Button captureButton;
	private TextView textView;
	private ProgressBar progressBar;
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.example_layout);
		
		// set UI element references:
		captureButton = (Button) findViewById (R.id.exampleButton);
		textView = (TextView) findViewById (R.id.exampleTextView);
		progressBar = (ProgressBar) findViewById (R.id.exampleProgressBar);
	
		// set event listener:
		captureButton.setOnClickListener(new OnClickListener() {	
			public void onClick(View v) {
				// open camera intent to capture an image:
				Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
				File photo = new File(Environment.getExternalStorageDirectory(), "capture.jpg");
				intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
				capturedImageUri = Uri.fromFile(photo);
				startActivityForResult(intent, 100);
			}
		});
		
		initPMCore();
	}
	
	private void initPMCore(){
		pmCore = new PetiteMadeleineCore(this, this, new PixlinqSearch(), masterListUrl);
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - L I F E  C Y C L E
	// get result from camera intent:
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case 100:
				if (resultCode == Activity.RESULT_OK) {
					sendIRRequest();
				}
		}
	}
	// implement these life cycle methods and call the corresponding methods
	// of the PetiteMadeleineCore object to ensure system integrity:
	public void onResume(){
		super.onResume();
		
		if(pmCore == null){
			initPMCore(); // recreate pmCore if it has been garbage collected
		} else {
			// otherwise call its own onResume() method:
			// this takes care of ContentManager updates etc.
			pmCore.onResume();
		}
	}
	
	public void onDestroy(){
		super.onDestroy();
		pmCore.onDestroy();
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	public void sendIRRequest(){
		byte[] queryImage = null;
		try {
			queryImage = Utils.readFile( capturedImageUri.getPath() );
			pmCore.sendIRRequest(queryImage);
			
			textView.setText("sending web request");
			progressBar.setVisibility(View.VISIBLE);
			captureButton.setEnabled(false); // disable capture until request has finished
		} catch (IOException e) {
			textView.setText("error reading image");
			progressBar.setVisibility(View.INVISIBLE);
			captureButton.setEnabled(true);
			e.printStackTrace();
		}
	}
	
	private void presentEntry(Entry entry) {
		if (entry.hasMultipleAttachments()) {
			// TODO implement UI for selecting media here
		} else {
			MediaAttachment media = entry.getSingleAttachment();
			presentMediaAttachment(media);
		}
	}
	
	public void presentMediaAttachment(MediaAttachment media) {
		
		if (media.type == MediaType.BROWSER) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(media.url));
			startActivity(browserIntent);
		}
		
		if (media.type == MediaType.PDF) {
			textView.setText("downloading " + media.url);
			progressBar.setVisibility(View.VISIBLE);
			captureButton.setEnabled(false); // disable capture until download has finished
			
			// download PDf to external storage before starting intent
			FileDownloader downloader = new FileDownloader();
			downloader.downloadFile(media.url, this);
		}
		
		if (media.type == MediaType.VIDEO) {
			Intent viewMediaIntent = new Intent();
			viewMediaIntent.setAction(android.content.Intent.ACTION_VIEW);
			viewMediaIntent.setDataAndType(Uri.parse(media.url), "video/*");
			viewMediaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(viewMediaIntent);
		}
		
		if (media.type == MediaType.YOUTUBE) {
			Intent youtubeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(media.url));
			startActivity(youtubeIntent);
		}
		
		if (media.type == MediaType.SLIDESHOW) {
			Intent slideshowIntent = new Intent(PetiteMadeleineBasicActivity.this, SlideshowActivity.class);
			slideshowIntent.putExtra("slideshowURL", media.url);
			startActivity(slideshowIntent);
		}
		
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - F I L E  D O W N L O A D E R  C A L L B A C K
	public void downloadFinished(File file) {
		if (file != null) {
			MediaType type = MediaAttachment.getMediaTypeFromFilename(file.getName());
			
			if (type == MediaType.PDF) {
				if(Utils.canDisplayPdf(this)) {
					Intent pdfIntent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(file));
					startActivity(pdfIntent);
				} else {
					// if no PDF Viewer is installed present a dialog with the
					// option to open google play to download Adobe Reader:
					AlertDialog alert = new AlertDialog.Builder(this).create();
					alert.setTitle(getResources().getString(R.string.no_pdf_viewer_title));
					alert.setMessage(getResources().getString(R.string.no_pdf_viewer_message));
					
					alert.setButton(AlertDialog.BUTTON1, getResources().getString(R.string.no_pdf_viewer_openstore_button), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Utils.openStoreForAdobeReader(getApplicationContext());
						}
					});
					
					alert.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.no_pdf_viewer_cancel_button), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// nothing to do here, we just let the method exit.
						}
					});
					
					alert.show();
				}
			}
		} else {
			textView.setText("error downloading file");
		}
		progressBar.setVisibility(View.INVISIBLE);
		captureButton.setEnabled(true);
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - P E T I T E  M A D E L E I N E  C A L L B A C K S
	public void contentManagerUpdateStarted() {
		textView.setText("updating content data");
		progressBar.setVisibility(View.VISIBLE);
		captureButton.setEnabled(false); // disable capture until system is ready
	}
	
	public void contentMangerUpdateFinished(Boolean validData) {
		textView.setText("ready");
		progressBar.setVisibility(View.INVISIBLE);
		captureButton.setEnabled(true); 
	}
	
	public void recognizedEntry(Entry entry) {
		textView.setText("recoginzed entry: " + entry.description);
		progressBar.setVisibility(View.INVISIBLE);
		captureButton.setEnabled(true); 
		
		presentEntry(entry);
	}
	
	public void imageNotRecognized() {
		textView.setText("image not regocnized");
		progressBar.setVisibility(View.INVISIBLE);
		captureButton.setEnabled(true); 
	}
	
	public void noEntryForMatch(String matchID) {
		textView.setText("image regocnized ('" + matchID + "') but there is no entry registered for it.");
		progressBar.setVisibility(View.INVISIBLE);
		captureButton.setEnabled(true); 
		
	}
	
	public void onNetworkError() {
		textView.setText("network error");
		progressBar.setVisibility(View.INVISIBLE);
		captureButton.setEnabled(true); 
	}
	
}