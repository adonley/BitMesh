# BitMesh 

BitMesh is a project built on the idea that if you pay for internet, it’s yours to do what you like with it. Including resell it. Networks are all about transitivity. BitMesh lets you share your internet connection with those around you in exchange for Bitcoin.

How it works: Turn on BitMesh, an open hotspot appears, people connect to it and are redirected to a captive portal where they must pay you in Bitcoin to use the internet. 

Entry points: 
<br>Sources/JavaClient/src/network/bitmesh/TransactionServer/Server/BitmeshServer.java
<br>Sources/JavaClient/src/network/bitmesh/TransactionServer/Client/BitmeshClient.java


#Overview
BitMesh uses the following: iptables, dnsmasq, hostapd, Java server, webclient, Java app, bitcore.js, micropayment channels, bitcoinj, the WISPr protocol, nanohttpd, protocol buffers, gulp

hostapd is in charge of the wifi connection and authentication process.<br>
dnsmasq assigns ip addresses via DHCP over the broadcasting interface<br>
iptables routes the traffic between the broadcasting and connected interface and NATs the traffic. <br>
bitcoinj is used server side to manage connections and payment receipts <br>
bitcore.js is used browser-side to create payment-channels with the bitcoinj server<br>
WISPr protocol shows clients a landing page when they connect to the BitMesh hotspot, before they even try to navigate to a website. This negates the issues with displaying a landing page when an https connection is requested. <br>
micropayment channels let you pay in small amounts per second or whatever and all the small transactions are aggregated securely off-chain, with the resulting aggregated transaction being posted on-chain after a time limit<br>
nanohttpd is used for serving the captive portal and the hotspot UI<br>
protocol buffers are used to communicate payments between browser and hotspot <br>
gulp is used to minify and deploy the javascript<br>


#Goals of the project
We want BitMesh to enable micro-ISPs, where users pay for what they use and privacy is respected. Sellers should be able to download install and have a node running without having to fiddle with installing dependencies, system configuration or compiling code. User experience is important. It should just work. (We are not there yet).


#Project Layout
Installers/Debian - this is the beginnings of a .deb file that would install bitmesh on debian systems. There were some issues in clearing the system database after a user answered a question once in an installer (i.e. - the system would automatically fill out the answers on which network interface to use during a reinstall). The basic function was to choose the interfaces on the host computer and put in place scripts to start and stop the server. This needs to be changed from the native package type and ideally would include compiling the java sources to the debian package spec.

Installers/Raspi - The raspi installer was the preliminary testing we were doing before we moved to the debian installer. The code in this directory is mostly incomplete.

Sources/CloudServer - A transaction server that would post locked transactions after the locktime expired. This is unnecessary with CHECKLOCKTIMEVERIFY.

Sources/JavaClient/jni/pcap is a sort of branch where bytes are paid for, instead of seconds. It was basically functional, but did not get integrated into the rest of the project. It requires jni because Java doesn’t hook into the network card like C can. If anyone knows how to do this in pure Java, patches are welcome.

Sources/JavaClient/resources/org/bitcoinj/crypto/ - stuff that comes with bitcoinj, a wordlist for pneumonics...and some cert?

Sources/JavaClient/resources/network/bitmesh/Sentry/Connectivity/ - shell scripts for modifying IPtables or mac equivalent “pf”. PF code does not work on OSX, yet.

Sources/JavaClient/resources/network/bitmesh/Sentry/WISPr/ - the first page shown in the captive portal is loaded in a different browser than normal. This page disappears once you connect to the internet on some systems, so we abuse the protocol to make it pop up immediately and redirect you to another page in your normal browser, where we can run javascript while you browse. 

Sources/JavaClient/resources/network/bitmesh/WebServer/ - This is the code that manages a web interface to manage the hotspot. Probably the weakest part of the project, security wise...

Sources/JavaClient/src/fi/iki/elonen/ - All the nanohttpd code. Some of it was patched, so it’s not easily updatable. 

Sources/JavaClient/src/org/bitcoin* - All the bitcoinj stuff. Note that we modified payment channels quite a bit, the .proto file and the bitcoinj and bitcore code so they could interoperate. We modified Bitcoinj payment channels to use P2SH, not raw-multisig, and bitcore uses the more extensive conversation defined in bitcoinj.

Sources/JavaClient/src/network/bitmesh/channels/ - Modified bitcoinj payment channel code. Put it in a different directory so we could still update bitcoinj without changing this.

Sources/JavaClient/src/network/bitmesh/websockets/ - Code for handling websocket connections server-side with browser and dashboard

Sources/JavaClient/src/network/bitmesh/WebServer/ - serves captive portal and wispr sequence 

Sources/JavaClient/src/network/bitmesh/Utils/StateMachine.java - we have state machines everywhere, so we made a formal class for it

Sources/JavaClient/src/network/bitmesh/Units/ - Units to be sold via captive portal. The micropayment platform can be ported fairly easily to selling other stuff, like power or milk

Sources/JavaClient/src/network/bitmesh/TransactionServer/ - code for managing all the payments and accounts 

Sources/JavaClient/src/network/bitmesh/Statistics/ - classes for keeping statistics for the dashboard

Sources/JavaClient/src/network/bitmesh/Sentry/Logic/ - classes for resource-management logic. Selling internet-seconds? Use SentryTimeLogic.java Selling internet-bytes? Use SentryDataLogic.java

Sources/JavaClient/src/network/bitmesh/Sentry/Connectivity/ - classes for handling clients and their connectivity and their payments

Sources/JavaClient/src/network/bitmesh/Sentry/WISPr/ - classes for handling and abusing on a per-platform basis the obscure and inconsistently-implemented WISPr protocol

Sources/Utilities - A utility library that was used by most java portions of the project to perform basic cryptographic functions. We did the best we could to obscure the private keys and only have them in memory when the class is loaded within the byte code by inlining them as byte arrays instead of static class variables. Proguard file included.

Sources/FileSigner - a small commandline app to sign BitMesh releases for verification. The idea was to be able to verify an update coming from the cloud server when starting up the app.

Sources/WebClient/js - most of these files are dependencies for bitcore, or for using protocol buffers with javascript. The main logic for us is in bitmesh.js or bitmesh_mobile.js. They are different files because javascript micropayments don’t work on mobile because when you change tabs on mobile, the javascript on the other tabs stops running, probably for power conservation.

#Todos:
Library management systems, migrate the main java source to maven or gradle.

Integrate CHECKLOCKTIMEVERIFY, make the code much simpler

Remove the usage tracking information since we aren’t concerned with BitMesh usage as much anymore.

Virtual network interfaces so users don’t need two wireless cards.

Finish dashboard UI for hotspot “how much money have I made? how much internet have I disseminated?”

Switch Nanohttpd to a different websocket framework. There were issues with websockets timing out after a certain duration and required a manual heartbeat.

Authentication for the dashboard

Integration with 802.11s
