$(document).ready(function() 
{   
    hide_elements();    

    var test = true;
	var bitcore = require('bitcore');
    var client_port;
    var gateway_port = "8080";
    var gateway;
    var cost;
    var cost_url;
    
    if(test)
    {
        client_port = "8081";
    }
    else
    {
        client_port = "8080";
    }
	
    event_update("Initiating BitMesh...");
    get_gateway();
	create_uri();

    //var cost_url = "http://" + gateway + ":8080/cost";

    function hide_elements()
    {
        $("#to_address_dropdown").hide();
        $("#send_status").hide();
        $("#refresh_address_input").hide();
    }

    // Show value of price on slider
    $('#price_unit_range').on("change mousemove", function() 
    {
        $("#price_per_unit_display").html($(this).val());
    });

    $("#refresh_address_input").on("click", function(e)
    {
        e.preventDefault();
        $("#refresh_address_input").hide();
        $("#send_status").hide();
        $("#send_status").empty();
        $("#send_input_field").show();
    });

    $("#send_money").on("click", function(e)
    {
        e.preventDefault();
        var send_input = $("#send_input_field");
        send_input.hide();

        // Make pending
        var send_status = $("#send_status");
        send_status.html("Transaction Pending...");
        send_status.show();

        // Check input addr ->
        var data = { to_address:$('#to_address').val() };

        $.ajax({
            type: 'POST',
            url: "send",
            data: JSON.stringify(data),
            contentType: 'application/json;',
            dataType: 'json',
            success: function(data)
            {
                console.log(data);
                // Websockets should show the new balance
                send_status.empty();
                if(data.status == "ok")
                    send_status.html("<small><b>Tx: </b>" + data.reason + "</small>");
                else
                    send_status.html(data.reason);
                $("#refresh_address_input").show();
            },
            error: function(xhr, textStatus, errorThrown) 
            {
                console.log('Wierd error here. BitMesh client not up?');
                console.log(errorThrown);
            }
        });

    });
    
    $('#connect').on("click", function(e) 
    {
    	e.preventDefault();
    	
    	var price_data = { price_scalar : parseInt($("#price").html()) };
    
        // State is reflected in the button type
        // success == not connected -> warning == connected
        if(! $("#connect").hasClass("btn-success"))
        {
            $.ajax({
                type: 'POST',
                url: "disconnect",
                contentType: 'application/json;',
                dataType: 'json',
                success: function()
                {
                    var connect_button = $("#connect");
                    var connect_button_text = $("#connect_button_text");
                    console.log("toggled connection micropayments.");
                    connect_button.removeClass("btn-warning");
                    connect_button.addClass("btn-success");
                    connect_button_text.empty();
                    connect_button_text.html(" Connect");
                },
                error: function(xhr, textStatus, errorThrown) 
                {
                    console.log('error');
                    console.log(errorThrown);
                }
            });
        }
        else
        {
            $.ajax({
                type: 'POST',
                url: "connect",
                data: JSON.stringify(price_data),
                contentType: 'application/json;',
                dataType: 'json',
                success: function()
                {
                    var connect_button = $("#connect");
                    var connect_button_text = $("#connect_button_text");
                    connect_button.removeClass("btn-success");
                    connect_button.addClass("btn-warning");
                    connect_button_text.empty();
                    connect_button_text.html(" Disconnect");
                    console.log("toggled connection micropayments.");

                },
                error: function(xhr, textStatus, errorThrown) 
                {
                    console.log('error');
                    console.log(errorThrown);
                }
            });
        }
    });

    $('#send_drop').on("click", function(e)
    {
        e.preventDefault();
        $("#to_address_dropdown").slideToggle();
    });
    
    /* Create the bitcoin URI to fill wallet initially */
    function create_uri()
    {
    	$.ajax({
        	type:"GET",
        	url: "address",
        	contentType: 'application/json;',
        	dataType: 'json',
        	success: function(data)
        	{
        		console.log(data);
                create_qrcode(data.address);
                update_balance(data.balance);
        	},
        	error: function(xhr, textStatus, errorThrown) 
        	{
            	console.log('error');
            	console.log(errorThrown);
        	}
        });
    }

    /* Get the price from the gateway */
    function get_price()
    {
        event_update("Checking gateway for BitMesh");

        // TODO: Use the gateway from the function
        // Gateway variable needed to be used here
        var cost_url = "http://" + gateway + ":" + gateway_port + "/cost";

        $.ajax(
        {
            type: 'GET',
            url: cost_url,
            dataType: 'json',
            success: function(data)
            {
                event_update("Gateway is a BitMesh node.");
                $("#price").html(data.price_scalar + " satoshi ");
                cost = data.price_scalar;
                // TODO: Fix this up to use constants server/client side
                if(data.meter_unit === "time_unit")
                    $("#unit").html("second.");
                event_update("Price is " + data.price_scalar + " satoshi.");
            },
            error: function(xhr, textStatus, errorThrown) 
            {
                event_update("Gateway not a BitMesh node.");
                var connect = $("#connect");
                connect.removeClass("btn-success");
                connect.addClass("btn-danger");
                connect.attr('disabled', true);
                console.log('error');
                console.log(errorThrown);
            }
        });
    }

    /* Get the gatway from the client */
    function get_gateway()
    {
        $.ajax(
        {
            type: 'GET',
            url: "gateway",
            dataType: 'json',
            success: function(data)
            {
                // If testing use localhost
                if(test)
                    gateway = "127.0.0.1"
                else
                    gateway = data.gateway;

                event_update("Found gateway: " + gateway);
            },
            error: function(xhr, textStatus, errorThrown) 
            {
                // If test then just use local host
                if(test)
                    gateway = "127.0.0.1"
                else
                {
                    gateway = "10.0.0.1";
                }
                event_update("Could not find gateway, using default: " + gateway);
                console.log("error, using default gateway " + gateway);
                console.log(errorThrown);
            }
        // Get price after bitmesh node found 
        }).done(function(e)
        {
            get_price();
        });
    }

    /* Create a QR code for a bitcoin address */
    function create_qrcode(address)
    {
        var uri = new bitcore.URI({
            address: address, //'1DU9hxnPZfQkuJeVag6cmdnTUYTCYsuNLP',
            amount : (101*cost) // in satoshis
        });

        var uriString = uri.toString();
        $('#qr_wrapper').html('<a id="qr_link" href="' + uriString + '"></a>');
        $('#qr_link').qrcode(uriString);            
        $("#address").html(address);
    }

    /* Update the balance shown to a user */
    function update_balance(balance)
    {
        $("#balance").html("Balance: " + balance + " satoshi.");
    }

    function event_update(update_str)
    {
        var date = construct_date();
        var update = "<p>" + date + ") " + update_str + "</p>";
        $("#updates").append(update);
        var scroller = $("#updates");
        var height = scroller[0].scrollHeight;
        scroller.scrollTop(height);    
    }

    function construct_date()
    {
        var date = new Date();
        var update = "";
        update += addZero(date.getHours()) + ":";
        update += addZero(date.getMinutes()) + ":";
        update += addZero(date.getSeconds());// + ".";
        //update += date.getMilliseconds();
        return update;
    }

    function addZero(i) 
    {
        if (i < 10) 
        {
            i = "0" + i;
        }
        return i;
    }

    /***************************************
     *      Begin websocket functions      *
     ***************************************/

    var client_websocket_uri = "ws://127.0.0.1:" + client_port;

    var websocket = new WebSocket(client_websocket_uri); 
    websocket.binaryType = "blob";
    websocket.binaryType = "arraybuffer";

    websocket.onopen = function(message)
    { 
        console.log("Connected WS");
    }; 

    websocket.onclose = function(message)
    { 
        console.log("Disconnected WS"); 
    }; 
    
    websocket.onmessage = function(message)
    {
        var msg = bytebuffer2str(message.data);
        var json_message = $.parseJSON(msg);
        if(json_message.type != "KEEP_ALIVE")
            console.log(json_message);

        switch(json_message.type)
        {
            case 'KEEP_ALIVE':
                websocket.send("KEEP_ALIVE");
                break;
            case 'ADDRESS_UPDATE':
                var address = json_message.address;
                create_qrcode(address);
                event_update("New address update " + address);
                break;
            case 'BALANCE_UPDATE':
                var balance = json_message.balance;
                update_balance(balance);
                event_update("Balance update, now: " + balance + " satoshi.");
                break;
            case 'PAYMENT_UPDATE':
                event_update("Just payed " + json_message.amount + "satoshi.");
                break;
            case 'CHANNEL_CLOSED':
            case 'CHANNEL_OPENED':
                break;
            default:
                break;
        }
    }; 
    
    websocket.onerror = function(message)
    { 
        console.log("Error: " + evt);
    };

    function bytebuffer2str(buf) 
    {
        return String.fromCharCode.apply(null, new Uint8Array(buf));
    }

});