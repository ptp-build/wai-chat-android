package chat.wai.ui.helpers.Simulator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public class ClickSimulator {
	public void performClick(View view, float x, float y) {
		// 绘制红点的画笔
		Paint paint = new Paint();
		paint.setColor(Color.RED);
		paint.setStyle(Paint.Style.FILL);

		// 模拟点击事件
		long downTime = System.currentTimeMillis();
		long eventTime = System.currentTimeMillis();
		int action = MotionEvent.ACTION_DOWN;

		MotionEvent motionEvent = MotionEvent.obtain(
				downTime,
				eventTime,
				action,
				x,
				y,
				0
		);
		view.dispatchTouchEvent(motionEvent);

		action = MotionEvent.ACTION_UP;
		eventTime = System.currentTimeMillis();
		motionEvent = MotionEvent.obtain(
				downTime,
				eventTime,
				action,
				x,
				y,
				0
		);
		view.dispatchTouchEvent(motionEvent);

		motionEvent.recycle();
	}
}
