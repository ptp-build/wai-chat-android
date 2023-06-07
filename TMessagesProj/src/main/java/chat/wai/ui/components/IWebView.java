package chat.wai.ui.components;

import static chat.wai.ui.helpers.WaiUtils.convertInputStreamToString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.ui.ActionBar.Theme;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@SuppressLint("ViewConstructor")
public class IWebView extends WebView {
  public boolean mPreventParentTouch = false;
  public static boolean loaded = false;

  @Override
  public void onResume() {
    super.onResume();
  }


  public void runJsCode(String code) {
    if (Build.VERSION.SDK_INT >= 21) {
      evaluateJavascript(code, null);
    } else {
      try {
        loadUrl("javascript:" + code);
      } catch (Exception e) {
        FileLog.e(e);
      }
    }
  }

  public void postEvent(String eventName,Object eventData) {
    try {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("eventName",eventName);
      jsonObject.put("eventData",eventData);
      String code = "document.dispatchEvent(new CustomEvent('wai.notify', { detail: "+jsonObject.toString()+" }))";
      runJsCode(code);
    } catch (Exception e) {
      FileLog.e(e);
    }
  }

  private class WebViewProxy {
    @JavascriptInterface
    public void postEvent(final String eventName, final String eventData) {
      FileLog.d(String.format("[postEvent] WebViewProxy eventName: %s,eventData: %s",eventName,eventData));
      AndroidUtilities.runOnUIThread(() -> {
        handleScriptMessage( eventName, eventData);
      });
    }
  }

  public void onWebPageFinished(WebView view, String url) {

  }
  public void reloadWebView(){
    this.loadUrl("javascript:window.location.reload( true )");
  }
  public void handleScriptMessage(final String eventName, final String eventData) {
    try {
      JSONObject object = new JSONObject(eventData);
      switch (eventName) {
        case "SET_THEME":
          Theme.ThemeInfo themeInfo;
          String dayThemeName = "Blue";
          String nightThemeName = "Night";
          if (object.getString("theme").equals("dark")) {
            themeInfo = Theme.getTheme(nightThemeName);
          } else {
            themeInfo = Theme.getTheme(dayThemeName);
          }
          NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme, themeInfo, true, null, -1);
          break;
        case "WAI_APP_INIT":
          NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.waiAppInit);
          break;
        default:
          break;
      }

    } catch (Throwable ignore) {}
  }

  @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
  public IWebView(Context context) {
    super(context);
    getSettings().setJavaScriptEnabled(true);
    getSettings().setDomStorageEnabled(true);

    getSettings().setAllowFileAccess(false);
    getSettings().setAllowContentAccess(false);
    getSettings().setAllowFileAccessFromFileURLs(true);
    getSettings().setAllowUniversalAccessFromFileURLs(false);
    getSettings().setMediaPlaybackRequiresUserGesture(false);
    addJavascriptInterface(new WebViewProxy(), "WaiBridge");

    setLayerType(View.LAYER_TYPE_HARDWARE, null);
    if (Build.VERSION.SDK_INT >= 26) {
      getSettings().setSafeBrowsingEnabled(false);
    }
    if (Build.VERSION.SDK_INT >= 21) {
      getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
      CookieManager cookieManager = CookieManager.getInstance();
      cookieManager.setAcceptThirdPartyCookies(this, true);
    }

    setWebViewClient(new WebViewClient() {
      @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
      @Override
      public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        Uri uri = Uri.parse(url);
        String host = uri.getHost();

        String path = uri.getPath();
        if (path.startsWith("/m/android") && host.contains(BuildVars.FRONT_END_HOST)) {
          try {
            String theme = uri.getQueryParameter("theme");
            String version = uri.getQueryParameter("v");

            InputStream inputStream = ApplicationLoader.applicationContext.getAssets().open("wai/m/index.html");
            String mimeType = getMimeType(url);
            String modifiedHtml = "<script>\n" +
                    "    window.__PLATFORM='android';\n" +
                    "    window.__THEME='"+theme+"';\n" +
                    "    window.__FRONT_VERSION='"+version+"';\n" +
                    "</script>\n";
            modifiedHtml += convertInputStreamToString(inputStream); // Read the original HTML content
            inputStream.close();

            ByteArrayInputStream modifiedInputStream = new ByteArrayInputStream(modifiedHtml.getBytes());
            return new WebResourceResponse(mimeType, null, modifiedInputStream);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        if (!path.endsWith(".html")
                && !path.startsWith("/api")
                && !path.startsWith("/m/android")
                && !path.startsWith("/m/ios")
                && !path.startsWith("/m/img-apple-")
                && host != null
                && host.contains(BuildVars.FRONT_END_HOST)) {
          try {
            // Load the file from assets
            InputStream inputStream = ApplicationLoader.applicationContext.getAssets().open("wai" + path);
            String mimeType = getMimeType(url);
            return new WebResourceResponse(mimeType, null, inputStream);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }

        return super.shouldInterceptRequest(view, request);
      }

      private String getMimeType(String url) {
        String mimeType = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
          mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return mimeType;
      }

      private boolean isInternalUrl(String url) {
        if (TextUtils.isEmpty(url)) {
          return false;
        }
        return false;
      }

      @Override
      public void onLoadResource(WebView view, String url) {
        if (isInternalUrl(url)) {
          return;
        }
        super.onLoadResource(view, url);
      }

      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return isInternalUrl(url) || super.shouldOverrideUrlLoading(view, url);
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        onWebPageFinished(view, url);
        loaded = true;
      }
    });
  }

  public void setJavaScriptEnabled(boolean enabled) {
    getSettings().setJavaScriptEnabled(enabled);
  }


}
