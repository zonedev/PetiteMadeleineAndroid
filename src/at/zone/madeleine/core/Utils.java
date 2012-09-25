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

package at.zone.madeleine.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.Uri;

public class Utils {
	
	// can be used to get jpeg data from file for pixlinQ
	public static final byte[] readFile(String filePath) throws IOException {
		File file = new File(filePath);
		FileInputStream fis = null;
		ByteArrayOutputStream bos = null;
		byte[] data = null;

		try {
			fis = new FileInputStream(file);
			bos = new ByteArrayOutputStream();
			while (fis.available() != 0) {
				byte[] crtBytes = new byte[fis.available()];
				fis.read(crtBytes);
				bos.write(crtBytes);
			}
			bos.flush();
			data = bos.toByteArray();
		} finally {
			if (bos != null) {
				bos.close();
			}
			if (fis != null) {
				fis.close();
			}
		}	
		return data;
	}
	
	public static final String MIME_TYPE_PDF = "application/pdf";

	/**
	 * Check if the supplied context can render PDF files via some installed application that reacts to a intent
	 * with the pdf mime type and viewing action.
	 */
	public static boolean canDisplayPdf(Context context) {
		PackageManager packageManager = context.getPackageManager();
		Intent testIntent = new Intent(Intent.ACTION_VIEW);
		testIntent.setType(MIME_TYPE_PDF);
		List<ResolveInfo> result = packageManager.queryIntentActivities(testIntent, PackageManager.MATCH_DEFAULT_ONLY);
		if (result.size() > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	// this just checks if we are connected NOT if we can access the internet!
	public static boolean hasNetworkConnection(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable() && cm.getActiveNetworkInfo().isConnected()) {
			return true;
		} else {
			return false;
		}
	}
	
	public static void openStoreForAdobeReader(Context context) {
		String readerUrl = "https://play.google.com/store/apps/details?id=com.adobe.reader";
		Intent storeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(readerUrl));
		context.startActivity(storeIntent);
	}
}
