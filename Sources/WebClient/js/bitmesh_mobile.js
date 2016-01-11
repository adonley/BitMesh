//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//===========================STATE MACHINE VARIABLES==========================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//


var CHANNEL_STATES = {
   BEGIN: 0,
   SENT_VERSION: 1,
   RECEIVED_VERSION: 2,
   RECEIVED_PRICE: 3,
   RECEIVED_FUNDING: 4,
   RECEIVED_ERROR: 5,
   SENT_ERROR: 6,
   RECEIVED_CLOSE: 7,
   SENT_CLOSE: 8,
   WEBSOCKET_CLOSED: 9
};

var UI_STATES = {
   BEGIN: 0,
   CONNECTED: 1,
   FUNDS_DEPLETED: 2,
   MULTIPLE_BITMESH_TABS_OPEN: 3
};

var channel_state; // currenty channel state
var ui_state;      // current ui state


//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//=================================CHANNEL VARIABLES==========================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//

var time_purchased = 0; // time purchased for this payment channel
var price_per_second = 1; // in satoshi per second, gets set by server
var server_address; // address for user to pay to


var total_paid = 0; // total paid to server
var time_spent = 0; // time spent already
var total_time_spent = 0; // total time spent over all channels

var payment_update_timer; // payment timer
var last_payment_update_time; // time of last payment update timer check



//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//=================================LIBRARY VARIABLES==========================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//

var bitmesh_test = true; // should we be in testnet or not?

// Protobuf vars
var ProtoBuf = dcodeIO.ProtoBuf;
var channel_builder = ProtoBuf.loadProtoFile("../proto/paymentchannel.proto");
var Message = channel_builder.build("paymentchannels.TwoWayChannelMessage");
var ChannelError = channel_builder.build("paymentchannels.Error");

// Bitcore vars
var bitcore = require('bitcore');
var network = bitmesh_test ? {
   network: 'testnet'
} : {
   network: 'mainnet'
};

var network_name = bitmesh_test ? 'testnet' : 'mainnet';


//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//===============================WEBSOCKET VARIABLES==========================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//


var websocket; // socket to communicate with router over
var websocket_open = false; // is the websocket open yet?
var multiple_tabs_open = false; // are there multiple tabs open?
var num_connection_retries = 0; // how many times have we tried to reconnect?
var server_IP =  "10.0.19.84"; // ip address of the server
var server_websocket_port = 11984; // port to websocket over with router
var POLICY_VIOLATION = 1008; // this is a websocket constant error code we
                              //use to indicate multiple tabs are open
var USD_price; // price of bitcoin in USD




//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//===========================STATE MACHINE FUNCITONS==========================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//


/**
 * Changes the state to the new_state. If the state is not in the correct order
 * then an IllegalStateException is thrown.
 * @param {number} The state to change to.
 */
function set_channel_state(new_state) {
   if (!channel_transition_is_valid(new_state)) {
      console.error('Illegal channel transition from ' + channel_state + ' to ' + new_state);
      throw {};
   }
   console.log('setting channel_state ' + new_state);
   channel_state = new_state;

   switch (channel_state)
   {
      case CHANNEL_STATES.BEGIN:
         get_bitcoin_price();
         start_websockets();
         break;
      case CHANNEL_STATES.SENT_VERSION:
         break;
      case CHANNEL_STATES.RECEIVED_VERSION:
         break;
      case CHANNEL_STATES.RECEIVED_PRICE:

         break;
      case CHANNEL_STATES.RECEIVED_FUNDING:
         break;
      case CHANNEL_STATES.RECEIVED_ERROR:
         break;
      case CHANNEL_STATES.SENT_ERROR:
         break;
      case CHANNEL_STATES.RECEIVED_CLOSE:
         break;
      case CHANNEL_STATES.SENT_CLOSE:
         break;
   }
}

/**
 * Defines the edges in the channel state machine
 * @param  {CHANNEL_STATE} new_state - state to transition to
 * @return {boolean}  whether or not the transition was valid
 */
