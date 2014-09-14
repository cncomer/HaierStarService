package com.bestjoy.app.utils;

public class FilesLengthUtils {

	public static long UNIT_M = 1 * 1024 * 1024;
	public static long UNIT_K = 1 * 1024;
	public static String computeLengthToString(long length) {
		StringBuilder sb = new StringBuilder();
		if (length < UNIT_K) {
			sb.append(length).append('B');
		} else if (length < UNIT_M) {
			float len = 1.0f * length / UNIT_K;
			sb.append(Math.round(len)).append("KB");
		} else {
			float len = 1.0f * length / UNIT_M;
			sb.append(Math.round(len)).append("MB");
		}
		return sb.toString();
	}
}
