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
   SENT_FUNDING_ADDRESS: 4,
   RECEIVED_FUNDING: 5,
   INITIATED: 6,
   SENT_REFUND: 7,
   RECEIVED_REFUND: 8,
   SENT_CONTRACT: 9,
   RECEIVED_CHANNEL_OPEN: 10,
   RECEIVED_ERROR: 11,
   SENT_ERROR: 12,
   RECEIVED_CLOSE: 13,
   SENT_CLOSE: 14,
   WEBSOCKET_CLOSED: 15
};

var UI_STATES = {
   BEGIN: 0,
   CONNECTED: 1,
   USER_DISCONNECTED: 2,
   FUNDS_DEPLETED: 3,
   MULTIPLE_BITMESH_TABS_OPEN: 4
};

var OUTPUT_STATES = {
   UNPROCESSED: 0,
   ATTACHED: 1,
   DEPLETED: 2
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

var fee = 1000; // fee paid for escrow tx
var min_escrow = 1000; // minimum amount to put into escrow
var min_initial_payment = 1000; // minimum initial payment, set by server
var time_purchased = 0; // time purchased for this payment channel
var total_time_purchased = 0; // total time purchased by browser
var escrow_amount = 0; // amount in multisig escrow
var price_per_second = 1; // in satoshi per second, gets set by server
var client_key, server_key; // keys for multisig escrow
var refund_address; // address to send client change to
var refund_tx; // refund transaction with lock time
var refund_amount; // amount to be returned to the client via the payment tx
var funding_keys = []; // all keys used for receiving funding
var funding_key; // key for funding the channel
var funding_address; // address for funding key
var consumer; // bitcore-payment-channel object
var coin_width; // amount of satoshi per increment

// Increment payment timer vars
var payment_update_timer; // payment timer

var started_channel = false; // have we opened the payment channel?
var waiting_to_initiate;     // are we waiting to initiate the channel?
var open_channel = true;     // should we open a channel?
var attempt_reopen_channel = false; // should we attempt to reopen an old channel?
var expiration_date;         // date when the channel expires
var previous_contract_hash;  // hash of previous contract, used to reopen channel

// Funding vars
var num_funding_outputs = 0;  // number of funding outputs we've received
var funding_outputs_used = 0; // number of funding outputs we've used in a channel
var funding_outputs = []; // unspent txo from user to be input to escrow
var funding_tx_hashes = []; // hashes of txs that have funded us

var last_increment;        // size of last payment increment made
var last_payment_update_time; // time of last payment update timer check
var received_ack = false; // whether our last payment was acked, used to tell if server is down
var fullyUsedUp = false; // whether the channel has been used up, used to change
                         // sighashtype on payment as well as chain consecutive channels
var bitmesh_url = 'https://bitmesh.network/transaction'; // url to send refund txs to

var total_paid = 0; // total paid to server
var time_spent = 0; // time spent already
var total_time_spent = 0; // total time spent over all channels
var num_unacked_payment_updates = 0; // number of times we checked if our payment was acked
var max_unacked_payment_updates = 5; // max number of such checks before reopening the channel


//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//=============================STRING VARIABLES===============================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//
//============================================================================//

// TODO: put strings in a separate file





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
var Signature = bitcore.crypto.Signature;
var Consumer = require('bitcore-channel').Consumer;
var TransactionSignature = bitcore.Transaction.Signature;
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



// TODO: some kind of notice that they may not receive their refund for lock_time

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
 * @param {new_state} The state to change to.
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
         init_keys();
         reset_counters();
         start_websockets();
         break;
      case CHANNEL_STATES.SENT_VERSION:
         break;
      case CHANNEL_STATES.RECEIVED_VERSION:
         break;
      case CHANNEL_STATES.RECEIVED_PRICE:
         // if we have already been funded, just initiate
         if (num_funding_outputs > funding_outputs_used)
         {
            initiate();
         }
         else
         {
            send_funding_address(funding_address);
         }
         break;
      case CHANNEL_STATES.SENT_FUNDING_ADDRESS:
         break;
      case CHANNEL_STATES.RECEIVED_FUNDING:
         if (waiting_to_initiate) {
            initiate();
         } else {
            waiting_to_initiate = true;
         }
         break;
      case CHANNEL_STATES.INITIATED:
         send_refund();
         break;
      case CHANNEL_STATES.SENT_REFUND:
         break;
      case CHANNEL_STATES.RECEIVED_REFUND:
         send_contract();
         break;
      case CHANNEL_STATES.SENT_CONTRACT:
         break;
      case CHANNEL_STATES.RECEIVED_CHANNEL_OPEN:
         break;
      case CHANNEL_STATES.RECEIVED_ERROR:
         start_payment_channel();
         break;
      case CHANNEL_STATES.SENT_ERROR:
         break;
      case CHANNEL_STATES.RECEIVED_CLOSE:
         start_payment_channel();
         break;
      case CHANNEL_STATES.SENT_CLOSE:
         start_payment_channel();
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
               (channel_state == CHANNEL_STATES.RECEIVED_VERSION && new_state == CHANNEL_STATES.RECEIVED_CHANNEL_OPEN) ||
               (channel_state == CHANNEL_STATES.RECEIVED_PRICE && new_state == CHANNEL_STATES.SENT_FUNDING_ADDRESS) ||
               (channel_state == CHANNEL_STATES.RECEIVED_PRICE && new_state == CHANNEL_STATES.INITIATED) ||
               (channel_state == CHANNEL_STATES.SENT_FUNDING_ADDRESS && new_state == CHANNEL_STATES.RECEIVED_FUNDING) ||
               (channel_state == CHANNEL_STATES.RECEIVED_FUNDING && new_state == CHANNEL_STATES.INITIATED) ||
               (channel_state == CHANNEL_STATES.INITIATED && new_state == CHANNEL_STATES.SENT_REFUND) ||
               (channel_state == CHANNEL_STATES.SENT_REFUND && new_state == CHANNEL_STATES.RECEIVED_REFUND) ||
               (channel_state == CHANNEL_STATES.RECEIVED_REFUND && new_state == CHANNEL_STATES.SENT_CONTRACT) ||
               (channel_state == CHANNEL_STATES.SENT_CONTRACT && new_state == CHANNEL_STATES.RECEIVED_CHANNEL_OPEN) ||
               (channel_state == CHANNEL_STATES.RECEIVED_CHANNEL_OPEN && new_state == CHANNEL_STATES.RECEIVED_CLOSE) ||
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
//      throw {};
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
      case UI_STATES.USER_DISCONNECTED:
         jQuery('#repayment_prompt').html('');
         jQuery('#disconnect_button').hide();
         jQuery('#reconnect_button').show();
         jQuery('#starting_status').hide();
         jQuery('#online_status').hide();
         jQuery('#disconnected_status').show();
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
               (new_state === UI_STATES.MULTIPLE_BITMESH_TABS_OPEN) ||
               (ui_state == UI_STATES.FUNDS_DEPLETED && new_state == UI_STATES.CONNECTED) ||
               (ui_state == UI_STATES.FUNDS_DEPLETED && new_state == UI_STATES.FUNDS_DEPLETED) ||
               (ui_state == UI_STATES.CONNECTED && new_state == UI_STATES.CONNECTED) ||
               (ui_state == UI_STATES.CONNECTED && new_state == UI_STATES.USER_DISCONNECTED) ||
               (ui_state == UI_STATES.USER_DISCONNECTED && new_state == UI_STATES.CONNECTED) ||
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

   // when we receive a message, execute this function on it
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
            case Message.MessageType.FUNDING:
               console.log("Received funding");
               receive_funding(msg.funding);
               break;
            case Message.MessageType.INITIATE:
               console.log("Received initiate");
               receive_initiate(msg.initiate);
               break;
            case Message.MessageType.RETURN_REFUND:
               console.log("Received refund signature");
               receive_refund(msg.return_refund.signature);
               break;
            case Message.MessageType.CHANNEL_OPEN:
               console.log("Received channel open");
               receive_channel_open();
               break;
            case Message.MessageType.PAYMENT_ACK:
               console.log("Received payment ack");
               receive_payment_ack();
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

   // when we receive an error, execute this function
   websocket.onerror = function(error) {
      console.log("Received error " + error.code);
      //print_object(websocket);
   };

   // when we have successfully opened the websocket, execute this
   websocket.onopen = function() {
      console.log("Websocket opened");
      websocket_open = true;
      waiting_to_initiate = true;
      if (open_channel) {
         start_payment_channel();
      }
   };

   // when the websocket closes, execute this
   websocket.onclose = function(event) {
      console.log("Received close event with code " + event.code );

      // if we closed because the server says there are multiple tabs open
      // notify user
      if (event.code === POLICY_VIOLATION)
      {
         multiple_tabs_open = true;
         set_ui_state(UI_STATES.MULTIPLE_BITMESH_TABS_OPEN);
      }
      // else try to reconnect with exponential backoff
      else
      {
         websocket_open = false;
         var timeout = 1000 * (Math.pow(2, num_connection_retries));
         console.log("Websocket closed. Attempting to restart in " + (timeout / 1000) + " seconds");
         console.log("=======================================");
         setTimeout(start_websockets, timeout);
         num_connection_retries++;
         cancel_payments();
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
 * Function that starts the payment channel
 */
function start_payment_channel() {
   cancel_payments();
   send_version();
}


/**
 * Send the browser version to the router
 */
function send_version() {
   console.log("Sending version");
   var msg;
   if (attempt_reopen_channel && previous_contract_hash !== undefined) {
      console.log("Attempting to reopen contract: ");
      print_array(previous_contract_hash);
      msg = new Message({
         "type": Message.MessageType.CLIENT_VERSION,
         "client_version": {
            "major": 1,
            "minor": 1,
            "previous_channel_contract_hash": previous_contract_hash
         }
      });
   } else {
      msg = new Message({
         "type": Message.MessageType.CLIENT_VERSION,
         "client_version": {
            "major": 1,
            "minor": 1,
         }
      });
   }
   send_message(msg);
   set_channel_state(CHANNEL_STATES.SENT_VERSION);
}

/**
 * Receive the server version and test it against our accepted versions.
 * @param  {TwoWayMessage.version} version [version that server is running]
 */
function receive_version(version) {
   if (version.major != 1 ||
      version.minor < 1) {
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
   reset_counters();
   price_per_second = price_msg.proposed_price.toNumber();

   if (price_per_second < 0) {
      console.log("Cannot pay negative price. Exiting gracefully.");
      exit_with_error_code(ChannelError.ErrorCode.OTHER);
      return;
   }

   var key_string = get_string_from_buffer(price_msg.multisig_key);

   console.log("server_key " + key_string);
   server_key = new bitcore.PublicKey(key_string, network);

   // TODO: start price bargaining here
   set_channel_state(CHANNEL_STATES.RECEIVED_PRICE);
}

/**
 * Send funding address to server
 * @param  {bitcore.Address} funding_address [address for the router to listen to]
 */
function send_funding_address(funding_address) {
   console.log("Sending funding address");
   var msg = new Message({
      "type": Message.MessageType.FUNDING_ADDRESS,
      "funding_address": {
         "funding_address": funding_address.toString()
      }
   });
   send_message(msg);

   // this should be done after seller is told to listen to address
   // otherwise the funding may be delivered without the seller hearing about it
   create_uri();
   set_channel_state(CHANNEL_STATES.SENT_FUNDING_ADDRESS);
}

/**
 * Receive message describing the funding that the user sent
 * @param  {TwoWayMessage.funding} funding [txOut that user spent on us]
 */
function receive_funding(funding) {

   var funding_tx = new bitcore.Transaction();
   funding_tx.fromString(get_string_from_buffer(funding.transaction));
   console.log("Funding tx: " + funding_tx.id);


   // if we have seen this funding transaction already, ignore it
   if (jQuery.inArray(funding_tx.id, funding_tx_hashes) != -1)
   {
      console.error("Received same funding tx twice. Ignoring tx: " + funding_tx.id);
      return;
   }


   // TODO: let them specify the refund address manually

   var refund_input = null;
   for (var i = 0; i < funding_tx.inputs.length; i++) {
      if (funding_tx.inputs[i].script.isPublicKeyHashIn()) {
         refund_input = funding_tx.inputs[i];
         break;
      }
   }

   // if they don't have a public key hash input, maybe they have a p2sh?
   if (refund_input === null) {
      for (var j = 0; j < funding_tx.inputs.length; j++) {
         if (funding_tx.inputs[j].script.isScriptHashIn()) {
            refund_input = funding_tx.inputs[j];
            break;
         }
      }
   }

   // if they don't have either, send error
   // TODO: figure out what to do here?
   if (refund_input === null) {
      console.log("WTF they sent us a transaction without address to send back to");
      exit_with_error_code(ChannelError.ErrorCode.BAD_TRANSACTION);
      // TODO refund customer or let them swipe a private key
      return;
   }


   refund_address = refund_input.script.toAddress(network_name);
   var funding_amount = 0;
   // process funding outputs
   for (var j = 0; j < funding_tx.outputs.length; j++) {
      var addr = funding_tx.outputs[j].script.toAddress(network_name).toString();
      if (addr == funding_address.toString()) {
         funding_amount += funding_tx.outputs[j].satoshis;
         //          funding_outputs.push(funding_tx.outputs[j]);
         funding_outputs.push(new bitcore.Transaction.UnspentOutput({
            "txId": funding_tx.id,
            "outputIndex": j,
            "address": addr,
            "script": funding_tx.outputs[j].script,
            "satoshis": funding_tx.outputs[j].satoshis
         }));
         funding_outputs[num_funding_outputs].output_state = OUTPUT_STATES.UNPROCESSED;
         funding_tx_hashes.push(funding_tx.id);
         num_funding_outputs++;
      }
   }

   // TODO: Let the user know?
   if (funding_amount <= 0) {
      console.log("WTF the server sent a tx that doesn't pay us.");
      exit_with_error_code(ChannelError.ErrorCode.BAD_TRANSACTION);
      return;
   }

   update_escrow_amount();
   set_channel_state(CHANNEL_STATES.RECEIVED_FUNDING);

}

/**
 * Receive an initiate message from the server
 * @param  {TwoWayMessage.Initiate} initiate_msg [message with various channel parameters]
 */
function receive_initiate(initiate_msg) {
   min_escrow = initiate_msg.min_accepted_channel_size;

   expiration_date = initiate_msg.expire_time_secs;

   min_initial_payment = initiate_msg.min_payment.toNumber();


   if (waiting_to_initiate) {
      initiate();
   } else {
      // TODO: what do we do here?
      console.error('received initate message but not expecting one');
   }
}

/**
 * Actually initiate the channel. This is separated into a separate function from
 * receive_initiate() so we can make sure we don't call it twice for a single channel
 */
function initiate() {
   console.log("Initiating payment channel");

   // this makes sure the time_left counter will be accurate now that output
   // states have changed
   update_escrow_amount();

   if (escrow_amount < min_escrow) {
      waiting_to_initiate = true;
      console.log("Funding received is too small.");
      // TODO don't refund user here, just notify them of their error
      jQuery('#repayment_prompt').html("The minimum amount to pay is " + min_escrow + " satoshis and you paid " + escrow_amount + " satoshis");
      alert("The minimum amount to pay is " + min_escrow + " satoshis and you paid " + escrow_amount + " satoshis");
      return;
   }

   //alert("Funding received! Please leave this tab open while you browse the internet.");

   // this makes sure we don't initiate twice
   waiting_to_initiate = false;

   // if the websocket breaks, this will tell it to try toreopen same channel
   attempt_reopen_channel = true;

   // this creates Consumer object, attaches funding outputs
   create_payment_channel();

   set_channel_state(CHANNEL_STATES.INITIATED);
}

/**
 * Create the payment channel object
 */
function create_payment_channel() {
   console.log("creating payment channel");
   var server_address = server_key.toAddress();
   console.log("server_address " + server_address);
   consumer = new Consumer({
      network: network_name,
      expires: expiration_date,
      fundingKey: funding_key,
      refundAddress: refund_address,
      commitmentKey: client_key,
      providerPublicKey: server_key,
      providerAddress: server_address
   });
   console.log("created payment channel");

   // TODO: think about this more
   for (var i = funding_outputs_used; i < num_funding_outputs; i++) {
      consumer.processFunding(funding_outputs[i]);
      funding_outputs[i].output_state = OUTPUT_STATES.ATTACHED;
   }
   funding_outputs_used = num_funding_outputs;
   console.log("processed funding");
}

/**
 * Send the refund tx to the server
 */
function send_refund() {
   console.log("Sending refund");

   refund_tx = consumer.setupRefund();
   console.log("Refund expires at: " + refund_tx.nLockTime);
   var msg = new Message({
      "type": Message.MessageType.PROVIDE_REFUND,
      "provide_refund": {
         "multisig_key": client_key.toPublicKey().toBuffer(),
         "tx": refund_tx.toBuffer()
      }
   });
   send_message(msg);
   set_channel_state(CHANNEL_STATES.SENT_REFUND);
}

/**
 * Receive the signature of the refund transactions
 * @param  {Buffer} refund_sig [seller's signature on the refund]
 */
function receive_refund(refund_sig) {
   //              var signature_buffer = new Uint8Array(refund_sig.buffer.slice(refund_sig.offset, refund_sig.limit - 1));
   //              refund_tx.applySignature(Signature.fromBuffer(signature_buffer));
   var signature_string = get_string_from_buffer(refund_sig);
   //                              var sigtype = Signature.SIGHASH_NONE;
   var sigtype = Signature.SIGHASH_NONE | Signature.SIGHASH_ANYONECANPAY;
   signature_string = signature_string.slice(0, signature_string.length - 2);
   var signature = new TransactionSignature({
      signature: signature_string,
      prevTxId: consumer.commitmentTx.id,
      outputIndex: 0,
      inputIndex: 0,
      publicKey: server_key,
      sigtype: sigtype,
   });
   //console.log("signature: " + signature_string);
   try {
      //console.log("refund_tx hash: " + refund_tx.id);
      refund_tx.applySignature(signature);

      if (!consumer.validateRefund(refund_tx, signature)) {
         throw "Refund not signed correctly. Exiting gracefully.";
      }
   } catch (err) {
      console.log("Error: " + err.stack);

      exit_with_error_code(ChannelError.ErrorCode.BAD_TRANSACTION);

      // TODO: refund customer or try again?
      // maybe should mark non-depleted outputs as unprocessed
      return;
   }

   post_refund_tx(refund_tx);

   set_channel_state(CHANNEL_STATES.RECEIVED_REFUND);
}

/**
 * Sends a post request to the bitcoin transaction server
 * using jQuery.
 * @param {transaction}  bitcoin transaction to send to server
 * to post to the blockchain.
 */
function post_refund_tx(transaction) {

   console.log("Transaction ID for refund: " + transaction.id);
   var serTx = transaction.toBuffer();

   var url = bitmesh_url;

   $.ajax({
      url: url,
      type: 'POST',
      data: serTx,
      processData: false,
      headers: {
         "Content-Type": "application/octet-stream",
      },
      success: function(data, status) {
         console.log("Refund successfully posted to cloud server");

         // Data will have the stack trace on a
         // bad call to the server

         // TODO: uncomment this
         //send_contract();
      },
      error: function(data, status) {
         console.log("Transaction not successfully added to server. Status: " + status);
         console.log(data);
      }
   });
}

/**
 * Sends a signed contract to the server beginning the opening of the payment
 * channel.
 */
function send_contract() {
   console.log("Sending contract");

   // get hash of contract to enable reopening later if needed
   previous_contract_hash = consumer.commitmentTx._getHash();
   previous_contract_hash = reverse_array(previous_contract_hash);
   console.log('previous_contract_hash: ');
   print_array(previous_contract_hash);

   // TODO: handle edge case where first payment is also last
   var sigtype = Signature.SIGHASH_ANYONECANPAY | Signature.SIGHASH_SINGLE;
   consumer.incrementPaymentBy(min_initial_payment, sigtype);
   last_increment = min_initial_payment;
   var signature_buffer = get_payment_update_signature(sigtype);
   refund_amount = consumer.paymentTx.outputs[0].satoshis;
   //console.log("contract hash: " + consumer.commitmentTx.id);
   console.log("paymentTx.paid = " + consumer.paymentTx.paid);
   console.log("refund_amount: " + refund_amount);

   /*
    console.log("refund_amount: " + refund_amount);
//  console.log("multisig script: " + consumer.commitmentTx.outscript.toHex());
    console.log("paymentTx: " + consumer.paymentTx.id);
    console.log("changeAddress: " + consumer.paymentTx.changeAddress.toString());
    console.log("changeScript: " + consumer.paymentTx._changeScript.toString());
   //console.log("commitment_script: " + consumer.commitmentTx.outputs[0].script.toString());
    */
   var msg = new Message({
      "type": Message.MessageType.PROVIDE_CONTRACT,
      "provide_contract": {
         "tx": consumer.commitmentTx.toBuffer(),
         "initial_payment": {
            "client_change_value": refund_amount,
            "signature": signature_buffer
         }
      }
   });
   send_message(msg);
   set_channel_state(CHANNEL_STATES.SENT_CONTRACT);
}

/**
 * Receive the channel open message from seller
 */
function receive_channel_open() {

   fullyUsedUp = false;

   if (!payment_update_timer)
   {
      var date = new Date();
      var now = date.getTime()/1000.0;
      last_payment_update_time = now;
      payment_update_timer = setInterval(update_payment, 100);
   }
   else
   {
      console.error("Cannot open one channel in the middle of another");
   }

   set_channel_state(CHANNEL_STATES.RECEIVED_CHANNEL_OPEN);
   set_ui_state(UI_STATES.CONNECTED);
}

/**
 * Increment the payment. This involves changing the transaction and resigning it.
 */
function increment_payment() {
   var increment = coin_width;
   // remainder = how much will be left for us after this increment
   // if remainder is positive, but smaller than dust, then just finish out the channel
   // if remainder is negative, we need to reduce the increment. finish out the channel
   // if remainder is zero, increment is already the remainder of the channel
   var remainder = consumer.paymentTx.amount - consumer.paymentTx.paid - increment;
   var sigtype = Signature.SIGHASH_ANYONECANPAY | Signature.SIGHASH_SINGLE;

   if (remainder < min_initial_payment) {
      increment = consumer.paymentTx.amount - consumer.paymentTx.paid;
      fullyUsedUp = true;
   }

   if (fullyUsedUp) {
      sigtype = Signature.SIGHASH_ANYONECANPAY | Signature.SIGHASH_NONE;
   }

   if (increment < 0) {
      console.error("Error: Increment was negative");
      exit_with_error_code(ChannelError.ErrorCode.SYNTAX_ERROR);
      return;
   }

   last_increment = increment;
   consumer.incrementPaymentBy(increment, sigtype);
   var signature_buffer = get_payment_update_signature(sigtype);
   if (consumer.paymentTx.outputs[0]) {
      refund_amount = consumer.paymentTx.outputs[0].satoshis;
   } else {
      refund_amount = 0;
   }
   /*
    console.log("Remainder: " + remainder);
    console.log("PaymentTx.amount = " + consumer.paymentTx.amount);
    console.log("PaymentTx.paid = " + consumer.paymentTx.paid);
    console.log("refund_amount: " + refund_amount);
//  console.log("redeemScript: " + consumer.paymentTx.inputs[0].redeemScript.toString());
//  console.log("redeemScript: " + consumer.paymentTx.inputs[0].redeemScript.toHex());
    console.log("+++++++++++++++++");
*/
   var msg = new Message({
      "type": Message.MessageType.UPDATE_PAYMENT,
      "update_payment": {
         "client_change_value": refund_amount,
         "signature": signature_buffer
      }
   });
   send_message(msg);
   console.log("Sent payment update");
   received_ack = false;
}


/**
 * Receive a payment ack from the server. Waitin until here to increment
 * time_purchased lets us keep a little closer to the server's timer
 */
function receive_payment_ack() {
   var purchase_amount = last_increment / price_per_second;
   console.log("Increasing time purchased by " + last_increment + " / " + price_per_second + " = " + purchase_amount);
   time_purchased += purchase_amount;
   total_time_purchased += purchase_amount;
   total_paid += last_increment;
   //  time_purchased = consumer.paymentTx.paid/price;
   received_ack = true;
}

/**
 * Receive a close message from the seller. This happens for random reasons as
 * well as when the funds are depleted.
 */
function receive_close() {
   // if the channel is used up
   if (fullyUsedUp) {

      // set each input for the last contract to state DEPLETED
      for (var i = 0; i < funding_outputs.length; i++)
      {
         var state = funding_outputs[i].output_state;
         if (state == OUTPUT_STATES.ATTACHED)
         {
            state = OUTPUT_STATES.DEPLETED;
            funding_outputs[i].output_state = OUTPUT_STATES.DEPLETED;
         }
      }

      // if there are not more funds waiting for us
      if (num_funding_outputs <= funding_outputs_used)
      {
         set_ui_state(UI_STATES.FUNDS_DEPLETED);
         alert('You have run out of funds. If you would like to continue internetting, please pay more money');

      }
      else
      {
         console.log("Payment channel ended. Immediately starting new one.");
      }
      attempt_reopen_channel = false;
      open_channel = true;

   } else {
      jQuery('#repayment_prompt').html('Channel closed unexpectedly. Will attempt to reopen.');
      console.log("Received unexpected close message.");
      attempt_reopen_channel = true;
      open_channel = true;

   }

   set_channel_state(CHANNEL_STATES.RECEIVED_CLOSE);

}

/**
 * Send close messgae to seller
 */
function close_channel() {
   console.log('Closing channel');
   var msg = new Message({
      "type": Message.MessageType.CLOSE
   });

   send_message(msg);
   attempt_reopen_channel = false;
   set_channel_state(CHANNEL_STATES.SENT_CLOSE);
}

/**
 * Receive error message from seller.
 * @param  {TwoWayMessage.Error} error [Error message describing what went wrong]
 */
function receive_error(error) {

   attempt_reopen_channel = true;
   set_channel_state(CHANNEL_STATES.RECEIVED_ERROR);
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

/**
 * Called when user presses the "disconnect" button. Stops payment channel,
 * but server does not refund their money yet.
 */
function user_disconnect() {

   console.log("User disconnected.");


   if (consumer.paymentTx.amount - consumer.paymentTx.paid <= min_initial_payment)
   {
      console.log("User tried to disconnect but the channel is almost done");
      jQuery('#repayment_prompt').html("Channel already depleted. Please wait one moment.");
   }
   else
   {
      // we send an error message here to allow for possibility of reopening channel.
      // this should probably be made more robust in future
      var msg = new Message({
         "type": Message.MessageType.ERROR,
         "error": {
            "code": ChannelError.ErrorCode.OTHER
         }
      });
      send_message(msg);
      cancel_payments();

      // set this to false so websockets don't automatically reopen the channel
      open_channel = false;

      set_channel_state(CHANNEL_STATES.SENT_ERROR);
      set_ui_state(UI_STATES.USER_DISCONNECTED);
   }
}

/**
 * Called when user presses the "reconnect" button. Basically restarts channel.
 */
function user_reconnect() {
   console.log("User reconnected.");
   attempt_reopen_channel = true;
   fullyUsedUp = false;

   // set this to true so websockets automatically reopen the channel if they fail
   open_channel = true;

   send_version();
   set_ui_state(UI_STATES.CONNECTED);
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
 * Initialize the funding/client keys
 */
function init_keys() {
   //  funding_key = new bitcore.PrivateKey('75d79298ce12ea86863794f0080a14b424d9169f7e325fad52f60753eb072afc', network_name);
   set_funding_key(new bitcore.PrivateKey(network_name));
   client_key = new bitcore.PrivateKey(network_name);
   console.log("Funding address: " + funding_address.toString());
}

/**
 * Set the funding key
 * @param {PrivateKey} key [Key to set as funding key]
 */
function set_funding_key(key)
{
   funding_keys.push(funding_key);
   funding_key = key;
   funding_address = funding_key.toPublicKey().toAddress();
}

/**
 * Displays the wif key
 */
function display_wif_key()
{
   console.log(funding_key.toWIF());
}

/**
 * Use the user supplied wif key.
 */
function restore_wif_key()
{
   // TODO: only be able to do this dependent on state

   // TODO: catch invalid strings here
   set_funding_key(bitcore.PrivateKey.fromWIF(jQuery('#restore_key').value));
   create_uri();
}

/**
 * Create the bitcoin uri. Also builds the qr code.
 */
function create_uri() {
   var uri = new bitcore.URI({
      address: funding_address,
      //      message: get_user_memo(),
      //      amount: 10000
   });
   var uriString = uri.toString();
 //  var img = jQuery('#logo')[0];
   //      jQuery('#qrcode').qrcode(uriString);
   jQuery('#uri_string').html(funding_address.toString());
   jQuery('#qr_wrapper').html('<a id="qr_link" href="' + uriString + '"></a>');
   //    jQuery('#qr_wrapper').html('<div class="pop-left" data-container="body" data-toggle="popover" data-placement="left" data-content="<a id=' + "'qr_link' href=" + uriString + '"></a></div>');
   jQuery('#qr_link').qrcode({
      'size': 150,
      'ecLevel': 'H',
      'radius': 0,
      'top': 0,
      'minVersion': 8,
      //     'fill': '#000',
      // 	'color': '#FAF',
      'text': uriString,
      'background': '#FFF',
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
         return '<a href="'+funding_address.toString()+'">'+funding_address.toString()+'</a>';
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
 * Prints an array. Used for debugging
 * @param  {Array} array [array to print]
 */
function print_array(array) {
   var string = "";
   for (var i = 0; i < array.length; i++) {
      var byt = array[i] < 16 ? '0' + array[i].toString(16) : array[i].toString(16);
      string += byt;
   }
   console.log(string);
}

/**
 * Reverses an array. For some reason the built in one didn't do what we want
 * @param  {Array} array [array to reverse]
 * @return {Array}       [reversed array]
 */
function reverse_array(array) {
   var length = array.length;
   var reversed = new Uint8Array(length);
   for (var i = 0; i < length; i++) {
      reversed[i] = array[length - i - 1];
   }
   return reversed;
}

/**
 * Get the signature for the payment update
 * @param  {Number} sigtype [type of signature, sighash_one or sighash_none]
 * @return {Buffer}         [Buffer with signature in it]
 */
function get_payment_update_signature(sigtype) {
   // TODO: assert the shit out of this routine
   var signature = consumer.paymentTx.getSignatures(client_key, sigtype)[0];
   var signature_buffer = signature.signature.toBuffer();
   var sig_buffer = new Uint8Array(signature_buffer.length + 1);
   for (var i = 0; i < signature_buffer.length; i++) {
      sig_buffer[i] = signature_buffer[i];
   }
   sig_buffer[signature_buffer.length] = sigtype;
   return sig_buffer;
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
 * Reset all the counters, called when a new channel is opened
 */
function reset_counters() {
   num_connection_retries = 0;
   time_spent = 0;
   time_purchased = 0;
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
 * Convert between satoshis and USD
 * @param  {Number} satoshis - amount of satoshis to convert
 * @return {Number} USD amount representing the amount of satoshis given
 */
function convert_satoshis_to_usd(satoshis)
{
   return USD_price * satoshis/100000000;
}

/**
 * Recalculate how much has been sent to the browser
 */
function update_escrow_amount() {
   // Sum the total amount of funding outputs we haven't used yet
   var total = 0;
   for (var i = 0; i < funding_outputs.length; i++) {
      if (funding_outputs[i].output_state != OUTPUT_STATES.DEPLETED)
      {
         total += funding_outputs[i].satoshis;
      }
   }
   escrow_amount = total - fee;
   coin_width = escrow_amount / 100;

   // Don't let the coin_width get too low
   if (coin_width < price_per_second)
   {
      coin_width = price_per_second;
   }
   console.log("coin_width: " + coin_width);
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

   if (time_purchased - time_spent < 5 && internet_is_reachable()) {
      if (received_ack) {
         num_unacked_payment_updates = 0;
         if (!fullyUsedUp) {
            increment_payment();
         }
      } else if (num_unacked_payment_updates > max_unacked_payment_updates) {
         console.log("Haven't received ack for payment yet: " + num_unacked_payment_updates);
         cancel_payments();
         exit_with_error_code(ChannelError.ErrorCode.OTHER);
      }
   }
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
   return (escrow_amount / price_per_second) - time_spent;
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
   window.onbeforeunload = function() {

      if(!multiple_tabs_open)
      {
         return 'If you leave this page, your internet connection will be severed';
      }

   };

   window.onunload = function(){
         close_channel();
   };


   set_ui_state(UI_STATES.BEGIN);
   set_channel_state(CHANNEL_STATES.BEGIN);
});



// THIS IS SO BROWSERS WILL LET US DEBUG DYNAMICALLY LOADED JS
//# sourceURL=bitmesh.all.js
