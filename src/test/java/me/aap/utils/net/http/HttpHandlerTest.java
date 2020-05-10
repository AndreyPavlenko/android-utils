package me.aap.utils.net.http;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.concurrent.NetThreadPool;
import me.aap.utils.holder.IntHolder;
import me.aap.utils.misc.TestUtils;
import me.aap.utils.net.NetChannel;
import me.aap.utils.net.NetHandler;
import me.aap.utils.net.NetServer;
import me.aap.utils.vfs.VfsHttpHandler;
import me.aap.utils.vfs.VfsManager;
import me.aap.utils.vfs.local.LocalFileSystem;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Objects.requireNonNull;

/**
 * @author Andrey Pavlenko
 */
public class HttpHandlerTest extends Assertions {
	private static ExecutorService exec;
	private static NetHandler handler;
	private static byte[] REQ;
	private static byte[] RESP;
	private static final String RESP_FORMAT = "HTTP/1.1 200 Ok\n" +
			"Query: %s\n" +
			"Content-Length: 10\n\n" +
			"0123456789";
	private static final byte[] CLOSE_RESP = ("HTTP/1.1 200 Ok\n" +
			"Connection: Close\n\n").getBytes(US_ASCII);

	@BeforeAll
	public static void setUpClass() throws IOException {
		TestUtils.enableTestMode();
		exec = new NetThreadPool(Runtime.getRuntime().availableProcessors() - 1);
		handler = NetHandler.create(exec);

		int nreq = 1000;
		String req = "GET /test?q=%04d HTTP/1.1\n" +
				"Host: localhost:8080\n" +
				"User-Agent: curl/7.65.3\n" +
				"Accept: */*\r\n\r\n";
		byte[] closeReq = ("GET /test HTTP/1.1\n" +
				"Host: localhost:8080\n" +
				"Connection: Close\r\n\r\n").getBytes(US_ASCII);
		int reqLen = req.length();
		int resLen = RESP_FORMAT.length() + 4;

		REQ = new byte[reqLen * nreq + closeReq.length];
		RESP = new byte[resLen * nreq + CLOSE_RESP.length];


		for (int i = 0; i < nreq; i++) {
			int n = i + 1;
			String r = String.format(req, n);
			System.arraycopy(r.getBytes(US_ASCII), 0, REQ, i * reqLen, reqLen);
			r = String.format(RESP_FORMAT, String.format("q=%04d", n));
			System.arraycopy(r.getBytes(US_ASCII), 0, RESP, i * resLen, resLen);
		}

		System.arraycopy(closeReq, 0, REQ, nreq * reqLen, closeReq.length);
		System.arraycopy(CLOSE_RESP, 0, RESP, nreq * resLen, CLOSE_RESP.length);
	}

	@AfterAll
	public static void tearDownClass() {
		handler.close();
		exec.shutdown();
		REQ = null;
		RESP = null;
	}


	@RepeatedTest(10)
	public void testIncomplete() throws Exception {
		Map<NetChannel, Integer> requests = new ConcurrentHashMap<>(1000);
		AtomicBoolean failed = new AtomicBoolean();
		HttpConnectionHandler http = new HttpConnectionHandler();
		HttpRequestHandler reqHandler = (channel, req, payload) -> {
			if (req.closeConnection()) {
				channel.write(() -> ByteBuffer.wrap(CLOSE_RESP)).thenRun(channel::close);
			} else {
				try {
					String q = requireNonNull(req.getQuery()).toString();
					int n = Integer.parseInt(q.substring(2));
					Integer prev = requests.put(channel, n);
					if (n == 1) assertNull(prev);
					else assertEquals(n - 1, prev);
					byte[] resp = String.format(RESP_FORMAT, req.getQuery()).getBytes(US_ASCII);
					channel.write(() -> ByteBuffer.wrap(resp));
				} catch (Throwable ex) {
					ex.printStackTrace();
					fail();
				}
			}
		};

		http.addHandler("/test", (p, m, v) -> reqHandler);

		NetServer server = handler.bind(o -> o.handler = http).getOrThrow();

		SocketAddress addr = server.getBindAddress();
		int nclients = 100;
		FutureSupplier<?>[] tasks = new FutureSupplier[nclients];

		for (int i = 0; i < nclients; i++) {
			int n = i + 1;

			tasks[i] = handler.connect(o -> {
				o.address = addr;
				o.opt.put(StandardSocketOptions.TCP_NODELAY, Boolean.TRUE);
			}).then(ch -> {
				IntHolder h = new IntHolder();
				ch.write(() -> ByteBuffer.wrap(REQ, h.value, n)).thenIterate(v -> {
					if (h.value < (REQ.length - 2 * n)) {
						return ch.write(ByteBuffer.wrap(REQ, h.value += n, n));
					} else {
						ch.write(ByteBuffer.wrap(REQ, h.value += n, REQ.length - h.value));
						return null;
					}
				});

				ByteBuffer resp = ByteBuffer.allocate(RESP.length);

				return ch.read().thenIterate(b -> {
					ByteBuffer bb = b.get();

					if (!bb.hasRemaining()) {
						assertEquals(new String(RESP), new String(resp.array()), () -> "n = " + n);
						ch.close();
						return null;
					}

					resp.put(bb);
					return ch.read();
				});
			}).onFailure(fail -> {
				fail.printStackTrace();
				failed.set(true);
			});
		}

		for (FutureSupplier<?> t : tasks) {
			t.get();
		}

		server.close();
		assertFalse(failed.get());
	}

