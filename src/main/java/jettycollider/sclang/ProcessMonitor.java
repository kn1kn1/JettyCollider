/*
 * JettyCollider
 *  - a remote control application which enables you to execute 
 *    SuperCollider programming language (sclang) on web browser.
 * 
 * Copyright (C) 2011-2012 Kenichi Kanai
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jettycollider.sclang;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ProcessMonitor {
	private static final Logger logger = LoggerFactory.getLogger(ProcessMonitor.class);

	private InputStreamLineReceiver stdoutReceiver;
	private InputStreamLineReceiver stderrReceiver;

	public void start(Process process) {
		stdoutReceiver = new InputStreamLineReceiver(process.getInputStream()) {
			@Override
			public void receiveLine(String msg) {
				stdout(msg);
			}
		};
		stdoutReceiver.start();
		
		stderrReceiver = new InputStreamLineReceiver(process.getErrorStream()) {
			@Override
			public void receiveLine(String msg) {
				stderr(msg);
			}
		};
		stderrReceiver.start();
	}

	public void dispose() {
		logger.debug("dispose");
		if (stdoutReceiver != null) {
			logger.debug("dispose - stdoutReceiver.dispose();");
			stdoutReceiver.dispose();
		}
		if (stderrReceiver != null) {
			logger.debug("dispose - stderrReceiver.dispose();");
			stderrReceiver.dispose();
		}
	}

	public abstract void stdout(String msg);

	public abstract void stderr(String msg);

	private abstract static class InputStreamLineReceiver extends Thread {
		private final Object lock = new Object();
		private final BufferedReader reader;
		private boolean disposed = false;

		public InputStreamLineReceiver(InputStream in) {
			this.reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(in)));
		}

		@Override
		public void run() {
			try {
				String line = reader.readLine();
				while (line != null) {
					receiveLine(line);
					
					synchronized (lock) {
						if (disposed) {
							break;
						}
						if (isInterrupted()) {
							break;
						}
						line = reader.readLine();
					}
				}
			} catch (IOException e) {
				logger.error("", e);
			} finally {
				dispose();
			}
		}

		private void dispose() {
			logger.debug("dispose");
			if (disposed) {
				return;
			}
			
			// FIXME WORKAROUND reader.close() freeze when SwingOSC executed first time.
//			try {
//				synchronized (lock) {
//					logger.debug("dispose - reader.close();");
//					reader.close();
//					disposed = true;
//					logger.debug("dispose - reader closed;");
//				}
//			} catch (IOException e) {
//				logger.error("", e);
//			}
			ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
			scheduler.submit(new Runnable() {
				@Override
				public void run() {
					try {
						synchronized (lock) {
							logger.debug("dispose - reader closing");
							reader.close();
							disposed = true;
							logger.debug("dispose - reader closed");
						}
					} catch (IOException e) {
						logger.error("", e);
					}
				}
			});
			
			logger.debug("dispose - end");
		}

		public abstract void receiveLine(String line);
	}
}