
#include "generator_base.h"

bool BaseSapphireGenerator::Generate(const FileDescriptor* file,
			                         const string& parameter,
                                     GeneratorContext* context,
                                     string* error) const {
  // Verify valid filename"

  string protoname = file->name();
  if(protoname.size() <= strlen(".proto")) {
    *error = "Invalid proto filename. Not long enough";
    return true;
  }
  if(protoname.find_last_of(".proto") != protoname.size() - 1) {
    *error = "Invalid proto filename. Proto file must end with .proto";
    return true;
  }

  // Generate base filename for output
  string base = protoname.substr(0, protoname.size() - strlen(".proto"));

  // Output stubs
  try {
 //   cout<<file;
    this->GenerateSapphireStubs(context, base, file);
  } catch (std::exception ex) {
    *error = ex.what();
    return false;
  } catch (string ex) {
    *error = ex;
    return false;
  } catch (char* ex) {
    *error = ex;
    return false;
  } catch (char const* ex) {
    *error = ex;
    return false;
  }
  return true;
}

bool BaseSapphireGenerator::GenerateAll(const vector<const FileDescriptor*>& files,
                                        const string& parameter, GeneratorContext* context, 
                                        string * error) const {
  for(auto f : files) {
    if(!Generate(f, parameter, context, error)) {
      return false;
    }
  }
  return true;
};

bool BaseSapphireGenerator::HasGenerateAll() const { return true; }

BaseSapphireGenerator::~BaseSapphireGenerator() {};
