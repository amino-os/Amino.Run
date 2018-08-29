#include "google/protobuf/descriptor.h"
#include "google/protobuf/io/zero_copy_stream.h"
#include "google/protobuf/io/printer.h"

#include <map>
#include <string>

#include "generator.h"

using google::protobuf::FileDescriptor;
using google::protobuf::MethodDescriptor;
using google::protobuf::Descriptor;
using google::protobuf::FieldDescriptor;
using google::protobuf::SourceLocation;
using google::protobuf::io::ZeroCopyOutputStream;
using google::protobuf::io::Printer;
using std::map;
using std::string;
typedef map<string, string> StringMap;

string capitalizeFirst(string s) {
  s[0] = toupper(s[0]);
  return s;
}


//TODO check input is not reserved keyword in golang
//TODO Camelcase all the names (because the go protobuf compiler does for public access)

// Map from protobuf type (from fielddescriptor)
// to in language type for primitives
map<FieldDescriptor::Type,string> typenames = {
  {FieldDescriptor::Type::TYPE_DOUBLE, "float64"},
  {FieldDescriptor::Type::TYPE_FLOAT, "float32"},
  {FieldDescriptor::Type::TYPE_INT64, "int64"},
  {FieldDescriptor::Type::TYPE_UINT64, "uint64"},
  {FieldDescriptor::Type::TYPE_INT32, "int32"},
  {FieldDescriptor::Type::TYPE_FIXED64, "uint64"},
  {FieldDescriptor::Type::TYPE_FIXED32, "uint32"},
  {FieldDescriptor::Type::TYPE_BOOL, "bool"},
  {FieldDescriptor::Type::TYPE_STRING, "string"},
  {FieldDescriptor::Type::TYPE_GROUP, "(DEPRECEATED PROTOBUF TYPE GROUP)"},
  {FieldDescriptor::Type::TYPE_MESSAGE, "TODO_MESSAGE"}, //TODO
  {FieldDescriptor::Type::TYPE_BYTES, "[]byte"},  //TODO: verify this is correct
  {FieldDescriptor::Type::TYPE_UINT32, "uint32"},
  {FieldDescriptor::Type::TYPE_ENUM, "TODO_ENUM"}, //TODO
  {FieldDescriptor::Type::TYPE_SFIXED32, "int32"},
  {FieldDescriptor::Type::TYPE_SFIXED64, "int64"},
  {FieldDescriptor::Type::TYPE_SINT32, "int32"},
  {FieldDescriptor::Type::TYPE_SINT64, "int64"}
};

string GetGoPrimitiveType(const FieldDescriptor* d) {
  switch(d->type()) {
    case FieldDescriptor::Type::TYPE_GROUP:
      throw "Depreceated protobuf type group unsupported";
    case FieldDescriptor::Type::TYPE_MESSAGE:
	  return "*" + capitalizeFirst(d->message_type()->name());
	default:
      return typenames[d->type()];
  }
}

string GetGoType(const FieldDescriptor* d) {
  //TODO how is protobuf oneof implemented?
  if(d->is_map()) {
    const Descriptor* entry = d->message_type();
    string out = "map[";
    out += GetGoType(entry->field(0)) + "]" + GetGoType(entry->field(1));
    return out;
  } else if(d->is_repeated()) {
    return "[]" + GetGoPrimitiveType(d);
  } else {
    return GetGoPrimitiveType(d);
  }
}

void GenerateComments(string comment, Printer* out) {
  if(comment.length() == 0)
      return;

  out->Print("/**\n");
  string::size_type ind = 0;
  string::size_type next;
  while((next = comment.find("\n",ind)) != string::npos) {
    out->Print(" * $line$ \n", "line", comment.substr(ind, next - ind));
    ind = next + 1;
  }
  out->Print(" * $line$ \n", "line", comment.substr(ind));
  out->Print(" **/\n");
}

