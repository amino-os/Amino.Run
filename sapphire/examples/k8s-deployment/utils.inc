
TOP_DIR=$(git rev-parse --show-toplevel)

check_java() {
  local _java=`which java`

  if [ ! -f $_java ]; then
    echo "ERROR: java is not installed."
    exit 1
  fi
}

build_dcap_sapphire() {
  echo "Building DCAP-sapphire.."
  # TODO: Invoke grade build to generate all the DCAP-sapphire jars
}

verify_jars_present() {
  jar_list=$@
  for jar in ${jar_list[@]}; do
    if [ ! -f $TOP_DIR/$jar ]; then
      build_dcap_sapphire
      break
    fi
  done
}

copy_jars() {
  jar_dir=$1
  shift
  jar_list=$@
  #rm -rf $jar_dir/*
  mkdir -p $jar_dir
  for jar in ${jar_list[@]}; do
    cp -f $TOP_DIR/$jar $jar_dir/
  done
}

