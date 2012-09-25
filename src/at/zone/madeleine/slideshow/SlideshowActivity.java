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

package at.zone.madeleine.slideshow;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ProgressBar;
import at.zone.madeleine.R;
import at.zone.madeleine.data.ContentManager;
import at.zone.madeleine.data.ContentManagerCallback;
import at.zone.madeleine.data.PListLoader;
import at.zone.madeleine.data.PListLoaderCallback;


public class SlideshowActivity extends Activity implements PListLoaderCallback, ContentManagerCallback {
	
	static final int MIN_DISTANCE = 10;
	private int width, height;
	
	PListLoader plistLoader;
	String slideshowURL;

	Gallery thumbnailBar;
	MdlGallery imgGallery;
	ImageAdapter imageAdapter;
	ThumbnailAdapter thumbnailAdapter;
	
	ProgressBar progressbar;
	

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final Bundle extras = getIntent().getExtras();
		slideshowURL = extras.getString("slideshowURL");
		
		// download content (links to images) from slideshowURL
		plistLoader = new PListLoader();
		plistLoader.loadPListFromUrl(slideshowURL, this);
		

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.slideshow);
		
		progressbar = (ProgressBar)findViewById(R.id.progress_ring);
		
		
		// create gallery for thumbnails
		// Reference the Gallery view
		thumbnailBar = (Gallery) findViewById(R.id.thumbnails);
		thumbnailBar.setBackgroundColor(Color.TRANSPARENT);
		thumbnailBar.setSpacing(10);

		// Set an item click listener to retrieve a full image if clicked a thumbnail
		thumbnailBar.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView parent, View v, int position, long id) {
				// get the corresponding image
				imgGallery.setSelection(position);
			}
		});
		
		registerForContextMenu(thumbnailBar);
		thumbnailBar.setVisibility(View.INVISIBLE);
		thumbnailBar.setUnselectedAlpha(1);
		

		Display display = ((WindowManager) getSystemService(WINDOW_SERVICE))
				.getDefaultDisplay();
		
		height = display.getHeight();
		width = display.getWidth();

		// create gallery for full images
		imgGallery = (MdlGallery) findViewById(R.id.gallery_images);
		imgGallery.setBackgroundColor(Color.BLACK);
		
		// enable toggle of thumbnailbar through longpress
		imgGallery.setOnItemLongClickListener( new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				
				if (thumbnailBar.getVisibility() == View.INVISIBLE)
					thumbnailBar.setVisibility(View.VISIBLE);
				else
					thumbnailBar.setVisibility(View.INVISIBLE);
				
				return false;
			}
		} );

		
		// check the orientation and set the appropriate spacing (in case you want different spacing for different orientation)
		if (display.getRotation() == Surface.ROTATION_0
		|| display.getRotation() == Surface.ROTATION_180) {
			// PORTRAIT
			imgGallery.setSpacing(40);
		} else {
			// LANDSCAPE
			imgGallery.setSpacing(10);
		}

	}
	
	@Override
	protected void onStart() {
		super.onStart();
		ContentManager.getInstance().registerDelegate(this);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		ContentManager.getInstance().unregisterDelegate(this);
	}

	// to enable this method add android:configChanges="orientation|screenSize" to the activity attribute:
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		Display display = ((WindowManager) getSystemService(WINDOW_SERVICE))
				.getDefaultDisplay();
		
		height = display.getHeight();
		width = display.getWidth();
		imageAdapter.getImageDownloader().height = height;
		imageAdapter.getImageDownloader().width = width;
		
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			imgGallery.setSpacing(10);
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			imgGallery.setSpacing(40);
		}

		imgGallery.getSelectedView().setLayoutParams(
				new Gallery.LayoutParams(width, height));
		
		// reset the zoom
		imgGallery.setOnTouchListener();
		((ImageView)imgGallery.getSelectedView()).setScaleType(ImageView.ScaleType.FIT_CENTER);
	}

	@Override
	public void plistDataReady(HashMap<String, Object> data, String tag) {
		ArrayList slides = (ArrayList) data.get("array");
		int slideCount = slides.size();
		String[] fullImageURLs = new String[slideCount];
		String[] thumbnailURLs = new String[slideCount];
		String[] imageDescription = new String[slideCount];
		for(int i=0;i<slideCount;i++){
			HashMap<String, Object> slide = (HashMap<String, Object>)slides.get(i);
			fullImageURLs[i] = (String) slide.get("url");
			thumbnailURLs[i] = (String) slide.get("thumbnail");
			imageDescription[i] = (String) slide.get("name");
		}
		
		// Set the adapter to our custom adapter (below)
		thumbnailAdapter = new ThumbnailAdapter(this,this);
		thumbnailAdapter.setURLS(thumbnailURLs);
		thumbnailBar.setAdapter(thumbnailAdapter);
				
		// now we have the images in the right order and can give it to the imageAdapter
		imageAdapter = new ImageAdapter(progressbar,this);
		imageAdapter.setURLS(fullImageURLs);
		imageAdapter.getImageDownloader().height = height;
		imageAdapter.getImageDownloader().width = width;
		imgGallery.setAdapter(imageAdapter);
		
		// TODO use the imageDescription for whatever you want
	}

	@Override
	public void onNetworkError() {
		// TODO implement network error handling
	}

	@Override
	public void contentManagerFinished(ContentManager contentManager) {		
		// reload the links to the images
		if(plistLoader != null)
			plistLoader.loadPListFromUrl(slideshowURL, this);
		
		// go back to the first image
		imgGallery.setSelection(0);
	
	}


}