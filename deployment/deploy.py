#!/usr/bin/python

import os, sys
import subprocess
from time import sleep
import atexit
import signal
from app import *
import getpass
import json
import os

ssh_cmd = "ssh"

#log_folder = "/bigraid/users/" + getpass.getuser() + "/sapphire/logs"
log_folder = "/bigraid/users/" + getpass.getuser() + "/sapphire_code/deployment/logs"

def run_cmd(cmd):
    print reduce(lambda x, y: x + " " + y, cmd, "")
    return subprocess.Popen(cmd)

def parse_server_config(server_file):
    f = open(server_file,"r")
    config = json.JSONDecoder().decode(f.read())
    return config


def start_oms(oms, server_file, android_home, cp_app, cp_sapphire, p_log):
    hostname = oms["hostname"]
    port = oms["port"]

    print 'Starting OMS on '+hostname+":"+port
    cmd = [ssh_cmd, hostname, android_export]
    cmd += [android_home + '/out/host/linux-x86/bin/dalvik']
    cmd += ['-Djava.util.logging.config.file=\"' + p_log + '\"']
    cmd += ['-cp ' + cp_app + ':' + cp_sapphire]
    cmd += ['sapphire.oms.OMSServerImpl', hostname, port]
    cmd += [app_class]
    cmd += [">", log_folder+"/oms-log."+hostname+"."+port, "2>&1 &"]
    run_cmd(cmd)

    sleep(2)

def start_servers(servers, oms, android_home, cp_app, cp_sapphire, p_log):
    for s in servers:
        hostname = s["hostname"]
        port = s["port"]
        print "Starting kernel server on "+hostname+":"+port

        # /bin/classes.dex is generated after you try to run an android app from eclipse_adt
        cmd = [ssh_cmd, hostname, android_export]
        cmd += [android_home + '/out/host/linux-x86/bin/dalvik']
        cmd += ['-Djava.util.logging.config.file=\"' + p_log + '\"']
        cmd += ['-cp ' + cp_app + ':' + cp_sapphire]
        cmd += ['sapphire.kernel.server.KernelServerImpl']
        cmd += [hostname, port, oms["hostname"], oms["port"]]
        cmd += [">", log_folder+"/log."+hostname+"."+port, "2>&1 &"]
        run_cmd(cmd)
    
    sleep(2)


def start_clients(clients, oms, android_home, cp_app, cp_sapphire, p_log):

    for client in clients:
        hostname = client["hostname"]
        port = client["port"]
        print 'Starting App on '+hostname+":"+port

        # /bin/classes.dex is generated after you try to run an android app from eclipse_adt
        cmd = [ssh_cmd, hostname, android_export]
        cmd += [android_home + '/out/host/linux-x86/bin/dalvik']
        cmd += ['-Djava.util.logging.config.file=\"' + p_log + '\"']
        cmd += ['-cp ' + cp_app + ':' + cp_sapphire]
        cmd += [app_client, oms["hostname"], oms["port"], hostname, port]
        cmd += [">", log_folder+"/client-log."+hostname+"."+port, "2>&1"]
        run_cmd(cmd)

if __name__ == '__main__':
    try:  
        android_home = os.environ["ANDROID_BUILD_TOP"]
        android_export = "ANDROID_BUILD_TOP="+android_home
    except KeyError: 
        print "ANDROID_BUILD_TOP is not set - should have been set while building android"
        sys.exit()

    cp_app =  android_home + '/../example_apps/' + app_name + '/bin/classes.dex'
    cp_sapphire = android_home + '/../sapphire/bin/classes.dex'
    p_log = android_home + '/../sapphire/logging.properties'

    server_file = "servers.json"
    config = parse_server_config(server_file)
    start_oms(config["oms"], os.path.abspath(server_file), android_home, cp_app, cp_sapphire, p_log)
    start_servers(config["servers"], config["oms"], android_home, cp_app, cp_sapphire, p_log)
    start_clients(config["clients"], config["oms"], android_home, cp_app, cp_sapphire, p_log)
    
