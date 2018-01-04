## sapphire core integration test
This integration test runs on linux hosts (cloud) only.
Its purpose is to provide small env that is able to evaluate DM in action.

### instructions
the working folder assumes the Saphhire git dir
(TODO) to automate the whole process
* cd sapphire/sapphire-core
* gradle build
* cd generator
* python ./generate_policy_stubs_on_host.py
* cd sapphire/sapphire-core
* gradle build
* cd sapphire/examples/hankdTodo
* cp build_on_host.gradle build.gradle
* gradle build
* cd generator
* python ./generate_app_stubs_on_host.py
* cd sapphire/examples/hankdTodo
* gradle build
* cd deployment/tests
* ./local_int_start.sh


