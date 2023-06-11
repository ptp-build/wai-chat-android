package chat.wai.ui.components;

import static chat.wai.ui.helpers.WaiUtils.convertInputStreamToString;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
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
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import chat.wai.ui.BaseActivity;
import chat.wai.ui.WaiIndexActivity;
import chat.wai.ui.pages.chatgpt.ChatGptBotActivity;
import chat.wai.ui.helpers.Simulator.BackspaceSimulator;
import chat.wai.ui.helpers.Simulator.EnterSimulator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressLint("ViewConstructor")
public class IWebView extends WebView {
  String userAgent_IOS = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Safari/604.1";
  private ValueCallback<Uri[]> mFilePathCallback;
  public static boolean loaded = false;
  private BaseActivity activity;
  private final static int REQUEST_CODE_WEB_VIEW_FILE = 3000;

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
      FileLog.d(String.format("[MOBILE.NOTIFY] eventName:%s",eventName));
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("eventName",eventName);
      jsonObject.put("eventData",eventData);
      String code = "document.dispatchEvent(new CustomEvent('MOBILE.NOTIFY', { detail: "+jsonObject.toString()+" }))";
      runJsCode(code);
    } catch (Exception e) {
      FileLog.e(e);
    }
  }

  public void setActivity(BaseActivity waiIndexActivity) {
    activity = waiIndexActivity;
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


  private String getFileBase64ContentFromAssets(String fileName) {
    try {
      InputStream inputStream = ApplicationLoader.applicationContext.getAssets().open(fileName);
      byte[] buffer = new byte[inputStream.available()];
      inputStream.read(buffer);
      inputStream.close();
      return Base64.encodeToString(buffer, Base64.DEFAULT);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
  }
  public String getFileRunCode(String fileName) {
    String data = getFileBase64ContentFromAssets(fileName);
    return "javascript:(function(){ eval(atob('" + data + "'));})()";
  }

  public void onWebPageFinished(WebView view, String url) {
    view.loadUrl(getFileRunCode("zepto.js"));
  }
  public void reloadWebView(){
    this.loadUrl("javascript:window.location.reload( )");
  }
  @SuppressLint("SetTextI18n")
  public void handleInput(String text){
    runJsCode("$('textarea').val('"+text+"')");
    EnterSimulator enterSimulator = new EnterSimulator();
    BackspaceSimulator backspaceSimulator = new BackspaceSimulator();
    enterSimulator.performEnter(this);
    backspaceSimulator.performBackspace(this);
    runJsCode("setTimeout(()=>{$('textarea').next().trigger('click')},100)");
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
          activity.showLoading(false);
          NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.waiAppInit);
          break;
        case "WRITE_INPUT":
          handleInput(object.getString("text"));
          break;
        case "OPEN_BROWSER":
          if(!object.getBoolean("outerBrowser")){
            activity.presentFragment(new ChatGptBotActivity(object.getString("url"),object.getString("title")));
          }
          break;
        case "SCAN_QRCODE":
          activity.showQrcodeScanView();
          break;
        case "WEBVIEW_LOADING":
          activity.showLoading(true);
          break;
        default:
          break;
      }

    } catch (Throwable ignore) {}
  }

  @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
  public IWebView(Context context) {
    super(context);
    WebSettings webSettings = getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setDomStorageEnabled(true);
    //webSettings.setUserAgentString(userAgent_IOS);

    webSettings.setAllowFileAccess(true);
    webSettings.setAllowContentAccess(false);
    webSettings.setAllowFileAccessFromFileURLs(true);
    webSettings.setAllowUniversalAccessFromFileURLs(true);
    webSettings.setMediaPlaybackRequiresUserGesture(false);
    addJavascriptInterface(new WebViewProxy(), "WaiBridge");

    setOnLongClickListener(new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View v) {
        reload();
        return true;
      }
    });
    setLayerType(View.LAYER_TYPE_HARDWARE, null);
    if (Build.VERSION.SDK_INT >= 26) {
      webSettings.setSafeBrowsingEnabled(false);
    }
    if (Build.VERSION.SDK_INT >= 21) {
      webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
      CookieManager cookieManager = CookieManager.getInstance();
      cookieManager.setAcceptThirdPartyCookies(this, true);
    }
    setWebChromeClient(new WebChromeClient() {
      @Override
      public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        if(!activity.requestPermission()){
          return false;
        }
        if (mFilePathCallback != null) {
          mFilePathCallback.onReceiveValue(null);
        }

        mFilePathCallback = filePathCallback;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          activity.startActivityForResult(fileChooserParams.createIntent(), REQUEST_CODE_WEB_VIEW_FILE);
        } else {
          Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
          intent.addCategory(Intent.CATEGORY_OPENABLE);
          intent.setType("*/*");
          activity.startActivityForResult(Intent.createChooser(intent, LocaleController.getString(R.string.BotWebViewFileChooserTitle)), REQUEST_CODE_WEB_VIEW_FILE);
        }

        return true;
      }

    });
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
        }else if (!path.endsWith(".html")
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
        }else if (path.startsWith("/static/wai-chat-bot")) {

          OkHttpClient client = new OkHttpClient();
          String url1 = "http://192.168.43.244:3100" + path;
          FileLog.d(String.format("[WAI] redirect %s", url1));
          Request httpRequest = new Request.Builder()
                  .url(url1)
                  .build();

          Response httpResponse = null;
          try {

            httpResponse = client.newCall(httpRequest).execute();
            InputStream responseStream = Objects.requireNonNull(httpResponse.body()).byteStream();
            // Create a WebResourceResponse with the response body and content type
            String contentType = httpResponse.header("Content-Type");
            return new WebResourceResponse(contentType, null, responseStream);
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
        activity.onWebViewPageFinished(view,url);
        loaded = true;
      }
    });
  }

  public void setJavaScriptEnabled(boolean enabled) {
    getSettings().setJavaScriptEnabled(enabled);
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE_WEB_VIEW_FILE && mFilePathCallback != null) {
      Uri[] results = null;

      if (resultCode == Activity.RESULT_OK) {
        if (data != null && data.getDataString() != null) {
          results = new Uri[] {Uri.parse(data.getDataString())};
        }
      }

      mFilePathCallback.onReceiveValue(results);
      mFilePathCallback = null;
    }
  }

}