function channel_transition_is_valid(new_state) {
   var valid = (channel_state === undefined && new_state == CHANNEL_STATES.BEGIN) ||
               (channel_state == CHANNEL_STATES.BEGIN && new_state == CHANNEL_STATES.SENT_VERSION) ||
               (channel_state == CHANNEL_STATES.WEBSOCKET_CLOSED && new_state == CHANNEL_STATES.SENT_VERSION) ||
               (channel_state == CHANNEL_STATES.SENT_VERSION && new_state == CHANNEL_STATES.SENT_VERSION) ||
               (channel_state == CHANNEL_STATES.SENT_VERSION && new_state == CHANNEL_STATES.RECEIVED_VERSION) ||
               (channel_state == CHANNEL_STATES.SENT_VERSION && new_state == CHANNEL_STATES.SENT_VERSION) ||
               (channel_state == CHANNEL_STATES.RECEIVED_VERSION && new_state == CHANNEL_STATES.RECEIVED_PRICE) ||
               (channel_state == CHANNEL_STATES.RECEIVED_PRICE && new_state == CHANNEL_STATES.RECEIVED_FUNDING) ||
               (channel_state == CHANNEL_STATES.RECEIVED_CLOSE && new_state == CHANNEL_STATES.SENT_VERSION) ||
               (channel_state == CHANNEL_STATES.SENT_CLOSE && new_state == CHANNEL_STATES.SENT_VERSION) ||
               (channel_state == CHANNEL_STATES.SENT_ERROR && new_state == CHANNEL_STATES.SENT_VERSION) ||
               new_state == CHANNEL_STATES.RECEIVED_CLOSE ||
               new_state == CHANNEL_STATES.RECEIVED_ERROR ||
               new_state == CHANNEL_STATES.SENT_ERROR ||
               new_state == CHANNEL_STATES.SENT_CLOSE ||
               new_state == CHANNEL_STATES.WEBSOCKET_CLOSED ||
               new_state == CHANNEL_STATES.RECEIVED_FUNDING;
   return valid;
}

/**
 * Change the ui_state variable
 * @param enum new_state - state to transition to
 */
function set_ui_state(new_state) {
   if (!ui_transition_is_valid(new_state)) {
      console.error('Illegal ui transition from ' + ui_state + ' to ' + new_state);
   }

   console.log('setting ui_state ' + new_state);
   ui_state = new_state;

   switch (ui_state)
   {
      case UI_STATES.BEGIN:
         update_display();
         jQuery('#repayment_prompt').html('');
         jQuery('#disconnect_button').hide();
         jQuery('#reconnect_button').hide();
         jQuery('#starting_status').show();
         jQuery('#online_status').hide();
         jQuery('#disconnected_status').hide();
         jQuery('#mult_tabs_status').hide();
         jQuery('#multiple_tabs_gif').hide();
         jQuery('#qr_div').show();
         break;
      case UI_STATES.CONNECTED:
         jQuery('#repayment_prompt').html('');
         jQuery('#disconnect_button').show();
         jQuery('#reconnect_button').hide();
         jQuery('#starting_status').hide();
         jQuery('#online_status').show();
         jQuery('#disconnected_status').hide();
         jQuery('#mult_tabs_status').hide();
         jQuery('#multiple_tabs_gif').hide();
         jQuery('#qr_div').show();
         break;
      case UI_STATES.FUNDS_DEPLETED:
         jQuery('#repayment_prompt').html('You have run out of funds. If you would like to continue internetting, please pay more money');
         jQuery('#disconnect_button').hide();
         jQuery('#reconnect_button').hide();
         jQuery('#starting_status').hide();
         jQuery('#online_status').hide();
         jQuery('#disconnected_status').show();
         jQuery('#mult_tabs_status').hide();
         jQuery('#multiple_tabs_gif').hide();
         jQuery('#qr_div').show();
         break;
      case UI_STATES.MULTIPLE_BITMESH_TABS_OPEN:
         jQuery('#repayment_prompt').html('');
         jQuery('#disconnect_button').hide();
         jQuery('#reconnect_button').hide();
         jQuery('#starting_status').hide();
         jQuery('#online_status').hide();
         jQuery('#disconnected_status').hide();
         jQuery('#mult_tabs_status').show();
         jQuery('#multiple_tabs_gif').show();
         jQuery('#qr_div').hide();
         break;
   }
}

/**
 *  Defines the edges in the ui state machine
 * @param  {enum} new_state [state to transition to]
 * @return {boolean} whether or not the transition was valid
 */
function ui_transition_is_valid(new_state) {
   var valid = (ui_state === undefined && new_state == UI_STATES.BEGIN) ||
               (ui_state == UI_STATES.BEGIN && new_state == UI_STATES.CONNECTED) ||
               (ui_state == UI_STATES.FUNDS_DEPLETED && new_state == UI_STATES.CONNECTED) ||
               (new_state === UI_STATES.MULTIPLE_BITMESH_TABS_OPEN) ||
               (ui_state == UI_STATES.FUNDS_DEPLETED && new_state == UI_STATES.FUNDS_DEPLETED) ||
               (ui_state == UI_STATES.CONNECTED && new_state == UI_STATES.CONNECTED) ||
               (ui_state == UI_STATES.CONNECTED && new_state == UI_STATES.FUNDS_DEPLETED);
   return valid;
}

