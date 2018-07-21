{
    syringe.info.MethodInfo curr_method = {{ self_info_field }}.getMethods().get("{{ method_key }}");
    $_ = ($r) {{ callback_field }}.call({{ self_info_field }}, curr_method, {{ static_context }} ? null : this, new java.util.ArrayList($args), (java.lang.Object) $_);
}