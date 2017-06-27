package org.servalproject.servalchat.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.servalproject.mid.Identity;
import org.servalproject.servaldna.AbstractId;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by jeremy on 26/06/17.
 */

public class Identicon extends Drawable {
	private final Bitmap bitPattern;
	private final Paint bitmapPaint;
	private final Rect dest = new Rect();
	private static final String TAG = "Identicon";

	public Identicon(AbstractId id){
		this(id.getBinary());
	}

	private static final int patterns[] = new int[]{
				0b00101, 0b00110, 0b01001, 0b01010,
				0b01011, 0b01100, 0b01101, 0b01110,
				0b10010, 0b10011, 0b10100, 0b10101,
				0b10110, 0b10111, 0b11001, 0b11010
	};

	public Identicon(byte[] value) {

		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(value);
			value = md.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		ByteBuffer b = ByteBuffer.wrap(value);

		int H = b.get() & 0xFF;
		byte val = b.get();
		int S = val &0xF;
		int V = val >>4 &0xF;

		int fg = Color.HSVToColor(new float[]{H/255f * 360f, S/31f + 0.5f, V/31f + 0.5f});
		int bg = 0;

		int colors[] = new int[25];

		for (int i=0; i<3; i++){
			val = ((i&1)==0) ? b.get() : (byte) (val >> 4);
			// use 4 bits to pick a 5 bit pattern so we never get all on or all off
			int bitPattern = patterns[val & 0xf];
			colors[i] = ((bitPattern & 1) !=0) ? fg : bg;
			colors[i+5] = ((bitPattern & 2) !=0) ? fg : bg;
			colors[i+10] = ((bitPattern & 4) !=0) ? fg : bg;
			colors[i+15] = ((bitPattern & 8) !=0) ? fg : bg;
			colors[i+20] = ((bitPattern & 16) !=0) ? fg : bg;
		}
		// copy column 1 to 5 and 2 to 4
		for (int i=0;i<25;i+=5){
			colors[i+4] = colors[i];
			colors[i+3] = colors[i+1];
		}

		bitPattern = Bitmap.createBitmap(colors, 5, 5, Bitmap.Config.ARGB_8888);
		bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(false);
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		dest.set(0,0,canvas.getWidth(),canvas.getHeight());
		dest.inset(canvas.getWidth() / 10,canvas.getHeight() / 10);
		canvas.drawColor(0xFFF0F0F0);
		canvas.drawBitmap(bitPattern, null, dest, bitmapPaint);
	}

	@Override
	public void setAlpha(@IntRange(from = 0, to = 255) int i) {

	}

	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter) {

	}

	@Override
	public int getOpacity() {
		return PixelFormat.UNKNOWN;
	}
}
