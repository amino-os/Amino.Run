## sapphire core integration test
This integration test runs on linux hosts (cloud) only.
Its purpose is to provide small env that is able to evaluate DM in action.

### instructions
```
# Set DCAP_ROOT
DCAP_ROOT="Replace with your local path to DCAP-Sapphire"

# Build sapphire-core
cd $DCAP_ROOT/sapphire/sapphire-core && gradle build

# Generate policy stubs
cd $DCAP_ROOT/generators && python ./generate_policy_stubs_on_host.py

# Build sapphire-core again
cd $DCAP_ROOT/sapphire/sapphire-core && gradle build

# Build HanksTodo
cd $DCAP_ROOT/sapphire/examples/hanksTodo
cp build_on_host.gradle build.gradle
gradle build

# Generate App Stub
cd $DCAP_ROOT/generator
python ./generate_app_stubs_on_host.py

cd $DCAP_ROOT/sapphire/examples/hanksTodo
gradle build

cd $DCAP_ROOT/deployment/tests
./local_int_start.sh
```

