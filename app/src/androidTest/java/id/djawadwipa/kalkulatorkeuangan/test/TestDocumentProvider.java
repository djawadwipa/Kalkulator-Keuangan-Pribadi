package id.djawadwipa.kalkulatorkeuangan.test;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

public final class TestDocumentProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return "application/octet-stream";
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment == null) {
            throw new FileNotFoundException("Nama dokumen tes tidak tersedia");
        }

        String sanitized = lastPathSegment.replaceAll("[^A-Za-z0-9._-]", "_");
        String fileName = sanitized.substring(0, Math.min(120, sanitized.length()));
        if (fileName.isEmpty()) {
            throw new FileNotFoundException("Nama dokumen tes tidak valid");
        }

        Context providerContext = getContext();
        if (providerContext == null) {
            throw new FileNotFoundException("Context provider tes tidak tersedia");
        }

        File root = new File(providerContext.getCacheDir(), "backup-smoke-test");
        if (!root.isDirectory() && !root.mkdirs()) {
            throw new FileNotFoundException("Direktori dokumen tes tidak dapat dibuat");
        }

        File file = new File(root, fileName);
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode));
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
