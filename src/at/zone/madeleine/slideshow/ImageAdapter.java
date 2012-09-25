/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ProgressBar;
import at.zone.madeleine.data.NetworkTaskCallback;

public class ImageAdapter extends BaseAdapter {
	
	private String[] remoteURLS = null;
	
	private NetworkTaskCallback netwrkErrorDelegate;
	
	protected final ImageDownloader imageDownloader;
	
	public ImageAdapter(){
		imageDownloader = new ImageDownloader();
	}
	
	public ImageAdapter(ProgressBar progressBar, NetworkTaskCallback delegate) {
		this();
		imageDownloader.mProgressBar = progressBar;
		netwrkErrorDelegate = delegate;
	}
	
	public int getCount() {
		return getRemoteURLS().length;
	}

	public String getItem(int position) {
		return getRemoteURLS()[position];
	}

	public long getItemId(int position) {
		return getRemoteURLS()[position].hashCode();
	}

	public View getView(int position, View view, ViewGroup parent) {
		if (view == null) {
			view = new ImageView(parent.getContext());
		}
		if (getRemoteURLS() != null) {
			imageDownloader.download(getRemoteURLS()[position], ((ImageView)view), netwrkErrorDelegate);
			view.setLayoutParams(new Gallery.LayoutParams(imageDownloader.width, imageDownloader.height));
			((ImageView)view).setScaleType(ImageView.ScaleType.FIT_CENTER);
			view.setBackgroundColor(Color.BLACK);
		} else { // this should not happen, however in case it does it shows a black screen if no images were provided for download
			view.setLayoutParams(new Gallery.LayoutParams(imageDownloader.width, imageDownloader.height));
			((ImageView)view).setScaleType(ImageView.ScaleType.FIT_CENTER);
			view.setBackgroundColor(Color.BLACK);
		}
		return view;
	}
	
	public ImageDownloader getImageDownloader() {
		return imageDownloader;
	}
	
	public void setURLS(String[] slideshowURLS) {
		setRemoteURLS(slideshowURLS);
	}
	
	public String[] getRemoteURLS() {
		return remoteURLS;
	}
	
	public void setRemoteURLS(String[] remoteURLS) {
		this.remoteURLS = remoteURLS;
	}
}
