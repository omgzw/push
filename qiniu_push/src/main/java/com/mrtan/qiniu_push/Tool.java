package com.mrtan.qiniu_push;

import android.app.Activity;
import android.content.Context;
import com.alibaba.fastjson.JSONObject;
import com.hjq.permissions.OnPermission;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import java.util.List;

public class Tool extends WXSDKEngine.DestroyableModule {
  @JSMethod(uiThread = true)
  public void checkCallPhonePermission(final JSCallback callback) {
    final JSONObject object = new JSONObject();
    Context context = this.mWXSDKInstance.getContext();
    if (XXPermissions.isHasPermission(context, "android.permission.CALL_PHONE")) {
      object.put("code", 1);
      object.put("msg", "已授权");
      if (callback != null) {
        callback.invokeAndKeepAlive(object);
      }
    } else {
      XXPermissions.with((Activity)context).permission("android.permission.CALL_PHONE").request(new OnPermission() {
            public void hasPermission(List<String> param1List, boolean param1Boolean) {
              object.put("code", 1);
              object.put("msg", "已授权");
              if (callback != null)
                callback.invokeAndKeepAlive(object); 
            }
            
            public void noPermission(List<String> param1List, boolean param1Boolean) {
              object.put("code", 0);
              object.put("msg", "未授权");
              if (callback != null)
                callback.invokeAndKeepAlive(object); 
            }
          });
    } 
  }
  
  @JSMethod(uiThread = true)
  public void checkCamera(final JSCallback callback) {
    final JSONObject object = new JSONObject();
    Context context = this.mWXSDKInstance.getContext();
    if (XXPermissions.isHasPermission(context, "android.permission.CAMERA")) {
      object.put("code", 1);
      object.put("msg", "已授权");
      if (callback != null) {
        callback.invokeAndKeepAlive(object);
      }
    } else {
      XXPermissions.with((Activity)context).permission("android.permission.CAMERA").request(new OnPermission() {
            public void hasPermission(List<String> param1List, boolean param1Boolean) {
              object.put("code", 1);
              object.put("msg", "已授权");
              if (callback != null)
                callback.invokeAndKeepAlive(object); 
            }
            
            public void noPermission(List<String> param1List, boolean param1Boolean) {
              object.put("code", 0);
              object.put("msg", "未授权");
              if (callback != null)
                callback.invokeAndKeepAlive(object); 
            }
          });
    } 
  }
  
  @JSMethod(uiThread = true)
  public void checkLoaction(final JSCallback callback) {
    final JSONObject object = new JSONObject();
    Context context = this.mWXSDKInstance.getContext();
    if (XXPermissions.isHasPermission(context, Permission.Group.LOCATION)) {
      object.put("code", 1);
      object.put("msg", "已授权");
      if (callback != null) {
        callback.invokeAndKeepAlive(object);
      }
    } else {
      XXPermissions.with((Activity)context).permission(Permission.Group.LOCATION).request(new OnPermission() {
            public void hasPermission(List<String> param1List, boolean param1Boolean) {
              object.put("code", 1);
              object.put("msg", "已授权");
              if (callback != null)
                callback.invokeAndKeepAlive(object); 
            }
            
            public void noPermission(List<String> param1List, boolean param1Boolean) {
              object.put("code", 0);
              object.put("msg", "未授权");
              if (callback != null)
                callback.invokeAndKeepAlive(object); 
            }
          });
    } 
  }
  
  @JSMethod(uiThread = true)
  public void checkMicrophone(final JSCallback callback) {
    final JSONObject object = new JSONObject();
    Context context = this.mWXSDKInstance.getContext();
    if (XXPermissions.isHasPermission(context, "android.permission.RECORD_AUDIO")) {
      object.put("code", 1);
      object.put("msg", "已授权");
      if (callback != null) {
        callback.invokeAndKeepAlive(object);
      }
    } else {
      XXPermissions.with((Activity)context).permission("android.permission.RECORD_AUDIO").request(new OnPermission() {
            public void hasPermission(List<String> param1List, boolean param1Boolean) {
              object.put("code", 1);
              object.put("msg", "已授权");
              if (callback != null)
                callback.invokeAndKeepAlive(object); 
            }
            
            public void noPermission(List<String> param1List, boolean param1Boolean) {
              object.put("code", 0);
              object.put("msg", "未授权");
              if (callback != null)
                callback.invokeAndKeepAlive(object); 
            }
          });
    } 
  }
  
  @JSMethod(uiThread = true)
  public void checkPhotos(final JSCallback callback) {
    Context context = this.mWXSDKInstance.getContext();
    final JSONObject object = new JSONObject();
    if (XXPermissions.isHasPermission(context, "android.permission.READ_EXTERNAL_STORAGE")) {
      object.put("code", 1);
      object.put("msg", "已授权");
      if (callback != null) {
        callback.invokeAndKeepAlive(object);
      }
    } else {
      XXPermissions.with((Activity)context).permission("android.permission.READ_EXTERNAL_STORAGE").request(new OnPermission() {
            public void hasPermission(List<String> param1List, boolean param1Boolean) {
              object.put("code", 1);
              object.put("msg", "已授权");
              if (callback != null)
                callback.invokeAndKeepAlive(object); 
            }
            
            public void noPermission(List<String> param1List, boolean param1Boolean) {
              object.put("code", 0);
              object.put("msg", "未授权");
              if (callback != null)
                callback.invokeAndKeepAlive(object); 
            }
          });
    } 
  }

  @Override
  public void destroy() {}
  
  @JSMethod(uiThread = true)
  public void goSettingPage() {
    XXPermissions.gotoPermissionSettings(this.mWXSDKInstance.getContext(), true);
  }
}