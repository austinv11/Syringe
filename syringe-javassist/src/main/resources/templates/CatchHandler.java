{
    syringe.info.MethodInfo curr_method = {{ self_info_field }}.getMethods().get("{{ method_key }}");
    $_ = ($r) {{ callback_field }}.exceptionThrown({{ self_info_field }}, curr_method, (java.lang.Throwable) $e);
}