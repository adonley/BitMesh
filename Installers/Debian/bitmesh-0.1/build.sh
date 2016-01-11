#!/bin/bash

#fakeroot dpkg-deb -z8 -Zgzip --build $1
#lintian $1".deb"

#dpkg -r bitmesh
debuild -us -uc
#dpkg -i ../bitmesh_0.1_amd64.deb
