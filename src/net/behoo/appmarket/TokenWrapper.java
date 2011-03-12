package net.behoo.appmarket;

import behoo.providers.BehooProvider;
import android.content.Context;
import android.database.Cursor;

public class TokenWrapper {
	public static String getToken(Context context) throws Throwable {
		String token = null;
		try {
			Cursor cursor = context.getContentResolver().query(BehooProvider.TOKEN_CONTENT_URI, 
				null, null, null, null);
			cursor.moveToFirst();		
			token = cursor.getString(0);
		} catch (Throwable tr) {
			tr.printStackTrace();
			throw tr;
		}
		
		return token;
	}
}
