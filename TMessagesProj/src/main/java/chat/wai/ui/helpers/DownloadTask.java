package chat.wai.ui.helpers;
import android.os.AsyncTask;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadTask extends AsyncTask<String, Void, String> {

	private final DownloadListener downloadListener;

	public interface DownloadListener {
		void onDownloadCompleted(String content);
		void onDownloadFailed(String errorMessage);
	}

	public DownloadTask(DownloadListener listener) {
		downloadListener = listener;
	}

	@Override
	protected String doInBackground(String... urls) {
		String fileUrl = urls[0];

		try {
			URL url = new URL(fileUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.connect();

			InputStream inputStream = connection.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			StringBuilder content = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {
				content.append(line).append("\n");
			}

			reader.close();
			inputStream.close();

			return content.toString();
		} catch (IOException e) {
			return "Download failed: " + e.getMessage();
		}
	}

	@Override
	protected void onPostExecute(String result) {
		if (downloadListener != null) {
			if (result.startsWith("Download failed")) {
				downloadListener.onDownloadFailed(result);
			} else {
				downloadListener.onDownloadCompleted(result);
			}
		}
	}
}

