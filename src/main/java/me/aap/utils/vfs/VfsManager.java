package me.aap.utils.vfs;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.aap.utils.async.FutureSupplier;

import static me.aap.utils.async.Completed.completedNull;

/**
 * @author Andrey Pavlenko
 */
public class VfsManager {
	@NonNull
	private volatile Providers providers;

	public VfsManager(VirtualFileSystem.Provider... providers) {
		this(Arrays.asList(providers));
	}

	public VfsManager(List<VirtualFileSystem.Provider> providers) {
		this.providers = new Providers(providers);
	}

	public void addProviders(VirtualFileSystem.Provider... providers) {
		addProviders(Arrays.asList(providers));
	}

	public synchronized void addProviders(List<VirtualFileSystem.Provider> providers) {
		Providers p = this.providers;
		List<VirtualFileSystem.Provider> list = new ArrayList<>(providers.size() + p.all.size());
		list.addAll(providers);
		this.providers = new Providers(list);
	}

	public void removeProviders(VirtualFileSystem.Provider... providers) {
		removeProviders(Arrays.asList(providers));
	}

	public synchronized void removeProviders(List<VirtualFileSystem.Provider> providers) {
		List<VirtualFileSystem.Provider> list = new ArrayList<>(this.providers.all);
		if (!list.removeAll(providers)) return;
		this.providers = new Providers(list);
	}

	public List<VirtualFileSystem.Provider> getProviders() {
		return providers.all;
	}

	@NonNull
	public FutureSupplier<VirtualResource> getResource(Uri uri) {
		Providers providers = this.providers;
		List<VirtualFileSystem.Provider> list = providers.map.get(uri.getScheme());
		if ((list != null) && !list.isEmpty()) return list.get(0).getResource(uri);
		for (VirtualFileSystem.Provider p : providers.any) {
			if (p.isSupportedResource(uri)) return p.getResource(uri);
		}
		return completedNull();
	}

	@NonNull
	public FutureSupplier<VirtualResource> resolve(@NonNull String pathOrUri, @Nullable VirtualResource relativeTo) {
		Uri u = pathOrUri.contains(":/") || (relativeTo == null) ? Uri.parse(pathOrUri)
				: Uri.parse(relativeTo.getUri().toString() + '/' + pathOrUri);
		return getResource(u);
	}

	public boolean isSupportedScheme(String scheme) {
		return providers.map.containsKey(scheme);
	}

	public List<VirtualFileSystem.Provider> getProviders(String scheme) {
		List<VirtualFileSystem.Provider> p = providers.map.get(scheme);
		return (p == null) ? Collections.emptyList() : p;
	}

	private static final class Providers {
		final List<VirtualFileSystem.Provider> all;
		final List<VirtualFileSystem.Provider> any;
		final Map<String, List<VirtualFileSystem.Provider>> map;

		Providers(List<VirtualFileSystem.Provider> providers) {
			ArrayList<VirtualFileSystem.Provider> all = new ArrayList();
			ArrayList<VirtualFileSystem.Provider> any = new ArrayList();
			Map<String, List<VirtualFileSystem.Provider>> map = new HashMap<>((int) (providers.size() * 1.5f));

			for (VirtualFileSystem.Provider p : providers) {
				if (all.contains(p)) continue;

				all.add(p);
				Set<String> schemes = p.getSupportedSchemes();

				if (schemes.isEmpty()) {
					any.add(p);
				} else {
					for (String s : schemes) {
						List<VirtualFileSystem.Provider> l = map.get(s);

						if (l == null) {
							map.put(s, Collections.singletonList(p));
						} else {
							List<VirtualFileSystem.Provider> newList = new ArrayList<>(l.size() + 1);
							newList.addAll(l);
							newList.add(p);
							map.put(s, newList);
						}
					}
				}
			}


			all.trimToSize();
			any.trimToSize();

			this.all = all.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(all);
			this.any = all.isEmpty() ? Collections.emptyList() : any;
			this.map = map;
		}
	}
}
