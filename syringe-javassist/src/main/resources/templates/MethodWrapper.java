{
    syringe.access.MethodAccessor invoke_{{ method_name }} = new syringe.access.MethodAccessor() {
        @Override
        Object invoke(Object[] args) throws Throwable {
            {{ invocation_statement }}
        }
    };
    method_map.put({{ method_name }}, invoke_{{ method_name }});
}