package chat.wai.ui.helpers.Simulator;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;

public class BackspaceSimulator {
	public void performBackspace(View view) {
		long downTime = SystemClock.uptimeMillis();
		long eventTime = SystemClock.uptimeMillis();
		int action = KeyEvent.ACTION_DOWN;

		KeyEvent keyEvent = new KeyEvent(downTime, eventTime, action, KeyEvent.KEYCODE_DEL, 0);
		view.dispatchKeyEvent(keyEvent);

		action = KeyEvent.ACTION_UP;
		eventTime = SystemClock.uptimeMillis();
		keyEvent = new KeyEvent(downTime, eventTime, action, KeyEvent.KEYCODE_DEL, 0);
		view.dispatchKeyEvent(keyEvent);
	}
}
