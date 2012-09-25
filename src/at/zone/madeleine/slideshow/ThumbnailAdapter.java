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

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Gallery;
import android.widget.ImageView;
import at.zone.madeleine.data.NetworkTaskCallback;

public class ThumbnailAdapter extends ImageAdapter{
	
	private final float mDensity;
	private NetworkTaskCallback netwrkErrorDelegate;
	
	public ThumbnailAdapter(Context c, NetworkTaskCallback delegate) {
		mDensity = c.getResources().getDisplayMetrics().density;
		netwrkErrorDelegate = delegate;
	}

	public View getView(int position, View view, ViewGroup parent) {
		if (view == null) {
			view = new ImageView(parent.getContext());
		}
		
		getImageDownloader().download(getRemoteURLS()[position], ((ImageView)view),netwrkErrorDelegate);
		view.setLayoutParams(new Gallery.LayoutParams((int)(100 * mDensity + 0.5f), (int)(66 * mDensity + 0.5f)));
		((ImageView)view).setScaleType(ImageView.ScaleType.FIT_CENTER);
		view.setBackgroundColor(Color.TRANSPARENT);

		return view;
	}

	public ImageDownloader getImageDownloader() {
		return imageDownloader;
	}

}
