
get_container_ip() {
  local _hostname=$(hostname -f)
  container_ip="$(cat /etc/hosts | grep $_hostname | grep -Eo '[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}')"
  if [ -z $container_ip ]; then
    echo "ERROR: Unable to acquire IP for this container."
    exit 1
  fi
}

get_service_ip() {
  local _svcname=$1
  service_ip=$(nslookup $_svcname | grep -A1 "Name:.*$_svcname" | grep -Eo '[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}')
}