	@RepeatedTest(10)
	public void test() throws Exception {
		Map<NetChannel, Integer> requests = new ConcurrentHashMap<>(1000);
		AtomicBoolean failed = new AtomicBoolean();
		HttpConnectionHandler http = new HttpConnectionHandler();
		HttpRequestHandler reqHandler = (channel, req, payload) -> {
			if (req.closeConnection()) {
				channel.write(() -> ByteBuffer.wrap(CLOSE_RESP)).thenRun(channel::close);
			} else {
				try {
					String q = requireNonNull(req.getQuery()).toString();
					int n = Integer.parseInt(q.substring(2));
					Integer prev = requests.put(channel, n);
					if (n == 1) assertNull(prev);
					else assertEquals(n - 1, prev);
					byte[] resp = String.format(RESP_FORMAT, req.getQuery()).getBytes(US_ASCII);
					channel.write(() -> ByteBuffer.wrap(resp));
				} catch (Throwable ex) {
					ex.printStackTrace();
					fail();
				}
			}
		};

		http.addHandler("/test", (p, m, v) -> reqHandler);
		NetServer server = handler.bind(o -> o.handler = http).getOrThrow();

		SocketAddress addr = server.getBindAddress();
		int nclients = 100;
		FutureSupplier<?>[] tasks = new FutureSupplier[nclients];

		for (int i = 0; i < nclients; i++) {

			tasks[i] = handler.connect(o -> o.address = addr).then(ch -> {
				ch.write(() -> ByteBuffer.wrap(REQ));
				ByteBuffer resp = ByteBuffer.allocate(RESP.length);

				return ch.read().thenIterate(b -> {
					ByteBuffer bb = b.get();

					if (!bb.hasRemaining()) {
						assertEquals(new String(RESP), new String(resp.array()));
						ch.close();
						return null;
					}

					resp.put(bb);
					return ch.read();
				});
			}).onFailure(fail -> {
				fail.printStackTrace();
				failed.set(true);
			});
		}

		for (FutureSupplier<?> t : tasks) {
			t.get();
		}

		server.close();
		assertFalse(failed.get());
	}

	@Test
	@Disabled
	public void httpServer() throws Exception {
		File index = new File("/var/www/html/index.nginx-debian.html");
		long len = index.length();
		byte[] resp = ("HTTP/1.1 200 OK\n" +
				"Server: nginx/1.17.10 (Ubuntu)\n" +
				"Date: Tue, 05 May 2020 17:08:57 GMT\n" +
				"Content-Type: text/html\n" +
				"Content-Length: " + len + "\n" +
				"Last-Modified: Tue, 24 Nov 2015 00:22:32 GMT\n" +
				"Connection: keep-alive\n" +
				"ETag: \"5653adc8-264\"\n" +
				"Accept-Ranges: bytes\n\n").getBytes(US_ASCII);
		ByteBuffer mapped = new FileInputStream(index).getChannel().map(FileChannel.MapMode.READ_ONLY, 0, len);
		HttpConnectionHandler http = new HttpConnectionHandler();
		http.addHandler("/", (path, method, version) -> (channel, req, payload) -> {
					req.getRange();
					channel.write(() -> new ByteBuffer[]{ByteBuffer.wrap(resp), mapped.duplicate()});
				}
		);

		handler.bind(o -> {
			o.port = 8080;
			o.handler = http;
		}).getOrThrow();

		Thread.sleep(600000);
	}

	@Test
	@Disabled
	public void vfsHttpServer() throws Exception {
		VfsManager mgr = new VfsManager(LocalFileSystem.getInstance());
		VfsHttpHandler vfsHandler = new VfsHttpHandler(mgr);
		HttpConnectionHandler http = new HttpConnectionHandler();
		http.addHandler(VfsHttpHandler.HTTP_PATH, (path, method, version) -> vfsHandler);

		handler.bind(o -> {
			o.port = 8080;
			o.handler = http;
		}).getOrThrow();

		Thread.sleep(600000);
	}
}
