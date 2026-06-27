package android.print;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import java.io.File;

/**
 * Treibt einen {@link PrintDocumentAdapter} (z. B. von WebView) ohne den
 * System-Druckdialog und schreibt das Ergebnis in eine Datei.
 *
 * Liegt bewusst im Package {@code android.print}: die Konstruktoren von
 * {@link PrintDocumentAdapter.LayoutResultCallback} /
 * {@link PrintDocumentAdapter.WriteResultCallback} sind package-private und
 * lassen sich nur aus diesem Package ableiten (aus Kotlin gar nicht).
 */
public final class PdfPrint {

    public interface Callback {
        void onFinished();
        void onFailed(CharSequence error);
        void onCancelled();
    }

    private final PrintAttributes attributes;

    public PdfPrint(PrintAttributes attributes) {
        this.attributes = attributes;
    }

    public void print(final PrintDocumentAdapter adapter, final File file,
                      final CancellationSignal signal, final Callback callback) {
        adapter.onStart();
        adapter.onLayout(null, attributes, signal,
            new PrintDocumentAdapter.LayoutResultCallback() {
                @Override
                public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                    final ParcelFileDescriptor pfd;
                    try {
                        pfd = ParcelFileDescriptor.open(file,
                            ParcelFileDescriptor.MODE_CREATE
                                | ParcelFileDescriptor.MODE_TRUNCATE
                                | ParcelFileDescriptor.MODE_WRITE_ONLY);
                    } catch (Exception e) {
                        adapter.onFinish();
                        callback.onFailed(e.getMessage());
                        return;
                    }
                    adapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, pfd, signal,
                        new PrintDocumentAdapter.WriteResultCallback() {
                            @Override
                            public void onWriteFinished(PageRange[] pages) {
                                close(pfd);
                                adapter.onFinish();
                                callback.onFinished();
                            }
                            @Override
                            public void onWriteFailed(CharSequence error) {
                                close(pfd);
                                adapter.onFinish();
                                callback.onFailed(error);
                            }
                            @Override
                            public void onWriteCancelled() {
                                close(pfd);
                                adapter.onFinish();
                                callback.onCancelled();
                            }
                        });
                }

                @Override
                public void onLayoutFailed(CharSequence error) {
                    adapter.onFinish();
                    callback.onFailed(error);
                }

                @Override
                public void onLayoutCancelled() {
                    adapter.onFinish();
                    callback.onCancelled();
                }
            }, new Bundle());
    }

    private static void close(ParcelFileDescriptor pfd) {
        try { pfd.close(); } catch (Exception ignored) { }
    }
}
