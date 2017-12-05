#!\\usr\\bin\\python

import os, sys
import subprocess

if __name__ == '__main__':

    sapphire_home = os.path.normpath(os.path.join(os.path.realpath(__file__), '..\\..'))
   
    inFolder = sapphire_home + '\\sapphire\\sapphire-core\\build\\classes\\main\\sapphire\\policy'
    package = 'sapphire.policy'
    outFolder = sapphire_home + '\\sapphire\\sapphire-core\\src\\main\\java\\sapphire\\policy\\stubs'
    
    cp_sapphire = 'sapphire-core.jar;apache.harmony.jar;java.rmi.jar'
 
    cmd = ['java', '-cp', cp_sapphire, 'sapphire.compiler.StubGenerator', inFolder, package, outFolder]

    print cmd

    p1 = subprocess.Popen(cmd)
    p1.wait()
    print "Done!"
