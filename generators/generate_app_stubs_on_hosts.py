#!/usr/bin/python

import os, sys
import subprocess
from apps.todo import *
#from apps.minnietwitter import *

if __name__ == '__main__':

    sapphire_home = os.path.normpath(os.path.join(os.path.realpath(__file__), '../..'))
 
    cp_app =  sapphire_home + '/sapphire/examples/' + app_name + '/build/libs/' + app_name + '.jar'
    cp_sapphire = sapphire_home + '/sapphire/sapphire-core/build/libs/sapphire-core.jar'
    cp_harmony = sapphire_home + '/sapphire/dependencies/apache.harmony/build/libs/apache.harmony.jar'
    
    cmd = ['java', '-cp',  cp_app + ':' + cp_sapphire + ':' + cp_harmony, 'sapphire.compiler.StubGenerator', sapphire_home + inFolder, package, sapphire_home + outFolder]

    p1 = subprocess.Popen(cmd)
    p1.wait()

    print("Done!")
