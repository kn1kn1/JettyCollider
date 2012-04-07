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

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import javax.servlet.Servlet;

import jettycollider.sclang.SCLangProcess;
import jettycollider.sclang.StartupSCFile;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyColliderServerMain {
	private static final Logger logger = LoggerFactory.getLogger(JettyColliderServerMain.class);

	private static final int DEFAULT_PORT = 7777;
	private static final int DEFAULT_WS_IDLE_TIME = 60 * 60 * 1000;	// 1hour; 3,600,000msec.
	private static final String DEFAULT_SCLANG_RUNTIME_FOLDER_PATH = SCLangProcess.SCLANG_RUNTIME_FOLDER_MAC;

	public static void main(String[] args) throws Exception {
		Properties properties = loadProperties();
		int port = Integer.parseInt(properties.getProperty("port", String.valueOf(DEFAULT_PORT)));
		int wsMaxIdleTime = Integer.parseInt(properties.getProperty("ws.maxIdleTime",
				String.valueOf(DEFAULT_WS_IDLE_TIME)));
		String sclangRuntimeFolder = properties.getProperty("sclangRuntimeFolder.path", DEFAULT_SCLANG_RUNTIME_FOLDER_PATH);
		if (!sclangRuntimeFolder.endsWith("/")) {
			sclangRuntimeFolder = sclangRuntimeFolder + "/";
		}
		boolean browseAfterStarted = Boolean.parseBoolean(properties.getProperty("browseAfterStarted",
				Boolean.TRUE.toString()));
		String startupScFilePath = properties.getProperty("startupScFile.path");
		StartupSCFile startupFile = (startupScFilePath != null) ? new StartupSCFile(startupScFilePath) : null;
		
		createSystemTrayMenu();

		startServer(port, wsMaxIdleTime, sclangRuntimeFolder, startupFile, browseAfterStarted);
	}

	private static Properties loadProperties() {
		Properties properties = new Properties();
		try {
			InputStream inputStream = new FileInputStream(new File("jettycollider.properties"));
			properties.load(inputStream);
		} catch (IOException e) {
			logger.error("loading " + "jettycollider.properties" + " failed.", e);
		}
		logger.info("--listing jettycollider.properties");
		logger.info(properties.toString());
		logger.info("--listing jettycollider.properties");
		return properties;
	}

	private static void createSystemTrayMenu() {
		MenuItem quitMenuItem = new MenuItem("Quit");
		quitMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		PopupMenu popupMenu = new PopupMenu();
		popupMenu.add(quitMenuItem);

		URL imageUrl = getResource("images/SCcube.png");
		TrayIcon trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().createImage(imageUrl));
		trayIcon.setImageAutoSize(true);
		trayIcon.setToolTip("JettyCollider");
		trayIcon.setPopupMenu(popupMenu);

		SystemTray systemTray = java.awt.SystemTray.getSystemTray();
		try {
			systemTray.add(trayIcon);
		} catch (AWTException e) {
			logger.error("adding tray icon failed.", e);
		}
	}

	private static void startServer(final int port, int wsMaxIdleTime, String sclangRuntimeFolder,
			StartupSCFile startupFile, boolean browseAfterStarted) throws Exception {
		Server server = new Server(port);

		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setResourceBase(getResource("html").toExternalForm());

		Servlet servlet = new JettyColliderWebSocketServlet(sclangRuntimeFolder, startupFile, wsMaxIdleTime);
		ServletContextHandler servletHander = new ServletContextHandler();
		servletHander.addServlet(new ServletHolder(servlet), "/ws/*");

		HandlerList handlerList = new HandlerList();
		handlerList.setHandlers(new Handler[] { resourceHandler, servletHander, });
		server.setHandler(handlerList);

		if (browseAfterStarted) {
			server.addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener() {
				@Override
				public void lifeCycleStarted(LifeCycle event) {
					browse();
				}

				private void browse() {
					Desktop desktop = Desktop.getDesktop();
					try {
						desktop.browse(new URI("http://localhost:" + port));
					} catch (IOException e) {
						logger.error("", e);
					} catch (URISyntaxException e) {
						logger.error("", e);
					}
				}
			});
		}

		server.start();
	}

	private static URL getResource(String name) {
		return JettyColliderServerMain.class.getClassLoader().getResource(name);
	}
}
