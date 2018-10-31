package sapphire.appexamples.hankstodo.stubs;

public final class TodoListManager_Stub extends sapphire.common.GraalObject implements sapphire.common.GraalAppObjectStub {

    sapphire.policy.SapphirePolicy.SapphireClientPolicy $__client = null;
    boolean $__directInvocation = false;

    public TodoListManager_Stub () {super();}

    public void $__initialize(sapphire.policy.SapphirePolicy.SapphireClientPolicy client) {
        $__client = client;
    }

    public void $__initialize(boolean directInvocation) {
        $__directInvocation = directInvocation;
    }

    public Object $__clone() throws CloneNotSupportedException {
        return clone();
    }

    public void $__initializeGraal(sapphire.app.SapphireObjectSpec spec, java.lang.Object[] params){
        try {
            super.$__initializeGraal(spec, params);
        } catch (java.lang.Exception e) {
            throw new java.lang.RuntimeException(e);
        }

    }

    public boolean $__directInvocation(){
        return $__directInvocation;
    }

    public java.lang.Object newTodoList(Object... args) {
        java.lang.Object $__result = null;
        if ($__directInvocation) {
            try {
                java.util.ArrayList<Object> objs = new java.util.ArrayList<>();
                objs.addAll(java.util.Arrays.asList(args));
                $__result = super.invoke("newTodoList", objs);
            } catch (java.lang.Exception e) {
                throw new sapphire.common.AppExceptionWrapper(e);
            }
        } else {
            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
            String $__method = "public java.lang.Object sapphire.appexamples.hankstodo.stubs.TodoListManager_Stub.newTodoList(java.lang.Object...)";
            $__params.addAll(java.util.Arrays.asList(args));
            try {
                $__result = $__client.onRPC($__method, $__params);
                if ($__result instanceof sapphire.graal.io.SerializeValue) {
                    $__result = deserializedSerializeValue((sapphire.graal.io.SerializeValue)$__result);
                    $__result = ((org.graalvm.polyglot.Value)$__result).as(java.lang.Object.class);
                }
            } catch (sapphire.common.AppExceptionWrapper e) {
                Exception ex = e.getException();
                if (ex instanceof java.lang.RuntimeException) {
                    throw (java.lang.RuntimeException) ex;
                } else {
                    throw new java.lang.RuntimeException(ex);
                }
            } catch (java.lang.Exception e) {
                throw new java.lang.RuntimeException(e);
            }
        }

        return ($__result);
    }

    public java.lang.Object deleteTodoList(Object... args) {
        java.lang.Object $__result = null;
        if ($__directInvocation) {
            try {
                java.util.ArrayList<Object> objs = new java.util.ArrayList<>();
                objs.addAll(java.util.Arrays.asList(args));
                $__result = super.invoke("deleteTodoList", objs);
            } catch (java.lang.Exception e) {
                throw new sapphire.common.AppExceptionWrapper(e);
            }
        } else {
            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
            String $__method = "public java.lang.Object sapphire.appexamples.hankstodo.stubs.TodoListManager_Stub.deleteTodoList(java.lang.Object...)";
            $__params.addAll(java.util.Arrays.asList(args));
            try {
                $__result = $__client.onRPC($__method, $__params);
                if ($__result instanceof sapphire.graal.io.SerializeValue) {
                    $__result = deserializedSerializeValue((sapphire.graal.io.SerializeValue)$__result);
                    $__result = ((org.graalvm.polyglot.Value)$__result).as(java.lang.Object.class);
                }
            } catch (sapphire.common.AppExceptionWrapper e) {
                Exception ex = e.getException();
                if (ex instanceof java.lang.RuntimeException) {
                    throw (java.lang.RuntimeException) ex;
                } else {
                    throw new java.lang.RuntimeException(ex);
                }
            } catch (java.lang.Exception e) {
                throw new java.lang.RuntimeException(e);
            }
        }

        return $__result;
    }

