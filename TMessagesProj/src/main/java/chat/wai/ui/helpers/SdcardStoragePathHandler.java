package chat.wai.ui.helpers;

import android.webkit.MimeTypeMap;
import android.webkit.WebResourceResponse;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.WebViewAssetLoader;

import java.io.FileInputStream;

public class SdcardStoragePathHandler implements WebViewAssetLoader.PathHandler {
	@Nullable
	@Override
	public WebResourceResponse handle(@NonNull String filePath) {
		String extension = MimeTypeMap.getFileExtensionFromUrl(filePath);
		String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		try {
			return new WebResourceResponse(mimeType, "UTF-8", new FileInputStream(filePath));
		} catch (Exception e) {
			return null;
		}
	}
}