//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//===========================WEBSOCKET FUNCTIONS==============================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//

/**
 * Send a message over the websocket to the router. Expects message to be a
 * protobuf.
 * @param  {protobuf message} message [message to send to router]
 */
function send_message(message) {
   var arrayBuffer = message.encode().toArrayBuffer();
   if (websocket.readyState === WebSocket.OPEN) {
      websocket.send(arrayBuffer);
   } else {
      console.log("Did not send message because websocket was in state: " + websocket.readyState);
   }
}

/**
 * Start up the websockets and set the event response functions
 */
function start_websockets() {
   console.log("start_websockets");
   websocket = new WebSocket("ws://" + server_IP + ':' + server_websocket_port);
   websocket.binaryType = "arraybuffer"; // We are talking binary
   // window.onbeforeunload = websocket.close;

   websocket.onmessage = function(evt) {
      try {
         // Decode the Message
         var msg = Message.decode(evt.data);
         switch (msg.type) {
            case Message.MessageType.SERVER_VERSION:
               console.log("Received server version");
               receive_version(msg.server_version);
               break;
            case Message.MessageType.PRICE:
               console.log("Received price message");
               receive_price(msg.price);
               break;
            case Message.MessageType.PAYMENT_ACK:
               console.log("Received payment ack");
               receive_payment_ack(msg.payment_ack);
               break;
            case Message.MessageType.CLOSE:
               console.log("Received close message");
               receive_close();
               break;
            case Message.MessageType.ERROR:
               console.log("Received error message " + msg.error.code);
               receive_error(msg.error.code);
               break;
         }
      } catch (err) {
         console.log("Error: \n" + err.stack);
      }
   };

   websocket.onerror = function(error) {
      console.log("Received error " + error.code);
      //print_object(websocket);
   };

   websocket.onopen = function() {
      console.log("Websocket opened");
      websocket_open = true;
      start_conversation();
   };

   websocket.onclose = function(event) {
      console.log("Received close event with code " + event.code );
      if (event.code === POLICY_VIOLATION)
      {
         //alert('Multiple tabs open');
         multiple_tabs_open = true;
         set_ui_state(UI_STATES.MULTIPLE_BITMESH_TABS_OPEN);
      }
      else
      {
         websocket_open = false;
         var timeout = 1000 * (Math.pow(2, num_connection_retries));
         console.log("Websocket closed. Attempting to restart in " + (timeout / 1000) + " seconds");
         console.log("=======================================");
         setTimeout(start_websockets, timeout);
         num_connection_retries++;
         set_channel_state(CHANNEL_STATES.WEBSOCKET_CLOSED);
      }
   };
}


//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//===========================CHANNEL FUNCTIONS================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//

/**
 * Function that starts the conversation
 */
function start_conversation() {
   send_version();
}

/**
 * Send the browser version to the router
 */
function send_version() {
   console.log("Sending version");
   var msg = new Message({
      "type": Message.MessageType.CLIENT_VERSION,
      "client_version": {
         "major": 1,
         "minor": 2,
      }
   });
   send_message(msg);
   set_channel_state(CHANNEL_STATES.SENT_VERSION);
}

/**
 * Receive the server version and test it against our accepted versions.
 * @param  {TwoWayMessage.version} version [version that server is running]
 */
function receive_version(version) {
   if (version.major != 1 ||
      version.minor != 2) {
      console.log("Unsupported server version. Exiting gracefully.");
      exit_with_error_code(ChannelError.ErrorCode.NO_ACCEPTABLE_VERSION);
      return;
   }
   set_channel_state(CHANNEL_STATES.RECEIVED_VERSION);
}

/**
 * Receive price message from the seller
 * @param  {TwoWayMessage.price} price_msg [price message receive via websockets]
 */
function receive_price(price_msg) {
   // reset_counters bc if we got here, it means we opened a new channel
   // TODO: if seller rejects reopen request, then inform user their money will be refunded
   price_per_second = price_msg.proposed_price.toNumber();

   if (price_per_second < 0) {
      console.log("Cannot pay negative price. Exiting gracefully.");
      exit_with_error_code(ChannelError.ErrorCode.OTHER);
      return;
   }

   var key_string = get_string_from_buffer(price_msg.multisig_key);

   console.log("server_key " + key_string);
   server_key = new bitcore.PublicKey(key_string, network);
   server_address = server_key.toAddress();
   create_uri(server_address);


   // TODO: start price bargaining here
   set_channel_state(CHANNEL_STATES.RECEIVED_PRICE);
}

