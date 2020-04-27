package me.aap.utils.vfs.sftp;

import com.jcraft.jsch.SftpATTRS;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.vfs.VirtualFile;

/**
 * @author Andrey Pavlenko
 */
class SftpFile extends SftpResource implements VirtualFile {

	public SftpFile(SftpRoot root, String path) {
		super(root, path);
	}

	@Override
	public FutureSupplier<Long> getLength() {
		return lstat().map(SftpATTRS::getSize);
	}
}
