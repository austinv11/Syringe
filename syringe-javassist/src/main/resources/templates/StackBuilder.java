{
    java.util.HashMap<String, syringe.access.FieldAccessor> field_map = new java.util.HashMap<String, syringe.access.FieldAccessor>();
    {{ populate_static_field_map }}
    if (!{{ is_static_context }}) {
        {{ populate_instance_field_map }}
    }
    syringe.util.LazyMap<String, syringe.access.FieldAccessor> field_map1 = new syringe.util.LazyMap<String, syringe.access.FieldAccessor>();


    java.util.HashMap<String, syringe.access.MethodAccessor> method_map = new java.util.HashMap<String, syringe.access.MethodAccessor>();
    {{ populate_static_method_map }}
    if (!{{ is_static_context }}) {
        {{ populate_instance_method_map }}
    }
    syringe.util.LazyMap<String, syringe.access.MethodAccessor> method_map1 = new syringe.util.LazyMap<String, syringe.access.MethodAccessor>();

    syringe.util.ClassName name = new syringe.util.ClassName({{ class_name }}.class);
    int modifiers = {{ class_modifiers }};
    java.util.ArrayList<syringe.util.Lazy<syringe.info.AnnotationInfo>> annotations = new java.util.ArrayList<>();
    java.lang.annotation.Annotation[] actual_annotations = {{ class_name }}.class.getDeclaredAnnotations();
    for (int i = 0; i < actual_annotations.length; i++) {
        annotations.add(syringe.javassist.util.annotationFromObject(actual_annotations[i]));
    }
    java.util.ArrayList<syringe.util.ClassName> extendsList = new java.util.ArrayList<>();
    java.lang.Class superClass = {{ class_name }}.class.getSuperclass();
    extendsList.add(new syringe.util.ClassName(superClass));
    java.lang.Class[] interfaces = {{ class_name }}.class.getInterfaces();
    for (int i = 0; i < interfaces.length; i++) {
        extendsList.add(new syringe.util.ClassName(interfaces[i]));
    }
    syringe.util.Lazy<Optional<Class>> transformed = new syringe.util.Lazy<>(java.util.Optional.of({{ class_name }}.class));
    {{ self_info_field }} = new syringe.info.ClassInfo(name, modifiers, field_map1, method_map1, annotations, extendsList, transformed);

    for (syringe.callbacks.clazz.ClassInitializerCallback c : {{ callbacks_field }}) {
        c.classInit({{ self_info_field }}, {{ is_static_context }}, field_map1, method_map1);
    }
}