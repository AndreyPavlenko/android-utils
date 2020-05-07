package me.aap.utils.vfs;

import android.net.Uri;

import java.nio.ByteBuffer;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.net.NetChannel;
import me.aap.utils.net.http.HttpError;
import me.aap.utils.net.http.HttpError.Forbidden;
import me.aap.utils.net.http.HttpError.NotFound;
import me.aap.utils.net.http.HttpError.ServiceUnavailable;
import me.aap.utils.net.http.HttpHeaderBuilder;
import me.aap.utils.net.http.HttpMethod;
import me.aap.utils.net.http.HttpRequest;
import me.aap.utils.net.http.HttpRequestHandler;
import me.aap.utils.net.http.HttpVersion;
import me.aap.utils.net.http.Range;

/**
 * @author Andrey Pavlenko
 */
public class VfsHttpHandler implements HttpRequestHandler {
	public static final String HTTP_PATH = "/vfs";
	public static final String HTTP_QUERY = "resource=";
	private final VfsManager mgr;

	public VfsHttpHandler(VfsManager mgr) {
		this.mgr = mgr;
	}

	@Override
	public void handleRequest(NetChannel channel, HttpRequest req, ByteBuffer payload) {
		Uri uri = getUri(req);

		if (uri == null) {
			NotFound.instance.write(channel);
			return;
		}

		Range range = req.getRange();
		HttpMethod method = req.getMethod();
		HttpVersion version = req.getVersion();
		boolean close = req.closeConnection();

		mgr.getResource(uri).onCompletion((result, fail) -> {
			if (fail != null) {
				NotFound.instance.write(channel);
				return;
			}

			if (!(result instanceof VirtualFile)) {
				Forbidden.instance.write(channel);
				return;
			}

			VirtualFile file = (VirtualFile) result;

			file.getLength().onCompletion((len, err) -> {
				if (err != null) {
					ServiceUnavailable.instance.write(channel);
					return;
				}

				if (range != null) {
					range.align(len);

					if (!range.isSatisfiable(len)) {
						HttpError.RangeNotSatisfiable.instance.write(channel);
						return;
					}
				}

				FutureSupplier<Void> reply;

				if (method == HttpMethod.HEAD) {
					reply = channel.write(() -> buildResponse(version, len, range, close));
				} else if (range != null) {
					long start = range.getStart();
					reply = file.transferTo(channel, start, range.getEnd() - start + 1, ()
							-> buildResponse(version, len, range, close));
				} else {
					reply = file.transferTo(channel, 0, len, () -> buildResponse(version, len, null, close));
				}

				reply.onCompletion((r, f) -> {
					if (close || (f != null)) channel.close();
				});
			});
		});
	}

	protected Uri getUri(HttpRequest req) {
		CharSequence q = req.getQuery();
		if ((q == null) || (q.length() <= HTTP_QUERY.length())) {
			return null;
		}
		return Uri.parse(Uri.decode(q.subSequence(HTTP_QUERY.length(), q.length()).toString()));
	}

	protected ByteBuffer buildResponse(HttpVersion version, long len, Range range, boolean close) {
		HttpHeaderBuilder b = new HttpHeaderBuilder();
		long contentLen = len;

		if (range != null) {
			contentLen = range.getEnd() - range.getStart() + 1;
			b.statusPartial(version);
			b.addLine("Content-Range: bytes " + range.getStart() + '-' + range.getEnd() + '/' + len);
		} else {
			b.statusOk(version);
		}

		b.addLine("Accept-Ranges: bytes");
		b.addLine("Content-Length: " + contentLen);
		if (close) b.addLine("Connection: close");
		return b.build();
	}
}
