package com.raccoonfink.imagepicker;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

public class ImagePickerPlugin extends CordovaPlugin {
	private static final String TAG = "ImagePickerPlugin";
	private static final AtomicInteger ATOMIC_REQUEST_CODE = new AtomicInteger(0);
	private Map<Integer,CallbackContext> m_inFlight = new HashMap<Integer,CallbackContext>();

	@Override
	public boolean execute(final String action, final CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
		if ("pick".equals(action)) {
			pickImage(callbackContext);
			return true;
		} else {
			callbackContext.error("Unknown action: " + action);
			return false;
		}
	}

	private void pickImage(CallbackContext context) {
		final int requestCode = ATOMIC_REQUEST_CODE.getAndIncrement();
		m_inFlight.put(requestCode, context);

		final Context applicationContext = cordova.getActivity().getApplicationContext();

		Intent intent = new Intent(applicationContext, ImagePickerPlugin.class);
		if (Build.VERSION.SDK_INT < 19) {
			intent = new Intent();
			intent.setAction(Intent.ACTION_GET_CONTENT);
			intent.setType("image/*");
			cordova.startActivityForResult(this, Intent.createChooser(intent, "Select image "), requestCode);
		} else {
			intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType("image/*");
			cordova.startActivityForResult(this, Intent.createChooser(intent, "Select image "), requestCode);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "Received response: " + data);
		if (m_inFlight.containsKey(requestCode)) {
			try {
				final CallbackContext context = m_inFlight.remove(requestCode);

				if (context == null) {
					Log.w(TAG, "No context associated with request: " + requestCode);
				} else if (resultCode == Activity.RESULT_OK) {
					try {
						final Uri uri = data.getData();
						final String path = getPath(uri);

						final JSONObject ret = new JSONObject();
						ret.put("success", true);
						ret.put("url", uri.toString());
						context.success(ret);
					} catch (final URISyntaxException e) {
						context.error(getError("Unable to resolve path.", resultCode));
					}
				} else {
					context.error(getError("No file chosen.", resultCode));
				}
			} catch (final JSONException e) {
				Log.e(TAG, "Unknown JSON error.", e);
			}
		} else {
			Log.w(TAG, "Received result from request " + requestCode + " but we never initiated that request.");
		}
	}

	private JSONObject getError(final String message, final int code) throws JSONException {
		Log.e(TAG, message + (code == 0? "":" ("+code+")"));
		final JSONObject ret = new JSONObject();
		ret.put("success", false);
		ret.put("error", message);
		ret.put("errorCode", code);
		return ret;
	}


	private String getPath(final Uri uri) throws URISyntaxException {
		final Context context = cordova.getActivity().getApplicationContext();
		if ("content".equalsIgnoreCase(uri.getScheme())) {
			final String[] projection = { "_data" };
			Cursor cursor = null;

			try {
				cursor = context.getContentResolver().query(uri, projection, null, null, null);
				int column_index = cursor.getColumnIndexOrThrow("_data");
				if (cursor.moveToFirst()) {
					return cursor.getString(column_index);
				}
			} catch (Exception e) {
				Log.e(TAG, "Failed to resolve data URI.", e);
			}
		} else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}
}