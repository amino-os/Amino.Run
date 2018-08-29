#include "google/protobuf/descriptor.h"
#include "google/protobuf/io/zero_copy_stream.h"
#include "google/protobuf/io/printer.h"
#include "google/protobuf/descriptor.h"
#include <map>
#include <string>
#include <google/protobuf/compiler/java/java_names.h>
#include <google/protobuf/compiler/code_generator.h>
#include <google/protobuf/compiler/plugin.h>
#include <google/protobuf/descriptor.h>
#include <google/protobuf/io/zero_copy_stream.h>
#include "generator.h"
using google::protobuf::MessageOptions;
using google::protobuf::FileDescriptor;
using google::protobuf::FileOptions;
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


//TODO check input is not reserved keyword in java
//TODO check name compatability with java->proto conversion tool
//TODO java doesn't support unsigned int types

// Map from protobuf type (from fielddescriptor)
// to in language type for primitives
map<FieldDescriptor::Type,string> typenames = {
  {FieldDescriptor::Type::TYPE_DOUBLE, "double"},
  {FieldDescriptor::Type::TYPE_FLOAT, "float"},
  {FieldDescriptor::Type::TYPE_INT64, "long"},
  {FieldDescriptor::Type::TYPE_UINT64, "long"},
  {FieldDescriptor::Type::TYPE_INT32, "int"},
  {FieldDescriptor::Type::TYPE_FIXED64, "long"},
  {FieldDescriptor::Type::TYPE_FIXED32, "int"},
  {FieldDescriptor::Type::TYPE_BOOL, "boolean"},
  {FieldDescriptor::Type::TYPE_STRING, "String"},
  {FieldDescriptor::Type::TYPE_GROUP, "(DEPRECEATED PROTOBUF TYPE GROUP)"},
  {FieldDescriptor::Type::TYPE_MESSAGE, "HANDLED_ELSEWHERE_MESSAGE"}, 
  {FieldDescriptor::Type::TYPE_BYTES, "char[]"},  //TODO: verify this is correct
  {FieldDescriptor::Type::TYPE_UINT32, "int"},
  {FieldDescriptor::Type::TYPE_ENUM, "TODO_ENUM"}, //TODO
  {FieldDescriptor::Type::TYPE_SFIXED32, "int"},
  {FieldDescriptor::Type::TYPE_SFIXED64, "long"},
  {FieldDescriptor::Type::TYPE_SINT32, "int"},
  {FieldDescriptor::Type::TYPE_SINT64, "long"}
};

//boxed types for maps and lists
map<FieldDescriptor::Type,string> boxtypenames = {
  {FieldDescriptor::Type::TYPE_DOUBLE, "Double"},
  {FieldDescriptor::Type::TYPE_FLOAT, "Float"},
  {FieldDescriptor::Type::TYPE_INT64, "Long"},
  {FieldDescriptor::Type::TYPE_UINT64, "Long"},
  {FieldDescriptor::Type::TYPE_INT32, "Integer"},
  {FieldDescriptor::Type::TYPE_FIXED64, "Long"},
  {FieldDescriptor::Type::TYPE_FIXED32, "Integer"},
  {FieldDescriptor::Type::TYPE_BOOL, "Boolean"},
  {FieldDescriptor::Type::TYPE_STRING, "String"},
  {FieldDescriptor::Type::TYPE_GROUP, "(DEPRECEATED PROTOBUF TYPE GROUP)"},
  {FieldDescriptor::Type::TYPE_MESSAGE, "HANDLED_ELSEWHERE_MESSAGE"},
  {FieldDescriptor::Type::TYPE_BYTES, "char[]"},  //TODO: verify this is correct
  {FieldDescriptor::Type::TYPE_UINT32, "Integer"},
  {FieldDescriptor::Type::TYPE_ENUM, "TODO_ENUM"}, //TODO
  {FieldDescriptor::Type::TYPE_SFIXED32, "Integer"},
  {FieldDescriptor::Type::TYPE_SFIXED64, "Long"},
  {FieldDescriptor::Type::TYPE_SINT32, "Integer"},
  {FieldDescriptor::Type::TYPE_SINT64, "Long"}
};


string GetJavaPrimitiveType(string name, const FieldDescriptor* d) {
  switch(d->type()) {
    case FieldDescriptor::Type::TYPE_GROUP:
      throw "Depreceated protobuf type group unsupported";
    case FieldDescriptor::Type::TYPE_MESSAGE:
	    return name + "." + d->message_type()->name();
  	default:
      return typenames[d->type()];
  }
}

string GetJavaBoxType(string name, const FieldDescriptor* d) {
  switch(d->type()) {
    case FieldDescriptor::Type::TYPE_GROUP:
      throw "Depreceated protobuf type group unsupported";
    case FieldDescriptor::Type::TYPE_MESSAGE:
	    return name + "." + d->message_type()->name();
    default:
      return boxtypenames[d->type()];
  }
}


