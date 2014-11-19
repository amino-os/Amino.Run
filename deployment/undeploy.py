#!/usr/bin/python

import os, sys
import subprocess
from time import sleep
import atexit
import signal
from app import *
import json

ssh_cmd = "ssh"
log_folder = "/bigraid/users/iyzhang/sapphire/logs"

def parse_server_config(server_file):
    f = open(server_file,"r")
    config = json.JSONDecoder().decode(f.read())
    return config

def run_cmd(cmd):
    print reduce(lambda x, y: x + " " + y, cmd, "")
    return subprocess.Popen(cmd)

def kill_servers(config):
    hosts = map(lambda x: x["hostname"], config["servers"])
    hosts.append(config["oms"]["hostname"])
    clients = map(lambda x: x["hostname"], config["clients"])
    hosts.extend(clients)
    allHosts = set(hosts)

    for h in allHosts:
        cmd = [ssh_cmd, h]
        cmd += ["pkill dalvikvm"]
        run_cmd(cmd)

if __name__ == '__main__':
    server_file = "servers.json"
    config = parse_server_config(server_file)

    kill_servers(config)
