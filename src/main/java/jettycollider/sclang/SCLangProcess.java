/*
 * JettyCollider
 *  - a remote control application which enables you to execute 
 *    SuperCollider programming language (sclang) on web browser.
 * 
 * Copyright (C) 2011  Kenichi Kanai
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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SCLangProcess {
	public static final String SCLANG_RUNTIME_FOLDER_MAC = "/Applications/SuperCollider/";

	private static final Logger logger = LoggerFactory.getLogger(SCLangProcess.class);

	private final List<ProcessMonitor> monitorList = new ArrayList<ProcessMonitor>();
	// FIXME scheduler is a workaround
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private final Runtime runtime;
	private final String command;

	private Process process;
	private OutputStreamWriter stdin;
	private StartupSCFile startupFile;
	private boolean recoding;
	// FIXME future is a workaround
	private ScheduledFuture<?> future;

	public SCLangProcess(String sclangRuntimeFolder) {
		this(sclangRuntimeFolder, null);
	}
	
	public SCLangProcess(String sclangRuntimeFolder, StartupSCFile startupFile) {
		this(sclangRuntimeFolder, " -d " + sclangRuntimeFolder, startupFile);
	}

	public SCLangProcess(String sclangRuntimeFolder, String option, StartupSCFile startupFile) {
		this.runtime = Runtime.getRuntime();
		this.command = sclangRuntimeFolder + "sclang" + " -i JettyCollider" + option;
		this.startupFile = startupFile;
	}

	public void addProcessMonitor(ProcessMonitor monitor) {
		monitorList.add(monitor);
	}

	public void execute() throws IOException {
		this.process = runtime.exec(command);
		this.stdin = new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream()));
		for (ProcessMonitor monitor : monitorList) {
			monitor.start(process);
		}
		logger.debug("startupFile: " + startupFile);
		if (startupFile != null) {
			String code = startupFile.getContent();
			if (code != null && code.length() != 0) {
				evaluate(code);
			}
		}
	}

	public void startServer() throws IOException {
		logger.debug("startServer");
		evaluate("Server.default.boot;");
	}

	public void stopServer() throws IOException {
		logger.debug("stopServer");
		evaluate("Server.default.quit;");
		recoding = false;
	}

	public void evaluate(String code) throws IOException {
		evaluate(code, false);
	}

	public void evaluate(String code, boolean silent) throws IOException {
		logger.info("evaluate: " + code);
		if (stdin == null) {
			return;
		}

		stdin.write(code);
		if (silent) {
			stdin.write(0x1b);
		} else {
			stdin.write(0x0c);
		}
		stdin.flush();
	}

	public void stopSound() throws IOException {
		evaluate("thisProcess.stop;", true);
	}

	public void toggleRecording() throws IOException {
		if (!recoding) {
			if (future != null && !future.isDone()) {
				// start recording is already scheduled.
				return;
			}
			evaluate("s.prepareForRecord;", true);

			// FIXME give server some time to prepare
			future = scheduler.schedule(new Runnable() {
				@Override
				public void run() {
					try {
						evaluate("s.record;", true);
						recoding = true;
					} catch (IOException e) {
						logger.error("", e);
					}
				}
			}, 100, TimeUnit.MILLISECONDS);

		} else {
			evaluate("s.stopRecording;", true);
			recoding = false;
		}
	}

	public void dispose() throws IOException {
		logger.debug("dispose");
		try {
			stopSound();
		} finally {
			try {
				stopServer();
			} finally {
				try {
					if (stdin != null) {
						logger.debug("dispose - stdin.close();");
						stdin.close();
					}
				} finally {
					try {
						logger.debug("dispose - scheduler.shutdown();");
						scheduler.shutdown();
					} finally {
						try {
							logger.debug("dispose - process.destroy();");
							process.destroy();
						} finally {
							for (ProcessMonitor monitor : monitorList) {
								logger.debug("dispose - monitor.dispose();");
								monitor.dispose();
							}
						}
					}
				}
			}
		}
	}
}
