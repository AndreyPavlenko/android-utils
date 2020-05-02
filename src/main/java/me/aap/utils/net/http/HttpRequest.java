package me.aap.utils.net.http;

import androidx.annotation.Nullable;

/**
 * @author Andrey Pavlenko
 */
public interface HttpRequest {

	HttpMethod getMethod();

	HttpVersion getVersion();

	CharSequence getUri();

	CharSequence getPath();

	@Nullable
	CharSequence getQuery();

	CharSequence getHeaders();

	long getContentLength();

	@Nullable
	Range getRange();

	boolean closeConnection();
}
