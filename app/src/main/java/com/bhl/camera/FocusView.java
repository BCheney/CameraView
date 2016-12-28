package com.bhl.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * Extends View. Just used to draw Rect when the screen is touched
 * for auto focus.
 * 
 * Use setHaveTouch function to set the status and the Rect to be drawn.
 * Call invalidate to draw Rect. Call invalidate again after 
 * setHaveTouch(false, Rect(0, 0, 0, 0)) to hide the rectangle.
 */
public class FocusView extends View {
	private boolean haveTouch = false;
	private Rect touchArea;
	private Paint paint;
	private Paint rectPaint;

	private int width,height;
	private int maxMargin;
	private int currentMargin = 0;

	private static final int MODE_RECT = 0;
	private static final int MODE_SQUARE = 1;
	private int mode = MODE_RECT;
	public FocusView(Context context){
		super(context);
		paint = new Paint();
		paint.setColor(0xeed7d7d7);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(2);
		haveTouch = false;

		rectPaint = new Paint();
		rectPaint.setColor(Color.BLACK);
		rectPaint.setStyle(Paint.Style.FILL);
		rectPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		rectPaint.setStrokeWidth(1);

		getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if(getWidth() > 0 && getHeight() > 0){
					width = getWidth();
					height = getHeight();
					maxMargin = (height-width)/2;
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						getViewTreeObserver().removeOnGlobalLayoutListener(this);
					}else{
						getViewTreeObserver().removeGlobalOnLayoutListener(this);
					}
				}
			}
		});
	}

	public void setHaveTouch(boolean val, Rect rect) {
		haveTouch = val;
		touchArea = rect;
	}

	public void drawRect(int margin){
		int toMargin = currentMargin + margin;
		if(toMargin >= 0 && toMargin <= maxMargin){
			currentMargin = toMargin;
			invalidate();
		}
	}

	public void returnNormal(){
		if(currentMargin > maxMargin/2){
			mode = MODE_SQUARE;
			while (currentMargin<maxMargin){
				currentMargin++;
				invalidate();
			}
		}else {
			mode = MODE_RECT;
			while (currentMargin>0){
				currentMargin--;
				invalidate();
			}
		}
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		if(haveTouch){
		    canvas.drawRect(touchArea.left, touchArea.top, touchArea.right, touchArea.bottom, paint);
	    }
		canvas.drawRect(0,0,width,currentMargin,rectPaint);
		canvas.drawRect(0,height-currentMargin,width,height,rectPaint);
	}

	public boolean isCaptureSquare(){
		return mode == MODE_SQUARE;
	}
}