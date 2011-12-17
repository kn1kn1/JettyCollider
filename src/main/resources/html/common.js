var ws;

function $(id){
  return document.getElementById(id);
}

function onOpenWebSocket() {
  $("start_server").addEventListener("click", onStartServerClick, false);
  $("stop_server").addEventListener("click", onStopServerClick, false);
  $("evaluate").addEventListener("click", onEvaluateClick, false);
  $("stop_sound").addEventListener("click", onStopSoundClick, false);
  $("toggle_recording").addEventListener("click", onToggleRecording, false);
  $("restart_interpreter").addEventListener("click", onRestartInterpreter, false);
  appendOutput("WebSocket connected");
}

function onCloseWebSocket() {
  $("start_server").removeEventListener("click", onStartServerClick, false);
  $("stop_server").removeEventListener("click", onStopServerClick, false);
  $("evaluate").removeEventListener("click", onEvaluateClick, false);
  $("stop_sound").removeEventListener("click", onStopSoundClick, false);
  $("toggle_recording").removeEventListener("click", onToggleRecording, false);
  $("restart_interpreter").removeEventListener("click", onRestartInterpreter, false);
  appendOutput("WebSocket disconnected");
}

function onMessageWebSocket(event) {
  appendOutput(event.data);
}

function onUnload() {
  ws.close();
}

function onStartServerClick() {
  ws.send("/start_server::");
}

function onStopServerClick() {
  ws.send("/stop_server::");
}

function onEvaluateClick() {
  var code = $("code").value;
  if (code == "") {
   return;
  }
  ws.send("/evaluate::" + code);
}

function onStopSoundClick() {
  ws.send("/stop_sound::");
}

function onToggleRecording() {
  ws.send("/toggle_recording::");
}

function onRestartInterpreter() {
  ws.send("/restart_interpreter::");
}

function onClearOutputClick() {
  setOutput("");
}

function appendOutput(msg) {
  setOutput($("stdout").value + msg);
}

function setOutput(msg) {
  var stdout = $("stdout");
  setTextContent(stdout, msg);
  stdout.scrollTop = stdout.scrollHeight;
}

function setTextContent(element, text) {
  while (element.firstChild !== null) {
    element.removeChild(element.firstChild); // remove all existing content
  }
  element.appendChild(document.createTextNode(text));
}

function init() {  
  var protocol = (location.protocol=="https:") ? "wss" :"ws";
  var host = location.host;
  var url = protocol + "://" + host + "/ws/";

  if (window.MozWebSocket) {
    window.WebSocket = window.MozWebSocket;
  }
  ws = new WebSocket(url);
  ws.addEventListener("open", onOpenWebSocket, false);
  ws.addEventListener("close", onCloseWebSocket, false);
  ws.addEventListener("message", onMessageWebSocket, false);

  window.addEventListener("unload", onUnload, false);
  $("clear_output").addEventListener("click", onClearOutputClick, false);
}

window.addEventListener("load", init, false);