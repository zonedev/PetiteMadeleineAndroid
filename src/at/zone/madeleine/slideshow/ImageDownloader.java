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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import at.zone.madeleine.data.ContentManager;
import at.zone.madeleine.data.NetworkTaskCallback;

/**
 * This helper class downloads images from the Internet and binds those with the provided ImageView.
 *
 * It requires the INTERNET permission, which should be added to your application's manifest file.
 *
 * A local cache of downloaded images is maintained internally to improve performance.
 */
public class ImageDownloader {
	
	ProgressBar mProgressBar = null;
	
	private NetworkTaskCallback netwrkErrorDelegate;
	
	private static final String LOG_TAG = "ImageDownloader";
	
	public int width = 0;
	public int height = 0;
	
	/**
	 * Download the specified image from the Internet and binds it to the provided ImageView. The
	 * binding is immediate if the image is found in the cache and will be done asynchronously
	 * otherwise. A null bitmap will be associated to the ImageView if an error occurs.
	 *
	 * @param url The URL of the image to download.
	 * @param imageView The ImageView to bind the downloaded image to.
	 */
	public void download(String url, ImageView imageView, NetworkTaskCallback delegate) {
		
		netwrkErrorDelegate = delegate;
		
		Bitmap bitmap = getBitmapFromDisc(url);

		if (bitmap == null) {
			doDownload(url, imageView);
			if(mProgressBar != null) {
				mProgressBar.setVisibility(View.VISIBLE);
			}
		} else {
			cancelPotentialDownload(url, imageView);
			imageView.setImageBitmap(bitmap);
			if(mProgressBar != null) {
				mProgressBar.setVisibility(View.INVISIBLE);
			}
		}
	}

	/**
	 * Same as download but the image is always downloaded.
	 */
	private void doDownload(String url, ImageView imageView) {
		// State sanity: url is guaranteed to never be null in DownloadedDrawable and cache keys.
		if (url == null) {
			imageView.setImageDrawable(null);
			return;
		}

		if (cancelPotentialDownload(url, imageView)) {
			BitmapDownloaderTask task = new BitmapDownloaderTask(imageView);
				DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
				imageView.setImageDrawable(downloadedDrawable);
				task.execute(url);
		}
	}
	