/**
 * Receive a payment ack from the server. Waitin until here to increment
 * time_purchased lets us keep a little closer to the server's timer
 */
function receive_payment_ack(payment_ack) {

   var purchase_amount = payment_ack.amount_purchased;
   var payment_amount = payment_ack.amount_spent;

   console.log("Sanity check: purchase_amount = " + purchase_amount);
   console.log("payment_amount = " + payment_amount);
   console.log("price_per_second = " + price_per_second);

   console.log("Increasing time purchased by " + purchase_amount);
   time_purchased = parseInt(time_purchased);
   time_purchased += purchase_amount;
   total_paid += payment_amount;

   var date = new Date();
   var now = date.getTime()/1000.0;
   last_payment_update_time = now;
   payment_update_timer = setInterval(update_payment, 100);

   received_ack = true;
   update_display();
   set_ui_state(UI_STATES.CONNECTED);
}

/**
 * Receive a close message from the seller. This happens for random reasons as
 * well as when the funds are depleted.
 */
function receive_close() {
   set_ui_state(UI_STATES.FUNDS_DEPLETED);
   set_channel_state(CHANNEL_STATES.RECEIVED_CLOSE);
   cancel_payments();
}

/**
 * Receive error message from seller.
 * @param  {TwoWayMessage.Error} error [Error message describing what went wrong]
 */
function receive_error(error) {
   attempt_reopen_channel = true;
   set_channel_state(CHANNEL_STATES.RECEIVED_ERROR);
   cancel_payments();
}

/**
 * Exits gracefully and sends error to server.
 * @param  {TwoWayMessage.Error} error_code [error code to send to server]
 */
function exit_with_error_code(error_code) {
   console.log("Exiting gracefully with code: " + error_code);
   var msg = new Message({
      "type": Message.MessageType.ERROR,
      "error": {
         "code": error_code
      }
   });
   send_message(msg);
   attempt_reopen_channel = true;

   jQuery('#disconnect_button').hide();
   jQuery('#reconnect_button').hide();

   set_channel_state(CHANNEL_STATES.SENT_ERROR);
   // TODO: tell user what's up
}


//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//===========================UTILITY FUNCTIONS================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//

/**
 * Create the bitcoin uri. Also builds the qr code.
 */
function create_uri(address) {
   var uri = new bitcore.URI({
      address: address,
      //      message: get_user_memo(),
      //      amount: 10000
   });
   var uriString = uri.toString();
 //  var img = jQuery('#logo')[0];
   //      jQuery('#qrcode').qrcode(uriString);
   jQuery('#uri_string').html(address.toString());
   jQuery('#qr_wrapper').html('<a id="qr_link" href="' + uriString + '"></a>');
   //    jQuery('#qr_wrapper').html('<div class="pop-left" data-container="body" data-toggle="popover" data-placement="left" data-content="<a id=' + "'qr_link' href=" + uriString + '"></a></div>');
   jQuery('#qr_link').qrcode({
      'size': 150,
      'ecLevel': 'H',
      'radius': 0,
      'top': 0,
      'minVersion': 8,
      //     'fill': '#000',
      //    'color': '#FAF',
      'text': uriString,
      //'background': '#FFF',
  //    'mode': 4,
   //   'mSize': 0.05,
   //   'image': img
   });


   $('.uri-popover').popover({
      trigger:'hover',
      placement:'right',
      container:'body',
      html:true,
      title: 'URI Code:',
      content:function(){
         return '<a href="'+address.toString()+'">'+address.toString()+'</a>';
      }
   });
}

/**
 * Should return whether or not it can reach some website.
 * @return whether the internet is reachable or not
 */
function internet_is_reachable() {
   // TODO: this
   return true;
}

/**
 * Get's the current price, in USD, from blockchain.info
 */
function get_bitcoin_price() {
   var location = "transaction";
   var url = "https://blockchain.info/ticker?cors=true";

   $.ajax({
      url: url,
      type: 'GET',
      async: true,
      success: function(data, status) {
         USD_price = data.USD.last;
         console.log(USD_price);
         update_display();
      }
   });
}

/**
 * Extract string from a Buffer. Just a type adapter.
 * @param {Buffer} [buffer] [buffer to turn into string]
 */
