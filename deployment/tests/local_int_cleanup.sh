#!/bin/bash

port_oms=22343
port_kernel=22345

echo "killing oms & kernel server..."
netstat -tpln 2>>/dev/null | grep -E "${port_oms}|${port_kernel}" | awk '{print $(NF)}' | cut -d '/' -f 1 | xargs kill

