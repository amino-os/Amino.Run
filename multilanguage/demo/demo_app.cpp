#include <iostream>
#include<uuid/uuid.h>   // For UUID creation.

using namespace std;

void come_back() {
  cout << "Back in CPP. !!!" << endl;
}


class Sapphire {

  uuid_t uid;
  char* app_string = (char*)"Hello DCAP from CPP. !!!"; 
  
  public:
    
    char* get_app_string()
    {
      return app_string;
    }

    void  set_sapphire_instance_uuid(char* id)
    {
       uuid_parse(id, uid);
    }
    char* get_sapphire_instance_uuid()
    {
      char* id = new char[100];
      uuid_unparse(uid,id);
      return id;
    }
    
    
    char* sapphire_createInstance();
    void  sapphire_deleteInstance(char*);
    
    void sapphire_helloWorld(char*);
    void sapphire_getObjCount();
    void sapphire_incObjCount();
};

char* Sapphire::sapphire_createInstance()
{
  char* uuid = NULL;
  cout << "Creating the Sapphire instance from CPP. !!!" << endl;
  return uuid;
}

void Sapphire::sapphire_deleteInstance(char* id)
{
  cout << "Deleting the Sapphire instance from CPP. !!! UUID: " << id << endl;
}

void Sapphire::sapphire_helloWorld(char* app_string)
{
  cout << "Hello World from CPP. !!!" << endl;
}

void Sapphire::sapphire_getObjCount()
{
  cout << "Inside CPP getObjCount() !!!" << endl;
}

void Sapphire::sapphire_incObjCount()
{
  cout << "Inside CPP sapphire_incObjCount() !!!";
}

int main() {

  cout << "Inside CPP main function. !!!" << endl;

  Sapphire *obj = new Sapphire();
  obj->sapphire_helloWorld(obj->get_app_string());
  delete obj;
  
  come_back();
}
