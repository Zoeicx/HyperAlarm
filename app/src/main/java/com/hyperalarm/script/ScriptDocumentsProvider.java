package com.hyperalarm.script;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;

import com.hyperalarm.alarm.core.ScriptPath;
import com.hyperalarm.common.utils.RootShell;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Storage Access Framework view of the root-only script directory.
 *
 * <p>The directory holds scripts that are executed as root, so a document id is treated as untrusted
 * input from the SAF client: only a single plain file name directly inside the directory is accepted.
 * Anything containing a separator, a dot segment or a NUL is refused, and the provider is read only -
 * it never advertises create/delete, because it cannot implement them safely.</p>
 */
public class ScriptDocumentsProvider extends DocumentsProvider {
    public static final String AUTHORITY = "com.hyperalarm.scripts";
    private static final String ROOT_ID = "scripts";
    private static final String DIR = "/data/adb/alarm_shell/scripts";

    private static final String[] DEFAULT_ROOT_PROJECTION = {
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
    };
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS
    };

    @Override public boolean onCreate() { return true; }

    @Override
    public Cursor queryRoots(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(projection == null ? DEFAULT_ROOT_PROJECTION : projection);
        MatrixCursor.RowBuilder row = cursor.newRow();
        row.add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID);
        row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, ROOT_ID);
        row.add(DocumentsContract.Root.COLUMN_TITLE,
                getContext().getString(com.hyperalarm.R.string.script_dir_label));
        // Read only: FLAG_SUPPORTS_CREATE is deliberately absent, createDocument is not implemented.
        row.add(DocumentsContract.Root.COLUMN_FLAGS,
                DocumentsContract.Root.FLAG_LOCAL_ONLY
                        | DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD);
        row.add(DocumentsContract.Root.COLUMN_ICON, 0);
        row.add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, 0);
        return cursor;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        MatrixCursor cursor = new MatrixCursor(projection == null ? DEFAULT_DOCUMENT_PROJECTION : projection);
        String path = requirePath(documentId);
        addDocumentRow(cursor, documentId, new File(path), isDirectory(path));
        return cursor;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        // The script directory is flat on purpose (a label is a plain file name), so only the root
        // can be listed; refusing other parents keeps ids at one level and unvalidatable input out.
        if (!ROOT_ID.equals(parentDocumentId)) throw new FileNotFoundException("unknown parent");

        MatrixCursor cursor = new MatrixCursor(projection == null ? DEFAULT_DOCUMENT_PROJECTION : projection);
        com.topjohnwu.superuser.Shell.Result result = RootShell.run(
                "ls -1 " + RootShell.quote(DIR) + " 2>/dev/null");
        if (result != null) {
            for (String name : result.getOut()) {
                if (name == null) continue;
                String child = name.trim();
                if (!ScriptPath.isPlainName(child)) continue;
                String childPath = DIR + "/" + child;
                addDocumentRow(cursor, ROOT_ID + "/" + child, new File(childPath), isDirectory(childPath));
            }
        }
        return cursor;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, android.os.CancellationSignal signal)
            throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("read only");
        String path = requirePath(documentId);
        File tmp = null;
        try {
            File dir = new File(getContext().getCacheDir(), "documents");
            if (!dir.exists() && !dir.mkdirs()) throw new FileNotFoundException(path);
            tmp = File.createTempFile("script", ".bin", dir);
            // The copy has to be readable by this process but by nobody else: hand it to our own uid
            // and keep the file private, instead of the world-readable default of cp.
            int uid = android.os.Process.myUid();
            com.topjohnwu.superuser.Shell.Result r = RootShell.run(
                    "cp -f " + RootShell.quote(path) + " " + RootShell.quote(tmp.getAbsolutePath())
                            + " && chown " + uid + " " + RootShell.quote(tmp.getAbsolutePath())
                            + " && chmod 600 " + RootShell.quote(tmp.getAbsolutePath()));
            if (r == null || r.getCode() != 0) throw new FileNotFoundException(path);
            ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY);
            // The descriptor keeps the inode alive after unlink, and no copy is left behind if the
            // file turns out to be unreadable later on.
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            tmp = null;
            return descriptor;
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Throwable t) {
            throw new FileNotFoundException(path);
        } finally {
            if (tmp != null) {
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
        }
    }

    @Override
    public String getDocumentType(String documentId) throws FileNotFoundException {
        String path = requirePath(documentId);
        if (isDirectory(path)) {
            return DocumentsContract.Document.MIME_TYPE_DIR;
        }
        return path.endsWith(".sh") ? "text/x-shellscript" : "application/octet-stream";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolved from the same validation as every other entry point: a document is only a child of
     * the root, and it has to be a plain name inside the script directory. The previous prefix check
     * answered "yes" for anything the caller made up, which is what made the directory grantable as
     * the whole file system.</p>
     */
    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        if (parentDocumentId == null || documentId == null) return false;
        if (documentId.equals(parentDocumentId)) return true;
        return ROOT_ID.equals(parentDocumentId) && pathOf(documentId) != null;
    }

    private boolean isDirectory(String path) {
        com.topjohnwu.superuser.Shell.Result r = RootShell.run(
                "[ -d " + RootShell.quote(path) + " ] && echo yes");
        return r != null && r.getOut().toString().contains("yes");
    }

    private void addDocumentRow(MatrixCursor cursor, String id, File file, boolean isDir) {
        MatrixCursor.RowBuilder row = cursor.newRow();
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, id);
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.getName().isEmpty() ? "scripts" : file.getName());
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE,
                isDir ? DocumentsContract.Document.MIME_TYPE_DIR : getMime(file.getName()));
        row.add(DocumentsContract.Document.COLUMN_SIZE, isDir ? 0 : file.length());
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, isDir ? 0 : file.lastModified());
        // Read only: no FLAG_DIR_SUPPORTS_CREATE, the provider cannot create documents.
        row.add(DocumentsContract.Document.COLUMN_FLAGS, 0);
    }

    private String getMime(String name) {
        return name.endsWith(".sh") ? "text/x-shellscript" : "application/octet-stream";
    }

    private String requirePath(String documentId) throws FileNotFoundException {
        String path = pathOf(documentId);
        if (path == null) throw new FileNotFoundException("invalid document id");
        return path;
    }

    /**
     * Maps a document id to a path, or returns null when the id is not one this provider handed out.
     *
     * <p>A valid id is either the root or {@code scripts/<plain name>}; the name goes through
     * {@link ScriptPath#isPlainName}, which rejects separators, dot segments, NUL and over-long
     * values - the same rule the alarm label has to satisfy before a script can run.</p>
     */
    private String pathOf(String documentId) {
        if (documentId == null) return null;
        if (ROOT_ID.equals(documentId)) return DIR;
        String name = documentId.startsWith(ROOT_ID + "/")
                ? documentId.substring(ROOT_ID.length() + 1)
                : documentId;
        if (!ScriptPath.isPlainName(name)) return null;
        String path = DIR + "/" + name;
        // A plain name is not enough on its own: a symlink planted in the script directory keeps a
        // plain name while pointing at, say, ../password. Resolve it and demand the target stays
        // inside, so the provider cannot be turned into a reader for anything root can reach.
        String root = RootShell.realPath(DIR);
        String real = RootShell.realPath(path);
        if (root == null || real == null || !real.startsWith(root + "/")) return null;
        return path;
    }
}
