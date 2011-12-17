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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartupSCFile {
	private static final Logger logger = LoggerFactory.getLogger(StartupSCFile.class);

	private final String filePath;

	public StartupSCFile(String filePath) {
		this.filePath = filePath;
	}

	public String getContent() {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(new File(filePath)))));
		} catch (FileNotFoundException e) {
			logger.error("startup sc file not found. filepath: " + filePath, e);
			return null;
		}

		StringBuilder sb = new StringBuilder();
		try {
			String line = reader.readLine();
			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = reader.readLine();
			}
		} catch (IOException e) {
			logger.error("", e);
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				logger.error("", e);
			}
		}
		return sb.toString();
	}
}
