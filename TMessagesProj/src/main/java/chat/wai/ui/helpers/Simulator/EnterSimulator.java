package chat.wai.ui.helpers.Simulator;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;

public class EnterSimulator {
	public void performEnter(View view) {
		long downTime = SystemClock.uptimeMillis();
		long eventTime = SystemClock.uptimeMillis();
		int action = KeyEvent.ACTION_DOWN;

		KeyEvent keyEvent = new KeyEvent(downTime, eventTime, action, KeyEvent.KEYCODE_ENTER, 0);
		view.dispatchKeyEvent(keyEvent);

		action = KeyEvent.ACTION_UP;
		eventTime = SystemClock.uptimeMillis();
		keyEvent = new KeyEvent(downTime, eventTime, action, KeyEvent.KEYCODE_ENTER, 0);
		view.dispatchKeyEvent(keyEvent);
	}
}
