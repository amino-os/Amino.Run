#ifndef BASE_SAPPHIRE_GENERATOR_H
#define BASE_SAPPHIRE_GENERATOR_H

#include <google/protobuf/compiler/code_generator.h>
#include <google/protobuf/descriptor.h>

#include <string>

using namespace std;
using google::protobuf::FileDescriptor;
using google::protobuf::compiler::GeneratorContext;

//Simplify CodeGenerator interface for implementation
class BaseSapphireGenerator : public google::protobuf::compiler::CodeGenerator {
 public:

  //Implement this in subclass
  virtual void GenerateSapphireStubs(GeneratorContext* context, 
                                     string base,
                                     const FileDescriptor* file) const = 0;

  bool Generate(const FileDescriptor* file,
                const string& parameter,
                GeneratorContext* context,
                string* error) const;

  bool GenerateAll(const vector<const FileDescriptor*>& files,
                   const string& parameter, GeneratorContext* context, 
                   string * error) const;

  bool HasGenerateAll() const;

  virtual ~BaseSapphireGenerator();
};

#endif  // BASE_SAPPHIRE_GENERATOR_H
