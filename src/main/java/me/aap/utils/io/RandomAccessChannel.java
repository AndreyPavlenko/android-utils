package me.aap.utils.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import me.aap.utils.log.Log;

/**
 * @author Andrey Pavlenko
 */
public interface RandomAccessChannel {

	int read(ByteBuffer dst, long position) throws IOException;

	int write(ByteBuffer src, long position) throws IOException;

	long transferTo(long position, long count, WritableByteChannel target) throws IOException;

	long transferFrom(ReadableByteChannel src, long position, long count) throws IOException;

	long size();

	static RandomAccessChannel wrap(FileChannel ch) {
		return new RandomAccessChannel() {
			@Override
			public int read(ByteBuffer dst, long position) throws IOException {
				return ch.read(dst, position);
			}

			@Override
			public int write(ByteBuffer src, long position) throws IOException {
				return ch.write(src, position);
			}

			@Override
			public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
				return ch.transferTo(position, count, target);
			}

			@Override
			public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
				return ch.transferFrom(src, position, count);
			}

			@Override
			public long size() {
				try {
					return ch.size();
				} catch (IOException ex) {
					Log.e(ex, "Failed to get channel size");
					return 0;
				}
			}
		};
	}
}
