package me.aap.utils.vfs.sftp;

import com.jcraft.jsch.ChannelSftp.LsEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.text.SharedTextBuilder;
import me.aap.utils.vfs.VirtualFolder;
import me.aap.utils.vfs.VirtualResource;

/**
 * @author Andrey Pavlenko
 */
class SftpFolder extends SftpResource implements VirtualFolder {

	public SftpFolder(SftpRoot root, String path) {
		super(root, path);
	}

	@Override
	public FutureSupplier<List<VirtualResource>> getChildren() {
		return getRoot().useChannel(ch -> {
			@SuppressWarnings("unchecked") List<LsEntry> ls = ch.ls(getPath());
			if (ls.isEmpty()) return Collections.emptyList();

			SftpRoot root = getRoot();
			List<VirtualResource> children = new ArrayList<>(ls.size());

			try (SharedTextBuilder tb = SharedTextBuilder.get()) {
				tb.append(getPath()).append('/');
				int len = tb.length();

				for (LsEntry e : ls) {
					String name = e.getFilename();
					if (name.equals(".") || name.equals("..")) continue;
					tb.setLength(len);
					String p = tb.append(name).toString();
					if (e.getAttrs().isDir()) children.add(new SftpFolder(root, p));
					else children.add(new SftpFile(root, p));
				}
			}

			return children;
		});
	}
}
