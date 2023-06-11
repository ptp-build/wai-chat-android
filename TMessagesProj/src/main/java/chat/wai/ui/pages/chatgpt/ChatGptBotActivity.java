package chat.wai.ui.pages.chatgpt;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewParent;
import android.webkit.WebView;
import android.widget.FrameLayout;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.ContextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SizeNotifierFrameLayout;

import java.util.ArrayList;

import chat.wai.config.QrCodeTypeConstant;
import chat.wai.ui.BaseActivity;
import chat.wai.ui.components.IWebView;
import chat.wai.ui.pages.CameraScanActivity;
import chat.wai.ui.pages.QrActivity;

public class ChatGptBotActivity extends BaseActivity implements NotificationCenter.NotificationCenterDelegate {

  private ContextProgressView progressView;
  private ActionBarMenuItem progressItem;
  private final static int share = 1;
  private final static int auth_button = 201;
  private final static int refresh_button = 202;
  private IWebView webView;
  private String url;
  private String title = "-";
  private final static int id_chat_compose_panel = 1000;
  public final static int ENTER_VIEW_HEIGHT_DP = 51;
  @Override
  public void didReceivedNotification(int id, int account, Object... args) {

  }

  public ChatGptBotActivity() {
    super();
  }

  public void showLoading(boolean loading){
    progressView.setVisibility(loading ? View.VISIBLE:View.GONE);
  }
  public void showQrcodeView(){

  }
  public void showQrcodeScanView(){
    if (Build.VERSION.SDK_INT >= 23 && getParentActivity().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      getParentActivity().requestPermissions(new String[]{Manifest.permission.CAMERA}, 22);
      return;
    }
    CameraScanActivity fragment = new CameraScanActivity(CameraScanActivity.TYPE_QR);
    fragment.setNeedGalleryButton(true);
    fragment.setDelegate(new CameraScanActivity.CameraScanActivityDelegate() {
      @Override
      public void didFindQr(String result) {
        FileLog.d(String.format("[didFindQr] %s",result));
        AndroidUtilities.runOnUIThread(() -> {
          JSONObject jsonObject = new JSONObject();
          try {
            jsonObject.put("result",result);
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
          webView.postEvent("SCAN_QRCODE_RESULT",jsonObject);
//          NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.scanResult, result);
        }, 300);
      }
    });
    presentFragment(fragment);

    FileLog.d("showQrcodeView");
    Bundle args = new Bundle();
    args.putBoolean("showsEditSharePwdButton", false);
    args.putInt("shareQrCodeType", QrCodeTypeConstant.QRCODE_TYPE_MNEMONIC_SHARE);
    args.putString("shareTxt", "扫描二维码签名");
    presentFragment(new QrActivity(args));
  }

  @Override
  public void onWebViewPageFinished(WebView view, String url){
    if(url.startsWith("https://chat.openai.com/")){
      view.loadUrl(webView.getFileRunCode("chatgpt.js"));
    }

    if (progressView != null && progressView.getVisibility() == View.VISIBLE) {
      AnimatorSet animatorSet = new AnimatorSet();
      animatorSet.playTogether(
              ObjectAnimator.ofFloat(progressView, "scaleX", 1.0f, 0.1f),
              ObjectAnimator.ofFloat(progressView, "scaleY", 1.0f, 0.1f),
              ObjectAnimator.ofFloat(progressView, "alpha", 1.0f, 0.0f));
      animatorSet.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animator) {
          progressItem.setVisibility(View.GONE);
        }
      });
      animatorSet.setDuration(150);
      animatorSet.start();
    }
  }
  public ChatGptBotActivity(String url, String title) {
    super();
    this.url = url;
    this.title = title;
  }
  @Override
  public boolean onFragmentCreate() {
    super.onFragmentCreate();
    return true;
  }
  @Override
  public void onFragmentDestroy() {
    super.onFragmentDestroy();
    webView.setLayerType(View.LAYER_TYPE_NONE, null);
    try {
      ViewParent parent = webView.getParent();
      if (parent != null) {
        ((FrameLayout) parent).removeView(webView);
      }
      webView.stopLoading();
      webView.loadUrl("about:blank");
      webView.destroy();
      webView = null;
    } catch (Exception e) {
      FileLog.e(e);
    }
  }


  @Override
  public View createView(Context context) {

    webView = new IWebView(context);
    webView.setActivity(this);
    actionBar.setTitle(title);
    actionBar.setBackButtonImage(R.drawable.ic_ab_back);
    actionBar.setAllowOverlayTitle(false);

    actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
      @Override
      public void onItemClick(int id) {
        if (id == -1) {
          finishFragment();
        } else if (id== auth_button){
          webView.postEvent("SHOW_WAI_BOT_SIGN_AUTH_MODAL",null);
        }else if (id== refresh_button){
          progressItem.setVisibility(View.VISIBLE);
          webView.reloadWebView();
        }
      }
    });
    ActionBarMenu menu = actionBar.createMenu();

    progressItem = menu.addItemWithWidth(share, R.drawable.share, AndroidUtilities.dp(54));

    ActionBarMenuItem menuItem = menu.addItem(0, R.drawable.ic_ab_other);
    menuItem.addSubItem(refresh_button,"刷新");
    menuItem.addSubItem(auth_button,"授权");

    progressView = new ContextProgressView(context, 3);
    progressItem.addView(progressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    progressView.setAlpha(1.0f);
    progressView.setScaleX(1.0f);
    progressView.setScaleY(1.0f);
    progressView.setVisibility(View.VISIBLE);
    progressItem.getContentView().setVisibility(View.GONE);
    progressItem.setEnabled(false);

    fragmentView = new FrameLayout(context);
    FrameLayout frameLayout = new FrameLayout(context);

    SizeNotifierFrameLayout keyboardFrameLayout =  new SizeNotifierFrameLayout(context) {
      @Override
      protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int frameBottom;
        webView.layout(0, 0,
                getMeasuredWidth(), frameBottom = getMeasuredHeight());
        frameLayout.layout(0, frameBottom, getMeasuredWidth(), getMeasuredHeight());
      }

      @Override
      protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec), height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
        int frameHeight = height;
        frameHeight -= AndroidUtilities.dp(ENTER_VIEW_HEIGHT_DP);
        webView.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(frameHeight, MeasureSpec.EXACTLY)
        );

        frameLayout.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(ENTER_VIEW_HEIGHT_DP), MeasureSpec.EXACTLY)
        );
      }
    };

    if(null != url){
      webView.loadUrl(url);
//
//      String content = IWebView.getHtmlContent("{}");
//      String baseUrl = BuildVars.BASE_URL;
//      String historyUrl = BuildVars.HISTORY_URL;
//      webView.loadDataWithBaseURL(baseUrl, content, "text/html", "UTF-8", historyUrl);
    }



    keyboardFrameLayout.addView(frameLayout);
    keyboardFrameLayout.addView(webView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT, Gravity.TOP,0,0,0,0));


    fragmentView = keyboardFrameLayout;
    fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
    return fragmentView;
  }

  @Override
  public void onPause() {
    super.onPause();
  }
  @Override
  public void onResume() {
    super.onResume();
    AndroidUtilities.requestAdjustResize(getParentActivity(), classGuid);
  }

  @Override
  public ArrayList<ThemeDescription> getThemeDescriptions() {
    ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

    themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND | ThemeDescription.FLAG_CHECKTAG, null, null, null, null, Theme.key_windowBackgroundWhite));
    themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND | ThemeDescription.FLAG_CHECKTAG, null, null, null, null, Theme.key_windowBackgroundGray));

    themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
    themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
    themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
    themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCH, null, null, null, null, Theme.key_actionBarDefaultSearch));
    themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCHPLACEHOLDER, null, null, null, null, Theme.key_actionBarDefaultSearchPlaceholder));

    themeDescriptions.add(new ThemeDescription(progressView, 0, null, null, null, null, Theme.key_contextProgressInner4));
    themeDescriptions.add(new ThemeDescription(progressView, 0, null, null, null, null, Theme.key_contextProgressOuter4));

    return themeDescriptions;
  }

  @Override
  public boolean onBackPressed() {
    finishFragment();
    return true;
  }
}
