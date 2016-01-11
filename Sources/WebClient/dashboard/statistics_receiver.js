var server_IP = "10.0.19.84";
var server_websocket_port = 48911;
var websocket = new WebSocket("ws://" + server_IP + ':' + server_websocket_port);

websocket.onmessage = function(evt) {
   console.log("Received message: " + evt.data);
};

websocket.onopen = function() {
   websocket.send("Hello");
};

websocket.onclose = function(event) {
   console.log('websocket closing with code: ' + event.code);
};