function get_string_from_buffer(buffer) {
   var string = "";
   var array = new Uint8Array(buffer.buffer);

   for (var i = buffer.offset; i < buffer.limit; i++) {
      var byt = array[i] < 16 ? '0' + array[i].toString(16) : array[i].toString(16);
      string += byt;
   }
   return string;
}

/**
 * Convert between satoshis and USD
 * @param  {Number} satoshis - amount of satoshis to convert
 * @return {Number} USD amount representing the amount of satoshis given
 */
function convert_satoshis_to_usd(satoshis)
{
   return USD_price * satoshis/100000000;
}

/**
 *  Recursive function that prints an object
 * @param  {Object} object - object to describe
 * @param  {String} indent - optional parameter - only used in recursion
 * @return {String}        String representing the object
 */
function print_object(object, indent)
{
   if (object === undefined)
   {
      return 'undefined';
   }
   if (indent === undefined)
   {
      console.log(print_object(object, ''));
      return;
   }

   if (typeof object == 'function')
   {
      return 'function()';
   }
   if (typeof object !== "object")
   {
      return object;
   }

   var new_indent = indent + '   ';
   var string = '{\n';
   for (var key in object)
   {
      string += new_indent + key + " : " + print_object(object[key], new_indent + '   ') + '\n';
   }
   string += indent + '}\n';
   return string;
}

//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//=============================TIMER FUNCTIONS================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//


/**
 * This gets called by the payment timer periodically
 */
function update_payment() {
   var date = new Date();
   var now = date.getTime()/1000.0;
   var delta = now - last_payment_update_time;
   last_payment_update_time = now;
   time_spent += delta;
   total_time_spent += delta;
   console.log('Time spent:' + time_spent);
   //console.log('Total time spent:' + total_time_spent++);
   console.log('Time purchased:' + time_purchased);
   console.log('++++++++++++++++++++++');
   //console.log('Total time purchased:' + total_time_purchased);
   update_display();
}

/**
 * Cancel the paymemnt timer. No more incrementing payments.
 */
function cancel_payments() {
   if (payment_update_timer) {
      clearInterval(payment_update_timer);
      payment_update_timer = null;
   }
}

/**
 * Update the user dashboard
 */
function update_display() {
   var time_left = get_time_left();
   var time, hours, minutes, seconds;
   var price_display;
   if (time_left > 0) {

      hours = Math.floor(time_left / 3600);
      hours = hours < 10 ? '0' + hours : hours;

      minutes = Math.floor((time_left % 3600) / 60);
      minutes = minutes < 10 ? '0' + minutes : minutes;

      seconds = Math.floor(time_left % 60);
      seconds = seconds < 10 ? '0' + seconds : seconds;

      time = hours + ':' + minutes + ':' + seconds;

   } else {

      time = "00:00:00";
      hours = '00';
      minutes = '00';
      seconds = '00';

   }
   var money = (total_paid / 100) + ' uBTC' + '<br>';
   if (USD_price !== undefined)
   {
      money += '$' + (convert_satoshis_to_usd(total_paid)).toFixed(4);
   }

   if (price_per_second !== undefined)
   {
      var satoshis_per_hour = price_per_second * 3600;
      var price_amount, price_unit;
      if (satoshis_per_hour >= 100) {
         price_unit = 'uBTC';
         price_amount = satoshis_per_hour / 100;
      } else {
         price_unit = 'satoshis';
         price_amount = price_per_second;
      }
      price_display = price_amount + ' ' + price_unit + '<br />';
      if (USD_price !== undefined)
      {
         price_display += '$' + (convert_satoshis_to_usd(satoshis_per_hour)).toFixed(4);
      }
   }
   else
   {
      price_display = '?';
   }

   jQuery('#price_display').html(price_display);
   jQuery('#money_spent').html(money);
   jQuery('#hours').html(hours+"<br />Hrs");
   jQuery('#minutes').html(minutes+"<br />Mins");
   jQuery('#seconds').html(seconds+"<br />Secs");
   jQuery('#title_display').html(time);
}

/**
 * Returns the amount of time left
 */
function get_time_left() {
   return (total_paid / price_per_second) - time_spent;
}


//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//================================BEGIN SCRIPT================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//

jQuery(document).ready(function() {
   set_ui_state(UI_STATES.BEGIN);
   set_channel_state(CHANNEL_STATES.BEGIN);
});



// THIS IS SO BROWSERS WILL LET US DEBUG DYNAMICALLY LOADED JS
//# sourceURL=bitmesh_mobile.all.js
