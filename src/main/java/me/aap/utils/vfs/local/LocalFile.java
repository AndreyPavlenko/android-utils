package me.aap.utils.vfs.local;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import me.aap.utils.async.Completed;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.io.FileUtils;
import me.aap.utils.vfs.VirtualFile;
import me.aap.utils.vfs.VirtualFolder;
import me.aap.utils.vfs.VirtualInputStream;
import me.aap.utils.vfs.VirtualOutputStream;

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
		return Completed.completed(file.length());
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
	public VirtualInputStream getInputStream(long offset) throws IOException {
		FileInputStream in = new FileInputStream(file);
		while (offset > 0) offset -= in.skip(offset);
		return VirtualInputStream.wrapInputStream(in, getInputBufferLen());
	}

	@Override
	public VirtualOutputStream getOutputStream() throws IOException {
		return VirtualOutputStream.wrapOutputStream(new FileOutputStream(file), getOutputBufferLen());
	}
}
