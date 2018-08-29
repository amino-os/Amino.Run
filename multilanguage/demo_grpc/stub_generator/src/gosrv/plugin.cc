#include <google/protobuf/compiler/plugin.h>

#include <string>

#include "gosrv/generator.h"

//Interface for Protoc to call our generator

int main(int argc, char* argv[]) {
  GoSapphireServerGenerator generator;
  return google::protobuf::compiler::PluginMain(argc, argv, &generator);
}
