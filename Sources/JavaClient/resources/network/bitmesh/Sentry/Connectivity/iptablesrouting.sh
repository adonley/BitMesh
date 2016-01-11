#!/bin/bash

BUY_INTERFACE=$1
SELL_INTERFACE=$2
IPT="iptables"
CAPTIVE_PORTAL=54.94.132.0
LOCALHOST=10.0.19.84
HTTP_PORT=8080
WEBSOCKET_PORT=11984
BITCOIN_PORT=8333
TESTNET_PORT=18333
OPTIND=1                          # Reset in case getopts has been used previously in the shell.

echo "BUY".$BUY_INTERFACE;
echo "SELL".$SELL_INTERFACE;

# Flush the tables
$IPT -F
$IPT -X
$IPT -t nat -F
$IPT -t nat -X
$IPT -t mangle -F
$IPT -t mangle -X
$IPT -t raw -F
$IPT -t raw -X

$IPT -F INPUT
$IPT -F OUTPUT
$IPT -F FORWARD

# Accept everything by default
$IPT -t nat --policy PREROUTING ACCEPT
$IPT -t nat --policy POSTROUTING ACCEPT
$IPT -t nat --policy OUTPUT ACCEPT

# Whitelist ourselves and blockchain.info
$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol tcp -d 54.232.226.45 -j ACCEPT
$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol tcp -d 190.93.243.195 -j ACCEPT
$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol tcp -d 141.101.112.196 -j ACCEPT
$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol tcp -d 54.94.132.0 -j ACCEPT
$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol tcp -d 54.207.45.234 -j ACCEPT
$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol tcp -d $LOCALHOST -j ACCEPT

# TODO: put this in -c
# Whitelist bitcoin traffic
$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol tcp --dport $BITCOIN_PORT -j ACCEPT
$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol udp --dport $BITCOIN_PORT -j ACCEPT
$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol tcp --dport $TESTNET_PORT -j ACCEPT
$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol udp --dport $TESTNET_PORT -j ACCEPT

# TODO: put this in the -c?
# Whitelist websockets
$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol tcp --dst $LOCALHOST --dport $WEBSOCKET_PORT -j ACCEPT

# Allow forwarding packets:
$IPT -A FORWARD --protocol ALL --in-interface $BUY_INTERFACE -j ACCEPT
$IPT -A FORWARD --in-interface $SELL_INTERFACE -m state --state ESTABLISHED,RELATED -j ACCEPT

while getopts "c:o:" opt; do
    case "$opt" in
    # c - redirect to captive portal
    c)
#        $IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol tcp -s $OPTARG --dport $BITCOIN_PORT -j ACCEPT
#        $IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol udp -s $OPTARG --dport $BITCOIN_PORT -j ACCEPT
#        $IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol tcp -s $OPTARG --dport $TESTNET_PORT -j ACCEPT
#        $IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol udp -s $OPTARG --dport $TESTNET_PORT -j ACCEPT
#        $IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol tcp -s $OPTARG --dst $LOCALHOST --dport $WEBSOCKET_PORT -j ACCEPT
	    $IPT -t nat -A PREROUTING --protocol tcp --source $OPTARG -j DNAT --to-destination $CAPTIVE_PORTAL
#	    $IPT -t nat -A PREROUTING --protocol udp --source $OPTARG -j DNAT --to-destination $CAPTIVE_PORTAL
        ;;
    # o - allow online
    o)
		$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --source $OPTARG -j ACCEPT
		;;
	*)
	    echo "Invalid argument $OPTARG"
	    ;;
    esac
done

# Drop ssl
$IPT -A INPUT --in-interface $SELL_INTERFACE --protocol tcp --dport 443 -j DROP

# Redirect everything else to ourselves
$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol tcp -j DNAT --to-destination $LOCALHOST:$HTTP_PORT
#$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol tcp --dport 443 -j DNAT --to-destination $LOCALHOST:$HTTP_PORT


# Allow for DNS and SSH to go through. Might have to rethink this

#$IPT -t nat -A PREROUTING --in-interface

#$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol udp -j DNAT --to-destination $LOCALHOST:$HTTP_PORT

#$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol tcp --dport 80 -j REDIRECT --to-port $HTTP_PORT
#$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol tcp --dport 443 -j REDIRECT --to-port $HTTP_PORT
#$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol udp --dport 80 -j REDIRECT --to-port $HTTP_PORT
#$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol tcp -m multiport ! --dports 22,53 -j REDIRECT
#$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol udp -m multiport ! --dports 22,53 -j REDIRECT
#$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol sctp -m multiport ! --dports 22,53 -j REDIRECT
#$IPT -t nat -A PREROUTING --in-interface $SELL_INTERFACE --protocol dccp -j REDIRECT

# Packet masquerading
$IPT -t nat -A POSTROUTING --out-interface $BUY_INTERFACE -j MASQUERADE
