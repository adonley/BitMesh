#!/bin/bash
###
# Script to bring up internet connection sharing on mac. Must be completed
# as superuser.
###

$SELL_INTERFACE=en0
$BUY_INTERFACE=en1

ifconfig $SELL_INTERFACE down
ifconfig $BUY_INTERFACE down

# Parse NAT plist
# http://hints.macworld.com/article.php?story=20090510120814850
# defaults -> edits plist
# /Library/Preferences/SystemConfiguration/com.apple.nat.plist
launchctl load -w /System/Library/LaunchDaemons/com.apple.NetworkSharing.plist

# Load pf from config file.
# https://gist.github.com/kujohn/7209628
pfctl -ef /etc/pf.conf

# DHCP Server
# http://www.jacquesf.com/2011/04/mac-os-x-dhcp-server/
# Check to see if this file exists /etc/bootpd.plist
sudo /bin/launchctl load -w /System/Library/LaunchDaemons/bootps.plist

# Anchor location
#/etc/pf.anchors

ifconfig $SELL_INTERFACE up
ifconfig $BUY_INTERFACE up

## Interesting Shiz
#  Stop bootpd plist from being overwritten: https://apple.stackexchange.com/questions/45913/how-to-stop-internetsharing-overwriting-etc-bootpd-plist
#  About internet sharing in mac: https://soundofsyntax.wordpress.com/2012/02/09/osx-internet-sharing-under-the-hood/
