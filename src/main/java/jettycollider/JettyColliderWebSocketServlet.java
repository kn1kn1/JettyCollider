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

import javax.servlet.http.HttpServletRequest;

import jettycollider.sclang.StartupSCFile;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

public class JettyColliderWebSocketServlet extends WebSocketServlet {
	private final String sclangRuntimeFolder;
	private final StartupSCFile startupFile;
	private final int wsMaxIdleTime;
	
	public JettyColliderWebSocketServlet(String sclangRuntimeFolder, StartupSCFile startupFile, int wsMaxIdleTime) {
		this.sclangRuntimeFolder = sclangRuntimeFolder;
		this.startupFile = startupFile;
		this.wsMaxIdleTime = wsMaxIdleTime;
	}
	
	@Override
	public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
		return new JettyColliderWebSocket(sclangRuntimeFolder, startupFile, wsMaxIdleTime);
	}
}
