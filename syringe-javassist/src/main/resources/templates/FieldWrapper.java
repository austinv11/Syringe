{
    syringe.access.FieldAccessor acc_{{ field_name }} = new syringe.access.FieldAccessor() {
        @Override
        public Object get() {
            return {{ field_name }};
        }
    };
    field_map.put({{ field_name }}, acc_{{ field_name }});
}