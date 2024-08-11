public class Parameter {
    private String name;
    private String type;
    private Object value;
    private String label;

    public Parameter(String name, String type, Object value, String label) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }
}
