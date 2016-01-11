#!/bin/bash
## Andrew Donley - 13 Aug, 2015
## Function for starting BitMesh and Stopping BitMesh

function main
{
	start_bitmesh;
	#iptables;
	#stop_bitmesh;
}

function start_bitmesh
{
	# Change interfaces files
	mv /etc/network/interfaces /etc/network/interfaces_orig
	mv /etc/network/interfaces_bmesh /etc/network/interfaces

	# Restart interface 
	service network-manager restart
	sleep 5
	service networking restart
	sleep 5
	ifconfig $IN_FACE down up 10.0.19.84
	sleep 10

	# start up hostapd and dnsmasq
	service hostapd restart
	service dnsmasq restart
}

function stop_bitmesh
{
	# Change interfaces files
	mv /etc/network/interfaces /etc/network/interfaces_bmesh
	mv /etc/network/interfaces_orig /etc/network/interfaces

	# Restart interface 
	service network-manager restart
	sleep 5
	service networking restart
	sleep 5

	# start up hostapd and dnsmasq
	service hostapd stop
	service dnsmasq stop
}

function iptables
{
	iptables -t nat -p PREROUTING ACCEPT
	iptables -t nat -p POSTROUTING ACCEPT
	iptables -t nat -p OUTPUT ACCEPT

	iptables -t nat -A PREROUTING -i 
}