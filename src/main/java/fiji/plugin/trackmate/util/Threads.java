/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2023 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package fiji.plugin.trackmate.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class Threads {

	private static Map<Thread, String> threads = new ConcurrentHashMap<>();
	private static Map<ExecutorService, String> executors = new ConcurrentHashMap<>();

	private static <K, O extends K> O track(O obj, Map<K, String> map) {
		StringWriter sw = new StringWriter();
		new Exception().printStackTrace(new PrintWriter(sw));
		map.put(obj, sw.toString());
		return obj;
	}

	public static void dumpLiving() {
		System.err.println("-------- BEGINNING DUMP OF ACTIVE TRACKMATE RESOURCES --------");
		for (Map.Entry<Thread, String> entry : threads.entrySet()) {
			Thread t = entry.getKey();
			if (t.isAlive() && !t.isDaemon()) {
				System.err.println("\n[" + t.getName() + "]");
				System.err.println(entry.getValue());
			}
		}
		for (Map.Entry<ExecutorService, String> entry : executors.entrySet()) {
			ExecutorService es = entry.getKey();
			if (!es.isShutdown()) {
				System.err.println("\n[" + es + "]");
				System.err.println(entry.getValue());
			}
		}
		System.err.println("-------- DUMP COMPLETE --------");
	}

	public static void run( final Runnable r )
	{
		Thread t = new Thread( r );
		track(t, threads);
		t.start();
	}

	public static void run( final String name, final Runnable r )
	{
		Thread t = new Thread( r, name );
		track(t, threads);
		t.start();
	}

	public static ExecutorService newFixedThreadPool( final int nThreads )
	{
		return track(Executors.newFixedThreadPool( nThreads ), executors);
	}
	public static ExecutorService newCachedThreadPool()
	{
		return track(Executors.newCachedThreadPool(), executors);
	}

	public static ExecutorService newSingleThreadExecutor()
	{
		return track(Executors.newSingleThreadExecutor(), executors);
	}

	public static ScheduledExecutorService newSingleThreadScheduledExecutor()
	{
		return track(Executors.newSingleThreadScheduledExecutor(), executors);
	}
}
