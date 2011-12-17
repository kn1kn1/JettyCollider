JettyCollider
=============

Introduction
------------
JettyCollider is a remote control application which enables you to execute SuperCollider programming language (sclang) on web browser.

System Requirements
-------------------
### Server Side
  - Mac OS X 10.4.9 or greater
  - SuperCollider 3.4.4
  - Java SE 6

### Client Side
Web browser which supports WebSocket

  - Safari
  - Chrome
  - Firefox

Usage
-----
### Server Side
Double click JettyCollider executable file or run command on terminal.

#### Executable file
  - JettyCollider.app.

#### Command (x.x.x is a version number)
```
% java -jar JettyCollider-x.x.x.jar 
```

Running sign is the SuperCollider icon displayed in task tray.

### Client Side
Access "http:[server address]:[server port]/" by web browser.

Configuration Files
-------------------
### jettycollider.properties
  - port: server port number. [default: 7777]
  - ws.maxIdleTime: max idle time in msec to close WebSocket connection. [default: 3,600,000]
  - sclangRuntimeFolder.path: path to parent folder of sclang. [default: /Applications/SuperCollider/]
  - browseAfterStarted: boolean value whether web browser is invoked after JettyCollider started. [default: true]
  - startupScFile.path: path to additional startup.sc file. [default: (not specified)]

### startup.sc
Additional startup file sclang executes after class library initialization. This file is not needed if you do not have any initialize operation or already have configured '~/.sclang.sc' or 'startup.sc'.

License
-------
JettyCollider is released under the GNU General Public License (GPL) version 3, see the file 'COPYING' for more information.