	/**
	 * Returns true if the current download has been canceled or if there was no download in
	 * progress on this image view.
	 * Returns false if the download in progress deals with the same url. The download is not
	 * stopped in that case.
	 */
	private static boolean cancelPotentialDownload(String url, ImageView imageView) {
		BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
		if (bitmapDownloaderTask != null) {
			String bitmapUrl = bitmapDownloaderTask.url;
			if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
				 bitmapDownloaderTask.cancel(true);
			} else {
				return false; // The same URL is already being downloaded.
			}
		}
		return true;
	}

	/**
	 * @param imageView Any imageView
	 * @return Retrieve the currently active download task (if any) associated with this imageView.
	 * null if there is no such task.
	 */
	private static BitmapDownloaderTask getBitmapDownloaderTask(ImageView imageView) {
		if (imageView != null) {
			Drawable drawable = imageView.getDrawable();
			if (drawable instanceof DownloadedDrawable) {
				DownloadedDrawable downloadedDrawable = (DownloadedDrawable)drawable;
				return downloadedDrawable.getBitmapDownloaderTask();
			}
		}
		return null;
	}

	Bitmap downloadBitmap(String url) {
		// AndroidHttpClient is not allowed to be used from the main thread
		final HttpClient client = AndroidHttpClient.newInstance("Android");
		final HttpGet getRequest = new HttpGet(url);
		
		boolean networkError = false;

		try {
			HttpResponse response = client.execute(getRequest);
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				networkError = true;
				return null;
			}
			
			final HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream inputStream = null;
				try {
					inputStream = entity.getContent();
					// return BitmapFactory.decodeStream(inputStream);
					// Bug on slow connections, fixed in future release.
					return BitmapFactory.decodeStream(new FlushedInputStream(inputStream));
				 } finally {
					if (inputStream != null) {
						inputStream.close();
					}
					entity.consumeContent();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			getRequest.abort();
			networkError = true;
		} catch (IllegalStateException e) {
			e.printStackTrace();
			getRequest.abort();
			networkError = true;
		} catch (Exception e) {
			e.printStackTrace();
			getRequest.abort();
			networkError = true;
		} finally {
			if ((client instanceof AndroidHttpClient)) {
				((AndroidHttpClient) client).close();
			}
			if(networkError) {
				// inform the activity that we have network error(s)
				netwrkErrorDelegate.onNetworkError();
			}
		}
		return null;
	}
	
	/*
	 * An InputStream that skips the exact number of bytes provided, unless it reaches EOF.
	 */
	static class FlushedInputStream extends FilterInputStream {
		public FlushedInputStream(InputStream inputStream) {
			super(inputStream);
		}
		
		@Override
		public long skip(long n) throws IOException {
			long totalBytesSkipped = 0L;
			while (totalBytesSkipped < n) {
				long bytesSkipped = in.skip(n - totalBytesSkipped);
				if (bytesSkipped == 0L) {
					int b = read();
					if (b < 0) {
						break;  // we reached EOF
					} else {
						bytesSkipped = 1; // we read one byte
					}
				}
				totalBytesSkipped += bytesSkipped;
			}
			return totalBytesSkipped;
		}
	}

	/**
	 * The actual AsyncTask that will asynchronously download the image.
	 */
	class BitmapDownloaderTask extends AsyncTask<String, Void, Bitmap> {
		private String url;
		private final WeakReference<ImageView> imageViewReference;
		
		public BitmapDownloaderTask(ImageView imageView) {
			imageViewReference = new WeakReference<ImageView>(imageView);
		}

		/**
		 * Actual download method.
		 */
		@Override
		protected Bitmap doInBackground(String... params) {
			url = params[0];
			return downloadBitmap(url);
		}

		/**
		 * Once the image is downloaded, associates it to the imageView
		 */
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (isCancelled()) {
				bitmap = null;
			}
			try {
				saveImageToDisc(url,bitmap);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if (imageViewReference != null) {
				ImageView imageView = imageViewReference.get();
				BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
				// Change bitmap only if this process is still associated with it
				if (this == bitmapDownloaderTask) {
					imageView.setImageBitmap(bitmap);
				}
				if(mProgressBar != null) {
					mProgressBar.setVisibility(View.INVISIBLE);
				}
			}
		}
	}

	/**
	 * A fake Drawable that will be attached to the imageView while the download is in progress.
	 *
	 * <p>Contains a reference to the actual download task, so that a download task can be stopped
	 * if a new binding is required, and makes sure that only the last started download process can
	 * bind its result, independently of the download finish order.</p>
	 */
	static class DownloadedDrawable extends ColorDrawable {
		private final WeakReference<BitmapDownloaderTask> bitmapDownloaderTaskReference;
		
		public DownloadedDrawable(BitmapDownloaderTask bitmapDownloaderTask) {
			super(Color.BLACK);
			bitmapDownloaderTaskReference =
			new WeakReference<BitmapDownloaderTask>(bitmapDownloaderTask);
		}
		
		public BitmapDownloaderTask getBitmapDownloaderTask() {
			return bitmapDownloaderTaskReference.get();
		}
	}

	/**
	 * Methods to save and retrieve temporarily images (full images and thumbnails).
	 * Note: 	These files should be deleted if not used anymore, i.e. if the app has been closed.
	 * 			onDestroy() in the main activity is a good place for this task (handled by ContentManager). 
	 */
	private void saveImageToDisc (String url, Bitmap bitmap) throws IOException{
		if(bitmap != null) {
			String filename = getFileNameFromURL(url);
			if(url.contains("thumbnail")){
				filename = "thumbnail_".concat(filename);
			}
			String path = ContentManager.getInstance().getSlideshowPath();
			OutputStream fOut = null;
			
			File file = new File(path, filename);
			try {
				fOut = new FileOutputStream(file);
				bitmap.compress(Bitmap.CompressFormat.JPEG,100, fOut);
				fOut.flush();
				fOut.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Retrieve Bitmap from local storage.
	 */
	private Bitmap getBitmapFromDisc(String url) {
		String filename = getFileNameFromURL(url);
		if(url.contains("thumbnail")){
			filename = "thumbnail_".concat(filename);
		}
		String path = ContentManager.getInstance().getSlideshowPath();
		Bitmap bitmap = BitmapFactory.decodeFile(path.concat(filename));
		return bitmap;
	}
	
	/**
	 * Get the filename from a given url.
	 */
	private String getFileNameFromURL(String url) {
		return url.substring(url.lastIndexOf("/")+1);
	}
}
