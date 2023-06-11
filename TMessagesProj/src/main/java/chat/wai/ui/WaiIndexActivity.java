/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package chat.wai.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.core.graphics.ColorUtils;
import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Intro;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.BasePermissionsActivity;
import org.telegram.ui.Components.ImageUpdater;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SimpleThemeDescription;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import chat.wai.ui.components.IWebView;
import chat.wai.ui.helpers.WaiUtils;

public class WaiIndexActivity extends BaseActivity implements NotificationCenter.NotificationCenterDelegate{
    private IWebView webView;
    private String currentUrl;

    private AlertDialog loadingDialog;
    @Override
    public boolean onFragmentCreate() {

        return true;
    }
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    public View createView(Context context) {
        requestPermission();
        parentLayout.getDrawerLayoutContainer().setAllowOpenDrawer(false,false);
        actionBar.setAddToContainer(false);
        String v = "";
        try {
            InputStream inputStream = ApplicationLoader.applicationContext.getAssets().open("wai/m/version.txt");
            v = WaiUtils.convertInputStreamToString(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String theme = Theme.isCurrentThemeDay() ? "light" :"dark";

        currentUrl = String.format("https://%s/m/android?v=%s&theme=%s",BuildVars.FRONT_END_HOST,v,theme);
        if (BuildVars.DEBUG_VERSION) {
            FileLog.d(currentUrl);
        }

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;


        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.onActivityResultReceived);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.needSetDayNightTheme);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.waiAppInit);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.suggestedLangpack);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.didSetPasscode);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.configLoaded);
        ConnectionsManager.getInstance(currentAccount).updateDcSettings();

        updateColors(false);
        webView = new IWebView(context);
        webView.loadUrl(currentUrl);
        webView.setActivity(this);

        frameLayout.addView(webView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT,0, AndroidUtilities.dp(14),0,0));

        loadingDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        loadingDialog.setOnCancelListener(dialog -> loadingDialog = null);
        loadingDialog.show();
        return fragmentView;
    }

    @Override
    public void showLoading(boolean loading){
        if(loadingDialog != null){
            if(loading){
                loadingDialog.show();
            }else{
                loadingDialog.dismiss();
            }
        }
    }
    public static boolean supportWebview() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if ("samsung".equals(manufacturer)) {
            if ("GT-I9500".equals(model)) {
                return false;
            }
        }
        return true;
    }
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onResume() {
        super.onResume();
        if (!AndroidUtilities.isTablet()) {
            Activity activity = getParentActivity();
            if (activity != null) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (!AndroidUtilities.isTablet()) {
            Activity activity = getParentActivity();
            if (activity != null) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        }
    }
    @Override
    public boolean onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
            return false;
        } else {
            return super.onBackPressed();
        }
    }

    @Override
    public boolean hasForceLightStatusBar() {
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();

        webView.setLayerType(View.LAYER_TYPE_NONE, null);
        try {
            if(loadingDialog != null){
                loadingDialog.dismiss();
                loadingDialog = null;
            }

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
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.onActivityResultReceived);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.needSetDayNightTheme);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.waiAppInit);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.didSetPasscode);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.suggestedLangpack);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.configLoaded);
    }

    public void alert(String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Alert");
        builder.setMessage(message);
        showDialog(builder.create());
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.suggestedLangpack || id == NotificationCenter.configLoaded) {}

        if(id == NotificationCenter.didSetPasscode){

        }
        if (id == NotificationCenter.onActivityResultReceived) {
            webView.onActivityResult((int) args[0], (int) args[1], (Intent) args[2]);
        }
        if(id == NotificationCenter.needSetDayNightTheme){
            webView.postEvent("setIsCurrentThemeLight",Theme.isCurrentThemeDay());
        }
        if(id == NotificationCenter.waiAppInit){

        }
    }
    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        return SimpleThemeDescription.createThemeDescriptions(() -> updateColors(true), Theme.key_windowBackgroundWhite,
                Theme.key_windowBackgroundWhiteBlueText4, Theme.key_chats_actionBackground, Theme.key_chats_actionPressedBackground,
                Theme.key_featuredStickers_buttonText, Theme.key_windowBackgroundWhiteBlackText, Theme.key_windowBackgroundWhiteGrayText3
        );
    }

    private void updateColors(boolean fromTheme) {
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        if (fromTheme) {
        } else Intro.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
    }

    public WaiIndexActivity setOnLogout() {
        return this;
    }

    @Override
    public boolean isLightStatusBar() {
        int color = Theme.getColor(Theme.key_windowBackgroundWhite, null, true);
        return ColorUtils.calculateLuminance(color) > 0.7f;
    }

}
