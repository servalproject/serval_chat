package org.servalproject.servalchat.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by jeremy on 29/03/17.
 */

public class TimestampView extends TextView {
	public TimestampView(Context context) {
		super(context);
	}

	public TimestampView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TimestampView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	private static DateFormat df = DateFormat.getDateInstance();
	private static DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);

	public void setDates(Date date, Date previous){
		if (date.getTime()<=0){
			setVisibility(View.GONE);
		} else {
			String thisDate = df.format(date);
			if (!thisDate.equals(df.format(previous==null ? new Date() : previous))) {
				setText(thisDate+" "+tf.format(date));
				setVisibility(View.VISIBLE);
			} else if (previous==null || previous.getTime() - date.getTime() >= 30 * 60 * 1000) {
				setText(tf.format(date));
				setVisibility(View.VISIBLE);
			} else {
				setVisibility(View.GONE);
			}
		}
	}
}
