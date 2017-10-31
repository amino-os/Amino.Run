#!/usr/bin/python

import os, sys
import subprocess

if __name__ == '__main__':

    try:  
        android_home = os.environ["ANDROID_BUILD_TOP"]
    except KeyError: 
        print "ANDROID_BUILD_TOP is not set - should have been set while building android"
        sys.exit()
   
    inFolder = android_home + '/../sapphire/bin/classes/sapphire/policy/'
    package = 'sapphire.policy'
    outFolder = android_home + '/../sapphire/src/sapphire/policy/stubs/'
    
    cp_sapphire = android_home + '/../sapphire/bin/classes.dex'
 
    cmd = [android_home + '/out/host/linux-x86/bin/dalvik', '-cp', cp_sapphire, 'sapphire.compiler.StubGenerator', inFolder, package, outFolder]

    print cmd

    p1 = subprocess.Popen(cmd)
    p1.wait()
    print "Done!"
