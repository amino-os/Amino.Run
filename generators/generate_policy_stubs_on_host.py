#!/usr/bin/python

import os, sys
import subprocess

if __name__ == '__main__':

    sapphire_home = os.path.normpath(os.path.join(os.path.realpath(__file__), '../..'))
   
    inFolder = sapphire_home + '/sapphire/sapphire-core/build/classes/java/main/sapphire/policy/'
    package = 'sapphire.policy'
    outFolder = sapphire_home + '/sapphire/sapphire-core/src/main/java/sapphire/policy/stubs/'
    
    cp_sapphire = sapphire_home + '/sapphire/sapphire-core/build/libs/sapphire-core.jar'
    cp_harmony = sapphire_home + "/sapphire/dependencies/apache.harmony/build/libs/apache.harmony.jar"
 
    cmd = ['java', '-cp', cp_sapphire+":"+cp_harmony, 'sapphire.compiler.StubGenerator', inFolder, package, outFolder]

    print(cmd)

    p1 = subprocess.Popen(cmd)
    p1.wait()
    print ("Done!")