    public java.lang.Object addTodo(Object... args) {
        java.lang.Object $__result = null;
        if ($__directInvocation) {
            try {
                java.util.ArrayList<Object> objs = new java.util.ArrayList<>();
                objs.addAll(java.util.Arrays.asList(args));
                $__result = super.invoke("addTodo", objs);
            } catch (java.lang.Exception e) {
                throw new sapphire.common.AppExceptionWrapper(e);
            }
        } else {
            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
            String $__method = "public java.lang.Object sapphire.appexamples.hankstodo.stubs.TodoListManager_Stub.addTodo(java.lang.Object...)";
            $__params.addAll(java.util.Arrays.asList(args));
            try {
                $__result = $__client.onRPC($__method, $__params);
                if ($__result instanceof sapphire.graal.io.SerializeValue) {
                    $__result = deserializedSerializeValue((sapphire.graal.io.SerializeValue)$__result);
                    $__result = ((org.graalvm.polyglot.Value)$__result).as(java.lang.Object.class);
                }
            } catch (sapphire.common.AppExceptionWrapper e) {
                Exception ex = e.getException();
                if (ex instanceof java.lang.RuntimeException) {
                    throw (java.lang.RuntimeException) ex;
                } else {
                    throw new java.lang.RuntimeException(ex);
                }
            } catch (java.lang.Exception e) {
                throw new java.lang.RuntimeException(e);
            }
        }

        return $__result;
    }

    public java.lang.Object getTodos(Object... args) {
        java.lang.Object $__result = null;
        if ($__directInvocation) {
            try {
                java.util.ArrayList<Object> objs = new java.util.ArrayList<>();
                objs.addAll(java.util.Arrays.asList(args));
                $__result = super.invoke("getTodos", objs);
            } catch (java.lang.Exception e) {
                throw new sapphire.common.AppExceptionWrapper(e);
            }
        } else {
            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
            String $__method = "public java.lang.Object sapphire.appexamples.hankstodo.stubs.TodoListManager_Stub.getTodos(java.lang.Object...)";
            $__params.addAll(java.util.Arrays.asList(args));
            try {
                $__result = $__client.onRPC($__method, $__params);
                if ($__result instanceof sapphire.graal.io.SerializeValue) {
                    $__result = deserializedSerializeValue((sapphire.graal.io.SerializeValue)$__result);
                    $__result = ((org.graalvm.polyglot.Value)$__result).as(java.lang.Object.class);
                }
            } catch (sapphire.common.AppExceptionWrapper e) {
                Exception ex = e.getException();
                if (ex instanceof java.lang.RuntimeException) {
                    throw (java.lang.RuntimeException) ex;
                } else {
                    throw new java.lang.RuntimeException(ex);
                }
            } catch (java.lang.Exception e) {
                throw new java.lang.RuntimeException(e);
            }
        }

        return $__result;
    }

    public java.lang.Object completeTodo(Object... args) {
        java.lang.Object $__result = null;
        if ($__directInvocation) {
            try {
                java.util.ArrayList<Object> objs = new java.util.ArrayList<>();
                objs.addAll(java.util.Arrays.asList(args));
                $__result = super.invoke("completeTodo", objs);
            } catch (java.lang.Exception e) {
                throw new sapphire.common.AppExceptionWrapper(e);
            }
        } else {
            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
            String $__method = "public java.lang.Object sapphire.appexamples.hankstodo.stubs.TodoListManager_Stub.completeTodo(java.lang.Object...)";
            $__params.addAll(java.util.Arrays.asList(args));
            try {
                $__result = $__client.onRPC($__method, $__params);
                if ($__result instanceof sapphire.graal.io.SerializeValue) {
                    $__result = deserializedSerializeValue((sapphire.graal.io.SerializeValue)$__result);
                    $__result = ((org.graalvm.polyglot.Value)$__result).as(java.lang.Object.class);
                }
            } catch (sapphire.common.AppExceptionWrapper e) {
                Exception ex = e.getException();
                if (ex instanceof java.lang.RuntimeException) {
                    throw (java.lang.RuntimeException) ex;
                } else {
                    throw new java.lang.RuntimeException(ex);
                }
            } catch (java.lang.Exception e) {
                throw new java.lang.RuntimeException(e);
            }
        }

        return $__result;
    }

}
