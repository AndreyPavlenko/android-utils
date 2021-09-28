package me.aap.utils.vfs.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import me.aap.utils.async.Completed;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.io.FileUtils;
import me.aap.utils.io.IoUtils;
import me.aap.utils.io.RandomAccessChannel;
import me.aap.utils.vfs.VirtualFile;
import me.aap.utils.vfs.VirtualFolder;
import me.aap.utils.io.AsyncInputStream;
import me.aap.utils.io.AsyncOutputStream;

import static me.aap.utils.async.Completed.failed;

/**
 * @author Andrey Pavlenko
 */
class LocalFile extends LocalResource implements VirtualFile {

	LocalFile(File file) {
		super(file);
	}

	LocalFile(File file, VirtualFolder parent) {
		super(file, parent);
	}

	@Override
	public FutureSupplier<Long> getLength() {
		return getVirtualFileSystem().getLength(getLocalFile());
	}

	@Override
	public boolean isLocalFile() {
		return true;
	}

	@NonNull
	@Override
	public FutureSupplier<Void> copyTo(VirtualFile to) {
		File toFile = to.getLocalFile();

		if (toFile != null) {
			try {
				FileUtils.copy(file, toFile);
				return Completed.completedNull();
			} catch (IOException ex) {
				return failed(ex);
			}
		}

		return VirtualFile.super.copyTo(to);
	}

	@NonNull
	@Override
	public FutureSupplier<Boolean> moveTo(VirtualFile to) {
		File toFile = to.getLocalFile();

		if (toFile != null) {
			try {
				FileUtils.move(file, toFile);
				return Completed.completed(true);
			} catch (IOException ex) {
				return failed(ex);
			}
		}

		return VirtualFile.super.moveTo(to);
	}

	@Override
	public AsyncInputStream getInputStream(long offset) throws IOException {
		FileInputStream in = new FileInputStream(file);
		IoUtils.skip(in, offset);
		return AsyncInputStream.from(in, getInputBufferLen());
	}

	@Override
	public AsyncOutputStream getOutputStream() throws IOException {
		return AsyncOutputStream.from(new FileOutputStream(file), getOutputBufferLen());
	}

	@Nullable
	@Override
	public RandomAccessChannel getChannel() {
		return getVirtualFileSystem().getChannel(getLocalFile());
	}
}
