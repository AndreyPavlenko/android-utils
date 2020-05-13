package me.aap.utils.vfs.gdrive;

import com.google.api.services.drive.Drive;

import java.util.List;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.vfs.VirtualFolder;
import me.aap.utils.vfs.VirtualResource;

/**
 * @author Andrey Pavlenko
 */
class GdriveFolder extends GdriveResource implements VirtualFolder {

	GdriveFolder(GdriveFileSystem fs, String id, String name) {
		super(fs, id, name);
	}

	GdriveFolder(GdriveFileSystem fs, String id, String name, VirtualFolder parent) {
		super(fs, id, name, parent);
	}

	@Override
	public FutureSupplier<List<VirtualResource>> getChildren() {
		return fs.useDrive(d -> {
			Drive.Files.List req = d.files().list().setQ('\'' + id + "' in parents and trashed = false")
					.setSpaces("drive")
					.setFields("nextPageToken, files(id, name, mimeType)");
			return fs.loadList(req, this, false);
		});
	}
}
