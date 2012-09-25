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
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class MdlGallery extends Gallery{
	
	Context c;
	int lastPosition = -1;
	
	public MdlGallery(Context context) {
		super(context);
		c = context;
	}
	
	public MdlGallery(Context context, AttributeSet attrs) {
		super(context, attrs);
		c = context;
	}
 
	public MdlGallery(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		c = context;
	}
	
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		velocityX = 0; // stop bouncing
		
		if(lastPosition != this.getSelectedItemPosition()) {
			// reset the onTouchListener if the image-position changed
			this.setOnTouchListener();
			lastPosition = this.getSelectedItemPosition();
		}
		return super.onFling(e1, e2, velocityX, velocityY);
	}
	
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		distanceX *= 2; // change this i.e. to 3 for faster "sliding" behavior
		return super.onScroll(e1, e2, distanceX, distanceY);
	}
	
	public void setOnTouchListener() {
		
		OnTouchListener mdlOnTouchListener = new OnTouchListener() {
			
			private static final String TAG = "Touch";
			
			// These matrices will be used to move and zoom image
			Matrix matrix = new Matrix();
			Matrix savedMatrix = new Matrix();
			
			// We can be in one of these 3 states
			static final int NONE = 0;
			static final int DRAG = 1;
			static final int ZOOM = 2;
			int mode = NONE;
			
			// Remember some things for zooming
			PointF start = new PointF();
			PointF mid = new PointF();
			float oldDist = 1f;
			float defaultScale = 1;
			boolean zoomedIn = false;
			
			// Remember some things for dragging
			PointF defaultPosition = new PointF();
			
			public boolean onTouch(View v, MotionEvent rawEvent) {
				WrapMotionEvent event = WrapMotionEvent.wrap(rawEvent);
				
				ImageView view = (ImageView) ((MdlGallery)v).getChildAt(0);
				
				// Dump touch event to log FOR DEBUGGING
				// dumpEvent(event);
	
				Display display = ((WindowManager)getContext().getSystemService(c.WINDOW_SERVICE)).getDefaultDisplay();
				
				if( display.getRotation() == Surface.ROTATION_0 || display.getRotation() == Surface.ROTATION_180 ) {
					defaultScale = (float)display.getWidth()/(float)view.getDrawable().getIntrinsicWidth();
				} else {
					defaultScale = (float)display.getHeight()/(float)view.getDrawable().getIntrinsicHeight();
				}
				
				float values[] = new float[9];
				matrix.getValues(values);
				
				if(values[0] == 1 && values[4] == 1&& values [8] == 1) {
					int dw = (int)((float)(display.getWidth() - view.getDrawable().getIntrinsicWidth()*defaultScale)/2);
					int dh = (int)((float)(display.getHeight() - view.getDrawable().getIntrinsicHeight()*defaultScale)/2);
					matrix.setScale(defaultScale, defaultScale);
					matrix.postTranslate(dw, dh);
				}
				
				int dw = (int)((float)(display.getWidth() - view.getDrawable().getIntrinsicWidth()*defaultScale)/2);
				int dh = (int)((float)(display.getHeight() - view.getDrawable().getIntrinsicHeight()*defaultScale)/2);
				defaultPosition.x = dw;
				defaultPosition.y = dh;
					
				// Handle touch events here...
				switch (event.getAction() & MotionEvent.ACTION_MASK) {
					
					case MotionEvent.ACTION_DOWN:
						savedMatrix.set(matrix);
						start.set(event.getX(), event.getY());
						mode = DRAG;
						break;
						
					case MotionEvent.ACTION_POINTER_DOWN:
						oldDist = spacing(event);
						if (oldDist > 10f) {
							savedMatrix.set(matrix);
							midPoint(mid, event);
							mode = ZOOM;
						}
						break;
					
					case MotionEvent.ACTION_UP:
					
					case MotionEvent.ACTION_POINTER_UP:
						mode = NONE;
						break;
						
					case MotionEvent.ACTION_MOVE:
						if (mode == DRAG && zoomedIn) {
							matrix.set(savedMatrix);
							// create some variables, x & y are future positions and a & b are current positions
							// x & a are on the x-axis and y & b on the y-axis
							float x, y, a, b;
							
							matrix.getValues(values);
							a = values[2];
							b = values[5];
							
							if( (event.getX() - start.x + a) > 0 && view.getDrawable().getIntrinsicWidth() * values[0] > display.getWidth() ) {
								// left border violation
								x = a;
								x *= -1;
							} else if( ((float)( (a + (event.getX() - start.x)) + view.getDrawable().getIntrinsicWidth() * values[0])) < display.getWidth() && view.getDrawable().getIntrinsicWidth() * values[0] > display.getWidth() ) {
								// right border violation
								if(a > (float)(display.getWidth() - view.getDrawable().getIntrinsicWidth() * values[0]) ){
									x = a - (float)(display.getWidth() - view.getDrawable().getIntrinsicWidth() * values[0]);
									x *= -1;
								} else {
									x = 0;
								}
							} else if ( view.getDrawable().getIntrinsicWidth() * values[0] < display.getWidth() ) {
								// image-width is smaller than the screen-width:
								if( (event.getX() - start.x + a) < 0 ) {
									// left border violation
									x = a;
									x *= -1;
								} else if ( (event.getX() - start.x + a + view.getDrawable().getIntrinsicWidth() * values[0] ) > display.getWidth() ) {
									// right border violation
									x = -a + display.getWidth() - view.getDrawable().getIntrinsicWidth() * values[0];
								} else {
									// no violation
									x = event.getX() - start.x;
								}
							} else {
								// no left-right-border violation
								x = event.getX() - start.x;
								if( view.getDrawable().getIntrinsicWidth() * values[0] < display.getWidth() ) {
									if( (a+x) < 0 ) {
										x = a;
										x *= -1;
									}
								}
								
							}
							
							if( ( view.getDrawable().getIntrinsicHeight() * values[0] ) > display.getHeight() ) {
								y = event.getY() - start.y + b;
								
								if(y > 0) {
									// up border violation
									y = b;
									y *= -1;
								} else if( (y + view.getDrawable().getIntrinsicHeight() * values[0]) < display.getHeight() ) {
									// down border violation
									if ( b > (float)(display.getHeight() - view.getDrawable().getIntrinsicHeight() * values[0]) ) {
										y = b - (float)(display.getHeight() - view.getDrawable().getIntrinsicHeight() * values[0]);
										y *= -1;
									} else {
										y = 0;
									}
								} else {
									y = event.getY() - start.y;
									if( view.getDrawable().getIntrinsicHeight() * values[0] < display.getHeight() ) {
										if( (b+y) < 0 ) {
											y = -b;
										}
									}
								}
							} else {
								y = event.getY() - start.y;
								if( view.getDrawable().getIntrinsicHeight() * values[0] < display.getHeight() ) {
									if( (b+y) < 0 ) {
										y = -b;
									} else if( (b+y+view.getDrawable().getIntrinsicHeight() * values[0]) > display.getHeight() ) {
										y = -b + display.getHeight() - view.getDrawable().getIntrinsicHeight() * values[0];
									}
								}
							}
							matrix.postTranslate(x,y);
							((ImageView) ((MdlGallery)v).getChildAt(0)).setScaleType(ScaleType.MATRIX);
							((ImageView) ((MdlGallery)v).getChildAt(0)).setImageMatrix(matrix);
							return true;
						} else if (mode == ZOOM) {
							float newDist = spacing(event);
							if (newDist > 10f) {
								matrix.set(savedMatrix);
								float scale;
								scale = newDist / oldDist;
								
								matrix.postScale(scale, scale, mid.x, mid.y);
								matrix.getValues(values);
								
								if(values[0] < defaultScale && values[4] < defaultScale) {
									matrix.setScale(defaultScale, defaultScale);
									matrix.postTranslate(dw, dh);
									zoomedIn = false;
								} else {
									zoomedIn = true;
								}
								((ImageView) ((MdlGallery)v).getChildAt(0)).setScaleType(ScaleType.MATRIX);
								((ImageView) ((MdlGallery)v).getChildAt(0)).setImageMatrix(matrix);
							}
						}
						break;
					}
					
				return false;
				
			}
			
			/** Show an event in the LogCat view, for debugging */
			private void dumpEvent(WrapMotionEvent event) {
				String names[] = { "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE",
						"POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?" };
				StringBuilder sb = new StringBuilder();
				int action = event.getAction();
				int actionCode = action & MotionEvent.ACTION_MASK;
				sb.append("event ACTION_").append(names[actionCode]);
				if (actionCode == MotionEvent.ACTION_POINTER_DOWN
						|| actionCode == MotionEvent.ACTION_POINTER_UP) {
					sb.append("(pid ").append(
							action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
					sb.append(")");
				}
				sb.append("[");
				for (int i = 0; i < event.getPointerCount(); i++) {
					sb.append("#").append(i);
					sb.append("(pid ").append(event.getPointerId(i));
					sb.append(")=").append((int) event.getX(i));
					sb.append(",").append((int) event.getY(i));
					if (i + 1 < event.getPointerCount())
						sb.append(";");
				}
				sb.append("]");
			}
			
			/** Determine the space between the first two fingers */
			private float spacing(WrapMotionEvent event) {
				float x = event.getX(0) - event.getX(1);
				float y = event.getY(0) - event.getY(1);
				return FloatMath.sqrt(x * x + y * y);
			}
			
			/** Calculate the mid point of the first two fingers */
			private void midPoint(PointF point, WrapMotionEvent event) {
				float x = event.getX(0) + event.getX(1);
				float y = event.getY(0) + event.getY(1);
				point.set(x / 2, y / 2);
			}
			
		};
		super.setOnTouchListener(mdlOnTouchListener);
	}
}
