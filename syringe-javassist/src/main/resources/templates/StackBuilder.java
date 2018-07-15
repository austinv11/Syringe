{
    HashMap<String, syringe.access.FieldAccessor> field_map = new HashMap<String, syringe.access.FieldAccessor>();
    {{ populate_static_field_map }}
    if (!{{ is_static_context }}) {
        {{ populate_instance_field_map }}
    }
    syringe.util.LazyMap<String, syringe.access.FieldAccessor> field_map1 = new syringe.util.LazyMap<String, syringe.access.FieldAccessor>();


    HashMap<String, syringe.access.MethodAccessor> method_map = new HashMap<String, syringe.access.MethodAccessor>();
    {{ populate_static_method_map }}
    if (!{{ is_static_context }}) {
        {{ populate_instance_method_map }}
    }
    syringe.util.LazyMap<String, syringe.access.MethodAccessor> method_map1 = new syringe.util.LazyMap<String, syringe.access.MethodAccessor>();

    for (syringe.callbacks.clazz.ClassInitializerCallback c : {{ callbacks_field }}) {
        c.classInit({{ self_info_field }}, {{ is_static_context }}, field_map1, method_map1);
    }
}