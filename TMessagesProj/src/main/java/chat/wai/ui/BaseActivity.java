/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package chat.wai.ui;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.webkit.WebView;

import androidx.annotation.CallSuper;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationCenter;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.BasePermissionsActivity;

public class BaseActivity extends BaseFragment{
    public SharedPreferences getPreferences(){
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("ac_config", Activity.MODE_PRIVATE);
        return preferences;
    }

    public void onWebViewPageFinished(WebView view, String url){

    }
    public void showLoading(boolean loading){

    }
    public void showQrcodeView(){

    }
    public void showQrcodeScanView(){

    }
    public boolean requestPermission(){
        boolean res = true;
        if (Build.VERSION.SDK_INT >= 23 && getParentActivity() != null) {

            if ( getParentActivity().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                getParentActivity().requestPermissions(new String[]{Manifest.permission.CAMERA}, 19);
                return false;
            }
            if ( getParentActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                getParentActivity().requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, BasePermissionsActivity.REQUEST_CODE_EXTERNAL_STORAGE);
                return false;
            }
            if (getParentActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                getParentActivity().requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 3);
                return false;
            }
        }
        return res;
    }


}
