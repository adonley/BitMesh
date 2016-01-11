#!/bin/bash
#
# Script to deploy the captive portal to the cloud
#

gulp deploy
scp -i ./SANPALO.pem -r dist/* ubuntu@ec2-54-94-132-0.sa-east-1.compute.amazonaws.com:/var/www/html
#scp -i ./SANPALO.pem -r css ubuntu@ec2-54-94-132-0.sa-east-1.compute.amazonaws.com:/var/www/html
#scp -i ./SANPALO.pem -r fonts ubuntu@ec2-54-94-132-0.sa-east-1.compute.amazonaws.com:/var/www/html
#scp -i ./SANPALO.pem -r proto ubuntu@ec2-54-94-132-0.sa-east-1.compute.amazonaws.com:/var/www/html
#scp -i ./SANPALO.pem -r images ubuntu@ec2-54-94-132-0.sa-east-1.compute.amazonaws.com:/var/www/html
#scp -i ./SANPALO.pem dist/bitmesh.html ubuntu@ec2-54-94-132-0.sa-east-1.compute.amazonaws.com:/var/www/html/index.html

