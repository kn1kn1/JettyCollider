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
package jettycollider;

import java.io.IOException;

import jettycollider.sclang.ProcessMonitor;
import jettycollider.sclang.SCLangProcess;
import jettycollider.sclang.StartupSCFile;

import org.eclipse.jetty.websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyColliderWebSocket implements WebSocket.OnTextMessage {
	private static final String CMD_START_SERVER = "/start_server::";
	private static final String CMD_STOP_SERVER = "/stop_server::";
	private static final String CMD_EVALUATE = "/evaluate::";
	private static final String CMD_STOP_SOUND = "/stop_sound::";
	private static final String CMD_TOGGLE_RECORDING = "/toggle_recording::";
	private static final String CMD_RESTART_INTERPRETER = "/restart_interpreter::";

	private static final Logger logger = LoggerFactory.getLogger(JettyColliderWebSocket.class);

	private final String sclangRuntimeFolder;
	private final StartupSCFile startupFile;
	private final int wsMaxIdleTime;

	private SCLangProcess process;
	private Connection connection;

	public JettyColliderWebSocket(String sclangRuntimeFolder, StartupSCFile startupFile, int wsMaxIdleTime) {
		this.sclangRuntimeFolder = sclangRuntimeFolder;
		this.startupFile = startupFile;
		this.wsMaxIdleTime = wsMaxIdleTime;
	}

	@Override
	public void onOpen(Connection connection) {
		this.connection = connection;
		this.connection.setMaxIdleTime(wsMaxIdleTime);
		try {
			this.process = startProcess(sclangRuntimeFolder, startupFile, connection);
		} catch (IOException e) {
			logger.error("", e);
		}
	}

	@Override
	public void onClose(int closeCode, String message) {
		try {
			stopProcess(process);
		} catch (IOException e) {
			logger.error("", e);
		}
	}

	@Override
	public void onMessage(String data) {
		if (process == null) {
			return;
		}

		try {
			if (data.startsWith(CMD_START_SERVER)) {
				logger.info("=> start server");
				process.startServer();
			} else if (data.startsWith(CMD_STOP_SERVER)) {
				logger.info("=> stop_server");
				process.stopServer();
			} else if (data.startsWith(CMD_EVALUATE)) {
				String code = data.substring(CMD_EVALUATE.length());
				logger.info("=> code: " + code);
				process.evaluate(code);
			} else if (data.startsWith(CMD_STOP_SOUND)) {
				logger.info("=> stop_sound");
				process.stopSound();
			} else if (data.startsWith(CMD_TOGGLE_RECORDING)) {
				logger.info("=> toggle_recording");
				process.toggleRecording();
			} else if (data.startsWith(CMD_RESTART_INTERPRETER)) {
				logger.info("=> restart interpreter");
				stopProcess(process);
				this.process = startProcess(sclangRuntimeFolder, startupFile, connection);
			}
		} catch (IOException e) {
			logger.error("", e);
		}
	}

	private static SCLangProcess startProcess(String sclangRuntimeFolder, StartupSCFile startupFile,
			final Connection connection) throws IOException {
		SCLangProcess process = new SCLangProcess(sclangRuntimeFolder, startupFile);
		process.addProcessMonitor(new ProcessMonitor() {
			@Override
			public void stdout(String msg) {
				logger.info(msg);
				if (!connection.isOpen()) {
					return;
				}
				try {
					connection.sendMessage(msg + "\n");
				} catch (IOException e) {
					logger.error("", e);
				}
			}

			@Override
			public void stderr(String msg) {
				logger.info(msg);
			}
		});

		process.execute();
		return process;
	}

	private static void stopProcess(SCLangProcess process) throws IOException {
		if (process == null) {
			return;
		}
		process.dispose();
	}
}