void GenerateMethod(Printer* out, const MethodDescriptor* method) {

  if(method->client_streaming() || method->server_streaming()) {
    throw "streaming services not supported";
  }

  StringMap methodDict;
  methodDict["name"] = method->service()->name();
  methodDict["method"] = capitalizeFirst(method->name());

  // Documentation
  SourceLocation sl;
  if(method->GetSourceLocation(&sl)) {
    GenerateComments(sl.leading_comments, out);
  }
  out->Print(methodDict, "func (Obj $name$) $method$_Wrap(");

  // Function Header
  methodDict["argname"] = "inParams";
  methodDict["type"]    = "[]byte";
  out->Print(methodDict, "inParams  [] byte");

  out->Print(") (");

  methodDict["argname"] = "outParams";
  methodDict["type"]    = "[]byte";
  out->Print(methodDict, "outParams  [] byte");

  out->Print(") {\n");

  // Function Body
  out->Indent();

  auto args = method->input_type();


  methodDict["argmsg"] = capitalizeFirst(args->name());
  out->Print(methodDict, "InPutStruct := &$argmsg${} ; ");
  //out->Indent();
  out->Print("\n");
  out->Print("_ = proto.Unmarshal(inParams, InPutStruct)\n");


  auto ret = method->output_type();

  methodDict["argmsg"] = capitalizeFirst(ret->name());
  out->Print(methodDict, "OutPutStruct := &$argmsg${} ; ");
 // out->Indent();
  out->Print("\n");

  for(int i = 0; i < ret->field_count(); ++i) {
      const FieldDescriptor* d = ret->field(i);
      //methoddict["type"] = GetGoType(d);
      methodDict["argname"] = d->name();
  	  methodDict["comma"] = (i == ret->field_count() - 1) ? "" : ", ";
      out->Print(methodDict, "OutPutStruct.$argname$$comma$");
  }
  //out->Indent();
  out->Print(" = ");

  //call actual function

  //methodDict["name"] = method->service()->name();
  methodDict["method"] = capitalizeFirst(method->name());
  out->Print(methodDict, " Obj.$method$(");

  for(int i = 0; i < args->field_count(); ++i) {
    const FieldDescriptor* d = args->field(i);
    methodDict["argname"] = d->name();
  	methodDict["comma"] = (i == args->field_count() - 1) ? "" : ", ";
  	out->Print(methodDict, "InParamSt.$argname$ $comma$");
  }
  out->Print(")");
  out->Print("\n");

  //TODO check err
  out->Print("outParams, _ := proto.Marshal(OutPutStruct)\n\n");

  out->Print("return ");


  out->Print("\n");
  out->Outdent();
  out->Print("}\n\n");
}

void GoSapphireServerGenerator::GenerateSapphireStubs(GeneratorContext* context, string name, const FileDescriptor* file) const {
  
  ZeroCopyOutputStream* zcos = context->Open("SapphireServerStub" + name + ".pb.go");;
  auto out = new Printer(zcos, '$');

  // Package statement
  // TODO include protobuf package structure, and nested message imports
  out->Print("package $pkg$\n\n", "pkg", name);
  out->Print("import \"github.com/golang/protobuf/proto\"\n\n");

  for(int i = 0; i < file->service_count(); ++i) {
    auto service = file->service(i);
    StringMap typedict;

    // Documentation
    SourceLocation sl;
    if(service->GetSourceLocation(&sl)) {
      GenerateComments(sl.leading_comments, out);
    }

    // Type Definition
    typedict["name"] = capitalizeFirst(service->name());
    out->Print(typedict, "type $name$ struct {\n");
    out->Indent();
    out->Print("Oid uint64\n");
    out->Outdent();
    out->Print("}\n\n");

    // Each method implementation
    for(int j = 0; j < service->method_count(); ++j) {
      GenerateMethod(out, service->method(j));
    }
  }

  if(out->failed()) {
    throw "IO error occured during proto generation";
  }

  //Printer needs to close before zcos
  delete out;
  delete zcos;
}

