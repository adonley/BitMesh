#!/bin/bash
## Created by Andrew Donley on April 7, 2015
## Installation script for a raspberrypi using
## BitMesh.


## Make sure user is sudo
#if [ "$(id -u)" != "0" ]; then
#   echo "Bitmesh must be installed using su or sudo." 1>&2;
#   exit 1;
#fi

## Asumption is that we have the dependencies installed already

function main 
{
	#describe_changes;
	# Setup has to come before the config functions.
	setup_interfaces;

	dnsmasq_config;
	hostapd_config;
	sysctl_config;
	interfaces_config;
	general_system_config;
}

function describe_changes
{
	# TODO: put this in the preinstall script before the dependencies get installed.
	printf "%s\n\n%s" "During the installation process we will be installing hostapd and dnsmasq. The original configurations for these programs and \"/etc/network/interfaces\" will be saved and moved to a temporary location when bitmesh is running. Also, ipv4/ipv6 forwarding will be enabled on your system." "Is this ok with you?";
	local ok;
	read ok;
}

function setup_interfaces
{
	local -a options;
	local count=0;

	## Get interfaces from the operating system
	for interface in $(ip link show | awk '/^[0-9]/ {print $2;} ' | sed 's/:$//');
	do
		#echo "\"$interface\"";
		if [ $interface != "lo" ] && [ $interface != "" ] ;
		then
			options[$count]=$interface;
			count=$((count+1));
		fi
	done

	printf 'Which network card has a connection to the internet?: \n';

	for ((i = 0; i < ${#options[@]}; i++)); do
		echo "$i) ${options[$i]}";
	done

	## Get the interface selection from the user
	read input;
	while (( $input >= ${#options} ))
	do
		echo "Bad interface selection. Try again: ";
		read input;
	done

	## Get the selected out of the options array
	OUT_FACE=${options[$input]};
	unset options[$input];
	printf "You've selected $OUT_FACE.\n";	

	## TODO: Need to check out the drivers for the inface, AP mode?

	## Hack to reorder the array
	declare -a new_options;
	count=0;
	for option in ${options[@]}
	do
		new_options[$count]=$option;
		count=$((count+1));
	done
	unset $options;

	printf 'Which network card will be the access point for people to connect to?: \n';

	## Echo the remaining options
	for ((i = 0; i < ${#new_options[@]}; i++)); 
	do
		echo "$i) ${new_options[$i]}";
	done

	## Get the interface
	read input;
	while (( $input >= ${#new_options} ))
	do
		echo "Bad interface selection. Try again: ";
		read input;
	done

	IN_FACE=${new_options[$input]};
	printf "You've selected $IN_FACE.\n";
	unset $new_options;

}

## Write interface and rang to dnsmasq.conf
function dnsmasq_config
{
	printf "Updating /etc/dnsmasq.conf\n";
	local conf_location="/etc/dnsmasq.conf";

	## Don't over-write the original if we've already installed
	if [ ! -f  $conf_location"_orig" ]
	then
		mv $conf_location $conf_location"_orig";
	else
		rm $conf_location
	fi
	
	printf "interface=$IN_FACE\ndhcp-range=10.0.19.84,10.0.19.255" >> /etc/dnsmasq.conf;
	#chmod 755 $conf_location # TODO: this probably needs to be changed...
}

## Fix hostapd init.d file
function hostapd_config
{
	printf "Updating /etc/hostapd/hostapd.conf, for some reason this is broken on the standard install.";
	sed -i 'DAEMON_CONF/c\DAEMON_CONF=/etc/hostapd/hostapd.conf' /etc/init.d/hostapd;
	# TODO: Create the file referenced above.
	printf "interface=wlan1\ncountry_code=US\nssid=BitMesh\nhw_mode=g\nchannel=1\nbeacon_int=100\nmacaddr_acl=0\nwmm_enabled=1" > /etc/hostapd/hostapd.conf
	chmod 755 /etc/hostapd/hostapd.conf
}

## Enable ipv4 forwarding
function sysctl_config
{
	sed -i '/net.ipv4.ip_forward/c\net.ipv4.ip_forward = 1' /etc/sysctl.conf;
	sed -i '/net.ipv6.conf.all.forwarding/c\net.ipv6.conf.all.forwarding = 1' /etc/sysctl.conf
	sysctl -p /etc/sysctl.conf
}

## Preserve the same interfaces file except for the interface in question
function interfaces_config
{
	## Only move the file if the original hasn't already been moved
	if [ ! -f  /etc/network/interfaces_orig ]
	then
		mv /etc/network/interfaces /etc/network/interfaces_orig
	else
		## Otherwise bring back original configuraton and edit from there
		mv /etc/network/interfaces_orig /etc/network/interfaces
	fi

	# Copy everything except for the interface we are interested in
	awk -vRS='' '$2 != "'$IN_FACE'"i {print; print "";}' /etc/network/interfaces > /etc/network/interfaces.tmp
	printf "iface $IN_FACE inet static\naddress 10.0.19.84" >> /etc/network/interfaces.tmp;
	mv /etc/network/interfaces.tmp /etc/network/interfaces
}

function general_system_config
{
	# service network-manager restart
	# sleep 5
	# service networking restart
	# sleep 5
	# ifconfig $IN_FACE down up 10.0.19.84
	# sleep 10
	service hostapd stop
	service dnsmasq stop

	# Move the interfaces file back to the original
	mv /etc/network/interfaces /etc/network/interfaces_bmesh
	mv /etc/network/interfaces_orig /etc/network/interfaces

	# TODO: Not guarenteed to work, stop these from running automatically
	update-rc.d hostapd disable
	update-rc.d dnsmasq disable
}

main;
