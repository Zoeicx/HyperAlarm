package com.hyperalarm.home;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Base64;
import android.widget.Toast;
import com.hyperalarm.R;

import com.hyperalarm.common.utils.RootShell;
import com.hyperalarm.alarm.core.ScriptPath;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import com.hyperalarm.common.utils.ThreadUtils;

public class ScriptImportActivity extends Activity {
    private static final String DIR = "/data/adb/alarm_shell/scripts";
    private static final int REQUEST_TREE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_TREE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_TREE || resultCode != RESULT_OK || data == null) {
            finish();
            return;
        }
        Uri tree = data.getData();
        if (tree == null) {
            finish();
            return;
        }
        // Refuse only when the probe has already answered "no": an unanswered probe must not turn
        // the first import into a false failure.
        if (RootShell.wasRootProbed() && !RootShell.isRoot()) {
            Toast.makeText(this, R.string.root_required, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        ThreadUtils.runOnBackground(() -> {
            int[] result = copyScripts(tree);
            int count = result[0];
            int skipped = result[1];
            String text;
            if (count > 0 && skipped > 0) {
                text = getString(R.string.script_import_done_skipped, count, skipped);
            } else if (count > 0) {
                text = getString(R.string.script_import_done, count);
            } else if (skipped > 0) {
                text = getString(R.string.script_import_none_skipped, skipped);
            } else {
                text = getString(R.string.script_import_none);
            }
            runOnUiThread(() -> {
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    /** @return {@code [imported, skipped]}; a skipped file had a display name the scripts directory cannot accept. */
    private int[] copyScripts(Uri tree) {
        int count = 0;
        int skipped = 0;
        try {
            String treeId = DocumentsContract.getTreeDocumentId(tree);
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, treeId);
            try (Cursor c = getContentResolver().query(children, new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null)) {
                if (c == null) return new int[]{0, 0};
                while (c.moveToNext()) {
                    String id = c.getString(0);
                    String name = c.getString(1);
                    String mime = c.getString(2);
                    if (name == null || !name.endsWith(".sh")
                            || DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) continue;
                    // The display name comes from a foreign DocumentsProvider and it may differ
                    // between the listing and the read, so only a plain file name is accepted.
                    // Without this a provider could answer a benign "a.sh" and hand back
                    // "../../data/adb/alarm_shell/scripts/x.sh", turning the import into an
                    // arbitrary path write with root.
                    if (!ScriptPath.isPlainName(name)) {
                        skipped++;
                        continue;
                    }
                    Uri file = DocumentsContract.buildDocumentUriUsingTree(tree, id);
                    byte[] bytes = read(file);
                    if (bytes == null) continue;
                    String path = DIR + "/" + name;
                    if (RootShell.writeFileFromBase64(path, Base64.encodeToString(bytes, Base64.NO_WRAP))) {
                        count++;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return new int[]{count, skipped};
    }

    private byte[] read(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) return null;
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }
}
