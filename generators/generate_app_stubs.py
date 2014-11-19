#!/usr/bin/python

import os, sys
import subprocess
from apps.todo import *
#from apps.minnietwitter import *

if __name__ == '__main__':

    try:  
        android_home = os.environ["ANDROID_BUILD_TOP"]
    except KeyError: 
        print "ANDROID_BUILD_TOP is not set - should have been set while building android"
        sys.exit()
   
    cp_app =  android_home + '/../example_apps/' + app_name + '/bin/classes.dex'
    cp_sapphire = android_home + '/../sapphire/bin/classes.dex'
    
    cmd = [android_home + '/out/host/linux-x86/bin/dalvik', '-cp',  cp_app + ':' + cp_sapphire, 'sapphire.compiler.StubGenerator', android_home + inFolder, package, android_home + outFolder]
    p1 = subprocess.Popen(cmd)
    p1.wait()

    print "Done!"
