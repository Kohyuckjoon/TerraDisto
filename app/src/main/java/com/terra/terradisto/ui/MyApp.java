package com.terra.terradisto.ui;

import android.app.Application;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;

import ch.leica.sdk.ErrorHandling.IllegalArgumentCheckedException;
import ch.leica.sdk.LeicaSdk;

public class MyApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        // commands.json 은 assets/에 존재해야 함
        LeicaSdk.InitObject initObject = new LeicaSdk.InitObject("commands.json");
        try {
            LeicaSdk.init(getApplicationContext(), initObject);                       // SDK 초기화
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentCheckedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LeicaSdk.setLogLevel(Log.VERBOSE);                                        // 로그 레벨(옵션)
        LeicaSdk.setLicenses(new AppLicenses().keys);                             // 라이선스 주입
        // 주: Receivers 등록/해제는 화면(Activity/Fragment 생명주기에 맞추는 것을 권장
    }
}
