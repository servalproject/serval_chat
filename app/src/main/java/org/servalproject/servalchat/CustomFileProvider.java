package org.servalproject.servalchat;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.servalproject.mid.Serval;
import org.servalproject.servaldna.ContentType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Created by jeremy on 28/03/18.
 */

public class CustomFileProvider extends ContentProvider {
	@Override
	public boolean onCreate() {
		return true;
	}

	private static final String TAG = "FileProvider";
	private static final String[] COLUMNS = {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE };
	private static final String AUTHORITY="org.servalproject.fileprovider";
	public static final String APK_NAME="ServalChat.apk";

	public static Uri forInstanceFile(Serval serval, File file) throws IOException {
		String base = serval.instancePath.getCanonicalPath();
		File canonical = file.getCanonicalFile();
		if (!canonical.exists())
			throw new FileNotFoundException();
		String instanceFile = canonical.getAbsolutePath();
		if (!instanceFile.startsWith(base))
			throw new IllegalArgumentException(instanceFile+" is not within "+base);
		return forPath("instance/"+instanceFile.substring(base.length()));
	}

	public static Uri forPath(String path){
		return new Uri.Builder().scheme("content")
				.authority(AUTHORITY).encodedPath(path).build();
	}

	private File getFileForUri(Uri uri) throws FileNotFoundException{
		List<String> folders = uri.getPathSegments();
		File file;
		Serval serval = Serval.getInstance();
		switch (folders.get(0)){
			case "instance":
				file = serval.instancePath;
				for(int i=1;i<folders.size();i++)
					file = new File(file, folders.get(i));
				return file;
			case APK_NAME:
				return serval.apkFile;
		}
		throw new FileNotFoundException();
	}

	@Nullable
	@Override
	public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
		if (!"r".equals(mode))
			throw new IllegalArgumentException("Invalid mode: "+mode);
		File file = getFileForUri(uri);
		return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
	}

	@Nullable
	@Override
	public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
		// Copied from android support's FileProvider
		File file = null;
		try {
			file = getFileForUri(uri);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e);
		}
		if (projection == null) {
			projection = COLUMNS;
		}
		MatrixCursor cursor = new MatrixCursor(projection, 1);
		Object[] values = new Object[projection.length];
		for (int i=0;i<projection.length;i++) {
			if (OpenableColumns.DISPLAY_NAME.equals(projection[i])) {
				values[i] = file.getName();
			} else if (OpenableColumns.SIZE.equals(projection[i])) {
				values[i] = file.length();
			}
		}
		cursor.addRow(values);
		return cursor;
	}

	@Nullable
	@Override
	public String getType(@NonNull Uri uri) {
		String ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
		if ("apk".equals(ext))
			return "image/apk";
		String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
		if (type == null || "".equals(type))
			type = ContentType.applicationOctetStream.toString();
		return type;
	}

	@Nullable
	@Override
	public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
		throw new UnsupportedOperationException();
	}
}