string GetJavaType(string name, const FieldDescriptor* d) {
  //TODO how is protobuf oneof implemented?
  if(d->is_map()) {
    const Descriptor* entry = d->message_type();
    string out = "Map<";
    out += GetJavaBoxType(name, entry->field(0)) + ", " + GetJavaBoxType(name, entry->field(1)) + ">";
    return out;
  } else if(d->is_repeated()) {
    return "List<" + GetJavaBoxType(name, d) + ">";
  } else {
    return GetJavaPrimitiveType(name, d);
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

void GenerateMethod(string name, Printer* out, const MethodDescriptor* method) {
//cout<<"Inside Generate Method";
  if(method->client_streaming() || method->server_streaming()) {
    throw "streaming services not supported";
  }
  StringMap methoddict;
  methoddict["name"] = name;
  methoddict["serv"] = method->service()->name();
  methoddict["method"] = method->name();
auto args = method->input_type();
  methoddict["argmsg"] = args->name();
    auto rets = method->output_type();
  methoddict["retmsg"] = rets->name();
  // Documentation
  SourceLocation sl;
  if(method->GetSourceLocation(&sl)) {
    GenerateComments(sl.leading_comments, out);
  }

  // Function Header
  auto ret = method->output_type();
    const FieldDescriptor* d = ret->field(0);
    methoddict["rettype"] = GetJavaType(name, d);
    methoddict["retname"] = capitalizeFirst(d->name());
  //out->Print("// Adding the Wrap Implemention");
  out->Print(methoddict,"byte[] $method$ (byte[] reqData ) ");
      

  // out->Print(") {\n");
out->Print("{\n");

  // Function Body
  out->Indent();
  out->Print(methoddict, "$name$.$argmsg$ request;\n");
out->Print("try{\n");
  out->Print(methoddict, "request = $name$.$argmsg$.parseFrom(reqData); \n\n");
out->Print("\n}catch (InvalidProtocolBufferException e) {\n");
out->Print(methoddict," System.out.println(\"$method$ parameter deserialization failed: \" + e.getMessage());\n }\n");
out->Print(methoddict,"$rettype$  $retname$ =");
  out->Print(methoddict,"super.$method$(");
  for(int i = 0; i < args->field_count(); ++i) {
    const FieldDescriptor* d1 = args->field(i);
    methoddict["argname"] = capitalizeFirst(d1->name());
    methoddict["comma"] = (i == args->field_count() - 1) ? "" : ", ";

  	if(d1->is_map()) {
      out->Print(methoddict, "request.get$argname$Map()$comma$");
    } else if(d1->is_repeated()) {
      out->Print(methoddict, "request.get$argname$List()$comma$");
    } else {
      out->Print(methoddict, "request.get$argname$()$comma$");
    }
  }
out->Print(");\n\n");
  //call kernel interface
  //TODO call kernel interface to make rpc 
  methoddict["ret"] = ret->name();
  out->Print(methoddict, "$name$.$ret$.Builder retmsg = $name$.$ret$.newBuilder();\n\n");
    
  
   	if(d->is_map()) {
      out->Print(methoddict, "retmsg.putAll$retname$($retname$);\n");
    } else if(d->is_repeated()) {
      out->Print(methoddict, "retmsg.addAll$retname$($retname$);\n");
    } else {
      out->Print(methoddict, "retmsg.set$retname$($retname$);\n");
    }
  out->Print(methoddict, "$name$.$ret$ msgbuffer = retmsg.build();\n\n");

   out->Print("return msgbuffer.toByteArray();\n");
  out->Outdent();
  out->Print("}\n\n");
}

void GenerateReturnType(string name, Printer* out, const MethodDescriptor* method) {
  const Descriptor* d = method->output_type();
  if(d->field_count() < 2)
      return;

  StringMap returndict;
  returndict["rettype"] = d->name();
  out->Print(returndict, "static class $rettype$ {\n");
  out->Indent();

  for(int i = 0; i < d->field_count(); ++i) {
    const FieldDescriptor* fd = d->field(i);
    returndict["type"] = GetJavaType(name, fd);
    returndict["name"] = capitalizeFirst(fd->name()); 
    out->Print(returndict, "public $type$ $name$;\n");
  }

  out->Outdent();
  out->Print("}\n\n");
}
void GenerateConstructMehtod(string name,string packagename,Printer *out){
  StringMap condict;
  condict["classname"]=name;
  condict["packagename"]=packagename;
   out->Print(condict,"public $classname$_Construct(String sObjConstructorName, byte[] sObjConstructorParams,\n");
    out->Print("\t\t\t\t\t\t\t\t              int sObjParentSObjId, int sObjId,\n");
    out->Print("\t\t\t\t\t\t\t\t              int sObjReplicaId,Object sObjReplicaObject){\n");
  out->Print("int constParamLen = 0;\n");
        out->Print("try {\n");
         out->Print(condict,"   constParamLen = $packagename$.parseFrom(sObjConstructorParams).getSerializedSize();\n");
        out->Print("} catch (InvalidProtocolBufferException e) {\n\n");

        out->Print("}\n");

        out->Print(condict,"Class<?> cl = $classname$_Stub.class.getClass();\n");
        out->Print(condict,"$classname$_Stub sObjReplica = null;\n");
        out->Print("if (constParamLen == 0) {\n");
            out->Print("try {\n");
                out->Print(condict,"sObjReplica = ($classname$)cl.newInstance();\n");
            out->Print("} catch (InstantiationException e) {\n");
                out->Print("logger.severe(\"SObj instance creation failed.!!!\" + e.getMessage());\n");
                out->Print("e.printStackTrace();\n");
                out->Print("return null;\n");
            out->Print("} catch (IllegalAccessException e) {\n");
                out->Print("logger.severe(\n");
                        out->Print("\"SObj instance creation raised Illegal accessException.!!!\"\n");
                                out->Print("+ e.getMessage());\n");
                out->Print("e.printStackTrace();\n");
                out->Print("return null;\n");
            out->Print("}\n");
        out->Print("} else {\n");
            out->Print("ByteArrayInputStream bis = new ByteArrayInputStream(sObjConstructorParams);\n");
            out->Print("Object inparams = null;\n");
            out->Print("try {\n");
                out->Print("inparams = (new ObjectInputStream(bis)).readObject();\n");
            out->Print("} catch (IOException e) {\n");
                out->Print("logger.severe(\"SObj Constructor params deserialization failed.!!!\" + e.getMessage());\n");
                out->Print("e.printStackTrace();\n");
                out->Print("return null;\n");
            out->Print("} catch (ClassNotFoundException e) {\n");
                out->Print("logger.severe(\"SObj Constructor params deserialization failed.!!!\" + e.getMessage());\n");
                out->Print("e.printStackTrace();\n");
                out->Print("return null;\n");
            out->Print("}\n");
        out->Print("}\n\n");

        out->Print("sObjReplica.replicaId.setsObjReplicaId(sObjId, replicaId, parentSObjId);\n");

        out->Print("return sObjReplica;\n\n");
    out->Print("}\n\n");
  
}
void GenerateConstructor(string name, Printer* out){
    StringMap condict;
    condict["classname"]= name;
   out->Print(condict,"public $classname$_Stub(){\n");
    //Contstructor Defination needs to be added 
  
   out->Print("super();\n");
  out->Print("}\n\n");
}
string ServiceJavaPackage(const FileDescriptor* file) {
  string result = google::protobuf::compiler::java::ClassName(file);
   size_t last_dot_pos = result.find_last_of('.');
  if (last_dot_pos != string::npos) {
    result.resize(last_dot_pos);
  } else {
    result = "";
  }
  return result;
}

void JavaSapphireGenerator::GenerateSapphireStubs(GeneratorContext* context, string name, const FileDescriptor* file) const {
  
  name = capitalizeFirst(name);
  for(int i = 0; i < file->service_count(); ++i) {	   
    auto service = file->service(i);
    StringMap typedict;
    
    ZeroCopyOutputStream* zcos = context->Open(service->name()+"_Stub" + ".java");; 
   string classname= service->name();
    auto out = new Printer(zcos, '$');
 string package=file->package();

    string javapackage = ServiceJavaPackage(file);
    

  typedict["javapackage"]=javapackage;
   
    out->Print(typedict,"package $javapackage$;\n");
       typedict["package"]=package;
    out->Print("import java.util.*;\n");
    out->Print("import com.google.protobuf.InvalidProtocolBufferException;\n");
    out->Print("import java.io.ByteArrayInputStream;\n");
out->Print("import java.io.ByteArrayOutputStream;\n");
out->Print("import java.io.IOException;\n");
out->Print("import java.io.ObjectInputStream;\n");
out->Print("import java.io.ObjectOutputStream;\n");
out->Print("import java.util.logging.Logger;\n");
name =capitalizeFirst(classname);
   typedict["package_import"]="import "+package+"."+name+"Proto";
	out->Print(typedict,"$package_import$;\n");
   typedict["classname"]=classname;
  out->Print(typedict,"import $javapackage$\n;");
	  SourceLocation sl;
	  if(service->GetSourceLocation(&sl)) {
      GenerateComments(sl.leading_comments, out);
	  }
    // Class Header
    typedict["name"] = service->name();
    out->Print(typedict, "class $classname$_Stub extends $classname$ {\n");

    out->Indent();
     // Each method implementation
    // out->Print(" SObjReplicaId replicaId;\n");  
      out->Print("Object sObj;\n");
   // GenerateConstructMehtod(classname,name+"Proto",out);
GenerateConstructor(classname,out);
    for(int j = 0; j < service->method_count(); ++j) {
      GenerateMethod(name+"Proto", out, service->method(j));
    }

    // Tuple types for returns
    for(int j = 0; j < service->method_count(); ++j) {
      GenerateReturnType(classname, out, service->method(j));
    }

    out->Outdent();
    out->Print("}\n");

    if(out->failed()) {
        throw "IO error occured during proto generation";
    }

    //Printer needs to close before zcos
    delete out;
    delete zcos;

  }
}


